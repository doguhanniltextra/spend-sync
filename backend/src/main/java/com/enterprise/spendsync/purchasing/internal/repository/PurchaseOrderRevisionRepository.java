package com.enterprise.spendsync.purchasing.internal.repository;

import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseOrderRevisionRepository extends JpaRepository<PurchaseOrderRevision, UUID> {

    List<PurchaseOrderRevision> findAllByPurchaseOrderIdAndTenantIdOrderByRevisionNumberAsc(UUID purchaseOrderId, UUID tenantId);
}
