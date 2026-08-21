package com.enterprise.spendsync.intelligence.internal.engine;

import com.enterprise.spendsync.intelligence.dto.WhatIfBudgetImpactResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SpendIntelligenceCalculator {

    private SpendIntelligenceCalculator() {}

    /**
     * Calculates the average daily spend amount since the start of the fiscal year.
     */
    public static BigDecimal calculateDailyBurnRate(BigDecimal totalSpent, int elapsedDaysInYear) {
        if (totalSpent == null || totalSpent.compareTo(BigDecimal.ZERO) <= 0 || elapsedDaysInYear <= 0) {
            return BigDecimal.ZERO;
        }
        return totalSpent.divide(BigDecimal.valueOf(elapsedDaysInYear), 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the estimated number of runway days until remaining budget reaches zero.
     */
    public static int calculateRunwayDays(BigDecimal availableBudget, BigDecimal dailyBurnRate) {
        if (availableBudget == null || availableBudget.compareTo(BigDecimal.ZERO) <= 0 ||
            dailyBurnRate == null || dailyBurnRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return availableBudget.divide(dailyBurnRate, 0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * Calculates estimated exhaustion date by adding runway days to the reference date.
     */
    public static LocalDate calculateEstimatedExhaustionDate(LocalDate fromDate, int runwayDays) {
        if (fromDate == null || runwayDays <= 0) {
            return LocalDate.now().plusDays(365);
        }
        return fromDate.plusDays(runwayDays);
    }

    /**
     * Calculates potential dynamic cash discount amount (e.g. 2% discount on gross invoice amount).
     */
    public static BigDecimal calculateCashDiscount(BigDecimal grossAmount, BigDecimal discountRatePercent) {
        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0 ||
            discountRatePercent == null || discountRatePercent.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return grossAmount.multiply(discountRatePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the annualized APR yield for early payment discount terms (e.g. 2/10 Net 30 -> 36.73% APR).
     * Formula: APR = (Discount% / (100 - Discount%)) * (360 / (NetTerms - DiscountTerms)) * 100
     */
    public static BigDecimal calculateAnnualizedApr(BigDecimal discountPercent, int netTermsDays, int discountTermsDays) {
        int daysSaved = netTermsDays - discountTermsDays;
        if (daysSaved <= 0 || discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal numerator = discountPercent.multiply(BigDecimal.valueOf(360));
        BigDecimal denominator = BigDecimal.valueOf(100).subtract(discountPercent).multiply(BigDecimal.valueOf(daysSaved));
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    /**
     * Simulates the marginal budget impact of an unapproved purchase requisition on a cost center budget.
     */
    public static WhatIfBudgetImpactResponse evaluateWhatIfImpact(
            UUID costCenterId,
            String costCenterName,
            BigDecimal allocated,
            BigDecimal spent,
            BigDecimal reserved,
            BigDecimal proposedAmount) {

        BigDecimal safeAllocated = allocated != null ? allocated : BigDecimal.ZERO;
        BigDecimal safeSpent = spent != null ? spent : BigDecimal.ZERO;
        BigDecimal safeReserved = reserved != null ? reserved : BigDecimal.ZERO;
        BigDecimal safeProposed = proposedAmount != null ? proposedAmount : BigDecimal.ZERO;

        BigDecimal currentCommitted = safeSpent.add(safeReserved);
        BigDecimal currentUtilization = safeAllocated.compareTo(BigDecimal.ZERO) > 0
                ? currentCommitted.divide(safeAllocated, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal simulatedCommitted = currentCommitted.add(safeProposed);
        BigDecimal simulatedUtilization = safeAllocated.compareTo(BigDecimal.ZERO) > 0
                ? simulatedCommitted.divide(safeAllocated, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal marginalIncrease = simulatedUtilization.subtract(currentUtilization);
        boolean causesOverrun = simulatedCommitted.compareTo(safeAllocated) > 0;
        boolean exceedsWarning = simulatedUtilization.compareTo(BigDecimal.valueOf(80)) >= 0;

        String riskMessage = causesOverrun
                ? String.format("CRITICAL OVERRUN: Approving this PR will cause budget deficit by %s TRY (%%%.1f utilization).",
                    simulatedCommitted.subtract(safeAllocated).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    simulatedUtilization.doubleValue())
                : exceedsWarning
                ? String.format("THRESHOLD ALERT: Approving this PR will elevate cost center utilization into high-risk zone (%%%.1f).",
                    simulatedUtilization.doubleValue())
                : String.format("BUDGET COMPLIANT: Cost center utilization will increase marginally by +%%%.1f to %%%.1f.",
                    marginalIncrease.doubleValue(), simulatedUtilization.doubleValue());

        return new WhatIfBudgetImpactResponse(
                costCenterId,
                costCenterName,
                safeAllocated.setScale(2, RoundingMode.HALF_UP),
                currentCommitted.setScale(2, RoundingMode.HALF_UP),
                currentUtilization.setScale(1, RoundingMode.HALF_UP),
                safeProposed.setScale(2, RoundingMode.HALF_UP),
                simulatedCommitted.setScale(2, RoundingMode.HALF_UP),
                simulatedUtilization.setScale(1, RoundingMode.HALF_UP),
                marginalIncrease.setScale(1, RoundingMode.HALF_UP),
                causesOverrun,
                exceedsWarning,
                riskMessage
        );
    }

    /**
     * Evaluates remaining days under the Turkish Commercial Code (TTK m.23) 8-day defect notice window.
     */
    public static int calculateRemainingStatutoryNoticeDays(LocalDate waybillDate, LocalDate currentDate, int statutoryLimitDays) {
        if (waybillDate == null || currentDate == null) {
            return statutoryLimitDays;
        }
        long daysElapsed = ChronoUnit.DAYS.between(waybillDate, currentDate);
        int remaining = statutoryLimitDays - (int) daysElapsed;
        return Math.max(remaining, 0);
    }

    public record PriceAnomalyResult(
            boolean isAnomaly,
            BigDecimal variancePercentage,
            String warningMessage
    ) {}

    /**
     * Evaluates unit price anomaly against historical benchmark price (e.g. flags if > +50% above benchmark).
     */
    public static PriceAnomalyResult evaluatePriceAnomaly(
            BigDecimal proposedUnitPrice,
            BigDecimal benchmarkPrice,
            BigDecimal anomalyThresholdPercent) {

        if (proposedUnitPrice == null || benchmarkPrice == null || benchmarkPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new PriceAnomalyResult(false, BigDecimal.ZERO, "Insufficient price baseline");
        }

        BigDecimal threshold = anomalyThresholdPercent != null ? anomalyThresholdPercent : new BigDecimal("50.00");
        BigDecimal variance = proposedUnitPrice.subtract(benchmarkPrice)
                .divide(benchmarkPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        boolean isAnomaly = variance.compareTo(threshold) > 0;
        String message = isAnomaly
                ? String.format("PRICE ANOMALY DETECTED: Proposed unit price (%s) exceeds historical baseline (%s) by +%.1f%% (Threshold: +%.1f%%).",
                proposedUnitPrice.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                benchmarkPrice.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                variance.doubleValue(),
                threshold.doubleValue())
                : String.format("Price within acceptable range (+%.1f%% variance).", variance.doubleValue());

        return new PriceAnomalyResult(isAnomaly, variance, message);
    }

    public record DuplicateInvoiceRiskResult(
            int riskScore,
            boolean isHighRisk,
            String riskLevel,
            String analysis
    ) {}

    /**
     * Evaluates duplicate invoice suspicion score (0 - 100).
     */
    public static DuplicateInvoiceRiskResult scoreDuplicateInvoiceRisk(
            UUID vendorId1,
            UUID vendorId2,
            String invoiceNum1,
            String invoiceNum2,
            BigDecimal amount1,
            BigDecimal amount2,
            LocalDate date1,
            LocalDate date2) {

        int score = 0;
        List<String> signals = new ArrayList<>();

        if (vendorId1 != null && vendorId1.equals(vendorId2)) {
            score += 30;
            signals.add("Matching Vendor");
        }

        if (amount1 != null && amount2 != null && amount1.compareTo(amount2) == 0) {
            score += 40;
            signals.add("Identical Payable Amount (" + amount1.toPlainString() + ")");
        }

        if (invoiceNum1 != null && invoiceNum2 != null && invoiceNum1.trim().equalsIgnoreCase(invoiceNum2.trim())) {
            score += 20;
            signals.add("Identical Invoice Number (" + invoiceNum1 + ")");
        }

        if (date1 != null && date2 != null) {
            long dayDiff = Math.abs(ChronoUnit.DAYS.between(date1, date2));
            if (dayDiff <= 7) {
                score += 10;
                signals.add("Dates within " + dayDiff + " days");
            }
        }

        boolean isHighRisk = score >= 70;
        String level = score >= 90 ? "CRITICAL_DUPLICATE" : score >= 70 ? "HIGH_SUSPICION" : score >= 40 ? "MEDIUM_SUSPICION" : "LOW_RISK";
        String analysis = String.join(", ", signals);

        return new DuplicateInvoiceRiskResult(score, isHighRisk, level, analysis);
    }
}
