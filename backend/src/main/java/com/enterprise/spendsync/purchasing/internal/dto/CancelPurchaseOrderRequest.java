package com.enterprise.spendsync.purchasing.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelPurchaseOrderRequest(
        @NotBlank(message = "Cancellation reason is mandatory")
        String cancellationReason
) {}
