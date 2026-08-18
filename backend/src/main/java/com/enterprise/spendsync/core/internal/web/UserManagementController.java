package com.enterprise.spendsync.core.internal.web;

import com.enterprise.spendsync.core.internal.dto.GenerateRequisitionerLinkRequest;
import com.enterprise.spendsync.core.internal.dto.InviteSubAccountRequest;
import com.enterprise.spendsync.core.internal.dto.RequisitionerLinkResponse;
import com.enterprise.spendsync.core.internal.dto.SubAccountInvitationResponse;
import com.enterprise.spendsync.core.internal.dto.UpdateStatusRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateUserLegalEntitiesRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateUserRolesRequest;
import com.enterprise.spendsync.core.internal.dto.UserResponse;
import com.enterprise.spendsync.core.internal.service.RequisitionerInvitationService;
import com.enterprise.spendsync.core.internal.service.SubAccountInvitationService;
import com.enterprise.spendsync.core.internal.service.UserManagementService;
import com.enterprise.spendsync.shared.config.Endpoints;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.Organization.BASE)
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final SubAccountInvitationService subAccountInvitationService;
    private final RequisitionerInvitationService requisitionerInvitationService;

    public UserManagementController(UserManagementService userManagementService,
                                  SubAccountInvitationService subAccountInvitationService,
                                  RequisitionerInvitationService requisitionerInvitationService) {
        this.userManagementService = userManagementService;
        this.subAccountInvitationService = subAccountInvitationService;
        this.requisitionerInvitationService = requisitionerInvitationService;
    }

    @GetMapping(Endpoints.Organization.USERS)
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userManagementService.getAllUsers());
    }

    @GetMapping(Endpoints.Organization.USERS + "/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userManagementService.getUserById(id));
    }

    @PutMapping(Endpoints.Organization.USER_ROLES)
    public ResponseEntity<UserResponse> updateRoles(@PathVariable UUID id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(userManagementService.updateUserRoles(id, request));
    }

    @PutMapping(Endpoints.Organization.USER_LEGAL_ENTITIES)
    public ResponseEntity<UserResponse> updateLegalEntities(@PathVariable UUID id, @Valid @RequestBody UpdateUserLegalEntitiesRequest request) {
        return ResponseEntity.ok(userManagementService.updateUserLegalEntities(id, request));
    }

    @PatchMapping(Endpoints.Organization.USER_STATUS)
    public ResponseEntity<UserResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(userManagementService.updateStatus(id, request.isActive()));
    }

    @PostMapping(Endpoints.Organization.INVITE_SUBACCOUNT)
    public ResponseEntity<SubAccountInvitationResponse> inviteSubAccount(@Valid @RequestBody InviteSubAccountRequest request) {
        SubAccountInvitationResponse response = subAccountInvitationService.inviteSubAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(Endpoints.Organization.GENERATE_REQUISITIONER_LINK)
    public ResponseEntity<RequisitionerLinkResponse> generateRequisitionerLink(@Valid @RequestBody GenerateRequisitionerLinkRequest request) {
        RequisitionerLinkResponse response = requisitionerInvitationService.generateRequisitionerLink(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(Endpoints.Organization.INVITATIONS)
    public ResponseEntity<List<SubAccountInvitationResponse>> getAllActiveInvitations() {
        return ResponseEntity.ok(subAccountInvitationService.getAllActiveInvitations());
    }

    @DeleteMapping(Endpoints.Organization.INVITATION_BY_ID)
    public ResponseEntity<Void> revokeInvitation(@PathVariable UUID id) {
        subAccountInvitationService.revokeInvitation(id);
        return ResponseEntity.noContent().build();
    }
}
