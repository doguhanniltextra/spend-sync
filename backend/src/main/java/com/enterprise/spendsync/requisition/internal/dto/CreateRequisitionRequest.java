package com.enterprise.spendsync.requisition.internal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateRequisitionRequest(
        @NotNull(message = "Legal Entity ID is required")
        UUID legalEntityId,

        @NotNull(message = "Cost Center ID is required")
        UUID costCenterId,

        @NotNull(message = "Delivery Facility ID is required")
        UUID deliveryFacilityId,

        @NotBlank(message = "Requisition title is required")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        @NotBlank(message = "Justification is required")
        String justification,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g. TRY, USD, EUR)")
        String currency,

        @NotEmpty(message = "Requisition must contain at least one line item")
        @Valid
        List<CreateLineItemRequest> lineItems
) {}
