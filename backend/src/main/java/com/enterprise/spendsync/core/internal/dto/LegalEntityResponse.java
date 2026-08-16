package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;

import java.time.Instant;
import java.util.UUID;

public record LegalEntityResponse(
        UUID id,
        UUID tenantId,
        String name,
        String companyCode,
        String taxNumber,
        String taxOffice,
        String baseCurrency,
        String registeredAddress,
        String country,
        boolean isActive,
        Instant createdAt
) {
    public static LegalEntityResponse fromEntity(LegalEntity entity) {
        return new LegalEntityResponse(
                entity.getId(),
                entity.getTenant().getId(),
                entity.getName(),
                entity.getCompanyCode(),
                entity.getTaxNumber(),
                entity.getTaxOffice(),
                entity.getBaseCurrency(),
                entity.getRegisteredAddress(),
                entity.getCountry(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
