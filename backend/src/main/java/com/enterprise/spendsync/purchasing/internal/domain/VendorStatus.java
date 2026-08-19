package com.enterprise.spendsync.purchasing.internal.domain;

/**
 * Operational status of a vendor master record.
 */
public enum VendorStatus {
    ACTIVE,             // Active and orderable
    BLOCKED,            // Temporarily suspended
    INACTIVE            // Inactive
}
