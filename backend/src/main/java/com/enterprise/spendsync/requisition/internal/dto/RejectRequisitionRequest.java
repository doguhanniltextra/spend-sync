package com.enterprise.spendsync.requisition.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequisitionRequest(
        @NotBlank(message = "Rejection reason is mandatory")
        String rejectionReason
) {}
