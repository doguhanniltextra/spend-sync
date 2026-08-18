package com.enterprise.spendsync.budget.internal.repository;

import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetPoolRepository extends JpaRepository<BudgetPool, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bp FROM BudgetPool bp WHERE bp.id = :id AND bp.tenant.id = :tenantId")
    Optional<BudgetPool> findByIdAndTenantIdWithLock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    Optional<BudgetPool> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<BudgetPool> findByCostCenterIdAndFiscalYearAndPeriodTypeAndPeriodValueAndTenantId(
            UUID costCenterId,
            int fiscalYear,
            BudgetPeriodType periodType,
            String periodValue,
            UUID tenantId
    );

    Optional<BudgetPool> findByCostCenterIdAndLegalEntityIdAndStatusAndTenantId(
            UUID costCenterId,
            UUID legalEntityId,
            BudgetStatus status,
            UUID tenantId
    );

    Optional<BudgetPool> findByLegalEntityIdAndCostCenterIsNullAndStatusAndTenantId(
            UUID legalEntityId,
            BudgetStatus status,
            UUID tenantId
    );

    List<BudgetPool> findAllByTenantIdAndFiscalYear(UUID tenantId, int fiscalYear);

    List<BudgetPool> findAllByTenantId(UUID tenantId);

    List<BudgetPool> findAllByTenantIdAndStatus(UUID tenantId, BudgetStatus status);
}
