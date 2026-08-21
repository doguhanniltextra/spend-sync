package com.enterprise.spendsync.vendorportal.service;

import com.enterprise.spendsync.matching.internal.service.UblTrInvoiceParserService;
import com.enterprise.spendsync.matching.internal.service.UblTrInvoiceParserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UBL-TR 1.2 e-Invoice XML Parser Pure Unit Tests")
class UblXmlParserServiceTest {

    private UblTrInvoiceParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new UblTrInvoiceParserServiceImpl();
    }

    @Test
    @DisplayName("TC-09-09: Parses standard GİB UBL-TR 1.2 XML e-Invoice headers, PO reference and amounts")
    void shouldParseValidUblTrXml() {
        String ublXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
                         xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
                         xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
                    <cbc:ID>GIB2026000000001</cbc:ID>
                    <cbc:UUID>a1b2c3d4-e5f6-7890-abcd-ef1234567890</cbc:UUID>
                    <cbc:IssueDate>2026-08-21</cbc:IssueDate>
                    <cbc:InvoiceTypeCode>SATIS</cbc:InvoiceTypeCode>
                    <cbc:DocumentCurrencyCode>TRY</cbc:DocumentCurrencyCode>
                    <cac:OrderReference>
                        <cbc:ID>PO-2026-00001</cbc:ID>
                    </cac:OrderReference>
                    <cac:AccountingSupplierParty>
                        <cac:Party>
                            <cac:PartyIdentification>
                                <cbc:ID>1234567890</cbc:ID>
                            </cac:PartyIdentification>
                        </cac:Party>
                    </cac:AccountingSupplierParty>
                    <cac:AccountingCustomerParty>
                        <cac:Party>
                            <cac:PartyIdentification>
                                <cbc:ID>9876543210</cbc:ID>
                            </cac:PartyIdentification>
                        </cac:Party>
                    </cac:AccountingCustomerParty>
                    <cac:TaxTotal>
                        <cbc:TaxAmount currencyID="TRY">20000.00</cbc:TaxAmount>
                    </cac:TaxTotal>
                    <cac:LegalMonetaryTotal>
                        <cbc:LineExtensionAmount currencyID="TRY">100000.00</cbc:LineExtensionAmount>
                        <cbc:TaxInclusiveAmount currencyID="TRY">120000.00</cbc:TaxInclusiveAmount>
                        <cbc:PayableAmount currencyID="TRY">120000.00</cbc:PayableAmount>
                    </cac:LegalMonetaryTotal>
                </Invoice>
                """;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(ublXml.getBytes(StandardCharsets.UTF_8));
        UblTrInvoiceParserService.ParsedUblInvoice parsed = parserService.parseUblXml(inputStream);

        assertThat(parsed).isNotNull();
        assertThat(parsed.invoiceNumber()).isEqualTo("GIB2026000000001");
        assertThat(parsed.ettn()).isEqualTo("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        assertThat(parsed.poNumber()).isEqualTo("PO-2026-00001");
        assertThat(parsed.supplierTaxNumber()).isEqualTo("1234567890");
        assertThat(parsed.buyerTaxNumber()).isEqualTo("9876543210");
        assertThat(parsed.subtotalAmount()).isEqualByComparingTo("100000.00");
        assertThat(parsed.taxAmount()).isEqualByComparingTo("20000.00");
        assertThat(parsed.payableAmount()).isEqualByComparingTo("120000.00");
    }

    @Test
    @DisplayName("TC-09-11: Malformed or unparseable XML throws ResponseStatusException")
    void shouldRejectMalformedXml() {
        String invalidXml = "<Invoice><unclosedTag>";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parserService.parseUblXml(inputStream))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Failed to parse UBL-TR e-Invoice XML");
    }
}
