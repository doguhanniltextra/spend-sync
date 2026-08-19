package com.enterprise.spendsync.vendorportal.internal.repository;

import com.enterprise.spendsync.vendorportal.internal.domain.VendorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorUserRepository extends JpaRepository<VendorUser, UUID> {

    Optional<VendorUser> findByEmail(String email);

    Optional<VendorUser> findByTenantIdAndEmail(UUID tenantId, String email);

    List<VendorUser> findAllByTenantIdAndVendorId(UUID tenantId, UUID vendorId);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}
