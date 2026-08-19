package com.enterprise.spendsync.vendorportal.internal.web;

import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.vendorportal.dto.BankChangeRequestDto;
import com.enterprise.spendsync.vendorportal.dto.VendorProfileResponse;
import com.enterprise.spendsync.vendorportal.internal.service.VendorAuthService;
import com.enterprise.spendsync.vendorportal.internal.service.VendorBankGovernanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Endpoints.VendorPortal.PROFILE_BASE)
public class VendorProfileController {

    private final VendorAuthService authService;
    private final VendorBankGovernanceService bankGovernanceService;

    public VendorProfileController(VendorAuthService authService, VendorBankGovernanceService bankGovernanceService) {
        this.authService = authService;
        this.bankGovernanceService = bankGovernanceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'VENDOR_FINANCE', 'VENDOR_OPERATIONS')")
    public ResponseEntity<VendorProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        VendorProfileResponse response = authService.getVendorProfile(principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping(Endpoints.VendorPortal.BANK_CHANGE_REQUEST)
    @PreAuthorize("hasAuthority('PERM_VENDOR_BANK_MANAGE') or hasRole('VENDOR_ADMIN')")
    public ResponseEntity<BankChangeRequestDto.Response> submitBankChangeRequest(
            @Valid @RequestBody BankChangeRequestDto.Submission request,
            @AuthenticationPrincipal UserPrincipal principal) {
        BankChangeRequestDto.Response response = bankGovernanceService.submitBankChangeRequest(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.VendorPortal.BANK_CHANGE_REQUEST + "s")
    @PreAuthorize("hasAnyRole('VENDOR_ADMIN', 'VENDOR_FINANCE') or hasAuthority('PERM_VENDOR_FINANCE_READ')")
    public ResponseEntity<List<BankChangeRequestDto.Response>> getMyBankChangeRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<BankChangeRequestDto.Response> list = bankGovernanceService.getVendorBankChangeRequests(principal.getId());
        return ResponseEntity.ok(list);
    }
}
