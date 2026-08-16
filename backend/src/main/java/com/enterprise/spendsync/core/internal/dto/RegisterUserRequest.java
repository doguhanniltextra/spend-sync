package com.enterprise.spendsync.core.internal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for registering a new user account before corporate tenant assignment.
 */
public record RegisterUserRequest(
        @NotBlank(message = "First name cannot be empty")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name cannot be empty")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email address cannot be empty")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @Size(max = 30, message = "Phone number cannot exceed 30 characters")
        String phoneNumber,

        @NotBlank(message = "Country code cannot be empty")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be ISO 3166-1 alpha-2 (e.g. TR, DE, US)")
        String country,

        @Size(max = 100, message = "Job title cannot exceed 100 characters")
        String jobTitle,

        String timezone,

        String preferredLanguage
) {}
