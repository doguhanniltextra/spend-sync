package com.enterprise.spendsync.shared.tenant;

import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Context holder for the active Tenant ID.
 * Resolves from ThreadLocal storage or active JWT SecurityContext principal.
 * Ensures strict multi-tenant data isolation across all service layers and database queries.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Optional<UUID> getTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId != null) {
            return Optional.of(tenantId);
        }

        // Fallback: Resolve tenantId from SecurityContext UserPrincipal (JWT authentication)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getTenantId() != null) {
                return Optional.of(principal.getTenantId());
            }
        }

        return Optional.empty();
    }

    public static UUID getRequiredTenantId() {
        return getTenantId().orElseThrow(() ->
                new SpendSyncException("No active tenant context found on the current thread.", HttpStatus.BAD_REQUEST, "MISSING_TENANT_CONTEXT") {});
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
