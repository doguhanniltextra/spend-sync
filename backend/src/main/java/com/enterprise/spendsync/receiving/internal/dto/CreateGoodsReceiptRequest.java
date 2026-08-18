package com.enterprise.spendsync.receiving.internal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateGoodsReceiptRequest(
        @NotNull(message = "Purchase order ID is mandatory")
        UUID purchaseOrderId,

        @NotBlank(message = "Waybill number is mandatory")
        String waybillNumber,

        @NotNull(message = "Waybill date is mandatory")
        LocalDate waybillDate,

        UUID deliveryFacilityId,

        String notes,

        @NotEmpty(message = "At least one line item must be received")
        @Valid
        List<CreateGRLineItemRequest> lineItems
) {}
