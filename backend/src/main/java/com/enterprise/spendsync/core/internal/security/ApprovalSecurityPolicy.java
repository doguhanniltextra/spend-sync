package com.enterprise.spendsync.core.internal.security;

/**
 * MOVED — This class has been relocated to {@link com.enterprise.spendsync.shared.security.ApprovalSecurityPolicy}.
 *
 * <p>Domain security policies are cross-cutting concerns and must not live inside
 * a domain module's {@code internal} package. Update all imports accordingly.</p>
 *
 * @deprecated Use {@code com.enterprise.spendsync.shared.security.ApprovalSecurityPolicy} instead.
 *             This file will be deleted in the next cleanup commit.
 */
@Deprecated(since = "Task-11-refactor", forRemoval = true)
public final class ApprovalSecurityPolicy {
    private ApprovalSecurityPolicy() {
        throw new UnsupportedOperationException(
            "ApprovalSecurityPolicy has moved to com.enterprise.spendsync.shared.security.ApprovalSecurityPolicy");
    }
}
