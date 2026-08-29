package com.enterprise.spendsync.notification.internal.service;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.notification.api.dto.NotificationPreferenceRequest;
import com.enterprise.spendsync.notification.api.dto.NotificationPreferenceResponse;
import com.enterprise.spendsync.notification.internal.domain.NotificationPreference;
import com.enterprise.spendsync.notification.internal.repository.NotificationPreferenceRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    public NotificationPreferenceServiceImpl(NotificationPreferenceRepository preferenceRepository,
                                             UserRepository userRepository,
                                             TenantRepository tenantRepository) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(UUID userId, UUID tenantId) {
        NotificationPreference pref = getOrCreatePreferenceEntity(userId, tenantId);
        return mapToResponse(pref);
    }

    @Override
    public NotificationPreferenceResponse updatePreferences(UUID userId, UUID tenantId, NotificationPreferenceRequest request) {
        NotificationPreference pref = getOrCreatePreferenceEntity(userId, tenantId);
        pref.updatePreferences(request.emailEnabled(), request.inAppEnabled(), request.emailAddress());
        NotificationPreference saved = preferenceRepository.save(pref);
        return mapToResponse(saved);
    }

    @Override
    public NotificationPreference getOrCreatePreferenceEntity(UUID userId, UUID tenantId) {
        return preferenceRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseGet(() -> {
                    User user = userRepository.findByIdAndTenantId(userId, tenantId)
                            .orElseThrow(() -> new SpendSyncException("User not found: " + userId, HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new SpendSyncException("Tenant not found: " + tenantId, HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});
                    NotificationPreference newPref = new NotificationPreference(user, tenant, true, true, null);
                    return preferenceRepository.save(newPref);
                });
    }

    private NotificationPreferenceResponse mapToResponse(NotificationPreference pref) {
        return new NotificationPreferenceResponse(
                pref.getId(),
                pref.getUser().getId(),
                pref.isEmailEnabled(),
                pref.isInAppEnabled(),
                pref.getEmailAddress(),
                pref.getUpdatedAt()
        );
    }
}
