package com.enterprise.spendsync.receiving.internal.dto;

import com.enterprise.spendsync.receiving.internal.domain.GoodsReceiptLineItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GRLineItemResponse(
        UUID id,
        UUID purchaseOrderLineItemId,
        String itemDescription,
        String itemCategory,
        String unitOfMeasure,
        BigDecimal orderedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal acceptedQuantity,
        BigDecimal rejectedQuantity,
        String rejectionReason,
        String notes,
        Instant createdAt
) {
    public static GRLineItemResponse from(GoodsReceiptLineItem item) {
        return new GRLineItemResponse(
                item.getId(),
                item.getPurchaseOrderLineItem().getId(),
                item.getPurchaseOrderLineItem().getItemDescription(),
                item.getPurchaseOrderLineItem().getItemCategory(),
                item.getPurchaseOrderLineItem().getUnitOfMeasure(),
                item.getPurchaseOrderLineItem().getQuantity(),
                item.getReceivedQuantity(),
                item.getAcceptedQuantity(),
                item.getRejectedQuantity(),
                item.getRejectionReason(),
                item.getNotes(),
                item.getCreatedAt()
        );
    }
}
