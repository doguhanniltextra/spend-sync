package com.enterprise.spendsync.budget.internal.dto;

import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBudgetStatusRequest(
        @NotNull(message = "New budget status is required")
        BudgetStatus status
) {}
