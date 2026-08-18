package com.enterprise.spendsync.receiving.internal.dto;

import com.enterprise.spendsync.receiving.internal.domain.GoodsReceipt;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceiptStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptResponse(
        UUID id,
        String receiptNumber,
        UUID purchaseOrderId,
        String poNumber,
        UUID vendorId,
        String vendorName,
        UUID deliveryFacilityId,
        String deliveryFacilityName,
        String waybillNumber,
        LocalDate waybillDate,
        UUID receivedByUserId,
        String receivedByUserName,
        GoodsReceiptStatus status,
        String notes,
        List<GRLineItemResponse> lineItems,
        Instant createdAt
) {
    public static GoodsReceiptResponse from(GoodsReceipt gr) {
        return new GoodsReceiptResponse(
                gr.getId(),
                gr.getReceiptNumber(),
                gr.getPurchaseOrder().getId(),
                gr.getPurchaseOrder().getPoNumber(),
                gr.getPurchaseOrder().getVendor().getId(),
                gr.getPurchaseOrder().getVendor().getName(),
                gr.getDeliveryFacility().getId(),
                gr.getDeliveryFacility().getName(),
                gr.getWaybillNumber(),
                gr.getWaybillDate(),
                gr.getReceivedByUser().getId(),
                gr.getReceivedByUser().getFirstName() + " " + gr.getReceivedByUser().getLastName(),
                gr.getStatus(),
                gr.getNotes(),
                gr.getLineItems().stream().map(GRLineItemResponse::from).toList(),
                gr.getCreatedAt()
        );
    }
}
