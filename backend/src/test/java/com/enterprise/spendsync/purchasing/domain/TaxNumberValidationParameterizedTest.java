package com.enterprise.spendsync.purchasing.domain;

import com.enterprise.spendsync.purchasing.internal.domain.TaxNumberValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class TaxNumberValidationParameterizedTest {

    @ParameterizedTest(name = "[{index}] TaxNumber=''{0}'' -> expectedValid={1}")
    @CsvSource(value = {
            "1234567890,true",
            "1234567891,false",
            "11111111110,true",
            "10000000146,true",
            "01234567890,false",
            "12345678901,false",
            "123456789,false",
            "12345A7890,false",
            "NULL,false",
            "'',false"
    }, nullValues = {"NULL"})
    @DisplayName("Should evaluate Tax Number (VKN / TCKN) validity across all decision branches")
    void shouldValidateTaxNumbers(String taxNumber, boolean expectedValid) {
        boolean valid = TaxNumberValidator.isValid(taxNumber);
        assertThat(valid)
                .as("isValid(\"%s\") should be %s", taxNumber, expectedValid)
                .isEqualTo(expectedValid);
    }

    @ParameterizedTest(name = "[{index}] VKN=''{0}'' -> expectedValid={1}")
    @CsvSource(value = {
            "1234567890,true",
            "1234567891,false",
            "123456789,false",
            "NULL,false"
    }, nullValues = {"NULL"})
    @DisplayName("Should validate 10-digit VKN checksum branches directly")
    void shouldValidateVknDirectly(String vkn, boolean expectedValid) {
        boolean valid = TaxNumberValidator.isValidVkn(vkn);
        assertThat(valid)
                .as("isValidVkn(\"%s\") should be %s", vkn, expectedValid)
                .isEqualTo(expectedValid);
    }

    @ParameterizedTest(name = "[{index}] TCKN=''{0}'' -> expectedValid={1}")
    @CsvSource(value = {
            "10000000146,true",
            "11111111110,true",
            "10000000147,false",
            "00000000000,false",
            "NULL,false"
    }, nullValues = {"NULL"})
    @DisplayName("Should validate 11-digit TCKN checksum branches directly")
    void shouldValidateTcknDirectly(String tckn, boolean expectedValid) {
        boolean valid = TaxNumberValidator.isValidTckn(tckn);
        assertThat(valid)
                .as("isValidTckn(\"%s\") should be %s", tckn, expectedValid)
                .isEqualTo(expectedValid);
    }
}
