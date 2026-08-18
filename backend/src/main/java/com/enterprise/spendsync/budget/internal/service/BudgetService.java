package com.enterprise.spendsync.budget.internal.service;

import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.dto.AdjustBudgetRequest;
import com.enterprise.spendsync.budget.internal.dto.BudgetPoolResponse;
import com.enterprise.spendsync.budget.internal.dto.BudgetReservationResult;
import com.enterprise.spendsync.budget.internal.dto.BudgetSummaryResponse;
import com.enterprise.spendsync.budget.internal.dto.BudgetTransactionResponse;
import com.enterprise.spendsync.budget.internal.dto.BudgetTransferRequest;
import com.enterprise.spendsync.budget.internal.dto.CreateBudgetPoolRequest;
import com.enterprise.spendsync.budget.internal.dto.UpdateBudgetStatusRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BudgetService {

    BudgetPoolResponse createBudgetPool(CreateBudgetPoolRequest request);

    BudgetPoolResponse getBudgetPoolById(UUID id);

    List<BudgetPoolResponse> getAllBudgetPools(Integer fiscalYear, BudgetStatus status);

    BudgetPoolResponse updateBudgetStatus(UUID id, UpdateBudgetStatusRequest request);

    BudgetPoolResponse adjustBudget(UUID id, AdjustBudgetRequest request);

    /**
     * Reserves funds in the specified budget pool upon PR submission.
     * Protected by Pessimistic Write Lock and Enforcement Mode checks.
     */
    BudgetReservationResult reserveBudget(UUID budgetPoolId,
                                         BigDecimal amount,
                                         UUID referenceId,
                                         String referenceType,
                                         String notes);

    /**
     * Releases reserved funds back to available balance upon PR rejection / expiration.
     */
    BudgetPoolResponse releaseBudget(UUID budgetPoolId,
                                    BigDecimal amount,
                                    UUID referenceId,
                                    String referenceType,
                                    String notes);

    /**
     * Commits reserved funds into spent ledger upon Invoice approval and 3-Way Match.
     */
    BudgetPoolResponse commitBudget(UUID budgetPoolId,
                                   BigDecimal amount,
                                   UUID referenceId,
                                   String referenceType,
                                   String notes);

    /**
     * Atomically transfers funds from a source budget pool to a target budget pool.
     */
    void transferBudget(BudgetTransferRequest request);

    BudgetSummaryResponse getBudgetSummary(int fiscalYear);

    List<BudgetTransactionResponse> getTransactionsForPool(UUID budgetPoolId);
}
