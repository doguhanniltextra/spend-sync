package com.enterprise.spendsync.requisition.internal.domain;

/**
 * Lifecycle state machine of a Purchase Requisition.
 */
public enum RequisitionStatus {
    /**
     * Initial draft created by requisitioner, editable, no budget reserved yet.
     */
    DRAFT,

    /**
     * Submitted by requisitioner; budget is reserved and approval DAG is active.
     */
    PENDING_APPROVAL,

    /**
     * All approval DAG steps completed successfully. Ready for PO conversion in purchasing module.
     */
    APPROVED,

    /**
     * Rejected by an approver. Reserved budget has been released back to available pool.
     */
    REJECTED,

    /**
     * Cancelled by requisitioner before final approval. Reserved budget has been released.
     */
    CANCELLED
}
