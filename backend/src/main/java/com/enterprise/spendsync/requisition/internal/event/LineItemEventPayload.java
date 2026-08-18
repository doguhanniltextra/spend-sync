package com.enterprise.spendsync.requisition.internal.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LineItemEventPayload(
        int lineNumber,
        String itemDescription,
        String itemCategory,
        BigDecimal quantity,
        String unitOfMeasure,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        LocalDate estimatedDeliveryDate
) {}
