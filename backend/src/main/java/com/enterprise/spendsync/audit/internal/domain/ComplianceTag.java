package com.enterprise.spendsync.audit.internal.domain;

/**
 * ISO and Regulatory compliance classifications for audit logging.
 */
public enum ComplianceTag {
    ISO_27001_ACCESS_CONTROL,   // Access control, login, permissions
    ISO_27001_LOGGING,          // Standard lifecycle event logging
    ISO_37001_ANTI_BRIBERY,     // Vendor approval, payment authorizations
    ISO_37001_SOD_CONTROL,      // Segregation of Duties violations & checks
    ISO_9001_TRACEABILITY,      // Requisition -> PO -> GR -> Invoice trace
    SOX_404_FINANCIAL_CONTROL   // Budget adjustments, reserves, commitments
}
