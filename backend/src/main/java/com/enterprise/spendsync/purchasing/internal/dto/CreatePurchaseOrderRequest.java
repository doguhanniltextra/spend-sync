package com.enterprise.spendsync.purchasing.internal.dto;

import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreatePurchaseOrderRequest(
        UUID requisitionId,

        @NotNull(message = "Legal Entity is mandatory")
        UUID legalEntityId,

        @NotNull(message = "Cost Center is mandatory")
        UUID costCenterId,

        @NotNull(message = "Delivery Facility is mandatory")
        UUID deliveryFacilityId,

        @NotNull(message = "Vendor is mandatory")
        UUID vendorId,

        Incoterms incoterms,

        PaymentTerms paymentTerms,

        String currency,

        String notes,

        @NotEmpty(message = "At least one line item is required")
        @Valid
        List<POLineItemRequest> lineItems
) {}
