package com.enterprise.spendsync.matching.domain;

import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.ThreeWayMatchingEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ThreeWayMatchingEngine Pure Unit Tests (Touchless Evaluation)")
class ThreeWayMatchingEngineTest {

    @Test
    @DisplayName("TC-07-01: Perfect match produces AUTO_MATCHED")
    void shouldProduceAutoMatchedOnExactMatch() {
        BigDecimal invoicedQty = new BigDecimal("10.0000");
        BigDecimal invoicedPrice = new BigDecimal("500.0000");
        BigDecimal poPrice = new BigDecimal("500.0000");
        BigDecimal allowedQty = new BigDecimal("10.0000");

        ThreeWayMatchingEngine.MatchResult result = ThreeWayMatchingEngine.evaluateLineItem(
                invoicedQty, invoicedPrice, poPrice, allowedQty, new BigDecimal("1.00")
        );

        assertThat(result.isMatched()).isTrue();
        assertThat(result.status()).isEqualTo(InvoiceMatchStatus.AUTO_MATCHED);
        assertThat(result.varianceReason()).isNull();
    }

    @Test
    @DisplayName("TC-07-05: Price exceeding tolerance produces DISCREPANCY_HOLD")
    void shouldProduceDiscrepancyOnPriceVariance() {
        BigDecimal invoicedQty = new BigDecimal("10.0000");
        BigDecimal invoicedPrice = new BigDecimal("520.0000"); // 4% higher (> 1%)
        BigDecimal poPrice = new BigDecimal("500.0000");
        BigDecimal allowedQty = new BigDecimal("10.0000");

        ThreeWayMatchingEngine.MatchResult result = ThreeWayMatchingEngine.evaluateLineItem(
                invoicedQty, invoicedPrice, poPrice, allowedQty, new BigDecimal("1.00")
        );

        assertThat(result.isMatched()).isFalse();
        assertThat(result.status()).isEqualTo(InvoiceMatchStatus.DISCREPANCY_HOLD);
        assertThat(result.varianceReason()).contains("Price discrepancy");
    }

    @Test
    @DisplayName("TC-07-06: Invoiced quantity exceeding accepted quantity produces DISCREPANCY_HOLD")
    void shouldProduceDiscrepancyOnQuantityVariance() {
        BigDecimal invoicedQty = new BigDecimal("12.0000"); // Invoiced 12 > Accepted 10
        BigDecimal invoicedPrice = new BigDecimal("500.0000");
        BigDecimal poPrice = new BigDecimal("500.0000");
        BigDecimal allowedQty = new BigDecimal("10.0000");

        ThreeWayMatchingEngine.MatchResult result = ThreeWayMatchingEngine.evaluateLineItem(
                invoicedQty, invoicedPrice, poPrice, allowedQty, new BigDecimal("1.00")
        );

        assertThat(result.isMatched()).isFalse();
        assertThat(result.status()).isEqualTo(InvoiceMatchStatus.DISCREPANCY_HOLD);
        assertThat(result.varianceReason()).contains("Quantity discrepancy");
    }

    @Test
    @DisplayName("TC-07-04: Penny rounding difference (<= 0.05 TL) is absorbed as matched")
    void shouldAbsorbPennyDifference() {
        BigDecimal invoicedPrice = new BigDecimal("500.03");
        BigDecimal poPrice = new BigDecimal("500.00");

        boolean withinTolerance = ThreeWayMatchingEngine.isPriceWithinTolerance(invoicedPrice, poPrice, BigDecimal.ZERO);
        assertThat(withinTolerance).isTrue();
    }
}
