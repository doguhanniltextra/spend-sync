package com.enterprise.spendsync.receiving.internal.event;

import java.math.BigDecimal;
import java.util.UUID;

public record GRLineItemPayload(
        UUID poLineItemId,
        String itemDescription,
        BigDecimal receivedQuantity,
        BigDecimal acceptedQuantity,
        BigDecimal rejectedQuantity,
        String rejectionReason
) {}
