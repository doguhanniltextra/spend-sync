package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.audit.internal.domain.AuditAction;
import com.enterprise.spendsync.audit.internal.domain.ComplianceTag;
import com.enterprise.spendsync.audit.internal.dto.RecordAuditRequest;
import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderLineItemRepository;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.shared.crypto.MaskingUtils;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.VendorAsnDispatchRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorAsnResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorOrderDetailResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorOrderSummaryResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorPoAcknowledgmentRequest;
import com.enterprise.spendsync.vendorportal.internal.domain.AsnShipmentStatus;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorAsnShipment;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorAsnShipmentLineItem;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorPoAcknowledgment;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorPoAcknowledgmentStatus;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorUser;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorAsnShipmentRepository;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorPoAcknowledgmentRepository;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VendorOrderServiceImpl implements VendorOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineItemRepository purchaseOrderLineItemRepository;
    private final VendorUserRepository vendorUserRepository;
    private final VendorPoAcknowledgmentRepository acknowledgmentRepository;
    private final VendorAsnShipmentRepository asnShipmentRepository;
    private final AuditService auditService;

    public VendorOrderServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                                  PurchaseOrderLineItemRepository purchaseOrderLineItemRepository,
                                  VendorUserRepository vendorUserRepository,
                                  VendorPoAcknowledgmentRepository acknowledgmentRepository,
                                  VendorAsnShipmentRepository asnShipmentRepository,
                                  AuditService auditService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderLineItemRepository = purchaseOrderLineItemRepository;
        this.vendorUserRepository = vendorUserRepository;
        this.acknowledgmentRepository = acknowledgmentRepository;
        this.asnShipmentRepository = asnShipmentRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorOrderSummaryResponse> getVendorOrders(PurchaseOrderStatus status, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        UUID tenantId = user.getTenant().getId();
        UUID vendorId = user.getVendor().getId();

        List<PurchaseOrder> orders;
        if (status != null) {
            orders = purchaseOrderRepository.findAllByTenantIdAndVendorIdAndStatusOrderByCreatedAtDesc(tenantId, vendorId, status);
        } else {
            orders = purchaseOrderRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId);
        }

        return orders.stream()
                .map(po -> {
                    Optional<VendorPoAcknowledgment> latestAck = acknowledgmentRepository
                            .findTopByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(tenantId, po.getId());

                    String ackStatus = latestAck.map(a -> a.getStatus().name()).orElse("PENDING");
                    java.time.LocalDate promisedDate = latestAck.map(VendorPoAcknowledgment::getPromisedDeliveryDate).orElse(null);

                    String facilityName = po.getDeliveryFacility() != null ? po.getDeliveryFacility().getName() : null;

                    return new VendorOrderSummaryResponse(
                            po.getId(),
                            po.getPoNumber(),
                            po.getTotalAmount(),
                            po.getCurrency(),
                            po.getStatus().name(),
                            ackStatus,
                            promisedDate,
                            facilityName,
                            po.getLineItems() != null ? po.getLineItems().size() : 0,
                            po.getIssuedAt(),
                            po.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VendorOrderDetailResponse getVendorOrderDetail(UUID orderId, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        PurchaseOrder po = getVendorOrderOrThrow(orderId, user);
        UUID tenantId = user.getTenant().getId();

        VendorOrderDetailResponse.DeliveryFacilityDto facilityDto = null;
        if (po.getDeliveryFacility() != null) {
            facilityDto = new VendorOrderDetailResponse.DeliveryFacilityDto(
                    po.getDeliveryFacility().getId(),
                    po.getDeliveryFacility().getName(),
                    po.getDeliveryFacility().getFacilityCode(),
                    po.getDeliveryFacility().getShippingAddress()
            );
        }

        List<VendorOrderDetailResponse.LineItemDto> lineItemsDto = po.getLineItems().stream()
                .map(li -> new VendorOrderDetailResponse.LineItemDto(
                        li.getId(),
                        li.getLineNumber(),
                        li.getItemDescription(),
                        li.getItemCategory(),
                        li.getQuantity(),
                        li.getUnitOfMeasure(),
                        li.getUnitPrice(),
                        li.getTotalPrice(),
                        li.getEstimatedDeliveryDate()
                ))
                .toList();

        Optional<VendorPoAcknowledgment> latestAck = acknowledgmentRepository
                .findTopByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(tenantId, po.getId());

        VendorOrderDetailResponse.AcknowledgmentDto ackDto = latestAck.map(a -> new VendorOrderDetailResponse.AcknowledgmentDto(
                a.getId(),
                a.getStatus().name(),
                a.getPromisedDeliveryDate(),
                a.getVendorNotes(),
                a.getAcknowledgedByUser().getFullName(),
                a.getCreatedAt()
        )).orElse(null);

        List<VendorAsnResponse> asnShipments = asnShipmentRepository
                .findAllByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(tenantId, po.getId())
                .stream()
                .map(this::toAsnResponse)
                .toList();

        return new VendorOrderDetailResponse(
                po.getId(),
                po.getPoNumber(),
                po.getTotalAmount(),
                po.getCurrency(),
                po.getStatus().name(),
                po.getIncoterms() != null ? po.getIncoterms().name() : null,
                po.getPaymentTerms() != null ? po.getPaymentTerms().name() : null,
                po.getNotes(),
                facilityDto,
                lineItemsDto,
                ackDto,
                asnShipments,
                po.getIssuedAt(),
                po.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public VendorOrderDetailResponse.AcknowledgmentDto acknowledgeOrder(UUID orderId, VendorPoAcknowledgmentRequest request, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        PurchaseOrder po = getVendorOrderOrThrow(orderId, user);

        TenantContext.setTenantId(user.getTenant().getId());

        VendorPoAcknowledgment ack = new VendorPoAcknowledgment(
                user.getTenant(),
                po,
                user.getVendor(),
                user,
                request.status(),
                request.promisedDeliveryDate(),
                request.vendorNotes()
        );

        VendorPoAcknowledgment saved = acknowledgmentRepository.save(ack);

        // Audit Trail (ISO 27001 & SOX 404)
        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.PURCHASE_ORDER_ISSUED,
                ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                user.getId(),
                user.getEmail(),
                "VENDOR_ADMIN",
                "127.0.0.1",
                "VendorPortal",
                "PurchaseOrder",
                po.getId().toString(),
                po.getLegalEntity() != null ? po.getLegalEntity().getId() : null,
                po.getCostCenter() != null ? po.getCostCenter().getId() : null,
                po.getTotalAmount(),
                po.getCurrency(),
                po.getStatus().name(),
                po.getStatus().name(),
                "Vendor " + user.getVendor().getName() + " acknowledged order " + po.getPoNumber() + " -> Status: " + request.status(),
                "{\"acknowledgmentStatus\":\"" + request.status() + "\",\"promisedDate\":\"" + request.promisedDeliveryDate() + "\"}"
        ));

        return new VendorOrderDetailResponse.AcknowledgmentDto(
                saved.getId(),
                saved.getStatus().name(),
                saved.getPromisedDeliveryDate(),
                saved.getVendorNotes(),
                user.getFullName(),
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public VendorAsnResponse dispatchAsnShipment(UUID orderId, VendorAsnDispatchRequest request, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        PurchaseOrder po = getVendorOrderOrThrow(orderId, user);
        UUID tenantId = user.getTenant().getId();

        TenantContext.setTenantId(tenantId);

        if (asnShipmentRepository.existsByTenantIdAndWaybillNumber(tenantId, request.waybillNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An ASN / e-Waybill with waybill number '" + request.waybillNumber() + "' already exists");
        }

        VendorAsnShipment shipment = new VendorAsnShipment(
                user.getTenant(),
                po,
                user.getVendor(),
                user,
                request.waybillNumber(),
                request.ettn() != null ? request.ettn() : UUID.randomUUID().toString(),
                request.carrierName(),
                request.trackingNumber(),
                request.vehiclePlate(),
                request.driverNationalId(),
                request.driverName(),
                request.driverPhone(),
                request.shipmentDate(),
                request.estimatedArrivalDate(),
                request.notes()
        );

        // Add Line items
        if (request.lineItems() != null && !request.lineItems().isEmpty()) {
            for (VendorAsnDispatchRequest.AsnLineItemDispatchDto itemDto : request.lineItems()) {
                PurchaseOrderLineItem poLine = po.getLineItems().stream()
                        .filter(li -> li.getId().equals(itemDto.purchaseOrderLineItemId()))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PO Line Item ID: " + itemDto.purchaseOrderLineItemId()));

                BigDecimal shippedQty = itemDto.shippedQuantity();
                if (shippedQty == null || shippedQty.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shipped quantity must be positive");
                }

                VendorAsnShipmentLineItem lineItem = new VendorAsnShipmentLineItem(
                        poLine,
                        shippedQty,
                        poLine.getUnitOfMeasure(),
                        itemDto.lotNumber(),
                        itemDto.serialNumbers()
                );
                shipment.addLineItem(lineItem);
            }
        } else {
            // Auto dispatch full PO line items
            for (PurchaseOrderLineItem poLine : po.getLineItems()) {
                VendorAsnShipmentLineItem lineItem = new VendorAsnShipmentLineItem(
                        poLine,
                        poLine.getQuantity(),
                        poLine.getUnitOfMeasure(),
                        null,
                        null
                );
                shipment.addLineItem(lineItem);
            }
        }

        VendorAsnShipment saved = asnShipmentRepository.save(shipment);

        // Update PO status to PARTIALLY_RECEIVED if it was ISSUED
        if (po.getStatus() == PurchaseOrderStatus.ISSUED) {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
            purchaseOrderRepository.save(po);
        }

        // Security / Privacy: Mask Driver National ID (TCKN) for Audit Logs
        String maskedTckn = request.driverNationalId() != null
                ? MaskingUtils.maskNationalId(request.driverNationalId())
                : null;

        // Audit Trail (SOX 404 & ISO 27001)
        auditService.recordAuditLog(new RecordAuditRequest(
                UUID.randomUUID().toString(),
                AuditAction.VENDOR_ASN_DISPATCHED,
                ComplianceTag.SOX_404_FINANCIAL_CONTROL,
                user.getId(),
                user.getEmail(),
                "VENDOR_ADMIN",
                "127.0.0.1",
                "VendorPortal",
                "VendorAsnShipment",
                saved.getId().toString(),
                po.getLegalEntity() != null ? po.getLegalEntity().getId() : null,
                po.getCostCenter() != null ? po.getCostCenter().getId() : null,
                po.getTotalAmount(),
                po.getCurrency(),
                "ISSUED",
                "DISPATCHED",
                "Vendor " + user.getVendor().getName() + " dispatched ASN / e-Waybill [" + request.waybillNumber() + "] for PO [" + po.getPoNumber() + "]",
                "{\"waybillNumber\":\"" + request.waybillNumber() + "\",\"carrier\":\"" + request.carrierName() + "\",\"trackingNumber\":\"" + request.trackingNumber() + "\",\"driverNationalIdMasked\":\"" + maskedTckn + "\"}"
        ));

        return toAsnResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorAsnResponse> getOrderAsnShipments(UUID orderId, UUID vendorUserId) {
        VendorUser user = getVendorUser(vendorUserId);
        PurchaseOrder po = getVendorOrderOrThrow(orderId, user);
        return asnShipmentRepository
                .findAllByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(user.getTenant().getId(), po.getId())
                .stream()
                .map(this::toAsnResponse)
                .toList();
    }

    private VendorUser getVendorUser(UUID vendorUserId) {
        return vendorUserRepository.findById(vendorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor user not found"));
    }

    private PurchaseOrder getVendorOrderOrThrow(UUID orderId, VendorUser user) {
        return purchaseOrderRepository
                .findByIdAndTenantIdAndVendorId(orderId, user.getTenant().getId(), user.getVendor().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase Order not found or not assigned to your vendor"));
    }

    private VendorAsnResponse toAsnResponse(VendorAsnShipment s) {
        String maskedTckn = s.getDriverNationalId() != null ? MaskingUtils.maskTaxNumber(s.getDriverNationalId()) : null;

        List<VendorAsnResponse.AsnLineItemResponse> lineItems = s.getLineItems().stream()
                .map(li -> new VendorAsnResponse.AsnLineItemResponse(
                        li.getId(),
                        li.getPurchaseOrderLineItem().getId(),
                        li.getPurchaseOrderLineItem().getLineNumber(),
                        li.getPurchaseOrderLineItem().getItemDescription(),
                        li.getShippedQuantity(),
                        li.getUnitOfMeasure(),
                        li.getLotNumber(),
                        li.getSerialNumbers()
                ))
                .toList();

        return new VendorAsnResponse(
                s.getId(),
                s.getPurchaseOrder().getId(),
                s.getPurchaseOrder().getPoNumber(),
                s.getWaybillNumber(),
                s.getEttn(),
                s.getCarrierName(),
                s.getTrackingNumber(),
                s.getVehiclePlate(),
                s.getDriverName(),
                maskedTckn,
                s.getDriverPhone(),
                s.getShipmentDate(),
                s.getEstimatedArrivalDate(),
                s.getStatus().name(),
                s.getNotes(),
                s.getDispatchedByUser().getFullName(),
                lineItems,
                s.getCreatedAt()
        );
    }
}
