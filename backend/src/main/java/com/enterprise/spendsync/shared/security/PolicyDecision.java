package com.enterprise.spendsync.shared.security;

/**
 * Value object representing the result of a domain security policy evaluation.
 *
 * <p>Use the static factory methods {@link #allowed()} and {@link #denied(String, String)}
 * to construct instances.</p>
 *
 * <pre>{@code
 *   PolicyDecision decision = approvalPolicy.canApproveRequisition(...);
 *   if (!decision.isAllowed()) {
 *       throw new SpendSyncException(decision.getReason(), HttpStatus.FORBIDDEN, decision.getErrorCode());
 *   }
 * }</pre>
 */
public final class PolicyDecision {

    private final boolean allowed;
    private final String errorCode;
    private final String reason;

    private PolicyDecision(boolean allowed, String errorCode, String reason) {
        this.allowed = allowed;
        this.errorCode = errorCode;
        this.reason = reason;
    }

    /**
     * Creates a decision that permits the operation.
     */
    public static PolicyDecision allowed() {
        return new PolicyDecision(true, null, null);
    }

    /**
     * Creates a decision that denies the operation with a structured reason.
     *
     * @param errorCode machine-readable code (e.g. {@code "SOD_VIOLATION_SELF_APPROVAL"})
     * @param reason    human-readable explanation for logging and API responses
     */
    public static PolicyDecision denied(String errorCode, String reason) {
        return new PolicyDecision(false, errorCode, reason);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return allowed
                ? "PolicyDecision{ALLOWED}"
                : "PolicyDecision{DENIED, errorCode='" + errorCode + "', reason='" + reason + "'}";
    }
}
