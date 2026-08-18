package com.enterprise.spendsync.requisition.internal.dto;

import com.enterprise.spendsync.requisition.internal.domain.RequisitionLineItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LineItemResponse(
        UUID id,
        int lineNumber,
        String itemDescription,
        String itemCategory,
        BigDecimal quantity,
        String unitOfMeasure,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        LocalDate estimatedDeliveryDate
) {
    public static LineItemResponse from(RequisitionLineItem item) {
        return new LineItemResponse(
                item.getId(),
                item.getLineNumber(),
                item.getItemDescription(),
                item.getItemCategory(),
                item.getQuantity(),
                item.getUnitOfMeasure(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getEstimatedDeliveryDate()
        );
    }
}
