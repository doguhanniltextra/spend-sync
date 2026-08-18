package com.enterprise.spendsync.budget.internal.web;

import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.dto.AdjustBudgetRequest;
import com.enterprise.spendsync.budget.internal.dto.BudgetPoolResponse;
import com.enterprise.spendsync.budget.internal.dto.BudgetSummaryResponse;
import com.enterprise.spendsync.budget.internal.dto.BudgetTransactionResponse;
import com.enterprise.spendsync.budget.internal.dto.BudgetTransferRequest;
import com.enterprise.spendsync.budget.internal.dto.CreateBudgetPoolRequest;
import com.enterprise.spendsync.budget.internal.dto.UpdateBudgetStatusRequest;
import com.enterprise.spendsync.budget.internal.service.BudgetService;
import com.enterprise.spendsync.shared.config.Endpoints;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Budget Management & Ledger REST Controller.
 * Protected by fine-grained RBAC authorities (PERM_BUDGET_READ, PERM_BUDGET_MANAGE).
 */
@RestController
@RequestMapping(Endpoints.Budget.BASE)
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PreAuthorize("hasAuthority('PERM_BUDGET_MANAGE')")
    @PostMapping(Endpoints.Budget.POOLS)
    public ResponseEntity<BudgetPoolResponse> createBudgetPool(@Valid @RequestBody CreateBudgetPoolRequest request) {
        BudgetPoolResponse response = budgetService.createBudgetPool(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('PERM_BUDGET_READ')")
    @GetMapping(Endpoints.Budget.POOLS)
    public ResponseEntity<List<BudgetPoolResponse>> getAllBudgetPools(
            @RequestParam(required = false) Integer fiscalYear,
            @RequestParam(required = false) BudgetStatus status
    ) {
        List<BudgetPoolResponse> response = budgetService.getAllBudgetPools(fiscalYear, status);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_BUDGET_READ')")
    @GetMapping(Endpoints.Budget.POOL_BY_ID)
    public ResponseEntity<BudgetPoolResponse> getBudgetPoolById(@PathVariable UUID id) {
        BudgetPoolResponse response = budgetService.getBudgetPoolById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_BUDGET_MANAGE')")
    @PatchMapping(Endpoints.Budget.POOL_STATUS)
    public ResponseEntity<BudgetPoolResponse> updateBudgetStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetStatusRequest request
    ) {
        BudgetPoolResponse response = budgetService.updateBudgetStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_BUDGET_MANAGE')")
    @PatchMapping(Endpoints.Budget.POOL_ADJUST)
    public ResponseEntity<BudgetPoolResponse> adjustBudget(
            @PathVariable UUID id,
            @Valid @RequestBody AdjustBudgetRequest request
    ) {
        BudgetPoolResponse response = budgetService.adjustBudget(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_BUDGET_READ')")
    @GetMapping(Endpoints.Budget.POOL_TRANSACTIONS)
    public ResponseEntity<List<BudgetTransactionResponse>> getTransactionsForPool(@PathVariable UUID id) {
        List<BudgetTransactionResponse> response = budgetService.getTransactionsForPool(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_BUDGET_MANAGE')")
    @PostMapping(Endpoints.Budget.TRANSFERS)
    public ResponseEntity<Void> transferBudget(@Valid @RequestBody BudgetTransferRequest request) {
        budgetService.transferBudget(request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('PERM_BUDGET_READ')")
    @GetMapping(Endpoints.Budget.SUMMARY)
    public ResponseEntity<BudgetSummaryResponse> getBudgetSummary(
            @RequestParam(defaultValue = "2026") int fiscalYear
    ) {
        BudgetSummaryResponse response = budgetService.getBudgetSummary(fiscalYear);
        return ResponseEntity.ok(response);
    }
}
