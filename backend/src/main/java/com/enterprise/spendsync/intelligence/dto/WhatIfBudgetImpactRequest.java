package com.enterprise.spendsync.intelligence.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record WhatIfBudgetImpactRequest(
        @NotNull(message = "costCenterId is required")
        UUID costCenterId,

        @NotNull(message = "proposedAmount is required")
        @Positive(message = "proposedAmount must be positive")
        BigDecimal proposedAmount
) {}
