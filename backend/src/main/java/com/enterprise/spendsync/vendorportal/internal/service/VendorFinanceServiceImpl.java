package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;
import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.catalog.internal.domain.CatalogItem;
import com.enterprise.spendsync.catalog.internal.repository.CatalogItemRepository;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.shared.crypto.MaskingUtils;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.*;
import com.enterprise.spendsync.vendorportal.internal.domain.*;
import com.enterprise.spendsync.vendorportal.internal.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class VendorFinanceServiceImpl implements VendorFinanceService {

    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final VendorEarlyPayOfferRepository earlyPayOfferRepository;
    private final VendorCatalogProposalRepository catalogProposalRepository;
    private final VendorMonthlyReconciliationRepository reconciliationRepository;
    private final VendorUserRepository vendorUserRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final AuditService auditService;

    public VendorFinanceServiceImpl(SupplierInvoiceRepository supplierInvoiceRepository,
                                  VendorEarlyPayOfferRepository earlyPayOfferRepository,
                                  VendorCatalogProposalRepository catalogProposalRepository,
                                  VendorMonthlyReconciliationRepository reconciliationRepository,
                                  VendorUserRepository vendorUserRepository,
                                  CatalogItemRepository catalogItemRepository,
                                  AuditService auditService) {
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.earlyPayOfferRepository = earlyPayOfferRepository;
        this.catalogProposalRepository = catalogProposalRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.vendorUserRepository = vendorUserRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public InvoicePaymentStatusResponse getInvoicePaymentStatus(UUID invoiceId, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, user.getTenant().getId(), user.getVendor().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        List<InvoicePaymentStatusResponse.PaymentTimelineStepDto> timeline = new ArrayList<>();

        // Step 1: SUBMITTED
        timeline.add(new InvoicePaymentStatusResponse.PaymentTimelineStepDto(
                "SUBMITTED",
                "Invoice Submitted",
                "e-Invoice / PO-Flip submitted successfully.",
                true,
                invoice.getCreatedAt()
        ));

        // Step 2: MATCHED
        boolean isMatched = invoice.getMatchStatus() == InvoiceMatchStatus.AUTO_MATCHED || invoice.getMatchStatus() == InvoiceMatchStatus.MANUALLY_MATCHED;
        timeline.add(new InvoicePaymentStatusResponse.PaymentTimelineStepDto(
                "MATCHED",
                "PO & Delivery Matching",
                isMatched ? "Invoice fully matched with purchase order and delivery receipt (Touchless)." : "Matching verification pending: " + invoice.getMatchStatus(),
                isMatched,
                isMatched ? invoice.getCreatedAt() : null
        ));

        // Step 3: APPROVED_FOR_PAYMENT
        boolean isApproved = invoice.getStatus() == InvoiceStatus.APPROVED_FOR_PAYMENT || invoice.getStatus() == InvoiceStatus.PAID;
        timeline.add(new InvoicePaymentStatusResponse.PaymentTimelineStepDto(
                "APPROVED_FOR_PAYMENT",
                "Approved for Payment",
                isApproved ? "Financial approval completed, scheduled due date: " + invoice.getDueDate() : "Financial approval pending.",
                isApproved,
                isApproved ? invoice.getUpdatedAt() : null
        ));

        // Step 4: PAID
        boolean isPaid = invoice.getStatus() == InvoiceStatus.PAID;
        timeline.add(new InvoicePaymentStatusResponse.PaymentTimelineStepDto(
                "PAID",
                "Bank Payment Executed",
                isPaid ? "Payment disbursed to vendor bank account via payment batch." : "Scheduled Payment Date: " + (invoice.getDueDate() != null ? invoice.getDueDate() : "Pending Due Date"),
                isPaid,
                isPaid ? invoice.getUpdatedAt() : null
        ));

        String maskedIban = user.getVendor().getIban() != null ? MaskingUtils.maskIban(user.getVendor().getIban()) : null;
        String bankRef = isPaid ? "TXN-TR-GARANTI-" + invoice.getInvoiceNumber().replace("GIB", "") : null;

        return new InvoicePaymentStatusResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus().name(),
                invoice.getMatchStatus().name(),
                invoice.getPayableAmount(),
                invoice.getCurrency(),
                invoice.getDueDate(),
                isPaid ? invoice.getDueDate() : null,
                bankRef,
                maskedIban,
                timeline
        );
    }

    @Override
    @Transactional
    public List<EarlyPayOfferResponse> getAvailableEarlyPaymentOffers(UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        UUID tenantId = user.getTenant().getId();
        UUID vendorId = user.getVendor().getId();

        TenantContext.setTenantId(tenantId);

        // Find approved invoices for this vendor
        List<SupplierInvoice> approvedInvoices = supplierInvoiceRepository
                .findAllByTenantIdAndVendorIdAndStatusOrderByCreatedAtDesc(tenantId, vendorId, InvoiceStatus.APPROVED_FOR_PAYMENT);

        List<EarlyPayOfferResponse> offers = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (SupplierInvoice inv : approvedInvoices) {
            // Check if offer already exists
            VendorEarlyPayOffer offer = earlyPayOfferRepository.findByTenantIdAndSupplierInvoiceId(tenantId, inv.getId()).orElse(null);

            if (offer == null) {
                // Dynamically generate offer: 2% early discount for T+3 accelerated payout
                BigDecimal discountPct = new BigDecimal("2.00");
                BigDecimal discountAmount = inv.getPayableAmount().multiply(discountPct).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                BigDecimal netPayout = inv.getPayableAmount().subtract(discountAmount);
                LocalDate acceleratedDate = today.plusDays(3);

                offer = new VendorEarlyPayOffer(
                        user.getTenant(),
                        inv,
                        user.getVendor(),
                        inv.getPayableAmount(),
                        inv.getDueDate() != null ? inv.getDueDate() : today.plusDays(30),
                        discountPct,
                        discountAmount,
                        netPayout,
                        acceleratedDate
                );
                offer = earlyPayOfferRepository.save(offer);
            }

            if (offer.getStatus() == EarlyPayOfferStatus.OFFERED) {
                offers.add(new EarlyPayOfferResponse(
                        offer.getId(),
                        inv.getId(),
                        inv.getInvoiceNumber(),
                        offer.getOriginalAmount(),
                        inv.getCurrency(),
                        offer.getDiscountPercentage(),
                        offer.getDiscountAmount(),
                        offer.getNetPayoutAmount(),
                        offer.getOriginalDueDate(),
                        offer.getAcceleratedPaymentDate(),
                        offer.getStatus().name()
                ));
            }
        }

        return offers;
    }

    @Override
    @Transactional
    public AcceptEarlyDiscountResponse acceptEarlyPaymentOffer(UUID invoiceId, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        UUID tenantId = user.getTenant().getId();
        UUID vendorId = user.getVendor().getId();

        TenantContext.setTenantId(tenantId);

        SupplierInvoice invoice = supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, tenantId, vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        VendorEarlyPayOffer offer = earlyPayOfferRepository.findByTenantIdAndSupplierInvoiceIdAndStatus(tenantId, invoiceId, EarlyPayOfferStatus.OFFERED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active early payment offer found for this invoice"));

        // Accept offer
        offer.accept();
        earlyPayOfferRepository.save(offer);

        // Update invoice amounts & accelerated payout date
        invoice.setPayableAmount(offer.getNetPayoutAmount());
        invoice.setDueDate(offer.getAcceleratedPaymentDate());
        supplierInvoiceRepository.save(invoice);

        // Audit Trail (SOX 404 Financial Governance)
        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.INVOICE_MATCH_SUCCESS,
                ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                user.getId(),
                user.getEmail(),
                "VENDOR_ADMIN",
                "127.0.0.1",
                "VendorPortal",
                "VendorEarlyPayOffer",
                offer.getId().toString(),
                invoice.getLegalEntity().getId(),
                invoice.getCostCenter().getId(),
                offer.getDiscountAmount(),
                invoice.getCurrency(),
                "OFFERED",
                "ACCEPTED",
                "Vendor " + user.getVendor().getName() + " accepted 2% early payment discount on invoice " + invoice.getInvoiceNumber() + ". Payout accelerated to " + offer.getAcceleratedPaymentDate(),
                "{\"originalAmount\":\"" + offer.getOriginalAmount() + "\",\"discountAmount\":\"" + offer.getDiscountAmount() + "\",\"netPayout\":\"" + offer.getNetPayoutAmount() + "\"}"
        ));

        return new AcceptEarlyDiscountResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                "ACCEPTED",
                offer.getOriginalAmount(),
                offer.getDiscountAmount(),
                offer.getNetPayoutAmount(),
                offer.getAcceleratedPaymentDate(),
                "Early payment offer accepted. Net " + offer.getNetPayoutAmount() + " " + invoice.getCurrency() + " payout scheduled for " + offer.getAcceleratedPaymentDate() + " bank batch."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StatementOfAccountsResponse getStatementOfAccounts(LocalDate startDate, LocalDate endDate, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        UUID tenantId = user.getTenant().getId();
        UUID vendorId = user.getVendor().getId();

        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfYear(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now().plusMonths(1);

        List<SupplierInvoice> invoices = supplierInvoiceRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId);

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        List<StatementOfAccountsResponse.SoaEntryDto> entries = new ArrayList<>();

        for (SupplierInvoice inv : invoices) {
            BigDecimal invTotal = inv.getPayableAmount();
            totalInvoiced = totalInvoiced.add(invTotal);

            BigDecimal paid = (inv.getStatus() == InvoiceStatus.PAID) ? invTotal : BigDecimal.ZERO;
            totalPaid = totalPaid.add(paid);

            entries.add(new StatementOfAccountsResponse.SoaEntryDto(
                    inv.getInvoiceDate(),
                    "INVOICE",
                    inv.getInvoiceNumber(),
                    inv.getPurchaseOrder() != null ? inv.getPurchaseOrder().getPoNumber() : "-",
                    invTotal,
                    paid,
                    invTotal.subtract(paid),
                    inv.getStatus().name()
            ));
        }

        BigDecimal openBalance = totalInvoiced.subtract(totalPaid);

        return new StatementOfAccountsResponse(
                vendorId,
                user.getVendor().getName(),
                totalInvoiced,
                totalPaid,
                openBalance,
                "TRY",
                start,
                end,
                entries
        );
    }

    @Override
    @Transactional
    public MonthlyReconciliationResponse getMonthlyReconciliation(int year, int month, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        UUID tenantId = user.getTenant().getId();
        UUID vendorId = user.getVendor().getId();

        TenantContext.setTenantId(tenantId);

        VendorMonthlyReconciliation rec = reconciliationRepository
                .findByTenantIdAndVendorIdAndPeriodYearAndPeriodMonth(tenantId, vendorId, year, month)
                .orElse(null);

        // Compute actual monthly invoice summary
        List<SupplierInvoice> allInvoices = supplierInvoiceRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId);
        YearMonth targetPeriod = YearMonth.of(year, month);

        List<SupplierInvoice> monthlyInvoices = allInvoices.stream()
                .filter(inv -> {
                    if (inv.getStatus() == InvoiceStatus.REJECTED || inv.getStatus() == InvoiceStatus.CANCELLED) return false;
                    YearMonth invPeriod = YearMonth.from(inv.getInvoiceDate());
                    return invPeriod.equals(targetPeriod);
                })
                .toList();

        int count = monthlyInvoices.size();
        BigDecimal total = monthlyInvoices.stream()
                .map(SupplierInvoice::getPayableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (rec == null) {
            rec = new VendorMonthlyReconciliation(user.getTenant(), user.getVendor(), year, month, count, total);
            rec = reconciliationRepository.save(rec);
        }

        return toReconciliationResponse(rec);
    }

    @Override
    @Transactional
    public MonthlyReconciliationResponse approveMonthlyReconciliation(MonthlyReconciliationApprovalRequest request, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        UUID tenantId = user.getTenant().getId();
        UUID vendorId = user.getVendor().getId();

        TenantContext.setTenantId(tenantId);

        VendorMonthlyReconciliation rec = reconciliationRepository
                .findByTenantIdAndVendorIdAndPeriodYearAndPeriodMonth(tenantId, vendorId, request.year(), request.month())
                .orElseGet(() -> {
                    // Compute on the fly if not exists
                    List<SupplierInvoice> allInvoices = supplierInvoiceRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId);
                    YearMonth targetPeriod = YearMonth.of(request.year(), request.month());
                    List<SupplierInvoice> monthlyInvoices = allInvoices.stream()
                            .filter(inv -> inv.getStatus() != InvoiceStatus.REJECTED && YearMonth.from(inv.getInvoiceDate()).equals(targetPeriod))
                            .toList();
                    int count = monthlyInvoices.size();
                    BigDecimal total = monthlyInvoices.stream().map(SupplierInvoice::getPayableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return reconciliationRepository.save(new VendorMonthlyReconciliation(user.getTenant(), user.getVendor(), request.year(), request.month(), count, total));
                });

        if (request.disputed()) {
            rec.dispute(request.notes());
        } else {
            // Generate SHA-256 digital signature seal
            String payload = tenantId + ":" + vendorId + ":" + request.year() + ":" + request.month() + ":" + rec.getTotalAmount() + ":" + Instant.now();
            String checksum = computeSha256(payload);
            rec.approve(request.notes() != null ? request.notes() : "Reconciliation approved.", checksum);
        }

        VendorMonthlyReconciliation saved = reconciliationRepository.save(rec);

        // Audit Log
        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.INVOICE_MATCH_SUCCESS,
                ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                user.getId(),
                user.getEmail(),
                "VENDOR_ADMIN",
                "127.0.0.1",
                "VendorPortal",
                "VendorMonthlyReconciliation",
                saved.getId().toString(),
                null,
                null,
                saved.getTotalAmount(),
                "TRY",
                "PENDING",
                saved.getStatus(),
                "BA-BS monthly reconciliation for " + request.year() + "/" + request.month() + " signed by vendor: " + saved.getStatus(),
                "{\"year\":" + request.year() + ",\"month\":" + request.month() + ",\"checksum\":\"" + saved.getSignedChecksum() + "\"}"
        ));

        return toReconciliationResponse(saved);
    }

    @Override
    @Transactional
    public VendorCatalogProposalResponse submitCatalogProposal(VendorCatalogProposalRequest request, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        UUID tenantId = user.getTenant().getId();

        TenantContext.setTenantId(tenantId);

        CatalogItem catalogItem = null;
        if (request.itemMasterId() != null) {
            catalogItem = catalogItemRepository.findByTenantIdAndId(tenantId, request.itemMasterId())
                    .orElse(null);
        }

        VendorCatalogProposal proposal = new VendorCatalogProposal(
                user.getTenant(),
                user.getVendor(),
                catalogItem,
                request.proposedItemCode(),
                request.proposedName(),
                request.proposedCategory(),
                request.proposedUnitPrice(),
                request.proposedCurrency(),
                request.vatRate(),
                request.leadTimeDays(),
                request.notes()
        );

        VendorCatalogProposal saved = catalogProposalRepository.save(proposal);

        // Audit Trail
        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.PURCHASE_ORDER_ISSUED,
                ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                user.getId(),
                user.getEmail(),
                "VENDOR_ADMIN",
                "127.0.0.1",
                "VendorPortal",
                "VendorCatalogProposal",
                saved.getId().toString(),
                null,
                null,
                saved.getProposedUnitPrice(),
                saved.getProposedCurrency(),
                "DRAFT",
                "PENDING_BUYER_REVIEW",
                "Vendor submitted catalog pricing proposal for '" + saved.getProposedName() + "' (" + saved.getProposedUnitPrice() + " " + saved.getProposedCurrency() + ")",
                "{\"itemCode\":\"" + saved.getProposedItemCode() + "\",\"unitPrice\":" + saved.getProposedUnitPrice() + "}"
        ));

        return toCatalogProposalResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorCatalogProposalResponse> getVendorCatalogProposals(UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        List<VendorCatalogProposal> list = catalogProposalRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(user.getTenant().getId(), user.getVendor().getId());
        return list.stream().map(this::toCatalogProposalResponse).toList();
    }

    private VendorUser getVendorUser(UUID vendorUserId) {
        return vendorUserRepository.findById(vendorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor user not found"));
    }

    private MonthlyReconciliationResponse toReconciliationResponse(VendorMonthlyReconciliation r) {
        return new MonthlyReconciliationResponse(
                r.getId(),
                r.getVendor().getId(),
                r.getVendor().getName(),
                r.getPeriodYear(),
                r.getPeriodMonth(),
                r.getInvoiceCount(),
                r.getTotalAmount(),
                "TRY",
                r.getStatus(),
                r.getVendorNotes(),
                r.getVendorApprovedAt(),
                r.getSignedChecksum()
        );
    }

    private VendorCatalogProposalResponse toCatalogProposalResponse(VendorCatalogProposal p) {
        return new VendorCatalogProposalResponse(
                p.getId(),
                p.getVendor().getId(),
                p.getVendor().getName(),
                p.getCatalogItem() != null ? p.getCatalogItem().getId() : null,
                p.getProposedItemCode(),
                p.getProposedName(),
                p.getProposedCategory(),
                p.getProposedUnitPrice(),
                p.getProposedCurrency(),
                p.getVatRate(),
                p.getLeadTimeDays(),
                p.getNotes(),
                p.getStatus().name(),
                p.getBuyerDecisionNotes(),
                p.getCreatedAt()
        );
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
