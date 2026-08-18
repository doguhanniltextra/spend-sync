package com.enterprise.spendsync.purchasing.internal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record POLineItemRequest(
        UUID requisitionLineItemId,

        @NotBlank(message = "Item description is mandatory")
        String itemDescription,

        String itemCategory,

        @NotNull(message = "Quantity is mandatory")
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        String unitOfMeasure,

        @NotNull(message = "Unit price is mandatory")
        @DecimalMin(value = "0.0000", message = "Unit price must be non-negative")
        BigDecimal unitPrice,

        BigDecimal overDeliveryTolerancePct,
        BigDecimal underDeliveryTolerancePct,
        LocalDate estimatedDeliveryDate
) {}
