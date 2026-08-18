package com.enterprise.spendsync.requisition.internal.service;

import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.domain.BudgetStatus;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.budget.internal.service.BudgetService;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.CostCenterRepository;
import com.enterprise.spendsync.core.internal.repository.FacilityRepository;
import com.enterprise.spendsync.core.internal.repository.LegalEntityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.requisition.internal.domain.ApprovalAuthorityLimit;
import com.enterprise.spendsync.requisition.internal.domain.ApprovalStepStatus;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionApprovalStep;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionLineItem;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.requisition.internal.dto.ApproveRequisitionStepRequest;
import com.enterprise.spendsync.requisition.internal.dto.CreateLineItemRequest;
import com.enterprise.spendsync.requisition.internal.dto.CreateRequisitionRequest;
import com.enterprise.spendsync.requisition.internal.dto.RejectRequisitionRequest;
import com.enterprise.spendsync.requisition.internal.dto.RequisitionDetailResponse;
import com.enterprise.spendsync.requisition.internal.dto.RequisitionSummaryResponse;
import com.enterprise.spendsync.requisition.internal.event.LineItemEventPayload;
import com.enterprise.spendsync.requisition.internal.event.RequisitionApprovedEvent;
import com.enterprise.spendsync.requisition.internal.event.RequisitionRejectedEvent;
import com.enterprise.spendsync.requisition.internal.repository.ApprovalAuthorityLimitRepository;
import com.enterprise.spendsync.requisition.internal.repository.PurchaseRequisitionRepository;
import com.enterprise.spendsync.requisition.internal.repository.RequisitionApprovalStepRepository;
import com.enterprise.spendsync.shared.domain.CrossAssignmentDetector;
import com.enterprise.spendsync.shared.domain.CrossAssignmentWarning;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.security.ApprovalSecurityPolicy;
import com.enterprise.spendsync.shared.security.PolicyDecision;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RequisitionServiceImpl implements RequisitionService {

    private final PurchaseRequisitionRepository requisitionRepository;
    private final RequisitionApprovalStepRepository approvalStepRepository;
    private final ApprovalAuthorityLimitRepository limitRepository;
    private final BudgetPoolRepository budgetPoolRepository;
    private final BudgetService budgetService;
    private final ApprovalLimitService approvalLimitService;
    private final ApprovalSecurityPolicy approvalSecurityPolicy;
    private final UserRepository userRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final CostCenterRepository costCenterRepository;
    private final FacilityRepository facilityRepository;
    private final TenantRepository tenantRepository;
    private final CrossAssignmentDetector crossAssignmentDetector;
    private final ApplicationEventPublisher eventPublisher;

    public RequisitionServiceImpl(PurchaseRequisitionRepository requisitionRepository,
                                  RequisitionApprovalStepRepository approvalStepRepository,
                                  ApprovalAuthorityLimitRepository limitRepository,
                                  BudgetPoolRepository budgetPoolRepository,
                                  BudgetService budgetService,
                                  ApprovalLimitService approvalLimitService,
                                  ApprovalSecurityPolicy approvalSecurityPolicy,
                                  UserRepository userRepository,
                                  LegalEntityRepository legalEntityRepository,
                                  CostCenterRepository costCenterRepository,
                                  FacilityRepository facilityRepository,
                                  TenantRepository tenantRepository,
                                  CrossAssignmentDetector crossAssignmentDetector,
                                  ApplicationEventPublisher eventPublisher) {
        this.requisitionRepository = requisitionRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.limitRepository = limitRepository;
        this.budgetPoolRepository = budgetPoolRepository;
        this.budgetService = budgetService;
        this.approvalLimitService = approvalLimitService;
        this.approvalSecurityPolicy = approvalSecurityPolicy;
        this.userRepository = userRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.costCenterRepository = costCenterRepository;
        this.facilityRepository = facilityRepository;
        this.tenantRepository = tenantRepository;
        this.crossAssignmentDetector = crossAssignmentDetector;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RequisitionDetailResponse createAndSubmitRequisition(CreateRequisitionRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = getCurrentUser(tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        LegalEntity legalEntity = legalEntityRepository.findByIdAndTenantId(request.legalEntityId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Legal entity not found in active tenant", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {});

        CostCenter costCenter = costCenterRepository.findByIdAndTenantId(request.costCenterId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Cost center not found in active tenant", HttpStatus.NOT_FOUND, "COST_CENTER_NOT_FOUND") {});

        Facility facility = facilityRepository.findByIdAndTenantId(request.deliveryFacilityId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Facility not found in active tenant", HttpStatus.NOT_FOUND, "FACILITY_NOT_FOUND") {});

        // 1. Calculate sequential requisition number (e.g. PR-2026-00001)
        long count = requisitionRepository.countByTenantId(tenantId) + 1;
        String prNumber = String.format("PR-%d-%05d", Year.now().getValue(), count);

        // 2. Build Requisition Header
        PurchaseRequisition pr = new PurchaseRequisition(
                tenant,
                prNumber,
                currentUser,
                legalEntity,
                costCenter,
                facility,
                null,
                RequisitionStatus.PENDING_APPROVAL,
                BigDecimal.ZERO,
                request.currency(),
                request.title(),
                request.justification()
        );

        // 3. Add Line Items
        int lineNumber = 1;
        for (CreateLineItemRequest itemReq : request.lineItems()) {
            RequisitionLineItem item = new RequisitionLineItem(
                    pr,
                    tenant,
                    lineNumber++,
                    itemReq.itemDescription(),
                    itemReq.itemCategory(),
                    itemReq.quantity(),
                    itemReq.unitOfMeasure(),
                    itemReq.unitPrice(),
                    itemReq.estimatedDeliveryDate()
            );
            pr.addLineItem(item);
        }

        BigDecimal totalAmount = pr.getTotalAmount();

        // 4. Resolve Active Budget Pool (Cost Center specific first, then Legal Entity fallback)
        BudgetPool pool = findActiveBudgetPool(legalEntity.getId(), costCenter.getId(), tenantId)
                .orElseThrow(() -> new SpendSyncException(
                        "No active budget pool found for Cost Center '" + costCenter.getName() + "' or Legal Entity '" + legalEntity.getName() + "'.",
                        HttpStatus.BAD_REQUEST,
                        "NO_ACTIVE_BUDGET_POOL"
                ) {});
        pr.setBudgetPool(pool);

        // 5. Construct Dynamic Sequential Approval Chain (DAG)
        List<RequisitionApprovalStep> steps = buildApprovalChain(pr, currentUser, legalEntity, costCenter, totalAmount, tenant);
        if (steps.isEmpty()) {
            throw new SpendSyncException(
                    "No authorized approver found in the Delegation of Authority (DoA) matrix for this requisition.",
                    HttpStatus.BAD_REQUEST,
                    "NO_APPROVER_CONFIGURED"
            ) {};
        }
        for (RequisitionApprovalStep step : steps) {
            pr.addApprovalStep(step);
        }

        // Save PR before budget reservation so that pr.getId() is guaranteed
        PurchaseRequisition savedPr = requisitionRepository.save(pr);

        // 6. Synchronous Budget Reservation (Will fail and rollback transaction if insufficient budget)
        budgetService.reserveBudget(
                pr.getBudgetPool().getId(),
                pr.getTotalAmount(),
                savedPr.getId(),
                "PURCHASE_REQUISITION",
                "PR Initial Reservation: " + savedPr.getRequisitionNumber()
        );

        return mapToDetailResponse(savedPr);
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitionDetailResponse getRequisitionById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        PurchaseRequisition pr = requisitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Purchase requisition not found", HttpStatus.NOT_FOUND, "REQUISITION_NOT_FOUND") {});
        return mapToDetailResponse(pr);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequisitionSummaryResponse> getMyRequisitions() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = getCurrentUser(tenantId);
        return requisitionRepository.findAllByTenantIdAndRequisitionerIdOrderByCreatedAtDesc(tenantId, currentUser.getId())
                .stream()
                .map(RequisitionSummaryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequisitionSummaryResponse> getAllRequisitions(RequisitionStatus status) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<PurchaseRequisition> list = (status != null)
                ? requisitionRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status)
                : requisitionRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        return list.stream().map(RequisitionSummaryResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequisitionDetailResponse> getMyPendingApprovals() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = getCurrentUser(tenantId);
        List<RequisitionApprovalStep> steps = approvalStepRepository.findPendingStepsForApprover(currentUser.getId(), tenantId);
        return steps.stream()
                .map(RequisitionApprovalStep::getRequisition)
                .distinct()
                .map(this::mapToDetailResponse)
                .toList();
    }

    @Override
    public RequisitionDetailResponse approveStep(UUID requisitionId, ApproveRequisitionStepRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = getCurrentUser(tenantId);

        PurchaseRequisition pr = requisitionRepository.findByIdAndTenantId(requisitionId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Purchase requisition not found", HttpStatus.NOT_FOUND, "REQUISITION_NOT_FOUND") {});

        if (pr.getStatus() != RequisitionStatus.PENDING_APPROVAL) {
            throw new SpendSyncException("Only requisitions in PENDING_APPROVAL status can be approved. Current: " + pr.getStatus(),
                    HttpStatus.BAD_REQUEST, "INVALID_REQUISITION_STATUS") {};
        }

        List<RequisitionApprovalStep> steps = approvalStepRepository.findAllByRequisitionIdOrderByStepOrderAsc(requisitionId);
        RequisitionApprovalStep activeStep = steps.stream()
                .filter(s -> s.getStatus() == ApprovalStepStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new SpendSyncException("No pending approval step found for requisition: " + requisitionId,
                        HttpStatus.BAD_REQUEST, "NO_PENDING_STEP") {});

        if (!activeStep.getApprover().getId().equals(currentUser.getId())) {
            throw new SpendSyncException("You are not the designated approver for step " + activeStep.getStepOrder() + " of this requisition.",
                    HttpStatus.FORBIDDEN, "NOT_CURRENT_APPROVER") {};
        }

        Optional<RequisitionApprovalStep> nextStepOpt = steps.stream()
                .filter(s -> s.getStepOrder() == activeStep.getStepOrder() + 1 && s.getStatus() == ApprovalStepStatus.WAITING)
                .findFirst();
        boolean isFinalStep = nextStepOpt.isEmpty();

        BigDecimal userLimit = resolveEffectiveCeiling(currentUser.getId(), pr.getLegalEntity().getId(), pr.getCostCenter().getId());

        PolicyDecision sodDecision = approvalSecurityPolicy.canApproveRequisition(
                currentUser.getId(),
                pr.getRequisitioner().getId(),
                currentUser.getRoles(),
                pr.getTotalAmount(),
                userLimit,
                isFinalStep
        );

        if (!sodDecision.isAllowed()) {
            throw new SpendSyncException(sodDecision.getReason(), HttpStatus.FORBIDDEN, sodDecision.getErrorCode()) {};
        }

        activeStep.setStatus(ApprovalStepStatus.APPROVED);
        activeStep.setDecisionNote(request != null ? request.notes() : null);
        activeStep.setDecidedAt(Instant.now());
        approvalStepRepository.save(activeStep);

        if (nextStepOpt.isPresent()) {
            RequisitionApprovalStep nextStep = nextStepOpt.get();
            nextStep.setStatus(ApprovalStepStatus.PENDING);
            approvalStepRepository.save(nextStep);
        } else {
            pr.setStatus(RequisitionStatus.APPROVED);
            pr.setApprovedAt(Instant.now());
            requisitionRepository.save(pr);

            publishApprovedEvent(pr);
        }

        return mapToDetailResponse(pr);
    }

    @Override
    public RequisitionDetailResponse rejectRequisition(UUID requisitionId, RejectRequisitionRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = getCurrentUser(tenantId);

        PurchaseRequisition pr = requisitionRepository.findByIdAndTenantId(requisitionId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Purchase requisition not found", HttpStatus.NOT_FOUND, "REQUISITION_NOT_FOUND") {});

        if (pr.getStatus() != RequisitionStatus.PENDING_APPROVAL) {
            throw new SpendSyncException("Only requisitions in PENDING_APPROVAL status can be rejected.",
                    HttpStatus.BAD_REQUEST, "INVALID_REQUISITION_STATUS") {};
        }

        PolicyDecision decision = approvalSecurityPolicy.canRejectRequisition(currentUser.getRoles());
        if (!decision.isAllowed()) {
            throw new SpendSyncException(decision.getReason(), HttpStatus.FORBIDDEN, decision.getErrorCode()) {};
        }

        List<RequisitionApprovalStep> steps = approvalStepRepository.findAllByRequisitionIdOrderByStepOrderAsc(requisitionId);
        for (RequisitionApprovalStep step : steps) {
            if (step.getStatus() == ApprovalStepStatus.PENDING) {
                step.setStatus(ApprovalStepStatus.REJECTED);
                step.setDecisionNote(request.rejectionReason());
                step.setDecidedAt(Instant.now());
                approvalStepRepository.save(step);
            } else if (step.getStatus() == ApprovalStepStatus.WAITING) {
                step.setStatus(ApprovalStepStatus.SKIPPED);
                approvalStepRepository.save(step);
            }
        }

        pr.setStatus(RequisitionStatus.REJECTED);
        pr.setRejectionReason(request.rejectionReason());
        requisitionRepository.save(pr);

        // Release reserved budget back to pool
        if (pr.getBudgetPool() != null) {
            budgetService.releaseBudget(
                    pr.getBudgetPool().getId(),
                    pr.getTotalAmount(),
                    pr.getId(),
                    "PURCHASE_REQUISITION",
                    "PR Rejected: " + pr.getRequisitionNumber() + " - Reason: " + request.rejectionReason()
            );
        }

        // Publish RequisitionRejectedEvent
        eventPublisher.publishEvent(RequisitionRejectedEvent.of(
                pr.getTenant().getId(),
                pr.getId(),
                pr.getRequisitionNumber(),
                currentUser.getId(),
                request.rejectionReason(),
                pr.getTotalAmount()
        ));

        return mapToDetailResponse(pr);
    }

    @Override
    public RequisitionDetailResponse cancelRequisition(UUID requisitionId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = getCurrentUser(tenantId);

        PurchaseRequisition pr = requisitionRepository.findByIdAndTenantId(requisitionId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Purchase requisition not found", HttpStatus.NOT_FOUND, "REQUISITION_NOT_FOUND") {});

        if (!pr.getRequisitioner().getId().equals(currentUser.getId())) {
            throw new SpendSyncException("Only the original requisitioner can cancel this requisition.",
                    HttpStatus.FORBIDDEN, "UNAUTHORIZED_CANCELLATION") {};
        }

        if (pr.getStatus() != RequisitionStatus.PENDING_APPROVAL && pr.getStatus() != RequisitionStatus.DRAFT) {
            throw new SpendSyncException("Only DRAFT or PENDING_APPROVAL requisitions can be cancelled.",
                    HttpStatus.BAD_REQUEST, "INVALID_REQUISITION_STATUS") {};
        }

        List<RequisitionApprovalStep> steps = approvalStepRepository.findAllByRequisitionIdOrderByStepOrderAsc(requisitionId);
        for (RequisitionApprovalStep step : steps) {
            if (step.getStatus() == ApprovalStepStatus.PENDING || step.getStatus() == ApprovalStepStatus.WAITING) {
                step.setStatus(ApprovalStepStatus.SKIPPED);
                approvalStepRepository.save(step);
            }
        }

        pr.setStatus(RequisitionStatus.CANCELLED);
        requisitionRepository.save(pr);

        // Release reserved budget back to pool
        if (pr.getBudgetPool() != null) {
            budgetService.releaseBudget(
                    pr.getBudgetPool().getId(),
                    pr.getTotalAmount(),
                    pr.getId(),
                    "PURCHASE_REQUISITION",
                    "PR Cancelled by Requisitioner: " + pr.getRequisitionNumber()
            );
        }

        return mapToDetailResponse(pr);
    }

    private RequisitionDetailResponse mapToDetailResponse(PurchaseRequisition pr) {
        CrossAssignmentWarning warning = crossAssignmentDetector.detect(pr.getLegalEntity(), pr.getDeliveryFacility());
        return RequisitionDetailResponse.from(pr, warning);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Internal Helper Methods
    // ───────────────────────────────────────────────────────────────────────────

    private List<RequisitionApprovalStep> buildApprovalChain(
            PurchaseRequisition pr,
            User requisitioner,
            LegalEntity legalEntity,
            CostCenter costCenter,
            BigDecimal totalAmount,
            Tenant tenant
    ) {
        List<RequisitionApprovalStep> steps = new ArrayList<>();
        List<ApprovalAuthorityLimit> limits = limitRepository.findAllByTenantIdAndLegalEntityId(tenant.getId(), legalEntity.getId())
                .stream()
                .filter(ApprovalAuthorityLimit::isActive)
                .sorted(Comparator.comparing(ApprovalAuthorityLimit::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // 1. Resolve Tier 1 Manager Approver (Direct Manager, Cost Center Manager or latest Level 1 Limit)
        User tier1Approver = resolveTierApprover(limits, 1, costCenter, requisitioner);
        if (tier1Approver != null && !tier1Approver.getId().equals(requisitioner.getId())) {
            steps.add(new RequisitionApprovalStep(pr, tenant, 1, tier1Approver, 1, ApprovalStepStatus.PENDING));
        }

        // Check if Tier 1 has sufficient ceiling for totalAmount
        BigDecimal tier1Limit = tier1Approver != null
                ? resolveEffectiveCeiling(tier1Approver.getId(), legalEntity.getId(), costCenter.getId())
                : BigDecimal.ZERO;

        boolean tier1Sufficient = tier1Approver != null && (tier1Limit == null || (tier1Limit.compareTo(totalAmount) >= 0));

        // 2. If Tier 1 insufficient, escalate to Tier 2 (Director)
        if (!tier1Sufficient) {
            User tier2Approver = resolveTierApprover(limits, 2, costCenter, requisitioner);
            if (tier2Approver != null && steps.stream().noneMatch(s -> s.getApprover().getId().equals(tier2Approver.getId()))) {
                ApprovalStepStatus stepStatus = steps.isEmpty() ? ApprovalStepStatus.PENDING : ApprovalStepStatus.WAITING;
                steps.add(new RequisitionApprovalStep(pr, tenant, steps.size() + 1, tier2Approver, 2, stepStatus));
            }

            BigDecimal tier2Limit = tier2Approver != null
                    ? resolveEffectiveCeiling(tier2Approver.getId(), legalEntity.getId(), costCenter.getId())
                    : BigDecimal.ZERO;

            boolean tier2Sufficient = tier2Approver != null && (tier2Limit == null || (tier2Limit.compareTo(totalAmount) >= 0));

            // 3. If Tier 2 still insufficient, escalate to Tier 4 (CFO)
            if (!tier2Sufficient) {
                User tier4Approver = resolveTierApprover(limits, 4, null, requisitioner);
                if (tier4Approver != null && steps.stream().noneMatch(s -> s.getApprover().getId().equals(tier4Approver.getId()))) {
                    ApprovalStepStatus stepStatus = steps.isEmpty() ? ApprovalStepStatus.PENDING : ApprovalStepStatus.WAITING;
                    steps.add(new RequisitionApprovalStep(pr, tenant, steps.size() + 1, tier4Approver, 4, stepStatus));
                }
            }
        }

        // Ensure at least first step is PENDING
        if (!steps.isEmpty() && steps.get(0).getStatus() != ApprovalStepStatus.PENDING) {
            steps.get(0).setStatus(ApprovalStepStatus.PENDING);
        }

        return steps;
    }

    private BigDecimal resolveEffectiveCeiling(UUID userId, UUID legalEntityId, UUID costCenterId) {
        var detailsOpt = approvalLimitService.getEffectiveLimitDetails(userId, legalEntityId, costCenterId);
        if (detailsOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        var limit = detailsOpt.get();
        if (limit.isUnlimited()) {
            return null; // Unlimited authority (e.g. CFO)
        }
        return limit.getMaxAmount();
    }

    private User resolveTierApprover(List<ApprovalAuthorityLimit> limits, int level, CostCenter costCenter, User requisitioner) {
        // Check direct manager if matched level
        if (requisitioner != null && requisitioner.getManagerUser() != null) {
            UUID mgrId = requisitioner.getManagerUser().getId();
            boolean mgrMatches = limits.stream().anyMatch(l -> l.getUser().getId().equals(mgrId) && l.getApprovalLevel() == level);
            if (mgrMatches) {
                return requisitioner.getManagerUser();
            }
        }

        // First look for cost-center specific limit at this level
        if (costCenter != null) {
            Optional<ApprovalAuthorityLimit> ccLimit = limits.stream()
                    .filter(l -> l.getApprovalLevel() == level && l.getCostCenter() != null && l.getCostCenter().getId().equals(costCenter.getId()))
                    .findFirst();
            if (ccLimit.isPresent()) {
                return ccLimit.get().getUser();
            }
        }

        // Fallback to Entity-wide limit at this level
        Optional<ApprovalAuthorityLimit> entityLimit = limits.stream()
                .filter(l -> l.getApprovalLevel() == level && l.getCostCenter() == null)
                .findFirst();

        return entityLimit.map(ApprovalAuthorityLimit::getUser).orElse(null);
    }

    private Optional<BudgetPool> findActiveBudgetPool(UUID legalEntityId, UUID costCenterId, UUID tenantId) {
        // Try Cost Center specific pool first
        Optional<BudgetPool> ccPool = budgetPoolRepository.findByCostCenterIdAndLegalEntityIdAndStatusAndTenantId(
                costCenterId, legalEntityId, BudgetStatus.ACTIVE, tenantId
        );
        if (ccPool.isPresent()) {
            return ccPool;
        }
        // Fallback to Legal Entity wide pool
        return budgetPoolRepository.findByLegalEntityIdAndCostCenterIsNullAndStatusAndTenantId(
                legalEntityId, BudgetStatus.ACTIVE, tenantId
        );
    }

    private void publishApprovedEvent(PurchaseRequisition pr) {
        List<LineItemEventPayload> itemPayloads = pr.getLineItems().stream()
                .map(item -> new LineItemEventPayload(
                        item.getLineNumber(),
                        item.getItemDescription(),
                        item.getItemCategory(),
                        item.getQuantity(),
                        item.getUnitOfMeasure(),
                        item.getUnitPrice(),
                        item.getTotalPrice(),
                        item.getEstimatedDeliveryDate()
                ))
                .toList();

        eventPublisher.publishEvent(RequisitionApprovedEvent.of(
                pr.getTenant().getId(),
                pr.getId(),
                pr.getRequisitionNumber(),
                pr.getRequisitioner().getId(),
                pr.getLegalEntity().getId(),
                pr.getCostCenter().getId(),
                pr.getDeliveryFacility().getId(),
                pr.getTotalAmount(),
                pr.getCurrency(),
                pr.getTitle(),
                itemPayloads
        ));
    }

    private User getCurrentUser(UUID tenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SpendSyncException("Authentication required", HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED") {};
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return userRepository.findByIdAndTenantId(up.getId(), tenantId)
                    .orElseThrow(() -> new SpendSyncException("User not found: " + up.getId(), HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
        }
        throw new SpendSyncException("Unable to resolve authenticated user principal", HttpStatus.UNAUTHORIZED, "INVALID_PRINCIPAL") {};
    }
}
