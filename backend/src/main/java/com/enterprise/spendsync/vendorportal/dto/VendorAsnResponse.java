package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VendorAsnResponse(
        UUID id,
        UUID purchaseOrderId,
        String poNumber,
        String waybillNumber,
        String ettn,
        String carrierName,
        String trackingNumber,
        String vehiclePlate,
        String driverName,
        String maskedDriverNationalId,
        String driverPhone,
        LocalDate shipmentDate,
        LocalDate estimatedArrivalDate,
        String status,
        String notes,
        String dispatchedByUserName,
        List<AsnLineItemResponse> lineItems,
        Instant createdAt
) {
    public record AsnLineItemResponse(
            UUID id,
            UUID purchaseOrderLineItemId,
            Integer lineNumber,
            String itemDescription,
            BigDecimal shippedQuantity,
            String unitOfMeasure,
            String lotNumber,
            String serialNumbers
    ) {}
}
