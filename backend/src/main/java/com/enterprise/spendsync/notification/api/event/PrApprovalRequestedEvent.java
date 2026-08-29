package com.enterprise.spendsync.notification.api.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event dispatched when a Purchase Requisition enters a pending approval step
 * and requires action from an authorized approver.
 */
public record PrApprovalRequestedEvent(
        UUID tenantId,
        UUID requisitionId,
        String requisitionNumber,
        UUID requisitionerId,
        String requisitionerName,
        UUID approverId,
        int stepOrder,
        BigDecimal totalAmount,
        String currency,
        String title
) {
    public static PrApprovalRequestedEvent of(
            UUID tenantId,
            UUID requisitionId,
            String requisitionNumber,
            UUID requisitionerId,
            String requisitionerName,
            UUID approverId,
            int stepOrder,
            BigDecimal totalAmount,
            String currency,
            String title
    ) {
        return new PrApprovalRequestedEvent(
                tenantId,
                requisitionId,
                requisitionNumber,
                requisitionerId,
                requisitionerName,
                approverId,
                stepOrder,
                totalAmount,
                currency,
                title
        );
    }
}
