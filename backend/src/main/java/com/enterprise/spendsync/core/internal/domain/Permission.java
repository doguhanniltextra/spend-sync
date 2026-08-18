package com.enterprise.spendsync.core.internal.domain;

/**
 * Granular permission codes for the SpendSync Procure-to-Pay engine.
 *
 * <p>Permissions are assigned immutably to roles via {@link RolePermissionRegistry}.
 * They represent a specific capability within a bounded domain module.
 * These are NOT configurable at tenant level — they are domain invariants derived
 * from ISO 37001 / SOX compliance requirements.</p>
 *
 * <p>Naming convention: {@code <DOMAIN>_<ACTION>}</p>
 */
public enum Permission {

    // ── Organization / Core ──────────────────────────────────────────────
    /**
     * Manage Legal Entities, Facilities, Cost Centers and company settings.
     */
    ORG_MANAGE,

    /**
     * Update user roles, assign legal entities, suspend/activate accounts.
     */
    USER_MANAGE,

    /**
     * Create sub-account invitations and requisitioner multi-use links.
     */
    INVITATION_CREATE,

    // ── Budget ───────────────────────────────────────────────────────────
    /**
     * View department budget limits and current consumption.
     */
    BUDGET_READ,

    /**
     * Define budget pools, adjust limits, authorize budget transfers.
     */
    BUDGET_MANAGE,

    // ── Requisition (PR) ─────────────────────────────────────────────────
    /**
     * Create and edit Purchase Requisitions (PR).
     */
    PR_CREATE,

    /**
     * Read only the PRs created by the authenticated user.
     */
    PR_READ_OWN,

    /**
     * Read all PRs across the department or company scope.
     */
    PR_READ_ALL,

    /**
     * Approve a PR (subject to signature-threshold and SoD checks).
     */
    PR_APPROVE,

    /**
     * Reject a PR and provide a rejection reason.
     */
    PR_REJECT,

    // ── Purchasing (PO) ──────────────────────────────────────────────────
    /**
     * Convert an approved PR into a formal Purchase Order (PO).
     */
    PO_CREATE,

    /**
     * View Purchase Orders.
     */
    PO_READ,

    /**
     * Revise a PO and transmit updated terms to the supplier.
     */
    PO_UPDATE,

    /**
     * Create and maintain Vendor Master records.
     */
    VENDOR_MANAGE,

    // ── Receiving / Dock (GR) ────────────────────────────────────────────
    /**
     * Perform physical goods count and issue a Goods Receipt (GR) document.
     */
    GR_CREATE,

    /**
     * View Goods Receipt documents and quality inspection reports.
     */
    GR_READ,

    // ── Finance / AP ─────────────────────────────────────────────────────
    /**
     * Enter a supplier invoice into the system for 3-way matching.
     */
    INVOICE_CREATE,

    /**
     * View invoices and 3-Way Match evaluation results.
     */
    INVOICE_READ,

    /**
     * Resolve 3-Way Match discrepancies (quantity / price variances).
     */
    MATCH_EVALUATE,

    /**
     * Authorize invoice payment and dispatch to ERP/payment outbox.
     */
    PAYMENT_RELEASE,

    // ── Audit & Compliance ──────────────────────────────────────────────
    /**
     * View immutable audit trail and ISO compliance logs.
     */
    AUDIT_READ
}
