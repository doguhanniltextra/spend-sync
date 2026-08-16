package com.enterprise.spendsync.shared.tenant;

import com.enterprise.spendsync.shared.exception.SpendSyncException;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * ThreadLocal context holder for the active Tenant ID.
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
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static UUID getRequiredTenantId() {
        return getTenantId().orElseThrow(() ->
                new SpendSyncException("No active tenant context found on the current thread.", HttpStatus.BAD_REQUEST, "MISSING_TENANT_CONTEXT") {});
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
