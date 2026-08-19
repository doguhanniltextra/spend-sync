package com.enterprise.spendsync.vendorportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record VendorCatalogProposalRequest(
        UUID itemMasterId,

        @NotBlank(message = "Proposed item code is required")
        @Size(max = 100)
        String proposedItemCode,

        @NotBlank(message = "Proposed name is required")
        @Size(max = 255)
        String proposedName,

        @NotBlank(message = "Proposed category is required")
        @Size(max = 100)
        String proposedCategory,

        @NotNull(message = "Proposed unit price is required")
        @Positive(message = "Proposed unit price must be positive")
        BigDecimal proposedUnitPrice,

        String proposedCurrency,
        BigDecimal vatRate,
        Integer leadTimeDays,
        String notes
) {}
