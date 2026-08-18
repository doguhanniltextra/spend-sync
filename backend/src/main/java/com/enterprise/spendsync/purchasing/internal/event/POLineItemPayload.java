package com.enterprise.spendsync.purchasing.internal.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record POLineItemPayload(
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
) {}
