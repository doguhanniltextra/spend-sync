package com.enterprise.spendsync.core.internal.repository;

import com.enterprise.spendsync.core.internal.domain.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, UUID> {
    List<Facility> findAllByTenantId(UUID tenantId);
    Optional<Facility> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Facility> findAllByLegalEntityId(UUID legalEntityId);
    Optional<Facility> findByTenantIdAndFacilityCode(UUID tenantId, String facilityCode);
    boolean existsByTenantIdAndFacilityCode(UUID tenantId, String facilityCode);
}
