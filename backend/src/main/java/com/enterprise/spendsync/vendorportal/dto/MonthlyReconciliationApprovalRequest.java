package com.enterprise.spendsync.vendorportal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MonthlyReconciliationApprovalRequest(
        @NotNull(message = "Year is required")
        @Min(2020)
        @Max(2050)
        Integer year,

        @NotNull(message = "Month is required")
        @Min(1)
        @Max(12)
        Integer month,

        String notes,

        boolean disputed
) {}
