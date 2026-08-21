package com.enterprise.spendsync.intelligence.service;

import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.intelligence.domain.InsightSeverity;
import com.enterprise.spendsync.intelligence.domain.InsightType;
import com.enterprise.spendsync.intelligence.domain.IntelligenceMode;
import com.enterprise.spendsync.intelligence.domain.TargetPersona;
import com.enterprise.spendsync.intelligence.dto.BudgetRunwayAnalysisDto;
import com.enterprise.spendsync.intelligence.dto.CashDiscountOpportunityDto;
import com.enterprise.spendsync.intelligence.dto.CopilotQueryRequest;
import com.enterprise.spendsync.intelligence.dto.CopilotQueryResponse;
import com.enterprise.spendsync.intelligence.dto.ExecutivePulseResponse;
import com.enterprise.spendsync.intelligence.dto.RecommendationCardDto;
import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactRequest;
import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactResponse;
import com.enterprise.spendsync.intelligence.internal.engine.BudgetRunwayEvaluator;
import com.enterprise.spendsync.intelligence.internal.engine.CashDiscountEvaluator;
import com.enterprise.spendsync.intelligence.internal.engine.HybridBriefingSynthesizer;
import com.enterprise.spendsync.intelligence.internal.engine.HybridBriefingSynthesizer.BriefingResult;
import com.enterprise.spendsync.intelligence.internal.engine.LegalRiskEvaluator;
import com.enterprise.spendsync.intelligence.internal.rag.PolicyDocumentRetriever;
import com.enterprise.spendsync.intelligence.internal.rag.PolicyDocumentRetriever.PolicyClause;
import com.enterprise.spendsync.intelligence.internal.service.SpendIntelligenceServiceImpl;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.matching.internal.domain.MatchType;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.requisition.internal.repository.PurchaseRequisitionRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SpendIntelligenceServiceComprehensiveTest {

    @Mock private BudgetPoolRepository budgetPoolRepository;
    @Mock private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock private PurchaseRequisitionRepository purchaseRequisitionRepository;
    @Mock private BudgetRunwayEvaluator budgetRunwayEvaluator;
    @Mock private CashDiscountEvaluator cashDiscountEvaluator;
    @Mock private LegalRiskEvaluator legalRiskEvaluator;
    @Mock private HybridBriefingSynthesizer briefingSynthesizer;
    @Mock private PolicyDocumentRetriever policyRetriever;

    @InjectMocks
    private SpendIntelligenceServiceImpl spendIntelligenceService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;
    private BudgetPool activePool;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenant = new Tenant("Intelligence Corp", "intel-corp");
        legalEntity = new LegalEntity(tenant, "Legal Entity", "LE-01", "1234567890", "TRY", "Istanbul", "TR");
        costCenter = new CostCenter(tenant, legalEntity, "CC-IT", "IT Department");
        costCenter.setId(UUID.randomUUID());
        activePool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("100000.00"), "TRY");
        activePool.setSpentAmount(new BigDecimal("30000.00"));
        activePool.setReservedAmount(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("Should build ExecutivePulse for CFO persona with full financial cards")
    void shouldBuildExecutivePulseForCfo() {
        User cfoUser = new User("cfo@spendsync.com", "hash", "Chief", "Financial", null, "TR");
        cfoUser.setRoles(Set.of(RoleType.ROOT_USER));

        when(budgetPoolRepository.findAllByTenantId(tenantId)).thenReturn(List.of(activePool));
        when(budgetRunwayEvaluator.evaluateRunwayForTenant(tenantId)).thenReturn(List.of(
                new BudgetRunwayAnalysisDto(costCenter.getId(), "CC-IT", "IT Department",
                        new BigDecimal("100000.00"), new BigDecimal("30000.00"), new BigDecimal("10000.00"),
                        new BigDecimal("60000.00"), new BigDecimal("1000.00"), 60, LocalDate.now().plusDays(60), false)
        ));

        when(cashDiscountEvaluator.evaluateDiscountOpportunities(tenantId)).thenReturn(List.of(
                new CashDiscountOpportunityDto(UUID.randomUUID(), "INV-001", "Vendor X",
                        new BigDecimal("10000.00"), "TRY", LocalDate.now().plusDays(30), LocalDate.now().plusDays(5),
                        new BigDecimal("2.00"), new BigDecimal("200.00"), new BigDecimal("9800.00"), new BigDecimal("36.70"))
        ));

        PurchaseRequisition pr = new PurchaseRequisition(tenant, "PR-001", cfoUser, legalEntity, costCenter,
                null, null, RequisitionStatus.PENDING_APPROVAL, BigDecimal.ZERO, "TRY", "Hardware", "Hardware purchase");
        pr.setTotalAmount(new BigDecimal("15000.00"));
        when(purchaseRequisitionRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, RequisitionStatus.PENDING_APPROVAL))
                .thenReturn(List.of(pr));

        SupplierInvoice invoice = new SupplierInvoice(tenant, "INV-001", "ETTN-1", LocalDate.now(),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, null, null, legalEntity, costCenter,
                "TRY", new BigDecimal("10000.00"), new BigDecimal("2000.00"), BigDecimal.ZERO,
                new BigDecimal("12000.00"), new BigDecimal("12000.00"), MatchType.THREE_WAY);
        invoice.setMatchStatus(InvoiceMatchStatus.DISCREPANCY_HOLD);
        invoice.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);

        when(supplierInvoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(invoice));

        when(legalRiskEvaluator.evaluateLegalRisks(tenantId)).thenReturn(List.of(
                new RecommendationCardDto(UUID.randomUUID(), InsightType.DISCREPANCY_LEGAL_RISK, InsightSeverity.CRITICAL,
                        "TTK Legal Defect Notice Risk", "Immediate legal action required.",
                        "Deadline approaching.", "Inspect GRN", "/receiving", BigDecimal.ONE, "ITEMS", "LEG-POL-03")
        ));

        when(briefingSynthesizer.synthesizeBriefing(any(), any(), any()))
                .thenReturn(new BriefingResult("CFO Executive Summary Briefing", IntelligenceMode.DETERMINISTIC_RULES, List.of()));

        ExecutivePulseResponse response = spendIntelligenceService.getExecutivePulse(tenantId, cfoUser);

        assertThat(response).isNotNull();
        assertThat(response.persona()).isEqualTo(TargetPersona.CFO);
        assertThat(response.personaTitle()).contains("Chief Financial Officer");
        assertThat(response.metrics().totalAllocatedBudget()).isEqualByComparingTo("100000.00");
        assertThat(response.metrics().totalSpentMtd()).isEqualByComparingTo("30000.00");
        assertThat(response.metrics().totalCommittedMtd()).isEqualByComparingTo("40000.00");
        assertThat(response.metrics().budgetUtilizationPercent()).isEqualByComparingTo("40.0");
        assertThat(response.metrics().budgetRunwayDaysLowest()).isEqualTo(60);
        assertThat(response.metrics().pendingApprovalCount()).isEqualTo(1);
        assertThat(response.metrics().criticalDiscrepancyCount()).isEqualTo(1);
        assertThat(response.actionableRecommendations()).isNotEmpty();
    }

    @Test
    @DisplayName("Should resolve different personas based on User roles")
    void shouldResolvePersonasCorrectly() {
        when(budgetPoolRepository.findAllByTenantId(tenantId)).thenReturn(List.of(activePool));
        when(briefingSynthesizer.synthesizeBriefing(any(), any(), any()))
                .thenReturn(new BriefingResult("Briefing", IntelligenceMode.DETERMINISTIC_RULES, List.of()));

        // Approver
        User approver = new User("approver@test.com", "hash", "App", "Rover", null, "TR");
        approver.setRoles(Set.of(RoleType.APPROVER));
        ExecutivePulseResponse pulseApprover = spendIntelligenceService.getExecutivePulse(tenantId, approver);
        assertThat(pulseApprover.persona()).isEqualTo(TargetPersona.APPROVER);

        // Procurement
        User buyer = new User("buyer@test.com", "hash", "Buy", "Er", null, "TR");
        buyer.setRoles(Set.of(RoleType.PROCUREMENT));
        ExecutivePulseResponse pulseProc = spendIntelligenceService.getExecutivePulse(tenantId, buyer);
        assertThat(pulseProc.persona()).isEqualTo(TargetPersona.PROCUREMENT);

        // AP Specialist
        User ap = new User("ap@test.com", "hash", "AP", "Spec", null, "TR");
        ap.setRoles(Set.of(RoleType.AP_SPECIALIST));
        ExecutivePulseResponse pulseAp = spendIntelligenceService.getExecutivePulse(tenantId, ap);
        assertThat(pulseAp.persona()).isEqualTo(TargetPersona.AP_SPECIALIST);

        // Requisitioner / Default
        User reqUser = new User("req@test.com", "hash", "Req", "User", null, "TR");
        reqUser.setRoles(Set.of(RoleType.REQUISITIONER));
        ExecutivePulseResponse pulseReq = spendIntelligenceService.getExecutivePulse(tenantId, reqUser);
        assertThat(pulseReq.persona()).isEqualTo(TargetPersona.REQUISITIONER);
    }

    @Test
    @DisplayName("Should handle zero budget allocation without division by zero")
    void shouldHandleZeroAllocation() {
        activePool.setAllocatedAmount(BigDecimal.ZERO);
        activePool.setSpentAmount(BigDecimal.ZERO);
        activePool.setReservedAmount(BigDecimal.ZERO);
        when(budgetPoolRepository.findAllByTenantId(tenantId)).thenReturn(List.of(activePool));
        when(briefingSynthesizer.synthesizeBriefing(any(), any(), any()))
                .thenReturn(new BriefingResult("Zero briefing", IntelligenceMode.DETERMINISTIC_RULES, List.of()));

        ExecutivePulseResponse response = spendIntelligenceService.getExecutivePulse(tenantId, null);
        assertThat(response.metrics().budgetUtilizationPercent()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should simulate What-If budget impact successfully")
    void shouldSimulateWhatIfBudgetImpact() {
        when(budgetPoolRepository.findAllByTenantId(tenantId)).thenReturn(List.of(activePool));

        WhatIfBudgetImpactRequest request = new WhatIfBudgetImpactRequest(costCenter.getId(), new BigDecimal("25000.00"));
        WhatIfBudgetImpactResponse response = spendIntelligenceService.simulateWhatIfBudgetImpact(tenantId, request);

        assertThat(response).isNotNull();
        assertThat(response.costCenterName()).isEqualTo("IT Department");
        assertThat(response.simulatedUtilizationPercent()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should throw POOL_NOT_FOUND when cost center budget pool does not exist")
    void shouldThrowWhenCostCenterPoolNotFound() {
        when(budgetPoolRepository.findAllByTenantId(tenantId)).thenReturn(List.of());

        WhatIfBudgetImpactRequest request = new WhatIfBudgetImpactRequest(UUID.randomUUID(), new BigDecimal("5000.00"));

        assertThatThrownBy(() -> spendIntelligenceService.simulateWhatIfBudgetImpact(tenantId, request))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Cost Center Budget Pool not found");
    }

    @Test
    @DisplayName("Should delegate to runway and discount evaluators")
    void shouldDelegateToEvaluators() {
        spendIntelligenceService.getBudgetRunwayAnalysis(tenantId);
        verify(budgetRunwayEvaluator).evaluateRunwayForTenant(tenantId);

        spendIntelligenceService.getCashDiscountOpportunities(tenantId);
        verify(cashDiscountEvaluator).evaluateDiscountOpportunities(tenantId);
    }

    @Test
    @DisplayName("Should answer Copilot queries with retrieved policies and fallback")
    void shouldAnswerCopilotQuery() {
        PolicyClause clause = new PolicyClause("PROC-POL-01", "Mandatory 3-Quote Tender Rule", "PROCUREMENT", "Content");
        when(policyRetriever.retrieveRelevantClauses("quote tender")).thenReturn(List.of(clause));

        CopilotQueryResponse response = spendIntelligenceService.askCopilot(
                tenantId, null, new CopilotQueryRequest("quote tender", null));

        assertThat(response.answerMarkdown()).contains("Mandatory 3-Quote Tender Rule");
        assertThat(response.citedPolicyClauses()).contains("PROC-POL-01");
        assertThat(response.executionMode()).isEqualTo(IntelligenceMode.DETERMINISTIC_RULES);
    }

    @Test
    @DisplayName("Should answer Copilot queries with default policy when none matched")
    void shouldAnswerCopilotQueryWithDefaultWhenEmpty() {
        when(policyRetriever.retrieveRelevantClauses("random query")).thenReturn(List.of());

        CopilotQueryResponse response = spendIntelligenceService.askCopilot(
                tenantId, null, new CopilotQueryRequest("random query", null));

        assertThat(response.answerMarkdown()).contains("Standard Procurement Policy");
        assertThat(response.citedPolicyClauses()).isEmpty();
    }
}
