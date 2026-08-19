package com.enterprise.spendsync.vendorportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VendorAsnDispatchRequest(
        @NotBlank(message = "Waybill number / ASN number is required")
        @Size(max = 100)
        String waybillNumber,

        @Size(max = 100)
        String ettn,

        @Size(max = 100)
        String carrierName,

        @Size(max = 100)
        String trackingNumber,

        @Size(max = 50)
        String vehiclePlate,

        @Size(max = 50)
        String driverNationalId,

        @Size(max = 150)
        String driverName,

        @Size(max = 50)
        String driverPhone,

        @NotNull(message = "Shipment date is required")
        LocalDate shipmentDate,

        @NotNull(message = "Estimated arrival date is required")
        LocalDate estimatedArrivalDate,

        String notes,

        List<AsnLineItemDispatchDto> lineItems
) {
    public record AsnLineItemDispatchDto(
            @NotNull(message = "Purchase order line item ID is required")
            UUID purchaseOrderLineItemId,

            @NotNull(message = "Shipped quantity is required")
            BigDecimal shippedQuantity,

            String lotNumber,
            String serialNumbers
    ) {}
}
