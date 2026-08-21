package com.enterprise.spendsync.testcontainers;

import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.budget.internal.service.BudgetService;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.repository.CostCenterRepository;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class BudgetConcurrencyContainerTest extends AbstractContainerIntegrationTest {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private BudgetPoolRepository budgetPoolRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private LegalEntityRepository legalEntityRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Test
    @DisplayName("TC-CT-04 to TC-CT-06: 10 parallel threads attempting 20k spend on 100k budget with real PostgreSQL Pessimistic Lock")
    void shouldPreventDoubleSpendingUnderHighConcurrency() throws InterruptedException {
        // 1. Seed Tenant, LegalEntity, CostCenter, and BudgetPool in PostgreSQL container
        Tenant tenant = tenantRepository.save(new Tenant("Concurrency Tenant", "concurrency-tenant-" + UUID.randomUUID()));
        LegalEntity legalEntity = legalEntityRepository.save(new LegalEntity(
                tenant, "Concurrency Corp", "CC-01", "1234567890", "TRY", "Istanbul", "TR"
        ));
        CostCenter costCenter = costCenterRepository.save(new CostCenter(
                tenant, legalEntity, "CC-ENG-01", "Engineering"
        ));

        // 100,000.00 TRY budget pool with HARD_STOP enforcement
        BudgetPool pool = budgetPoolRepository.save(new BudgetPool(
                tenant,
                legalEntity,
                costCenter,
                2026,
                BudgetPeriodType.ANNUAL,
                "ANNUAL",
                BudgetStatus.ACTIVE,
                BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO,
                new BigDecimal("100000.0000"),
                "TRY"
        ));

        UUID budgetPoolId = pool.getId();
        UUID tenantId = tenant.getId();

        // 2. Prepare 10 concurrent threads each requesting 20,000.00 TRY reservation (Total 200,000.00 TRY)
        int threadCount = 10;
        BigDecimal reservationAmount = new BigDecimal("20000.0000");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready to fire simultaneously
                    startLatch.await();
                    TenantContext.setTenantId(tenantId);

                    budgetService.reserveBudget(
                            budgetPoolId,
                            reservationAmount,
                            UUID.randomUUID(),
                            "PR",
                            "Parallel Concurrency Test"
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    TenantContext.clear();
                    finishLatch.countDown();
                }
            });
        }

        // Fire all 10 threads at once
        startLatch.countDown();

        boolean completedInTime = finishLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completedInTime).isTrue();

        // 3. Verify exactly 5 threads succeeded and exactly 5 threads failed (100k / 20k = 5)
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failureCount.get()).isEqualTo(5);

        // 4. Verify the database state in real PostgreSQL: remaining available balance MUST be exactly 0.0000
        BudgetPool updatedPool = budgetPoolRepository.findById(budgetPoolId).orElseThrow();
        assertThat(updatedPool.getReservedAmount()).isEqualByComparingTo(new BigDecimal("100000.0000"));
        assertThat(updatedPool.getAvailableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
