package com.enterprise.spendsync.vendorportal.internal.repository;

import com.enterprise.spendsync.vendorportal.internal.domain.VendorPoAcknowledgment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorPoAcknowledgmentRepository extends JpaRepository<VendorPoAcknowledgment, UUID> {

    List<VendorPoAcknowledgment> findAllByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(UUID tenantId, UUID purchaseOrderId);

    Optional<VendorPoAcknowledgment> findTopByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(UUID tenantId, UUID purchaseOrderId);

    boolean existsByTenantIdAndPurchaseOrderId(UUID tenantId, UUID purchaseOrderId);
}
