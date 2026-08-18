package com.enterprise.spendsync.purchasing.internal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RevisePurchaseOrderRequest(
        @NotBlank(message = "Revision reason is mandatory")
        String reason,

        @NotEmpty(message = "Line items cannot be empty")
        @Valid
        List<RevisePOLineItemRequest> lineItems
) {}
