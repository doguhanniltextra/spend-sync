package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EarlyPayOfferResponse(
        UUID offerId,
        UUID invoiceId,
        String invoiceNumber,
        BigDecimal originalAmount,
        String currency,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        BigDecimal netPayoutAmount,
        LocalDate originalDueDate,
        LocalDate acceleratedPaymentDate,
        String status
) {}
