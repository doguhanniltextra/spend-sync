package com.enterprise.spendsync.budget.internal.domain;

/**
 * Lifecycle state of a Budget Pool.
 */
public enum BudgetStatus {
    /**
     * Budget is being planned / prepared. Not open for spending or PR reservations.
     */
    DRAFT,

    /**
     * Active budget. Open for PR reservations, commitments, and transfers.
     */
    ACTIVE,

    /**
     * Spending freeze / austerity measure. Existing reservations are preserved,
     * but new reservations and PR submissions are blocked.
     */
    FROZEN,

    /**
     * Fiscal period closed. No further transactions or modifications allowed.
     */
    CLOSED
}
