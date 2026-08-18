package com.enterprise.spendsync.core.internal.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable Role → Permission registry for the SpendSync Procure-to-Pay engine.
 *
 * <p>This class is the single source of truth for which capabilities each role holds.
 * Permission sets are built once at startup and are unmodifiable thereafter,
 * enforcing the principle that role-to-permission mapping is a <em>domain invariant</em>
 * (derived from ISO 37001 / SOX compliance), not a runtime configuration option.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 *   // Inject the registry
 *   Set<Permission> perms = registry.getPermissionsForRole(RoleType.APPROVER);
 *
 *   // Get Spring GrantedAuthority set for a multi-role user
 *   Set<GrantedAuthority> authorities = registry.getAuthoritiesForRoles(user.getRoles());
 * }</pre>
 */
@Component
public class RolePermissionRegistry {

    private static final Map<RoleType, Set<Permission>> ROLE_PERMISSIONS;

    static {
        EnumMap<RoleType, Set<Permission>> map = new EnumMap<>(RoleType.class);

        // ── ROOT_USER ─────────────────────────────────────────────────────────
        // Full organizational and observational rights.
        // Does NOT approve PRs or create invoices — separation of administrative power.
        map.put(RoleType.ROOT_USER, EnumSet.of(
                Permission.ORG_MANAGE,
                Permission.USER_MANAGE,
                Permission.INVITATION_CREATE,
                Permission.BUDGET_READ,
                Permission.BUDGET_MANAGE,
                Permission.PR_READ_ALL,
                Permission.PO_CREATE,
                Permission.PO_READ,
                Permission.PO_UPDATE,
                Permission.VENDOR_MANAGE,
                Permission.GR_READ,
                Permission.INVOICE_READ,
                Permission.AUDIT_READ
        ));

        // ── ACCOUNT_USER ──────────────────────────────────────────────────────
        // Finance controller / budget steward. Sees all PRs, manages budgets.
        // Cannot approve PRs directly — SoD boundary.
        map.put(RoleType.ACCOUNT_USER, EnumSet.of(
                Permission.BUDGET_READ,
                Permission.BUDGET_MANAGE,
                Permission.PR_READ_ALL,
                Permission.INVOICE_READ,
                Permission.AUDIT_READ
        ));

        // ── APPROVER ──────────────────────────────────────────────────────────
        // Approval authority for PRs within signature threshold.
        // Cannot create PRs (SoD: requester ≠ approver enforced at service layer).
        map.put(RoleType.APPROVER, EnumSet.of(
                Permission.PR_READ_ALL,
                Permission.PR_APPROVE,
                Permission.PR_REJECT,
                Permission.BUDGET_READ
        ));

        // ── FACILITY_USER ─────────────────────────────────────────────────────
        // Dock / warehouse personnel. Receives goods and issues GR documents.
        // No financial or approval permissions.
        map.put(RoleType.FACILITY_USER, EnumSet.of(
                Permission.GR_CREATE,
                Permission.GR_READ,
                Permission.PO_READ
        ));

        // ── PROCUREMENT ───────────────────────────────────────────────────────
        // Converts approved PRs to POs, manages supplier relationships.
        // Cannot approve PRs (would short-circuit the approval workflow).
        map.put(RoleType.PROCUREMENT, EnumSet.of(
                Permission.PR_READ_ALL,
                Permission.PO_CREATE,
                Permission.PO_READ,
                Permission.PO_UPDATE,
                Permission.VENDOR_MANAGE,
                Permission.MATCH_EVALUATE
        ));

        // ── AP_SPECIALIST ─────────────────────────────────────────────────────
        // Accounts Payable: enters invoices, resolves 3-way match, releases payments.
        // Cannot create or approve PRs — financial execution role only.
        map.put(RoleType.AP_SPECIALIST, EnumSet.of(
                Permission.INVOICE_CREATE,
                Permission.INVOICE_READ,
                Permission.MATCH_EVALUATE,
                Permission.PAYMENT_RELEASE,
                Permission.PO_READ,
                Permission.GR_READ
        ));

        // ── REQUISITIONER ─────────────────────────────────────────────────────
        // End-user / department staff. Creates PRs and tracks their own requests.
        // Most constrained role by design — cannot see others' PRs or approve anything.
        map.put(RoleType.REQUISITIONER, EnumSet.of(
                Permission.PR_CREATE,
                Permission.PR_READ_OWN
        ));

        ROLE_PERMISSIONS = Collections.unmodifiableMap(map);
    }

    /**
     * Returns the immutable {@link Permission} set for the given role.
     *
     * @param role a non-null {@link RoleType}
     * @return unmodifiable {@code Set<Permission>}, never null, may be empty
     */
    public Set<Permission> getPermissionsForRole(RoleType role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Collections.emptySet());
    }

    /**
     * Returns the union of all permissions held by the given roles as Spring
     * {@link GrantedAuthority} objects (prefixed with {@code "PERM_"}).
     *
     * <p>Role authorities themselves are also included prefixed with {@code "ROLE_"}
     * so that both {@code @PreAuthorize("hasRole('APPROVER')")} and
     * {@code @PreAuthorize("hasAuthority('PERM_PR_APPROVE')")} patterns work.</p>
     *
     * @param roles a set of roles assigned to the user
     * @return a flat {@code Set<GrantedAuthority>} including role + permission authorities
     */
    public Set<GrantedAuthority> getAuthoritiesForRoles(Set<RoleType> roles) {
        Set<GrantedAuthority> authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());

        roles.stream()
                .flatMap(role -> getPermissionsForRole(role).stream())
                .map(perm -> (GrantedAuthority) new SimpleGrantedAuthority("PERM_" + perm.name()))
                .forEach(authorities::add);

        return Collections.unmodifiableSet(authorities);
    }

    /**
     * Checks whether the union of permissions across the given roles includes
     * the specified permission. Convenience method for programmatic policy checks.
     *
     * @param roles      the user's role set
     * @param permission the permission to test
     * @return {@code true} if at least one role grants the permission
     */
    public boolean hasPermission(Set<RoleType> roles, Permission permission) {
        return roles.stream()
                .anyMatch(role -> getPermissionsForRole(role).contains(permission));
    }
}
