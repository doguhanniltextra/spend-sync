package com.enterprise.spendsync.payment.internal.dto;

import com.enterprise.spendsync.payment.internal.domain.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreatePaymentBatchRequest(
        @NotNull(message = "Legal Entity ID is mandatory")
        UUID legalEntityId,

        PaymentMethod paymentMethod,

        String idempotencyKey,

        @NotEmpty(message = "At least one invoice must be included in the batch")
        List<UUID> invoiceIds
) {}
