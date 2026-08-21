package com.enterprise.spendsync.core.internal.web;

import com.enterprise.spendsync.core.internal.dto.AcceptSubAccountInviteRequest;
import com.enterprise.spendsync.core.internal.dto.AuthTokenResponse;
import com.enterprise.spendsync.core.internal.dto.JoinAsRequisitionerRequest;
import com.enterprise.spendsync.core.internal.dto.LoginRequest;
import com.enterprise.spendsync.core.internal.dto.RefreshTokenRequest;
import com.enterprise.spendsync.core.internal.dto.RegisterUserRequest;
import com.enterprise.spendsync.core.internal.dto.RequisitionerLinkDetailsResponse;
import com.enterprise.spendsync.core.internal.dto.SubAccountInvitationDetailsResponse;
import com.enterprise.spendsync.core.internal.dto.UserResponse;
import com.enterprise.spendsync.core.internal.service.AuthService;
import com.enterprise.spendsync.core.internal.service.RequisitionerInvitationService;
import com.enterprise.spendsync.core.internal.service.SubAccountInvitationService;
import com.enterprise.spendsync.core.internal.service.UserService;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.enterprise.spendsync.shared.ratelimit.RateLimit;
import com.enterprise.spendsync.shared.ratelimit.RateLimitType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Authentication, Login and User Onboarding REST Controller.
 */
@RestController
@RequestMapping(Endpoints.Auth.BASE)
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final SubAccountInvitationService subAccountInvitationService;
    private final RequisitionerInvitationService requisitionerInvitationService;

    public AuthController(UserService userService,
                          AuthService authService,
                          SubAccountInvitationService subAccountInvitationService,
                          RequisitionerInvitationService requisitionerInvitationService) {
        this.userService = userService;
        this.authService = authService;
        this.subAccountInvitationService = subAccountInvitationService;
        this.requisitionerInvitationService = requisitionerInvitationService;
    }

    @PostMapping(Endpoints.Auth.REGISTER_USER)
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(Endpoints.Auth.LOGIN)
    @RateLimit(key = "login", limit = 5, periodSeconds = 60, type = RateLimitType.IP)
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(Endpoints.Auth.REFRESH)
    public ResponseEntity<AuthTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(Endpoints.Auth.LOGOUT)
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(Endpoints.Auth.USERS_BY_ID)
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping(Endpoints.Auth.SUBACCOUNT_INVITE_DETAILS)
    public ResponseEntity<SubAccountInvitationDetailsResponse> getSubAccountInviteDetails(@PathVariable String token) {
        SubAccountInvitationDetailsResponse response = subAccountInvitationService.getSubAccountInvitationDetails(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping(Endpoints.Auth.ACCEPT_SUBACCOUNT_INVITE)
    public ResponseEntity<UserResponse> acceptSubAccountInvite(@Valid @RequestBody AcceptSubAccountInviteRequest request) {
        UserResponse response = subAccountInvitationService.acceptSubAccountInvite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.Auth.REQUISITIONER_INVITE_DETAILS)
    public ResponseEntity<RequisitionerLinkDetailsResponse> getRequisitionerInviteDetails(@PathVariable String token) {
        RequisitionerLinkDetailsResponse response = requisitionerInvitationService.getRequisitionerLinkDetails(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping(Endpoints.Auth.JOIN_AS_REQUISITIONER)
    public ResponseEntity<UserResponse> joinAsRequisitioner(@Valid @RequestBody JoinAsRequisitionerRequest request) {
        UserResponse response = requisitionerInvitationService.joinAsRequisitioner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
