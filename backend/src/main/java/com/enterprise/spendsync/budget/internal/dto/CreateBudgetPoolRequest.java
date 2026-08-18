package com.enterprise.spendsync.budget.internal.dto;

import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateBudgetPoolRequest(
        @NotNull(message = "Cost Center ID is required")
        UUID costCenterId,

        @NotNull(message = "Legal Entity ID is required")
        UUID legalEntityId,

        @NotNull(message = "Fiscal Year is required")
        @Min(value = 2020, message = "Fiscal Year must be 2020 or later")
        int fiscalYear,

        BudgetPeriodType periodType,

        String periodValue,

        BudgetStatus status,

        BudgetEnforcementMode enforcementMode,

        BigDecimal tolerancePercentage,

        @NotNull(message = "Allocated amount is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Allocated amount must be non-negative")
        BigDecimal allocatedAmount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g. TRY, USD, EUR)")
        String currency
) {}
