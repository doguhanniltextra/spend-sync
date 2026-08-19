package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;
import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.matching.internal.domain.InvoiceDiscrepancy;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.matching.internal.domain.MatchType;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoiceLineItem;
import com.enterprise.spendsync.matching.internal.repository.InvoiceDiscrepancyRepository;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceLineItemRepository;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.matching.internal.service.GibXsltRendererService;
import com.enterprise.spendsync.matching.internal.service.UblTrInvoiceParserService;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.PoFlipInvoiceRequest;
import com.enterprise.spendsync.vendorportal.dto.SupplierInvoiceDetailResponse;
import com.enterprise.spendsync.vendorportal.dto.SupplierInvoiceResponse;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorUser;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class VendorInvoiceServiceImpl implements VendorInvoiceService {

    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierInvoiceLineItemRepository invoiceLineItemRepository;
    private final InvoiceDiscrepancyRepository discrepancyRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final VendorUserRepository vendorUserRepository;
    private final UblTrInvoiceParserService ublParserService;
    private final GibXsltRendererService xsltRendererService;
    private final AuditService auditService;

    public VendorInvoiceServiceImpl(SupplierInvoiceRepository supplierInvoiceRepository,
                                   SupplierInvoiceLineItemRepository invoiceLineItemRepository,
                                   InvoiceDiscrepancyRepository discrepancyRepository,
                                   PurchaseOrderRepository purchaseOrderRepository,
                                   VendorUserRepository vendorUserRepository,
                                   UblTrInvoiceParserService ublParserService,
                                   GibXsltRendererService xsltRendererService,
                                   AuditService auditService) {
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.vendorUserRepository = vendorUserRepository;
        this.ublParserService = ublParserService;
        this.xsltRendererService = xsltRendererService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public SupplierInvoiceResponse createPoFlipInvoice(UUID poId, PoFlipInvoiceRequest request, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        UUID tenantId = user.getTenant().getId();
        TenantContext.setTenantId(tenantId);

        PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantIdAndVendorId(poId, tenantId, user.getVendor().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found or not assigned to your vendor"));

        // ETTN and Invoice Number uniqueness
        if (supplierInvoiceRepository.existsByTenantIdAndEttn(tenantId, request.ettn())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An invoice with ETTN '" + request.ettn() + "' already exists");
        }
        if (supplierInvoiceRepository.existsByTenantIdAndVendorIdAndInvoiceNumber(tenantId, user.getVendor().getId(), request.invoiceNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice number '" + request.invoiceNumber() + "' has already been registered for your company");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalWithholding = BigDecimal.ZERO;

        boolean isAllServices = true;

        SupplierInvoice invoice = new SupplierInvoice(
                user.getTenant(),
                request.invoiceNumber(),
                request.ettn(),
                request.invoiceDate(),
                request.invoiceType() != null ? request.invoiceType() : InvoiceType.SATIS,
                request.profileId() != null ? request.profileId() : InvoiceProfile.TICARI_FATURA,
                po,
                user.getVendor(),
                po.getLegalEntity(),
                po.getCostCenter(),
                po.getCurrency(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                MatchType.THREE_WAY
        );

        int lineIdx = 1;
        for (PoFlipInvoiceRequest.PoFlipLineItemDto itemDto : request.lineItems()) {
            PurchaseOrderLineItem poLine = po.getLineItems().stream()
                    .filter(l -> l.getId().equals(itemDto.purchaseOrderLineItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PO line item ID: " + itemDto.purchaseOrderLineItemId()));

            // Partial Invoicing ceiling check
            BigDecimal previousInvoicedQty = getAlreadyInvoicedQuantity(poLine.getId(), tenantId);
            BigDecimal remainingQty = poLine.getQuantity().subtract(previousInvoicedQty);

            if (itemDto.invoicedQuantity().compareTo(remainingQty) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Cannot invoice quantity %s on line %d. Maximum remaining invoicable quantity is %s (Total PO: %s, Previously Invoiced: %s)",
                                itemDto.invoicedQuantity(), poLine.getLineNumber(), remainingQty, poLine.getQuantity(), previousInvoicedQty));
            }

            BigDecimal unitPrice = poLine.getUnitPrice();
            BigDecimal lineSubtotal = unitPrice.multiply(itemDto.invoicedQuantity());
            BigDecimal taxRate = itemDto.taxRate() != null ? itemDto.taxRate() : new BigDecimal("20.00");
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            // Tevkifat calculation
            BigDecimal lineWithholding = BigDecimal.ZERO;
            if (itemDto.tevkifatRate() != null && itemDto.tevkifatRate().contains("/")) {
                String[] parts = itemDto.tevkifatRate().split("/");
                if (parts.length == 2) {
                    try {
                        BigDecimal num = new BigDecimal(parts[0].trim());
                        BigDecimal denom = new BigDecimal(parts[1].trim());
                        lineWithholding = lineTax.multiply(num).divide(denom, 4, RoundingMode.HALF_UP);
                    } catch (Exception ignored) {}
                }
            }

            BigDecimal lineTotal = lineSubtotal.add(lineTax);

            subtotal = subtotal.add(lineSubtotal);
            totalTax = totalTax.add(lineTax);
            totalWithholding = totalWithholding.add(lineWithholding);

            // Check if service category
            String cat = poLine.getItemCategory() != null ? poLine.getItemCategory().toUpperCase() : "";
            if (!cat.contains("SERVICE") && !cat.contains("SAAS") && !cat.contains("SUBSCRIPTION") && !cat.contains("CONSULTING")) {
                isAllServices = false;
            }

            SupplierInvoiceLineItem lineItem = new SupplierInvoiceLineItem(
                    user.getTenant(),
                    poLine,
                    null,
                    itemDto.invoicedQuantity(),
                    unitPrice,
                    taxRate,
                    lineTax,
                    itemDto.tevkifatCode(),
                    itemDto.tevkifatRate(),
                    lineWithholding,
                    lineTotal
            );
            invoice.addLineItem(lineItem);
            lineIdx++;
        }

        BigDecimal grandTotal = subtotal.add(totalTax);
        BigDecimal payableAmount = grandTotal.subtract(totalWithholding);

        // Update amounts on invoice entity
        invoice = new SupplierInvoice(
                user.getTenant(),
                request.invoiceNumber(),
                request.ettn(),
                request.invoiceDate(),
                request.invoiceType() != null ? request.invoiceType() : InvoiceType.SATIS,
                request.profileId() != null ? request.profileId() : InvoiceProfile.TICARI_FATURA,
                po,
                user.getVendor(),
                po.getLegalEntity(),
                po.getCostCenter(),
                po.getCurrency(),
                subtotal,
                totalTax,
                totalWithholding,
                grandTotal,
                payableAmount,
                isAllServices ? MatchType.TWO_WAY : MatchType.THREE_WAY
        );

        // Re-attach line items
        for (PoFlipInvoiceRequest.PoFlipLineItemDto itemDto : request.lineItems()) {
            PurchaseOrderLineItem poLine = po.getLineItems().stream()
                    .filter(l -> l.getId().equals(itemDto.purchaseOrderLineItemId()))
                    .findFirst()
                    .orElse(null);
            if (poLine != null) {
                BigDecimal unitPrice = poLine.getUnitPrice();
                BigDecimal lineSubtotal = unitPrice.multiply(itemDto.invoicedQuantity());
                BigDecimal taxRate = itemDto.taxRate() != null ? itemDto.taxRate() : new BigDecimal("20.00");
                BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                BigDecimal lineWithholding = BigDecimal.ZERO;
                if (itemDto.tevkifatRate() != null && itemDto.tevkifatRate().contains("/")) {
                    String[] parts = itemDto.tevkifatRate().split("/");
                    if (parts.length == 2) {
                        try {
                            BigDecimal num = new BigDecimal(parts[0].trim());
                            BigDecimal denom = new BigDecimal(parts[1].trim());
                            lineWithholding = lineTax.multiply(num).divide(denom, 4, RoundingMode.HALF_UP);
                        } catch (Exception ignored) {}
                    }
                }
                SupplierInvoiceLineItem lineItem = new SupplierInvoiceLineItem(
                        user.getTenant(),
                        poLine,
                        null,
                        itemDto.invoicedQuantity(),
                        unitPrice,
                        taxRate,
                        lineTax,
                        itemDto.tevkifatCode(),
                        itemDto.tevkifatRate(),
                        lineWithholding,
                        lineSubtotal.add(lineTax)
                );
                invoice.addLineItem(lineItem);
            }
        }

        // Set due date based on payment terms (NET_30 -> 30 days)
        int termDays = 30;
        if (po.getPaymentTerms() != null) {
            String name = po.getPaymentTerms().name();
            if (name.contains("60")) termDays = 60;
            else if (name.contains("90")) termDays = 90;
            else if (name.contains("15")) termDays = 15;
            else if (name.contains("IMMEDIATE")) termDays = 0;
        }
        invoice.setDueDate(request.invoiceDate().plusDays(termDays));

        // Touchless Matching Decision Engine
        if (isAllServices) {
            // 2-Way Match: PO + Invoice
            invoice.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);
            invoice.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);
        } else {
            // 3-Way Match: PO + GR + Invoice
            if (po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED || po.getStatus() == PurchaseOrderStatus.FULFILLED) {
                invoice.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);
                invoice.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);
            } else {
                invoice.setMatchStatus(InvoiceMatchStatus.PENDING_RECEIPT);
                invoice.setStatus(InvoiceStatus.SUBMITTED);
            }
        }

        // Generate synthetic UBL XML for PO-Flip storage (AES-256-GCM encrypted)
        String generatedXml = String.format("<Invoice><ID>%s</ID><UUID>%s</UUID><IssueDate>%s</IssueDate><PayableAmount>%s</PayableAmount></Invoice>",
                request.invoiceNumber(), request.ettn(), request.invoiceDate(), payableAmount);
        invoice.setRawUblXml(generatedXml);

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);

        // Audit Trail (SOX 404 & ISO 37001)
        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.INVOICE_MATCH_SUCCESS,
                ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                user.getId(),
                user.getEmail(),
                "VENDOR_ADMIN",
                "127.0.0.1",
                "VendorPortal",
                "SupplierInvoice",
                saved.getId().toString(),
                po.getLegalEntity().getId(),
                po.getCostCenter().getId(),
                payableAmount,
                po.getCurrency(),
                "SUBMITTED",
                saved.getStatus().name(),
                "PO-Flip e-Invoice created for PO " + po.getPoNumber() + " -> Invoice: " + request.invoiceNumber() + " (Net: " + payableAmount + " " + po.getCurrency() + ")",
                "{\"invoiceNumber\":\"" + request.invoiceNumber() + "\",\"ettn\":\"" + request.ettn() + "\",\"matchStatus\":\"" + saved.getMatchStatus() + "\"}"
        ));

        return toInvoiceResponse(saved);
    }

    @Override
    @Transactional
    public SupplierInvoiceResponse uploadUblXmlInvoice(MultipartFile file, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        UUID tenantId = user.getTenant().getId();
        TenantContext.setTenantId(tenantId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "XML file is required");
        }

        try {
            UblTrInvoiceParserService.ParsedUblInvoice parsed = ublParserService.parseUblXml(file.getInputStream());

            if (supplierInvoiceRepository.existsByTenantIdAndEttn(tenantId, parsed.ettn())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An invoice with ETTN '" + parsed.ettn() + "' already exists");
            }

            // Find matching PO
            PurchaseOrder po = null;
            if (parsed.poNumber() != null && !parsed.poNumber().isBlank()) {
                po = purchaseOrderRepository.findByPoNumberAndTenantId(parsed.poNumber(), tenantId)
                        .filter(p -> p.getVendor().getId().equals(user.getVendor().getId()))
                        .orElse(null);
            }

            if (po == null) {
                // Fallback to latest PO for this vendor
                List<PurchaseOrder> vendorPos = purchaseOrderRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, user.getVendor().getId());
                if (!vendorPos.isEmpty()) {
                    po = vendorPos.get(0);
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No matching Purchase Order found for supplier invoice");
                }
            }

            InvoiceProfile profile = "TICARI_FATURA".equalsIgnoreCase(parsed.profileId()) ? InvoiceProfile.TICARI_FATURA : InvoiceProfile.TEMEL_FATURA;
            InvoiceType type = InvoiceType.SATIS;
            if ("TEVKIFAT".equalsIgnoreCase(parsed.invoiceTypeCode())) type = InvoiceType.TEVKIFAT;
            else if ("IADE".equalsIgnoreCase(parsed.invoiceTypeCode())) type = InvoiceType.IADE;
            else if ("ISTISNA".equalsIgnoreCase(parsed.invoiceTypeCode())) type = InvoiceType.ISTISNA;

            SupplierInvoice invoice = new SupplierInvoice(
                    user.getTenant(),
                    parsed.invoiceNumber(),
                    parsed.ettn(),
                    parsed.issueDate(),
                    type,
                    profile,
                    po,
                    user.getVendor(),
                    po.getLegalEntity(),
                    po.getCostCenter(),
                    parsed.currency() != null ? parsed.currency() : "TRY",
                    parsed.subtotalAmount(),
                    parsed.taxAmount(),
                    parsed.withholdingTaxAmount(),
                    parsed.totalAmount(),
                    parsed.payableAmount(),
                    MatchType.THREE_WAY
            );

            // Store raw XML (Encrypted by JPA converter)
            String rawXmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            invoice.setRawUblXml(rawXmlContent);
            invoice.setDueDate(parsed.issueDate().plusDays(30));

            // Map Line Items and Detect Discrepancies
            boolean hasDiscrepancy = false;
            int idx = 0;
            for (UblTrInvoiceParserService.ParsedUblLineItem pItem : parsed.lineItems()) {
                PurchaseOrderLineItem poLine = null;
                if (po.getLineItems() != null && idx < po.getLineItems().size()) {
                    poLine = po.getLineItems().get(idx);
                } else if (po.getLineItems() != null && !po.getLineItems().isEmpty()) {
                    poLine = po.getLineItems().get(0);
                }

                if (poLine != null) {
                    // Check price discrepancy
                    if (pItem.unitPrice().compareTo(poLine.getUnitPrice()) > 0) {
                        hasDiscrepancy = true;
                        BigDecimal variance = pItem.unitPrice().subtract(poLine.getUnitPrice()).multiply(pItem.quantity());
                        InvoiceDiscrepancy disc = new InvoiceDiscrepancy(
                                user.getTenant(),
                                invoice,
                                "PRICE_VARIANCE",
                                poLine.getUnitPrice().toString(),
                                pItem.unitPrice().toString(),
                                variance,
                                new BigDecimal("10.00")
                        );
                        invoice.addDiscrepancy(disc);
                    }

                    SupplierInvoiceLineItem lineItem = new SupplierInvoiceLineItem(
                            user.getTenant(),
                            poLine,
                            null,
                            pItem.quantity(),
                            pItem.unitPrice(),
                            pItem.taxRate(),
                            pItem.taxAmount(),
                            pItem.tevkifatCode(),
                            pItem.tevkifatRate(),
                            pItem.tevkifatAmount(),
                            pItem.lineTotalAmount()
                    );
                    invoice.addLineItem(lineItem);
                }
                idx++;
            }

            if (hasDiscrepancy) {
                invoice.setMatchStatus(InvoiceMatchStatus.PRICE_DISCREPANCY);
                invoice.setStatus(InvoiceStatus.SUBMITTED);
                invoice.setDiscrepancyReason("Invoice unit price exceeds purchase order unit price.");
            } else {
                invoice.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);
                invoice.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);
            }

            SupplierInvoice saved = supplierInvoiceRepository.save(invoice);

            // Audit
            auditService.recordAuditLog(new RecordAuditRequest(
                    UUID.randomUUID().toString(),
                    hasDiscrepancy ? AuditAction.INVOICE_MATCH_FAILED : AuditAction.INVOICE_MATCH_SUCCESS,
                    ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                    user.getId(),
                    user.getEmail(),
                    "VENDOR_ADMIN",
                    "127.0.0.1",
                    "VendorPortal",
                    "SupplierInvoice",
                    saved.getId().toString(),
                    po.getLegalEntity().getId(),
                    po.getCostCenter().getId(),
                    saved.getPayableAmount(),
                    saved.getCurrency(),
                    "SUBMITTED",
                    saved.getStatus().name(),
                    "Uploaded UBL-TR XML e-Invoice: " + parsed.invoiceNumber() + " (ETTN: " + parsed.ettn() + ")",
                    "{\"matchStatus\":\"" + saved.getMatchStatus() + "\"}"
            ));

            return toInvoiceResponse(saved);

        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to process UBL-TR XML e-Invoice: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierInvoiceResponse> getVendorInvoices(InvoiceStatus status, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        UUID tenantId = user.getTenant().getId();

        List<SupplierInvoice> invoices;
        if (status != null) {
            invoices = supplierInvoiceRepository.findAllByTenantIdAndVendorIdAndStatusOrderByCreatedAtDesc(tenantId, user.getVendor().getId(), status);
        } else {
            invoices = supplierInvoiceRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, user.getVendor().getId());
        }

        return invoices.stream().map(this::toInvoiceResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierInvoiceDetailResponse getVendorInvoiceDetail(UUID invoiceId, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, user.getTenant().getId(), user.getVendor().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found or not belonging to your vendor"));

        List<SupplierInvoiceDetailResponse.InvoiceLineItemDto> lineItems = invoice.getLineItems().stream()
                .map(li -> new SupplierInvoiceDetailResponse.InvoiceLineItemDto(
                        li.getId(),
                        li.getPurchaseOrderLineItem() != null ? li.getPurchaseOrderLineItem().getId() : null,
                        li.getPurchaseOrderLineItem() != null ? li.getPurchaseOrderLineItem().getLineNumber() : 1,
                        li.getPurchaseOrderLineItem() != null ? li.getPurchaseOrderLineItem().getItemDescription() : "Product / Service",
                        li.getInvoicedQuantity(),
                        li.getPurchaseOrderLineItem() != null ? li.getPurchaseOrderLineItem().getUnitOfMeasure() : "PIECE",
                        li.getUnitPrice(),
                        li.getTaxRate(),
                        li.getTaxAmount(),
                        li.getTevkifatCode(),
                        li.getTevkifatRate(),
                        li.getTevkifatAmount(),
                        li.getLineTotalAmount()
                ))
                .toList();

        List<SupplierInvoiceDetailResponse.InvoiceDiscrepancyDto> discrepancies = invoice.getDiscrepancies().stream()
                .map(d -> new SupplierInvoiceDetailResponse.InvoiceDiscrepancyDto(
                        d.getId(),
                        d.getDiscrepancyType(),
                        d.getExpectedValue(),
                        d.getActualValue(),
                        d.getVarianceAmount(),
                        d.getVariancePercentage(),
                        d.isResolved(),
                        d.getResolutionNotes(),
                        d.getCreatedAt()
                ))
                .toList();

        return new SupplierInvoiceDetailResponse(
                invoice.getId(),
                invoice.getPurchaseOrder() != null ? invoice.getPurchaseOrder().getId() : null,
                invoice.getPurchaseOrder() != null ? invoice.getPurchaseOrder().getPoNumber() : null,
                invoice.getVendor().getId(),
                invoice.getVendor().getName(),
                invoice.getInvoiceNumber(),
                invoice.getEttn(),
                invoice.getInvoiceProfile().name(),
                invoice.getInvoiceType().name(),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                invoice.getCurrency(),
                invoice.getExchangeRate(),
                invoice.getSubtotalAmount(),
                invoice.getTaxAmount(),
                invoice.getWithholdingTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getPayableAmount(),
                invoice.getMatchType().name(),
                invoice.getMatchStatus().name(),
                invoice.getStatus().name(),
                invoice.getDiscrepancyReason(),
                invoice.getRejectionReason(),
                lineItems,
                discrepancies,
                invoice.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String getInvoiceHtml(UUID invoiceId, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, user.getTenant().getId(), user.getVendor().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        return xsltRendererService.renderInvoiceHtml(invoice);
    }

    private BigDecimal getAlreadyInvoicedQuantity(UUID poLineId, UUID tenantId) {
        List<SupplierInvoiceLineItem> existingLines = invoiceLineItemRepository.findAllByTenantIdAndPurchaseOrderLineItemId(tenantId, poLineId);
        return existingLines.stream()
                .filter(l -> l.getSupplierInvoice().getStatus() != InvoiceStatus.REJECTED)
                .map(SupplierInvoiceLineItem::getInvoicedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private VendorUser getVendorUser(UUID vendorUserId) {
        return vendorUserRepository.findById(vendorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor user not found"));
    }

    private SupplierInvoiceResponse toInvoiceResponse(SupplierInvoice inv) {
        return new SupplierInvoiceResponse(
                inv.getId(),
                inv.getPurchaseOrder() != null ? inv.getPurchaseOrder().getId() : null,
                inv.getPurchaseOrder() != null ? inv.getPurchaseOrder().getPoNumber() : null,
                inv.getInvoiceNumber(),
                inv.getEttn(),
                inv.getInvoiceProfile().name(),
                inv.getInvoiceType().name(),
                inv.getInvoiceDate(),
                inv.getDueDate(),
                inv.getCurrency(),
                inv.getSubtotalAmount(),
                inv.getTaxAmount(),
                inv.getWithholdingTaxAmount(),
                inv.getTotalAmount(),
                inv.getPayableAmount(),
                inv.getMatchType().name(),
                inv.getMatchStatus().name(),
                inv.getStatus().name(),
                inv.getRejectionReason(),
                inv.getCreatedAt()
        );
    }
}
