package com.enterprise.spendsync.budget.internal.repository;

import com.enterprise.spendsync.budget.internal.domain.BudgetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetTransactionRepository extends JpaRepository<BudgetTransaction, UUID> {

    List<BudgetTransaction> findAllByBudgetPoolIdOrderByCreatedAtDesc(UUID budgetPoolId);

    List<BudgetTransaction> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
