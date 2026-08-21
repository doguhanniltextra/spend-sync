package com.enterprise.spendsync.core.security;

import com.enterprise.spendsync.core.internal.domain.Permission;
import com.enterprise.spendsync.core.internal.domain.RolePermissionRegistry;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RolePermissionRegistry Pure Unit Tests (ISO 37001 RBAC Mapping)")
class RolePermissionRegistryTest {

    private RolePermissionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RolePermissionRegistry();
    }

    @Test
    @DisplayName("Should contain immutable unmodifiable permission sets for all roles")
    void shouldReturnImmutablePermissionSets() {
        Set<Permission> approverPerms = registry.getPermissionsForRole(RoleType.APPROVER);

        assertThat(approverPerms).isNotNull().isNotEmpty();
        assertThatThrownBy(() -> approverPerms.add(Permission.ORG_MANAGE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should enforce Segregation of Administrative Power for ROOT_USER")
    void shouldEnforceRootUserPermissions() {
        Set<Permission> rootPerms = registry.getPermissionsForRole(RoleType.ROOT_USER);

        // ROOT_USER has administrative & org rights
        assertThat(rootPerms).contains(
                Permission.ORG_MANAGE,
                Permission.USER_MANAGE,
                Permission.BUDGET_MANAGE,
                Permission.AUDIT_READ
        );
    }

    @Test
    @DisplayName("Should map PROCUREMENT role to PO creation and vendor management")
    void shouldMapProcurementRolePermissions() {
        Set<Permission> procurementPerms = registry.getPermissionsForRole(RoleType.PROCUREMENT);

        assertThat(procurementPerms).contains(
                Permission.PO_CREATE,
                Permission.PO_READ,
                Permission.PO_UPDATE,
                Permission.VENDOR_MANAGE
        );
        assertThat(procurementPerms).doesNotContain(Permission.PR_APPROVE, Permission.PAYMENT_RELEASE);
    }

    @Test
    @DisplayName("Should map FACILITY_USER role to Goods Receipt creation")
    void shouldMapFacilityUserRolePermissions() {
        Set<Permission> facilityPerms = registry.getPermissionsForRole(RoleType.FACILITY_USER);

        assertThat(facilityPerms).contains(Permission.GR_CREATE, Permission.GR_READ);
        assertThat(facilityPerms).doesNotContain(Permission.PO_CREATE, Permission.PR_APPROVE);
    }

    @Test
    @DisplayName("Should combine permissions when a user has multiple roles (Union)")
    void shouldCombinePermissionsForMultiRoleUser() {
        Set<RoleType> multiRoles = Set.of(RoleType.PROCUREMENT, RoleType.APPROVER);

        Set<GrantedAuthority> authorities = registry.getAuthoritiesForRoles(multiRoles);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains(
                        "PERM_PO_CREATE",
                        "PERM_VENDOR_MANAGE",
                        "PERM_PR_APPROVE",
                        "ROLE_PROCUREMENT",
                        "ROLE_APPROVER"
                );
    }

    @Test
    @DisplayName("Should correctly evaluate hasPermission helper")
    void shouldEvaluateHasPermission() {
        Set<RoleType> roles = Set.of(RoleType.APPROVER);

        assertThat(registry.hasPermission(roles, Permission.PR_APPROVE)).isTrue();
        assertThat(registry.hasPermission(roles, Permission.PO_CREATE)).isFalse();
    }
}
