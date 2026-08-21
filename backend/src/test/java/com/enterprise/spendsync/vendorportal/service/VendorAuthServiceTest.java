package com.enterprise.spendsync.vendorportal.service;

import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import com.enterprise.spendsync.shared.security.JwtTokenProvider;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.VendorAuthResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorLoginRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorProfileResponse;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorUser;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorUserRepository;
import com.enterprise.spendsync.vendorportal.internal.service.VendorAuthServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VendorAuthServiceTest {

    @Mock private VendorUserRepository vendorUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuditService auditService;

    @InjectMocks
    private VendorAuthServiceImpl vendorAuthService;

    private UUID tenantId;
    private UUID vendorId;
    private UUID vendorUserId;
    private Tenant tenant;
    private Vendor vendor;
    private VendorUser vendorUser;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        vendorUserId = UUID.randomUUID();

        tenant = new Tenant("Portal Corp", "portal-corp");
        tenant.setId(tenantId);

        vendor = new Vendor(tenant, "Super Supplier", "1234567890", "Maslak", VendorCategory.IT_HARDWARE,
                VendorTier.TIER_1_STRATEGIC, true, "contact@super.com", "555-1111",
                "Buyukdere Cad. No:10", "Istanbul", "TR", PaymentTerms.NET_30, "Is Bankasi", "TR1122334455");
        vendor.setId(vendorId);

        vendorUser = new VendorUser(tenant, vendor, "admin@super.com", "hashedSecret",
                "Ali Veli", "555-2222", RoleType.VENDOR_ADMIN, true);
        vendorUser.setId(vendorUserId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should successfully authenticate vendor and return JWT token")
    void shouldLoginSuccessfully() {
        when(vendorUserRepository.findByEmail("admin@super.com")).thenReturn(Optional.of(vendorUser));
        when(passwordEncoder.matches("rawSecret", "hashedSecret")).thenReturn(true);
        when(jwtTokenProvider.generateVendorAccessToken(vendorUser)).thenReturn("jwt.token.vendor");
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(86400L);

        VendorLoginRequest request = new VendorLoginRequest("admin@super.com", "rawSecret");
        VendorAuthResponse response = vendorAuthService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt.token.vendor");
        assertThat(response.email()).isEqualTo("admin@super.com");
        assertThat(response.companyName()).isEqualTo("Super Supplier");
        assertThat(response.roles()).contains("VENDOR_ADMIN");
        assertThat(TenantContext.getTenantId()).contains(tenantId);
        verify(vendorUserRepository).save(vendorUser);
        verify(auditService).recordAuditLog(any());
    }

    @Test
    @DisplayName("Should throw 401 UNAUTHORIZED when vendor user email not found")
    void shouldThrowWhenEmailNotFound() {
        when(vendorUserRepository.findByEmail("unknown@super.com")).thenReturn(Optional.empty());

        VendorLoginRequest request = new VendorLoginRequest("unknown@super.com", "rawSecret");

        assertThatThrownBy(() -> vendorAuthService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should throw 401 UNAUTHORIZED when password does not match")
    void shouldThrowWhenPasswordMismatch() {
        when(vendorUserRepository.findByEmail("admin@super.com")).thenReturn(Optional.of(vendorUser));
        when(passwordEncoder.matches("wrongSecret", "hashedSecret")).thenReturn(false);

        VendorLoginRequest request = new VendorLoginRequest("admin@super.com", "wrongSecret");

        assertThatThrownBy(() -> vendorAuthService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should throw 403 FORBIDDEN when vendor account is deactivated")
    void shouldThrowWhenAccountDeactivated() {
        vendorUser.setActive(false);
        when(vendorUserRepository.findByEmail("admin@super.com")).thenReturn(Optional.of(vendorUser));
        when(passwordEncoder.matches("rawSecret", "hashedSecret")).thenReturn(true);

        VendorLoginRequest request = new VendorLoginRequest("admin@super.com", "rawSecret");

        assertThatThrownBy(() -> vendorAuthService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vendor account is deactivated");
    }

    @Test
    @DisplayName("Should return vendor profile with masked tax number and IBAN")
    void shouldGetVendorProfile() {
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));

        VendorProfileResponse profile = vendorAuthService.getVendorProfile(vendorUserId);

        assertThat(profile).isNotNull();
        assertThat(profile.companyName()).isEqualTo("Super Supplier");
        assertThat(profile.maskedTaxNumber()).isNotBlank();
        assertThat(profile.maskedIban()).isNotBlank();
        assertThat(profile.userEmail()).isEqualTo("admin@super.com");
    }

    @Test
    @DisplayName("Should throw 404 NOT_FOUND when vendor user not found on getVendorProfile")
    void shouldThrowWhenVendorUserNotFoundOnProfile() {
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorAuthService.getVendorProfile(vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vendor user not found");
    }
}
