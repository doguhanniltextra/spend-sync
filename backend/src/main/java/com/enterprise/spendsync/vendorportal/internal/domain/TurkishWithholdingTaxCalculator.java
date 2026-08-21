package com.enterprise.spendsync.vendorportal.internal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Turkish Revenue Administration (GİB) Withholding Tax (KDV Tevkifatı) Engine.
 * Implements standard Turkish VAT withholding codes (601, 608, 627, 610, etc.).
 */
public final class TurkishWithholdingTaxCalculator {

    private TurkishWithholdingTaxCalculator() {
    }

    public record CalculationResult(
            BigDecimal baseAmount,
            BigDecimal taxRate,
            BigDecimal vatAmount,
            String tevkifatCode,
            String tevkifatRate,
            BigDecimal withholdingAmount,
            BigDecimal accruedVatAmount,
            BigDecimal totalPayableAmount
    ) {}

    /**
     * Calculates VAT and Tevkifat withholding amounts based on line base amount, VAT rate and Tevkifat code/rate.
     *
     * @param baseAmount Matrah (KDV Hariç Tutar)
     * @param vatRate KDV Oranı (örn: 20.00)
     * @param tevkifatCode GİB Tevkifat Kodu (örn: "601", "608", "627", "610", "NONE")
     * @param explicitRate Tevkifat Oranı (örn: "2/10", "5/10", "7/10", "9/10", null)
     */
    public static CalculationResult calculate(BigDecimal baseAmount, BigDecimal vatRate, String tevkifatCode, String explicitRate) {
        if (baseAmount == null) {
            baseAmount = BigDecimal.ZERO;
        }
        if (vatRate == null) {
            vatRate = new BigDecimal("20.00");
        }

        BigDecimal vatAmount = baseAmount.multiply(vatRate).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        String resolvedRate = resolveTevkifatRate(tevkifatCode, explicitRate);
        BigDecimal withholdingAmount = BigDecimal.ZERO;

        if (resolvedRate != null && resolvedRate.contains("/")) {
            String[] parts = resolvedRate.split("/");
            if (parts.length == 2) {
                try {
                    BigDecimal num = new BigDecimal(parts[0].trim());
                    BigDecimal denom = new BigDecimal(parts[1].trim());
                    withholdingAmount = vatAmount.multiply(num).divide(denom, 4, RoundingMode.HALF_UP);
                } catch (Exception ignored) {}
            }
        }

        BigDecimal accruedVatAmount = vatAmount.subtract(withholdingAmount);
        BigDecimal totalPayableAmount = baseAmount.add(accruedVatAmount);

        return new CalculationResult(
                baseAmount,
                vatRate,
                vatAmount,
                tevkifatCode,
                resolvedRate,
                withholdingAmount,
                accruedVatAmount,
                totalPayableAmount
        );
    }

    private static String resolveTevkifatRate(String code, String explicitRate) {
        if (explicitRate != null && !explicitRate.isBlank()) {
            return explicitRate.trim();
        }
        if (code == null || code.isBlank() || "NONE".equalsIgnoreCase(code)) {
            return null;
        }
        return switch (code.trim()) {
            case "601" -> "2/10"; // Yapım İşleri
            case "608" -> "5/10"; // Temizlik Hizmetleri
            case "627" -> "7/10"; // Bilişim / Danışmanlık
            case "610" -> "9/10"; // Servis Taşımacılığı
            default -> null;
        };
    }
}
