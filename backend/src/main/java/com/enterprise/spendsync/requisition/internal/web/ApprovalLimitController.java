package com.enterprise.spendsync.requisition.internal.web;

import com.enterprise.spendsync.requisition.internal.dto.ApprovalLimitResponse;
import com.enterprise.spendsync.requisition.internal.dto.SetApprovalLimitRequest;
import com.enterprise.spendsync.requisition.internal.service.ApprovalLimitService;
import com.enterprise.spendsync.shared.config.Endpoints;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Approval Authority Limits (DoA Matrix) REST Controller.
 */
@RestController
@RequestMapping(Endpoints.Requisition.BASE)
public class ApprovalLimitController {

    private final ApprovalLimitService approvalLimitService;

    public ApprovalLimitController(ApprovalLimitService approvalLimitService) {
        this.approvalLimitService = approvalLimitService;
    }

    @PreAuthorize("hasAuthority('PERM_ORG_MANAGE') or hasAuthority('PERM_USER_MANAGE')")
    @PostMapping(Endpoints.Requisition.APPROVAL_LIMITS)
    public ResponseEntity<ApprovalLimitResponse> setApprovalLimit(@Valid @RequestBody SetApprovalLimitRequest request) {
        ApprovalLimitResponse response = approvalLimitService.setApprovalLimit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('PERM_PR_READ_ALL') or hasAuthority('PERM_ORG_MANAGE') or hasAuthority('PERM_BUDGET_READ')")
    @GetMapping(Endpoints.Requisition.APPROVAL_LIMITS)
    public ResponseEntity<List<ApprovalLimitResponse>> getAllLimits(
            @RequestParam(required = false) UUID legalEntityId,
            @RequestParam(required = false) UUID userId
    ) {
        List<ApprovalLimitResponse> response = approvalLimitService.getAllLimits(legalEntityId, userId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_PR_READ_ALL') or hasAuthority('PERM_ORG_MANAGE')")
    @GetMapping(Endpoints.Requisition.APPROVAL_LIMIT_BY_ID)
    public ResponseEntity<ApprovalLimitResponse> getApprovalLimitById(@PathVariable UUID id) {
        ApprovalLimitResponse response = approvalLimitService.getApprovalLimitById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_ORG_MANAGE') or hasAuthority('PERM_USER_MANAGE')")
    @PatchMapping(Endpoints.Requisition.APPROVAL_LIMIT_STATUS)
    public ResponseEntity<ApprovalLimitResponse> toggleLimitStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean active = body.getOrDefault("active", true);
        ApprovalLimitResponse response = approvalLimitService.toggleLimitStatus(id, active);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_PR_READ_ALL') or hasAuthority('PERM_PR_CREATE') or hasAuthority('PERM_ORG_MANAGE')")
    @GetMapping(Endpoints.Requisition.EFFECTIVE_LIMIT)
    public ResponseEntity<Map<String, Object>> getEffectiveLimit(
            @RequestParam UUID userId,
            @RequestParam UUID legalEntityId,
            @RequestParam(required = false) UUID costCenterId
    ) {
        var limitOpt = approvalLimitService.getEffectiveLimitDetails(userId, legalEntityId, costCenterId);
        Map<String, Object> result = new HashMap<>();

        if (limitOpt.isEmpty()) {
            result.put("hasConfiguredLimit", false);
            result.put("isUnlimited", false);
            result.put("maxAmount", null);
            return ResponseEntity.ok(result);
        }

        var limit = limitOpt.get();
        result.put("hasConfiguredLimit", true);
        result.put("isUnlimited", limit.isUnlimited());
        result.put("maxAmount", limit.getMaxAmount() != null ? limit.getMaxAmount() : "UNLIMITED");
        result.put("approvalLevel", limit.getApprovalLevel());
        result.put("currency", limit.getCurrency());
        result.put("scope", limit.getCostCenter() != null ? "COST_CENTER" : "LEGAL_ENTITY");

        return ResponseEntity.ok(result);
    }
}
