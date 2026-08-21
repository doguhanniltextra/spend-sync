package com.enterprise.spendsync.requisition.service;

import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.requisition.internal.domain.ApprovalStepStatus;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionApprovalStep;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.requisition.internal.dto.ApproveRequisitionStepRequest;
import com.enterprise.spendsync.requisition.internal.dto.RejectRequisitionRequest;
import com.enterprise.spendsync.requisition.internal.repository.PurchaseRequisitionRepository;
import com.enterprise.spendsync.requisition.internal.repository.RequisitionApprovalStepRepository;
import com.enterprise.spendsync.requisition.internal.service.ApprovalLimitService;
import com.enterprise.spendsync.requisition.internal.service.RequisitionServiceImpl;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.security.ApprovalSecurityPolicy;
import com.enterprise.spendsync.shared.security.PolicyDecision;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequisitionApprovalNegativeBranchTest {

    @Mock
    private PurchaseRequisitionRepository requisitionRepository;

    @Mock
    private RequisitionApprovalStepRepository approvalStepRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApprovalLimitService approvalLimitService;

    @Mock
    private ApprovalSecurityPolicy approvalSecurityPolicy;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RequisitionServiceImpl requisitionService;

    private UUID tenantId;
    private UUID currentUserId;
    private Tenant tenant;
    private User currentUser;
    private User requesterUser;
    private PurchaseRequisition pr;
    private LegalEntity le;
    private CostCenter cc;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        currentUserId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        UserPrincipal principal = new UserPrincipal(
                currentUserId, tenantId, "approver@spendsync.com", "pass", "Approver User",
                true, Set.of(RoleType.APPROVER), Collections.emptySet()
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        tenant = new Tenant("Test Tenant", "test-tenant");
        le = new LegalEntity(tenant, "LE", "LE-01", "1234567890", "TRY", "Address", "TR");
        cc = new CostCenter(tenant, le, "CC-01", "Finance");

        currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setEmail("approver@spendsync.com");
        currentUser.setTenant(tenant);
        currentUser.setRoles(Set.of(RoleType.APPROVER));

        requesterUser = new User();
        requesterUser.setId(UUID.randomUUID());
        requesterUser.setEmail("requester@spendsync.com");
        requesterUser.setTenant(tenant);
        requesterUser.setRoles(Set.of(RoleType.REQUISITIONER));

        pr = new PurchaseRequisition(
                tenant, "PR-2026-0001", requesterUser, le, cc, null, null,
                RequisitionStatus.DRAFT, new BigDecimal("5000.00"), "TRY", "Test PR", "Justification"
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should throw when approving PR not in PENDING_APPROVAL status (e.g. DRAFT)")
    void shouldThrowWhenApprovingNonPendingPr() {
        UUID prId = UUID.randomUUID();
        pr.setStatus(RequisitionStatus.DRAFT);

        when(userRepository.findByIdAndTenantId(eq(currentUserId), eq(tenantId)))
                .thenReturn(Optional.of(currentUser));
        when(requisitionRepository.findByIdAndTenantId(eq(prId), eq(tenantId)))
                .thenReturn(Optional.of(pr));

        assertThatThrownBy(() -> requisitionService.approveStep(prId, new ApproveRequisitionStepRequest("Approved")))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Only requisitions in PENDING_APPROVAL status can be approved");
    }

    @Test
    @DisplayName("Should throw when approver is not the designated approver for active step")
    void shouldThrowWhenNotDesignatedApprover() {
        UUID prId = UUID.randomUUID();
        pr.setStatus(RequisitionStatus.PENDING_APPROVAL);

        User otherApprover = new User();
        otherApprover.setId(UUID.randomUUID()); // Different user ID

        RequisitionApprovalStep step = new RequisitionApprovalStep(pr, tenant, 1, otherApprover, 1, ApprovalStepStatus.PENDING);

        when(userRepository.findByIdAndTenantId(eq(currentUserId), eq(tenantId)))
                .thenReturn(Optional.of(currentUser));
        when(requisitionRepository.findByIdAndTenantId(eq(prId), eq(tenantId)))
                .thenReturn(Optional.of(pr));
        when(approvalStepRepository.findAllByRequisitionIdOrderByStepOrderAsc(eq(prId)))
                .thenReturn(List.of(step));

        assertThatThrownBy(() -> requisitionService.approveStep(prId, new ApproveRequisitionStepRequest("Approved")))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("You are not the designated approver for step");
    }

    @Test
    @DisplayName("Should throw SoD violation when requester tries to self-approve their own PR")
    void shouldThrowWhenSodSelfApprovalDetected() {
        UUID prId = UUID.randomUUID();
        pr.setStatus(RequisitionStatus.PENDING_APPROVAL);

        RequisitionApprovalStep step = new RequisitionApprovalStep(pr, tenant, 1, currentUser, 1, ApprovalStepStatus.PENDING);

        when(userRepository.findByIdAndTenantId(eq(currentUserId), eq(tenantId)))
                .thenReturn(Optional.of(currentUser));
        when(requisitionRepository.findByIdAndTenantId(eq(prId), eq(tenantId)))
                .thenReturn(Optional.of(pr));
        when(approvalStepRepository.findAllByRequisitionIdOrderByStepOrderAsc(eq(prId)))
                .thenReturn(List.of(step));
        when(approvalSecurityPolicy.canApproveRequisition(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(PolicyDecision.denied("SOD_VIOLATION_SELF_APPROVAL", "Self-approval is strictly forbidden"));

        assertThatThrownBy(() -> requisitionService.approveStep(prId, new ApproveRequisitionStepRequest("Approved")))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Self-approval is strictly forbidden");
    }

    @Test
    @DisplayName("Should throw when rejecting a PR that is not in PENDING_APPROVAL status")
    void shouldThrowWhenRejectingNonPendingPr() {
        UUID prId = UUID.randomUUID();
        pr.setStatus(RequisitionStatus.APPROVED);

        when(userRepository.findByIdAndTenantId(eq(currentUserId), eq(tenantId)))
                .thenReturn(Optional.of(currentUser));
        when(requisitionRepository.findByIdAndTenantId(eq(prId), eq(tenantId)))
                .thenReturn(Optional.of(pr));

        assertThatThrownBy(() -> requisitionService.rejectRequisition(prId, new RejectRequisitionRequest("Rejected")))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Only requisitions in PENDING_APPROVAL status can be rejected");
    }

    @Test
    @DisplayName("Should throw when a user other than the original requisitioner attempts to cancel the PR")
    void shouldThrowWhenNonRequisitionerCancels() {
        UUID prId = UUID.randomUUID();
        pr.setStatus(RequisitionStatus.PENDING_APPROVAL);

        when(userRepository.findByIdAndTenantId(eq(currentUserId), eq(tenantId)))
                .thenReturn(Optional.of(currentUser)); // currentUser != requesterUser
        when(requisitionRepository.findByIdAndTenantId(eq(prId), eq(tenantId)))
                .thenReturn(Optional.of(pr));

        assertThatThrownBy(() -> requisitionService.cancelRequisition(prId))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Only the original requisitioner can cancel this requisition");
    }

    @Test
    @DisplayName("Should throw when attempting to cancel an already APPROVED requisition")
    void shouldThrowWhenCancellingApprovedPr() {
        UUID prId = UUID.randomUUID();
        PurchaseRequisition approvedPr = new PurchaseRequisition(
                tenant, "PR-2026-0002", currentUser, le, cc, null, null,
                RequisitionStatus.APPROVED, new BigDecimal("5000.00"), "TRY", "Test PR", "Justification"
        );

        when(userRepository.findByIdAndTenantId(eq(currentUserId), eq(tenantId)))
                .thenReturn(Optional.of(currentUser));
        when(requisitionRepository.findByIdAndTenantId(eq(prId), eq(tenantId)))
                .thenReturn(Optional.of(approvedPr));

        assertThatThrownBy(() -> requisitionService.cancelRequisition(prId))
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("Only DRAFT or PENDING_APPROVAL requisitions can be cancelled");
    }
}
