package com.enterprise.spendsync.budget.internal.service;

import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.domain.BudgetTransaction;
import com.enterprise.spendsync.budget.internal.domain.BudgetTransactionType;
import com.enterprise.spendsync.budget.internal.dto.AdjustBudgetRequest;
import com.enterprise.spendsync.budget.internal.dto.BudgetPoolResponse;
import com.enterprise.spendsync.budget.internal.dto.BudgetReservationResult;
import com.enterprise.spendsync.budget.internal.dto.BudgetSummaryResponse;
import com.enterprise.spendsync.budget.internal.dto.BudgetTransactionResponse;
import com.enterprise.spendsync.budget.internal.dto.BudgetTransferRequest;
import com.enterprise.spendsync.budget.internal.dto.CreateBudgetPoolRequest;
import com.enterprise.spendsync.budget.internal.dto.UpdateBudgetStatusRequest;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.budget.internal.repository.BudgetTransactionRepository;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.repository.CostCenterRepository;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BudgetServiceImpl implements BudgetService {

    private final BudgetPoolRepository budgetPoolRepository;
    private final BudgetTransactionRepository budgetTransactionRepository;
    private final CostCenterRepository costCenterRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final TenantRepository tenantRepository;

    public BudgetServiceImpl(BudgetPoolRepository budgetPoolRepository,
                             BudgetTransactionRepository budgetTransactionRepository,
                             CostCenterRepository costCenterRepository,
                             LegalEntityRepository legalEntityRepository,
                             TenantRepository tenantRepository) {
        this.budgetPoolRepository = budgetPoolRepository;
        this.budgetTransactionRepository = budgetTransactionRepository;
        this.costCenterRepository = costCenterRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public BudgetPoolResponse createBudgetPool(CreateBudgetPoolRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        LegalEntity legalEntity = legalEntityRepository.findByIdAndTenantId(request.legalEntityId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Legal Entity not found in active tenant", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {});

        CostCenter costCenter = costCenterRepository.findByIdAndTenantId(request.costCenterId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Cost Center not found in active tenant", HttpStatus.NOT_FOUND, "COST_CENTER_NOT_FOUND") {});

        BudgetPeriodType periodType = request.periodType() != null ? request.periodType() : BudgetPeriodType.ANNUAL;
        String periodValue = request.periodValue() != null && !request.periodValue().isBlank()
                ? request.periodValue().trim()
                : periodType.name();

        // Check duplicate budget pool for the same period
        budgetPoolRepository.findByCostCenterIdAndFiscalYearAndPeriodTypeAndPeriodValueAndTenantId(
                costCenter.getId(),
                request.fiscalYear(),
                periodType,
                periodValue,
                tenantId
        ).ifPresent(p -> {
            throw new SpendSyncException(
                    String.format("A budget pool already exists for Cost Center '%s' in fiscal year %d and period '%s'.",
                            costCenter.getName(), request.fiscalYear(), periodValue),
                    HttpStatus.CONFLICT,
                    "DUPLICATE_BUDGET_POOL"
            ) {};
        });

        BudgetPool pool = new BudgetPool(
                tenant,
                legalEntity,
                costCenter,
                request.fiscalYear(),
                periodType,
                periodValue,
                request.status() != null ? request.status() : BudgetStatus.ACTIVE,
                request.enforcementMode() != null ? request.enforcementMode() : BudgetEnforcementMode.HARD_STOP,
                request.tolerancePercentage() != null ? request.tolerancePercentage() : BigDecimal.ZERO,
                request.allocatedAmount(),
                request.currency()
        );

        BudgetPool savedPool = budgetPoolRepository.save(pool);

        // Initial allocation audit ledger entry
        BudgetTransaction initialTx = new BudgetTransaction(
                savedPool,
                tenant,
                BudgetTransactionType.INITIAL_ALLOCATION,
                savedPool.getAllocatedAmount(),
                BigDecimal.ZERO,
                savedPool.getAllocatedAmount(),
                savedPool.getId(),
                "INITIAL_SETUP",
                "Initial budget allocation upon pool creation."
        );
        budgetTransactionRepository.save(initialTx);

        return BudgetPoolResponse.from(savedPool);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetPoolResponse getBudgetPoolById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        BudgetPool pool = budgetPoolRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Budget Pool not found", HttpStatus.NOT_FOUND, "BUDGET_POOL_NOT_FOUND") {});
        return BudgetPoolResponse.from(pool);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetPoolResponse> getAllBudgetPools(Integer fiscalYear, BudgetStatus status) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<BudgetPool> pools;

        if (fiscalYear != null && status != null) {
            pools = budgetPoolRepository.findAllByTenantIdAndFiscalYear(tenantId, fiscalYear).stream()
                    .filter(p -> p.getStatus() == status)
                    .toList();
        } else if (fiscalYear != null) {
            pools = budgetPoolRepository.findAllByTenantIdAndFiscalYear(tenantId, fiscalYear);
        } else if (status != null) {
            pools = budgetPoolRepository.findAllByTenantIdAndStatus(tenantId, status);
        } else {
            pools = budgetPoolRepository.findAllByTenantId(tenantId);
        }

        return pools.stream().map(BudgetPoolResponse::from).toList();
    }

    @Override
    public BudgetPoolResponse updateBudgetStatus(UUID id, UpdateBudgetStatusRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        BudgetPool pool = budgetPoolRepository.findByIdAndTenantIdWithLock(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Budget Pool not found", HttpStatus.NOT_FOUND, "BUDGET_POOL_NOT_FOUND") {});

        if (pool.getStatus() == BudgetStatus.CLOSED && request.status() != BudgetStatus.CLOSED) {
            throw new SpendSyncException("A CLOSED budget pool cannot be reopened. Create a new fiscal period pool instead.",
                    HttpStatus.BAD_REQUEST, "INVALID_BUDGET_STATE") {};
        }

        pool.setStatus(request.status());
        BudgetPool updated = budgetPoolRepository.save(pool);
        return BudgetPoolResponse.from(updated);
    }

    @Override
    public BudgetPoolResponse adjustBudget(UUID id, AdjustBudgetRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        BudgetPool pool = budgetPoolRepository.findByIdAndTenantIdWithLock(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Budget Pool not found", HttpStatus.NOT_FOUND, "BUDGET_POOL_NOT_FOUND") {});

        if (pool.getStatus() == BudgetStatus.CLOSED) {
            throw new SpendSyncException("Cannot adjust a CLOSED budget pool.", HttpStatus.BAD_REQUEST, "BUDGET_CLOSED") {};
        }

        BigDecimal committedFunds = pool.getReservedAmount().add(pool.getSpentAmount());
        if (request.newAllocatedAmount().compareTo(committedFunds) < 0) {
            throw new SpendSyncException(
                    String.format("New allocation (%s) cannot be lower than already committed funds (%s = reserved %s + spent %s).",
                            request.newAllocatedAmount(), committedFunds, pool.getReservedAmount(), pool.getSpentAmount()),
                    HttpStatus.BAD_REQUEST,
                    "ADJUSTMENT_BELOW_COMMITTED_FUNDS"
            ) {};
        }

        BigDecimal previousAllocated = pool.getAllocatedAmount();
        pool.setAllocatedAmount(request.newAllocatedAmount());
        BudgetPool updated = budgetPoolRepository.save(pool);

        BigDecimal difference = request.newAllocatedAmount().subtract(previousAllocated);
        BudgetTransaction tx = new BudgetTransaction(
                updated,
                pool.getTenant(),
                BudgetTransactionType.ADJUSTMENT,
                difference,
                previousAllocated,
                request.newAllocatedAmount(),
                updated.getId(),
                "MANUAL_ADJUSTMENT",
                request.reason()
        );
        budgetTransactionRepository.save(tx);

        return BudgetPoolResponse.from(updated);
    }

    @Override
    public BudgetReservationResult reserveBudget(UUID budgetPoolId,
                                                BigDecimal amount,
                                                UUID referenceId,
                                                String referenceType,
                                                String notes) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        BudgetPool pool = budgetPoolRepository.findByIdAndTenantIdWithLock(budgetPoolId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Budget Pool not found", HttpStatus.NOT_FOUND, "BUDGET_POOL_NOT_FOUND") {});

        // Status check
        if (pool.getStatus() == BudgetStatus.DRAFT) {
            throw new SpendSyncException("Budget pool is in DRAFT state and not yet active for spending.",
                    HttpStatus.BAD_REQUEST, "BUDGET_NOT_ACTIVE") {};
        }
        if (pool.getStatus() == BudgetStatus.FROZEN) {
            throw new SpendSyncException("Budget pool is currently FROZEN under spending freeze measures. New PR submissions are blocked.",
                    HttpStatus.BAD_REQUEST, "BUDGET_FROZEN") {};
        }
        if (pool.getStatus() == BudgetStatus.CLOSED) {
            throw new SpendSyncException("Budget pool is CLOSED.", HttpStatus.BAD_REQUEST, "BUDGET_CLOSED") {};
        }

        BigDecimal availableBefore = pool.getAvailableAmount();
        boolean isOverrun = false;
        String resultMessage = "Budget successfully reserved.";

        if (amount.compareTo(availableBefore) > 0) {
            if (pool.getEnforcementMode() == BudgetEnforcementMode.HARD_STOP) {
                throw new SpendSyncException(
                        String.format("Insufficient available budget. Requested: %s %s, Available: %s %s. Hard stop enforced.",
                                amount, pool.getCurrency(), availableBefore, pool.getCurrency()),
                        HttpStatus.BAD_REQUEST,
                        "INSUFFICIENT_BUDGET"
                ) {};
            } else if (pool.getEnforcementMode() == BudgetEnforcementMode.TOLERANCE) {
                BigDecimal maxAllowed = pool.getMaxAllowedAllocation();
                BigDecimal totalRequired = pool.getReservedAmount().add(pool.getSpentAmount()).add(amount);

                if (totalRequired.compareTo(maxAllowed) > 0) {
                    throw new SpendSyncException(
                            String.format("Requested amount %s exceeds available budget including configured %s%% tolerance (Max allowed: %s).",
                                    amount, pool.getTolerancePercentage(), maxAllowed),
                            HttpStatus.BAD_REQUEST,
                            "BUDGET_TOLERANCE_EXCEEDED"
                    ) {};
                }
                isOverrun = true;
                resultMessage = String.format("Budget reserved within %s%% tolerance window (Overrun flagged).", pool.getTolerancePercentage());
            } else if (pool.getEnforcementMode() == BudgetEnforcementMode.SOFT_STOP) {
                isOverrun = true;
                resultMessage = "Budget reserved with SOFT_STOP warning. Executive approval required.";
            }
        }

        // Apply reservation
        pool.setReservedAmount(pool.getReservedAmount().add(amount));
        budgetPoolRepository.save(pool);

        BigDecimal availableAfter = pool.getAvailableAmount();

        // Audit transaction
        BudgetTransaction tx = new BudgetTransaction(
                pool,
                pool.getTenant(),
                BudgetTransactionType.RESERVE,
                amount,
                availableBefore,
                availableAfter,
                referenceId,
                referenceType != null ? referenceType : "REQUISITION",
                notes != null ? notes : resultMessage
        );
        budgetTransactionRepository.save(tx);

        return BudgetReservationResult.success(
                pool.getId(),
                isOverrun,
                resultMessage,
                amount,
                availableBefore,
                availableAfter,
                pool.getReservedAmount()
        );
    }

    @Override
    public BudgetPoolResponse releaseBudget(UUID budgetPoolId,
                                           BigDecimal amount,
                                           UUID referenceId,
                                           String referenceType,
                                           String notes) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        BudgetPool pool = budgetPoolRepository.findByIdAndTenantIdWithLock(budgetPoolId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Budget Pool not found", HttpStatus.NOT_FOUND, "BUDGET_POOL_NOT_FOUND") {});

        BigDecimal availableBefore = pool.getAvailableAmount();
        BigDecimal actualRelease = amount.min(pool.getReservedAmount());

        pool.setReservedAmount(pool.getReservedAmount().subtract(actualRelease));
        BudgetPool updated = budgetPoolRepository.save(pool);

        BigDecimal availableAfter = updated.getAvailableAmount();

        BudgetTransaction tx = new BudgetTransaction(
                updated,
                pool.getTenant(),
                BudgetTransactionType.RELEASE,
                actualRelease,
                availableBefore,
                availableAfter,
                referenceId,
                referenceType != null ? referenceType : "REQUISITION",
                notes != null ? notes : "Released reserved funds back to available pool."
        );
        budgetTransactionRepository.save(tx);

        return BudgetPoolResponse.from(updated);
    }

    @Override
    public BudgetPoolResponse commitBudget(UUID budgetPoolId,
                                          BigDecimal amount,
                                          UUID referenceId,
                                          String referenceType,
                                          String notes) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        BudgetPool pool = budgetPoolRepository.findByIdAndTenantIdWithLock(budgetPoolId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Budget Pool not found", HttpStatus.NOT_FOUND, "BUDGET_POOL_NOT_FOUND") {});

        BigDecimal availableBefore = pool.getAvailableAmount();

        // Reduce reservation and increase spent
        BigDecimal releaseFromReserved = amount.min(pool.getReservedAmount());
        pool.setReservedAmount(pool.getReservedAmount().subtract(releaseFromReserved));
        pool.setSpentAmount(pool.getSpentAmount().add(amount));

        BudgetPool updated = budgetPoolRepository.save(pool);
        BigDecimal availableAfter = updated.getAvailableAmount();

        BudgetTransaction tx = new BudgetTransaction(
                updated,
                pool.getTenant(),
                BudgetTransactionType.COMMIT,
                amount,
                availableBefore,
                availableAfter,
                referenceId,
                referenceType != null ? referenceType : "INVOICE",
                notes != null ? notes : "Committed funds from approved invoice."
        );
        budgetTransactionRepository.save(tx);

        return BudgetPoolResponse.from(updated);
    }

    @Override
    public void transferBudget(BudgetTransferRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();

        if (request.sourceBudgetPoolId().equals(request.targetBudgetPoolId())) {
            throw new SpendSyncException("Source and target budget pool cannot be the same.", HttpStatus.BAD_REQUEST, "SAME_SOURCE_TARGET_POOL") {};
        }

        // Prevent deadlocks by acquiring locks in deterministic ID order
        UUID firstId = request.sourceBudgetPoolId().compareTo(request.targetBudgetPoolId()) < 0
                ? request.sourceBudgetPoolId() : request.targetBudgetPoolId();
        UUID secondId = request.sourceBudgetPoolId().compareTo(request.targetBudgetPoolId()) < 0
                ? request.targetBudgetPoolId() : request.sourceBudgetPoolId();

        BudgetPool pool1 = budgetPoolRepository.findByIdAndTenantIdWithLock(firstId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Budget Pool " + firstId + " not found", HttpStatus.NOT_FOUND, "BUDGET_POOL_NOT_FOUND") {});
        BudgetPool pool2 = budgetPoolRepository.findByIdAndTenantIdWithLock(secondId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Budget Pool " + secondId + " not found", HttpStatus.NOT_FOUND, "BUDGET_POOL_NOT_FOUND") {});

        BudgetPool sourcePool = request.sourceBudgetPoolId().equals(pool1.getId()) ? pool1 : pool2;
        BudgetPool targetPool = request.targetBudgetPoolId().equals(pool1.getId()) ? pool1 : pool2;

        if (sourcePool.getStatus() != BudgetStatus.ACTIVE || targetPool.getStatus() != BudgetStatus.ACTIVE) {
            throw new SpendSyncException("Both source and target budget pools must be in ACTIVE status for transfers.",
                    HttpStatus.BAD_REQUEST, "BUDGET_NOT_ACTIVE") {};
        }

        if (!sourcePool.getCurrency().equalsIgnoreCase(targetPool.getCurrency())) {
            throw new SpendSyncException(
                    String.format("Currency mismatch between source (%s) and target (%s) budget pool.",
                            sourcePool.getCurrency(), targetPool.getCurrency()),
                    HttpStatus.BAD_REQUEST,
                    "CURRENCY_MISMATCH"
            ) {};
        }

        if (request.amount().compareTo(sourcePool.getAvailableAmount()) > 0) {
            throw new SpendSyncException(
                    String.format("Insufficient available budget in source pool for transfer. Requested: %s, Available: %s.",
                            request.amount(), sourcePool.getAvailableAmount()),
                    HttpStatus.BAD_REQUEST,
                    "INSUFFICIENT_TRANSFER_BUDGET"
            ) {};
        }

        // Debit Source
        BigDecimal sourceAllocBefore = sourcePool.getAllocatedAmount();
        sourcePool.setAllocatedAmount(sourcePool.getAllocatedAmount().subtract(request.amount()));
        budgetPoolRepository.save(sourcePool);

        BudgetTransaction debitTx = new BudgetTransaction(
                sourcePool,
                sourcePool.getTenant(),
                BudgetTransactionType.TRANSFER_OUT,
                request.amount(),
                sourceAllocBefore,
                sourcePool.getAllocatedAmount(),
                targetPool.getId(),
                "BUDGET_TRANSFER",
                "Transferred to " + targetPool.getCostCenter().getName() + ": " + request.reason()
        );
        budgetTransactionRepository.save(debitTx);

        // Credit Target
        BigDecimal targetAllocBefore = targetPool.getAllocatedAmount();
        targetPool.setAllocatedAmount(targetPool.getAllocatedAmount().add(request.amount()));
        budgetPoolRepository.save(targetPool);

        BudgetTransaction creditTx = new BudgetTransaction(
                targetPool,
                targetPool.getTenant(),
                BudgetTransactionType.TRANSFER_IN,
                request.amount(),
                targetAllocBefore,
                targetPool.getAllocatedAmount(),
                sourcePool.getId(),
                "BUDGET_TRANSFER",
                "Transferred from " + sourcePool.getCostCenter().getName() + ": " + request.reason()
        );
        budgetTransactionRepository.save(creditTx);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetSummaryResponse getBudgetSummary(int fiscalYear) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<BudgetPool> pools = budgetPoolRepository.findAllByTenantIdAndFiscalYear(tenantId, fiscalYear);

        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalReserved = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;

        for (BudgetPool p : pools) {
            totalAllocated = totalAllocated.add(p.getAllocatedAmount());
            totalReserved = totalReserved.add(p.getReservedAmount());
            totalSpent = totalSpent.add(p.getSpentAmount());
            totalAvailable = totalAvailable.add(p.getAvailableAmount());
        }

        List<BudgetPoolResponse> poolResponses = pools.stream().map(BudgetPoolResponse::from).toList();

        return new BudgetSummaryResponse(
                fiscalYear,
                pools.size(),
                totalAllocated,
                totalReserved,
                totalSpent,
                totalAvailable,
                poolResponses
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetTransactionResponse> getTransactionsForPool(UUID budgetPoolId) {
        return budgetTransactionRepository.findAllByBudgetPoolIdOrderByCreatedAtDesc(budgetPoolId).stream()
                .map(BudgetTransactionResponse::from)
                .toList();
    }
}
