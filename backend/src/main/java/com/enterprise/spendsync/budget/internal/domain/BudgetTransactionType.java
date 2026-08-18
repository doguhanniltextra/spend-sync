package com.enterprise.spendsync.budget.internal.domain;

/**
 * Type of budget ledger mutation for double-entry tracking and audit trail.
 */
public enum BudgetTransactionType {
    /**
     * Initial budget allocation at pool creation.
     */
    INITIAL_ALLOCATION,

    /**
     * Funds reserved upon Purchase Requisition (PR) submission.
     */
    RESERVE,

    /**
     * Reserved funds released upon PR rejection, cancellation or expiration.
     */
    RELEASE,

    /**
     * Reserved funds committed / spent upon Invoice approval and 3-Way Match.
     */
    COMMIT,

    /**
     * Manual budget limit adjustment by Account User or Root User.
     */
    ADJUSTMENT,

    /**
     * Debit leg of an inter-budget or inter-period transfer.
     */
    TRANSFER_OUT,

    /**
     * Credit leg of an inter-budget or inter-period transfer.
     */
    TRANSFER_IN
}
