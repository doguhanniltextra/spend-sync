package com.enterprise.spendsync.core.service;

import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.FacilityType;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.dto.CreateFacilityRequest;
import com.enterprise.spendsync.core.internal.dto.FacilityResponse;
import com.enterprise.spendsync.core.internal.dto.UpdateFacilityRequest;
import com.enterprise.spendsync.core.internal.repository.FacilityRepository;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.service.FacilityServiceImpl;
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
@DisplayName("FacilityService Unit & Mock Tests (Physical Plant, Warehouse & Shipping Locations)")
class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private LegalEntityRepository legalEntityRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private FacilityServiceImpl facilityService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Group");

        legalEntity = new LegalEntity(tenant, "Entity TR", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should retrieve all facilities for active tenant")
    void shouldGetAllFacilitiesForTenant() {
        Facility f1 = new Facility(tenant, legalEntity, "Main Warehouse", "WH-01", FacilityType.WAREHOUSE, "Gebze OSB");
        f1.setId(UUID.randomUUID());
        Facility f2 = new Facility(tenant, legalEntity, "Headquarters", "HQ-01", FacilityType.OFFICE, "Levent, Istanbul");
        f2.setId(UUID.randomUUID());

        when(facilityRepository.findAllByTenantId(tenantId)).thenReturn(List.of(f1, f2));

        List<FacilityResponse> facilities = facilityService.getAllFacilities(null);

        assertThat(facilities).hasSize(2);
        assertThat(facilities.get(0).facilityCode()).isEqualTo("WH-01");
        assertThat(facilities.get(1).facilityCode()).isEqualTo("HQ-01");
    }

    @Test
    @DisplayName("Should retrieve facilities scoped to legal entity")
    void shouldGetFacilitiesByLegalEntity() {
        Facility f1 = new Facility(tenant, legalEntity, "Branch Office", "BO-01", FacilityType.OFFICE, "Ankara");
        f1.setId(UUID.randomUUID());

        when(facilityRepository.findAllByLegalEntityId(legalEntity.getId())).thenReturn(List.of(f1));

        List<FacilityResponse> facilities = facilityService.getAllFacilities(legalEntity.getId());

        assertThat(facilities).hasSize(1);
        assertThat(facilities.get(0).name()).isEqualTo("Branch Office");
    }

    @Test
    @DisplayName("Should get facility by ID")
    void shouldGetFacilityById() {
        UUID facilityId = UUID.randomUUID();
        Facility f1 = new Facility(tenant, legalEntity, "Main Warehouse", "PLANT-01", FacilityType.WAREHOUSE, "Bursa");
        f1.setId(facilityId);

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(f1));

        FacilityResponse response = facilityService.getFacilityById(facilityId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(facilityId);
        assertThat(response.facilityType()).isEqualTo(FacilityType.WAREHOUSE);
    }

    @Test
    @DisplayName("Should reject cross-tenant facility access")
    void shouldRejectCrossTenantFacilityAccess() {
        UUID facilityId = UUID.randomUUID();
        Tenant otherTenant = new Tenant();
        otherTenant.setId(UUID.randomUUID());

        Facility foreignFacility = new Facility(otherTenant, legalEntity, "Foreign Hub", "HUB-01", FacilityType.WAREHOUSE, "Izmir");
        foreignFacility.setId(facilityId);

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(foreignFacility));

        assertThatThrownBy(() -> facilityService.getFacilityById(facilityId))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("FACILITY_NOT_FOUND");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("Should create facility with uppercase code and contact details")
    void shouldCreateFacilitySuccessfully() {
        CreateFacilityRequest request = new CreateFacilityRequest(
                legalEntity.getId(),
                "Distribution Center",
                "dc-ist-01",
                FacilityType.WAREHOUSE,
                "Tuzla Lojistik Koyu",
                "Mehmet Demir",
                "+90 532 999 8877"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findById(legalEntity.getId())).thenReturn(Optional.of(legalEntity));
        when(facilityRepository.existsByTenantIdAndFacilityCode(tenantId, "dc-ist-01")).thenReturn(false);
        when(facilityRepository.save(any(Facility.class))).thenAnswer(i -> {
            Facility f = i.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        FacilityResponse response = facilityService.createFacility(request);

        assertThat(response).isNotNull();
        assertThat(response.facilityCode()).isEqualTo("DC-IST-01");
        assertThat(response.contactPerson()).isEqualTo("Mehmet Demir");
        assertThat(response.contactPhone()).isEqualTo("+90 532 999 8877");
        verify(facilityRepository).save(any(Facility.class));
    }

    @Test
    @DisplayName("Should reject facility creation when code already exists in tenant")
    void shouldRejectDuplicateFacilityCode() {
        CreateFacilityRequest request = new CreateFacilityRequest(
                legalEntity.getId(),
                "Warehouse 2",
                "WH-01",
                FacilityType.WAREHOUSE,
                "Address",
                null, null
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findById(legalEntity.getId())).thenReturn(Optional.of(legalEntity));
        when(facilityRepository.existsByTenantIdAndFacilityCode(tenantId, "WH-01")).thenReturn(true);

        assertThatThrownBy(() -> facilityService.createFacility(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("FACILITY_CODE_ALREADY_EXISTS");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("Should update facility details")
    void shouldUpdateFacilitySuccessfully() {
        UUID facilityId = UUID.randomUUID();
        Facility facility = new Facility(tenant, legalEntity, "Old Name", "WH-01", FacilityType.WAREHOUSE, "Old Address");
        facility.setId(facilityId);

        UpdateFacilityRequest updateReq = new UpdateFacilityRequest("Updated DC", "New Address", "New Contact", "+90 500 000 0000");

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(facilityRepository.save(any(Facility.class))).thenAnswer(i -> i.getArgument(0));

        FacilityResponse response = facilityService.updateFacility(facilityId, updateReq);

        assertThat(response.name()).isEqualTo("Updated DC");
        assertThat(response.shippingAddress()).isEqualTo("New Address");
        assertThat(response.contactPerson()).isEqualTo("New Contact");
    }

    @Test
    @DisplayName("Should toggle facility active status")
    void shouldUpdateStatusSuccessfully() {
        UUID facilityId = UUID.randomUUID();
        Facility facility = new Facility(tenant, legalEntity, "Main Warehouse", "WH-01", FacilityType.WAREHOUSE, "Address");
        facility.setId(facilityId);
        facility.setActive(true);

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(facilityRepository.save(any(Facility.class))).thenAnswer(i -> i.getArgument(0));

        FacilityResponse res = facilityService.updateStatus(facilityId, false);
        assertThat(res.isActive()).isFalse();
    }
}
