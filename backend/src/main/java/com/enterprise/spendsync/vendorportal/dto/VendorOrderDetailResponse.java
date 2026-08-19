package com.enterprise.spendsync.vendorportal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VendorOrderDetailResponse(
        UUID id,
        String poNumber,
        BigDecimal totalAmount,
        String currency,
        String status,
        String incoterms,
        String paymentTerms,
        String notes,
        DeliveryFacilityDto deliveryFacility,
        List<LineItemDto> lineItems,
        AcknowledgmentDto latestAcknowledgment,
        List<VendorAsnResponse> asnShipments,
        Instant issuedAt,
        Instant createdAt
) {
    public record DeliveryFacilityDto(
            UUID id,
            String name,
            String code,
            String shippingAddress
    ) {}

    public record LineItemDto(
            UUID id,
            Integer lineNumber,
            String itemDescription,
            String itemCategory,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            LocalDate estimatedDeliveryDate
    ) {}

    public record AcknowledgmentDto(
            UUID id,
            String status,
            LocalDate promisedDeliveryDate,
            String vendorNotes,
            String acknowledgedByName,
            Instant createdAt
    ) {}
}
