package com.enterprise.spendsync.requisition.internal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record SetApprovalLimitRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Legal Entity ID is required")
        UUID legalEntityId,

        UUID costCenterId, // Nullable: null means entire legal entity

        @Min(value = 1, message = "Approval level must be between 1 (Manager) and 4 (CFO/Board)")
        @Max(value = 4, message = "Approval level must be between 1 (Manager) and 4 (CFO/Board)")
        int approvalLevel,

        @DecimalMin(value = "0.0", inclusive = true, message = "Minimum amount must be non-negative")
        BigDecimal minAmount,

        BigDecimal maxAmount, // Nullable: null means unlimited signing authority (CFO level)

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g. TRY, USD, EUR)")
        String currency
) {}
