package com.enterprise.spendsync.requisition.internal.domain;

/**
 * State of a single step within a dynamic approval DAG chain.
 */
public enum ApprovalStepStatus {
    /**
     * Currently waiting for this approver's active review/decision.
     */
    PENDING,

    /**
     * Inactive, waiting for preceding step(s) in the DAG chain to approve first.
     */
    WAITING,

    /**
     * Successfully approved by the designated approver.
     */
    APPROVED,

    /**
     * Rejected by this approver, terminating the entire requisition workflow.
     */
    REJECTED,

    /**
     * Skipped (e.g. if a higher-level authority directly pre-empted or approved).
     */
    SKIPPED
}
