package com.enterprise.spendsync.vendorportal.internal.web;

import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.vendorportal.dto.BankChangeDecisionRequest;
import com.enterprise.spendsync.vendorportal.dto.BankChangeRequestDto;
import com.enterprise.spendsync.vendorportal.dto.VendorInvitationDetailsResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorInviteRequest;
import com.enterprise.spendsync.vendorportal.internal.service.VendorBankGovernanceService;
import com.enterprise.spendsync.vendorportal.internal.service.VendorOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Purchasing.VENDORS_BASE)
public class BuyerVendorInviteController {

    private final VendorOnboardingService onboardingService;
    private final VendorBankGovernanceService bankGovernanceService;

    public BuyerVendorInviteController(
            VendorOnboardingService onboardingService,
            VendorBankGovernanceService bankGovernanceService) {
        this.onboardingService = onboardingService;
        this.bankGovernanceService = bankGovernanceService;
    }

    @PostMapping(Endpoints.Purchasing.VENDOR_INVITE)
    @PreAuthorize("hasAnyAuthority('PERM_VENDOR_MANAGE', 'PERM_ORG_MANAGE') or hasRole('ROOT_USER')")
    public ResponseEntity<VendorInvitationDetailsResponse> inviteVendor(
            @Valid @RequestBody VendorInviteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        VendorInvitationDetailsResponse response = onboardingService.inviteVendor(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.Purchasing.VENDOR_INVITE + "/list")
    @PreAuthorize("hasAnyAuthority('PERM_VENDOR_MANAGE', 'PERM_ORG_MANAGE') or hasRole('ROOT_USER')")
    public ResponseEntity<List<VendorInvitationDetailsResponse>> listInvitations() {
        List<VendorInvitationDetailsResponse> response = onboardingService.listInvitations();
        return ResponseEntity.ok(response);
    }

    @GetMapping(Endpoints.Purchasing.BANK_CHANGE_REQUESTS)
    @PreAuthorize("hasAnyAuthority('PERM_VENDOR_MANAGE', 'PERM_ORG_MANAGE', 'PERM_BUDGET_MANAGE') or hasRole('ROOT_USER')")
    public ResponseEntity<List<BankChangeRequestDto.Response>> listPendingBankChangeRequests() {
        List<BankChangeRequestDto.Response> list = bankGovernanceService.listPendingBankChangeRequests();
        return ResponseEntity.ok(list);
    }

    @PostMapping(Endpoints.Purchasing.BANK_CHANGE_APPROVE)
    @PreAuthorize("hasAnyAuthority('PERM_ORG_MANAGE', 'PERM_BUDGET_MANAGE') or hasRole('ROOT_USER')")
    public ResponseEntity<BankChangeRequestDto.Response> approveBankChangeRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) BankChangeDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        BankChangeRequestDto.Response response = bankGovernanceService.approveBankChangeRequest(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping(Endpoints.Purchasing.BANK_CHANGE_REJECT)
    @PreAuthorize("hasAnyAuthority('PERM_ORG_MANAGE', 'PERM_BUDGET_MANAGE') or hasRole('ROOT_USER')")
    public ResponseEntity<BankChangeRequestDto.Response> rejectBankChangeRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) BankChangeDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        BankChangeRequestDto.Response response = bankGovernanceService.rejectBankChangeRequest(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }
}
