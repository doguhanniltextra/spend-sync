package com.enterprise.spendsync.vendorportal.internal.web;

import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.vendorportal.dto.*;
import com.enterprise.spendsync.vendorportal.internal.service.VendorFinanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class VendorFinanceController {

    private final VendorFinanceService vendorFinanceService;

    public VendorFinanceController(VendorFinanceService vendorFinanceService) {
        this.vendorFinanceService = vendorFinanceService;
    }

    // --- 1. AP Payment Status & Timeline ---
    @GetMapping(Endpoints.VendorPortal.INVOICES_BASE + Endpoints.VendorPortal.INVOICE_PAYMENT_STATUS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoicePaymentStatusResponse> getInvoicePaymentStatus(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        InvoicePaymentStatusResponse response = vendorFinanceService.getInvoicePaymentStatus(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    // --- 2. Dynamic Discounting Early Payment Offers ---
    @GetMapping(Endpoints.VendorPortal.FINANCE_BASE + Endpoints.VendorPortal.EARLY_PAY_OFFERS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EarlyPayOfferResponse>> getAvailableEarlyPaymentOffers(
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        List<EarlyPayOfferResponse> offers = vendorFinanceService.getAvailableEarlyPaymentOffers(principal.getId());
        return ResponseEntity.ok(offers);
    }

    @PostMapping(Endpoints.VendorPortal.INVOICES_BASE + Endpoints.VendorPortal.INVOICE_ACCEPT_EARLY_DISCOUNT)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AcceptEarlyDiscountResponse> acceptEarlyPaymentOffer(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        AcceptEarlyDiscountResponse response = vendorFinanceService.acceptEarlyPaymentOffer(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    // --- 3. Statement of Accounts (SOA) ---
    @GetMapping(Endpoints.VendorPortal.FINANCE_BASE + Endpoints.VendorPortal.SOA)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StatementOfAccountsResponse> getStatementOfAccounts(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        StatementOfAccountsResponse response = vendorFinanceService.getStatementOfAccounts(startDate, endDate, principal.getId());
        return ResponseEntity.ok(response);
    }

    // --- 4. BA-BS e-Mutabakat (Reconciliation) ---
    @GetMapping(Endpoints.VendorPortal.FINANCE_BASE + Endpoints.VendorPortal.RECONCILIATION)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MonthlyReconciliationResponse> getMonthlyReconciliation(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        MonthlyReconciliationResponse response = vendorFinanceService.getMonthlyReconciliation(year, month, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping(Endpoints.VendorPortal.FINANCE_BASE + Endpoints.VendorPortal.RECONCILIATION_APPROVE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MonthlyReconciliationResponse> approveMonthlyReconciliation(
            @Valid @RequestBody MonthlyReconciliationApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        MonthlyReconciliationResponse response = vendorFinanceService.approveMonthlyReconciliation(request, principal.getId());
        return ResponseEntity.ok(response);
    }

    // --- 5. Catalog Proposals ---
    @PostMapping(Endpoints.VendorPortal.CATALOG_BASE + Endpoints.VendorPortal.CATALOG_PROPOSALS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VendorCatalogProposalResponse> submitCatalogProposal(
            @Valid @RequestBody VendorCatalogProposalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        VendorCatalogProposalResponse response = vendorFinanceService.submitCatalogProposal(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.VendorPortal.CATALOG_BASE + Endpoints.VendorPortal.CATALOG_PROPOSALS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VendorCatalogProposalResponse>> getVendorCatalogProposals(
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        List<VendorCatalogProposalResponse> proposals = vendorFinanceService.getVendorCatalogProposals(principal.getId());
        return ResponseEntity.ok(proposals);
    }

    private void validateVendorAccess(UserPrincipal principal) {
        if (principal == null || !principal.isVendor() || principal.getVendorId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access restricted to authenticated vendor users");
        }
    }
}
