package com.enterprise.spendsync.matching.internal.service;

import com.enterprise.spendsync.budget.internal.service.BudgetService;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoiceLineItem;
import com.enterprise.spendsync.matching.internal.dto.CreateInvoiceLineItemRequest;
import com.enterprise.spendsync.matching.internal.dto.CreateSupplierInvoiceRequest;
import com.enterprise.spendsync.matching.internal.dto.ManagerOverrideRequest;
import com.enterprise.spendsync.matching.internal.dto.RejectInvoiceRequest;
import com.enterprise.spendsync.matching.internal.dto.SupplierInvoiceResponse;
import com.enterprise.spendsync.matching.internal.event.InvoiceMatchedEvent;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceLineItemRepository;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderLineItemRepository;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceiptLineItem;
import com.enterprise.spendsync.receiving.internal.repository.GoodsReceiptLineItemRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MatchingServiceImpl implements MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingServiceImpl.class);

    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierInvoiceLineItemRepository supplierInvoiceLineItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineItemRepository purchaseOrderLineItemRepository;
    private final GoodsReceiptLineItemRepository goodsReceiptLineItemRepository;
    private final BudgetService budgetService;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal PRICE_TOLERANCE_PERCENTAGE = BigDecimal.valueOf(1.00); // ±1%

    public MatchingServiceImpl(SupplierInvoiceRepository supplierInvoiceRepository,
                               SupplierInvoiceLineItemRepository supplierInvoiceLineItemRepository,
                               PurchaseOrderRepository purchaseOrderRepository,
                               PurchaseOrderLineItemRepository purchaseOrderLineItemRepository,
                               GoodsReceiptLineItemRepository goodsReceiptLineItemRepository,
                               BudgetService budgetService,
                               TenantRepository tenantRepository,
                               UserRepository userRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.supplierInvoiceLineItemRepository = supplierInvoiceLineItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderLineItemRepository = purchaseOrderLineItemRepository;
        this.goodsReceiptLineItemRepository = goodsReceiptLineItemRepository;
        this.budgetService = budgetService;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public SupplierInvoiceResponse createAndEvaluateInvoice(CreateSupplierInvoiceRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        // 1. Duplicate ETTN and Invoice Number Check
        if (supplierInvoiceRepository.findByTenantIdAndEttn(tenantId, request.ettn()).isPresent()) {
            throw new SpendSyncException("An invoice with this ETTN already exists: " + request.ettn(), HttpStatus.CONFLICT, "DUPLICATE_INVOICE_ETTN") {};
        }

        PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantId(request.purchaseOrderId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Purchase Order not found", HttpStatus.NOT_FOUND, "PO_NOT_FOUND") {});

        if (supplierInvoiceRepository.findByTenantIdAndVendorIdAndInvoiceNumber(tenantId, po.getVendor().getId(), request.invoiceNumber()).isPresent()) {
            throw new SpendSyncException("An invoice with this number already exists for vendor: " + request.invoiceNumber(), HttpStatus.CONFLICT, "DUPLICATE_INVOICE_NUMBER") {};
        }

        if (po.getStatus() == PurchaseOrderStatus.DRAFT || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new SpendSyncException("Invoice cannot be created for PO in status: " + po.getStatus(), HttpStatus.BAD_REQUEST, "INVALID_PO_STATUS_FOR_INVOICE") {};
        }

        // Initialize Invoice Header
        SupplierInvoice invoice = new SupplierInvoice(
                tenant,
                request.invoiceNumber(),
                request.ettn(),
                request.invoiceDate(),
                request.invoiceType() != null ? request.invoiceType() : InvoiceType.SATIS,
                request.invoiceProfile() != null ? request.invoiceProfile() : InvoiceProfile.TICARI_FATURA,
                po,
                po.getVendor(),
                po.getLegalEntity(),
                po.getCostCenter(),
                po.getCurrency(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        boolean hasDiscrepancy = false;
        StringBuilder discrepancyReport = new StringBuilder();

        // 2. Line Item Matching Evaluation (Hybrid 2-Way & 3-Way)
        for (CreateInvoiceLineItemRequest lineReq : request.lineItems()) {
            PurchaseOrderLineItem poLine = purchaseOrderLineItemRepository.findById(lineReq.purchaseOrderLineItemId())
                    .orElseThrow(() -> new SpendSyncException("PO Line Item not found: " + lineReq.purchaseOrderLineItemId(), HttpStatus.NOT_FOUND, "PO_LINE_NOT_FOUND") {});

            if (!poLine.getPurchaseOrder().getId().equals(po.getId())) {
                throw new SpendSyncException("PO line item does not match specified purchase order", HttpStatus.BAD_REQUEST, "INVALID_LINE_PO_MAPPING") {};
            }

            BigDecimal lineSubtotal = lineReq.invoicedQuantity().multiply(lineReq.unitPrice());
            BigDecimal taxRate = lineReq.taxRate() != null ? lineReq.taxRate() : BigDecimal.valueOf(20.00);
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineSubtotal.add(lineTax);

            totalSubtotal = totalSubtotal.add(lineSubtotal);
            totalTax = totalTax.add(lineTax);

            GoodsReceiptLineItem grLine = null;
            if (lineReq.goodsReceiptLineItemId() != null) {
                grLine = goodsReceiptLineItemRepository.findById(lineReq.goodsReceiptLineItemId()).orElse(null);
            }

            SupplierInvoiceLineItem invLine = new SupplierInvoiceLineItem(
                    tenant,
                    poLine,
                    grLine,
                    lineReq.invoicedQuantity(),
                    lineReq.unitPrice(),
                    taxRate,
                    lineTax,
                    lineTotal
            );

            // ── 3-Way vs 2-Way Evaluation ───────────────────────────────────
            boolean isServiceOrSoftware = "SERVICE".equalsIgnoreCase(poLine.getItemCategory()) || "SOFTWARE".equalsIgnoreCase(poLine.getItemCategory());
            BigDecimal maxAllowedQuantity;

            if (isServiceOrSoftware) {
                // 2-Way Match: against PO quantity
                maxAllowedQuantity = poLine.getQuantity();
            } else {
                // 3-Way Match: against GR accepted quantity
                maxAllowedQuantity = goodsReceiptLineItemRepository.sumAcceptedQuantityByPoLineId(poLine.getId());
            }

            // Quantity Check
            if (lineReq.invoicedQuantity().compareTo(maxAllowedQuantity) > 0) {
                hasDiscrepancy = true;
                String msg = String.format("Line %d Quantity Discrepancy: Invoiced (%.2f) > Accepted (%.2f); ",
                        poLine.getLineNumber(), lineReq.invoicedQuantity(), maxAllowedQuantity);
                invLine.setMatchStatus(InvoiceMatchStatus.DISCREPANCY_HOLD);
                invLine.setVarianceReason(msg);
                discrepancyReport.append(msg);
            }

            // Price Check (Tolerance Band: ±1%)
            BigDecimal priceDiff = lineReq.unitPrice().subtract(poLine.getUnitPrice()).abs();
            BigDecimal maxPriceTolerance = poLine.getUnitPrice().multiply(PRICE_TOLERANCE_PERCENTAGE).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            if (priceDiff.compareTo(maxPriceTolerance) > 0) {
                hasDiscrepancy = true;
                String msg = String.format("Line %d Price Discrepancy: Invoiced (%.2f) != Contract (%.2f); ",
                        poLine.getLineNumber(), lineReq.unitPrice(), poLine.getUnitPrice());
                invLine.setMatchStatus(InvoiceMatchStatus.DISCREPANCY_HOLD);
                invLine.setVarianceReason(invLine.getVarianceReason() != null ? invLine.getVarianceReason() + msg : msg);
                discrepancyReport.append(msg);
            }

            if (invLine.getMatchStatus() == InvoiceMatchStatus.EVALUATING) {
                invLine.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);
            }

            invoice.addLineItem(invLine);
        }

        BigDecimal grandTotal = totalSubtotal.add(totalTax);
        setInvoiceAmounts(invoice, totalSubtotal, totalTax, grandTotal);

        UUID budgetPoolId = po.getRequisition() != null ? po.getRequisition().getBudgetPool().getId() : null;

        // 3. Match Decision & Budget Commit / Hold
        if (!hasDiscrepancy) {
            invoice.setMatchStatus(InvoiceMatchStatus.AUTO_MATCHED);
            invoice.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);

            // Commit Budget
            if (budgetPoolId != null) {
                budgetService.commitBudget(
                        budgetPoolId,
                        grandTotal,
                        invoice.getId(),
                        "3WAY_MATCH_AUTO_COMMIT",
                        "Automated 3-Way Match approved for Invoice: " + invoice.getInvoiceNumber()
                );

                // Release unspent reserved budget if invoice is less than PO total
                if (po.getTotalAmount().compareTo(grandTotal) > 0) {
                    BigDecimal unusedBudget = po.getTotalAmount().subtract(grandTotal);
                    budgetService.releaseBudget(
                            budgetPoolId,
                            unusedBudget,
                            invoice.getId(),
                            "INVOICE_SHORT_CLOSE_RELEASE",
                            "Unused reservation released after invoice finalization: " + unusedBudget
                    );
                }
            }
        } else {
            invoice.setMatchStatus(InvoiceMatchStatus.DISCREPANCY_HOLD);
            invoice.setStatus(InvoiceStatus.SUBMITTED);
            invoice.setDiscrepancyReason(discrepancyReport.toString().trim());
        }

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);

        // Publish Domain Event
        InvoiceMatchedEvent event = new InvoiceMatchedEvent(
                tenantId,
                saved.getId(),
                saved.getInvoiceNumber(),
                saved.getEttn(),
                po.getId(),
                po.getPoNumber(),
                po.getVendor().getId(),
                budgetPoolId,
                saved.getTotalAmount(),
                saved.getCurrency(),
                saved.getMatchStatus(),
                saved.getDiscrepancyReason(),
                Instant.now()
        );
        eventPublisher.publishEvent(event);

        log.info("Invoice {} matched with status: {}", saved.getInvoiceNumber(), saved.getMatchStatus());
        return SupplierInvoiceResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierInvoiceResponse getInvoiceById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Supplier Invoice not found", HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND") {});
        return SupplierInvoiceResponse.from(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierInvoiceResponse> getInvoicesByPurchaseOrder(UUID purchaseOrderId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return supplierInvoiceRepository.findAllByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(tenantId, purchaseOrderId)
                .stream()
                .map(SupplierInvoiceResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierInvoiceResponse> getAllInvoices() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return supplierInvoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(SupplierInvoiceResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SupplierInvoiceResponse managerOverride(UUID id, ManagerOverrideRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = resolveCurrentUser(tenantId);

        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Supplier Invoice not found", HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND") {});

        if (invoice.getMatchStatus() != InvoiceMatchStatus.DISCREPANCY_HOLD) {
            throw new SpendSyncException("Only invoices on DISCREPANCY_HOLD can be manually overridden. Current: " + invoice.getMatchStatus(),
                    HttpStatus.BAD_REQUEST, "INVALID_STATUS_FOR_OVERRIDE") {};
        }

        invoice.setMatchStatus(InvoiceMatchStatus.MANUALLY_MATCHED);
        invoice.setStatus(InvoiceStatus.APPROVED_FOR_PAYMENT);
        invoice.setManagerOverrideNote(request.overrideNote());
        invoice.setManagerOverrideByUser(currentUser);

        // Commit Budget
        PurchaseOrder po = invoice.getPurchaseOrder();
        if (po.getRequisition() != null && po.getRequisition().getBudgetPool() != null) {
            budgetService.commitBudget(
                    po.getRequisition().getBudgetPool().getId(),
                    invoice.getTotalAmount(),
                    invoice.getId(),
                    "3WAY_MATCH_MANUAL_OVERRIDE_COMMIT",
                    "Manager override approved by " + currentUser.getEmail() + ": " + request.overrideNote()
            );
        }

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        log.info("Invoice {} manually overridden and approved for payment by {}", saved.getInvoiceNumber(), currentUser.getEmail());
        return SupplierInvoiceResponse.from(saved);
    }

    @Override
    @Transactional
    public SupplierInvoiceResponse rejectInvoice(UUID id, RejectInvoiceRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Supplier Invoice not found", HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND") {});

        invoice.setMatchStatus(InvoiceMatchStatus.REJECTED);
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setDiscrepancyReason("Commercial Rejection (GIB Iade): " + request.rejectionReason());

        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        log.info("Invoice {} commercially rejected (TTK §18/3): {}", saved.getInvoiceNumber(), request.rejectionReason());
        return SupplierInvoiceResponse.from(saved);
    }

    private void setInvoiceAmounts(SupplierInvoice invoice, BigDecimal subtotal, BigDecimal tax, BigDecimal total) {
        try {
            var subField = SupplierInvoice.class.getDeclaredField("subtotalAmount");
            subField.setAccessible(true);
            subField.set(invoice, subtotal);

            var taxField = SupplierInvoice.class.getDeclaredField("taxAmount");
            taxField.setAccessible(true);
            taxField.set(invoice, tax);

            var totalField = SupplierInvoice.class.getDeclaredField("totalAmount");
            totalField.setAccessible(true);
            totalField.set(invoice, total);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set invoice amounts reflection", e);
        }
    }

    private User resolveCurrentUser(UUID tenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findByIdAndTenantId(principal.getId(), tenantId)
                    .orElseThrow(() -> new SpendSyncException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
        }
        return userRepository.findAllByTenantId(tenantId).stream().findFirst()
                .orElseThrow(() -> new SpendSyncException("Default user not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
    }
}
