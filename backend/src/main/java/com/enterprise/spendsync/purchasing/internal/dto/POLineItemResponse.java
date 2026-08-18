package com.enterprise.spendsync.purchasing.internal.dto;

import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record POLineItemResponse(
        UUID id,
        UUID requisitionLineItemId,
        int lineNumber,
        String itemDescription,
        String itemCategory,
        BigDecimal quantity,
        String unitOfMeasure,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        BigDecimal overDeliveryTolerancePct,
        BigDecimal underDeliveryTolerancePct,
        LocalDate estimatedDeliveryDate
) {
    public static POLineItemResponse from(PurchaseOrderLineItem item) {
        return new POLineItemResponse(
                item.getId(),
                item.getRequisitionLineItem() != null ? item.getRequisitionLineItem().getId() : null,
                item.getLineNumber(),
                item.getItemDescription(),
                item.getItemCategory(),
                item.getQuantity(),
                item.getUnitOfMeasure(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getOverDeliveryTolerancePct(),
                item.getUnderDeliveryTolerancePct(),
                item.getEstimatedDeliveryDate()
        );
    }
}
