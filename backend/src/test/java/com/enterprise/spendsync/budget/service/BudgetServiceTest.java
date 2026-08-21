package com.enterprise.spendsync.budget.service;

import com.enterprise.spendsync.budget.internal.domain.*;
import com.enterprise.spendsync.budget.internal.dto.*;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.budget.internal.repository.BudgetTransactionRepository;
import com.enterprise.spendsync.budget.internal.service.BudgetServiceImpl;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.repository.CostCenterRepository;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService Unit & Mock Tests (Allocation, Reservation, Overrun & Transfers)")
class BudgetServiceTest {

    @Mock
    private BudgetPoolRepository budgetPoolRepository;

    @Mock
    private BudgetTransactionRepository budgetTransactionRepository;

    @Mock
    private CostCenterRepository costCenterRepository;

    @Mock
    private LegalEntityRepository legalEntityRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "Legal Entity TR", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-ENG", "Engineering");
        costCenter.setId(UUID.randomUUID());
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create budget pool and record initial allocation ledger entry")
    void shouldCreateBudgetPoolSuccessfully() {
        CreateBudgetPoolRequest request = new CreateBudgetPoolRequest(
                costCenter.getId(),
                legalEntity.getId(),
                2026,
                BudgetPeriodType.ANNUAL,
                "ANNUAL",
                BudgetStatus.ACTIVE,
                BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO,
                new BigDecimal("1000000.00"),
                "TRY"
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.findByIdAndTenantId(costCenter.getId(), tenantId)).thenReturn(Optional.of(costCenter));
        when(budgetPoolRepository.findByCostCenterIdAndFiscalYearAndPeriodTypeAndPeriodValueAndTenantId(
                costCenter.getId(), 2026, BudgetPeriodType.ANNUAL, "ANNUAL", tenantId
        )).thenReturn(Optional.empty());

        when(budgetPoolRepository.save(any(BudgetPool.class))).thenAnswer(i -> {
            BudgetPool p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        BudgetPoolResponse response = budgetService.createBudgetPool(request);

        assertThat(response).isNotNull();
        assertThat(response.fiscalYear()).isEqualTo(2026);
        assertThat(response.allocatedAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(response.availableAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(response.currency()).isEqualTo("TRY");

        verify(budgetTransactionRepository).save(any(BudgetTransaction.class));
    }

    @Test
    @DisplayName("Should reject creation when budget pool already exists for same period")
    void shouldRejectDuplicateBudgetPool() {
        CreateBudgetPoolRequest request = new CreateBudgetPoolRequest(
                costCenter.getId(),
                legalEntity.getId(),
                2026,
                BudgetPeriodType.ANNUAL,
                "ANNUAL",
                BudgetStatus.ACTIVE,
                BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO,
                new BigDecimal("500000.00"),
                "TRY"
        );

        BudgetPool existingPool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("500000.00"), "TRY");

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.findByIdAndTenantId(costCenter.getId(), tenantId)).thenReturn(Optional.of(costCenter));
        when(budgetPoolRepository.findByCostCenterIdAndFiscalYearAndPeriodTypeAndPeriodValueAndTenantId(
                costCenter.getId(), 2026, BudgetPeriodType.ANNUAL, "ANNUAL", tenantId
        )).thenReturn(Optional.of(existingPool));

        assertThatThrownBy(() -> budgetService.createBudgetPool(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("DUPLICATE_BUDGET_POOL");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("Should transition status and prevent reopening CLOSED budget pool")
    void shouldManageBudgetStatusTransitions() {
        UUID poolId = UUID.randomUUID();
        BudgetPool pool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("100000.00"), "TRY");
        pool.setId(poolId);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(poolId, tenantId)).thenReturn(Optional.of(pool));
        when(budgetPoolRepository.save(any(BudgetPool.class))).thenAnswer(i -> i.getArgument(0));

        // ACTIVE -> FROZEN
        BudgetPoolResponse frozen = budgetService.updateBudgetStatus(poolId, new UpdateBudgetStatusRequest(BudgetStatus.FROZEN));
        assertThat(frozen.status()).isEqualTo(BudgetStatus.FROZEN);

        // FROZEN -> CLOSED
        BudgetPoolResponse closed = budgetService.updateBudgetStatus(poolId, new UpdateBudgetStatusRequest(BudgetStatus.CLOSED));
        assertThat(closed.status()).isEqualTo(BudgetStatus.CLOSED);

        // CLOSED -> ACTIVE (Must throw error)
        assertThatThrownBy(() -> budgetService.updateBudgetStatus(poolId, new UpdateBudgetStatusRequest(BudgetStatus.ACTIVE)))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("INVALID_BUDGET_STATE");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("Should adjust budget allocation and record transaction")
    void shouldAdjustBudgetSuccessfully() {
        UUID poolId = UUID.randomUUID();
        BudgetPool pool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("100000.00"), "TRY");
        pool.setId(poolId);
        pool.setReservedAmount(new BigDecimal("20000.00"));
        pool.setSpentAmount(new BigDecimal("10000.00"));

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(poolId, tenantId)).thenReturn(Optional.of(pool));
        when(budgetPoolRepository.save(any(BudgetPool.class))).thenAnswer(i -> i.getArgument(0));

        AdjustBudgetRequest request = new AdjustBudgetRequest(new BigDecimal("150000.00"), "Mid-year budget expansion");
        BudgetPoolResponse adjusted = budgetService.adjustBudget(poolId, request);

        assertThat(adjusted.allocatedAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(adjusted.availableAmount()).isEqualByComparingTo(new BigDecimal("120000.00")); // 150k - (20k + 10k)
        verify(budgetTransactionRepository).save(any(BudgetTransaction.class));
    }

    @Test
    @DisplayName("Should reject budget adjustment below already committed funds (Reserved + Spent)")
    void shouldRejectAdjustmentBelowCommittedFunds() {
        UUID poolId = UUID.randomUUID();
        BudgetPool pool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("100000.00"), "TRY");
        pool.setId(poolId);
        pool.setReservedAmount(new BigDecimal("40000.00"));
        pool.setSpentAmount(new BigDecimal("30000.00")); // Committed = 70,000

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(poolId, tenantId)).thenReturn(Optional.of(pool));

        AdjustBudgetRequest request = new AdjustBudgetRequest(new BigDecimal("50000.00"), "Reduction below committed");

        assertThatThrownBy(() -> budgetService.adjustBudget(poolId, request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("ADJUSTMENT_BELOW_COMMITTED_FUNDS");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("Should reserve funds successfully under HARD_STOP mode within limit")
    void shouldReserveFundsSuccessfully() {
        UUID poolId = UUID.randomUUID();
        BudgetPool pool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("100000.00"), "TRY");
        pool.setId(poolId);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(poolId, tenantId)).thenReturn(Optional.of(pool));
        when(budgetPoolRepository.save(any(BudgetPool.class))).thenAnswer(i -> i.getArgument(0));

        UUID prId = UUID.randomUUID();
        BudgetReservationResult result = budgetService.reserveBudget(poolId, new BigDecimal("25000.00"), prId, "REQUISITION", "PR #1001");

        assertThat(result.success()).isTrue();
        assertThat(result.isOverrun()).isFalse();
        assertThat(result.requestedAmount()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(result.reservedTotal()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(result.availableAfter()).isEqualByComparingTo(new BigDecimal("75000.00"));
        verify(budgetTransactionRepository).save(any(BudgetTransaction.class));
    }

    @Test
    @DisplayName("Should reject reservation when exceeding available budget under HARD_STOP mode")
    void shouldRejectWhenExceedingHardStopLimit() {
        UUID poolId = UUID.randomUUID();
        BudgetPool pool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("10000.00"), "TRY");
        pool.setId(poolId);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(poolId, tenantId)).thenReturn(Optional.of(pool));

        assertThatThrownBy(() -> budgetService.reserveBudget(poolId, new BigDecimal("15000.00"), UUID.randomUUID(), "REQUISITION", "Over budget PR"))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("INSUFFICIENT_BUDGET");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("Should allow reservation within TOLERANCE window and flag overrun")
    void shouldAllowReservationWithinTolerance() {
        UUID poolId = UUID.randomUUID();
        BudgetPool pool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.TOLERANCE, new BigDecimal("10.00"), new BigDecimal("100000.00"), "TRY");
        pool.setId(poolId);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(poolId, tenantId)).thenReturn(Optional.of(pool));
        when(budgetPoolRepository.save(any(BudgetPool.class))).thenAnswer(i -> i.getArgument(0));

        // Total available is 100k, but max allowed with 10% tolerance is 110k
        BudgetReservationResult result = budgetService.reserveBudget(poolId, new BigDecimal("105000.00"), UUID.randomUUID(), "REQUISITION", "PR with 5% overrun");

        assertThat(result.success()).isTrue();
        assertThat(result.isOverrun()).isTrue();
        assertThat(result.message()).contains("tolerance window");
    }

    @Test
    @DisplayName("Should reject reservation exceeding TOLERANCE window")
    void shouldRejectReservationExceedingTolerance() {
        UUID poolId = UUID.randomUUID();
        BudgetPool pool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.TOLERANCE, new BigDecimal("10.00"), new BigDecimal("100000.00"), "TRY");
        pool.setId(poolId);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(poolId, tenantId)).thenReturn(Optional.of(pool));

