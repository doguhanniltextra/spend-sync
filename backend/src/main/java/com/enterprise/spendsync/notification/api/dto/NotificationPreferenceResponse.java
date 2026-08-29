package com.enterprise.spendsync.notification.api.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationPreferenceResponse(
        UUID id,
        UUID userId,
        boolean emailEnabled,
        boolean inAppEnabled,
        String emailAddress,
        Instant updatedAt
) {}
