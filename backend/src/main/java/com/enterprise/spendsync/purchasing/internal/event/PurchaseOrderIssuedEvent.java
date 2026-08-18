package com.enterprise.spendsync.purchasing.internal.event;

import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderIssuedEvent(
        UUID eventId,
        UUID tenantId,
        UUID poId,
        String poNumber,
        int revisionNumber,
        UUID requisitionId,
        UUID legalEntityId,
        UUID costCenterId,
        UUID deliveryFacilityId,
        UUID vendorId,
        String vendorName,
        String vendorOrderEmail,
        Incoterms incoterms,
        BigDecimal totalAmount,
        String currency,
        List<POLineItemPayload> lineItems,
        Instant issuedAt
) {
    public static PurchaseOrderIssuedEvent of(
            UUID tenantId,
            UUID poId,
            String poNumber,
            int revisionNumber,
            UUID requisitionId,
            UUID legalEntityId,
            UUID costCenterId,
            UUID deliveryFacilityId,
            UUID vendorId,
            String vendorName,
            String vendorOrderEmail,
            Incoterms incoterms,
            BigDecimal totalAmount,
            String currency,
            List<POLineItemPayload> lineItems
    ) {
        return new PurchaseOrderIssuedEvent(
                UUID.randomUUID(),
                tenantId,
                poId,
                poNumber,
                revisionNumber,
                requisitionId,
                legalEntityId,
                costCenterId,
                deliveryFacilityId,
                vendorId,
                vendorName,
                vendorOrderEmail,
                incoterms,
                totalAmount,
                currency,
                lineItems,
                Instant.now()
        );
    }
}
