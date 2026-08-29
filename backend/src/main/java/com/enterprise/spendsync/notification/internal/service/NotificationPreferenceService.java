package com.enterprise.spendsync.notification.internal.service;

import com.enterprise.spendsync.notification.api.dto.NotificationPreferenceRequest;
import com.enterprise.spendsync.notification.api.dto.NotificationPreferenceResponse;
import com.enterprise.spendsync.notification.internal.domain.NotificationPreference;

import java.util.UUID;

public interface NotificationPreferenceService {

    NotificationPreferenceResponse getPreferences(UUID userId, UUID tenantId);

    NotificationPreferenceResponse updatePreferences(UUID userId, UUID tenantId, NotificationPreferenceRequest request);

    NotificationPreference getOrCreatePreferenceEntity(UUID userId, UUID tenantId);
}
