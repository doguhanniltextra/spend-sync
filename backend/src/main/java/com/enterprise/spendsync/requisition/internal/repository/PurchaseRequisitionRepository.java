package com.enterprise.spendsync.requisition.internal.repository;

import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRequisitionRepository extends JpaRepository<PurchaseRequisition, UUID> {

    Optional<PurchaseRequisition> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<PurchaseRequisition> findByRequisitionNumberAndTenantId(String requisitionNumber, UUID tenantId);

    List<PurchaseRequisition> findAllByTenantIdAndRequisitionerIdOrderByCreatedAtDesc(UUID tenantId, UUID requisitionerId);

    List<PurchaseRequisition> findAllByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, RequisitionStatus status);

    List<PurchaseRequisition> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("SELECT COUNT(pr) FROM PurchaseRequisition pr WHERE pr.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
