package com.enterprise.spendsync.requisition.internal.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RequisitionApprovedEvent(
        UUID eventId,
        UUID tenantId,
        UUID requisitionId,
        String requisitionNumber,
        UUID requisitionerId,
        UUID legalEntityId,
        UUID costCenterId,
        UUID deliveryFacilityId,
        BigDecimal totalAmount,
        String currency,
        String title,
        List<LineItemEventPayload> lineItems,
        Instant approvedAt
) {
    public static RequisitionApprovedEvent of(
            UUID tenantId,
            UUID requisitionId,
            String requisitionNumber,
            UUID requisitionerId,
            UUID legalEntityId,
            UUID costCenterId,
            UUID deliveryFacilityId,
            BigDecimal totalAmount,
            String currency,
            String title,
            List<LineItemEventPayload> lineItems
    ) {
        return new RequisitionApprovedEvent(
                UUID.randomUUID(),
                tenantId,
                requisitionId,
                requisitionNumber,
                requisitionerId,
                legalEntityId,
                costCenterId,
                deliveryFacilityId,
                totalAmount,
                currency,
                title,
                lineItems,
                Instant.now()
        );
    }
}
