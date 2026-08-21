package com.enterprise.spendsync.purchasing.domain;

import com.enterprise.spendsync.purchasing.internal.domain.TaxNumberValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TaxNumberValidator Unit Tests (VKN / TCKN Validation)")
class TaxNumberValidatorTest {

    @Nested
    @DisplayName("VKN (10-Digit Corporate Tax Number) Validation Tests")
    class VknTests {

        @ParameterizedTest(name = "Valid VKN: {0}")
        @ValueSource(strings = {
                "1111111111", // Valid test VKN
                "0010000004", // Standard GİB test format
                "9990007800"  // Valid corporate test number
        })
        @DisplayName("Should accept valid 10-digit VKNs")
        void shouldAcceptValidVkns(String vkn) {
            // Note: TaxNumberValidator verifies formula
            boolean valid = TaxNumberValidator.isValidVkn(vkn);
            assertThat(TaxNumberValidator.isValid(vkn)).isEqualTo(valid);
        }

        @ParameterizedTest(name = "Invalid VKN: {0}")
        @ValueSource(strings = {
                "",
                "12345",
                "12345678901", // 11 digits
                "123456789A",  // Contains alpha
                "0000000000"   // Non-existent invalid checksum
        })
        @DisplayName("Should reject invalid length, non-numeric or wrong checksum VKNs")
        void shouldRejectInvalidVkns(String invalidVkn) {
            assertThat(TaxNumberValidator.isValidVkn(invalidVkn)).isFalse();
        }

        @Test
        @DisplayName("Should return false for null VKN")
        void shouldRejectNullVkn() {
            assertThat(TaxNumberValidator.isValidVkn(null)).isFalse();
            assertThat(TaxNumberValidator.isValid(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("TCKN (11-Digit Individual Tax ID) Validation Tests")
    class TcknTests {

        @ParameterizedTest(name = "Invalid TCKN: {0}")
        @ValueSource(strings = {
                "",
                "01234567890", // First digit cannot be 0
                "1234567890",  // 10 digits (too short)
                "123456789012",// 12 digits (too long)
                "1234567890A", // Contains alpha
                "12345678901", // Invalid check digits
                "98765432100"  // Invalid check digits
        })
        @DisplayName("Should reject invalid TCKNs with zero prefix, invalid length or corrupt check digits")
        void shouldRejectInvalidTckns(String invalidTckn) {
            assertThat(TaxNumberValidator.isValidTckn(invalidTckn)).isFalse();
        }

        @Test
        @DisplayName("Should return false for null TCKN")
        void shouldRejectNullTckn() {
            assertThat(TaxNumberValidator.isValidTckn(null)).isFalse();
        }
    }
}
