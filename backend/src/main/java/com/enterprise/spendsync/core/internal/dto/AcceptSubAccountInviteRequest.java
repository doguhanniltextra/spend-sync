package com.enterprise.spendsync.core.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AcceptSubAccountInviteRequest(
        @NotBlank(message = "Invite token cannot be empty")
        String token,

        @NotBlank(message = "First name cannot be empty")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name cannot be empty")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        String phoneNumber,
        String jobTitle,

        @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be ISO 3166-1 alpha-2 (e.g. TR, US)")
        String country,

        String timezone,
        String preferredLanguage
) {}
