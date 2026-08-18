package com.enterprise.spendsync.matching.internal.event;

import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceMatchedEvent(
        UUID tenantId,
        UUID invoiceId,
        String invoiceNumber,
        String ettn,
        UUID purchaseOrderId,
        String poNumber,
        UUID vendorId,
        UUID budgetPoolId,
        BigDecimal totalAmount,
        String currency,
        InvoiceMatchStatus matchStatus,
        String discrepancyReason,
        Instant timestamp
) {}
