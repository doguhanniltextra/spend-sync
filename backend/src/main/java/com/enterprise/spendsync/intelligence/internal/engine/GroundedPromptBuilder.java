package com.enterprise.spendsync.intelligence.internal.engine;

import com.enterprise.spendsync.intelligence.domain.TargetPersona;
import com.enterprise.spendsync.intelligence.dto.FinancialPulseMetricsDto;
import com.enterprise.spendsync.intelligence.internal.rag.PolicyDocumentRetriever.PolicyClause;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GroundedPromptBuilder {

    public String buildExecutiveBriefingPrompt(
            TargetPersona persona,
            String userName,
            FinancialPulseMetricsDto metrics,
            List<PolicyClause> policies) {

        String policyText = policies.stream()
                .map(p -> String.format("- [%s] %s: %s", p.clauseCode(), p.title(), p.content()))
                .collect(Collectors.joining("\n"));

        return String.format("""
            You are SpendSync Enterprise Financial Intelligence Copilot.
            Generate a concise, high-impact 60-second executive narrative briefing for %s (%s).
            
            STRICT RULES:
            1. Use ONLY the verified ground-truth numbers provided below. NEVER invent, calculate, or hallucinate different financial figures.
            2. State the single most urgent decision first, followed by clear next actions.
            
            [VERIFIED GROUND-TRUTH METRICS]
            - Target Persona: %s
            - Total Allocated Budget: %s TRY
            - Total Spent MTD: %s TRY (Utilization: %s%%)
            - Lowest Cost Center Runway: %d Days
            - Actionable Early Cash Discount Savings: %s TRY (Yield: 36.7%% APR)
            - Pending Approval Inbox: %d requests totaling %s TRY
            - Critical 3-Way Match & Legal Risks: %d items
            
            [RETRIEVED CORPORATE POLICIES]
            %s
            
            Respond with a clean 2-3 paragraph executive summary in English.
            """,
                userName,
                persona.name(),
                persona.name(),
                metrics.totalAllocatedBudget() != null ? metrics.totalAllocatedBudget().toPlainString() : "0",
                metrics.totalSpentMtd() != null ? metrics.totalSpentMtd().toPlainString() : "0",
                metrics.budgetUtilizationPercent() != null ? metrics.budgetUtilizationPercent().toPlainString() : "0",
                metrics.budgetRunwayDaysLowest(),
                metrics.totalPotentialDiscountSavings() != null ? metrics.totalPotentialDiscountSavings().toPlainString() : "0",
                metrics.pendingApprovalCount(),
                metrics.pendingApprovalVolume() != null ? metrics.pendingApprovalVolume().toPlainString() : "0",
                metrics.criticalDiscrepancyCount(),
                policyText
        );
    }
}
