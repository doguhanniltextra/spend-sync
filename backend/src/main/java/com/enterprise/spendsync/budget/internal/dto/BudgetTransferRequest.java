package com.enterprise.spendsync.budget.internal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetTransferRequest(
        @NotNull(message = "Source Budget Pool ID is required")
        UUID sourceBudgetPoolId,

        @NotNull(message = "Target Budget Pool ID is required")
        UUID targetBudgetPoolId,

        @NotNull(message = "Transfer amount is required")
        @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Transfer reason is required")
        String reason
) {}
