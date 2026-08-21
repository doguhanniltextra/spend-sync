package com.enterprise.spendsync.shared.security;

import com.enterprise.spendsync.core.internal.domain.RolePermissionRegistry;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApprovalSecurityPolicy Pure Unit Tests (SoD & Threshold Enforcements)")
class ApprovalSecurityPolicyTest {

    private ApprovalSecurityPolicy policy;

    @BeforeEach
    void setUp() {
        RolePermissionRegistry registry = new RolePermissionRegistry();
        policy = new ApprovalSecurityPolicy(registry);
    }

    @Test
    @DisplayName("Should prohibit Self-Approval (Segregation of Duties - SoD)")
    void shouldProhibitSelfApproval() {
        UUID userId = UUID.randomUUID();

        PolicyDecision decision = policy.canApproveRequisition(
                userId,                  // approverId
                userId,                  // requisitionerId (same user!)
                Set.of(RoleType.APPROVER),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(50000),
                true
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getErrorCode()).isEqualTo("SOD_VIOLATION_SELF_APPROVAL");
        assertThat(decision.getReason()).contains("cannot approve a requisition they submitted themselves");
    }

    @Test
    @DisplayName("Should deny approval if user lacks PR_APPROVE permission")
    void shouldDenyIfLacksPermission() {
        UUID approverId = UUID.randomUUID();
        UUID requisitionerId = UUID.randomUUID();

        PolicyDecision decision = policy.canApproveRequisition(
                approverId,
                requisitionerId,
                Set.of(RoleType.PROCUREMENT), // PROCUREMENT does NOT have PR_APPROVE
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(50000),
                true
        );

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getErrorCode()).isEqualTo("INSUFFICIENT_PERMISSION");
    }

    @ParameterizedTest(name = "PR Amount: {0}, Approver Limit: {1} -> Allowed: {2}")
    @CsvSource({
            "5000.00,  10000.00, true",   // Within limit
            "10000.00, 10000.00, true",   // Exactly at limit
            "10000.01, 10000.00, false",  // Exceeds limit by 1 cent
            "75000.00, 50000.00, false"   // Exceeds limit
    })
    @DisplayName("Should enforce monetary signature threshold limits")
    void shouldEnforceMonetaryLimits(BigDecimal prAmount, BigDecimal approverMaxLimit, boolean expectedAllowed) {
        UUID approverId = UUID.randomUUID();
        UUID requisitionerId = UUID.randomUUID();

        PolicyDecision decision = policy.canApproveRequisition(
                approverId,
                requisitionerId,
                Set.of(RoleType.APPROVER),
                prAmount,
                approverMaxLimit,
                true
        );

        assertThat(decision.isAllowed()).isEqualTo(expectedAllowed);
        if (!expectedAllowed) {
            assertThat(decision.getErrorCode()).isEqualTo("SIGNATURE_THRESHOLD_EXCEEDED");
        }
    }

    @Test
    @DisplayName("Should allow approval when approverMaxLimit is null (Unlimited CFO Authority)")
    void shouldAllowUnlimitedCfoAuthority() {
        UUID approverId = UUID.randomUUID();
        UUID requisitionerId = UUID.randomUUID();

        PolicyDecision decision = policy.canApproveRequisition(
                approverId,
                requisitionerId,
                Set.of(RoleType.APPROVER),
                BigDecimal.valueOf(10_000_000), // 10 Million TL
                null,                           // null = unlimited authority
                true
        );

        assertThat(decision.isAllowed()).isTrue();
    }
}
