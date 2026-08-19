package com.enterprise.spendsync.vendorportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorInviteRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Tax number / VKN is required")
        @Size(max = 50, message = "Tax number cannot exceed 50 characters")
        String taxNumber,

        @NotBlank(message = "Company name is required")
        @Size(max = 255, message = "Company name cannot exceed 255 characters")
        String companyName
) {}
