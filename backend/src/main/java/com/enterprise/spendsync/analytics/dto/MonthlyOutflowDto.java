package com.enterprise.spendsync.analytics.dto;

import java.math.BigDecimal;

public record MonthlyOutflowDto(
        String month,
        BigDecimal confirmedDueInvoices,
        BigDecimal projectedPoDeliveries,
        BigDecimal totalExpectedOutflow
) {}
