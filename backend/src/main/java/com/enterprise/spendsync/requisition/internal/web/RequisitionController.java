package com.enterprise.spendsync.requisition.internal.web;

import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.requisition.internal.dto.ApproveRequisitionStepRequest;
import com.enterprise.spendsync.requisition.internal.dto.CreateRequisitionRequest;
import com.enterprise.spendsync.requisition.internal.dto.RejectRequisitionRequest;
import com.enterprise.spendsync.requisition.internal.dto.RequisitionDetailResponse;
import com.enterprise.spendsync.requisition.internal.dto.RequisitionSummaryResponse;
import com.enterprise.spendsync.requisition.internal.service.RequisitionService;
import com.enterprise.spendsync.shared.config.Endpoints;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Requisition.BASE)
public class RequisitionController {

    private final RequisitionService requisitionService;

    public RequisitionController(RequisitionService requisitionService) {
        this.requisitionService = requisitionService;
    }

    @PreAuthorize("hasAuthority('PERM_PR_CREATE') or hasAuthority('PERM_PR_MANAGE')")
    @PostMapping
    public ResponseEntity<RequisitionDetailResponse> createAndSubmitRequisition(
            @Valid @RequestBody CreateRequisitionRequest request
    ) {
        RequisitionDetailResponse response = requisitionService.createAndSubmitRequisition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('PERM_PR_READ_OWN') or hasAuthority('PERM_PR_READ_ALL')")
    @GetMapping(Endpoints.Requisition.MY_REQUISITIONS)
    public ResponseEntity<List<RequisitionSummaryResponse>> getMyRequisitions() {
        List<RequisitionSummaryResponse> response = requisitionService.getMyRequisitions();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_PR_APPROVE') or hasAuthority('PERM_PR_MANAGE')")
    @GetMapping(Endpoints.Requisition.PENDING_APPROVALS)
    public ResponseEntity<List<RequisitionDetailResponse>> getMyPendingApprovals() {
        List<RequisitionDetailResponse> response = requisitionService.getMyPendingApprovals();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_PR_READ_OWN') or hasAuthority('PERM_PR_READ_ALL')")
    @GetMapping(Endpoints.Requisition.REQUISITION_BY_ID)
    public ResponseEntity<RequisitionDetailResponse> getRequisitionById(@PathVariable UUID id) {
        RequisitionDetailResponse response = requisitionService.getRequisitionById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_PR_APPROVE') or hasAuthority('PERM_PR_MANAGE')")
    @PostMapping(Endpoints.Requisition.APPROVE)
    public ResponseEntity<RequisitionDetailResponse> approveStep(
            @PathVariable UUID id,
            @RequestBody(required = false) ApproveRequisitionStepRequest request
    ) {
        RequisitionDetailResponse response = requisitionService.approveStep(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_PR_REJECT') or hasAuthority('PERM_PR_MANAGE')")
    @PostMapping(Endpoints.Requisition.REJECT)
    public ResponseEntity<RequisitionDetailResponse> rejectRequisition(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequisitionRequest request
    ) {
        RequisitionDetailResponse response = requisitionService.rejectRequisition(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_PR_CREATE') or hasAuthority('PERM_PR_READ_OWN')")
    @PostMapping(Endpoints.Requisition.CANCEL)
    public ResponseEntity<RequisitionDetailResponse> cancelRequisition(@PathVariable UUID id) {
        RequisitionDetailResponse response = requisitionService.cancelRequisition(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_PR_READ_ALL') or hasAuthority('PERM_ORG_MANAGE')")
    @GetMapping
    public ResponseEntity<List<RequisitionSummaryResponse>> getAllRequisitions(
            @RequestParam(required = false) RequisitionStatus status
    ) {
        List<RequisitionSummaryResponse> response = requisitionService.getAllRequisitions(status);
        return ResponseEntity.ok(response);
    }
}
