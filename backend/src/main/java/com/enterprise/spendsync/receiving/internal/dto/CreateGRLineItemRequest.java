package com.enterprise.spendsync.receiving.internal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateGRLineItemRequest(
        @NotNull(message = "Purchase order line item ID is mandatory")
        UUID purchaseOrderLineItemId,

        @NotNull(message = "Received quantity is mandatory")
        @DecimalMin(value = "0.0001", message = "Received quantity must be greater than zero")
        BigDecimal receivedQuantity,

        @NotNull(message = "Accepted quantity is mandatory")
        @DecimalMin(value = "0.0000", message = "Accepted quantity cannot be negative")
        BigDecimal acceptedQuantity,

        BigDecimal rejectedQuantity,

        String rejectionReason,

        String notes
) {}
