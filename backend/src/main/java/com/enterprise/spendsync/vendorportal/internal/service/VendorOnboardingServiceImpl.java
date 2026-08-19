package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;
import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import com.enterprise.spendsync.purchasing.internal.repository.VendorRepository;
import com.enterprise.spendsync.shared.security.JwtTokenProvider;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.VendorAcceptInviteRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorAuthResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorInvitationDetailsResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorInviteRequest;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorInvitation;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorInvitationStatus;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorUser;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorInvitationRepository;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class VendorOnboardingServiceImpl implements VendorOnboardingService {

    private final VendorInvitationRepository invitationRepository;
    private final VendorUserRepository vendorUserRepository;
    private final VendorRepository vendorRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public VendorOnboardingServiceImpl(
            VendorInvitationRepository invitationRepository,
            VendorUserRepository vendorUserRepository,
            VendorRepository vendorRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuditService auditService) {
        this.invitationRepository = invitationRepository;
        this.vendorUserRepository = vendorUserRepository;
        this.vendorRepository = vendorRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public VendorInvitationDetailsResponse inviteVendor(VendorInviteRequest request, UUID invitedByUserId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        if (vendorUserRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A vendor user with this email already exists");
        }

        // Check if there is already an active pending invitation
        invitationRepository.findByTenantIdAndEmailAndStatus(tenantId, request.email(), VendorInvitationStatus.PENDING)
                .ifPresent(existing -> {
                    if (!existing.isExpired()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "An active invitation is already pending for this email");
                    }
                    existing.setStatus(VendorInvitationStatus.EXPIRED);
                    invitationRepository.save(existing);
                });

        String token = "v_inv_" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        VendorInvitation invitation = new VendorInvitation(
                tenant,
                request.email(),
                request.taxNumber(),
                request.companyName(),
                token,
                expiresAt,
                invitedByUserId
        );

        VendorInvitation saved = invitationRepository.save(invitation);

        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.USER_INVITED,
                ComplianceTag.ISO_37001_ANTI_BRIBERY,
                invitedByUserId,
                null,
                "BUYER",
                "127.0.0.1",
                "BuyerPortal",
                "VendorInvitation",
                saved.getId().toString(),
                null,
                null,
                null,
                null,
                null,
                "PENDING",
                "Issued onboarding invitation for supplier: " + request.companyName() + " (" + request.email() + ")",
                "{\"email\":\"" + request.email() + "\",\"taxNumberMasked\":\"" + com.enterprise.spendsync.shared.crypto.MaskingUtils.maskTaxNumber(request.taxNumber()) + "\"}"
        ));

        return toDetailsResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorInvitationDetailsResponse getInvitationDetails(String token) {
        VendorInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invitation token"));

        if (invitation.getStatus() == VendorInvitationStatus.PENDING && invitation.isExpired()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Invitation has expired");
        }

        return toDetailsResponse(invitation);
    }

    @Override
    @Transactional
    public VendorAuthResponse acceptInvitation(VendorAcceptInviteRequest request) {
        VendorInvitation invitation = invitationRepository.findByInvitationToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invitation token"));

        if (invitation.getStatus() != VendorInvitationStatus.PENDING || invitation.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is no longer valid");
        }

        Tenant tenant = invitation.getTenant();
        TenantContext.setTenantId(tenant.getId());

        // 1. Establish or Link Vendor Master entity
        Vendor vendor = vendorRepository.findByTaxNumberAndTenantId(invitation.getTaxNumber(), tenant.getId())
                .orElseGet(() -> {
                    Vendor newVendor = new Vendor(
                            tenant,
                            invitation.getCompanyName(),
                            invitation.getTaxNumber(),
                            request.taxOffice(),
                            VendorCategory.IT_HARDWARE,
                            VendorTier.TIER_3_STANDARD,
                            true,
                            invitation.getEmail(),
                            request.phoneNumber(),
                            request.address(),
                            request.city(),
                            request.country() != null ? request.country() : "TR",
                            PaymentTerms.NET_30,
                            request.bankName(),
                            request.iban()
                    );
                    return vendorRepository.save(newVendor);
                });

        // 2. Create primary VendorUser
        VendorUser vendorUser = new VendorUser(
                tenant,
                vendor,
                invitation.getEmail(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.phoneNumber(),
                RoleType.VENDOR_ADMIN,
                true
        );
        vendorUser.setLastLoginAt(Instant.now());
        VendorUser savedUser = vendorUserRepository.save(vendorUser);

        // 3. Mark invitation accepted
        invitation.setStatus(VendorInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);

        // 4. Generate Vendor JWT Token
        String accessToken = jwtTokenProvider.generateVendorAccessToken(savedUser);

        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.USER_REGISTERED,
                ComplianceTag.ISO_37001_ANTI_BRIBERY,
                savedUser.getId(),
                savedUser.getEmail(),
                "VENDOR_ADMIN",
                "127.0.0.1",
                "VendorPortalOnboarding",
                "VendorUser",
                savedUser.getId().toString(),
                null,
                null,
                null,
                null,
                "PENDING",
                "ACTIVE",
                "Completed self-onboarding for vendor: " + vendor.getName() + " by " + request.fullName(),
                "{\"vendorId\":\"" + vendor.getId() + "\",\"email\":\"" + savedUser.getEmail() + "\"}"
        ));

        return new VendorAuthResponse(
                accessToken,
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirySeconds(),
                savedUser.getId(),
                vendor.getId(),
                tenant.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                vendor.getName(),
                Set.of(savedUser.getRole().name())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorInvitationDetailsResponse> listInvitations() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return invitationRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toDetailsResponse)
                .toList();
    }

    private VendorInvitationDetailsResponse toDetailsResponse(VendorInvitation inv) {
        return new VendorInvitationDetailsResponse(
                inv.getId(),
                inv.getInvitationToken(),
                inv.getEmail(),
                inv.getTaxNumber(),
                inv.getCompanyName(),
                inv.getStatus().name(),
                inv.getExpiresAt()
        );
    }
}
