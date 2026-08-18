package com.enterprise.spendsync.requisition.internal.service;

import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.requisition.internal.dto.ApproveRequisitionStepRequest;
import com.enterprise.spendsync.requisition.internal.dto.CreateRequisitionRequest;
import com.enterprise.spendsync.requisition.internal.dto.RejectRequisitionRequest;
import com.enterprise.spendsync.requisition.internal.dto.RequisitionDetailResponse;
import com.enterprise.spendsync.requisition.internal.dto.RequisitionSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface RequisitionService {

    /**
     * Creates line items, calculates total, reserves budget atomically, and constructs
     * the sequential approval DAG chain based on statutory limits.
     */
    RequisitionDetailResponse createAndSubmitRequisition(CreateRequisitionRequest request);

    RequisitionDetailResponse getRequisitionById(UUID id);

    List<RequisitionSummaryResponse> getMyRequisitions();

    List<RequisitionSummaryResponse> getAllRequisitions(RequisitionStatus status);

    List<RequisitionDetailResponse> getMyPendingApprovals();

    /**
     * Approves current step in the approval DAG. Enforces SoD and authority limits.
     * Transitions next step to PENDING or finalizes PR to APPROVED and publishes RequisitionApprovedEvent.
     */
    RequisitionDetailResponse approveStep(UUID requisitionId, ApproveRequisitionStepRequest request);

    /**
     * Rejects the requisition at current step, releases reserved budget back to available pool,
     * and publishes RequisitionRejectedEvent.
     */
    RequisitionDetailResponse rejectRequisition(UUID requisitionId, RejectRequisitionRequest request);

    RequisitionDetailResponse cancelRequisition(UUID requisitionId);
}
