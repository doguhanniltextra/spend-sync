package com.enterprise.spendsync.core.internal.web;

import com.enterprise.spendsync.core.internal.dto.AcceptSubAccountInviteRequest;
import com.enterprise.spendsync.core.internal.dto.RegisterUserRequest;
import com.enterprise.spendsync.core.internal.dto.SubAccountInvitationDetailsResponse;
import com.enterprise.spendsync.core.internal.dto.UserResponse;
import com.enterprise.spendsync.core.internal.service.SubAccountInvitationService;
import com.enterprise.spendsync.core.internal.service.UserService;
import com.enterprise.spendsync.shared.config.Endpoints;
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
 * Authentication and User Onboarding REST Controller.
 */
@RestController
@RequestMapping(Endpoints.Auth.BASE)
public class AuthController {

    private final UserService userService;
    private final SubAccountInvitationService invitationService;

    public AuthController(UserService userService, SubAccountInvitationService invitationService) {
        this.userService = userService;
        this.invitationService = invitationService;
    }

    @PostMapping(Endpoints.Auth.REGISTER_USER)
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.Auth.USERS_BY_ID)
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping(Endpoints.Auth.SUBACCOUNT_INVITE_DETAILS)
    public ResponseEntity<SubAccountInvitationDetailsResponse> getSubAccountInviteDetails(@PathVariable String token) {
        SubAccountInvitationDetailsResponse response = invitationService.getSubAccountInvitationDetails(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping(Endpoints.Auth.ACCEPT_SUBACCOUNT_INVITE)
    public ResponseEntity<UserResponse> acceptSubAccountInvite(@Valid @RequestBody AcceptSubAccountInviteRequest request) {
        UserResponse response = invitationService.acceptSubAccountInvite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
