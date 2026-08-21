package com.enterprise.spendsync.shared.ratelimit;

/**
 * Identifier strategy for rate limiting.
 */
public enum RateLimitType {
    /**
     * Rate limit by client remote IP address (X-Forwarded-For or remote socket).
     * Ideal for unauthenticated / public endpoints like Login and Register.
     */
    IP,

    /**
     * Rate limit by authenticated User ID / Principal.
     * Ideal for heavy user operations like Excel export, Report generation.
     */
    USER,

    /**
     * Rate limit by active Multi-Tenant ID.
     * Ideal for B2B supplier integration and API rate allocation.
     */
    TENANT
}
