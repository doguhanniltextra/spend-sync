package com.enterprise.spendsync.intelligence.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CashDiscountOpportunityDto(
        UUID invoiceId,
        String invoiceNumber,
        String supplierName,
        BigDecimal grossAmount,
        String currency,
        LocalDate invoiceDueDate,
        LocalDate earlyDiscountDeadline,
        BigDecimal discountPercent,
        BigDecimal potentialCashSavings,
        BigDecimal netPayableIfDiscounted,
        BigDecimal annualizedAprYield
) {}
