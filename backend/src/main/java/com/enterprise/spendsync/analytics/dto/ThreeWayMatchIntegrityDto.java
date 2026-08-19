package com.enterprise.spendsync.analytics.dto;

import java.math.BigDecimal;

public record ThreeWayMatchIntegrityDto(
        long totalInvoices,
        long matchedInvoices,
        long discrepancyHoldInvoices,
        double firstTimeMatchRatePercent,
        BigDecimal discrepancyBlockedAmount
) {}
