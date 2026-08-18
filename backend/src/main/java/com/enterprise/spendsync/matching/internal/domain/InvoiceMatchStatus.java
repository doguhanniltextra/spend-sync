package com.enterprise.spendsync.matching.internal.domain;

public enum InvoiceMatchStatus {
    EVALUATING,
    AUTO_MATCHED,
    MANUALLY_MATCHED,
    DISCREPANCY_HOLD,
    REJECTED
}
