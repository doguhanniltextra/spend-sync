package com.enterprise.spendsync.purchasing.internal.dto;

import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderRevision;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PORevisionResponse(
        UUID id,
        int revisionNumber,
        BigDecimal previousTotalAmount,
        BigDecimal newTotalAmount,
        BigDecimal differentialAmount,
        String reason,
        UUID revisedByUserId,
        String revisedByUserName,
        String snapshotPayload,
        Instant createdAt
) {
    public static PORevisionResponse from(PurchaseOrderRevision rev) {
        String userName = rev.getRevisedByUser() != null
                ? rev.getRevisedByUser().getFirstName() + " " + rev.getRevisedByUser().getLastName()
                : "System";

        return new PORevisionResponse(
                rev.getId(),
                rev.getRevisionNumber(),
                rev.getPreviousTotalAmount(),
                rev.getNewTotalAmount(),
                rev.getDifferentialAmount(),
                rev.getReason(),
                rev.getRevisedByUser() != null ? rev.getRevisedByUser().getId() : null,
                userName,
                rev.getSnapshotPayload(),
                rev.getCreatedAt()
        );
    }
}
