package com.enterprise.spendsync.matching.internal.repository;

import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, UUID> {

    Optional<SupplierInvoice> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<SupplierInvoice> findByTenantIdAndEttn(UUID tenantId, String ettn);

    Optional<SupplierInvoice> findByTenantIdAndVendorIdAndInvoiceNumber(UUID tenantId, UUID vendorId, String invoiceNumber);

    List<SupplierInvoice> findAllByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(UUID tenantId, UUID purchaseOrderId);

    List<SupplierInvoice> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
