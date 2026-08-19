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

    List<SupplierInvoice> findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(UUID tenantId, UUID vendorId);

    List<SupplierInvoice> findAllByTenantIdAndVendorIdAndStatusOrderByCreatedAtDesc(UUID tenantId, UUID vendorId, com.enterprise.spendsync.matching.internal.domain.InvoiceStatus status);

    Optional<SupplierInvoice> findByIdAndTenantIdAndVendorId(UUID id, UUID tenantId, UUID vendorId);

    boolean existsByTenantIdAndEttn(UUID tenantId, String ettn);

    boolean existsByTenantIdAndVendorIdAndInvoiceNumber(UUID tenantId, UUID vendorId, String invoiceNumber);
}
