package com.enterprise.spendsync.purchasing.internal.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseOrderCancelledEvent(
        UUID eventId,
        UUID tenantId,
        UUID poId,
        String poNumber,
        UUID cancelledByUserId,
        String cancellationReason,
        BigDecimal releasedBudgetAmount,
        Instant cancelledAt
) {
    public static PurchaseOrderCancelledEvent of(
            UUID tenantId,
            UUID poId,
            String poNumber,
            UUID cancelledByUserId,
            String cancellationReason,
            BigDecimal releasedBudgetAmount
    ) {
        return new PurchaseOrderCancelledEvent(
                UUID.randomUUID(),
                tenantId,
                poId,
                poNumber,
                cancelledByUserId,
                cancellationReason,
                releasedBudgetAmount,
                Instant.now()
        );
    }
}
