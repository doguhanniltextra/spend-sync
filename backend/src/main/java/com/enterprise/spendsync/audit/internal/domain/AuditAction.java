package com.enterprise.spendsync.audit.internal.domain;

/**
 * Standard business actions tracked by the immutable audit engine.
 */
public enum AuditAction {
    // User & Identity
    USER_REGISTERED,
    USER_LOGGED_IN,
    USER_INVITED,
    SUBACCOUNT_ACCEPTED,

    // Budget Engine
    BUDGET_POOL_CREATED,
    BUDGET_RESERVED,
    BUDGET_RELEASED,
    BUDGET_COMMITTED,
    BUDGET_ADJUSTED,
    BUDGET_TRANSFERRED,

    // Approval Authority Limits
    APPROVAL_LIMIT_CREATED,
    APPROVAL_LIMIT_UPDATED,

    // Requisition (PR) Lifecycle
    REQUISITION_CREATED,
    REQUISITION_STEP_APPROVED,
    REQUISITION_APPROVED,
    REQUISITION_REJECTED,
    REQUISITION_CANCELLED,

    // Compliance & SoD Violations
    SOD_VIOLATION_BLOCKED,
    SIGNATURE_LIMIT_EXCEEDED_BLOCKED,
    CROSS_ENTITY_WARNING_TRIGGERED,

    // Vendor Master
    VENDOR_CREATED,
    VENDOR_STATUS_CHANGED,

    // Purchasing (PO) Lifecycle
    PURCHASE_ORDER_CREATED,
    PURCHASE_ORDER_ISSUED,
    PURCHASE_ORDER_REVISED,
    PURCHASE_ORDER_CANCELLED,

    // Receiving & AP
    GOODS_RECEIPT_CREATED,
    INVOICE_MATCH_SUCCESS,
    INVOICE_MATCH_FAILED
}
