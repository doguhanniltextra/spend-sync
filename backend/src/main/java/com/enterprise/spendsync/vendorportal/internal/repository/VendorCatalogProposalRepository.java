package com.enterprise.spendsync.vendorportal.internal.repository;

import com.enterprise.spendsync.vendorportal.internal.domain.VendorCatalogProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorCatalogProposalRepository extends JpaRepository<VendorCatalogProposal, UUID> {

    List<VendorCatalogProposal> findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(UUID tenantId, UUID vendorId);

    List<VendorCatalogProposal> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
