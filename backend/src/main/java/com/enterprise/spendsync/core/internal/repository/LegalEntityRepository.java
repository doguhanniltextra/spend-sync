package com.enterprise.spendsync.core.internal.repository;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalEntityRepository extends JpaRepository<LegalEntity, UUID> {
    List<LegalEntity> findAllByTenantId(UUID tenantId);
    Optional<LegalEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<LegalEntity> findByTenantIdAndCompanyCode(UUID tenantId, String companyCode);
    boolean existsByTenantIdAndTaxNumber(UUID tenantId, String taxNumber);
}
