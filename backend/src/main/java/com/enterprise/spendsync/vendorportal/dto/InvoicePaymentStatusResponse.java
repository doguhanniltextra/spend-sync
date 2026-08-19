package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoicePaymentStatusResponse(
        UUID invoiceId,
        String invoiceNumber,
        String status,
        String matchStatus,
        BigDecimal payableAmount,
        String currency,
        LocalDate originalDueDate,
        LocalDate paymentExecutionDate,
        String bankReferenceNumber,
        String maskedIban,
        List<PaymentTimelineStepDto> timeline
) {
    public record PaymentTimelineStepDto(
            String step,
            String title,
            String description,
            boolean completed,
            Instant timestamp
    ) {}
}
