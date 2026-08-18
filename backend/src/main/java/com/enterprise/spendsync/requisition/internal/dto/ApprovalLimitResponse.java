package com.enterprise.spendsync.requisition.internal.dto;

import com.enterprise.spendsync.requisition.internal.domain.ApprovalAuthorityLimit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApprovalLimitResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userFullName,
        String userEmail,
        UUID legalEntityId,
        String legalEntityName,
        UUID costCenterId,
        String costCenterName,
        int approvalLevel,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        boolean isUnlimited,
        String currency,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApprovalLimitResponse from(ApprovalAuthorityLimit limit) {
        String costCenterName = limit.getCostCenter() != null ? limit.getCostCenter().getName() : null;
        UUID costCenterId = limit.getCostCenter() != null ? limit.getCostCenter().getId() : null;
        String userFullName = limit.getUser().getFirstName() + " " + limit.getUser().getLastName();

        return new ApprovalLimitResponse(
                limit.getId(),
                limit.getTenant().getId(),
                limit.getUser().getId(),
                userFullName,
                limit.getUser().getEmail(),
                limit.getLegalEntity().getId(),
                limit.getLegalEntity().getName(),
                costCenterId,
                costCenterName,
                limit.getApprovalLevel(),
                limit.getMinAmount(),
                limit.getMaxAmount(),
                limit.isUnlimited(),
                limit.getCurrency(),
                limit.isActive(),
                limit.getCreatedAt(),
                limit.getUpdatedAt()
        );
    }
}
