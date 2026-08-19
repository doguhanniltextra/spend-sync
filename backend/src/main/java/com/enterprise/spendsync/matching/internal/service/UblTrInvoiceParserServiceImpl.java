package com.enterprise.spendsync.matching.internal.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class UblTrInvoiceParserServiceImpl implements UblTrInvoiceParserService {

    @Override
    public ParsedUblInvoice parseUblXml(InputStream xmlInputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            // ISO 27001 / OWASP: XXE and Entity Expansion Protection
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlInputStream);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            // Extract Header Elements
            String invoiceNumber = getTagValueByLocalName(root, "ID");
            String ettn = getTagValueByLocalName(root, "UUID");
            String profileId = getTagValueOrDefault(root, "ProfileID", "TICARI_FATURA");
            String invoiceTypeCode = getTagValueOrDefault(root, "InvoiceTypeCode", "SATIS");

            String issueDateStr = getTagValueByLocalName(root, "IssueDate");
            LocalDate issueDate = issueDateStr != null && !issueDateStr.isBlank()
                    ? LocalDate.parse(issueDateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
                    : LocalDate.now();

            String currency = getTagValueOrDefault(root, "DocumentCurrencyCode", "TRY");

            // PO Reference
            String poNumber = extractPoReference(root);

            // Supplier & Customer Tax Numbers
            String supplierTaxNumber = extractPartyTaxNumber(root, "AccountingSupplierParty");
            String buyerTaxNumber = extractPartyTaxNumber(root, "AccountingCustomerParty");

            // Monetary Totals
            BigDecimal subtotalAmount = parseBigDecimal(getTagValueByLocalName(root, "LineExtensionAmount"), BigDecimal.ZERO);
            BigDecimal taxAmount = extractTaxTotal(root, "TaxTotal");
            BigDecimal withholdingTaxAmount = extractTaxTotal(root, "WithholdingTaxTotal");
            BigDecimal totalAmount = parseBigDecimal(getTagValueByLocalName(root, "TaxInclusiveAmount"), subtotalAmount.add(taxAmount));
            BigDecimal payableAmount = parseBigDecimal(getTagValueByLocalName(root, "PayableAmount"), totalAmount.subtract(withholdingTaxAmount));

            // Embedded XSLT
            String embeddedXslt = extractEmbeddedXslt(root);

            // Line Items
            List<ParsedUblLineItem> lineItems = extractLineItems(root);

            return new ParsedUblInvoice(
                    invoiceNumber,
                    ettn,
                    profileId,
                    invoiceTypeCode,
                    issueDate,
                    currency,
                    poNumber,
                    supplierTaxNumber,
                    buyerTaxNumber,
                    subtotalAmount,
                    taxAmount,
                    withholdingTaxAmount,
                    totalAmount,
                    payableAmount,
                    embeddedXslt,
                    lineItems
            );

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse UBL-TR e-Invoice XML: " + e.getMessage(), e);
        }
    }

    private String getTagValueByLocalName(Element element, String localName) {
        NodeList list = element.getElementsByTagNameNS("*", localName);
        if (list.getLength() > 0 && list.item(0) != null) {
            return list.item(0).getTextContent().trim();
        }
        // Fallback without namespace
        list = element.getElementsByTagName(localName);
        if (list.getLength() > 0 && list.item(0) != null) {
            return list.item(0).getTextContent().trim();
        }
        return null;
    }

    private String getTagValueOrDefault(Element element, String localName, String defaultValue) {
        String val = getTagValueByLocalName(element, localName);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    private String extractPoReference(Element root) {
        NodeList orderRefs = root.getElementsByTagNameNS("*", "OrderReference");
        if (orderRefs.getLength() > 0 && orderRefs.item(0) instanceof Element orderEl) {
            return getTagValueByLocalName(orderEl, "ID");
        }
        return null;
    }

    private String extractPartyTaxNumber(Element root, String partyTag) {
        NodeList parties = root.getElementsByTagNameNS("*", partyTag);
        if (parties.getLength() > 0 && parties.item(0) instanceof Element partyEl) {
            return getTagValueByLocalName(partyEl, "ID");
        }
        return null;
    }

    private BigDecimal extractTaxTotal(Element root, String taxTag) {
        NodeList taxTotals = root.getElementsByTagNameNS("*", taxTag);
        if (taxTotals.getLength() > 0 && taxTotals.item(0) instanceof Element taxEl) {
            String val = getTagValueByLocalName(taxEl, "TaxAmount");
            return parseBigDecimal(val, BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    private String extractEmbeddedXslt(Element root) {
        NodeList docs = root.getElementsByTagNameNS("*", "AdditionalDocumentReference");
        for (int i = 0; i < docs.getLength(); i++) {
            if (docs.item(i) instanceof Element el) {
                String docType = getTagValueByLocalName(el, "DocumentType");
                if ("XSLT".equalsIgnoreCase(docType)) {
                    return getTagValueByLocalName(el, "EmbeddedDocumentBinaryObject");
                }
            }
        }
        return null;
    }

    private List<ParsedUblLineItem> extractLineItems(Element root) {
        List<ParsedUblLineItem> items = new ArrayList<>();
        NodeList lines = root.getElementsByTagNameNS("*", "InvoiceLine");

        for (int i = 0; i < lines.getLength(); i++) {
            if (lines.item(i) instanceof Element lineEl) {
                int lineNum = i + 1;
                String idVal = getTagValueByLocalName(lineEl, "ID");
                if (idVal != null) {
                    try { lineNum = Integer.parseInt(idVal); } catch (NumberFormatException ignored) {}
                }

                String description = getTagValueByLocalName(lineEl, "Name");
                if (description == null) {
                    description = getTagValueByLocalName(lineEl, "Description");
                }

                String qtyStr = getTagValueByLocalName(lineEl, "InvoicedQuantity");
                BigDecimal quantity = parseBigDecimal(qtyStr, BigDecimal.ONE);

                String priceStr = getTagValueByLocalName(lineEl, "PriceAmount");
                BigDecimal unitPrice = parseBigDecimal(priceStr, BigDecimal.ZERO);

                String lineTotalStr = getTagValueByLocalName(lineEl, "LineExtensionAmount");
                BigDecimal lineTotal = parseBigDecimal(lineTotalStr, quantity.multiply(unitPrice));

                // Tax Subtotals
                BigDecimal taxRate = parseBigDecimal(getTagValueByLocalName(lineEl, "Percent"), new BigDecimal("20.00"));
                BigDecimal taxAmount = parseBigDecimal(getTagValueByLocalName(lineEl, "TaxAmount"), lineTotal.multiply(taxRate).divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));

                // Tevkifat check
                String tevkifatCode = null;
                String tevkifatRate = null;
                BigDecimal tevkifatAmount = BigDecimal.ZERO;

                NodeList withholdingList = lineEl.getElementsByTagNameNS("*", "WithholdingTaxTotal");
                if (withholdingList.getLength() > 0 && withholdingList.item(0) instanceof Element wEl) {
                    tevkifatAmount = parseBigDecimal(getTagValueByLocalName(wEl, "TaxAmount"), BigDecimal.ZERO);
                    tevkifatCode = getTagValueByLocalName(wEl, "TaxTypeCode");
                    tevkifatRate = getTagValueByLocalName(wEl, "Percent");
                }

                items.add(new ParsedUblLineItem(
                        lineNum,
                        description != null ? description : "Product / Service Line Item",
                        quantity,
                        "PIECE",
                        unitPrice,
                        taxRate,
                        taxAmount,
                        tevkifatCode,
                        tevkifatRate,
                        tevkifatAmount,
                        lineTotal
                ));
            }
        }
        return items;
    }

    private BigDecimal parseBigDecimal(String value, BigDecimal defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
