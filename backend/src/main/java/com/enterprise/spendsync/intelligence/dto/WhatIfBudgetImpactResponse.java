package com.enterprise.spendsync.intelligence.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WhatIfBudgetImpactResponse(
        UUID costCenterId,
        String costCenterName,
        BigDecimal allocatedBudget,
        BigDecimal currentCommitted,
        BigDecimal currentUtilizationPercent,
        BigDecimal proposedAmount,
        BigDecimal simulatedCommitted,
        BigDecimal simulatedUtilizationPercent,
        BigDecimal marginalIncreasePercent,
        boolean causesOverrun,
        boolean exceedsWarningThreshold,
        String riskAssessmentMessage
) {}
