package com.enterprise.spendsync.core.internal.service;

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
import com.enterprise.spendsync.shared.exception.EmailAlreadyExistsException;
import com.enterprise.spendsync.shared.exception.InvalidInvitationTokenException;
import com.enterprise.spendsync.shared.exception.InvalidPasswordException;
import com.enterprise.spendsync.shared.exception.InvitationExpiredException;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class RequisitionerInvitationServiceImpl implements RequisitionerInvitationService {

    private static final String APP_BASE_URL = "https://app.spendsync.com";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final PasswordEncoder passwordEncoder;

    public RequisitionerInvitationServiceImpl(UserInvitationRepository invitationRepository,
                                             UserRepository userRepository,
                                             TenantRepository tenantRepository,
                                             LegalEntityRepository legalEntityRepository,
                                             PasswordEncoder passwordEncoder) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RequisitionerLinkResponse generateRequisitionerLink(GenerateRequisitionerLinkRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        LegalEntity legalEntity = legalEntityRepository.findById(request.targetLegalEntityId())
                .orElseThrow(() -> new SpendSyncException("Legal entity not found", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {});

        if (!legalEntity.getTenant().getId().equals(tenantId)) {
            throw new SpendSyncException("Legal entity does not belong to active tenant.", HttpStatus.FORBIDDEN, "CROSS_TENANT_ACCESS_DENIED") {};
        }

        // Generate 256-bit cryptographically secure token
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = HexFormat.of().formatHex(randomBytes);

        int expirationDays = (request.expirationDays() != null && request.expirationDays() > 0)
                ? request.expirationDays()
                : 7;
        Instant expiresAt = Instant.now().plus(expirationDays, ChronoUnit.DAYS);

        // Multi-use Requisitioner invitation (email is null, isMultiUse is true)
        UserInvitation invitation = new UserInvitation(
                tenant,
                null,
                legalEntity,
                token,
                true,
                expiresAt
        );
        invitation.setTargetRoles(Set.of(RoleType.REQUISITIONER));
        UserInvitation savedInvitation = invitationRepository.save(invitation);

        return RequisitionerLinkResponse.fromEntity(savedInvitation, APP_BASE_URL);
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionerLinkDetailsResponse getRequisitionerLinkDetails(String token) {
        UserInvitation invitation = findValidRequisitionerLinkOrThrow(token);

        return new RequisitionerLinkDetailsResponse(
                invitation.getTenant().getName(),
                invitation.getTargetLegalEntity() != null ? invitation.getTargetLegalEntity().getName() : null,
                RoleType.REQUISITIONER.name(),
                true,
                invitation.getExpiresAt()
        );
    }

    @Override
    public UserResponse joinAsRequisitioner(JoinAsRequisitionerRequest request) {
        UserInvitation invitation = findValidRequisitionerLinkOrThrow(request.token());
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        if (request.password() == null || !PASSWORD_PATTERN.matcher(request.password()).matches()) {
            throw new InvalidPasswordException("Password does not meet enterprise security criteria.");
        }

        String country = request.country() != null ? request.country().trim().toUpperCase() : "TR";
        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                request.phoneNumber() != null ? request.phoneNumber().trim() : null,
                country
        );

        user.setTenant(invitation.getTenant());
        if (invitation.getTargetLegalEntity() != null) {
            user.assignLegalEntity(invitation.getTargetLegalEntity());
        }
        user.addRole(RoleType.REQUISITIONER);
        user.setEmailVerified(true);
        user.setJobTitle(request.jobTitle() != null ? request.jobTitle().trim() : null);
        user.setEmployeeId(request.employeeId() != null ? request.employeeId().trim() : null);
        user.setTimezone(request.timezone() != null ? request.timezone().trim() : "Europe/Istanbul");
        user.setPreferredLanguage(request.preferredLanguage() != null ? request.preferredLanguage().trim() : "tr");

        User savedUser = userRepository.save(user);

        // NOTICE: For multi-use links, invitation.isAccepted is NOT set to true so other employees can continue onboarding!

        return UserResponse.fromEntity(savedUser);
    }

    private UserInvitation findValidRequisitionerLinkOrThrow(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidInvitationTokenException("Requisitioner link token is missing.");
        }

        UserInvitation invitation = invitationRepository.findByInviteToken(token.trim())
                .orElseThrow(() -> new InvalidInvitationTokenException("Invalid or unrecognized invitation link."));

        if (!invitation.isMultiUse()) {
            throw new InvalidInvitationTokenException("This link is not a multi-use requisitioner link.");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new InvitationExpiredException("This requisitioner invitation link expired on " + invitation.getExpiresAt() + ".");
        }

        return invitation;
    }
}
