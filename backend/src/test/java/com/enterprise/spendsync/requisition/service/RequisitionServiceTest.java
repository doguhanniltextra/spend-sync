package com.enterprise.spendsync.requisition.service;

import com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode;
import com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.budget.internal.service.BudgetService;
import com.enterprise.spendsync.core.internal.domain.*;
import com.enterprise.spendsync.core.internal.repository.CostCenterRepository;
import com.enterprise.spendsync.core.internal.repository.FacilityRepository;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.requisition.internal.domain.*;
import com.enterprise.spendsync.requisition.internal.dto.*;
import com.enterprise.spendsync.requisition.internal.event.RequisitionApprovedEvent;
import com.enterprise.spendsync.requisition.internal.event.RequisitionRejectedEvent;
import com.enterprise.spendsync.requisition.internal.repository.ApprovalAuthorityLimitRepository;
import com.enterprise.spendsync.requisition.internal.repository.PurchaseRequisitionRepository;
import com.enterprise.spendsync.requisition.internal.repository.RequisitionApprovalStepRepository;
import com.enterprise.spendsync.requisition.internal.service.ApprovalLimitService;
import com.enterprise.spendsync.requisition.internal.service.RequisitionServiceImpl;
import com.enterprise.spendsync.shared.domain.CrossAssignmentDetector;
import com.enterprise.spendsync.shared.domain.CrossAssignmentWarning;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequisitionService Unit & Mock Tests (PR Creation, Approval Chain & Events)")
class RequisitionServiceTest {

    @Mock
    private PurchaseRequisitionRepository requisitionRepository;

    @Mock
    private RequisitionApprovalStepRepository approvalStepRepository;

    @Mock
    private ApprovalAuthorityLimitRepository limitRepository;

    @Mock
    private BudgetPoolRepository budgetPoolRepository;

    @Mock
    private BudgetService budgetService;

    @Mock
    private ApprovalLimitService approvalLimitService;

    @Mock
    private ApprovalSecurityPolicy approvalSecurityPolicy;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LegalEntityRepository legalEntityRepository;

    @Mock
    private CostCenterRepository costCenterRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CrossAssignmentDetector crossAssignmentDetector;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RequisitionServiceImpl requisitionService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;
    private Facility facility;
    private User requisitioner;
    private User manager;
    private User director;
    private BudgetPool budgetPool;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-ENG", "Engineering");
        costCenter.setId(UUID.randomUUID());

        facility = new Facility(tenant, legalEntity, "Main Warehouse", "WH-01", FacilityType.WAREHOUSE, "Gebze OSB");
        facility.setId(UUID.randomUUID());

        requisitioner = new User("user@spendsync.com", "pass", "Ali", "Demir", null, "TR");
        requisitioner.setId(UUID.randomUUID());
        requisitioner.setRoles(Set.of(RoleType.REQUISITIONER));

        manager = new User("manager@spendsync.com", "pass", "Jane", "Doe", null, "TR");
        manager.setId(UUID.randomUUID());
        manager.setRoles(Set.of(RoleType.APPROVER));

        director = new User("director@spendsync.com", "pass", "John", "Director", null, "TR");
        director.setId(UUID.randomUUID());
        director.setRoles(Set.of(RoleType.APPROVER));

        budgetPool = new BudgetPool(tenant, legalEntity, costCenter, 2026, BudgetPeriodType.ANNUAL, "ANNUAL",
                BudgetStatus.ACTIVE, BudgetEnforcementMode.HARD_STOP, BigDecimal.ZERO, new BigDecimal("1000000.00"), "TRY");
        budgetPool.setId(UUID.randomUUID());

