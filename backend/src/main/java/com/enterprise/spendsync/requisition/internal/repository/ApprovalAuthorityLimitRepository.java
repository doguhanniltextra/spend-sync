package com.enterprise.spendsync.requisition.internal.repository;

import com.enterprise.spendsync.requisition.internal.domain.ApprovalAuthorityLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalAuthorityLimitRepository extends JpaRepository<ApprovalAuthorityLimit, UUID> {

    Optional<ApprovalAuthorityLimit> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ApprovalAuthorityLimit> findByUserIdAndLegalEntityIdAndCostCenterIdAndTenantId(
            UUID userId,
            UUID legalEntityId,
            UUID costCenterId,
            UUID tenantId
    );

    Optional<ApprovalAuthorityLimit> findByUserIdAndLegalEntityIdAndCostCenterIsNullAndTenantId(
            UUID userId,
            UUID legalEntityId,
            UUID tenantId
    );

    List<ApprovalAuthorityLimit> findAllByTenantId(UUID tenantId);

    List<ApprovalAuthorityLimit> findAllByLegalEntityIdAndCostCenterIdAndApprovalLevelAndTenantId(
            UUID legalEntityId,
            UUID costCenterId,
            int approvalLevel,
            UUID tenantId
    );

    List<ApprovalAuthorityLimit> findAllByLegalEntityIdAndCostCenterIsNullAndApprovalLevelAndTenantId(
            UUID legalEntityId,
            int approvalLevel,
            UUID tenantId
    );

    List<ApprovalAuthorityLimit> findAllByTenantIdAndLegalEntityId(UUID tenantId, UUID legalEntityId);

    List<ApprovalAuthorityLimit> findAllByTenantIdAndUserId(UUID tenantId, UUID userId);

    /**
     * Resolves effective limits ordered by specificity:
     * 1. Cost-center specific limit (if matches costCenterId)
     * 2. Legal-entity wide limit (costCenterId IS NULL)
     */
    @Query("""
        SELECT aal FROM ApprovalAuthorityLimit aal
        WHERE aal.tenant.id = :tenantId
          AND aal.user.id = :userId
          AND aal.legalEntity.id = :legalEntityId
          AND aal.isActive = true
          AND (aal.costCenter.id = :costCenterId OR aal.costCenter IS NULL)
        ORDER BY CASE WHEN aal.costCenter IS NOT NULL THEN 0 ELSE 1 END ASC
    """)
    List<ApprovalAuthorityLimit> findEffectiveLimits(
            @Param("userId") UUID userId,
            @Param("legalEntityId") UUID legalEntityId,
            @Param("costCenterId") UUID costCenterId,
            @Param("tenantId") UUID tenantId
    );
}
