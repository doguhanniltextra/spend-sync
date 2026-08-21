package com.enterprise.spendsync.budget.domain;

import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BudgetPool Domain Entity Pure Unit Tests")
class BudgetPoolTest {

    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("SpendSync Group");

        legalEntity = new LegalEntity(tenant, "Legal Entity TR", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-IT", "IT Department");
        costCenter.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should initialize pool with zero spent/reserved amounts and available equal to allocated")
    void shouldInitializeWithCorrectBalances() {
        BigDecimal allocation = new BigDecimal("500000.00");
        BudgetPool pool = new BudgetPool(
                tenant,
                legalEntity,
                costCenter,
                2026,
                BudgetPeriodType.ANNUAL,
                "ANNUAL",
                BudgetStatus.ACTIVE,
                BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO,
                allocation,
                "TRY"
        );

        assertThat(pool.getAllocatedAmount()).isEqualByComparingTo(allocation);
        assertThat(pool.getReservedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pool.getSpentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pool.getAvailableAmount()).isEqualByComparingTo(allocation);
        assertThat(pool.getCurrency()).isEqualTo("TRY");
        assertThat(pool.getStatus()).isEqualTo(BudgetStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should correctly calculate available funds as allocated - (reserved + spent)")
    void shouldCalculateAvailableFundsAccurately() {
        BigDecimal allocation = new BigDecimal("100000.00");
        BudgetPool pool = new BudgetPool(
                tenant,
                legalEntity,
                costCenter,
                2026,
                BudgetPeriodType.Q1,
                "Q1",
                BudgetStatus.ACTIVE,
                BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO,
                allocation,
                "usd"
        );

        // Normalize currency to uppercase
        assertThat(pool.getCurrency()).isEqualTo("USD");

        pool.setReservedAmount(new BigDecimal("25000.00"));
        pool.setSpentAmount(new BigDecimal("40000.00"));

        // Available = 100,000 - (25,000 + 40,000) = 35,000
        assertThat(pool.getAvailableAmount()).isEqualByComparingTo(new BigDecimal("35000.00"));
    }

    @Test
    @DisplayName("Should compute max allowed allocation when TOLERANCE enforcement mode is enabled")
    void shouldComputeMaxAllowedAllocationWithTolerance() {
        BigDecimal allocation = new BigDecimal("100000.00");
        BigDecimal tolerance = new BigDecimal("15.00"); // 15% tolerance

        BudgetPool pool = new BudgetPool(
                tenant,
                legalEntity,
                costCenter,
                2026,
                BudgetPeriodType.ANNUAL,
                "ANNUAL",
                BudgetStatus.ACTIVE,
                BudgetEnforcementMode.TOLERANCE,
                tolerance,
                allocation,
                "EUR"
        );

        // Max allowed = 100,000 * (1 + 0.15) = 115,000.00
        assertThat(pool.getMaxAllowedAllocation()).isEqualByComparingTo(new BigDecimal("115000.00"));
    }

    @Test
    @DisplayName("Should return allocated amount as max allowed when mode is HARD_STOP regardless of tolerance")
    void shouldReturnAllocatedAmountForHardStop() {
        BigDecimal allocation = new BigDecimal("200000.00");
        BigDecimal tolerance = new BigDecimal("20.00");

        BudgetPool pool = new BudgetPool(
                tenant,
                legalEntity,
                costCenter,
                2026,
                BudgetPeriodType.ANNUAL,
                "ANNUAL",
                BudgetStatus.ACTIVE,
                BudgetEnforcementMode.HARD_STOP,
                tolerance,
                allocation,
                "TRY"
        );

        assertThat(pool.getMaxAllowedAllocation()).isEqualByComparingTo(allocation);
    }
}
