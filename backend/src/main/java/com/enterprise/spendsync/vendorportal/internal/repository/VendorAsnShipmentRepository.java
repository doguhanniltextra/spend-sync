package com.enterprise.spendsync.vendorportal.internal.repository;

import com.enterprise.spendsync.vendorportal.internal.domain.VendorAsnShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorAsnShipmentRepository extends JpaRepository<VendorAsnShipment, UUID> {

    List<VendorAsnShipment> findAllByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(UUID tenantId, UUID purchaseOrderId);

    List<VendorAsnShipment> findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(UUID tenantId, UUID vendorId);

    Optional<VendorAsnShipment> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndWaybillNumber(UUID tenantId, String waybillNumber);
}
