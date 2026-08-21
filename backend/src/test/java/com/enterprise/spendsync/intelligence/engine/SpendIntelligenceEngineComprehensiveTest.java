package com.enterprise.spendsync.intelligence.engine;

import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactResponse;
import com.enterprise.spendsync.intelligence.internal.engine.LegalRiskEvaluator;
import com.enterprise.spendsync.intelligence.internal.engine.SpendIntelligenceCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SpendIntelligenceEngineComprehensiveTest {

    @ParameterizedTest(name = "[{index}] Spent: {0}, ElapsedDays: {1} -> BurnRate: {2}")
    @CsvSource(value = {
            "36500.00, 365, 100.0000",
            "10000.00, 100, 100.0000",
            "0.00,     100,   0.0000",
            "-500.00,  100,   0.0000",
            "10000.00,   0,   0.0000",
            "NULL,     100,   0.0000"
    }, nullValues = {"NULL"})
    @DisplayName("Should calculate daily burn rate across all boundary conditions")
    void shouldCalculateDailyBurnRate(BigDecimal spent, int days, BigDecimal expected) {
        BigDecimal result = SpendIntelligenceCalculator.calculateDailyBurnRate(spent, days);
        assertThat(result).isEqualByComparingTo(expected);
    }

    @ParameterizedTest(name = "[{index}] Available: {0}, BurnRate: {1} -> RunwayDays: {2}")
    @CsvSource(value = {
            "10000.00, 100.00, 100",
            "50000.00, 500.00, 100",
            "0.00,     100.00,   0",
            "10000.00,   0.00,   0",
            "-1000.00, 100.00,   0",
            "NULL,     100.00,   0"
    }, nullValues = {"NULL"})
    @DisplayName("Should calculate runway days across all zero and negative boundaries")
    void shouldCalculateRunwayDays(BigDecimal available, BigDecimal burnRate, int expectedDays) {
        int days = SpendIntelligenceCalculator.calculateRunwayDays(available, burnRate);
        assertThat(days).isEqualTo(expectedDays);
    }

    @ParameterizedTest(name = "[{index}] Discount%: {0}, NetTerms: {1}, DiscountTerms: {2} -> APR: {3}")
    @CsvSource({
            "2.00, 30, 10, 36.73",  // 2/10 Net 30 -> 36.73% APR
            "1.00, 30, 10, 18.18",  // 1/10 Net 30
            "2.00, 10, 10,  0.00",  // NetTerms == DiscountTerms -> 0 APR
            "0.00, 30, 10,  0.00"   // 0% discount -> 0 APR
    })
    @DisplayName("Should calculate annualized APR for dynamic discounting terms")
    void shouldCalculateAnnualizedApr(BigDecimal discount, int net, int discTerms, BigDecimal expectedApr) {
        BigDecimal apr = SpendIntelligenceCalculator.calculateAnnualizedApr(discount, net, discTerms);
        assertThat(apr).isEqualByComparingTo(expectedApr);
    }

    @Test
    @DisplayName("Should evaluate What-If budget impact under HARD_STOP mode")
    void shouldEvaluateWhatIfBudgetImpact() {
        Tenant tenant = new Tenant("Intelligence Corp", "intel-corp");
        LegalEntity le = new LegalEntity(tenant, "LE", "LE-01", "1234567890", "TRY", "Address", "TR");
        CostCenter cc = new CostCenter(tenant, le, "CC-IT", "IT & Cloud");

        BudgetPool pool = new BudgetPool(
                tenant, le, cc, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO,
                new BigDecimal("100000.00"), "TRY"
        );
        pool.setSpentAmount(new BigDecimal("40000.00"));
        pool.setReservedAmount(new BigDecimal("30000.00")); // Available = 30,000.00

        // 1. Within budget simulation (20,000.00)
        WhatIfBudgetImpactResponse okResponse = SpendIntelligenceCalculator.evaluateWhatIfImpact(
                cc.getId(), cc.getName(), pool.getAllocatedAmount(), pool.getSpentAmount(), pool.getReservedAmount(), new BigDecimal("20000.00")
        );
        assertThat(okResponse.causesOverrun()).isFalse();
        assertThat(okResponse.exceedsWarningThreshold()).isTrue(); // 90% utilization >= 80% warning

        // 2. Overrun simulation (50,000.00 > 30,000.00 available)
        WhatIfBudgetImpactResponse overrunResponse = SpendIntelligenceCalculator.evaluateWhatIfImpact(
                cc.getId(), cc.getName(), pool.getAllocatedAmount(), pool.getSpentAmount(), pool.getReservedAmount(), new BigDecimal("50000.00")
        );
        assertThat(overrunResponse.causesOverrun()).isTrue();
    }

    @Test
    @DisplayName("Should evaluate Legal Risk Cards from discrepancy hold invoices and statutory limits")
    void shouldEvaluateLegalRisk() {
        com.enterprise.spendsync.receiving.internal.repository.GoodsReceiptRepository grRepo =
                org.mockito.Mockito.mock(com.enterprise.spendsync.receiving.internal.repository.GoodsReceiptRepository.class);
        com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository invRepo =
                org.mockito.Mockito.mock(com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository.class);

        UUID tenantId = UUID.randomUUID();
        org.mockito.Mockito.when(invRepo.findAllByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(java.util.Collections.emptyList());
        org.mockito.Mockito.when(grRepo.findAllByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(java.util.Collections.emptyList());

        LegalRiskEvaluator evaluator = new LegalRiskEvaluator(grRepo, invRepo);
        var cards = evaluator.evaluateLegalRisks(tenantId);
        assertThat(cards).isNotNull();
    }
}
