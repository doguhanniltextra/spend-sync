package com.enterprise.spendsync.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CatalogItemUpdateRequest(
        @NotBlank(message = "Item name is required")
        @Size(max = 255, message = "Item name must be at most 255 characters")
        String name,

        String description,

        UUID categoryId,

        UUID preferredVendorId,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be non-negative")
        BigDecimal unitPrice,

        String currency,

        BigDecimal vatRate,

        String unitOfMeasure,

        String contractReference,

        LocalDate validFrom,

        LocalDate validUntil,

        Boolean isPreferred,

        Boolean isActive,

        String glAccountCode
) {
}
