package com.enterprise.spendsync.matching.internal.service;

import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoiceLineItem;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Service
public class GibXsltRendererServiceImpl implements GibXsltRendererService {

    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(new Locale("tr", "TR")));

    @Override
    public String renderInvoiceHtml(SupplierInvoice invoice) {
        StringBuilder itemsHtml = new StringBuilder();
        for (SupplierInvoiceLineItem li : invoice.getLineItems()) {
            String tevkifatText = li.getTevkifatCode() != null
                    ? li.getTevkifatCode() + " (" + li.getTevkifatRate() + ")"
                    : "-";

            itemsHtml.append(String.format("""
                <tr>
                    <td style="text-align: center; border: 1px solid #cbd5e1; padding: 8px;">%d</td>
                    <td style="border: 1px solid #cbd5e1; padding: 8px;">%s</td>
                    <td style="text-align: right; border: 1px solid #cbd5e1; padding: 8px;">%s</td>
                    <td style="text-align: right; border: 1px solid #cbd5e1; padding: 8px;">%s %s</td>
                    <td style="text-align: center; border: 1px solid #cbd5e1; padding: 8px;">%%%s</td>
                    <td style="text-align: right; border: 1px solid #cbd5e1; padding: 8px;">%s %s</td>
                    <td style="text-align: center; border: 1px solid #cbd5e1; padding: 8px;">%s</td>
                    <td style="text-align: right; border: 1px solid #cbd5e1; padding: 8px;"><b>%s %s</b></td>
                </tr>
            """,
                    li.getPurchaseOrderLineItem() != null ? li.getPurchaseOrderLineItem().getLineNumber() : 1,
                    escapeHtml(li.getPurchaseOrderLineItem() != null ? li.getPurchaseOrderLineItem().getItemDescription() : "Product / Service"),
                    li.getInvoicedQuantity(),
                    CURRENCY_FMT.format(li.getUnitPrice()),
                    invoice.getCurrency(),
                    li.getTaxRate(),
                    CURRENCY_FMT.format(li.getTaxAmount()),
                    invoice.getCurrency(),
                    tevkifatText,
                    CURRENCY_FMT.format(li.getLineTotalAmount()),
                    invoice.getCurrency()
            ));
        }

        String poRef = invoice.getPurchaseOrder() != null ? invoice.getPurchaseOrder().getPoNumber() : "N/A";
        String sellerName = invoice.getVendor() != null ? invoice.getVendor().getName() : "Vendor";
        String sellerTax = invoice.getVendor() != null ? invoice.getVendor().getTaxNumber() : "-";
        String sellerTaxOffice = invoice.getVendor() != null ? invoice.getVendor().getTaxOffice() : "-";
        String sellerAddress = invoice.getVendor() != null ? invoice.getVendor().getAddress() + ", " + invoice.getVendor().getCity() : "-";

        String buyerName = invoice.getLegalEntity() != null ? invoice.getLegalEntity().getName() : "Buyer Company";
        String buyerTax = invoice.getLegalEntity() != null ? invoice.getLegalEntity().getTaxNumber() : "-";
        String buyerAddress = invoice.getLegalEntity() != null ? invoice.getLegalEntity().getRegisteredAddress() : "-";

        return String.format("""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Electronic Invoice - %s</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #1e293b; background-color: #f8fafc; margin: 0; padding: 20px; }
        .invoice-container { max-width: 900px; margin: 0 auto; background: #ffffff; border: 2px solid #0f172a; padding: 24px; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); }
        .gib-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #0f172a; padding-bottom: 12px; margin-bottom: 16px; }
        .gib-logo { font-size: 24px; font-weight: 800; color: #dc2626; letter-spacing: -0.5px; }
        .gib-badge { background-color: #f1f5f9; border: 1px solid #cbd5e1; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
        .party-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
        .party-box { border: 1px solid #cbd5e1; padding: 12px; border-radius: 4px; background-color: #fafafa; }
        .party-title { font-weight: 700; font-size: 14px; text-transform: uppercase; color: #334155; margin-bottom: 8px; border-bottom: 1px solid #e2e8f0; padding-bottom: 4px; }
        table.items-table { width: 100%%; border-collapse: collapse; margin-bottom: 20px; font-size: 13px; }
        table.items-table th { background-color: #0f172a; color: #ffffff; padding: 8px; border: 1px solid #0f172a; text-align: center; }
        .totals-section { display: flex; justify-content: flex-end; margin-top: 10px; }
        .totals-table { width: 350px; border-collapse: collapse; font-size: 14px; }
        .totals-table td { padding: 6px 10px; border-bottom: 1px solid #e2e8f0; }
        .totals-table tr.grand-total { font-size: 16px; font-weight: 800; background-color: #f8fafc; border-top: 2px solid #0f172a; }
        .qr-placeholder { border: 2px dashed #94a3b8; width: 100px; height: 100px; display: flex; align-items: center; justify-content: center; font-size: 11px; color: #64748b; text-align: center; }
    </style>
</head>
<body>
    <div class="invoice-container">
        <div class="gib-header">
            <div>
                <div class="gib-logo">e-INVOICE</div>
                <div style="font-size: 12px; color: #64748b;">ELECTRONIC INVOICE UBL-TR 1.2 STANDARD</div>
            </div>
            <div style="text-align: right;">
                <div style="font-size: 18px; font-weight: 700;">%s</div>
                <div class="gib-badge">Profile: %s | Type: %s</div>
            </div>
        </div>

        <div style="display: flex; justify-content: space-between; margin-bottom: 16px; font-size: 13px;">
            <div><b>ETTN / UUID:</b> %s</div>
            <div><b>Issue Date:</b> %s | <b>Purchase Order No (PO):</b> %s</div>
        </div>

        <div class="party-grid">
            <div class="party-box">
                <div class="party-title">SUPPLIER DETAILS</div>
                <div><b>%s</b></div>
                <div>Tax ID / VAT No: %s</div>
                <div>Tax Office: %s</div>
                <div>Address: %s</div>
            </div>
            <div class="party-box">
                <div class="party-title">BUYER DETAILS</div>
                <div><b>%s</b></div>
                <div>Tax ID / VAT No: %s</div>
                <div>Address: %s</div>
            </div>
        </div>

        <table class="items-table">
            <thead>
                <tr>
                    <th>Line</th>
                    <th>Item / Service Description</th>
                    <th>Quantity</th>
                    <th>Unit Price</th>
                    <th>VAT Rate</th>
                    <th>VAT Amount</th>
                    <th>Withholding</th>
                    <th>Line Total</th>
                </tr>
            </thead>
            <tbody>
                %s
            </tbody>
        </table>

        <div style="display: flex; justify-content: space-between; align-items: flex-end;">
            <div class="qr-placeholder">DIGITAL QR CODE VERIFICATION</div>
            <div class="totals-section">
                <table class="totals-table">
                    <tr>
                        <td><b>Subtotal (Tax Base):</b></td>
                        <td style="text-align: right;">%s %s</td>
                    </tr>
                    <tr>
                        <td><b>Calculated VAT:</b></td>
                        <td style="text-align: right;">%s %s</td>
                    </tr>
                    <tr>
                        <td><b>Withholding Tax Deduction:</b></td>
                        <td style="text-align: right; color: #dc2626;">-%s %s</td>
                    </tr>
                    <tr class="grand-total">
                        <td><b>PAYABLE TOTAL (NET):</b></td>
                        <td style="text-align: right; color: #0f172a;"><b>%s %s</b></td>
                    </tr>
                </table>
            </div>
        </div>
    </div>
</body>
</html>
        """,
                escapeHtml(invoice.getInvoiceNumber()),
                escapeHtml(invoice.getInvoiceNumber()),
                invoice.getInvoiceProfile().name(),
                invoice.getInvoiceType().name(),
                escapeHtml(invoice.getEttn()),
                invoice.getInvoiceDate(),
                escapeHtml(poRef),
                escapeHtml(sellerName),
                escapeHtml(sellerTax),
                escapeHtml(sellerTaxOffice),
                escapeHtml(sellerAddress),
                escapeHtml(buyerName),
                escapeHtml(buyerTax),
                escapeHtml(buyerAddress),
                itemsHtml.toString(),
                CURRENCY_FMT.format(invoice.getSubtotalAmount()),
                invoice.getCurrency(),
                CURRENCY_FMT.format(invoice.getTaxAmount()),
                invoice.getCurrency(),
                CURRENCY_FMT.format(invoice.getWithholdingTaxAmount()),
                invoice.getCurrency(),
                CURRENCY_FMT.format(invoice.getPayableAmount()),
                invoice.getCurrency()
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
