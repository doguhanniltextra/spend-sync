package com.enterprise.spendsync.payment.internal.dto;

import com.enterprise.spendsync.payment.internal.domain.PaymentBatch;
import com.enterprise.spendsync.payment.internal.domain.PaymentBatchStatus;
import com.enterprise.spendsync.payment.internal.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentBatchResponse(
        UUID id,
        String batchNumber,
        UUID legalEntityId,
        String legalEntityName,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount,
        String currency,
        int itemCount,
        PaymentBatchStatus status,
        UUID createdByUserId,
        String createdByUserName,
        UUID approvedByUserId,
        String approvedByUserName,
        Instant approvedAt,
        String xmlPayload,
        String idempotencyKey,
        List<PaymentBatchItemResponse> items,
        Instant createdAt
) {
    public static PaymentBatchResponse from(PaymentBatch batch) {
        return new PaymentBatchResponse(
                batch.getId(),
                batch.getBatchNumber(),
                batch.getLegalEntity().getId(),
                batch.getLegalEntity().getName(),
                batch.getPaymentMethod(),
                batch.getTotalAmount(),
                batch.getCurrency(),
                batch.getItemCount(),
                batch.getStatus(),
                batch.getCreatedByUser().getId(),
                batch.getCreatedByUser().getFirstName() + " " + batch.getCreatedByUser().getLastName(),
                batch.getApprovedByUser() != null ? batch.getApprovedByUser().getId() : null,
                batch.getApprovedByUser() != null ? batch.getApprovedByUser().getFirstName() + " " + batch.getApprovedByUser().getLastName() : null,
                batch.getApprovedAt(),
                batch.getXmlPayload(),
                batch.getIdempotencyKey(),
                batch.getLineItems().stream().map(PaymentBatchItemResponse::from).toList(),
                batch.getCreatedAt()
        );
    }
}
