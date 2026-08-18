package com.enterprise.spendsync.purchasing.internal.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseOrderRevisedEvent(
        UUID eventId,
        UUID tenantId,
        UUID poId,
        String poNumber,
        int revisionNumber,
        BigDecimal previousTotalAmount,
        BigDecimal newTotalAmount,
        BigDecimal differentialAmount,
        String reason,
        Instant revisedAt
) {
    public static PurchaseOrderRevisedEvent of(
            UUID tenantId,
            UUID poId,
            String poNumber,
            int revisionNumber,
            BigDecimal previousTotalAmount,
            BigDecimal newTotalAmount,
            BigDecimal differentialAmount,
            String reason
    ) {
        return new PurchaseOrderRevisedEvent(
                UUID.randomUUID(),
                tenantId,
                poId,
                poNumber,
                revisionNumber,
                previousTotalAmount,
                newTotalAmount,
                differentialAmount,
                reason,
                Instant.now()
        );
    }
}
