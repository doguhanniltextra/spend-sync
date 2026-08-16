package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.CostCenter;

import java.time.Instant;
import java.util.UUID;

public record CostCenterResponse(
        UUID id,
        UUID tenantId,
        UUID legalEntityId,
        String legalEntityName,
        String code,
        String name,
        UUID managerUserId,
        String managerFullName,
        boolean isActive,
        Instant createdAt
) {
    public static CostCenterResponse fromEntity(CostCenter costCenter) {
        return new CostCenterResponse(
                costCenter.getId(),
                costCenter.getTenant().getId(),
                costCenter.getLegalEntity().getId(),
                costCenter.getLegalEntity().getName(),
                costCenter.getCode(),
                costCenter.getName(),
                costCenter.getManagerUser() != null ? costCenter.getManagerUser().getId() : null,
                costCenter.getManagerUser() != null ? costCenter.getManagerUser().getFullName() : null,
                costCenter.isActive(),
                costCenter.getCreatedAt()
        );
    }
}
