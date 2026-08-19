package com.enterprise.spendsync.vendorportal.internal.repository;

import com.enterprise.spendsync.vendorportal.internal.domain.EarlyPayOfferStatus;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorEarlyPayOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorEarlyPayOfferRepository extends JpaRepository<VendorEarlyPayOffer, UUID> {

    List<VendorEarlyPayOffer> findAllByTenantIdAndVendorIdAndStatusOrderByCreatedAtDesc(UUID tenantId, UUID vendorId, EarlyPayOfferStatus status);

    List<VendorEarlyPayOffer> findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(UUID tenantId, UUID vendorId);

    Optional<VendorEarlyPayOffer> findByTenantIdAndSupplierInvoiceIdAndStatus(UUID tenantId, UUID supplierInvoiceId, EarlyPayOfferStatus status);

    Optional<VendorEarlyPayOffer> findByTenantIdAndSupplierInvoiceId(UUID tenantId, UUID supplierInvoiceId);
}
