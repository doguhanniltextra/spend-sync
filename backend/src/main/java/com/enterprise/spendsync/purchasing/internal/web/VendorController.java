package com.enterprise.spendsync.purchasing.internal.web;

import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorStatus;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import com.enterprise.spendsync.purchasing.internal.dto.CreateVendorRequest;
import com.enterprise.spendsync.purchasing.internal.dto.UpdateVendorStatusRequest;
import com.enterprise.spendsync.purchasing.internal.dto.VendorResponse;
import com.enterprise.spendsync.purchasing.internal.service.VendorService;
import com.enterprise.spendsync.shared.config.Endpoints;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Purchasing.VENDORS_BASE)
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_VENDOR_MANAGE')")
    public ResponseEntity<VendorResponse> createVendor(@Valid @RequestBody CreateVendorRequest request) {
        VendorResponse response = vendorService.createVendor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.Purchasing.VENDOR_BY_ID)
    @PreAuthorize("hasAnyAuthority('PERM_PO_READ', 'PERM_VENDOR_MANAGE', 'PERM_PO_CREATE')")
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable UUID id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_PO_READ', 'PERM_VENDOR_MANAGE', 'PERM_PO_CREATE')")
    public ResponseEntity<List<VendorResponse>> getAllVendors(
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(required = false) VendorCategory category,
            @RequestParam(required = false) VendorTier tier) {
        return ResponseEntity.ok(vendorService.getAllVendors(status, category, tier));
    }

    @PatchMapping(Endpoints.Purchasing.VENDOR_STATUS)
    @PreAuthorize("hasAuthority('PERM_VENDOR_MANAGE')")
    public ResponseEntity<VendorResponse> updateVendorStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVendorStatusRequest request) {
        return ResponseEntity.ok(vendorService.updateVendorStatus(id, request));
    }
}
