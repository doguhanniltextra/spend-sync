package com.enterprise.spendsync.matching.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectInvoiceRequest(
        @NotBlank(message = "Rejection reason is mandatory")
        String rejectionReason
) {}
