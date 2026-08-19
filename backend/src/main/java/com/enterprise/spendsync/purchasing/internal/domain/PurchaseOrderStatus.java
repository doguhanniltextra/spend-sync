package com.enterprise.spendsync.purchasing.internal.domain;

/**
 * Lifecycle states of a Purchase Order (PO).
 */
public enum PurchaseOrderStatus {
    DRAFT,              // Buyer preparing draft PO
    ISSUED,             // Issued and transmitted to vendor, awaiting delivery
    REVISED,            // Under revision workflow
    PARTIALLY_RECEIVED, // Partially received at warehouse (GR)
    FULFILLED,          // Completely fulfilled and invoiced
    CANCELLED           // Order cancelled (remaining budget reservation released)
}
