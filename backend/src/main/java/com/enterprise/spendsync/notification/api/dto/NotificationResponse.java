package com.enterprise.spendsync.notification.api.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String eventType,
        String title,
        String body,
        String referenceType,
        UUID referenceId,
        boolean isRead,
        Instant createdAt,
        Instant readAt
) {}
