package com.enterprise.spendsync.matching.domain;

import com.enterprise.spendsync.matching.internal.domain.ThreeWayMatchingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MatchingToleranceRule Parameterized Boundary Tests")
class MatchingToleranceRuleTest {

    @ParameterizedTest(name = "Contract: {0}, Invoiced: {1}, Tolerance: {2}%, Expected Within: {3}")
    @CsvSource({
            "100.00, 100.00, 1.00, true",   // Exact match
            "100.00, 101.00, 1.00, true",   // Exactly +1.00%
            "100.00, 99.00, 1.00, true",    // Exactly -1.00%
            "100.00, 101.01, 1.00, false",  // +1.01% (exceeds)
            "100.00, 98.99, 1.00, false",   // -1.01% (exceeds)
            "1000.00, 1020.00, 2.00, true", // Exactly +2.00% under 2% tolerance
            "1000.00, 1020.01, 2.00, false" // +2.001% exceeds 2% tolerance
    })
    @DisplayName("Should validate price variance percentage boundary thresholds")
    void shouldEvaluatePriceVarianceBoundaries(String contractPrice, String invoicedPrice, String tolerancePct, boolean expectedWithin) {
        BigDecimal poPrice = new BigDecimal(contractPrice);
        BigDecimal invPrice = new BigDecimal(invoicedPrice);
        BigDecimal tolPct = new BigDecimal(tolerancePct);

        boolean actual = ThreeWayMatchingEngine.isPriceWithinTolerance(invPrice, poPrice, tolPct);
        assertThat(actual).isEqualTo(expectedWithin);
    }

    @ParameterizedTest(name = "Invoiced: {0}, Allowed: {1}, Expected Within: {2}")
    @CsvSource({
            "10.00, 10.00, true",   // Exact quantity match
            "5.00, 10.00, true",    // Partial quantity match
            "10.01, 10.00, false",  // Over quantity by 0.01
            "15.00, 10.00, false"   // Over quantity by 5.00
    })
    @DisplayName("Should validate quantity variance ceiling")
    void shouldEvaluateQuantityVarianceCeiling(String invoiced, String allowed, boolean expectedWithin) {
        BigDecimal invQty = new BigDecimal(invoiced);
        BigDecimal allowQty = new BigDecimal(allowed);

        boolean actual = ThreeWayMatchingEngine.isQuantityWithinTolerance(invQty, allowQty);
        assertThat(actual).isEqualTo(expectedWithin);
    }
}
