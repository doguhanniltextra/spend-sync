package com.enterprise.spendsync.vendorportal.service;

import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderLineItemRepository;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.vendorportal.dto.VendorAsnDispatchRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorAsnResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorOrderDetailResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorOrderSummaryResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorPoAcknowledgmentRequest;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorAsnShipment;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorPoAcknowledgment;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorPoAcknowledgmentStatus;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorUser;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorAsnShipmentRepository;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorPoAcknowledgmentRepository;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorUserRepository;
import com.enterprise.spendsync.vendorportal.internal.service.VendorOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorOrderServiceTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderLineItemRepository purchaseOrderLineItemRepository;
    @Mock private VendorUserRepository vendorUserRepository;
    @Mock private VendorPoAcknowledgmentRepository acknowledgmentRepository;
    @Mock private VendorAsnShipmentRepository asnShipmentRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private VendorOrderServiceImpl vendorOrderService;

    private UUID tenantId;
    private UUID vendorId;
    private UUID vendorUserId;
    private UUID orderId;
    private Tenant tenant;
    private Vendor vendor;
    private VendorUser vendorUser;
    private PurchaseOrder purchaseOrder;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        vendorUserId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        tenant = new Tenant("Test Corp", "test-corp");
        tenant.setId(tenantId);

        vendor = new Vendor(tenant, "Test Vendor", "1234567890", "Istanbul", VendorCategory.IT_HARDWARE,
                VendorTier.TIER_1_STRATEGIC, true, "vendor@test.com", "555-0000",
                "Vendor Addr", "Istanbul", "TR", PaymentTerms.NET_30, "Test Bank", "TR123456789");
        vendor.setId(vendorId);

        vendorUser = new VendorUser(tenant, vendor, "vendor@test.com", "hash", "Vendor User", "555-0001",
                RoleType.VENDOR_ADMIN, true);
        vendorUser.setId(vendorUserId);

        purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(orderId);
        purchaseOrder.setVendor(vendor);
        purchaseOrder.setStatus(PurchaseOrderStatus.ISSUED);
    }

    // =====================================================
    // getVendorOrders: Branch tests
    // =====================================================

    @Test
    @DisplayName("Should return filtered orders when status is provided")
    void shouldReturnOrdersFilteredByStatus() {
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findAllByTenantIdAndVendorIdAndStatusOrderByCreatedAtDesc(
                tenantId, vendorId, PurchaseOrderStatus.ISSUED))
                .thenReturn(List.of(purchaseOrder));
        when(acknowledgmentRepository.findTopByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        List<VendorOrderSummaryResponse> result = vendorOrderService.getVendorOrders(PurchaseOrderStatus.ISSUED, vendorUserId);

        assertThat(result).hasSize(1);
        verify(purchaseOrderRepository).findAllByTenantIdAndVendorIdAndStatusOrderByCreatedAtDesc(tenantId, vendorId, PurchaseOrderStatus.ISSUED);
    }

    @Test
    @DisplayName("Should return all orders when status is null")
    void shouldReturnAllOrdersWhenStatusNull() {
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId))
                .thenReturn(List.of(purchaseOrder));
        when(acknowledgmentRepository.findTopByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        List<VendorOrderSummaryResponse> result = vendorOrderService.getVendorOrders(null, vendorUserId);

        assertThat(result).hasSize(1);
        verify(purchaseOrderRepository).findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId);
    }

    @Test
    @DisplayName("Should include acknowledgment status and promised date when ack present")
    void shouldReturnAckStatusFromLatestAcknowledgment() {
        VendorPoAcknowledgment ack = new VendorPoAcknowledgment(
                tenant, purchaseOrder, vendor, vendorUser,
                VendorPoAcknowledgmentStatus.ACCEPTED, LocalDate.of(2026, 9, 1), "OK");

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId))
                .thenReturn(List.of(purchaseOrder));
        when(acknowledgmentRepository.findTopByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(ack));

        List<VendorOrderSummaryResponse> result = vendorOrderService.getVendorOrders(null, vendorUserId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).acknowledgmentStatus()).isEqualTo("ACCEPTED");
        assertThat(result.get(0).promisedDeliveryDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("Should return PENDING ack status when no acknowledgment exists")
    void shouldDefaultAckStatusToPendingWhenNoAckExists() {
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId))
                .thenReturn(List.of(purchaseOrder));
        when(acknowledgmentRepository.findTopByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        List<VendorOrderSummaryResponse> result = vendorOrderService.getVendorOrders(null, vendorUserId);

        assertThat(result.get(0).acknowledgmentStatus()).isEqualTo("PENDING");
        assertThat(result.get(0).promisedDeliveryDate()).isNull();
    }

    @Test
    @DisplayName("Should throw 404 when vendor user not found on getVendorOrders")
    void shouldThrowWhenVendorUserNotFoundOnGetOrders() {
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorOrderService.getVendorOrders(null, vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vendor user not found");
    }

    // =====================================================
    // getVendorOrderDetail: Branch tests
    // =====================================================

    @Test
    @DisplayName("Should return order detail with null facility when deliveryFacility is null")
    void shouldReturnDetailWithNullFacilityWhenMissing() {
        purchaseOrder.setDeliveryFacility(null);
        purchaseOrder.setIncoterms(null);
        purchaseOrder.setPaymentTerms(null);

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(orderId, tenantId, vendorId))
                .thenReturn(Optional.of(purchaseOrder));
        when(acknowledgmentRepository.findTopByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(asnShipmentRepository.findAllByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());

        VendorOrderDetailResponse result = vendorOrderService.getVendorOrderDetail(orderId, vendorUserId);

        assertThat(result.deliveryFacility()).isNull();
        assertThat(result.latestAcknowledgment()).isNull();
        assertThat(result.asnShipments()).isEmpty();
    }

    @Test
    @DisplayName("Should throw 404 when purchase order not found for vendor")
    void shouldThrowWhenOrderNotFoundForVendor() {
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(orderId, tenantId, vendorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorOrderService.getVendorOrderDetail(orderId, vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Purchase Order not found");
    }

    // =====================================================
    // acknowledgeOrder
    // =====================================================

    @Test
    @DisplayName("Should save and return acknowledgment DTO on successful acknowledge")
    void shouldAcknowledgeOrderSuccessfully() {
        VendorPoAcknowledgmentRequest request = new VendorPoAcknowledgmentRequest(
                VendorPoAcknowledgmentStatus.ACCEPTED, LocalDate.of(2026, 9, 15), "Onaylandı");

        VendorPoAcknowledgment savedAck = new VendorPoAcknowledgment(
                tenant, purchaseOrder, vendor, vendorUser,
                VendorPoAcknowledgmentStatus.ACCEPTED, LocalDate.of(2026, 9, 15), "Onaylandı");

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(orderId, tenantId, vendorId))
                .thenReturn(Optional.of(purchaseOrder));
        when(acknowledgmentRepository.save(any())).thenReturn(savedAck);

        VendorOrderDetailResponse.AcknowledgmentDto result =
                vendorOrderService.acknowledgeOrder(orderId, request, vendorUserId);

        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(result.promisedDeliveryDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(result.vendorNotes()).isEqualTo("Onaylandı");
        verify(auditService).recordAuditLog(any());
    }

    // =====================================================
    // dispatchAsnShipment: Branch tests
    // =====================================================

    @Test
    @DisplayName("Should throw CONFLICT when waybill number already exists")
    void shouldThrowConflictWhenDuplicateWaybillNumber() {
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(orderId, tenantId, vendorId))
                .thenReturn(Optional.of(purchaseOrder));
        when(asnShipmentRepository.existsByTenantIdAndWaybillNumber(eq(tenantId), eq("WB-001")))
                .thenReturn(true);

        VendorAsnDispatchRequest request = new VendorAsnDispatchRequest(
                "WB-001", null, "Carrier A", "TRK-001", "34ABC123",
                "12345678901", "Driver Name", "555-0001",
                LocalDate.now(), LocalDate.now().plusDays(3), null, null);

        assertThatThrownBy(() -> vendorOrderService.dispatchAsnShipment(orderId, request, vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should auto-dispatch full PO lines when no line items specified in request")
    void shouldAutoDispatchFullPoLinesWhenNoLineItemsInRequest() {
        PurchaseOrderLineItem poLine = new PurchaseOrderLineItem(
                tenant, purchaseOrder, null, 1, "Item", "CAT",
                new BigDecimal("10"), "EA", new BigDecimal("100.00"), null, null, null);
        poLine.setId(UUID.randomUUID());
        purchaseOrder.addLineItem(poLine);
        purchaseOrder.setStatus(PurchaseOrderStatus.ISSUED);

        VendorAsnShipment savedShipment = new VendorAsnShipment(
                tenant, purchaseOrder, vendor, vendorUser,
                "WB-002", "ETTN-2", "Carrier B", "TRK-002", "34XYZ999",
                null, "Driver B", "555-0002",
                LocalDate.now(), LocalDate.now().plusDays(2), null);
        ReflectionTestUtils.setField(savedShipment, "id", UUID.randomUUID());

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(orderId, tenantId, vendorId))
                .thenReturn(Optional.of(purchaseOrder));
        when(asnShipmentRepository.existsByTenantIdAndWaybillNumber(any(), any())).thenReturn(false);
        when(asnShipmentRepository.save(any())).thenReturn(savedShipment);

        VendorAsnDispatchRequest request = new VendorAsnDispatchRequest(
                "WB-002", null, "Carrier B", "TRK-002", "34XYZ999",
                null, "Driver B", "555-0002",
                LocalDate.now(), LocalDate.now().plusDays(2), null, null);

        VendorAsnResponse result = vendorOrderService.dispatchAsnShipment(orderId, request, vendorUserId);

        assertThat(result).isNotNull();
        assertThat(result.waybillNumber()).isEqualTo("WB-002");
        verify(purchaseOrderRepository).save(purchaseOrder);
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when line item ID is invalid in dispatch request")
    void shouldThrowWhenInvalidPoLineItemIdInDispatchRequest() {
        PurchaseOrderLineItem poLine = new PurchaseOrderLineItem(
                tenant, purchaseOrder, null, 1, "Item", "CAT",
                new BigDecimal("10"), "EA", new BigDecimal("100.00"), null, null, null);
        poLine.setId(UUID.randomUUID());
        purchaseOrder.addLineItem(poLine);

        UUID unknownLineId = UUID.randomUUID();
        VendorAsnDispatchRequest.AsnLineItemDispatchDto lineDto =
                new VendorAsnDispatchRequest.AsnLineItemDispatchDto(unknownLineId, new BigDecimal("5"), "LOT1", null);

        VendorAsnDispatchRequest request = new VendorAsnDispatchRequest(
                "WB-003", null, "Carrier C", "TRK-003", "34AAA001",
                "12345678901", "Driver C", "555-0003",
                LocalDate.now(), LocalDate.now().plusDays(1), null, List.of(lineDto));

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(orderId, tenantId, vendorId))
                .thenReturn(Optional.of(purchaseOrder));
        when(asnShipmentRepository.existsByTenantIdAndWaybillNumber(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> vendorOrderService.dispatchAsnShipment(orderId, request, vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid PO Line Item ID");
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when shipped quantity is zero or negative")
    void shouldThrowWhenShippedQuantityIsZeroOrNegative() {
        UUID lineId = UUID.randomUUID();
        PurchaseOrderLineItem poLine = new PurchaseOrderLineItem(
                tenant, purchaseOrder, null, 1, "Item", "CAT",
                new BigDecimal("10"), "EA", new BigDecimal("100.00"), null, null, null);
        poLine.setId(lineId);
        purchaseOrder.addLineItem(poLine);

        VendorAsnDispatchRequest.AsnLineItemDispatchDto lineDto =
                new VendorAsnDispatchRequest.AsnLineItemDispatchDto(lineId, BigDecimal.ZERO, null, null);

        VendorAsnDispatchRequest request = new VendorAsnDispatchRequest(
                "WB-004", null, "Carrier D", "TRK-004", "34BBB002",
                "12345678901", "Driver D", "555-0004",
                LocalDate.now(), LocalDate.now().plusDays(1), null, List.of(lineDto));

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(orderId, tenantId, vendorId))
                .thenReturn(Optional.of(purchaseOrder));
        when(asnShipmentRepository.existsByTenantIdAndWaybillNumber(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> vendorOrderService.dispatchAsnShipment(orderId, request, vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Shipped quantity must be positive");
    }

    // =====================================================
    // getOrderAsnShipments
    // =====================================================

    @Test
    @DisplayName("Should return ASN shipments for a valid order")
    void shouldReturnAsnShipments() {
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(orderId, tenantId, vendorId))
                .thenReturn(Optional.of(purchaseOrder));
        when(asnShipmentRepository.findAllByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());

        List<VendorAsnResponse> result = vendorOrderService.getOrderAsnShipments(orderId, vendorUserId);

        assertThat(result).isEmpty();
    }
}