        // Max allowed is 110k, requesting 115k
        assertThatThrownBy(() -> budgetService.reserveBudget(poolId, new BigDecimal("115000.00"), UUID.randomUUID(), "REQUISITION", "PR exceeding tolerance"))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("BUDGET_TOLERANCE_EXCEEDED");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("Should release reserved budget back to available funds upon PR rejection/cancellation")
    void shouldReleaseBudgetSuccessfully() {
        UUID poolId = UUID.randomUUID();
        BudgetPool pool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("100000.00"), "TRY");
        pool.setId(poolId);
        pool.setReservedAmount(new BigDecimal("30000.00"));

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(poolId, tenantId)).thenReturn(Optional.of(pool));
        when(budgetPoolRepository.save(any(BudgetPool.class))).thenAnswer(i -> i.getArgument(0));

        BudgetPoolResponse response = budgetService.releaseBudget(poolId, new BigDecimal("30000.00"), UUID.randomUUID(), "REQUISITION", "PR Cancelled");

        assertThat(response.reservedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.availableAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
        verify(budgetTransactionRepository).save(any(BudgetTransaction.class));
    }

    @Test
    @DisplayName("Should commit funds from reserved to spent upon invoice approval")
    void shouldCommitBudgetSuccessfully() {
        UUID poolId = UUID.randomUUID();
        BudgetPool pool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("100000.00"), "TRY");
        pool.setId(poolId);
        pool.setReservedAmount(new BigDecimal("25000.00"));

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(poolId, tenantId)).thenReturn(Optional.of(pool));
        when(budgetPoolRepository.save(any(BudgetPool.class))).thenAnswer(i -> i.getArgument(0));

        BudgetPoolResponse response = budgetService.commitBudget(poolId, new BigDecimal("25000.00"), UUID.randomUUID(), "INVOICE", "Invoice Approved");

        assertThat(response.reservedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.spentAmount()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(response.availableAmount()).isEqualByComparingTo(new BigDecimal("75000.00"));
        verify(budgetTransactionRepository).save(any(BudgetTransaction.class));
    }

    @Test
    @DisplayName("Should transfer budget between two pools with matching currency")
    void shouldTransferBudgetSuccessfully() {
        UUID sourcePoolId = UUID.randomUUID();
        UUID targetPoolId = UUID.randomUUID();

        CostCenter ccSource = new CostCenter(tenant, legalEntity, "CC-SRC", "Source CC");
        ccSource.setId(UUID.randomUUID());
        CostCenter ccTarget = new CostCenter(tenant, legalEntity, "CC-TGT", "Target CC");
        ccTarget.setId(UUID.randomUUID());

        BudgetPool sourcePool = new BudgetPool(tenant, legalEntity, ccSource, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("200000.00"), "TRY");
        sourcePool.setId(sourcePoolId);

        BudgetPool targetPool = new BudgetPool(tenant, legalEntity, ccTarget, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("50000.00"), "TRY");
        targetPool.setId(targetPoolId);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(sourcePoolId, tenantId)).thenReturn(Optional.of(sourcePool));
        when(budgetPoolRepository.findByIdAndTenantIdWithLock(targetPoolId, tenantId)).thenReturn(Optional.of(targetPool));

        BudgetTransferRequest request = new BudgetTransferRequest(sourcePoolId, targetPoolId, new BigDecimal("30000.00"), "Inter-department rebalancing");
        budgetService.transferBudget(request);

        assertThat(sourcePool.getAllocatedAmount()).isEqualByComparingTo(new BigDecimal("170000.00"));
        assertThat(targetPool.getAllocatedAmount()).isEqualByComparingTo(new BigDecimal("80000.00"));
        verify(budgetTransactionRepository, times(2)).save(any(BudgetTransaction.class));
    }

    @Test
    @DisplayName("Should reject transfer when currencies do not match")
    void shouldRejectTransferWithCurrencyMismatch() {
        UUID sourcePoolId = UUID.randomUUID();
        UUID targetPoolId = UUID.randomUUID();

        BudgetPool sourcePool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("100000.00"), "USD");
        sourcePool.setId(sourcePoolId);

        BudgetPool targetPool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("50000.00"), "EUR");
        targetPool.setId(targetPoolId);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(sourcePoolId, tenantId)).thenReturn(Optional.of(sourcePool));
        when(budgetPoolRepository.findByIdAndTenantIdWithLock(targetPoolId, tenantId)).thenReturn(Optional.of(targetPool));

        BudgetTransferRequest request = new BudgetTransferRequest(sourcePoolId, targetPoolId, new BigDecimal("10000.00"), "FX Transfer Attempt");

        assertThatThrownBy(() -> budgetService.transferBudget(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("CURRENCY_MISMATCH");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("Should aggregate budget summary across all pools for a fiscal year")
    void shouldGetBudgetSummary() {
        BudgetPool p1 = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("100000.00"), "TRY");
        p1.setId(UUID.randomUUID());
        p1.setReservedAmount(new BigDecimal("20000.00"));
        p1.setSpentAmount(new BigDecimal("30000.00"));

        BudgetPool p2 = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("200000.00"), "TRY");
        p2.setId(UUID.randomUUID());
        p2.setReservedAmount(new BigDecimal("50000.00"));
        p2.setSpentAmount(new BigDecimal("50000.00"));

        when(budgetPoolRepository.findAllByTenantIdAndFiscalYear(tenantId, 2026)).thenReturn(List.of(p1, p2));

        BudgetSummaryResponse summary = budgetService.getBudgetSummary(2026);

        assertThat(summary).isNotNull();
        assertThat(summary.fiscalYear()).isEqualTo(2026);
        assertThat(summary.totalPools()).isEqualTo(2);
        assertThat(summary.totalAllocated()).isEqualByComparingTo(new BigDecimal("300000.00"));
        assertThat(summary.totalReserved()).isEqualByComparingTo(new BigDecimal("70000.00"));
        assertThat(summary.totalSpent()).isEqualByComparingTo(new BigDecimal("80000.00"));
        assertThat(summary.totalAvailable()).isEqualByComparingTo(new BigDecimal("150000.00"));
    }
}
