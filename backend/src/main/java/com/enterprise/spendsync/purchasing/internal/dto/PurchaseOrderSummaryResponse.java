package com.enterprise.spendsync.purchasing.internal.dto;

import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseOrderSummaryResponse(
        UUID id,
        String poNumber,
        int revisionNumber,
        String requisitionNumber,
        String legalEntityName,
        String costCenterName,
        String deliveryFacilityName,
        String vendorName,
        PurchaseOrderStatus status,
        Incoterms incoterms,
        String currency,
        BigDecimal totalAmount,
        int lineItemCount,
        boolean isCrossEntity,
        Instant issuedAt,
        Instant createdAt
) {
    public static PurchaseOrderSummaryResponse from(PurchaseOrder po, boolean isCrossEntity) {
        return new PurchaseOrderSummaryResponse(
                po.getId(),
                po.getPoNumber(),
                po.getRevisionNumber(),
                po.getRequisition() != null ? po.getRequisition().getRequisitionNumber() : null,
                po.getLegalEntity().getName(),
                po.getCostCenter().getName(),
                po.getDeliveryFacility().getName(),
                po.getVendor().getName(),
                po.getStatus(),
                po.getIncoterms(),
                po.getCurrency(),
                po.getTotalAmount(),
                po.getLineItems().size(),
                isCrossEntity,
                po.getIssuedAt(),
                po.getCreatedAt()
        );
    }
}
