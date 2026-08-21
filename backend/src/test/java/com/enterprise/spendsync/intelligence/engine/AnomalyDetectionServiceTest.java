package com.enterprise.spendsync.intelligence.engine;

import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactResponse;
import com.enterprise.spendsync.intelligence.internal.engine.SpendIntelligenceCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Spend Intelligence Anomaly Detection & AI Risk Scoring Pure Unit Tests")
class AnomalyDetectionServiceTest {

    @ParameterizedTest(name = "Proposed: {0} TL, Benchmark: {1} TL, Threshold: %{2} -> Anomaly: {3}, Variance: %{4}")
    @CsvSource({
            // TC-10-07: +60% variance > 50% threshold -> Anomaly Flagged
            "160.00, 100.00, 50.00, true, 60.00",
            // +20% variance <= 50% threshold -> Normal
            "120.00, 100.00, 50.00, false, 20.00",
            // +100% price spike (Double price) -> Critical Anomaly
            "200.00, 100.00, 50.00, true, 100.00",
            // Lower price (-10%) -> Normal
            "90.00, 100.00, 50.00, false, -10.00"
    })
    @DisplayName("TC-10-07: Detects price anomalies when item price exceeds historical moving average by threshold")
    void shouldDetectPriceAnomalies(String proposed, String benchmark, String threshold, boolean expectedAnomaly, String expectedVariance) {
        SpendIntelligenceCalculator.PriceAnomalyResult result = SpendIntelligenceCalculator.evaluatePriceAnomaly(
                new BigDecimal(proposed),
                new BigDecimal(benchmark),
                new BigDecimal(threshold)
        );

        assertThat(result.isAnomaly()).isEqualTo(expectedAnomaly);
        assertThat(result.variancePercentage()).isEqualByComparingTo(new BigDecimal(expectedVariance));
        if (expectedAnomaly) {
            assertThat(result.warningMessage()).contains("PRICE ANOMALY DETECTED");
        }
    }

    @Test
    @DisplayName("TC-10-08: Scores duplicate invoice suspicion (100% for identical vendor, amount, invoice number & dates)")
    void shouldScoreCriticalDuplicateInvoiceRisk() {
        UUID vendorId = UUID.randomUUID();
        LocalDate now = LocalDate.now();

        SpendIntelligenceCalculator.DuplicateInvoiceRiskResult result = SpendIntelligenceCalculator.scoreDuplicateInvoiceRisk(
                vendorId, vendorId,
                "INV-2026-9999", "INV-2026-9999",
                new BigDecimal("150000.00"), new BigDecimal("150000.00"),
                now, now.plusDays(1)
        );

        assertThat(result.riskScore()).isEqualTo(100);
        assertThat(result.isHighRisk()).isTrue();
        assertThat(result.riskLevel()).isEqualTo("CRITICAL_DUPLICATE");
        assertThat(result.analysis()).contains("Matching Vendor");
        assertThat(result.analysis()).contains("Identical Payable Amount");
        assertThat(result.analysis()).contains("Identical Invoice Number");
    }

    @Test
    @DisplayName("TC-10-08: Scores low risk when invoices have different vendors, amounts and distant dates")
    void shouldScoreLowRiskForDifferentInvoices() {
        SpendIntelligenceCalculator.DuplicateInvoiceRiskResult result = SpendIntelligenceCalculator.scoreDuplicateInvoiceRisk(
                UUID.randomUUID(), UUID.randomUUID(),
                "INV-A", "INV-B",
                new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                LocalDate.now(), LocalDate.now().plusDays(30)
        );

        assertThat(result.riskScore()).isEqualTo(0);
        assertThat(result.isHighRisk()).isFalse();
        assertThat(result.riskLevel()).isEqualTo("LOW_RISK");
    }

    @Test
    @DisplayName("Evaluates What-If budget impact and flags critical overrun when PR causes budget deficit")
    void shouldEvaluateWhatIfBudgetOverrun() {
        UUID costCenterId = UUID.randomUUID();
        BigDecimal allocated = new BigDecimal("1000000.00");
        BigDecimal spent = new BigDecimal("800000.00");
        BigDecimal reserved = new BigDecimal("100000.00");
        BigDecimal proposedPr = new BigDecimal("200000.00"); // Total would be 1.1M (> 1.0M)

        WhatIfBudgetImpactResponse impact = SpendIntelligenceCalculator.evaluateWhatIfImpact(
                costCenterId, "IT Department", allocated, spent, reserved, proposedPr
        );

        assertThat(impact.causesOverrun()).isTrue();
        assertThat(impact.exceedsWarningThreshold()).isTrue();
        assertThat(impact.simulatedUtilizationPercent()).isEqualByComparingTo("110.0");
        assertThat(impact.riskAssessmentMessage()).contains("CRITICAL OVERRUN");
    }

    @Test
    @DisplayName("Calculates remaining statutory defect notice days under Turkish Commercial Code (TTK m.23)")
    void shouldCalculateRemainingTtkNoticeDays() {
        LocalDate waybillDate = LocalDate.now().minusDays(3);
        int remainingDays = SpendIntelligenceCalculator.calculateRemainingStatutoryNoticeDays(
                waybillDate, LocalDate.now(), 8
        );

        assertThat(remainingDays).isEqualTo(5);
    }
}
