package com.enterprise.spendsync.receiving.internal.service;

import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.FacilityRepository;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderLineItemRepository;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceipt;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceiptLineItem;
import com.enterprise.spendsync.receiving.internal.dto.CreateGRLineItemRequest;
import com.enterprise.spendsync.receiving.internal.dto.CreateGoodsReceiptRequest;
import com.enterprise.spendsync.receiving.internal.dto.GoodsReceiptResponse;
import com.enterprise.spendsync.receiving.internal.dto.PendingPOForReceivingResponse;
import com.enterprise.spendsync.receiving.internal.event.GRLineItemPayload;
import com.enterprise.spendsync.receiving.internal.event.GoodsReceivedEvent;
import com.enterprise.spendsync.receiving.internal.repository.GoodsReceiptLineItemRepository;
import com.enterprise.spendsync.receiving.internal.repository.GoodsReceiptRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.notification.EmailService;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

    private static final Logger log = LoggerFactory.getLogger(GoodsReceiptServiceImpl.class);

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptLineItemRepository goodsReceiptLineItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineItemRepository purchaseOrderLineItemRepository;
    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;

    public GoodsReceiptServiceImpl(GoodsReceiptRepository goodsReceiptRepository,
                                  GoodsReceiptLineItemRepository goodsReceiptLineItemRepository,
                                  PurchaseOrderRepository purchaseOrderRepository,
                                  PurchaseOrderLineItemRepository purchaseOrderLineItemRepository,
                                  FacilityRepository facilityRepository,
                                  UserRepository userRepository,
                                  TenantRepository tenantRepository,
                                  ApplicationEventPublisher eventPublisher,
                                  EmailService emailService) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.goodsReceiptLineItemRepository = goodsReceiptLineItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderLineItemRepository = purchaseOrderLineItemRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public GoodsReceiptResponse createGoodsReceipt(CreateGoodsReceiptRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        User currentUser = resolveCurrentUser(tenantId);

        PurchaseOrder po = purchaseOrderRepository.findByIdAndTenantId(request.purchaseOrderId(), tenantId)
                .orElseThrow(() -> new SpendSyncException("Purchase Order not found", HttpStatus.NOT_FOUND, "PO_NOT_FOUND") {});

        if (po.getStatus() != PurchaseOrderStatus.ISSUED && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new SpendSyncException(
                    "Goods receipt can only be created for ISSUED or PARTIALLY_RECEIVED orders. Current status: " + po.getStatus(),
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PO_STATUS_FOR_RECEIVING"
            ) {};
        }

        Facility facility = request.deliveryFacilityId() != null
                ? facilityRepository.findByIdAndTenantId(request.deliveryFacilityId(), tenantId)
                    .orElseThrow(() -> new SpendSyncException("Delivery facility not found", HttpStatus.NOT_FOUND, "FACILITY_NOT_FOUND") {})
                : po.getDeliveryFacility();

        String receiptNumber = generateReceiptNumber(tenantId);

        GoodsReceipt gr = new GoodsReceipt(
                tenant,
                receiptNumber,
                po,
                facility,
                request.waybillNumber(),
                request.waybillDate(),
                currentUser,
                request.notes()
        );

        List<GRLineItemPayload> eventLinePayloads = new ArrayList<>();
        boolean allLinesFullyReceived = true;

        for (CreateGRLineItemRequest lineReq : request.lineItems()) {
            PurchaseOrderLineItem poLine = purchaseOrderLineItemRepository.findById(lineReq.purchaseOrderLineItemId())
                    .orElseThrow(() -> new SpendSyncException("PO Line Item not found: " + lineReq.purchaseOrderLineItemId(), HttpStatus.NOT_FOUND, "PO_LINE_NOT_FOUND") {});

            if (!poLine.getPurchaseOrder().getId().equals(po.getId())) {
                throw new SpendSyncException("PO line item does not belong to the specified PO", HttpStatus.BAD_REQUEST, "INVALID_PO_LINE_MAPPING") {};
            }

            BigDecimal rejectedQty = lineReq.rejectedQuantity() != null ? lineReq.rejectedQuantity() : BigDecimal.ZERO;
            BigDecimal sumReceived = lineReq.acceptedQuantity().add(rejectedQty);

            if (lineReq.receivedQuantity().compareTo(sumReceived) != 0) {
                throw new SpendSyncException(
                        String.format("Received quantity (%.2f) must equal the sum of accepted (%.2f) and rejected (%.2f) quantities",
                                lineReq.receivedQuantity(), lineReq.acceptedQuantity(), rejectedQty),
                        HttpStatus.BAD_REQUEST,
                        "QUANTITY_INCONSISTENCY"
                ) {};
            }

            if (rejectedQty.compareTo(BigDecimal.ZERO) > 0 && (lineReq.rejectionReason() == null || lineReq.rejectionReason().isBlank())) {
                throw new SpendSyncException(
                        "Rejection reason is mandatory when rejected quantity is greater than zero",
                        HttpStatus.BAD_REQUEST,
                        "REJECTION_REASON_MANDATORY"
                ) {};
            }

            // Over-delivery tolerance check
            BigDecimal previouslyAccepted = goodsReceiptLineItemRepository.sumAcceptedQuantityByPoLineId(poLine.getId());
            BigDecimal newTotalAccepted = previouslyAccepted.add(lineReq.acceptedQuantity());
            BigDecimal maxAllowedQuantity = poLine.getQuantity().multiply(
                    BigDecimal.ONE.add(poLine.getOverDeliveryTolerancePct().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            );

            if (newTotalAccepted.compareTo(maxAllowedQuantity) > 0) {
                throw new SpendSyncException(
                        String.format("Total accepted quantity (%.2f) exceeds maximum allowed (%.2f) under %.1f%% over-delivery tolerance for line %d",
                                newTotalAccepted, maxAllowedQuantity, poLine.getOverDeliveryTolerancePct(), poLine.getLineNumber()),
                        HttpStatus.BAD_REQUEST,
                        "OVER_DELIVERY_TOLERANCE_EXCEEDED"
                ) {};
            }

            GoodsReceiptLineItem grLine = new GoodsReceiptLineItem(
                    tenant,
                    poLine,
                    lineReq.receivedQuantity(),
                    lineReq.acceptedQuantity(),
                    rejectedQty,
                    lineReq.rejectionReason(),
                    lineReq.notes()
            );

            gr.addLineItem(grLine);

            eventLinePayloads.add(new GRLineItemPayload(
                    poLine.getId(),
                    poLine.getItemDescription(),
                    lineReq.receivedQuantity(),
                    lineReq.acceptedQuantity(),
                    rejectedQty,
                    lineReq.rejectionReason()
            ));

            if (newTotalAccepted.compareTo(poLine.getQuantity()) < 0) {
                allLinesFullyReceived = false;
            }
        }

        GoodsReceipt savedGr = goodsReceiptRepository.save(gr);

        // Update PO Status based on fulfillment
        if (allLinesFullyReceived && po.getLineItems().size() == request.lineItems().size()) {
            po.setStatus(PurchaseOrderStatus.FULFILLED);
        } else {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }
        purchaseOrderRepository.save(po);

        // Publish Domain Event
        GoodsReceivedEvent event = new GoodsReceivedEvent(
                tenantId,
                savedGr.getId(),
                savedGr.getReceiptNumber(),
                po.getId(),
                po.getPoNumber(),
                facility.getId(),
                savedGr.getWaybillNumber(),
                currentUser.getId(),
                eventLinePayloads,
                Instant.now()
        );
        eventPublisher.publishEvent(event);

        // Notify Requisitioner
        if (po.getRequisition() != null && po.getRequisition().getRequisitioner() != null) {
            String requisitionerEmail = po.getRequisition().getRequisitioner().getEmail();
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("receiptNumber", savedGr.getReceiptNumber());
            emailData.put("poNumber", po.getPoNumber());
            emailData.put("facilityName", facility.getName());
            emailData.put("waybillNumber", savedGr.getWaybillNumber());
            emailData.put("vendorName", po.getVendor().getName());

            emailService.sendTemplatedEmail(
                    requisitionerEmail,
                    "Mal Kabul Gerçekleşti: " + savedGr.getReceiptNumber() + " - " + po.getPoNumber(),
                    "goods-receipt-completed",
                    emailData
            );
        }

        log.info("Goods Receipt {} successfully created for PO: {}", savedGr.getReceiptNumber(), po.getPoNumber());
        return GoodsReceiptResponse.from(savedGr);
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsReceiptResponse getGoodsReceiptById(UUID id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        GoodsReceipt gr = goodsReceiptRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new SpendSyncException("Goods Receipt not found", HttpStatus.NOT_FOUND, "GR_NOT_FOUND") {});
        return GoodsReceiptResponse.from(gr);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> getAllGoodsReceipts() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return goodsReceiptRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(GoodsReceiptResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> getGoodsReceiptsByPurchaseOrder(UUID purchaseOrderId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return goodsReceiptRepository.findAllByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(tenantId, purchaseOrderId)
                .stream()
                .map(GoodsReceiptResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingPOForReceivingResponse> getPendingOrdersForReceiving() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return purchaseOrderRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatus.ISSUED || po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED)
                .map(PendingPOForReceivingResponse::from)
                .toList();
    }

    private String generateReceiptNumber(UUID tenantId) {
        int currentYear = LocalDate.now().getYear();
        String prefix = String.format("GR-%d-", currentYear);
        long count = goodsReceiptRepository.countByTenantIdAndReceiptNumberPrefix(tenantId, prefix);
        return String.format("GR-%d-%05d", currentYear, count + 1);
    }

    private User resolveCurrentUser(UUID tenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findByIdAndTenantId(principal.getId(), tenantId)
                    .orElseThrow(() -> new SpendSyncException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
        }
        return userRepository.findAllByTenantId(tenantId).stream().findFirst()
                .orElseThrow(() -> new SpendSyncException("Default user not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND") {});
    }
}
