package com.enterprise.spendsync.core.service;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.dto.CostCenterResponse;
import com.enterprise.spendsync.core.internal.dto.CreateCostCenterRequest;
import com.enterprise.spendsync.core.internal.dto.UpdateCostCenterRequest;
import com.enterprise.spendsync.core.internal.repository.CostCenterRepository;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.core.internal.service.CostCenterServiceImpl;
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
@DisplayName("CostCenterService Unit & Mock Tests (Budget Allocation Center & Hierarchy)")
class CostCenterServiceTest {

    @Mock
    private CostCenterRepository costCenterRepository;

    @Mock
    private LegalEntityRepository legalEntityRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CostCenterServiceImpl costCenterService;

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
    @DisplayName("Should get all cost centers for tenant when legalEntityId is null")
    void shouldGetAllCostCentersForTenant() {
        CostCenter c1 = new CostCenter(tenant, legalEntity, "CC-IT-01", "IT Operations");
        c1.setId(UUID.randomUUID());
        CostCenter c2 = new CostCenter(tenant, legalEntity, "CC-FIN-01", "Finance");
        c2.setId(UUID.randomUUID());

        when(costCenterRepository.findAllByTenantId(tenantId)).thenReturn(List.of(c1, c2));

        List<CostCenterResponse> results = costCenterService.getAllCostCenters(null);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).code()).isEqualTo("CC-IT-01");
        assertThat(results.get(1).code()).isEqualTo("CC-FIN-01");
    }

    @Test
    @DisplayName("Should get cost centers scoped to specific legal entity")
    void shouldGetCostCentersByLegalEntity() {
        CostCenter c1 = new CostCenter(tenant, legalEntity, "CC-HR-01", "Human Resources");
        c1.setId(UUID.randomUUID());

        when(costCenterRepository.findAllByLegalEntityId(legalEntity.getId())).thenReturn(List.of(c1));

        List<CostCenterResponse> results = costCenterService.getAllCostCenters(legalEntity.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).code()).isEqualTo("CC-HR-01");
    }

    @Test
    @DisplayName("Should get cost center by ID")
    void shouldGetCostCenterById() {
        UUID ccId = UUID.randomUUID();
        CostCenter cc = new CostCenter(tenant, legalEntity, "CC-RND-01", "R&D Software");
        cc.setId(ccId);

        when(costCenterRepository.findById(ccId)).thenReturn(Optional.of(cc));

        CostCenterResponse response = costCenterService.getCostCenterById(ccId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(ccId);
        assertThat(response.name()).isEqualTo("R&D Software");
    }

    @Test
    @DisplayName("Should reject cost center access if belongs to different tenant")
    void shouldRejectCrossTenantCostCenterAccess() {
        UUID ccId = UUID.randomUUID();
        Tenant otherTenant = new Tenant();
        otherTenant.setId(UUID.randomUUID());

        CostCenter foreignCC = new CostCenter(otherTenant, legalEntity, "CC-FOREIGN", "Foreign CC");
        foreignCC.setId(ccId);

        when(costCenterRepository.findById(ccId)).thenReturn(Optional.of(foreignCC));

        assertThatThrownBy(() -> costCenterService.getCostCenterById(ccId))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("COST_CENTER_NOT_FOUND");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("Should create cost center successfully with manager user assignment")
    void shouldCreateCostCenterSuccessfully() {
        UUID managerId = UUID.randomUUID();
        User manager = new User("manager@spendsync.com", "pass", "Jane", "Doe", null, "TR");
        manager.setId(managerId);
        manager.setTenant(tenant);

        CreateCostCenterRequest request = new CreateCostCenterRequest(
                legalEntity.getId(),
                "cc-ops-01",
                "Operations & Logistics",
                managerId
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findById(legalEntity.getId())).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.existsByTenantIdAndCode(tenantId, "cc-ops-01")).thenReturn(false);
        when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(costCenterRepository.save(any(CostCenter.class))).thenAnswer(i -> {
            CostCenter cc = i.getArgument(0);
            cc.setId(UUID.randomUUID());
            return cc;
        });

        CostCenterResponse response = costCenterService.createCostCenter(request);

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("CC-OPS-01");
        assertThat(response.name()).isEqualTo("Operations & Logistics");
        assertThat(response.managerUserId()).isEqualTo(managerId);
        assertThat(response.managerFullName()).isEqualTo("Jane Doe");
    }

    @Test
    @DisplayName("Should reject cost center creation when code already exists in tenant")
    void shouldRejectDuplicateCostCenterCode() {
        CreateCostCenterRequest request = new CreateCostCenterRequest(
                legalEntity.getId(),
                "CC-DUP",
                "Duplicate CC",
                null
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findById(legalEntity.getId())).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.existsByTenantIdAndCode(tenantId, "CC-DUP")).thenReturn(true);

        assertThatThrownBy(() -> costCenterService.createCostCenter(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("COST_CENTER_CODE_ALREADY_EXISTS");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("Should reject cost center creation when manager belongs to different tenant")
    void shouldRejectCrossTenantManager() {
        UUID managerId = UUID.randomUUID();
        Tenant otherTenant = new Tenant();
        otherTenant.setId(UUID.randomUUID());

        User foreignManager = new User("other@spendsync.com", "pass", "Foreign", "User", null, "US");
        foreignManager.setId(managerId);
        foreignManager.setTenant(otherTenant);

        CreateCostCenterRequest request = new CreateCostCenterRequest(
                legalEntity.getId(),
                "CC-SEC",
                "Security CC",
                managerId
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findById(legalEntity.getId())).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.existsByTenantIdAndCode(tenantId, "CC-SEC")).thenReturn(false);
        when(userRepository.findById(managerId)).thenReturn(Optional.of(foreignManager));

        assertThatThrownBy(() -> costCenterService.createCostCenter(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("CROSS_TENANT_ACCESS_DENIED");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("Should update cost center details and manager")
    void shouldUpdateCostCenterSuccessfully() {
        UUID ccId = UUID.randomUUID();
        CostCenter cc = new CostCenter(tenant, legalEntity, "CC-01", "Old Name");
        cc.setId(ccId);

        UUID newManagerId = UUID.randomUUID();
        User newManager = new User("newmanager@spendsync.com", "pass", "Bob", "Smith", null, "TR");
        newManager.setId(newManagerId);
        newManager.setTenant(tenant);

        UpdateCostCenterRequest updateReq = new UpdateCostCenterRequest("Updated Name", newManagerId);

        when(costCenterRepository.findById(ccId)).thenReturn(Optional.of(cc));
        when(userRepository.findById(newManagerId)).thenReturn(Optional.of(newManager));
        when(costCenterRepository.save(any(CostCenter.class))).thenAnswer(i -> i.getArgument(0));

        CostCenterResponse response = costCenterService.updateCostCenter(ccId, updateReq);

        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.managerUserId()).isEqualTo(newManagerId);
    }

    @Test
    @DisplayName("Should toggle cost center active status")
    void shouldUpdateStatusSuccessfully() {
        UUID ccId = UUID.randomUUID();
        CostCenter cc = new CostCenter(tenant, legalEntity, "CC-01", "Facilities");
        cc.setId(ccId);
        cc.setActive(true);

        when(costCenterRepository.findById(ccId)).thenReturn(Optional.of(cc));
        when(costCenterRepository.save(any(CostCenter.class))).thenAnswer(i -> i.getArgument(0));

        CostCenterResponse res = costCenterService.updateStatus(ccId, false);
        assertThat(res.isActive()).isFalse();
    }
}
