package com.enterprise.spendsync.core.service;

import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.dto.CreateLegalEntityRequest;
import com.enterprise.spendsync.core.internal.dto.LegalEntityResponse;
import com.enterprise.spendsync.core.internal.dto.UpdateLegalEntityRequest;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.service.LegalEntityServiceImpl;
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
@DisplayName("LegalEntityService Unit & Mock Tests (Multi-Tenant Isolation & Entity Lifecycle)")
class LegalEntityServiceTest {

    @Mock
    private LegalEntityRepository legalEntityRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private LegalEntityServiceImpl legalEntityService;

    private UUID tenantId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Global Enterprise Holding");
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should fetch all legal entities for the active tenant")
    void shouldGetAllLegalEntitiesForActiveTenant() {
        LegalEntity e1 = new LegalEntity(tenant, "Entity TR", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        e1.setId(UUID.randomUUID());

        LegalEntity e2 = new LegalEntity(tenant, "Entity DE", "DE01", "9876543210", "EUR", "Berlin", "DE");
        e2.setId(UUID.randomUUID());

        when(legalEntityRepository.findAllByTenantId(tenantId)).thenReturn(List.of(e1, e2));

        List<LegalEntityResponse> result = legalEntityService.getAllLegalEntities();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).companyCode()).isEqualTo("TR01");
        assertThat(result.get(1).companyCode()).isEqualTo("DE01");
    }

    @Test
    @DisplayName("Should get legal entity by ID when belonging to active tenant")
    void shouldGetLegalEntityByIdSuccessfully() {
        UUID entityId = UUID.randomUUID();
        LegalEntity entity = new LegalEntity(tenant, "Entity TR", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        entity.setId(entityId);

        when(legalEntityRepository.findById(entityId)).thenReturn(Optional.of(entity));

        LegalEntityResponse response = legalEntityService.getLegalEntityById(entityId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(entityId);
        assertThat(response.name()).isEqualTo("Entity TR");
    }

    @Test
    @DisplayName("Should reject retrieval when legal entity belongs to a different tenant (Cross-Tenant Guard)")
    void shouldRejectCrossTenantGetById() {
        UUID entityId = UUID.randomUUID();
        Tenant otherTenant = new Tenant();
        otherTenant.setId(UUID.randomUUID());

        LegalEntity foreignEntity = new LegalEntity(otherTenant, "Foreign Corp", "FC01", "9999999999", "USD", "NY", "US");
        foreignEntity.setId(entityId);

        when(legalEntityRepository.findById(entityId)).thenReturn(Optional.of(foreignEntity));

        assertThatThrownBy(() -> legalEntityService.getLegalEntityById(entityId))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("LEGAL_ENTITY_NOT_FOUND");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("Should create legal entity with sanitized uppercase code and currency")
    void shouldCreateLegalEntitySuccessfully() {
        CreateLegalEntityRequest request = new CreateLegalEntityRequest(
                "SpendSync UK Ltd",
                "uk01",
                "GB123456789",
                "London Central",
                "gbp",
                "100 London Wall, London",
                "gb"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.existsByTenantIdAndTaxNumber(tenantId, "GB123456789")).thenReturn(false);
        when(legalEntityRepository.save(any(LegalEntity.class))).thenAnswer(i -> {
            LegalEntity le = i.getArgument(0);
            le.setId(UUID.randomUUID());
            return le;
        });

        LegalEntityResponse response = legalEntityService.createLegalEntity(request);

        assertThat(response).isNotNull();
        assertThat(response.companyCode()).isEqualTo("UK01");
        assertThat(response.baseCurrency()).isEqualTo("GBP");
        assertThat(response.country()).isEqualTo("GB");
        assertThat(response.taxOffice()).isEqualTo("London Central");
        verify(legalEntityRepository).save(any(LegalEntity.class));
    }

    @Test
    @DisplayName("Should reject creation if tax number already exists in active tenant")
    void shouldRejectDuplicateTaxNumber() {
        CreateLegalEntityRequest request = new CreateLegalEntityRequest(
                "Duplicate Entity",
                "DUP01",
                "1234567890",
                "Mecidiyekoy",
                "TRY",
                "Istanbul",
                "TR"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.existsByTenantIdAndTaxNumber(tenantId, "1234567890")).thenReturn(true);

        assertThatThrownBy(() -> legalEntityService.createLegalEntity(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("TAX_NUMBER_ALREADY_EXISTS");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(legalEntityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update legal entity mutable properties")
    void shouldUpdateLegalEntitySuccessfully() {
        UUID entityId = UUID.randomUUID();
        LegalEntity entity = new LegalEntity(tenant, "Old Name", "TR01", "1234567890", "TRY", "Old Address", "TR");
        entity.setId(entityId);

        UpdateLegalEntityRequest updateReq = new UpdateLegalEntityRequest("New Legal Name", "Besiktas", "New Registered Address");

        when(legalEntityRepository.findById(entityId)).thenReturn(Optional.of(entity));
        when(legalEntityRepository.save(any(LegalEntity.class))).thenAnswer(i -> i.getArgument(0));

        LegalEntityResponse updated = legalEntityService.updateLegalEntity(entityId, updateReq);

        assertThat(updated.name()).isEqualTo("New Legal Name");
        assertThat(updated.registeredAddress()).isEqualTo("New Registered Address");
        assertThat(updated.taxOffice()).isEqualTo("Besiktas");
    }

    @Test
    @DisplayName("Should toggle active/inactive status")
    void shouldUpdateStatusSuccessfully() {
        UUID entityId = UUID.randomUUID();
        LegalEntity entity = new LegalEntity(tenant, "Entity TR", "TR01", "1234567890", "TRY", "Address", "TR");
        entity.setId(entityId);
        entity.setActive(true);

        when(legalEntityRepository.findById(entityId)).thenReturn(Optional.of(entity));
        when(legalEntityRepository.save(any(LegalEntity.class))).thenAnswer(i -> i.getArgument(0));

        LegalEntityResponse disabled = legalEntityService.updateStatus(entityId, false);
        assertThat(disabled.isActive()).isFalse();

        LegalEntityResponse enabled = legalEntityService.updateStatus(entityId, true);
        assertThat(enabled.isActive()).isTrue();
    }
}
