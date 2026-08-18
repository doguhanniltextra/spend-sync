package com.enterprise.spendsync.core.internal.web;

import com.enterprise.spendsync.core.internal.domain.Permission;
import com.enterprise.spendsync.core.internal.domain.RolePermissionRegistry;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.shared.config.Endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC inspection endpoint — returns the full role-to-permission matrix.
 *
 * <p>Intended for development, compliance auditing, and integration testing.
 * In production this endpoint should be secured behind ROOT_USER authentication.</p>
 */
@RestController
@RequestMapping(Endpoints.Organization.BASE)
public class RbacInspectionController {

    private final RolePermissionRegistry registry;

    public RbacInspectionController(RolePermissionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Returns the complete role → permission matrix.
     * GET /api/v1/organization/rbac/matrix
     */
    @GetMapping("/rbac/matrix")
    public ResponseEntity<Map<String, Object>> getPermissionMatrix() {
        Map<String, Object> matrix = new LinkedHashMap<>();

        for (RoleType role : RoleType.values()) {
            Set<Permission> perms = registry.getPermissionsForRole(role);
            Map<String, Object> roleInfo = new LinkedHashMap<>();
            roleInfo.put("permissionCount", perms.size());
            roleInfo.put("permissions", perms.stream()
                    .map(Permission::name)
                    .sorted()
                    .collect(Collectors.toList()));
            matrix.put(role.name(), roleInfo);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalRoles", RoleType.values().length);
        response.put("totalPermissions", Permission.values().length);
        response.put("matrix", matrix);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns permissions for a single role.
     * GET /api/v1/organization/rbac/roles/{role}
     */
    @GetMapping("/rbac/roles/{role}")
    public ResponseEntity<Map<String, Object>> getRolePermissions(@PathVariable String role) {
        RoleType roleType = RoleType.valueOf(role.toUpperCase());
        Set<Permission> perms = registry.getPermissionsForRole(roleType);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("role", roleType.name());
        response.put("permissionCount", perms.size());
        response.put("permissions", perms.stream()
                .map(Permission::name)
                .sorted()
                .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }
}
