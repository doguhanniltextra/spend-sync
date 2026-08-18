package com.enterprise.spendsync.matching.internal.web;

import com.enterprise.spendsync.matching.internal.dto.CreateSupplierInvoiceRequest;
import com.enterprise.spendsync.matching.internal.dto.ManagerOverrideRequest;
import com.enterprise.spendsync.matching.internal.dto.RejectInvoiceRequest;
import com.enterprise.spendsync.matching.internal.dto.SupplierInvoiceResponse;
import com.enterprise.spendsync.matching.internal.service.MatchingService;
import com.enterprise.spendsync.shared.config.Endpoints;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Matching.BASE)
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping(Endpoints.Matching.INVOICES)
    @PreAuthorize("hasAnyAuthority('PERM_INVOICE_CREATE', 'PERM_MATCH_EVALUATE', 'PERM_ORG_MANAGE')")
    public ResponseEntity<SupplierInvoiceResponse> createAndEvaluateInvoice(@Valid @RequestBody CreateSupplierInvoiceRequest request) {
        SupplierInvoiceResponse response = matchingService.createAndEvaluateInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.Matching.INVOICE_BY_ID)
    @PreAuthorize("hasAnyAuthority('PERM_INVOICE_READ', 'PERM_MATCH_EVALUATE', 'PERM_PO_READ')")
    public ResponseEntity<SupplierInvoiceResponse> getInvoiceById(@PathVariable UUID id) {
        return ResponseEntity.ok(matchingService.getInvoiceById(id));
    }

    @GetMapping(Endpoints.Matching.INVOICES_BY_PO)
    @PreAuthorize("hasAnyAuthority('PERM_INVOICE_READ', 'PERM_MATCH_EVALUATE', 'PERM_PO_READ')")
    public ResponseEntity<List<SupplierInvoiceResponse>> getInvoicesByPurchaseOrder(@PathVariable UUID poId) {
        return ResponseEntity.ok(matchingService.getInvoicesByPurchaseOrder(poId));
    }

    @GetMapping(Endpoints.Matching.INVOICES)
    @PreAuthorize("hasAnyAuthority('PERM_INVOICE_READ', 'PERM_MATCH_EVALUATE', 'PERM_PO_READ')")
    public ResponseEntity<List<SupplierInvoiceResponse>> getAllInvoices() {
        return ResponseEntity.ok(matchingService.getAllInvoices());
    }

    @PostMapping(Endpoints.Matching.OVERRIDE)
    @PreAuthorize("hasAnyAuthority('PERM_MATCH_EVALUATE', 'PERM_ORG_MANAGE', 'PERM_BUDGET_MANAGE')")
    public ResponseEntity<SupplierInvoiceResponse> managerOverride(
            @PathVariable UUID id,
            @Valid @RequestBody ManagerOverrideRequest request) {
        return ResponseEntity.ok(matchingService.managerOverride(id, request));
    }

    @PostMapping(Endpoints.Matching.REJECT)
    @PreAuthorize("hasAnyAuthority('PERM_MATCH_EVALUATE', 'PERM_INVOICE_CREATE', 'PERM_ORG_MANAGE')")
    public ResponseEntity<SupplierInvoiceResponse> rejectInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody RejectInvoiceRequest request) {
        return ResponseEntity.ok(matchingService.rejectInvoice(id, request));
    }
}
