package com.enterprise.spendsync.vendorportal.domain;

import com.enterprise.spendsync.vendorportal.internal.domain.TurkishWithholdingTaxCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Turkish Withholding Tax (KDV Tevkifatı) Parameterized Calculations")
class TurkishWithholdingTaxCalculatorTest {

    @ParameterizedTest(name = "Matrah: {0}, KDV: %{1}, Kod: {2}, Oran: {3} -> Tevkifat: {4}, Net Ödenecek: {5}")
    @CsvSource({
            // TC-09-04: Standart %20 KDV, 0 Tevkifat
            "10000.00, 20.00, NONE, , 0.0000, 12000.0000",
            // TC-09-05: 601 Yapım İşleri 2/10 Tevkifat (2000 * 2/10 = 400 Tevkifat, 1600 KDV -> 11600)
            "10000.00, 20.00, 601, 2/10, 400.0000, 11600.0000",
            // TC-09-06: 608 Temizlik Hizmetleri 5/10 Tevkifat (2000 * 5/10 = 1000 Tevkifat, 1000 KDV -> 11000)
            "10000.00, 20.00, 608, 5/10, 1000.0000, 11000.0000",
            // TC-09-07: 627 Bilişim / Danışmanlık 7/10 Tevkifat (2000 * 7/10 = 1400 Tevkifat, 600 KDV -> 10600)
            "10000.00, 20.00, 627, 7/10, 1400.0000, 10600.0000",
            // TC-09-08: 610 Taşımacılık 9/10 Tevkifat (2000 * 9/10 = 1800 Tevkifat, 200 KDV -> 10200)
            "10000.00, 20.00, 610, 9/10, 1800.0000, 10200.0000",
            // Code auto-resolution without explicit rate
            "10000.00, 20.00, 601, , 400.0000, 11600.0000",
            "10000.00, 20.00, 627, , 1400.0000, 10600.0000"
    })
    @DisplayName("Should accurately calculate Turkish Withholding Tax according to GİB formulas")
    void shouldCalculateWithholdingTax(String base, String vatRateStr, String code, String rate, String expectedWithholding, String expectedPayable) {
        BigDecimal baseAmount = new BigDecimal(base);
        BigDecimal vatRate = new BigDecimal(vatRateStr);

        TurkishWithholdingTaxCalculator.CalculationResult result =
                TurkishWithholdingTaxCalculator.calculate(baseAmount, vatRate, code, rate);

        assertThat(result.withholdingAmount()).isEqualByComparingTo(new BigDecimal(expectedWithholding));
        assertThat(result.totalPayableAmount()).isEqualByComparingTo(new BigDecimal(expectedPayable));
    }
}
