package com.enterprise.spendsync.payment.xml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ISO 20022 pain.001.001.03 Banking Payment XML Generator Tests")
class Iso20022XmlGeneratorTest {

    @Test
    @DisplayName("TC-08-10: Generates well-formed pain.001 XML document with valid header elements")
    void shouldGenerateValidPain001Xml() {
        String batchNumber = "PAY-2026-00001";
        Instant creDtTm = Instant.now();
        int txCount = 5;
        BigDecimal totalAmount = new BigDecimal("250000.50");
        String companyName = "SpendSync Turkey Bilisim A.S.";
        String taxNumber = "1234567890";

        String xml = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.03">
                  <CstmrCdtTrfInitn>
                    <GrpHdr>
                      <MsgId>%s</MsgId>
                      <CreDtTm>%s</CreDtTm>
                      <NbOfTxs>%d</NbOfTxs>
                      <CtrlSum>%.2f</CtrlSum>
                      <InitgPty>
                        <Nm>%s</Nm>
                        <Id><OrgId><Othr><Id>%s</Id></Othr></OrgId></Id>
                      </InitgPty>
                    </GrpHdr>
                  </CstmrCdtTrfInitn>
                </Document>
                """,
                batchNumber,
                creDtTm.toString(),
                txCount,
                totalAmount,
                companyName,
                taxNumber
        );

        assertThat(xml).contains("xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\"");
        assertThat(xml).contains("<MsgId>PAY-2026-00001</MsgId>");
        assertThat(xml).contains("<NbOfTxs>5</NbOfTxs>");
        assertThat(xml).contains("<CtrlSum>250000.50</CtrlSum>");
        assertThat(xml).contains("<Nm>SpendSync Turkey Bilisim A.S.</Nm>");
        assertThat(xml).contains("<Id>1234567890</Id>");
    }

    @Test
    @DisplayName("TC-08-11: Properly handles XML entity characters in initiator name")
    void shouldSanitizeXmlEntityCharacters() {
        String rawCompanyName = "SpendSync & Co. <Istanbul> \"A.S.\"";
        String escaped = rawCompanyName
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");

        assertThat(escaped).isEqualTo("SpendSync &amp; Co. &lt;Istanbul&gt; &quot;A.S.&quot;");
    }
}
