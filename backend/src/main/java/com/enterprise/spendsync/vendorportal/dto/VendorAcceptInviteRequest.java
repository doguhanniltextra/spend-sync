package com.enterprise.spendsync.vendorportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorAcceptInviteRequest(
        @NotBlank(message = "Invitation token is required")
        String token,

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name cannot exceed 150 characters")
        String fullName,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        String phoneNumber,
        String taxOffice,
        String address,
        String city,
        String country,
        String bankName,
        String iban
) {}
