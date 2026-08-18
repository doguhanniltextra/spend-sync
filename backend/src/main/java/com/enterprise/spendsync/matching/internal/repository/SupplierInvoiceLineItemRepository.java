package com.enterprise.spendsync.matching.internal.repository;

import com.enterprise.spendsync.matching.internal.domain.SupplierInvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierInvoiceLineItemRepository extends JpaRepository<SupplierInvoiceLineItem, UUID> {

    List<SupplierInvoiceLineItem> findAllBySupplierInvoiceId(UUID supplierInvoiceId);
}
