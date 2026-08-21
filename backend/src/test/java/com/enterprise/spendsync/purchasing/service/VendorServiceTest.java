package com.enterprise.spendsync.purchasing.service;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorStatus;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import com.enterprise.spendsync.purchasing.internal.dto.CreateVendorRequest;
import com.enterprise.spendsync.purchasing.internal.dto.UpdateVendorStatusRequest;
import com.enterprise.spendsync.purchasing.internal.dto.VendorResponse;
import com.enterprise.spendsync.purchasing.internal.repository.VendorRepository;
import com.enterprise.spendsync.purchasing.internal.service.VendorServiceImpl;
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
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendorService Unit & Mock Tests (Vendor Master & Lifecycle)")
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private VendorServiceImpl vendorService;

    private UUID tenantId;
    private Tenant tenant;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        vendor = new Vendor(
                tenant,
                "Acme Global Tech",
                "1234567890",
                "Besiktas",
                VendorCategory.IT_HARDWARE,
                VendorTier.TIER_1_STRATEGIC,
                true,
                "orders@acmeglobal.com",
                "+90 212 555 0100",
                "Buyukdere Cad. No: 12",
                "Istanbul",
                "TR",
                PaymentTerms.NET_30,
                "Garanti BBVA",
                "TR330006200000001234567890"
        );
        vendor.setId(UUID.randomUUID());
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create new Vendor successfully when tax number is unique")
    void shouldCreateVendorSuccessfully() {
        CreateVendorRequest request = new CreateVendorRequest(
                "Acme Global Tech",
                "1234567890",
                "Besiktas",
                VendorCategory.IT_HARDWARE,
                VendorTier.TIER_1_STRATEGIC,
                true,
                "orders@acmeglobal.com",
                "+90 212 555 0100",
                "Buyukdere Cad. No: 12",
                "Istanbul",
                "TR",
                PaymentTerms.NET_30,
                "Garanti BBVA",
                "TR330006200000001234567890"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(vendorRepository.existsByTaxNumberAndTenantId("1234567890", tenantId)).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(i -> {
            Vendor v = i.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        VendorResponse response = vendorService.createVendor(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Acme Global Tech");
        assertThat(response.taxNumber()).isEqualTo("1234567890");
        assertThat(response.tier()).isEqualTo(VendorTier.TIER_1_STRATEGIC);
        assertThat(response.status()).isEqualTo(VendorStatus.ACTIVE);

        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    @DisplayName("Should reject vendor creation if tax number already exists in tenant (409 Conflict)")
    void shouldRejectDuplicateVendorTaxNumber() {
        CreateVendorRequest request = new CreateVendorRequest(
                "Duplicate Vendor",
                "1234567890",
                null, null, null, null,
                "dup@vendor.com",
                null, null, null, null,
                PaymentTerms.NET_30,
                null, null
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(vendorRepository.existsByTaxNumberAndTenantId("1234567890", tenantId)).thenReturn(true);

        assertThatThrownBy(() -> vendorService.createVendor(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(sex.getErrorCode()).isEqualTo("DUPLICATE_VENDOR_TAX_NUMBER");
                });
    }

    @Test
    @DisplayName("Should retrieve vendor by ID or throw 404 NOT_FOUND")
    void shouldGetVendorById() {
        when(vendorRepository.findByIdAndTenantId(vendor.getId(), tenantId)).thenReturn(Optional.of(vendor));

        VendorResponse response = vendorService.getVendorById(vendor.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(vendor.getId());
        assertThat(response.name()).isEqualTo("Acme Global Tech");
    }

    @Test
    @DisplayName("Should filter vendors by status (ACTIVE, ON_HOLD, BLOCKED)")
    void shouldFilterVendorsByStatus() {
        when(vendorRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, VendorStatus.BLOCKED))
                .thenReturn(List.of(vendor));

        List<VendorResponse> blockedVendors = vendorService.getAllVendors(VendorStatus.BLOCKED, null, null);

        assertThat(blockedVendors).hasSize(1);
        verify(vendorRepository).findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, VendorStatus.BLOCKED);
    }

    @Test
    @DisplayName("Should update vendor status to BLOCKED or ON_HOLD")
    void shouldUpdateVendorStatus() {
        when(vendorRepository.findByIdAndTenantId(vendor.getId(), tenantId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(i -> i.getArgument(0));

        UpdateVendorStatusRequest request = new UpdateVendorStatusRequest(VendorStatus.BLOCKED);
        VendorResponse updated = vendorService.updateVendorStatus(vendor.getId(), request);

        assertThat(updated.status()).isEqualTo(VendorStatus.BLOCKED);
        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.BLOCKED);
    }
}
