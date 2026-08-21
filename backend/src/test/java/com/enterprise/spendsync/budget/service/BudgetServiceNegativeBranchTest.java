package com.enterprise.spendsync.budget.service;

import com.enterprise.spendsync.budget.internal.dto.AdjustBudgetRequest;
import com.enterprise.spendsync.budget.internal.dto.BudgetTransferRequest;
import com.enterprise.spendsync.budget.internal.dto.UpdateBudgetStatusRequest;
import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceNegativeBranchTest {

    @Mock
    private BudgetPoolRepository budgetPoolRepository;

    @Mock
    private BudgetTransactionRepository budgetTransactionRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private LegalEntityRepository legalEntityRepository;

    @Mock
    private CostCenterRepository costCenterRepository;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;
    private BudgetPool activePool;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant("Test Corp", "test-corp");
        legalEntity = new LegalEntity(tenant, "Legal Entity", "LE-01", "1234567890", "TRY", "Istanbul", "TR");
        costCenter = new CostCenter(tenant, legalEntity, "CC-01", "Finance");

        activePool = new BudgetPool(
                tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO,
                new BigDecimal("50000.00"), "TRY"
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should throw BUDGET_POOL_NOT_FOUND when updating non-existent pool")
    void shouldThrowWhenPoolNotFound() {
        UUID randomId = UUID.randomUUID();
        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(randomId), eq(tenantId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.updateBudgetStatus(randomId, new UpdateBudgetStatusRequest(BudgetStatus.FROZEN)))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Budget Pool not found");
    }

    @Test
    @DisplayName("Should throw INVALID_BUDGET_STATE when trying to reopen a CLOSED budget pool")
    void shouldThrowWhenReopeningClosedPool() {
        UUID poolId = UUID.randomUUID();
        activePool.setStatus(BudgetStatus.CLOSED);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(poolId), eq(tenantId)))
                .thenReturn(Optional.of(activePool));

        assertThatThrownBy(() -> budgetService.updateBudgetStatus(poolId, new UpdateBudgetStatusRequest(BudgetStatus.ACTIVE)))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("A CLOSED budget pool cannot be reopened");
    }

    @Test
    @DisplayName("Should throw BUDGET_CLOSED when adjusting a CLOSED budget pool")
    void shouldThrowWhenAdjustingClosedPool() {
        UUID poolId = UUID.randomUUID();
        activePool.setStatus(BudgetStatus.CLOSED);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(poolId), eq(tenantId)))
                .thenReturn(Optional.of(activePool));

        assertThatThrownBy(() -> budgetService.adjustBudget(poolId, new AdjustBudgetRequest(new BigDecimal("60000.00"), "Increase")))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Cannot adjust a CLOSED budget pool");
    }

    @Test
    @DisplayName("Should throw ADJUSTMENT_BELOW_COMMITTED_FUNDS when lowering allocation below reserved+spent")
    void shouldThrowWhenAdjustingBelowCommittedFunds() {
        UUID poolId = UUID.randomUUID();
        activePool.setReservedAmount(new BigDecimal("20000.00"));
        activePool.setSpentAmount(new BigDecimal("15000.00")); // Total committed = 35,000

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(poolId), eq(tenantId)))
                .thenReturn(Optional.of(activePool));

        assertThatThrownBy(() -> budgetService.adjustBudget(poolId, new AdjustBudgetRequest(new BigDecimal("30000.00"), "Reduce")))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("cannot be lower than already committed funds");
    }

    @Test
    @DisplayName("Should throw BUDGET_NOT_ACTIVE when reserving against a DRAFT budget pool")
    void shouldThrowWhenReservingDraftBudget() {
        UUID poolId = UUID.randomUUID();
        activePool.setStatus(BudgetStatus.DRAFT);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(poolId), eq(tenantId)))
                .thenReturn(Optional.of(activePool));

        assertThatThrownBy(() -> budgetService.reserveBudget(poolId, new BigDecimal("1000.00"), UUID.randomUUID(), "PR", "Note"))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("DRAFT state and not yet active");
    }

    @Test
    @DisplayName("Should throw BUDGET_FROZEN when reserving against a FROZEN budget pool")
    void shouldThrowWhenReservingFrozenBudget() {
        UUID poolId = UUID.randomUUID();
        activePool.setStatus(BudgetStatus.FROZEN);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(poolId), eq(tenantId)))
                .thenReturn(Optional.of(activePool));

        assertThatThrownBy(() -> budgetService.reserveBudget(poolId, new BigDecimal("1000.00"), UUID.randomUUID(), "PR", "Note"))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("FROZEN under spending freeze measures");
    }

    @Test
    @DisplayName("Should throw BUDGET_CLOSED when reserving against a CLOSED budget pool")
    void shouldThrowWhenReservingClosedBudget() {
        UUID poolId = UUID.randomUUID();
        activePool.setStatus(BudgetStatus.CLOSED);

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(poolId), eq(tenantId)))
                .thenReturn(Optional.of(activePool));

        assertThatThrownBy(() -> budgetService.reserveBudget(poolId, new BigDecimal("1000.00"), UUID.randomUUID(), "PR", "Note"))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Budget pool is CLOSED");
    }

    @Test
    @DisplayName("Should throw HARD_STOP when reservation exceeds available budget in HARD_STOP mode")
    void shouldThrowWhenHardStopExceeded() {
        UUID poolId = UUID.randomUUID();
        // available = allocatedAmount - reservedAmount - spentAmount = 1000 - 0 - 0 = 1000
        // requesting 1500 > 1000 triggers HARD_STOP enforcement
        activePool.setAllocatedAmount(new BigDecimal("1000.00"));

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(poolId), eq(tenantId)))
                .thenReturn(Optional.of(activePool));

        assertThatThrownBy(() -> budgetService.reserveBudget(poolId, new BigDecimal("1500.00"), UUID.randomUUID(), "PR", "Note"))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Hard stop enforced");
    }

    @Test
    @DisplayName("Should throw SAME_SOURCE_TARGET_POOL when transferring to same budget pool")
    void shouldThrowWhenTransferringToSamePool() {
        UUID poolId = UUID.randomUUID();
        BudgetTransferRequest request = new BudgetTransferRequest(poolId, poolId, new BigDecimal("500.00"), "Internal");

        assertThatThrownBy(() -> budgetService.transferBudget(request))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Source and target budget pool cannot be the same");
    }

    @Test
    @DisplayName("Should throw CURRENCY_MISMATCH when transferring between pools of different currencies")
    void shouldThrowWhenTransferringDifferentCurrencies() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        activePool.setId(sourceId);

        BudgetPool usdPool = new BudgetPool(
                tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO,
                new BigDecimal("50000.00"), "USD"
        );
        usdPool.setId(targetId);

        UUID firstId = sourceId.compareTo(targetId) < 0 ? sourceId : targetId;
        UUID secondId = sourceId.compareTo(targetId) < 0 ? targetId : sourceId;

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(firstId), eq(tenantId)))
                .thenReturn(Optional.of(firstId.equals(sourceId) ? activePool : usdPool));
        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(secondId), eq(tenantId)))
                .thenReturn(Optional.of(secondId.equals(sourceId) ? activePool : usdPool));

        BudgetTransferRequest request = new BudgetTransferRequest(sourceId, targetId, new BigDecimal("500.00"), "Cross currency");

        assertThatThrownBy(() -> budgetService.transferBudget(request))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Currency mismatch between source");
    }

    @Test
    @DisplayName("Should throw INSUFFICIENT_TRANSFER_BUDGET when source pool has insufficient funds")
    void shouldThrowWhenInsufficientFundsForTransfer() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        activePool.setId(sourceId);
        activePool.setAllocatedAmount(new BigDecimal("100.00")); // Available = 100.00

        BudgetPool targetPool = new BudgetPool(
                tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO,
                new BigDecimal("50000.00"), "TRY"
        );
        targetPool.setId(targetId);

        UUID firstId = sourceId.compareTo(targetId) < 0 ? sourceId : targetId;
        UUID secondId = sourceId.compareTo(targetId) < 0 ? targetId : sourceId;

        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(firstId), eq(tenantId)))
                .thenReturn(Optional.of(firstId.equals(sourceId) ? activePool : targetPool));
        when(budgetPoolRepository.findByIdAndTenantIdWithLock(eq(secondId), eq(tenantId)))
                .thenReturn(Optional.of(secondId.equals(sourceId) ? activePool : targetPool));

        BudgetTransferRequest request = new BudgetTransferRequest(sourceId, targetId, new BigDecimal("500.00"), "Transfer");

        assertThatThrownBy(() -> budgetService.transferBudget(request))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Insufficient available budget in source pool for transfer");
    }
}
