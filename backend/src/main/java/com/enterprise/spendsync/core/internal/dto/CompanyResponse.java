package com.enterprise.spendsync.core.internal.dto;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;

import java.time.Instant;
import java.util.UUID;

/**
 * Public response DTO returning the created Tenant and default Legal Entity details.
 */
public record CompanyResponse(
        UUID tenantId,
        String tenantName,
        String slug,
        String subscriptionTier,
        UUID legalEntityId,
        String legalEntityName,
        String companyCode,
        String taxNumber,
        String taxOffice,
        String baseCurrency,
        String country,
        UUID ownerUserId,
        Instant createdAt
) {
    public static CompanyResponse fromEntities(Tenant tenant, LegalEntity legalEntity, UUID ownerUserId) {
        return new CompanyResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getSubscriptionTier(),
                legalEntity.getId(),
                legalEntity.getName(),
                legalEntity.getCompanyCode(),
                legalEntity.getTaxNumber(),
                legalEntity.getTaxOffice(),
                legalEntity.getBaseCurrency(),
                legalEntity.getCountry(),
                ownerUserId,
                tenant.getCreatedAt()
        );
    }
}
