package com.enterprise.spendsync.matching.internal.repository;

import com.enterprise.spendsync.matching.internal.domain.InvoiceDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceDiscrepancyRepository extends JpaRepository<InvoiceDiscrepancy, UUID> {

    List<InvoiceDiscrepancy> findAllByTenantIdAndSupplierInvoiceIdOrderByCreatedAtDesc(UUID tenantId, UUID supplierInvoiceId);

    List<InvoiceDiscrepancy> findAllByTenantIdAndResolvedFalseOrderByCreatedAtDesc(UUID tenantId);
}
