package com.enterprise.spendsync.core.internal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinAsRequisitionerRequest(
        @NotBlank(message = "Invite token cannot be empty")
        String token,

        @NotBlank(message = "Email address cannot be empty")
        @Email(message = "Invalid email format")
        String email,

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
        String employeeId,

        @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be ISO 3166-1 alpha-2 (e.g. TR, DE, US)")
        String country,

        String timezone,
        String preferredLanguage
) {}
