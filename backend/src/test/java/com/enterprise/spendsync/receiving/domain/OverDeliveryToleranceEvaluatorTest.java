package com.enterprise.spendsync.receiving.domain;

import com.enterprise.spendsync.receiving.internal.domain.OverDeliveryToleranceEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OverDeliveryToleranceEvaluator Pure Unit Tests")
class OverDeliveryToleranceEvaluatorTest {

    @ParameterizedTest(name = "Ordered: {0}, Tolerance: {1}%, Expected Max: {2}")
    @CsvSource({
            "100.0000, 5.00, 105.0000",
            "100.0000, 10.00, 110.0000",
            "50.0000, 0.00, 50.0000",
            "200.0000, 2.50, 205.0000"
    })
    @DisplayName("Should correctly calculate maximum allowed quantity with tolerance ceiling")
    void shouldCalculateMaxAllowedQuantity(String ordered, String tolerance, String expectedMax) {
        BigDecimal orderedQty = new BigDecimal(ordered);
        BigDecimal tolerancePct = new BigDecimal(tolerance);
        BigDecimal expected = new BigDecimal(expectedMax);

        BigDecimal actual = OverDeliveryToleranceEvaluator.calculateMaxAllowedQuantity(orderedQty, tolerancePct);
        assertThat(actual).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("Should accept quantity within 5% tolerance window")
    void shouldAcceptWithinTolerance() {
        BigDecimal ordered = new BigDecimal("100.0000");
        BigDecimal tolerance = new BigDecimal("5.00");
        BigDecimal previous = new BigDecimal("50.0000");
        BigDecimal newly = new BigDecimal("54.0000"); // Total 104 <= 105

        boolean within = OverDeliveryToleranceEvaluator.isWithinTolerance(ordered, tolerance, previous, newly);
        assertThat(within).isTrue();
    }

    @Test
    @DisplayName("Should reject quantity exceeding tolerance window")
    void shouldRejectExceedingTolerance() {
        BigDecimal ordered = new BigDecimal("100.0000");
        BigDecimal tolerance = new BigDecimal("5.00");
        BigDecimal previous = new BigDecimal("50.0000");
        BigDecimal newly = new BigDecimal("56.0000"); // Total 106 > 105

        boolean within = OverDeliveryToleranceEvaluator.isWithinTolerance(ordered, tolerance, previous, newly);
        assertThat(within).isFalse();
    }

    @Test
    @DisplayName("Should return zero max quantity for zero or negative ordered quantity")
    void shouldHandleZeroOrNegativeOrdered() {
        assertThat(OverDeliveryToleranceEvaluator.calculateMaxAllowedQuantity(BigDecimal.ZERO, new BigDecimal("5.0")))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(OverDeliveryToleranceEvaluator.calculateMaxAllowedQuantity(new BigDecimal("-10.0"), new BigDecimal("5.0")))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
