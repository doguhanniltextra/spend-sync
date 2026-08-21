package com.enterprise.spendsync.requisition.service;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.CostCenterRepository;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.requisition.internal.domain.ApprovalAuthorityLimit;
import com.enterprise.spendsync.requisition.internal.dto.ApprovalLimitResponse;
import com.enterprise.spendsync.requisition.internal.dto.SetApprovalLimitRequest;
import com.enterprise.spendsync.requisition.internal.repository.ApprovalAuthorityLimitRepository;
import com.enterprise.spendsync.requisition.internal.service.ApprovalLimitServiceImpl;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalLimitService Unit & Mock Tests (DoA Matrix & Effective Limits)")
class ApprovalLimitServiceTest {

    @Mock
    private ApprovalAuthorityLimitRepository limitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LegalEntityRepository legalEntityRepository;

    @Mock
    private CostCenterRepository costCenterRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private ApprovalLimitServiceImpl approvalLimitService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;
    private User approver;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-FIN", "Finance Department");
        costCenter.setId(UUID.randomUUID());

        approver = new User("approver@spendsync.com", "pass", "Jane", "Doe", null, "TR");
        approver.setId(UUID.randomUUID());
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create a new cost-center specific approval authority limit")
    void shouldSetCostCenterApprovalLimitSuccessfully() {
        SetApprovalLimitRequest request = new SetApprovalLimitRequest(
                approver.getId(),
                legalEntity.getId(),
                costCenter.getId(),
                1,
                BigDecimal.ZERO,
                new BigDecimal("50000.00"),
                "TRY"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(approver.getId(), tenantId)).thenReturn(Optional.of(approver));
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.findByIdAndTenantId(costCenter.getId(), tenantId)).thenReturn(Optional.of(costCenter));
        when(limitRepository.findByUserIdAndLegalEntityIdAndCostCenterIdAndTenantId(
                approver.getId(), legalEntity.getId(), costCenter.getId(), tenantId
        )).thenReturn(Optional.empty());

