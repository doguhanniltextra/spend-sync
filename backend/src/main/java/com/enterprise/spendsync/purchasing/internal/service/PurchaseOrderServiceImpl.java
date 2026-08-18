package com.enterprise.spendsync.purchasing.internal.service;

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
import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderRevision;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorStatus;
import com.enterprise.spendsync.purchasing.internal.dto.CancelPurchaseOrderRequest;
import com.enterprise.spendsync.purchasing.internal.dto.CreatePurchaseOrderRequest;
import com.enterprise.spendsync.purchasing.internal.dto.POLineItemRequest;
import com.enterprise.spendsync.purchasing.internal.dto.PORevisionResponse;
import com.enterprise.spendsync.purchasing.internal.dto.PurchaseOrderDetailResponse;
import com.enterprise.spendsync.purchasing.internal.dto.PurchaseOrderSummaryResponse;
import com.enterprise.spendsync.purchasing.internal.dto.RevisePOLineItemRequest;
import com.enterprise.spendsync.purchasing.internal.dto.RevisePurchaseOrderRequest;
import com.enterprise.spendsync.purchasing.internal.event.POLineItemPayload;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderCancelledEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderIssuedEvent;
import com.enterprise.spendsync.purchasing.internal.event.PurchaseOrderRevisedEvent;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderLineItemRepository;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRevisionRepository;
import com.enterprise.spendsync.purchasing.internal.repository.VendorRepository;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionLineItem;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.requisition.internal.repository.PurchaseRequisitionRepository;
import com.enterprise.spendsync.shared.domain.CrossAssignmentDetector;
import com.enterprise.spendsync.shared.domain.CrossAssignmentWarning;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.notification.EmailService;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineItemRepository lineItemRepository;
    private final PurchaseOrderRevisionRepository revisionRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseRequisitionRepository requisitionRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final CostCenterRepository costCenterRepository;
    private final FacilityRepository facilityRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;
    private final CrossAssignmentDetector crossAssignmentDetector;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    public PurchaseOrderServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                                   PurchaseOrderLineItemRepository lineItemRepository,
                                   PurchaseOrderRevisionRepository revisionRepository,
                                   VendorRepository vendorRepository,
                                   PurchaseRequisitionRepository requisitionRepository,
                                   LegalEntityRepository legalEntityRepository,
                                   CostCenterRepository costCenterRepository,
                                   FacilityRepository facilityRepository,
                                   TenantRepository tenantRepository,
                                   UserRepository userRepository,
                                   BudgetService budgetService,
                                   CrossAssignmentDetector crossAssignmentDetector,
                                   EmailService emailService,
                                   ApplicationEventPublisher eventPublisher) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.lineItemRepository = lineItemRepository;
        this.revisionRepository = revisionRepository;
        this.vendorRepository = vendorRepository;
        this.requisitionRepository = requisitionRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.costCenterRepository = costCenterRepository;
        this.facilityRepository = facilityRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.budgetService = budgetService;
        this.crossAssignmentDetector = crossAssignmentDetector;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PurchaseOrderDetailResponse createPurchaseOrder(CreatePurchaseOrderRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        User currentUser = getCurrentUser(tenantId);

        LegalEntity legalEntity = legalEntityRepository.findByIdAndTenantId(request.legalEntityId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Legal Entity not found", HttpStatus.NOT_FOUND, "LEGAL_ENTITY_NOT_FOUND") {});

        CostCenter costCenter = costCenterRepository.findByIdAndTenantId(request.costCenterId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Cost Center not found", HttpStatus.NOT_FOUND, "COST_CENTER_NOT_FOUND") {});

        Facility facility = facilityRepository.findByIdAndTenantId(request.deliveryFacilityId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Facility not found", HttpStatus.NOT_FOUND, "FACILITY_NOT_FOUND") {});

        Vendor vendor = vendorRepository.findByIdAndTenantId(request.vendorId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Vendor not found", HttpStatus.NOT_FOUND, "VENDOR_NOT_FOUND") {});

        if (vendor.getStatus() != VendorStatus.ACTIVE) {
            throw new SpendSyncException("Cannot issue purchase order to an inactive or blocked vendor.",
                    HttpStatus.BAD_REQUEST, "VENDOR_NOT_ACTIVE") {};
        }

        PurchaseRequisition pr = null;
        if (request.requisitionId() != null) {
            pr = requisitionRepository.findByIdAndTenantId(request.requisitionId(), tenantId)
                    .orElseThrow(() -> new SpendSyncException("Requisition not found", HttpStatus.NOT_FOUND, "REQUISITION_NOT_FOUND") {});

            if (pr.getStatus() != RequisitionStatus.APPROVED) {
                throw new SpendSyncException("Cannot create PO from a requisition that is not in APPROVED status. Current status: " + pr.getStatus(),
                        HttpStatus.BAD_REQUEST, "REQUISITION_NOT_APPROVED") {};
            }
        }

        long nextSeq = purchaseOrderRepository.countByTenantId(tenantId) + 1;
        String poNumber = PurchaseOrder.generatePoNumber(LocalDate.now().getYear(), nextSeq);

        PurchaseOrder po = new PurchaseOrder(
                tenant,
                poNumber,
                pr,
                legalEntity,
                costCenter,
                facility,
                vendor,
                request.incoterms() != null ? request.incoterms() : Incoterms.DAP,
                request.currency(),
                request.paymentTerms(),
                request.notes(),
                currentUser
        );

        int lineNum = 1;
        for (POLineItemRequest itemReq : request.lineItems()) {
            RequisitionLineItem reqItem = null;
            if (pr != null && itemReq.requisitionLineItemId() != null) {
                reqItem = pr.getLineItems().stream()
                        .filter(li -> li.getId().equals(itemReq.requisitionLineItemId()))
                        .findFirst()
                        .orElse(null);
            }

            PurchaseOrderLineItem poItem = new PurchaseOrderLineItem(
                    tenant,
                    po,
                    reqItem,
                    lineNum++,
                    itemReq.itemDescription(),
                    itemReq.itemCategory(),
                    itemReq.quantity(),
                    itemReq.unitOfMeasure(),
                    itemReq.unitPrice(),
                    itemReq.overDeliveryTolerancePct(),
                    itemReq.underDeliveryTolerancePct(),
                    itemReq.estimatedDeliveryDate()
            );
            po.addLineItem(poItem);
        }

        PurchaseOrder savedPo = purchaseOrderRepository.save(po);
        return mapToDetailResponse(savedPo);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderDetailResponse getPurchaseOrderById(UUID poId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantId(poId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Purchase Order not found", HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND") {});
        return mapToDetailResponse(po);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderSummaryResponse> getAllPurchaseOrders(PurchaseOrderStatus status, UUID vendorId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<PurchaseOrder> orders;
        if (status != null) {
            orders = purchaseOrderRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        } else if (vendorId != null) {
            orders = purchaseOrderRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId);
        } else {
            orders = purchaseOrderRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        }

        return orders.stream()
                .map(po -> {
                    CrossAssignmentWarning warning = crossAssignmentDetector.detect(po.getLegalEntity(), po.getDeliveryFacility());
                    return PurchaseOrderSummaryResponse.from(po, warning.isCrossEntity());
                })
                .toList();
    }

    @Override
    @Transactional
    public PurchaseOrderDetailResponse issuePurchaseOrder(UUID poId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantId(poId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Purchase Order not found", HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND") {});

        if (po.getStatus() != PurchaseOrderStatus.DRAFT && po.getStatus() != PurchaseOrderStatus.REVISED) {
            throw new SpendSyncException("Only DRAFT or REVISED purchase orders can be issued. Current status: " + po.getStatus(),
                    HttpStatus.BAD_REQUEST, "INVALID_PO_STATUS") {};
        }

        po.setStatus(PurchaseOrderStatus.ISSUED);
        po.setIssuedAt(Instant.now());
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        // Send Official PO email to Vendor order email
        Map<String, Object> emailData = new HashMap<>();
        emailData.put("poNumber", savedPo.getPoNumber());
        emailData.put("vendorName", savedPo.getVendor().getName());
        emailData.put("legalEntityName", savedPo.getLegalEntity().getName());
        emailData.put("totalAmount", savedPo.getTotalAmount() + " " + savedPo.getCurrency());
        emailData.put("incoterms", savedPo.getIncoterms().name());
        emailData.put("paymentTerms", savedPo.getPaymentTerms().name());
        emailData.put("deliveryAddress", savedPo.getDeliveryFacility().getShippingAddress());

        emailService.sendTemplatedEmail(
                savedPo.getVendor().getOrderEmail(),
                "Resmi Satın Alma Siparişi: " + savedPo.getPoNumber() + " - " + savedPo.getLegalEntity().getName(),
                "purchase-order-issued",
                emailData
        );

        // Publish PurchaseOrderIssuedEvent for Receiving & Audit
        List<POLineItemPayload> itemPayloads = savedPo.getLineItems().stream()
                .map(item -> new POLineItemPayload(
                        item.getLineNumber(),
                        item.getItemDescription(),
                        item.getItemCategory(),
                        item.getQuantity(),
                        item.getUnitOfMeasure(),
                        item.getUnitPrice(),
                        item.getTotalPrice(),
                        item.getOverDeliveryTolerancePct(),
                        item.getUnderDeliveryTolerancePct(),
                        item.getEstimatedDeliveryDate()
                ))
                .toList();

        eventPublisher.publishEvent(PurchaseOrderIssuedEvent.of(
                savedPo.getTenant().getId(),
                savedPo.getId(),
                savedPo.getPoNumber(),
                savedPo.getRevisionNumber(),
                savedPo.getRequisition() != null ? savedPo.getRequisition().getId() : null,
                savedPo.getLegalEntity().getId(),
                savedPo.getCostCenter().getId(),
                savedPo.getDeliveryFacility().getId(),
                savedPo.getVendor().getId(),
                savedPo.getVendor().getName(),
                savedPo.getVendor().getOrderEmail(),
                savedPo.getIncoterms(),
                savedPo.getTotalAmount(),
                savedPo.getCurrency(),
                itemPayloads
        ));

        return mapToDetailResponse(savedPo);
    }

    @Override
    @Transactional
    public PurchaseOrderDetailResponse revisePurchaseOrder(UUID poId, RevisePurchaseOrderRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = getCurrentUser(tenantId);

        PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantId(poId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Purchase Order not found", HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND") {});

        if (po.getStatus() != PurchaseOrderStatus.ISSUED && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new SpendSyncException("Only ISSUED or PARTIALLY_RECEIVED purchase orders can be revised. Current status: " + po.getStatus(),
                    HttpStatus.BAD_REQUEST, "INVALID_PO_STATUS_FOR_REVISION") {};
        }

        BigDecimal previousTotal = po.getTotalAmount();

        // Update line items
        Map<UUID, RevisePOLineItemRequest> requestedItems = request.lineItems().stream()
                .filter(i -> i.lineItemId() != null)
                .collect(Collectors.toMap(RevisePOLineItemRequest::lineItemId, i -> i));

        for (PurchaseOrderLineItem item : po.getLineItems()) {
            if (requestedItems.containsKey(item.getId())) {
                RevisePOLineItemRequest r = requestedItems.get(item.getId());
                item.updatePriceAndQuantity(r.quantity(), r.unitPrice());
                if (r.overDeliveryTolerancePct() != null) item.setOverDeliveryTolerancePct(r.overDeliveryTolerancePct());
                if (r.underDeliveryTolerancePct() != null) item.setUnderDeliveryTolerancePct(r.underDeliveryTolerancePct());
                if (r.estimatedDeliveryDate() != null) item.setEstimatedDeliveryDate(r.estimatedDeliveryDate());
            }
        }

        po.recalculateTotal();
        BigDecimal newTotal = po.getTotalAmount();
        BigDecimal differential = newTotal.subtract(previousTotal);

        // Budget Differential Adjustment
        if (po.getRequisition() != null && po.getRequisition().getBudgetPool() != null) {
            UUID budgetPoolId = po.getRequisition().getBudgetPool().getId();
            if (differential.compareTo(BigDecimal.ZERO) > 0) {
                budgetService.reserveBudget(
                        budgetPoolId,
                        differential,
                        po.getId(),
                        "PURCHASE_ORDER_REVISION",
                        "PO Revision Increase (" + po.getPoNumber() + " Rev " + (po.getRevisionNumber() + 1) + "): " + request.reason()
                );
            } else if (differential.compareTo(BigDecimal.ZERO) < 0) {
                budgetService.releaseBudget(
                        budgetPoolId,
                        differential.abs(),
                        po.getId(),
                        "PURCHASE_ORDER_REVISION",
                        "PO Revision Decrease (" + po.getPoNumber() + " Rev " + (po.getRevisionNumber() + 1) + "): " + request.reason()
                );
            }
        }

        int newRevisionNumber = po.getRevisionNumber() + 1;
        po.setRevisionNumber(newRevisionNumber);

        // Record revision snapshot
        PurchaseOrderRevision revision = new PurchaseOrderRevision(
                po.getTenant(),
                po,
                newRevisionNumber,
                previousTotal,
                newTotal,
                differential,
                request.reason(),
                currentUser,
                "{\"previousTotal\":" + previousTotal + ",\"newTotal\":" + newTotal + ",\"differential\":" + differential + "}"
        );
        revisionRepository.save(revision);

        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        eventPublisher.publishEvent(PurchaseOrderRevisedEvent.of(
                savedPo.getTenant().getId(),
                savedPo.getId(),
                savedPo.getPoNumber(),
                newRevisionNumber,
                previousTotal,
                newTotal,
                differential,
                request.reason()
        ));

        return mapToDetailResponse(savedPo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PORevisionResponse> getPurchaseOrderRevisions(UUID poId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return revisionRepository.findAllByPurchaseOrderIdAndTenantIdOrderByRevisionNumberAsc(poId, tenantId)
                .stream()
                .map(PORevisionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PurchaseOrderDetailResponse cancelPurchaseOrder(UUID poId, CancelPurchaseOrderRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        User currentUser = getCurrentUser(tenantId);

        PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantId(poId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Purchase Order not found", HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND") {});

        if (po.getStatus() == PurchaseOrderStatus.FULFILLED || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new SpendSyncException("Cannot cancel a PO with status " + po.getStatus(),
                    HttpStatus.BAD_REQUEST, "INVALID_PO_STATUS_FOR_CANCELLATION") {};
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        // Release reserved budget back to pool
        if (po.getRequisition() != null && po.getRequisition().getBudgetPool() != null) {
            budgetService.releaseBudget(
                    po.getRequisition().getBudgetPool().getId(),
                    po.getTotalAmount(),
                    po.getId(),
                    "PURCHASE_ORDER",
                    "PO Cancelled: " + po.getPoNumber() + " - Reason: " + request.cancellationReason()
            );
        }

        eventPublisher.publishEvent(PurchaseOrderCancelledEvent.of(
                savedPo.getTenant().getId(),
                savedPo.getId(),
                savedPo.getPoNumber(),
                currentUser.getId(),
                request.cancellationReason(),
                savedPo.getTotalAmount()
        ));

        return mapToDetailResponse(savedPo);
    }

    private PurchaseOrderDetailResponse mapToDetailResponse(PurchaseOrder po) {
        CrossAssignmentWarning warning = crossAssignmentDetector.detect(po.getLegalEntity(), po.getDeliveryFacility());
        List<PORevisionResponse> revisions = revisionRepository.findAllByPurchaseOrderIdAndTenantIdOrderByRevisionNumberAsc(po.getId(), po.getTenant().getId())
                .stream()
                .map(PORevisionResponse::from)
                .toList();
        return PurchaseOrderDetailResponse.from(po, warning, revisions);
    }

    private User getCurrentUser(UUID tenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SpendSyncException("Authentication required", HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED") {};
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return userRepository.findByIdAndTenantId(up.getId(), tenantId)
                    .orElseThrow(() -> new SpendSyncException("Authenticated user not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
        }
        throw new SpendSyncException("Invalid authentication principal", HttpStatus.UNAUTHORIZED, "INVALID_PRINCIPAL") {};
    }
}
