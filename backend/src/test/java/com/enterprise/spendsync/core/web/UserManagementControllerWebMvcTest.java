package com.enterprise.spendsync.core.web;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.dto.*;
import com.enterprise.spendsync.core.internal.service.RequisitionerInvitationService;
import com.enterprise.spendsync.core.internal.service.SubAccountInvitationService;
import com.enterprise.spendsync.core.internal.service.UserManagementService;
import com.enterprise.spendsync.core.internal.web.UserManagementController;
import com.enterprise.spendsync.shared.config.Endpoints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementController REST Web API Slice Tests")
class UserManagementControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private UserManagementService userManagementService;

    @Mock
    private SubAccountInvitationService subAccountInvitationService;

    @Mock
    private RequisitionerInvitationService requisitionerInvitationService;

    @InjectMocks
    private UserManagementController userManagementController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userManagementController).build();
    }

    @Test
    @DisplayName("GET /api/v1/organization/users - returns 200 OK and list of users")
    void shouldGetAllUsers() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse u = new UserResponse(
                userId, "user@spendsync.com", "John", "Doe", "John Doe", "Lead",
                "+90 555 123", "TR", "Europe/Istanbul", "tr", true, true,
                Set.of(RoleType.PROCUREMENT), Instant.now()
        );

        when(userManagementService.getAllUsers()).thenReturn(List.of(u));

        mockMvc.perform(get(Endpoints.Organization.BASE + Endpoints.Organization.USERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userId.toString()))
                .andExpect(jsonPath("$[0].email").value("user@spendsync.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/organization/users/{id}/roles - updates user roles")
    void shouldUpdateUserRoles() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRolesRequest req = new UpdateUserRolesRequest(Set.of(RoleType.APPROVER, RoleType.PROCUREMENT));

        UserResponse updated = new UserResponse(
                userId, "user@spendsync.com", "John", "Doe", "John Doe", "Lead",
                "+90 555 123", "TR", "Europe/Istanbul", "tr", true, true,
                Set.of(RoleType.APPROVER, RoleType.PROCUREMENT), Instant.now()
        );

        when(userManagementService.updateUserRoles(eq(userId), any(UpdateUserRolesRequest.class))).thenReturn(updated);

        mockMvc.perform(put(Endpoints.Organization.BASE + "/users/" + userId + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    @DisplayName("PATCH /api/v1/organization/users/{id}/status - updates active status")
    void shouldUpdateUserStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateStatusRequest req = new UpdateStatusRequest(false);

        UserResponse updated = new UserResponse(
                userId, "user@spendsync.com", "John", "Doe", "John Doe", "Lead",
                "+90 555 123", "TR", "Europe/Istanbul", "tr", false, true,
                Set.of(RoleType.PROCUREMENT), Instant.now()
        );

        when(userManagementService.updateStatus(eq(userId), eq(false))).thenReturn(updated);

        mockMvc.perform(patch(Endpoints.Organization.BASE + "/users/" + userId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/organization/users/invite-subaccount - invites subaccount")
    void shouldInviteSubAccount() throws Exception {
        UUID legalEntityId = UUID.randomUUID();
        InviteSubAccountRequest req = new InviteSubAccountRequest(
                "new.buyer@spendsync.com",
                legalEntityId,
                Set.of(RoleType.PROCUREMENT),
                48
        );

        SubAccountInvitationResponse response = new SubAccountInvitationResponse(
                UUID.randomUUID(),
                "new.buyer@spendsync.com",
                legalEntityId,
                "SpendSync Turkey",
                Set.of(RoleType.PROCUREMENT),
                "64hexToken",
                "https://app.spendsync.com/accept-invite?token=64hexToken",
                false,
                Instant.now().plusSeconds(3600),
                Instant.now()
        );

        when(subAccountInvitationService.inviteSubAccount(any(InviteSubAccountRequest.class))).thenReturn(response);

        mockMvc.perform(post(Endpoints.Organization.BASE + Endpoints.Organization.INVITE_SUBACCOUNT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.buyer@spendsync.com"))
                .andExpect(jsonPath("$.inviteToken").value("64hexToken"));
    }

    @Test
    @DisplayName("DELETE /api/v1/organization/invitations/{id} - revokes invitation")
    void shouldRevokeInvitation() throws Exception {
        UUID invId = UUID.randomUUID();

        mockMvc.perform(delete(Endpoints.Organization.BASE + "/invitations/" + invId))
                .andExpect(status().isNoContent());

        verify(subAccountInvitationService).revokeInvitation(invId);
    }
}
