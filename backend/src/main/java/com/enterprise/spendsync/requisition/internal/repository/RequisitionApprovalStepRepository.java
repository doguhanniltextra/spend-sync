package com.enterprise.spendsync.requisition.internal.repository;

import com.enterprise.spendsync.requisition.internal.domain.ApprovalStepStatus;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequisitionApprovalStepRepository extends JpaRepository<RequisitionApprovalStep, UUID> {

    List<RequisitionApprovalStep> findAllByRequisitionIdOrderByStepOrderAsc(UUID requisitionId);

    Optional<RequisitionApprovalStep> findByRequisitionIdAndApproverIdAndTenantId(UUID requisitionId, UUID approverId, UUID tenantId);

    @Query("""
        SELECT step FROM RequisitionApprovalStep step
        WHERE step.approver.id = :approverId
          AND step.tenant.id = :tenantId
          AND step.status = 'PENDING'
          AND step.requisition.status = 'PENDING_APPROVAL'
        ORDER BY step.createdAt ASC
    """)
    List<RequisitionApprovalStep> findPendingStepsForApprover(
            @Param("approverId") UUID approverId,
            @Param("tenantId") UUID tenantId
    );
}
