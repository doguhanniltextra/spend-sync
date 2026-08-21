package com.enterprise.spendsync.matching.service;

import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.ThreeWayMatchingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class ThreeWayMatchParameterizedTest {

    @ParameterizedTest(name = "[{index}] InvoicedPrice: {0}, POPrice: {1}, Tolerance: {2}% -> Expected Price Within Tolerance: {3}")
    @CsvSource({
            "100.00, 100.00, 1.0, true",    // Exact match
            "100.50, 100.00, 1.0, true",    // +0.50% (Within 1% tolerance)
            "101.00, 100.00, 1.0, true",    // Exactly +1.00% (At tolerance threshold)
            "101.05, 100.00, 1.0, false",   // +1.05% (Exceeds 1% tolerance)
            "99.00,  100.00, 1.0, true",    // -1.00% (Under contract price)
            "98.00,  100.00, 1.0, false",   // -2.00% (Exceeds tolerance diff)
            "100.04, 100.00, 0.0, true",    // Penny tolerance (0.04 diff <= 0.05 limit)
            "100.06, 100.00, 0.0, false"    // Exceeds penny tolerance (0.06 diff > 0.05 limit)
    })
    @DisplayName("Should validate price variance under tolerance percentage and penny rounding")
    void shouldEvaluatePriceToleranceBranches(
            BigDecimal invoicedPrice, BigDecimal poPrice, BigDecimal tolerancePct, boolean expectedResult) {
        boolean result = ThreeWayMatchingEngine.isPriceWithinTolerance(invoicedPrice, poPrice, tolerancePct);
        assertThat(result).isEqualTo(expectedResult);
    }

    @ParameterizedTest(name = "[{index}] InvoicedQty: {0}, AllowedQty: {1} -> Expected Quantity Within Tolerance: {2}")
    @CsvSource({
            "10.0, 10.0, true",   // Exact quantity match
            "8.0,  10.0, true",   // Partial delivery (less than allowed)
            "0.0,  10.0, true",   // Zero quantity
            "10.1, 10.0, false",  // Over delivery (exceeds allowed)
            "15.0, 10.0, false"   // Significant quantity discrepancy
    })
    @DisplayName("Should validate quantity tolerance against GRN or PO limits")
    void shouldEvaluateQuantityToleranceBranches(
            BigDecimal invoicedQty, BigDecimal allowedQty, boolean expectedResult) {
        boolean result = ThreeWayMatchingEngine.isQuantityWithinTolerance(invoicedQty, allowedQty);
        assertThat(result).isEqualTo(expectedResult);
    }

    @ParameterizedTest(name = "[{index}] Qty: {0}, InvPrice: {1}, POPrice: {2}, MaxQty: {3}, Tol: {4}% -> Status: {5}")
    @CsvSource({
            "10.0, 100.00, 100.00, 10.0, 1.0, AUTO_MATCHED",     // 1. Perfect Match
            "10.0, 100.80, 100.00, 10.0, 1.0, AUTO_MATCHED",     // 2. Price within tolerance (+0.8%)
            "10.0, 102.50, 100.00, 10.0, 1.0, DISCREPANCY_HOLD", // 3. Price exceeds tolerance (+2.5%)
            "12.0, 100.00, 100.00, 10.0, 1.0, DISCREPANCY_HOLD", // 4. Quantity exceeds allowed (12 > 10)
            "12.0, 105.00, 100.00, 10.0, 1.0, DISCREPANCY_HOLD"  // 5. Both Price and Quantity Discrepancy
    })
    @DisplayName("Should evaluate complete 3-Way match line item status across all decision branches")
    void shouldEvaluateLineItemBranches(
            BigDecimal invoicedQty, BigDecimal invoicedPrice, BigDecimal poPrice,
            BigDecimal maxAllowedQty, BigDecimal priceTolerancePct, InvoiceMatchStatus expectedStatus) {
        ThreeWayMatchingEngine.MatchResult result = ThreeWayMatchingEngine.evaluateLineItem(
                invoicedQty, invoicedPrice, poPrice, maxAllowedQty, priceTolerancePct
        );
        assertThat(result.status()).isEqualTo(expectedStatus);
        if (expectedStatus == InvoiceMatchStatus.AUTO_MATCHED) {
            assertThat(result.isMatched()).isTrue();
            assertThat(result.varianceReason()).isNull();
        } else {
            assertThat(result.isMatched()).isFalse();
            assertThat(result.varianceReason()).isNotBlank();
        }
    }
}
