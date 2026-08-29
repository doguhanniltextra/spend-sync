package com.enterprise.spendsync.notification.service;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.notification.api.dto.NotificationPreferenceRequest;
import com.enterprise.spendsync.notification.api.dto.NotificationPreferenceResponse;
import com.enterprise.spendsync.notification.internal.domain.NotificationPreference;
import com.enterprise.spendsync.notification.internal.repository.NotificationPreferenceRepository;
import com.enterprise.spendsync.notification.internal.service.NotificationPreferenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceImplTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private NotificationPreferenceServiceImpl preferenceService;

    private UUID userId;
    private UUID tenantId;
    private User user;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();

        user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        tenant = mock(Tenant.class);
    }

    @Test
    @DisplayName("Should return existing preferences when already present in repository")
    void shouldReturnExistingPreferences() {
        NotificationPreference existing = new NotificationPreference(user, tenant, true, false, "override@domain.com");
        when(preferenceRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(existing));

        NotificationPreferenceResponse response = preferenceService.getPreferences(userId, tenantId);

        assertThat(response.emailEnabled()).isTrue();
        assertThat(response.inAppEnabled()).isFalse();
        assertThat(response.emailAddress()).isEqualTo("override@domain.com");
    }

    @Test
    @DisplayName("Should create default preferences when user has no stored preference")
    void shouldCreateDefaultPreferencesWhenNoneExist() {
        when(preferenceRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.empty());
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

        NotificationPreferenceResponse response = preferenceService.getPreferences(userId, tenantId);

        assertThat(response.emailEnabled()).isTrue();
        assertThat(response.inAppEnabled()).isTrue();
        assertThat(response.emailAddress()).isNull();
    }

    @Test
    @DisplayName("Should update delivery preferences successfully")
    void shouldUpdateDeliveryPreferences() {
        NotificationPreference existing = new NotificationPreference(user, tenant, true, true, null);
        when(preferenceRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

        NotificationPreferenceRequest request = new NotificationPreferenceRequest(false, true, "alert@domain.com");
        NotificationPreferenceResponse updated = preferenceService.updatePreferences(userId, tenantId, request);

        assertThat(updated.emailEnabled()).isFalse();
        assertThat(updated.inAppEnabled()).isTrue();
        assertThat(updated.emailAddress()).isEqualTo("alert@domain.com");
    }
}
