package com.enterprise.spendsync.core.auth;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.domain.UserInvitation;
import com.enterprise.spendsync.core.internal.dto.AcceptSubAccountInviteRequest;
import com.enterprise.spendsync.core.internal.dto.InviteSubAccountRequest;
import com.enterprise.spendsync.core.internal.dto.SubAccountInvitationDetailsResponse;
import com.enterprise.spendsync.core.internal.dto.SubAccountInvitationResponse;
import com.enterprise.spendsync.core.internal.dto.UserResponse;
import com.enterprise.spendsync.core.internal.event.SubAccountInvitedEvent;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserInvitationRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.core.internal.service.SubAccountInvitationServiceImpl;
import com.enterprise.spendsync.shared.exception.EmailAlreadyExistsException;
import com.enterprise.spendsync.shared.exception.InvalidInvitationTokenException;
import com.enterprise.spendsync.shared.exception.InvalidPasswordException;
import com.enterprise.spendsync.shared.exception.InvitationExpiredException;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubAccountInvitationService Unit & Mock Tests (Token, Password & Events)")
class SubAccountInvitationServiceTest {

    @Mock
    private UserInvitationRepository invitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private LegalEntityRepository legalEntityRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SubAccountInvitationServiceImpl invitationService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global Inc.");

        legalEntity = new LegalEntity();
        legalEntity.setId(UUID.randomUUID());
        legalEntity.setName("SpendSync Turkey A.S.");
        legalEntity.setTenant(tenant);
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should generate cryptographically secure invitation and publish SubAccountInvitedEvent")
    void shouldInviteSubAccountSuccessfully() {
        InviteSubAccountRequest request = new InviteSubAccountRequest(
                "buyer.sub@spendsync.com",
                legalEntity.getId(),
                Set.of(RoleType.PROCUREMENT),
                48
        );

        when(userRepository.existsByEmail("buyer.sub@spendsync.com")).thenReturn(false);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findById(legalEntity.getId())).thenReturn(Optional.of(legalEntity));
        when(invitationRepository.save(any(UserInvitation.class))).thenAnswer(i -> {
            UserInvitation inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        SubAccountInvitationResponse response = invitationService.inviteSubAccount(request);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("buyer.sub@spendsync.com");
        assertThat(response.inviteToken()).hasSize(64);
        assertThat(response.inviteUrl()).contains("/accept-invite?token=");

        verify(eventPublisher).publishEvent(any(SubAccountInvitedEvent.class));
    }

    @Test
    @DisplayName("Should reject invitation when email is already registered")
    void shouldRejectWhenEmailAlreadyExists() {
        InviteSubAccountRequest request = new InviteSubAccountRequest(
                "existing@spendsync.com",
                legalEntity.getId(),
                Set.of(RoleType.PROCUREMENT),
                48
        );

        when(userRepository.existsByEmail("existing@spendsync.com")).thenReturn(true);

        assertThatThrownBy(() -> invitationService.inviteSubAccount(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject cross-tenant invitation creation")
    void shouldRejectCrossTenantInvitation() {
        Tenant otherTenant = new Tenant();
        otherTenant.setId(UUID.randomUUID());

        LegalEntity foreignLegalEntity = new LegalEntity();
        foreignLegalEntity.setId(UUID.randomUUID());
        foreignLegalEntity.setTenant(otherTenant);

        InviteSubAccountRequest request = new InviteSubAccountRequest(
                "newuser@spendsync.com",
                foreignLegalEntity.getId(),
                Set.of(RoleType.PROCUREMENT),
                48
        );

        when(userRepository.existsByEmail("newuser@spendsync.com")).thenReturn(false);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findById(foreignLegalEntity.getId())).thenReturn(Optional.of(foreignLegalEntity));

        assertThatThrownBy(() -> invitationService.inviteSubAccount(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(e -> {
                    SpendSyncException se = (SpendSyncException) e;
                    assertThat(se.getErrorCode()).isEqualTo("CROSS_TENANT_ACCESS_DENIED");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("Should accept valid invite, enforce password policy, and create activated user")
    void shouldAcceptInviteSuccessfully() {
        String token = "valid-64-hex-token-string-123456789012345678901234567890123456789012";
        UserInvitation invitation = new UserInvitation(
                tenant,
                "subaccount@spendsync.com",
                legalEntity,
                token,
                false,
                Instant.now().plus(48, ChronoUnit.HOURS)
        );
        invitation.setTargetRoles(Set.of(RoleType.PROCUREMENT));

        AcceptSubAccountInviteRequest request = new AcceptSubAccountInviteRequest(
                token,
                "Ahmet",
                "Yilmaz",
                "StrongP@ssw0rd1",
                "+90 555 123 4567",
                "Senior Buyer",
                "TR",
                "Europe/Istanbul",
                "tr"
        );

        when(invitationRepository.findByInviteToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmail("subaccount@spendsync.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongP@ssw0rd1")).thenReturn("$2a$10$encodedStrongPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse userResponse = invitationService.acceptSubAccountInvite(request);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.email()).isEqualTo("subaccount@spendsync.com");
        assertThat(userResponse.firstName()).isEqualTo("Ahmet");
        assertThat(userResponse.lastName()).isEqualTo("Yilmaz");
        assertThat(invitation.isAccepted()).isTrue();

        verify(invitationRepository).save(invitation);
    }

    @Test
    @DisplayName("Should reject weak password not compliant with ISO 27001 standard policy")
    void shouldRejectWeakPassword() {
        String token = "valid-token";
        UserInvitation invitation = new UserInvitation(
                tenant,
                "test@spendsync.com",
                legalEntity,
                token,
                false,
                Instant.now().plus(48, ChronoUnit.HOURS)
        );

        AcceptSubAccountInviteRequest request = new AcceptSubAccountInviteRequest(
                token,
                "Ahmet",
                "Yilmaz",
                "weak",
                null,
                null,
                "TR",
                null,
                null
        );

        when(invitationRepository.findByInviteToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmail("test@spendsync.com")).thenReturn(false);

        assertThatThrownBy(() -> invitationService.acceptSubAccountInvite(request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Password does not meet enterprise security criteria");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject expired invitation token")
    void shouldRejectExpiredInvitation() {
        String token = "expired-token";
        UserInvitation invitation = new UserInvitation(
                tenant,
                "test@spendsync.com",
                legalEntity,
                token,
                false,
                Instant.now().minus(1, ChronoUnit.HOURS)
        );

        AcceptSubAccountInviteRequest request = new AcceptSubAccountInviteRequest(
                token,
                "Ali",
                "Veli",
                "ValidP@ss123",
                null, null, "TR", null, null
        );

        when(invitationRepository.findByInviteToken(token)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.acceptSubAccountInvite(request))
                .isInstanceOf(InvitationExpiredException.class)
                .hasMessageContaining("This invitation has expired");
    }

    @Test
    @DisplayName("Should return invitation details when valid token provided")
    void shouldGetInvitationDetails() {
        String token = "active-token";
        UserInvitation invitation = new UserInvitation(
                tenant,
                "invitee@spendsync.com",
                legalEntity,
                token,
                false,
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        invitation.setTargetRoles(Set.of(RoleType.PROCUREMENT));

        when(invitationRepository.findByInviteToken(token)).thenReturn(Optional.of(invitation));

        SubAccountInvitationDetailsResponse details = invitationService.getSubAccountInvitationDetails(token);

        assertThat(details).isNotNull();
        assertThat(details.companyName()).isEqualTo("SpendSync Global Inc.");
        assertThat(details.legalEntityName()).isEqualTo("SpendSync Turkey A.S.");
        assertThat(details.email()).isEqualTo("invitee@spendsync.com");
        assertThat(details.targetRoles()).contains(RoleType.PROCUREMENT);
        assertThat(details.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should reject already accepted invitation token")
    void shouldRejectAlreadyAcceptedToken() {
        String token = "already-used-token";
        UserInvitation invitation = new UserInvitation(
                tenant,
                "used@spendsync.com",
                legalEntity,
                token,
                false,
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        invitation.setAccepted(true);

        when(invitationRepository.findByInviteToken(token)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.getSubAccountInvitationDetails(token))
                .isInstanceOf(InvalidInvitationTokenException.class)
                .hasMessageContaining("already been used and accepted");
    }

    @Test
    @DisplayName("Should fetch all active unaccepted invitations for current tenant")
    void shouldGetAllActiveInvitations() {
        UserInvitation inv1 = new UserInvitation(tenant, "u1@spendsync.com", legalEntity, "t1", false, Instant.now().plus(24, ChronoUnit.HOURS));
        inv1.setId(UUID.randomUUID());
        inv1.setTargetRoles(Set.of(RoleType.PROCUREMENT));

        UserInvitation inv2Expired = new UserInvitation(tenant, "u2@spendsync.com", legalEntity, "t2", false, Instant.now().minus(1, ChronoUnit.HOURS));
        inv2Expired.setId(UUID.randomUUID());

        UserInvitation inv3Accepted = new UserInvitation(tenant, "u3@spendsync.com", legalEntity, "t3", false, Instant.now().plus(24, ChronoUnit.HOURS));
        inv3Accepted.setId(UUID.randomUUID());
        inv3Accepted.setAccepted(true);

        when(invitationRepository.findAllByTenantId(tenantId)).thenReturn(List.of(inv1, inv2Expired, inv3Accepted));

        List<SubAccountInvitationResponse> results = invitationService.getAllActiveInvitations();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).email()).isEqualTo("u1@spendsync.com");
    }

    @Test
    @DisplayName("Should revoke invitation when belongs to same tenant")
    void shouldRevokeInvitationSuccessfully() {
        UUID invitationId = UUID.randomUUID();
        UserInvitation inv = new UserInvitation(tenant, "u1@spendsync.com", legalEntity, "t1", false, Instant.now().plus(24, ChronoUnit.HOURS));
        inv.setId(invitationId);

        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(inv));

        invitationService.revokeInvitation(invitationId);

        verify(invitationRepository).delete(inv);
    }
}
