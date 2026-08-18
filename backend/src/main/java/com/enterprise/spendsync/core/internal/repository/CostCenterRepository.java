package com.enterprise.spendsync.core.internal.repository;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, UUID> {
    List<CostCenter> findAllByTenantId(UUID tenantId);
    Optional<CostCenter> findByIdAndTenantId(UUID id, UUID tenantId);
    List<CostCenter> findAllByLegalEntityId(UUID legalEntityId);
    Optional<CostCenter> findByTenantIdAndCode(UUID tenantId, String code);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
