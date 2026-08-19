package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AcceptEarlyDiscountResponse(
        UUID invoiceId,
        String invoiceNumber,
        String status,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal netPayoutAmount,
        LocalDate scheduledPaymentDate,
        String message
) {}
