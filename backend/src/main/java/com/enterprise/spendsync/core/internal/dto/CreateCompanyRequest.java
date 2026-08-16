package com.enterprise.spendsync.core.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Payload for creating a new corporate Tenant and primary Legal Entity.
 */
public record CreateCompanyRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotBlank(message = "Company name cannot be empty")
        @Size(max = 255, message = "Company name cannot exceed 255 characters")
        String companyName,

        @NotBlank(message = "Legal entity name cannot be empty")
        @Size(max = 255, message = "Legal entity name cannot exceed 255 characters")
        String legalEntityName,

        @NotBlank(message = "Company code cannot be empty")
        @Size(max = 20, message = "Company code cannot exceed 20 characters")
        String companyCode,

        @NotBlank(message = "Tax number (VKN/Tax ID) cannot be empty")
        @Size(max = 50, message = "Tax number cannot exceed 50 characters")
        String taxNumber,

        @Size(max = 100, message = "Tax office cannot exceed 100 characters")
        String taxOffice,

        @NotBlank(message = "Base currency cannot be empty")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Base currency must be ISO 4217 3-letter code (e.g. TRY, EUR, USD)")
        String baseCurrency,

        @NotBlank(message = "Registered address cannot be empty")
        String registeredAddress,

        @NotBlank(message = "Country code cannot be empty")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be ISO 3166-1 alpha-2 (e.g. TR, DE, US)")
        String country
) {}
