package com.enterprise.spendsync.matching.internal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateInvoiceLineItemRequest(
        @NotNull(message = "PO line item ID is mandatory")
        UUID purchaseOrderLineItemId,

        UUID goodsReceiptLineItemId,

        @NotNull(message = "Invoiced quantity is mandatory")
        @DecimalMin(value = "0.0001", message = "Invoiced quantity must be greater than zero")
        BigDecimal invoicedQuantity,

        @NotNull(message = "Unit price is mandatory")
        @DecimalMin(value = "0.0000", message = "Unit price cannot be negative")
        BigDecimal unitPrice,

        BigDecimal taxRate
) {}
