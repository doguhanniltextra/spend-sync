package com.enterprise.spendsync.vendorportal.internal.web;

import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.vendorportal.dto.VendorAcceptInviteRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorAuthResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorInvitationDetailsResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorLoginRequest;
import com.enterprise.spendsync.vendorportal.internal.service.VendorAuthService;
import com.enterprise.spendsync.vendorportal.internal.service.VendorOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Endpoints.VendorPortal.AUTH_BASE)
public class VendorAuthController {

    private final VendorOnboardingService onboardingService;
    private final VendorAuthService authService;

    public VendorAuthController(VendorOnboardingService onboardingService, VendorAuthService authService) {
        this.onboardingService = onboardingService;
        this.authService = authService;
    }

    @GetMapping(Endpoints.VendorPortal.INVITE_DETAILS)
    public ResponseEntity<VendorInvitationDetailsResponse> getInvitationDetails(@PathVariable String token) {
        VendorInvitationDetailsResponse response = onboardingService.getInvitationDetails(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping(Endpoints.VendorPortal.ACCEPT_INVITE)
    public ResponseEntity<VendorAuthResponse> acceptInvite(@Valid @RequestBody VendorAcceptInviteRequest request) {
        VendorAuthResponse response = onboardingService.acceptInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(Endpoints.VendorPortal.LOGIN)
    public ResponseEntity<VendorAuthResponse> login(@Valid @RequestBody VendorLoginRequest request) {
        VendorAuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
