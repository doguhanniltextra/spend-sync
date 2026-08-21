package com.enterprise.spendsync.shared.security;

import com.enterprise.spendsync.core.internal.domain.RolePermissionRegistry;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FacilitySecurityPolicy Pure Unit Tests (Dock Receiving Access Control)")
class FacilitySecurityPolicyTest {

    private FacilitySecurityPolicy policy;

    @BeforeEach
    void setUp() {
        RolePermissionRegistry registry = new RolePermissionRegistry();
        policy = new FacilitySecurityPolicy(registry);
    }

    @Test
    @DisplayName("Should allow Goods Receipt creation when user has GR_CREATE and is assigned to facility")
    void shouldAllowWhenAssignedAndPermitted() {
        UUID facilityId = UUID.randomUUID();

        PolicyDecision decision = policy.canReceiveAtFacility(
                Set.of(RoleType.FACILITY_USER),
                facilityId,
                Set.of(facilityId, UUID.randomUUID()) // assigned facilities
        );

        assertThat(decision.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("Should deny when user is not assigned to target facility")
    void shouldDenyWhenNotAssignedToFacility() {
        UUID targetFacilityId = UUID.randomUUID();
        UUID otherFacilityId = UUID.randomUUID();

        PolicyDecision decision = policy.canReceiveAtFacility(
                Set.of(RoleType.FACILITY_USER),
                targetFacilityId,
                Set.of(otherFacilityId) // user is only assigned to other facility
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getErrorCode()).isEqualTo("FACILITY_ACCESS_DENIED");
    }

    @Test
    @DisplayName("Should deny when user lacks GR_CREATE permission even if assigned")
    void shouldDenyWhenLacksGrCreatePermission() {
        UUID targetFacilityId = UUID.randomUUID();

        PolicyDecision decision = policy.canReceiveAtFacility(
                Set.of(RoleType.REQUISITIONER), // does NOT hold GR_CREATE
                targetFacilityId,
                Set.of(targetFacilityId)
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getErrorCode()).isEqualTo("INSUFFICIENT_PERMISSION");
    }
}
