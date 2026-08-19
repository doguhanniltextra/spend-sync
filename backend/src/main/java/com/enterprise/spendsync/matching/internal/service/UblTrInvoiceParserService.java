package com.enterprise.spendsync.matching.internal.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface UblTrInvoiceParserService {

    ParsedUblInvoice parseUblXml(InputStream xmlInputStream);

    record ParsedUblInvoice(
            String invoiceNumber,
            String ettn,
            String profileId,
            String invoiceTypeCode,
            LocalDate issueDate,
            String currency,
            String poNumber,
            String supplierTaxNumber,
            String buyerTaxNumber,
            BigDecimal subtotalAmount,
            BigDecimal taxAmount,
            BigDecimal withholdingTaxAmount,
            BigDecimal totalAmount,
            BigDecimal payableAmount,
            String embeddedXslt,
            List<ParsedUblLineItem> lineItems
    ) {}

    record ParsedUblLineItem(
            int lineNumber,
            String itemDescription,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            String tevkifatCode,
            String tevkifatRate,
            BigDecimal tevkifatAmount,
            BigDecimal lineTotalAmount
    ) {}
}
