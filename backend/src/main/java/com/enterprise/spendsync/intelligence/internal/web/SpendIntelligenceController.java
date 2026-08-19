package com.enterprise.spendsync.intelligence.internal.web;

import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.intelligence.dto.BudgetRunwayAnalysisDto;
import com.enterprise.spendsync.intelligence.dto.CashDiscountOpportunityDto;
import com.enterprise.spendsync.intelligence.dto.CopilotQueryRequest;
import com.enterprise.spendsync.intelligence.dto.CopilotQueryResponse;
import com.enterprise.spendsync.intelligence.dto.ExecutivePulseResponse;
import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactRequest;
import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactResponse;
import com.enterprise.spendsync.intelligence.internal.service.SpendIntelligenceService;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Intelligence.BASE)
public class SpendIntelligenceController {

    private final SpendIntelligenceService spendIntelligenceService;
    private final UserRepository userRepository;

    public SpendIntelligenceController(
            SpendIntelligenceService spendIntelligenceService,
            UserRepository userRepository) {
        this.spendIntelligenceService = spendIntelligenceService;
        this.userRepository = userRepository;
    }

    @GetMapping(Endpoints.Intelligence.PULSE)
    @PreAuthorize("hasAnyAuthority('PERM_PR_READ_OWN', 'PERM_PR_READ_ALL', 'PERM_ORG_MANAGE', 'PERM_BUDGET_READ')")
    public ResponseEntity<ExecutivePulseResponse> getExecutivePulse() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = resolveCurrentUser(tenantId);
        return ResponseEntity.ok(spendIntelligenceService.getExecutivePulse(tenantId, currentUser));
    }

    @PostMapping(Endpoints.Intelligence.ASK)
    @PreAuthorize("hasAnyAuthority('PERM_PR_READ_OWN', 'PERM_PR_READ_ALL', 'PERM_ORG_MANAGE', 'PERM_BUDGET_READ')")
    public ResponseEntity<CopilotQueryResponse> askCopilot(@Valid @RequestBody CopilotQueryRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = resolveCurrentUser(tenantId);
        return ResponseEntity.ok(spendIntelligenceService.askCopilot(tenantId, currentUser, request));
    }

    @GetMapping(Endpoints.Intelligence.BUDGET_RUNWAY)
    @PreAuthorize("hasAnyAuthority('PERM_BUDGET_READ', 'PERM_ORG_MANAGE')")
    public ResponseEntity<List<BudgetRunwayAnalysisDto>> getBudgetRunway() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(spendIntelligenceService.getBudgetRunwayAnalysis(tenantId));
    }

    @GetMapping(Endpoints.Intelligence.SAVINGS_OPPORTUNITIES)
    @PreAuthorize("hasAnyAuthority('PERM_PAYMENT_READ', 'PERM_ORG_MANAGE')")
    public ResponseEntity<List<CashDiscountOpportunityDto>> getSavingsOpportunities() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(spendIntelligenceService.getCashDiscountOpportunities(tenantId));
    }

    @PostMapping(Endpoints.Intelligence.WHAT_IF_SIMULATE)
    @PreAuthorize("hasAnyAuthority('PERM_PR_APPROVE', 'PERM_PR_CREATE', 'PERM_ORG_MANAGE', 'PERM_BUDGET_READ')")
    public ResponseEntity<WhatIfBudgetImpactResponse> simulateWhatIf(@Valid @RequestBody WhatIfBudgetImpactRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return ResponseEntity.ok(spendIntelligenceService.simulateWhatIfBudgetImpact(tenantId, request));
    }

    private User resolveCurrentUser(UUID tenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findByIdAndTenantId(principal.getId(), tenantId).orElse(null);
        }
        return null;
    }
}