        // Set authenticated user principal in SecurityContext
        UserPrincipal principal = new UserPrincipal(
                requisitioner.getId(), tenantId, null, "USER", requisitioner.getEmail(), null, "Ali Demir", true,
                requisitioner.getRoles(), Set.of()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token", Set.of()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create and submit PR, build approval chain, and reserve budget")
    void shouldCreateAndSubmitRequisitionSuccessfully() {
        CreateLineItemRequest itemReq = new CreateLineItemRequest(
                "Dell XPS 15", "HARDWARE", new BigDecimal("2.0"), "PCS", new BigDecimal("40000.00"), LocalDate.now().plusDays(7)
        );

        CreateRequisitionRequest request = new CreateRequisitionRequest(
                legalEntity.getId(),
                costCenter.getId(),
                facility.getId(),
                "New Developer Laptops",
                "Hardware refresh for incoming engineers",
                "TRY",
                List.of(itemReq)
        );

        // Limit configuration: Manager level 1 limit up to 100k
        ApprovalAuthorityLimit managerLimit = new ApprovalAuthorityLimit(
                tenant, manager, legalEntity, costCenter, 1, BigDecimal.ZERO, new BigDecimal("100000.00"), "TRY", true
        );
        managerLimit.setId(UUID.randomUUID());

        when(userRepository.findByIdAndTenantId(requisitioner.getId(), tenantId)).thenReturn(Optional.of(requisitioner));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.findByIdAndTenantId(costCenter.getId(), tenantId)).thenReturn(Optional.of(costCenter));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(requisitionRepository.countByTenantId(tenantId)).thenReturn(0L);

        when(budgetPoolRepository.findByCostCenterIdAndLegalEntityIdAndStatusAndTenantId(
                costCenter.getId(), legalEntity.getId(), BudgetStatus.ACTIVE, tenantId
        )).thenReturn(Optional.of(budgetPool));

        when(limitRepository.findAllByTenantIdAndLegalEntityId(tenantId, legalEntity.getId()))
                .thenReturn(List.of(managerLimit));

        when(approvalLimitService.getEffectiveLimitDetails(manager.getId(), legalEntity.getId(), costCenter.getId()))
                .thenReturn(Optional.of(managerLimit));

        when(crossAssignmentDetector.detect(legalEntity, facility)).thenReturn(CrossAssignmentWarning.none());

        when(requisitionRepository.save(any(PurchaseRequisition.class))).thenAnswer(i -> {
            PurchaseRequisition pr = i.getArgument(0);
            pr.setId(UUID.randomUUID());
            return pr;
        });

        RequisitionDetailResponse response = requisitionService.createAndSubmitRequisition(request);

