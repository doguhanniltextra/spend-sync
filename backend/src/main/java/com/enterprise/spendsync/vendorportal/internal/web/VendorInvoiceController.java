package com.enterprise.spendsync.vendorportal.internal.web;

import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.vendorportal.dto.PoFlipInvoiceRequest;
import com.enterprise.spendsync.vendorportal.dto.SupplierInvoiceDetailResponse;
import com.enterprise.spendsync.vendorportal.dto.SupplierInvoiceResponse;
import com.enterprise.spendsync.vendorportal.internal.service.VendorInvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.VendorPortal.INVOICES_BASE)
public class VendorInvoiceController {

    private final VendorInvoiceService vendorInvoiceService;

    public VendorInvoiceController(VendorInvoiceService vendorInvoiceService) {
        this.vendorInvoiceService = vendorInvoiceService;
    }

    @PostMapping(Endpoints.VendorPortal.INVOICE_PO_FLIP)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupplierInvoiceResponse> createPoFlipInvoice(
            @PathVariable("poId") UUID poId,
            @Valid @RequestBody PoFlipInvoiceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        SupplierInvoiceResponse response = vendorInvoiceService.createPoFlipInvoice(poId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = Endpoints.VendorPortal.INVOICE_UPLOAD_UBL, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupplierInvoiceResponse> uploadUblXmlInvoice(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        SupplierInvoiceResponse response = vendorInvoiceService.uploadUblXmlInvoice(file, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SupplierInvoiceResponse>> getVendorInvoices(
            @RequestParam(name = "status", required = false) InvoiceStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        List<SupplierInvoiceResponse> invoices = vendorInvoiceService.getVendorInvoices(status, principal.getId());
        return ResponseEntity.ok(invoices);
    }

    @GetMapping(Endpoints.VendorPortal.INVOICE_BY_ID)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupplierInvoiceDetailResponse> getVendorInvoiceDetail(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        SupplierInvoiceDetailResponse detail = vendorInvoiceService.getVendorInvoiceDetail(id, principal.getId());
        return ResponseEntity.ok(detail);
    }

    @GetMapping(value = Endpoints.VendorPortal.INVOICE_HTML, produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getInvoiceHtml(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        validateVendorAccess(principal);
        String html = vendorInvoiceService.getInvoiceHtml(id, principal.getId());
        return ResponseEntity.ok(html);
    }

    private void validateVendorAccess(UserPrincipal principal) {
        if (principal == null || !principal.isVendor() || principal.getVendorId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access restricted to authenticated vendor users");
        }
    }
}
