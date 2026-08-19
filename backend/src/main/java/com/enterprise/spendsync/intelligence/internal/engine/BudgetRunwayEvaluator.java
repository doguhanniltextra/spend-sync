package com.enterprise.spendsync.intelligence.internal.engine;

import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.intelligence.dto.BudgetRunwayAnalysisDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class BudgetRunwayEvaluator {

    private final BudgetPoolRepository budgetPoolRepository;

    public BudgetRunwayEvaluator(BudgetPoolRepository budgetPoolRepository) {
        this.budgetPoolRepository = budgetPoolRepository;
    }

    public List<BudgetRunwayAnalysisDto> evaluateRunwayForTenant(UUID tenantId) {
        LocalDate today = LocalDate.now();
        int elapsedDaysInYear = Math.max(today.getDayOfYear(), 1);
        int daysLeftInFiscalYear = Math.max(today.lengthOfYear() - elapsedDaysInYear, 1);

        List<BudgetPool> pools = budgetPoolRepository.findAllByTenantId(tenantId)
                .stream()
                .filter(p -> p.getStatus() == BudgetStatus.ACTIVE)
                .toList();

        List<BudgetRunwayAnalysisDto> results = new ArrayList<>();

        for (BudgetPool pool : pools) {
            BigDecimal allocated = pool.getAllocatedAmount() != null ? pool.getAllocatedAmount() : BigDecimal.ZERO;
            BigDecimal spent = pool.getSpentAmount() != null ? pool.getSpentAmount() : BigDecimal.ZERO;
            BigDecimal reserved = pool.getReservedAmount() != null ? pool.getReservedAmount() : BigDecimal.ZERO;
            BigDecimal available = pool.getAvailableAmount() != null ? pool.getAvailableAmount() : BigDecimal.ZERO;

            BigDecimal dailyBurnRate = SpendIntelligenceCalculator.calculateDailyBurnRate(spent, elapsedDaysInYear);
            int runwayDays = SpendIntelligenceCalculator.calculateRunwayDays(available, dailyBurnRate);
            LocalDate estExhaustion = SpendIntelligenceCalculator.calculateEstimatedExhaustionDate(today, runwayDays);

            // Risk condition: Runway is less than days remaining in the year and budget is over 50% consumed
            boolean isExhaustionRisk = (runwayDays > 0 && runwayDays < daysLeftInFiscalYear) ||
                    (allocated.compareTo(BigDecimal.ZERO) > 0 &&
                     spent.add(reserved).divide(allocated, 2, RoundingMode.HALF_UP).compareTo(new BigDecimal("0.75")) >= 0);

            results.add(new BudgetRunwayAnalysisDto(
                    pool.getCostCenter().getId(),
                    pool.getCostCenter().getCode(),
                    pool.getCostCenter().getName(),
                    allocated.setScale(2, RoundingMode.HALF_UP),
                    spent.setScale(2, RoundingMode.HALF_UP),
                    reserved.setScale(2, RoundingMode.HALF_UP),
                    available.setScale(2, RoundingMode.HALF_UP),
                    dailyBurnRate.setScale(2, RoundingMode.HALF_UP),
                    runwayDays,
                    estExhaustion,
                    isExhaustionRisk
            ));
        }

        // Sort with high exhaustion risk first, then by runway ascending
        return results.stream()
                .sorted(Comparator.comparing(BudgetRunwayAnalysisDto::isExhaustionRisk).reversed()
                        .thenComparing(BudgetRunwayAnalysisDto::remainingRunwayDays))
                .toList();
    }
}
