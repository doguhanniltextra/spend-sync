package com.enterprise.spendsync.receiving.internal.dto;

import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PendingPOForReceivingResponse(
        UUID id,
        String poNumber,
        UUID vendorId,
        String vendorName,
        UUID deliveryFacilityId,
        String deliveryFacilityName,
        PurchaseOrderStatus status,
        Incoterms incoterms,
        BigDecimal totalAmount,
        String currency,
        int lineItemCount,
        Instant issuedAt
) {
    public static PendingPOForReceivingResponse from(PurchaseOrder po) {
        return new PendingPOForReceivingResponse(
                po.getId(),
                po.getPoNumber(),
                po.getVendor().getId(),
                po.getVendor().getName(),
                po.getDeliveryFacility().getId(),
                po.getDeliveryFacility().getName(),
                po.getStatus(),
                po.getIncoterms(),
                po.getTotalAmount(),
                po.getCurrency(),
                po.getLineItems().size(),
                po.getIssuedAt()
        );
    }
}
