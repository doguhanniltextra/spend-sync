package com.enterprise.spendsync.payment.internal.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentDispatchedEvent(
        UUID tenantId,
        UUID paymentBatchId,
        String batchNumber,
        UUID legalEntityId,
        BigDecimal totalAmount,
        String currency,
        int itemCount,
        UUID approvedByUserId,
        List<PaymentItemPayload> items,
        Instant timestamp
) {}
