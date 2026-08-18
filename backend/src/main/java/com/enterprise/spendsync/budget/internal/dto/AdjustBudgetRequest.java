package com.enterprise.spendsync.budget.internal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustBudgetRequest(
        @NotNull(message = "New allocated amount is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Allocated amount must be non-negative")
        BigDecimal newAllocatedAmount,

        @NotBlank(message = "Adjustment reason is required")
        String reason
) {}
