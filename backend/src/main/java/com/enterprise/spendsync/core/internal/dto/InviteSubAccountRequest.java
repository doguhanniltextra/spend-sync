package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record InviteSubAccountRequest(
        @NotBlank(message = "Recipient email cannot be empty")
        @Email(message = "Invalid recipient email address")
        String email,

        @NotNull(message = "Target legal entity ID is required")
        UUID targetLegalEntityId,

        @NotEmpty(message = "At least one role must be assigned")
        Set<RoleType> targetRoles,

        Integer expirationHours // Optional, defaults to 48 hours
) {}
