package com.enterprise.spendsync.notification.api.dto;

import jakarta.validation.constraints.Email;

public record NotificationPreferenceRequest(
        boolean emailEnabled,
        boolean inAppEnabled,
        @Email(message = "Invalid email format")
        String emailAddress
) {}
