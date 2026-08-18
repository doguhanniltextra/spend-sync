package com.enterprise.spendsync.requisition.internal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLineItemRequest(
        @NotBlank(message = "Item description is required")
        String itemDescription,

        @NotBlank(message = "Item category is required")
        String itemCategory,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotBlank(message = "Unit of measure is required (e.g. PIECE, BOX, HOUR)")
        String unitOfMeasure,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
        BigDecimal unitPrice,

        LocalDate estimatedDeliveryDate
) {}
