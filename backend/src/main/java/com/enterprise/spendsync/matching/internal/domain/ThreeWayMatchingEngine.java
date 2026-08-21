package com.enterprise.spendsync.matching.internal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * High-performance algorithmic engine for Touchless 3-Way & 2-Way Invoice Matching.
 */
public final class ThreeWayMatchingEngine {

    public static final BigDecimal DEFAULT_PRICE_TOLERANCE_PCT = BigDecimal.valueOf(1.00); // 1%
    public static final BigDecimal PENNY_ROUNDING_TOLERANCE = BigDecimal.valueOf(0.05); // 0.05 currency units

    private ThreeWayMatchingEngine() {
    }

    public record MatchResult(
            boolean isMatched,
            InvoiceMatchStatus status,
            String varianceReason
    ) {
        public static MatchResult matched() {
            return new MatchResult(true, InvoiceMatchStatus.AUTO_MATCHED, null);
        }

        public static MatchResult discrepancy(String reason) {
            return new MatchResult(false, InvoiceMatchStatus.DISCREPANCY_HOLD, reason);
        }
    }

    /**
     * Evaluates unit price variance against PO contract unit price under tolerance percentage.
     */
    public static boolean isPriceWithinTolerance(BigDecimal invoicedPrice, BigDecimal poPrice, BigDecimal tolerancePct) {
        if (invoicedPrice == null || poPrice == null) {
            return false;
        }
        if (invoicedPrice.compareTo(poPrice) == 0) {
            return true;
        }

        BigDecimal diff = invoicedPrice.subtract(poPrice).abs();
        // Penny tolerance check
        if (diff.compareTo(PENNY_ROUNDING_TOLERANCE) <= 0) {
            return true;
        }

        BigDecimal tol = tolerancePct != null ? tolerancePct : DEFAULT_PRICE_TOLERANCE_PCT;
        BigDecimal maxAllowedDiff = poPrice.multiply(tol).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return diff.compareTo(maxAllowedDiff) <= 0;
    }

    /**
     * Evaluates quantity variance against accepted GR quantity (3-Way) or PO quantity (2-Way).
     */
    public static boolean isQuantityWithinTolerance(BigDecimal invoicedQty, BigDecimal maxAllowedQty) {
        if (invoicedQty == null || maxAllowedQty == null) {
            return false;
        }
        return invoicedQty.compareTo(maxAllowedQty) <= 0;
    }

    /**
     * Evaluates a line item match.
     */
    public static MatchResult evaluateLineItem(BigDecimal invoicedQty,
                                              BigDecimal invoicedPrice,
                                              BigDecimal poPrice,
                                              BigDecimal maxAllowedQty,
                                              BigDecimal priceTolerancePct) {
        if (!isQuantityWithinTolerance(invoicedQty, maxAllowedQty)) {
            return MatchResult.discrepancy(String.format("Quantity discrepancy: Invoiced (%.2f) > Allowed (%.2f)", invoicedQty, maxAllowedQty));
        }
        if (!isPriceWithinTolerance(invoicedPrice, poPrice, priceTolerancePct)) {
            return MatchResult.discrepancy(String.format("Price discrepancy: Invoiced (%.2f) != Contract (%.2f)", invoicedPrice, poPrice));
        }
        return MatchResult.matched();
    }
}
