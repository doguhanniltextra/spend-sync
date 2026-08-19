package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;
import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.shared.security.JwtTokenProvider;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.VendorAuthResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorLoginRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorProfileResponse;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorUser;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class VendorAuthServiceImpl implements VendorAuthService {

    private final VendorUserRepository vendorUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public VendorAuthServiceImpl(
            VendorUserRepository vendorUserRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuditService auditService) {
        this.vendorUserRepository = vendorUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public VendorAuthResponse login(VendorLoginRequest request) {
        VendorUser user = vendorUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendor account is deactivated. Contact buyer organization.");
        }

        TenantContext.setTenantId(user.getTenant().getId());
        user.setLastLoginAt(Instant.now());
        vendorUserRepository.save(user);

        String accessToken = jwtTokenProvider.generateVendorAccessToken(user);

        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.USER_LOGGED_IN,
                ComplianceTag.ISO_27001_ACCESS_CONTROL,
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                "127.0.0.1",
                "VendorPortalAuth",
                "VendorUser",
                user.getId().toString(),
                null,
                null,
                null,
                null,
                null,
                "LOGGED_IN",
                "Vendor login successful",
                "{\"vendorId\":\"" + user.getVendor().getId() + "\"}"
        ));

        return new VendorAuthResponse(
                accessToken,
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirySeconds(),
                user.getId(),
                user.getVendor().getId(),
                user.getTenant().getId(),
                user.getEmail(),
                user.getFullName(),
                user.getVendor().getName(),
                Set.of(user.getRole().name())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VendorProfileResponse getVendorProfile(UUID vendorUserId) {
        VendorUser user = vendorUserRepository.findById(vendorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor user not found"));

        Vendor vendor = user.getVendor();

        return new VendorProfileResponse(
                vendor.getId(),
                user.getId(),
                vendor.getName(),
                vendor.getTaxNumber(),
                com.enterprise.spendsync.shared.crypto.MaskingUtils.maskTaxNumber(vendor.getTaxNumber()),
                vendor.getTaxOffice(),
                vendor.getCategory().name(),
                vendor.getTier().name(),
                vendor.getOrderEmail(),
                vendor.getPhoneNumber(),
                vendor.getAddress(),
                vendor.getCity(),
                vendor.getCountry(),
                vendor.getPaymentTerms().name(),
                vendor.getBankName(),
                vendor.getIban(),
                com.enterprise.spendsync.shared.crypto.MaskingUtils.maskIban(vendor.getIban()),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }
}
