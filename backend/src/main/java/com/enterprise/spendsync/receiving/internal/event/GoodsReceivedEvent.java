package com.enterprise.spendsync.receiving.internal.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GoodsReceivedEvent(
        UUID tenantId,
        UUID goodsReceiptId,
        String receiptNumber,
        UUID purchaseOrderId,
        String poNumber,
        UUID deliveryFacilityId,
        String waybillNumber,
        UUID receivedByUserId,
        List<GRLineItemPayload> lineItems,
        Instant timestamp
) {}
