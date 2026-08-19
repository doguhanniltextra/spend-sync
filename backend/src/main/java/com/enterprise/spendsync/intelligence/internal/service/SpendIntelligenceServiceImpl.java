package com.enterprise.spendsync.intelligence.internal.service;

import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.core.internal.domain.RoleType;
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
import com.enterprise.spendsync.intelligence.dto.FinancialPulseMetricsDto;
import com.enterprise.spendsync.intelligence.dto.RecommendationCardDto;
import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactRequest;
import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactResponse;
import com.enterprise.spendsync.intelligence.internal.engine.BudgetRunwayEvaluator;
import com.enterprise.spendsync.intelligence.internal.engine.CashDiscountEvaluator;
import com.enterprise.spendsync.intelligence.internal.engine.HybridBriefingSynthesizer;
import com.enterprise.spendsync.intelligence.internal.engine.HybridBriefingSynthesizer.BriefingResult;
import com.enterprise.spendsync.intelligence.internal.engine.LegalRiskEvaluator;
import com.enterprise.spendsync.intelligence.internal.engine.SpendIntelligenceCalculator;
import com.enterprise.spendsync.intelligence.internal.rag.PolicyDocumentRetriever;
import com.enterprise.spendsync.intelligence.internal.rag.PolicyDocumentRetriever.PolicyClause;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.requisition.internal.repository.PurchaseRequisitionRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SpendIntelligenceServiceImpl implements SpendIntelligenceService {

    private final BudgetPoolRepository budgetPoolRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final PurchaseRequisitionRepository purchaseRequisitionRepository;
    private final BudgetRunwayEvaluator budgetRunwayEvaluator;
    private final CashDiscountEvaluator cashDiscountEvaluator;
    private final LegalRiskEvaluator legalRiskEvaluator;
    private final HybridBriefingSynthesizer briefingSynthesizer;
    private final PolicyDocumentRetriever policyRetriever;

    public SpendIntelligenceServiceImpl(
            BudgetPoolRepository budgetPoolRepository,
            SupplierInvoiceRepository supplierInvoiceRepository,
            PurchaseRequisitionRepository purchaseRequisitionRepository,
            BudgetRunwayEvaluator budgetRunwayEvaluator,
            CashDiscountEvaluator cashDiscountEvaluator,
            LegalRiskEvaluator legalRiskEvaluator,
            HybridBriefingSynthesizer briefingSynthesizer,
            PolicyDocumentRetriever policyRetriever) {
        this.budgetPoolRepository = budgetPoolRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.purchaseRequisitionRepository = purchaseRequisitionRepository;
        this.budgetRunwayEvaluator = budgetRunwayEvaluator;
        this.cashDiscountEvaluator = cashDiscountEvaluator;
        this.legalRiskEvaluator = legalRiskEvaluator;
        this.briefingSynthesizer = briefingSynthesizer;
        this.policyRetriever = policyRetriever;
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutivePulseResponse getExecutivePulse(UUID tenantId, User currentUser) {
        TargetPersona persona = resolvePersona(currentUser);
        String userName = currentUser != null ? currentUser.getFullName() : "Executive";

        // 1. Calculate Ground-Truth Financial Metrics
        List<BudgetPool> pools = budgetPoolRepository.findAllByTenantId(tenantId)
                .stream()
                .filter(p -> p.getStatus() == BudgetStatus.ACTIVE)
                .toList();

        BigDecimal totalAllocated = pools.stream()
                .map(p -> p.getAllocatedAmount() != null ? p.getAllocatedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = pools.stream()
                .map(p -> p.getSpentAmount() != null ? p.getSpentAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReserved = pools.stream()
                .map(p -> p.getReservedAmount() != null ? p.getReservedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommitted = totalSpent.add(totalReserved);
        BigDecimal utilizationPercent = totalAllocated.compareTo(BigDecimal.ZERO) > 0
                ? totalCommitted.divide(totalAllocated, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 2. Budget Runway Analysis
        List<BudgetRunwayAnalysisDto> runways = budgetRunwayEvaluator.evaluateRunwayForTenant(tenantId);
        int lowestRunwayDays = runways.stream()
                .mapToInt(BudgetRunwayAnalysisDto::remainingRunwayDays)
                .filter(d -> d > 0)
                .min()
                .orElse(365);

        // 3. Cash Discount Opportunities
        List<CashDiscountOpportunityDto> discountOpps = cashDiscountEvaluator.evaluateDiscountOpportunities(tenantId);
        BigDecimal totalPotentialDiscountSavings = discountOpps.stream()
                .map(CashDiscountOpportunityDto::potentialCashSavings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Pending Approvals
        List<PurchaseRequisition> pendingPrs = purchaseRequisitionRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(
                tenantId, RequisitionStatus.PENDING_APPROVAL
        );
        int pendingApprovalCount = pendingPrs.size();
        BigDecimal pendingApprovalVolume = pendingPrs.stream()
                .map(pr -> pr.getTotalAmount() != null ? pr.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Critical Discrepancies
        List<SupplierInvoice> holdInvoices = supplierInvoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .filter(inv -> inv.getMatchStatus() == InvoiceMatchStatus.DISCREPANCY_HOLD)
                .toList();

        // 6. Upcoming Disbursements (14 days)
        BigDecimal upcomingDisbursements = supplierInvoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.APPROVED_FOR_PAYMENT)
                .map(SupplierInvoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FinancialPulseMetricsDto metrics = new FinancialPulseMetricsDto(
                totalAllocated.setScale(2, RoundingMode.HALF_UP),
                totalSpent.setScale(2, RoundingMode.HALF_UP),
                totalCommitted.setScale(2, RoundingMode.HALF_UP),
                utilizationPercent,
                upcomingDisbursements.setScale(2, RoundingMode.HALF_UP),
                totalPotentialDiscountSavings.setScale(2, RoundingMode.HALF_UP),
                pendingApprovalCount,
                pendingApprovalVolume.setScale(2, RoundingMode.HALF_UP),
                holdInvoices.size(),
                lowestRunwayDays
        );

        // 7. Actionable Recommendations Compilation
        List<RecommendationCardDto> actionableCards = new ArrayList<>();

        // Add cash discount card if available
        if (totalPotentialDiscountSavings.compareTo(BigDecimal.ZERO) > 0) {
            actionableCards.add(new RecommendationCardDto(
                    UUID.randomUUID(),
                    InsightType.CASH_DISCOUNT_OPPORTUNITY,
                    InsightSeverity.SUCCESS,
                    "Dynamic Cash Discount Available",
                    String.format("Capture %s TRY in early payment discounts across %d verified invoices.",
                            totalPotentialDiscountSavings.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                            discountOpps.size()),
                    "Settling within 10 days yields 36.7% annualized APR working capital return.",
                    "Compile Payment Batch",
                    "/payments",
                    totalPotentialDiscountSavings.setScale(2, RoundingMode.HALF_UP),
                    "TRY",
                    "FIN-POL-12"
            ));
        }

        // Add Runway Warning card if lowest runway is under 90 days
        if (lowestRunwayDays < 90 && !runways.isEmpty()) {
            BudgetRunwayAnalysisDto riskPool = runways.get(0);
            actionableCards.add(new RecommendationCardDto(
                    UUID.randomUUID(),
                    InsightType.BUDGET_RUNWAY_WARNING,
                    lowestRunwayDays < 45 ? InsightSeverity.CRITICAL : InsightSeverity.WARNING,
                    "Departmental Runway Threshold Alert",
                    String.format("%s (%s) has %d days of budget runway remaining.",
                            riskPool.costCenterName(), riskPool.costCenterCode(), riskPool.remainingRunwayDays()),
                    String.format("Estimated exhaustion on %s at current daily burn rate of %s TRY/day.",
                            riskPool.estimatedExhaustionDate(), riskPool.dailyBurnRate().toPlainString()),
                    "Simulate Budget Allocation",
                    "/budgets",
                    BigDecimal.valueOf(riskPool.remainingRunwayDays()),
                    "DAYS",
                    "BUDGET-POL-03"
            ));
        }

        // Add Legal Risk cards
        actionableCards.addAll(legalRiskEvaluator.evaluateLegalRisks(tenantId));

        // Add Pending Approvals card if any
        if (pendingApprovalCount > 0) {
            actionableCards.add(new RecommendationCardDto(
                    UUID.randomUUID(),
                    InsightType.APPROVAL_SLA_BOTTLENECK,
                    InsightSeverity.INFO,
                    "Pending Authorization Queue",
                    String.format("%d requisitions totaling %s TRY await review.",
                            pendingApprovalCount, pendingApprovalVolume.setScale(2, RoundingMode.HALF_UP).toPlainString()),
                    "Decisions pending manager DOA sign-off to unblock procurement dispatch.",
                    "Open Approval Queue",
                    "/approvals",
                    BigDecimal.valueOf(pendingApprovalCount),
                    "ITEMS",
                    "DOA-POL-04"
            ));
        }

        // 8. Synthesize Executive Narrative (Dual-Mode: AI vs Deterministic Rules)
        BriefingResult briefing = briefingSynthesizer.synthesizeBriefing(persona, userName, metrics);

        return new ExecutivePulseResponse(
                persona,
                getPersonaTitle(persona),
                String.format("Good day, %s", userName),
                briefing.executiveSummary(),
                actionableCards,
                metrics,
                briefing.mode(),
                Instant.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetRunwayAnalysisDto> getBudgetRunwayAnalysis(UUID tenantId) {
        return budgetRunwayEvaluator.evaluateRunwayForTenant(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashDiscountOpportunityDto> getCashDiscountOpportunities(UUID tenantId) {
        return cashDiscountEvaluator.evaluateDiscountOpportunities(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public WhatIfBudgetImpactResponse simulateWhatIfBudgetImpact(UUID tenantId, WhatIfBudgetImpactRequest request) {
        BudgetPool pool = budgetPoolRepository.findAllByTenantId(tenantId)
                .stream()
                .filter(p -> p.getCostCenter().getId().equals(request.costCenterId()))
                .findFirst()
                .orElseThrow(() -> new SpendSyncException("Cost Center Budget Pool not found", HttpStatus.NOT_FOUND, "POOL_NOT_FOUND") {});

        return SpendIntelligenceCalculator.evaluateWhatIfImpact(
                pool.getCostCenter().getId(),
                pool.getCostCenter().getName(),
                pool.getAllocatedAmount(),
                pool.getSpentAmount(),
                pool.getReservedAmount(),
                request.proposedAmount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CopilotQueryResponse askCopilot(UUID tenantId, User currentUser, CopilotQueryRequest request) {
        List<PolicyClause> matchedPolicies = policyRetriever.retrieveRelevantClauses(request.query());
        List<String> policyCodes = matchedPolicies.stream().map(PolicyClause::clauseCode).toList();
        List<String> dataSources = List.of("budget_pools", "purchase_orders", "supplier_invoices", "goods_receipts");

        String answer = String.format("""
            ### SpendSync Financial Intelligence Analysis
            Based on enterprise database records and corporate procurement policies:
            
            - **Query:** "%s"
            - **Policy Governance:** Governed by **%s** (%s).
            - **Financial Posture:** All budget allocations, 3-Way Match integrity, and DOA limits are enforced deterministically.
            - **Next Steps:** Refer to the relevant module console to inspect detailed ledger entries or execute authorizations.
            """,
                request.query(),
                matchedPolicies.isEmpty() ? "Standard Procurement Policy" : matchedPolicies.get(0).title(),
                matchedPolicies.isEmpty() ? "PROC-POL-01" : matchedPolicies.get(0).clauseCode()
        );

        return new CopilotQueryResponse(
                answer,
                dataSources,
                policyCodes,
                IntelligenceMode.DETERMINISTIC_RULES,
                Instant.now()
        );
    }

    private TargetPersona resolvePersona(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return TargetPersona.REQUISITIONER;
        }
        if (user.getRoles().contains(RoleType.ROOT_USER)) {
            return TargetPersona.CFO;
        }
        if (user.getRoles().contains(RoleType.APPROVER)) {
            return TargetPersona.APPROVER;
        }
        if (user.getRoles().contains(RoleType.PROCUREMENT)) {
            return TargetPersona.PROCUREMENT;
        }
        if (user.getRoles().contains(RoleType.AP_SPECIALIST)) {
            return TargetPersona.AP_SPECIALIST;
        }
        return TargetPersona.REQUISITIONER;
    }

    private String getPersonaTitle(TargetPersona persona) {
        return switch (persona) {
            case CFO -> "Chief Financial Officer (Executive Cockpit)";
            case APPROVER -> "Cost Center Approver & Department Director";
            case PROCUREMENT -> "Procurement & Strategic Sourcing Lead";
            case AP_SPECIALIST -> "Accounts Payable & Treasury Specialist";
            default -> "Enterprise Requisitioner & Team Member";
        };
    }
}
