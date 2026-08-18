package com.enterprise.spendsync.purchasing.internal.repository;

import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    Optional<PurchaseOrder> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<PurchaseOrder> findByPoNumberAndTenantId(String poNumber, UUID tenantId);

    List<PurchaseOrder> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<PurchaseOrder> findAllByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, PurchaseOrderStatus status);

    List<PurchaseOrder> findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(UUID tenantId, UUID vendorId);

    List<PurchaseOrder> findAllByTenantIdAndRequisitionId(UUID tenantId, UUID requisitionId);

    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