        assertThat(response).isNotNull();
        assertThat(response.requisitionNumber()).isEqualTo("PR-2026-00001");
        assertThat(response.status()).isEqualTo(RequisitionStatus.PENDING_APPROVAL);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("80000.00"));
        assertThat(response.approvalSteps()).hasSize(1);
        assertThat(response.approvalSteps().get(0).approverName()).isEqualTo("Jane Doe");

        verify(budgetService).reserveBudget(eq(budgetPool.getId()), argThat(amt -> amt != null && amt.compareTo(new BigDecimal("80000.00")) == 0), any(), eq("PURCHASE_REQUISITION"), any());
    }

    @Test
    @DisplayName("Should generate multi-tier approval chain when PR total exceeds manager ceiling")
    void shouldGenerateMultiTierApprovalChainWhenExceedingManagerCeiling() {
        // Total amount = 150,000 TL
        CreateLineItemRequest itemReq = new CreateLineItemRequest(
                "High-End Server", "HARDWARE", new BigDecimal("1.0"), "PCS", new BigDecimal("150000.00"), LocalDate.now().plusDays(7)
        );

        CreateRequisitionRequest request = new CreateRequisitionRequest(
                legalEntity.getId(),
                costCenter.getId(),
                facility.getId(),
                "Server Procurement",
                "New rack server",
                "TRY",
                List.of(itemReq)
        );

        // Tier 1 Manager Limit: 50,000 TL (Insufficient for 150k)
        ApprovalAuthorityLimit managerLimit = new ApprovalAuthorityLimit(
                tenant, manager, legalEntity, costCenter, 1, BigDecimal.ZERO, new BigDecimal("50000.00"), "TRY", true
        );
        managerLimit.setId(UUID.randomUUID());

        // Tier 2 Director Limit: 200,000 TL (Sufficient for 150k)
        ApprovalAuthorityLimit directorLimit = new ApprovalAuthorityLimit(
                tenant, director, legalEntity, costCenter, 2, BigDecimal.ZERO, new BigDecimal("200000.00"), "TRY", true
        );
        directorLimit.setId(UUID.randomUUID());

        when(userRepository.findByIdAndTenantId(requisitioner.getId(), tenantId)).thenReturn(Optional.of(requisitioner));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(legalEntityRepository.findByIdAndTenantId(legalEntity.getId(), tenantId)).thenReturn(Optional.of(legalEntity));
        when(costCenterRepository.findByIdAndTenantId(costCenter.getId(), tenantId)).thenReturn(Optional.of(costCenter));
        when(facilityRepository.findByIdAndTenantId(facility.getId(), tenantId)).thenReturn(Optional.of(facility));
        when(requisitionRepository.countByTenantId(tenantId)).thenReturn(1L);

        when(budgetPoolRepository.findByCostCenterIdAndLegalEntityIdAndStatusAndTenantId(
                costCenter.getId(), legalEntity.getId(), BudgetStatus.ACTIVE, tenantId
        )).thenReturn(Optional.of(budgetPool));

        when(limitRepository.findAllByTenantIdAndLegalEntityId(tenantId, legalEntity.getId()))
                .thenReturn(List.of(managerLimit, directorLimit));

        when(approvalLimitService.getEffectiveLimitDetails(manager.getId(), legalEntity.getId(), costCenter.getId()))
                .thenReturn(Optional.of(managerLimit));
        when(approvalLimitService.getEffectiveLimitDetails(director.getId(), legalEntity.getId(), costCenter.getId()))
                .thenReturn(Optional.of(directorLimit));

        when(crossAssignmentDetector.detect(legalEntity, facility)).thenReturn(CrossAssignmentWarning.none());

        when(requisitionRepository.save(any(PurchaseRequisition.class))).thenAnswer(i -> {
            PurchaseRequisition pr = i.getArgument(0);
            pr.setId(UUID.randomUUID());
            return pr;
        });

        RequisitionDetailResponse response = requisitionService.createAndSubmitRequisition(request);

        assertThat(response).isNotNull();
        // Approval chain must have 2 steps: Step 1 (Manager - PENDING), Step 2 (Director - WAITING)
        assertThat(response.approvalSteps()).hasSize(2);
        assertThat(response.approvalSteps().get(0).approverName()).isEqualTo("Jane Doe");
        assertThat(response.approvalSteps().get(0).status()).isEqualTo(ApprovalStepStatus.PENDING);
        assertThat(response.approvalSteps().get(1).approverName()).isEqualTo("John Director");
        assertThat(response.approvalSteps().get(1).status()).isEqualTo(ApprovalStepStatus.WAITING);
    }

    @Test
    @DisplayName("Should approve final step, update status to APPROVED and publish RequisitionApprovedEvent")
    void shouldApproveFinalStepSuccessfully() {
        UUID prId = UUID.randomUUID();
        PurchaseRequisition pr = new PurchaseRequisition(
                tenant, "PR-2026-00003", requisitioner, legalEntity, costCenter, facility,
                budgetPool, RequisitionStatus.PENDING_APPROVAL, new BigDecimal("40000.00"), "TRY", "Title", "Justification"
        );
        pr.setId(prId);

        RequisitionApprovalStep step1 = new RequisitionApprovalStep(pr, tenant, 1, manager, 1, ApprovalStepStatus.PENDING);
        step1.setId(UUID.randomUUID());

        // Authenticate as manager
        UserPrincipal managerPrincipal = new UserPrincipal(
                manager.getId(), tenantId, null, "USER", manager.getEmail(), null, "Jane Doe", true, manager.getRoles(), Set.of()
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(managerPrincipal, "token", Set.of()));

        when(userRepository.findByIdAndTenantId(manager.getId(), tenantId)).thenReturn(Optional.of(manager));
        when(requisitionRepository.findByIdAndTenantId(prId, tenantId)).thenReturn(Optional.of(pr));
        when(approvalStepRepository.findAllByRequisitionIdOrderByStepOrderAsc(prId)).thenReturn(List.of(step1));
        when(approvalLimitService.getEffectiveLimitDetails(manager.getId(), legalEntity.getId(), costCenter.getId()))
                .thenReturn(Optional.of(new ApprovalAuthorityLimit(tenant, manager, legalEntity, costCenter, 1, BigDecimal.ZERO, new BigDecimal("50000.00"), "TRY", true)));

        when(approvalSecurityPolicy.canApproveRequisition(
                eq(manager.getId()), eq(requisitioner.getId()), eq(manager.getRoles()), eq(new BigDecimal("40000.00")), eq(new BigDecimal("50000.00")), eq(true)
        )).thenReturn(PolicyDecision.allowed());

        when(crossAssignmentDetector.detect(legalEntity, facility)).thenReturn(CrossAssignmentWarning.none());

        RequisitionDetailResponse response = requisitionService.approveStep(prId, new ApproveRequisitionStepRequest("Approved for budget"));

        assertThat(response).isNotNull();
        assertThat(pr.getStatus()).isEqualTo(RequisitionStatus.APPROVED);
        assertThat(step1.getStatus()).isEqualTo(ApprovalStepStatus.APPROVED);
        assertThat(pr.getApprovedAt()).isNotNull();

        verify(eventPublisher).publishEvent(any(RequisitionApprovedEvent.class));
    }

    @Test
    @DisplayName("Should reject approval attempt by unauthorized user (not designated for active step)")
    void shouldRejectApprovalByUnauthorizedUser() {
        UUID prId = UUID.randomUUID();
        PurchaseRequisition pr = new PurchaseRequisition(
                tenant, "PR-2026-00004", requisitioner, legalEntity, costCenter, facility,
                budgetPool, RequisitionStatus.PENDING_APPROVAL, new BigDecimal("40000.00"), "TRY", "Title", "Justification"
        );
        pr.setId(prId);

        RequisitionApprovalStep step1 = new RequisitionApprovalStep(pr, tenant, 1, manager, 1, ApprovalStepStatus.PENDING);
        step1.setId(UUID.randomUUID());

        // Authenticate as director (who is not the designated approver for step 1)
        UserPrincipal directorPrincipal = new UserPrincipal(
                director.getId(), tenantId, null, "USER", director.getEmail(), null, "John Director", true, director.getRoles(), Set.of()
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(directorPrincipal, "token", Set.of()));

        when(userRepository.findByIdAndTenantId(director.getId(), tenantId)).thenReturn(Optional.of(director));
        when(requisitionRepository.findByIdAndTenantId(prId, tenantId)).thenReturn(Optional.of(pr));
        when(approvalStepRepository.findAllByRequisitionIdOrderByStepOrderAsc(prId)).thenReturn(List.of(step1));

        assertThatThrownBy(() -> requisitionService.approveStep(prId, new ApproveRequisitionStepRequest("Attempting out-of-turn approval")))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("NOT_CURRENT_APPROVER");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("Should reject PR, release reserved budget, and publish RequisitionRejectedEvent")
    void shouldRejectRequisitionSuccessfully() {
        UUID prId = UUID.randomUUID();
        PurchaseRequisition pr = new PurchaseRequisition(
                tenant, "PR-2026-00005", requisitioner, legalEntity, costCenter, facility,
                budgetPool, RequisitionStatus.PENDING_APPROVAL, new BigDecimal("60000.00"), "TRY", "Title", "Justification"
        );
        pr.setId(prId);

        RequisitionApprovalStep step1 = new RequisitionApprovalStep(pr, tenant, 1, manager, 1, ApprovalStepStatus.PENDING);
        step1.setId(UUID.randomUUID());

        // Authenticate as manager
        UserPrincipal managerPrincipal = new UserPrincipal(
                manager.getId(), tenantId, null, "USER", manager.getEmail(), null, "Jane Doe", true, manager.getRoles(), Set.of()
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(managerPrincipal, "token", Set.of()));

        when(userRepository.findByIdAndTenantId(manager.getId(), tenantId)).thenReturn(Optional.of(manager));
        when(requisitionRepository.findByIdAndTenantId(prId, tenantId)).thenReturn(Optional.of(pr));
        when(approvalSecurityPolicy.canRejectRequisition(manager.getRoles())).thenReturn(PolicyDecision.allowed());
        when(approvalStepRepository.findAllByRequisitionIdOrderByStepOrderAsc(prId)).thenReturn(List.of(step1));
        when(crossAssignmentDetector.detect(legalEntity, facility)).thenReturn(CrossAssignmentWarning.none());

        RequisitionDetailResponse response = requisitionService.rejectRequisition(prId, new RejectRequisitionRequest("Budget constraints"));

        assertThat(response).isNotNull();
        assertThat(pr.getStatus()).isEqualTo(RequisitionStatus.REJECTED);
        assertThat(pr.getRejectionReason()).isEqualTo("Budget constraints");
        assertThat(step1.getStatus()).isEqualTo(ApprovalStepStatus.REJECTED);

        verify(budgetService).releaseBudget(eq(budgetPool.getId()), eq(new BigDecimal("60000.00")), eq(prId), eq("PURCHASE_REQUISITION"), any());
        verify(eventPublisher).publishEvent(any(RequisitionRejectedEvent.class));
    }

    @Test
    @DisplayName("Should allow requisitioner to cancel PR and release reserved budget")
    void shouldCancelRequisitionSuccessfully() {
        UUID prId = UUID.randomUUID();
        PurchaseRequisition pr = new PurchaseRequisition(
                tenant, "PR-2026-00006", requisitioner, legalEntity, costCenter, facility,
                budgetPool, RequisitionStatus.PENDING_APPROVAL, new BigDecimal("20000.00"), "TRY", "Title", "Justification"
        );
        pr.setId(prId);

        RequisitionApprovalStep step1 = new RequisitionApprovalStep(pr, tenant, 1, manager, 1, ApprovalStepStatus.PENDING);
        step1.setId(UUID.randomUUID());

        when(userRepository.findByIdAndTenantId(requisitioner.getId(), tenantId)).thenReturn(Optional.of(requisitioner));
        when(requisitionRepository.findByIdAndTenantId(prId, tenantId)).thenReturn(Optional.of(pr));
        when(approvalStepRepository.findAllByRequisitionIdOrderByStepOrderAsc(prId)).thenReturn(List.of(step1));
        when(crossAssignmentDetector.detect(legalEntity, facility)).thenReturn(CrossAssignmentWarning.none());

        RequisitionDetailResponse response = requisitionService.cancelRequisition(prId);

        assertThat(response).isNotNull();
        assertThat(pr.getStatus()).isEqualTo(RequisitionStatus.CANCELLED);
        assertThat(step1.getStatus()).isEqualTo(ApprovalStepStatus.SKIPPED);

        verify(budgetService).releaseBudget(eq(budgetPool.getId()), eq(new BigDecimal("20000.00")), eq(prId), eq("PURCHASE_REQUISITION"), any());
    }

    @Test
    @DisplayName("Should reject cancellation attempt by non-requisitioner user")
    void shouldRejectCancellationByNonRequisitioner() {
        UUID prId = UUID.randomUUID();
        PurchaseRequisition pr = new PurchaseRequisition(
                tenant, "PR-2026-00007", requisitioner, legalEntity, costCenter, facility,
                budgetPool, RequisitionStatus.PENDING_APPROVAL, new BigDecimal("20000.00"), "TRY", "Title", "Justification"
        );
        pr.setId(prId);

        // Authenticate as another user
        User otherUser = new User("other@spendsync.com", "pass", "Other", "User", null, "TR");
        otherUser.setId(UUID.randomUUID());
        UserPrincipal otherPrincipal = new UserPrincipal(
                otherUser.getId(), tenantId, null, "USER", otherUser.getEmail(), null, "Other User", true, Set.of(RoleType.PROCUREMENT), Set.of()
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(otherPrincipal, "token", Set.of()));

        when(userRepository.findByIdAndTenantId(otherUser.getId(), tenantId)).thenReturn(Optional.of(otherUser));
        when(requisitionRepository.findByIdAndTenantId(prId, tenantId)).thenReturn(Optional.of(pr));

        assertThatThrownBy(() -> requisitionService.cancelRequisition(prId))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException se = (SpendSyncException) ex;
                    assertThat(se.getErrorCode()).isEqualTo("UNAUTHORIZED_CANCELLATION");
                    assertThat(se.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }
}