        when(limitRepository.save(any(ApprovalAuthorityLimit.class))).thenAnswer(i -> {
            ApprovalAuthorityLimit l = i.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        ApprovalLimitResponse response = approvalLimitService.setApprovalLimit(request);

        assertThat(response).isNotNull();
        assertThat(response.approvalLevel()).isEqualTo(1);
        assertThat(response.maxAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(response.costCenterId()).isEqualTo(costCenter.getId());
        assertThat(response.isUnlimited()).isFalse();

        verify(limitRepository).save(any(ApprovalAuthorityLimit.class));
    }

    @Test
    @DisplayName("Should create an unlimited entity-wide limit (e.g. CFO authority)")
    void shouldSetUnlimitedEntityWideApprovalLimit() {
        SetApprovalLimitRequest request = new SetApprovalLimitRequest(
                approver.getId(),
                legalEntity.getId(),
                null, // Entity wide
                4,
                BigDecimal.ZERO,
                null, // null = unlimited
                "TRY"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(approver.getId(), tenantId)).thenReturn(Optional.of(approver));
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(limitRepository.findByUserIdAndLegalEntityIdAndCostCenterIsNullAndTenantId(
                approver.getId(), legalEntity.getId(), tenantId
        )).thenReturn(Optional.empty());

        when(limitRepository.save(any(ApprovalAuthorityLimit.class))).thenAnswer(i -> {
            ApprovalAuthorityLimit l = i.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        ApprovalLimitResponse response = approvalLimitService.setApprovalLimit(request);

        assertThat(response).isNotNull();
        assertThat(response.approvalLevel()).isEqualTo(4);
        assertThat(response.maxAmount()).isNull();
        assertThat(response.isUnlimited()).isTrue();
        assertThat(response.costCenterId()).isNull();
    }

    @Test
    @DisplayName("Should reject limit creation when cost center does not belong to specified legal entity")
    void shouldRejectCostCenterLegalEntityMismatch() {
        LegalEntity otherEntity = new LegalEntity(tenant, "Other Corp", "OC01", "9876543210", "TRY", "Ankara", "TR");
        otherEntity.setId(UUID.randomUUID());

        CostCenter mismatchedCC = new CostCenter(tenant, otherEntity, "CC-OTHER", "Other CC");
        mismatchedCC.setId(UUID.randomUUID());

        SetApprovalLimitRequest request = new SetApprovalLimitRequest(
                approver.getId(),
                legalEntity.getId(),
                mismatchedCC.getId(),
                1,
                BigDecimal.ZERO,
                new BigDecimal("25000.00"),
                "TRY"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(approver.getId(), tenantId)).thenReturn(Optional.of(approver));
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.findByIdAndTenantId(mismatchedCC.getId(), tenantId)).thenReturn(Optional.of(mismatchedCC));

        assertThatThrownBy(() -> approvalLimitService.setApprovalLimit(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("COST_CENTER_LEGAL_ENTITY_MISMATCH");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("Should resolve effective limit by prioritizing CostCenter limit over LegalEntity fallback")
    void shouldResolveEffectiveLimitPrioritizingCostCenter() {
        ApprovalAuthorityLimit ccLimit = new ApprovalAuthorityLimit(
                tenant, approver, legalEntity, costCenter, 1, BigDecimal.ZERO, new BigDecimal("75000.00"), "TRY", true
        );
        ccLimit.setId(UUID.randomUUID());

        when(limitRepository.findEffectiveLimits(
                approver.getId(), legalEntity.getId(), costCenter.getId(), tenantId
        )).thenReturn(List.of(ccLimit));

        Optional<ApprovalAuthorityLimit> result = approvalLimitService.getEffectiveLimitDetails(
                approver.getId(), legalEntity.getId(), costCenter.getId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getMaxAmount()).isEqualByComparingTo(new BigDecimal("75000.00"));
        assertThat(result.get().getCostCenter()).isNotNull();
    }

    @Test
    @DisplayName("Should resolve effective limit using LegalEntity fallback when CostCenter limit not configured")
    void shouldResolveEffectiveLimitUsingLegalEntityFallback() {
        ApprovalAuthorityLimit entityLimit = new ApprovalAuthorityLimit(
                tenant, approver, legalEntity, null, 2, BigDecimal.ZERO, new BigDecimal("250000.00"), "TRY", true
        );
        entityLimit.setId(UUID.randomUUID());

        when(limitRepository.findEffectiveLimits(
                approver.getId(), legalEntity.getId(), costCenter.getId(), tenantId
        )).thenReturn(List.of(entityLimit));

        Optional<ApprovalAuthorityLimit> result = approvalLimitService.getEffectiveLimitDetails(
                approver.getId(), legalEntity.getId(), costCenter.getId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getMaxAmount()).isEqualByComparingTo(new BigDecimal("250000.00"));
        assertThat(result.get().getCostCenter()).isNull();
    }

    @Test
    @DisplayName("Should toggle limit active status")
    void shouldToggleLimitStatus() {
        UUID limitId = UUID.randomUUID();
        ApprovalAuthorityLimit limit = new ApprovalAuthorityLimit(
                tenant, approver, legalEntity, costCenter, 1, BigDecimal.ZERO, new BigDecimal("50000.00"), "TRY", true
        );
        limit.setId(limitId);

        when(limitRepository.findByIdAndTenantId(limitId, tenantId)).thenReturn(Optional.of(limit));
        when(limitRepository.save(any(ApprovalAuthorityLimit.class))).thenAnswer(i -> i.getArgument(0));

        ApprovalLimitResponse disabled = approvalLimitService.toggleLimitStatus(limitId, false);
        assertThat(disabled.isActive()).isFalse();

        ApprovalLimitResponse enabled = approvalLimitService.toggleLimitStatus(limitId, true);
        assertThat(enabled.isActive()).isTrue();
    }
}
