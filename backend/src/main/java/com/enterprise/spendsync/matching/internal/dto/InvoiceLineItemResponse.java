package com.enterprise.spendsync.matching.internal.dto;

import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoiceLineItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceLineItemResponse(
        UUID id,
        UUID purchaseOrderLineItemId,
        String itemDescription,
        UUID goodsReceiptLineItemId,
        BigDecimal invoicedQuantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal lineTotal,
        InvoiceMatchStatus matchStatus,
        String varianceReason,
        Instant createdAt
) {
    public static InvoiceLineItemResponse from(SupplierInvoiceLineItem item) {
        return new InvoiceLineItemResponse(
                item.getId(),
                item.getPurchaseOrderLineItem().getId(),
                item.getPurchaseOrderLineItem().getItemDescription(),
                item.getGoodsReceiptLineItem() != null ? item.getGoodsReceiptLineItem().getId() : null,
                item.getInvoicedQuantity(),
                item.getUnitPrice(),
                item.getTaxRate(),
                item.getTaxAmount(),
                item.getLineTotal(),
                item.getMatchStatus(),
                item.getVarianceReason(),
                item.getCreatedAt()
        );
    }
}
