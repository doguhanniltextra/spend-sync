package com.enterprise.spendsync.shared.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PII & Sensitive Data Log Masking Converter Tests")
class PiiMaskingConverterTest {

    private final PiiMaskingConverter converter = new PiiMaskingConverter();

    @ParameterizedTest
    @CsvSource({
            "'User TCKN is 12345678901 in record', 'User TCKN is *******8901 in record'",
            "'Driver identity: 98765432109', 'Driver identity: *******2109'"
    })
    @DisplayName("Should mask 11-digit TCKN preserving last 4 digits")
    void shouldMaskTckn(String input, String expected) {
        String masked = PiiMaskingConverter.mask(input);
        assertThat(masked).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "'Vendor VKN is 1234567890 for invoice', 'Vendor VKN is 12******90 for invoice'",
            "'Company tax ID 9876543210 registered', 'Company tax ID 98******10 registered'"
    })
    @DisplayName("Should mask 10-digit VKN preserving first 2 and last 2 digits")
    void shouldMaskVkn(String input, String expected) {
        String masked = PiiMaskingConverter.mask(input);
        assertThat(masked).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should mask 26-digit Turkish IBAN preserving country code and ending")
    void shouldMaskIban() {
        String input = "Payment dispatched to IBAN TR330006100511223344556677 successfully";
        String masked = PiiMaskingConverter.mask(input);
        assertThat(masked).contains("TR33 0006 **** **** **** 77");
        assertThat(masked).doesNotContain("1005112233445566");
    }

    @Test
    @DisplayName("Should mask 16-digit credit card PAN")
    void shouldMaskCreditCard() {
        String input = "Corporate card 4543 6012 3456 7890 charged for travel";
        String masked = PiiMaskingConverter.mask(input);
        assertThat(masked).contains("4543 60** **** 7890");
    }

    @Test
    @DisplayName("Should mask Bearer authorization token")
    void shouldMaskBearerToken() {
        String input = "Outgoing request with header Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.sig";
        String masked = PiiMaskingConverter.mask(input);
        assertThat(masked).isEqualTo("Outgoing request with header Authorization: Bearer ********");
    }

    @Test
    @DisplayName("Should mask sensitive JSON fields like password and secret")
    void shouldMaskSensitiveJsonFields() {
        String input = "{\"username\": \"admin\", \"password\": \"SuperSecret123!\", \"rawUblXml\": \"<Invoice>Secret</Invoice>\"}";
        String masked = PiiMaskingConverter.mask(input);
        assertThat(masked).contains("\"password\":\"********\"");
        assertThat(masked).contains("\"rawUblXml\":\"********\"");
        assertThat(masked).doesNotContain("SuperSecret123!");
    }

    @Test
    @DisplayName("Should return null or blank string unchanged")
    void shouldHandleNullOrBlank() {
        assertThat(PiiMaskingConverter.mask(null)).isNull();
        assertThat(PiiMaskingConverter.mask("")).isEmpty();
        assertThat(converter.transform(null, null)).isNull();
        assertThat(converter.transform(null, "")).isEmpty();
    }
}
