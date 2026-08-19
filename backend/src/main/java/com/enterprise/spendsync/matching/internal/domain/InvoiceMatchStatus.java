package com.enterprise.spendsync.matching.internal.domain;

public enum InvoiceMatchStatus {
    EVALUATING,
    AUTO_MATCHED,
    MANUALLY_MATCHED,
    DISCREPANCY_HOLD,
    PRICE_DISCREPANCY,
    QUANTITY_DISCREPANCY,
    PENDING_RECEIPT,
    REJECTED
}
