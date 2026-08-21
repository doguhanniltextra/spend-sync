package com.enterprise.spendsync.receiving.internal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Evaluates over-delivery tolerances for Goods Receipt processing in SpendSync P2P Engine.
 */
public final class OverDeliveryToleranceEvaluator {

    private OverDeliveryToleranceEvaluator() {
    }

    /**
     * Calculates maximum allowed cumulative receipt quantity given PO ordered quantity and tolerance percentage.
     */
    public static BigDecimal calculateMaxAllowedQuantity(BigDecimal orderedQuantity, BigDecimal tolerancePercentage) {
        if (orderedQuantity == null || orderedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal tolerancePct = (tolerancePercentage != null && tolerancePercentage.compareTo(BigDecimal.ZERO) > 0)
                ? tolerancePercentage
                : BigDecimal.ZERO;

        BigDecimal multiplier = BigDecimal.ONE.add(tolerancePct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return orderedQuantity.multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Verifies if total accepted quantity (previously accepted + newly accepted) is within allowed over-delivery tolerance.
     */
    public static boolean isWithinTolerance(BigDecimal orderedQuantity,
                                            BigDecimal tolerancePercentage,
                                            BigDecimal previouslyAccepted,
                                            BigDecimal newlyAccepted) {
        BigDecimal prev = previouslyAccepted != null ? previouslyAccepted : BigDecimal.ZERO;
        BigDecimal newly = newlyAccepted != null ? newlyAccepted : BigDecimal.ZERO;
        BigDecimal totalAccepted = prev.add(newly);

        BigDecimal maxAllowed = calculateMaxAllowedQuantity(orderedQuantity, tolerancePercentage);
        return totalAccepted.compareTo(maxAllowed) <= 0;
    }
}
