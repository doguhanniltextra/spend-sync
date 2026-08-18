package com.enterprise.spendsync.purchasing.internal.repository;

import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorStatus;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    Optional<Vendor> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Vendor> findByTaxNumberAndTenantId(String taxNumber, UUID tenantId);

    boolean existsByTaxNumberAndTenantId(String taxNumber, UUID tenantId);

    List<Vendor> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Vendor> findAllByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, VendorStatus status);

    List<Vendor> findAllByTenantIdAndCategoryOrderByCreatedAtDesc(UUID tenantId, VendorCategory category);

    List<Vendor> findAllByTenantIdAndTierOrderByCreatedAtDesc(UUID tenantId, VendorTier tier);
}
