package com.enterprise.spendsync.shared.security;

import com.enterprise.spendsync.core.internal.domain.Permission;
import com.enterprise.spendsync.core.internal.domain.RolePermissionRegistry;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Domain security policy for Facility / Dock receiving operations.
 *
 * <p>Controls which users may perform Goods Receipt (GR) actions at a given facility.
 * Checks are two-layered:</p>
 * <ol>
 *   <li><strong>Permission layer</strong> — User must hold {@link Permission#GR_CREATE}.</li>
 *   <li><strong>Assignment layer</strong> — User must be assigned to the target facility
 *       (checked against {@code user_assigned_legal_entities} or a future
 *       {@code user_assigned_facilities} table).</li>
 * </ol>
 *
 * <p>The assignment check is <em>context-driven</em>: the calling service (receiving module)
 * passes the set of facility UUIDs the user is already associated with. This keeps the policy
 * class stateless and independently testable.</p>
 */
@Component
public class FacilitySecurityPolicy {

    private final RolePermissionRegistry registry;

    public FacilitySecurityPolicy(RolePermissionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Evaluates whether a user may create a Goods Receipt at the specified facility.
     *
     * @param userRoles              the performing user's current role set
     * @param targetFacilityId       UUID of the facility where goods are being received
     * @param userAssignedFacilities set of facility UUIDs the user is assigned to
     * @return {@link PolicyDecision#allowed()} if permitted, otherwise {@link PolicyDecision#denied(String, String)}
     */
    public PolicyDecision canReceiveAtFacility(
            Set<RoleType> userRoles,
            UUID targetFacilityId,
            Set<UUID> userAssignedFacilities
    ) {
        // 1. Role capability check — must hold GR_CREATE
        if (!registry.hasPermission(userRoles, Permission.GR_CREATE)) {
            return PolicyDecision.denied("INSUFFICIENT_PERMISSION",
                    "User does not hold the GR_CREATE permission required for dock receiving.");
        }

        // 2. Facility assignment check — user must be assigned to this specific facility
        if (!userAssignedFacilities.contains(targetFacilityId)) {
            return PolicyDecision.denied("FACILITY_ACCESS_DENIED",
                    "User is not assigned to facility " + targetFacilityId +
                    ". Goods receipt requires explicit facility assignment.");
        }

        return PolicyDecision.allowed();
    }

    /**
     * Evaluates whether a user may read Goods Receipt documents at the specified facility.
     *
     * @param userRoles              the performing user's current role set
     * @param targetFacilityId       UUID of the facility to read GR records from
     * @param userAssignedFacilities set of facility UUIDs the user is assigned to
     * @return {@link PolicyDecision#allowed()} if permitted, otherwise denied
     */
    public PolicyDecision canReadFacilityReceipts(
            Set<RoleType> userRoles,
            UUID targetFacilityId,
            Set<UUID> userAssignedFacilities
    ) {
        // AP_SPECIALIST and ROOT_USER may read all GR records across facilities
        if (registry.hasPermission(userRoles, Permission.GR_READ)
                && (userRoles.contains(RoleType.ROOT_USER) || userRoles.contains(RoleType.AP_SPECIALIST))) {
            return PolicyDecision.allowed();
        }

        // FACILITY_USER and PROCUREMENT: must be assigned to the target facility
        if (!registry.hasPermission(userRoles, Permission.GR_READ)) {
            return PolicyDecision.denied("INSUFFICIENT_PERMISSION",
                    "User does not hold the GR_READ permission.");
        }

        if (!userAssignedFacilities.contains(targetFacilityId)) {
            return PolicyDecision.denied("FACILITY_ACCESS_DENIED",
                    "User is not assigned to facility " + targetFacilityId + ".");
        }

        return PolicyDecision.allowed();
    }
}
