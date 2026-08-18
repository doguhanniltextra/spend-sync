package com.enterprise.spendsync.shared.security;

import com.enterprise.spendsync.core.internal.domain.Permission;
import com.enterprise.spendsync.core.internal.domain.RolePermissionRegistry;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Domain security policy for Purchase Requisition (PR) approval operations.
 *
 * <p>Enforces two financial control layers mandated by ISO 37001 and SOX §302:</p>
 * <ol>
 *   <li><strong>Segregation of Duties (SoD)</strong> — A user cannot approve a PR
 *       they themselves submitted. ({@code requisitionerId != approverId})</li>
 *   <li><strong>Signature Threshold</strong> — An approver's authority is bounded
 *       by their configured monetary limit. PRs exceeding the limit must be escalated.</li>
 * </ol>
 *
 * <p>This class contains only <em>stateless policy logic</em>. Persistence lookups
 * (e.g. resolving the approver's limit from {@code approval_authority_limits}) must
 * be injected from the calling service layer (requisition module).</p>
 */
@Component
public class ApprovalSecurityPolicy {

    private final RolePermissionRegistry registry;

    public ApprovalSecurityPolicy(RolePermissionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Checks whether the specified user may approve a given PR step.
     *
     * @param approverId         UUID of the user attempting to approve
     * @param requisitionerId    UUID of the user who originally submitted the PR
     * @param approverRoles      the approver's current role set
     * @param prAmount           the monetary value of the PR
     * @param approverMaxLimit   the maximum amount the approver is authorized to sign off
     *                           ({@code null} means unlimited authority — e.g. CFO level)
     * @param isFinalStep        true if this is the final approving step required for PR to become APPROVED
     * @return a {@link PolicyDecision} describing the outcome and, if denied, the reason
     */
    public PolicyDecision canApproveRequisition(
            UUID approverId,
            UUID requisitionerId,
            Set<RoleType> approverRoles,
            BigDecimal prAmount,
            BigDecimal approverMaxLimit,
            boolean isFinalStep
    ) {
        // 1. Role capability check — must hold PR_APPROVE permission
        if (!registry.hasPermission(approverRoles, Permission.PR_APPROVE)) {
            return PolicyDecision.denied("INSUFFICIENT_PERMISSION",
                    "User does not hold the PR_APPROVE permission.");
        }

        // 2. SoD check — self-approval is strictly forbidden (ISO 37001 §A.8.2)
        if (approverId.equals(requisitionerId)) {
            return PolicyDecision.denied("SOD_VIOLATION_SELF_APPROVAL",
                    "An approver cannot approve a requisition they submitted themselves.");
        }

        // 3. Signature threshold check for final step — null means unlimited authority (e.g. CFO/YK level)
        if (isFinalStep && approverMaxLimit != null && prAmount.compareTo(approverMaxLimit) > 0) {
            return PolicyDecision.denied("SIGNATURE_THRESHOLD_EXCEEDED",
                    String.format("PR amount %s exceeds the final approver's authorized limit of %s. " +
                                  "Escalation to a higher authority is required.",
                            prAmount, approverMaxLimit));
        }

        return PolicyDecision.allowed();
    }

    /**
     * Checks whether the specified user may reject a given PR.
     *
     * @param rejectorRoles the rejecting user's role set
     * @return a {@link PolicyDecision} describing the outcome
     */
    public PolicyDecision canRejectRequisition(Set<RoleType> rejectorRoles) {
        if (!registry.hasPermission(rejectorRoles, Permission.PR_REJECT)) {
            return PolicyDecision.denied("INSUFFICIENT_PERMISSION",
                    "User does not hold the PR_REJECT permission.");
        }
        return PolicyDecision.allowed();
    }
}
