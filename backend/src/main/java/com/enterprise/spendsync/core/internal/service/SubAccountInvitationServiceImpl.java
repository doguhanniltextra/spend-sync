package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
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
import com.enterprise.spendsync.shared.exception.EmailAlreadyExistsException;
import com.enterprise.spendsync.shared.exception.InvalidInvitationTokenException;
import com.enterprise.spendsync.shared.exception.InvalidPasswordException;
import com.enterprise.spendsync.shared.exception.InvitationExpiredException;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class SubAccountInvitationServiceImpl implements SubAccountInvitationService {

    private static final String APP_BASE_URL = "https://app.spendsync.com";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public SubAccountInvitationServiceImpl(UserInvitationRepository invitationRepository,
            UserRepository userRepository,
            TenantRepository tenantRepository,
            LegalEntityRepository legalEntityRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SubAccountInvitationResponse inviteSubAccount(InviteSubAccountRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        String normalizedEmail = request.email().trim().toLowerCase();

        // 1. Check if user already registered in the platform
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        // 2. Validate Tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(
                        () -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {
                        });

        // 3. Validate Target Legal Entity
        LegalEntity legalEntity = legalEntityRepository.findById(request.targetLegalEntityId())
                .orElseThrow(() -> new SpendSyncException("Legal entity not found", HttpStatus.NOT_FOUND,
                        "LEGAL_ENTITY_NOT_FOUND") {
                });

        if (!legalEntity.getTenant().getId().equals(tenantId)) {
            throw new SpendSyncException("Legal entity does not belong to active tenant.", HttpStatus.FORBIDDEN,
                    "CROSS_TENANT_ACCESS_DENIED") {
            };
        }

        // 4. Generate 256-bit cryptographically secure token
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = HexFormat.of().formatHex(randomBytes);

        int expirationHours = (request.expirationHours() != null && request.expirationHours() > 0)
                ? request.expirationHours()
                : 48;
        Instant expiresAt = Instant.now().plus(expirationHours, ChronoUnit.HOURS);

        // 5. Persist invitation
        UserInvitation invitation = new UserInvitation(
                tenant,
                normalizedEmail,
                legalEntity,
                token,
                false,
                expiresAt);
        invitation.setTargetRoles(new HashSet<>(request.targetRoles()));
        UserInvitation savedInvitation = invitationRepository.save(invitation);

        String inviteUrl = APP_BASE_URL + "/accept-invite?token=" + token;

        // 6. Publish domain event to trigger async email notification
        SubAccountInvitedEvent event = new SubAccountInvitedEvent(
                savedInvitation.getId(),
                tenant.getId(),
                normalizedEmail,
                tenant.getName(),
                legalEntity.getName(),
                new HashSet<>(request.targetRoles()),
                token,
                inviteUrl,
                expiresAt);
        eventPublisher.publishEvent(event);

        return SubAccountInvitationResponse.fromEntity(savedInvitation, APP_BASE_URL);
    }

    @Override
    @Transactional(readOnly = true)
    public SubAccountInvitationDetailsResponse getSubAccountInvitationDetails(String token) {
        UserInvitation invitation = findValidInvitationOrThrow(token);

        return new SubAccountInvitationDetailsResponse(
                invitation.getTenant().getName(),
                invitation.getTargetLegalEntity() != null ? invitation.getTargetLegalEntity().getName() : null,
                invitation.getEmail(),
                new HashSet<>(invitation.getTargetRoles()),
                true,
                invitation.getExpiresAt());
    }

    @Override
    public UserResponse acceptSubAccountInvite(AcceptSubAccountInviteRequest request) {
        UserInvitation invitation = findValidInvitationOrThrow(request.token());

        String email = invitation.getEmail();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        // Validate password against ISO 27001 standard policy
        if (request.password() == null || !PASSWORD_PATTERN.matcher(request.password()).matches()) {
            throw new InvalidPasswordException("Password does not meet enterprise security criteria.");
        }

        // Create User entity
        String country = request.country() != null ? request.country().trim().toUpperCase() : "TR";
        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                request.phoneNumber() != null ? request.phoneNumber().trim() : null,
                country);

        user.setTenant(invitation.getTenant());
        if (invitation.getTargetLegalEntity() != null) {
            user.assignLegalEntity(invitation.getTargetLegalEntity());
        }
        user.setRoles(new HashSet<>(invitation.getTargetRoles()));
        user.setEmailVerified(true);
        user.setJobTitle(request.jobTitle() != null ? request.jobTitle().trim() : null);
        user.setTimezone(request.timezone() != null ? request.timezone().trim() : "Europe/Istanbul");
        user.setPreferredLanguage(request.preferredLanguage() != null ? request.preferredLanguage().trim() : "tr");

        User savedUser = userRepository.save(user);

        // Mark single-use invitation as accepted and close it
        invitation.setAccepted(true);
        invitationRepository.save(invitation);

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubAccountInvitationResponse> getAllActiveInvitations() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return invitationRepository.findAllByTenantId(tenantId).stream()
                .filter(i -> !i.isAccepted() && i.getExpiresAt().isAfter(Instant.now()))
                .map(i -> SubAccountInvitationResponse.fromEntity(i, APP_BASE_URL))
                .toList();
    }

    @Override
    public void revokeInvitation(UUID invitationId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        UserInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new SpendSyncException("Invitation not found", HttpStatus.NOT_FOUND,
                        "INVITATION_NOT_FOUND") {
                });

        if (!invitation.getTenant().getId().equals(tenantId)) {
            throw new SpendSyncException("Invitation does not belong to active tenant.", HttpStatus.FORBIDDEN,
                    "CROSS_TENANT_ACCESS_DENIED") {
            };
        }

        invitationRepository.delete(invitation);
    }

    private UserInvitation findValidInvitationOrThrow(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidInvitationTokenException("Invitation token is missing.");
        }

        UserInvitation invitation = invitationRepository.findByInviteToken(token.trim())
                .orElseThrow(() -> new InvalidInvitationTokenException("Invalid or unrecognized invitation token."));

        if (invitation.isAccepted()) {
            throw new InvalidInvitationTokenException("This invitation has already been used and accepted.");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new InvitationExpiredException("This invitation has expired on " + invitation.getExpiresAt() + ".");
        }

        return invitation;
    }
}
