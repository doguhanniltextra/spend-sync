package com.enterprise.spendsync.core.auth;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.dto.*;
import com.enterprise.spendsync.core.internal.service.AuthService;
import com.enterprise.spendsync.core.internal.service.RequisitionerInvitationService;
import com.enterprise.spendsync.core.internal.service.SubAccountInvitationService;
import com.enterprise.spendsync.core.internal.service.UserService;
import com.enterprise.spendsync.core.internal.web.AuthController;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController REST Web API Controller Tests")
class AuthControllerWebMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private SubAccountInvitationService subAccountInvitationService;

    @Mock
    private RequisitionerInvitationService requisitionerInvitationService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - returns 200 OK and JWT auth tokens")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("admin@spendsync.com", "Password123!");
        AuthTokenResponse tokenResponse = new AuthTokenResponse(
                "mock-access-token",
                "mock-refresh-token",
                3600L,
                "Bearer",
                UUID.randomUUID(),
                "admin@spendsync.com",
                "Admin User",
                UUID.randomUUID(),
                Set.of("ROLE_ROOT_USER")
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post(Endpoints.Auth.BASE + Endpoints.Auth.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("admin@spendsync.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - returns 200 OK and refreshed tokens")
    void shouldRefreshTokenSuccessfully() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        AuthTokenResponse tokenResponse = new AuthTokenResponse(
                "new-access-token",
                "new-refresh-token",
                3600L,
                "Bearer",
                UUID.randomUUID(),
                "admin@spendsync.com",
                "Admin User",
                UUID.randomUUID(),
                Set.of("ROLE_ROOT_USER")
        );

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post(Endpoints.Auth.BASE + Endpoints.Auth.REFRESH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - returns 204 No Content")
    void shouldLogoutSuccessfully() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("logout-token");

        mockMvc.perform(post(Endpoints.Auth.BASE + Endpoints.Auth.LOGOUT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/auth/users/{id} - returns 200 OK and user profile")
    void shouldGetUserById() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(
                userId,
                "user@spendsync.com",
                "John",
                "Doe",
                "John Doe",
                "Manager",
                "+90 555 111 2233",
                "TR",
                "Europe/Istanbul",
                "tr",
                true,
                true,
                Set.of(RoleType.PROCUREMENT),
                Instant.now()
        );

        when(userService.getUserById(userId)).thenReturn(response);

        mockMvc.perform(get(Endpoints.Auth.BASE + "/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@spendsync.com"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/invitations/subaccount/{token} - returns 200 OK and invitation details")
    void shouldGetSubAccountInviteDetails() throws Exception {
        String token = "invite-token-abc";
        SubAccountInvitationDetailsResponse response = new SubAccountInvitationDetailsResponse(
                "SpendSync Global",
                "SpendSync Turkey",
                "sub@spendsync.com",
                Set.of(RoleType.PROCUREMENT),
                true,
                Instant.now().plusSeconds(3600)
        );

        when(subAccountInvitationService.getSubAccountInvitationDetails(token)).thenReturn(response);

        mockMvc.perform(get(Endpoints.Auth.BASE + "/invitations/subaccount/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("SpendSync Global"))
                .andExpect(jsonPath("$.email").value("sub@spendsync.com"))
                .andExpect(jsonPath("$.isValid").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/accept-subaccount-invite - returns 201 Created")
    void shouldAcceptSubAccountInvite() throws Exception {
        AcceptSubAccountInviteRequest request = new AcceptSubAccountInviteRequest(
                "invite-token-123",
                "Ali",
                "Demir",
                "StrongP@ssw0rd1",
                "+90 532 111 2233",
                "Procurement Lead",
                "TR",
                "Europe/Istanbul",
                "tr"
        );

        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(),
                "ali@spendsync.com",
                "Ali",
                "Demir",
                "Ali Demir",
                "Procurement Lead",
                "+90 532 111 2233",
                "TR",
                "Europe/Istanbul",
                "tr",
                true,
                true,
                Set.of(RoleType.PROCUREMENT),
                Instant.now()
        );

        when(subAccountInvitationService.acceptSubAccountInvite(any(AcceptSubAccountInviteRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post(Endpoints.Auth.BASE + Endpoints.Auth.ACCEPT_SUBACCOUNT_INVITE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ali@spendsync.com"))
                .andExpect(jsonPath("$.firstName").value("Ali"));
    }
}
