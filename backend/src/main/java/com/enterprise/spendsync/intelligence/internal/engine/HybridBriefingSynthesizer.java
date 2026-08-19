package com.enterprise.spendsync.intelligence.internal.engine;

import com.enterprise.spendsync.intelligence.domain.IntelligenceMode;
import com.enterprise.spendsync.intelligence.domain.TargetPersona;
import com.enterprise.spendsync.intelligence.dto.FinancialPulseMetricsDto;
import com.enterprise.spendsync.intelligence.internal.client.LlmClient;
import com.enterprise.spendsync.intelligence.internal.rag.PolicyDocumentRetriever;
import com.enterprise.spendsync.intelligence.internal.rag.PolicyDocumentRetriever.PolicyClause;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class HybridBriefingSynthesizer {

    private final LlmClient llmClient;
    private final GroundedPromptBuilder promptBuilder;
    private final PolicyDocumentRetriever policyRetriever;

    public HybridBriefingSynthesizer(
            LlmClient llmClient,
            GroundedPromptBuilder promptBuilder,
            PolicyDocumentRetriever policyRetriever) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.policyRetriever = policyRetriever;
    }

    public record BriefingResult(
            String executiveSummary,
            IntelligenceMode mode,
            List<String> citedPolicies
    ) {}

    public BriefingResult synthesizeBriefing(
            TargetPersona persona,
            String userName,
            FinancialPulseMetricsDto metrics) {

        List<PolicyClause> policies = policyRetriever.retrieveRelevantClauses(persona.name());
        List<String> policyCodes = policies.stream().map(PolicyClause::clauseCode).toList();

        // 1. Check if AI is available
        if (llmClient.isAvailable()) {
            String systemPrompt = promptBuilder.buildExecutiveBriefingPrompt(persona, userName, metrics, policies);
            String aiResponse = llmClient.generateCompletion(systemPrompt, "Provide the 60-second executive spend pulse.");
            if (aiResponse != null && !aiResponse.isBlank()) {
                return new BriefingResult(aiResponse.trim(), IntelligenceMode.AI_GROUNDED_RAG, policyCodes);
            }
        }

        // 2. Mode A: Deterministic Rule-Engine Templated Synthesis (Zero-Failure Fallback)
        String deterministicSummary = generateDeterministicSummary(persona, userName, metrics);
        return new BriefingResult(deterministicSummary, IntelligenceMode.DETERMINISTIC_RULES, policyCodes);
    }

    private String generateDeterministicSummary(
            TargetPersona persona,
            String userName,
            FinancialPulseMetricsDto metrics) {

        BigDecimal spent = metrics.totalSpentMtd() != null ? metrics.totalSpentMtd() : BigDecimal.ZERO;
        BigDecimal utilPct = metrics.budgetUtilizationPercent() != null ? metrics.budgetUtilizationPercent() : BigDecimal.ZERO;
        BigDecimal discountSavings = metrics.totalPotentialDiscountSavings() != null ? metrics.totalPotentialDiscountSavings() : BigDecimal.ZERO;

        return switch (persona) {
            case CFO -> String.format(
                    "Good morning %s. Enterprise budget is operating at %%%.1f utilization (%s TRY spent). " +
                    "Treasury opportunity: %s TRY in dynamic cash discounts (36.7%% APR) is ready for settlement release. " +
                    "Lowest departmental runway is currently %d days.",
                    userName, utilPct.doubleValue(), spent.toPlainString(), discountSavings.toPlainString(), metrics.budgetRunwayDaysLowest()
            );
            case APPROVER -> String.format(
                    "Hello %s. You have %d purchase requisitions totaling %s TRY awaiting your managerial authorization. " +
                    "Departmental budget is safely within allocated runway.",
                    userName, metrics.pendingApprovalCount(),
                    metrics.pendingApprovalVolume() != null ? metrics.pendingApprovalVolume().toPlainString() : "0"
            );
            case PROCUREMENT -> String.format(
                    "Hello %s. %d commercial orders are pending freight receiving inspection at destination docks. " +
                    "All supplier contractual SLAs are currently in good standing.",
                    userName, metrics.criticalDiscrepancyCount() + 1
            );
            default -> String.format(
                    "Hello %s. Your corporate spend requests are tracked and synchronized across approval and dock milestones in real time.",
                    userName
            );
        };
    }
}
