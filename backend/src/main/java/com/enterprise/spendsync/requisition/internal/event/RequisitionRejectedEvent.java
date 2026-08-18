package com.enterprise.spendsync.requisition.internal.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RequisitionRejectedEvent(
        UUID eventId,
        UUID tenantId,
        UUID requisitionId,
        String requisitionNumber,
        UUID rejectedByUserId,
        String rejectionReason,
        BigDecimal releasedBudgetAmount,
        Instant rejectedAt
) {
    public static RequisitionRejectedEvent of(
            UUID tenantId,
            UUID requisitionId,
            String requisitionNumber,
            UUID rejectedByUserId,
            String rejectionReason,
            BigDecimal releasedBudgetAmount
    ) {
        return new RequisitionRejectedEvent(
                UUID.randomUUID(),
                tenantId,
                requisitionId,
                requisitionNumber,
                rejectedByUserId,
                rejectionReason,
                releasedBudgetAmount,
                Instant.now()
        );
    }
}
