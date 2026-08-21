package com.enterprise.spendsync.vendorportal.service;

import com.enterprise.spendsync.vendorportal.internal.domain.TurkishWithholdingTaxCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class TurkishWithholdingTaxParameterizedTest {

    @ParameterizedTest(name = "[{index}] Code: {0}, Base: {1}, VAT: %{2}, Explicit: {3} -> Withheld: {4}, Total: {5}")
    @CsvSource({
            "601, 10000.00, 20.00, ,       400.00,  11600.00", // 601 (2/10): VAT=2000, Withheld=400, Accrued=1600, Total=11600
            "608, 10000.00, 20.00, ,      1000.00,  11000.00", // 608 (5/10): VAT=2000, Withheld=1000, Accrued=1000, Total=11000
            "627, 10000.00, 20.00, ,      1400.00,  10600.00", // 627 (7/10): VAT=2000, Withheld=1400, Accrued=600, Total=10600
            "610, 10000.00, 20.00, ,      1800.00,  10200.00", // 610 (9/10): VAT=2000, Withheld=1800, Accrued=200, Total=10200
            "NONE, 10000.00, 20.00, ,        0.00,  12000.00", // NONE: VAT=2000, Withheld=0, Total=12000
            "999, 10000.00, 20.00, ,         0.00,  12000.00", // Unknown code without explicit rate
            ",    10000.00, 20.00, 3/10,   600.00,  11400.00", // Explicit 3/10 rate: VAT=2000, Withheld=600, Total=11400
            ",    10000.00, 10.00, 5/10,   500.00,  10500.00"  // Explicit 5/10 with 10% VAT
    })
    @DisplayName("Should evaluate Turkish VAT withholding calculations across all GİB rate combinations")
    void shouldCalculateWithholdingTax(
            String tevkifatCode, BigDecimal baseAmount, BigDecimal vatRate, String explicitRate,
            BigDecimal expectedWithheldAmount, BigDecimal expectedTotalPayable) {

        TurkishWithholdingTaxCalculator.CalculationResult result =
                TurkishWithholdingTaxCalculator.calculate(baseAmount, vatRate, tevkifatCode, explicitRate);

        assertThat(result.withholdingAmount()).isEqualByComparingTo(expectedWithheldAmount);
        assertThat(result.totalPayableAmount()).isEqualByComparingTo(expectedTotalPayable);
    }
}
