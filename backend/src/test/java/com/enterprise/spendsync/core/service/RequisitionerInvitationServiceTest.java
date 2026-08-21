package com.enterprise.spendsync.core.service;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.domain.UserInvitation;
import com.enterprise.spendsync.core.internal.dto.GenerateRequisitionerLinkRequest;
import com.enterprise.spendsync.core.internal.dto.JoinAsRequisitionerRequest;
import com.enterprise.spendsync.core.internal.dto.RequisitionerLinkDetailsResponse;
import com.enterprise.spendsync.core.internal.dto.RequisitionerLinkResponse;
import com.enterprise.spendsync.core.internal.dto.UserResponse;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserInvitationRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.core.internal.service.RequisitionerInvitationServiceImpl;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequisitionerInvitationServiceTest {

    @Mock private UserInvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private LegalEntityRepository legalEntityRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RequisitionerInvitationServiceImpl invitationService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant("Acme Corp", "acme-corp");
        tenant.setId(tenantId);

        legalEntity = new LegalEntity(tenant, "Acme TR", "LE-01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should generate requisitioner link with custom expiration")
    void shouldGenerateRequisitionerLink() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findById(legalEntity.getId())).thenReturn(Optional.of(legalEntity));

        UserInvitation savedInvitation = new UserInvitation(
                tenant, null, legalEntity, "secure-token-123", true, Instant.now().plus(14, ChronoUnit.DAYS));
        savedInvitation.setId(UUID.randomUUID());
        savedInvitation.addTargetRole(RoleType.REQUISITIONER);

        when(invitationRepository.save(any())).thenReturn(savedInvitation);

        GenerateRequisitionerLinkRequest request = new GenerateRequisitionerLinkRequest(legalEntity.getId(), 14);
        RequisitionerLinkResponse response = invitationService.generateRequisitionerLink(request);

        assertThat(response).isNotNull();
        assertThat(response.joinUrl()).contains("secure-token-123");
        verify(invitationRepository).save(argThat(UserInvitation::isMultiUse));
    }

    @Test
    @DisplayName("Should throw TENANT_NOT_FOUND when generating link for missing tenant")
    void shouldThrowWhenTenantNotFound() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        GenerateRequisitionerLinkRequest request = new GenerateRequisitionerLinkRequest(legalEntity.getId(), 7);

        assertThatThrownBy(() -> invitationService.generateRequisitionerLink(request))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Tenant not found");
    }

    @Test
    @DisplayName("Should throw LEGAL_ENTITY_NOT_FOUND when target legal entity not found")
    void shouldThrowWhenLegalEntityNotFound() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findById(legalEntity.getId())).thenReturn(Optional.empty());

        GenerateRequisitionerLinkRequest request = new GenerateRequisitionerLinkRequest(legalEntity.getId(), 7);

        assertThatThrownBy(() -> invitationService.generateRequisitionerLink(request))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Legal entity not found");
    }

    @Test
    @DisplayName("Should throw CROSS_TENANT_ACCESS_DENIED when legal entity belongs to another tenant")
    void shouldThrowWhenCrossTenantLegalEntity() {
        Tenant foreignTenant = new Tenant("Foreign Corp", "foreign-corp");
        foreignTenant.setId(UUID.randomUUID());
        legalEntity.setTenant(foreignTenant);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findById(legalEntity.getId())).thenReturn(Optional.of(legalEntity));

        GenerateRequisitionerLinkRequest request = new GenerateRequisitionerLinkRequest(legalEntity.getId(), 7);

        assertThatThrownBy(() -> invitationService.generateRequisitionerLink(request))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Legal entity does not belong to active tenant");
    }

    @Test
    @DisplayName("Should get requisitioner link details for valid token")
    void shouldGetRequisitionerLinkDetails() {
        UserInvitation invitation = new UserInvitation(
                tenant, null, legalEntity, "valid-token", true, Instant.now().plus(5, ChronoUnit.DAYS));

        when(invitationRepository.findByInviteToken("valid-token")).thenReturn(Optional.of(invitation));

        RequisitionerLinkDetailsResponse details = invitationService.getRequisitionerLinkDetails("valid-token");

        assertThat(details.companyName()).isEqualTo("Acme Corp");
        assertThat(details.legalEntityName()).isEqualTo("Acme TR");
        assertThat(details.targetRole()).isEqualTo("REQUISITIONER");
        assertThat(details.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should throw InvalidInvitationTokenException when token is missing or blank")
    void shouldThrowWhenTokenMissing() {
        assertThatThrownBy(() -> invitationService.getRequisitionerLinkDetails(null))
                .isInstanceOf(InvalidInvitationTokenException.class);

        assertThatThrownBy(() -> invitationService.getRequisitionerLinkDetails("   "))
                .isInstanceOf(InvalidInvitationTokenException.class);
    }

    @Test
    @DisplayName("Should throw InvalidInvitationTokenException when invitation is not multi-use")
    void shouldThrowWhenNotMultiUse() {
        UserInvitation singleUse = new UserInvitation(
                tenant, "user@test.com", legalEntity, "single-token", false, Instant.now().plus(5, ChronoUnit.DAYS));

        when(invitationRepository.findByInviteToken("single-token")).thenReturn(Optional.of(singleUse));

        assertThatThrownBy(() -> invitationService.getRequisitionerLinkDetails("single-token"))
                .isInstanceOf(InvalidInvitationTokenException.class)
                .hasMessageContaining("not a multi-use");
    }

    @Test
    @DisplayName("Should throw InvitationExpiredException when token has expired")
    void shouldThrowWhenTokenExpired() {
        UserInvitation expired = new UserInvitation(
                tenant, null, legalEntity, "expired-token", true, Instant.now().minus(1, ChronoUnit.DAYS));

        when(invitationRepository.findByInviteToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> invitationService.getRequisitionerLinkDetails("expired-token"))
                .isInstanceOf(InvitationExpiredException.class);
    }

    @Test
    @DisplayName("Should onboard user via joinAsRequisitioner successfully")
    void shouldJoinAsRequisitioner() {
        UserInvitation invitation = new UserInvitation(
                tenant, null, legalEntity, "token-join", true, Instant.now().plus(5, ChronoUnit.DAYS));

        when(invitationRepository.findByInviteToken("token-join")).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmail("john.doe@acme.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedHash");

        User savedUser = new User("john.doe@acme.com", "encodedHash", "John", "Doe", "555-1234", "TR");
        savedUser.setId(UUID.randomUUID());
        savedUser.setTenant(tenant);
        savedUser.addRole(RoleType.REQUISITIONER);
        when(userRepository.save(any())).thenReturn(savedUser);

        JoinAsRequisitionerRequest request = new JoinAsRequisitionerRequest(
                "token-join", "john.doe@acme.com", "John", "Doe", "SecurePass123!",
                "555-1234", "Engineer", "EMP-100", "TR", "Europe/Istanbul", "tr"
        );

        UserResponse response = invitationService.joinAsRequisitioner(request);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("john.doe@acme.com");
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when joining with registered email")
    void shouldThrowWhenEmailAlreadyExists() {
        UserInvitation invitation = new UserInvitation(
                tenant, null, legalEntity, "token-dup", true, Instant.now().plus(5, ChronoUnit.DAYS));

        when(invitationRepository.findByInviteToken("token-dup")).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmail("existing@acme.com")).thenReturn(true);

        JoinAsRequisitionerRequest request = new JoinAsRequisitionerRequest(
                "token-dup", "existing@acme.com", "Jane", "Doe", "SecurePass123!",
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> invitationService.joinAsRequisitioner(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should throw InvalidPasswordException when password does not meet security criteria")
    void shouldThrowWhenPasswordInvalid() {
        UserInvitation invitation = new UserInvitation(
                tenant, null, legalEntity, "token-weak", true, Instant.now().plus(5, ChronoUnit.DAYS));

        when(invitationRepository.findByInviteToken("token-weak")).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmail("weak@acme.com")).thenReturn(false);

        JoinAsRequisitionerRequest request = new JoinAsRequisitionerRequest(
                "token-weak", "weak@acme.com", "Jane", "Doe", "weak",
                null, null, null, null, null, null
        );

        assertThatThrownBy(() -> invitationService.joinAsRequisitioner(request))
                .isInstanceOf(InvalidPasswordException.class);
    }
}
