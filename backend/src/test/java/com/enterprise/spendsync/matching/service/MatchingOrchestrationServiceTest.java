package com.enterprise.spendsync.matching.service;

import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.service.BudgetService;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.FacilityType;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import com.enterprise.spendsync.matching.internal.domain.*;
import com.enterprise.spendsync.matching.internal.dto.*;
import com.enterprise.spendsync.matching.internal.event.InvoiceMatchedEvent;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceLineItemRepository;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.matching.internal.service.MatchingServiceImpl;
import com.enterprise.spendsync.purchasing.internal.domain.*;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderLineItemRepository;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceipt;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceiptLineItem;
import com.enterprise.spendsync.receiving.internal.repository.GoodsReceiptLineItemRepository;
import com.enterprise.spendsync.requisition.internal.domain.PurchaseRequisition;
import com.enterprise.spendsync.requisition.internal.domain.RequisitionStatus;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchingOrchestrationService Unit & Mock Tests (Touchless 3-Way & 2-Way Matching Engine)")
class MatchingOrchestrationServiceTest {

    @Mock
    private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock
    private SupplierInvoiceLineItemRepository supplierInvoiceLineItemRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PurchaseOrderLineItemRepository purchaseOrderLineItemRepository;
    @Mock
    private GoodsReceiptLineItemRepository goodsReceiptLineItemRepository;
    @Mock
    private BudgetService budgetService;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MatchingServiceImpl matchingService;

    private UUID tenantId;
    private Tenant tenant;
    private LegalEntity legalEntity;
    private CostCenter costCenter;
    private Facility facility;
    private Vendor vendor;
    private User accountant;
    private BudgetPool budgetPool;
    private PurchaseRequisition requisition;
    private PurchaseOrder po;
    private PurchaseOrderLineItem poLine;
    private GoodsReceiptLineItem grLine;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("SpendSync Global");

        legalEntity = new LegalEntity(tenant, "SpendSync Turkey", "TR01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-IT", "IT Department");
        costCenter.setId(UUID.randomUUID());

        facility = new Facility(tenant, legalEntity, "Main Warehouse", "FAC-01", FacilityType.WAREHOUSE, "Gebze OSB");
        facility.setId(UUID.randomUUID());

        vendor = new Vendor(
                tenant, "Global IT Hardware Inc.", "9998887776", "Maslak",
                VendorCategory.IT_HARDWARE, VendorTier.TIER_1_STRATEGIC, true,
                "finance@globalit.com", "+90 212 999 0000", "Maslak", "Istanbul", "TR",
                PaymentTerms.NET_30, "Garanti BBVA", "TR1122334455"
        );
        vendor.setId(UUID.randomUUID());

        accountant = new User("accountant@spendsync.com", "pass", "AP", "Accountant", null, "TR");
        accountant.setId(UUID.randomUUID());

        budgetPool = new BudgetPool(
                tenant, legalEntity, costCenter, 2026, com.enterprise.spendsync.budget.internal.domain.BudgetPeriodType.ANNUAL,
                "2026", com.enterprise.spendsync.budget.internal.domain.BudgetStatus.ACTIVE,
                com.enterprise.spendsync.budget.internal.domain.BudgetEnforcementMode.HARD_STOP,
                BigDecimal.ZERO, new BigDecimal("1000000.00"), "TRY"
        );
        budgetPool.setId(UUID.randomUUID());

        requisition = new PurchaseRequisition(
                tenant, "PR-2026-00001", accountant, legalEntity, costCenter, facility,
                budgetPool, RequisitionStatus.APPROVED, new BigDecimal("100000.00"), "TRY", "IT Gear", "Hardware"
        );
        requisition.setId(UUID.randomUUID());

        po = new PurchaseOrder(
                tenant, "PO-2026-00001", requisition, legalEntity, costCenter, facility,
                vendor, Incoterms.DAP, "TRY", PaymentTerms.NET_30, null, accountant
        );
        po.setId(UUID.randomUUID());
        po.setStatus(PurchaseOrderStatus.ISSUED);

        poLine = new PurchaseOrderLineItem(
                tenant, po, null, 1, "Server Rack 42U", "IT_HARDWARE",
                new BigDecimal("10.0000"), "PIECE", new BigDecimal("10000.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(10)
        );
        poLine.setId(UUID.randomUUID());
        po.addLineItem(poLine);

        GoodsReceipt gr = new GoodsReceipt(
                tenant, "GR-2026-00001", po, facility, "IRS-01", LocalDate.now(), accountant, null
        );
        gr.setId(UUID.randomUUID());

        grLine = new GoodsReceiptLineItem(
                tenant, poLine, new BigDecimal("10.0000"), new BigDecimal("10.0000"),
                BigDecimal.ZERO, null, null
        );
        grLine.setId(UUID.randomUUID());
        gr.addLineItem(grLine);

        UserPrincipal principal = new UserPrincipal(
                accountant.getId(), tenantId, null, "ACCOUNTANT", accountant.getEmail(), null, "AP Accountant", true, Set.of(), Set.of()
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
    @DisplayName("TC-07-01: Perfect match produces AUTO_MATCHED, APPROVED_FOR_PAYMENT and commits budget")
    void shouldAutoMatchInvoiceOnExactMatch() {
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                po.getId(),
                "INV-2026-001",
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                LocalDate.now(),
                InvoiceType.SATIS,
                InvoiceProfile.TICARI_FATURA,
                List.of(new CreateInvoiceLineItemRequest(
                        poLine.getId(),
                        grLine.getId(),
                        new BigDecimal("10.0000"),
                        new BigDecimal("10000.0000"),
                        new BigDecimal("20.00")
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(supplierInvoiceRepository.findByTenantIdAndEttn(tenantId, request.ettn())).thenReturn(Optional.empty());
        when(purchaseOrderRepository.findByIdAndTenantId(po.getId(), tenantId)).thenReturn(Optional.of(po));
        when(supplierInvoiceRepository.findByTenantIdAndVendorIdAndInvoiceNumber(tenantId, vendor.getId(), request.invoiceNumber())).thenReturn(Optional.empty());
        when(purchaseOrderLineItemRepository.findById(poLine.getId())).thenReturn(Optional.of(poLine));
        when(goodsReceiptLineItemRepository.findById(grLine.getId())).thenReturn(Optional.of(grLine));
        when(goodsReceiptLineItemRepository.sumAcceptedQuantityByPoLineId(poLine.getId())).thenReturn(new BigDecimal("10.0000"));

        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        SupplierInvoiceResponse response = matchingService.createAndEvaluateInvoice(request);

        assertThat(response).isNotNull();
        assertThat(response.matchStatus()).isEqualTo(InvoiceMatchStatus.AUTO_MATCHED);
        assertThat(response.status()).isEqualTo(InvoiceStatus.APPROVED_FOR_PAYMENT);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("120000.0000"));

        verify(budgetService).commitBudget(
                eq(budgetPool.getId()),
                argThat(amt -> amt.compareTo(new BigDecimal("120000.0000")) == 0),
                any(),
                eq("3WAY_MATCH_AUTO_COMMIT"),
                contains("Automated 3-Way Match approved")
        );

        verify(eventPublisher).publishEvent(any(InvoiceMatchedEvent.class));
    }

    @Test
    @DisplayName("TC-07-02: Duplicate ETTN is rejected with 409 Conflict")
    void shouldRejectDuplicateEttn() {
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                po.getId(), "INV-001", "dup-ettn", LocalDate.now(), null, null, List.of()
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(supplierInvoiceRepository.findByTenantIdAndEttn(tenantId, "dup-ettn"))
                .thenReturn(Optional.of(new SupplierInvoice()));

        assertThatThrownBy(() -> matchingService.createAndEvaluateInvoice(request))
                .isInstanceOf(SpendSyncException.class)
                .satisfies(ex -> {
                    SpendSyncException sex = (SpendSyncException) ex;
                    assertThat(sex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(sex.getErrorCode()).isEqualTo("DUPLICATE_INVOICE_ETTN");
                });
    }

    @Test
    @DisplayName("TC-07-05: Price variance exceeding tolerance puts invoice on DISCREPANCY_HOLD")
    void shouldPutOnDiscrepancyHoldWhenPriceExceedsTolerance() {
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                po.getId(),
                "INV-2026-002",
                "ettn-price-var",
                LocalDate.now(),
                InvoiceType.SATIS,
                InvoiceProfile.TICARI_FATURA,
                List.of(new CreateInvoiceLineItemRequest(
                        poLine.getId(),
                        grLine.getId(),
                        new BigDecimal("10.0000"),
                        new BigDecimal("10500.0000"), // 5% price variance (> 1% tolerance)
                        new BigDecimal("20.00")
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(supplierInvoiceRepository.findByTenantIdAndEttn(tenantId, request.ettn())).thenReturn(Optional.empty());
        when(purchaseOrderRepository.findByIdAndTenantId(po.getId(), tenantId)).thenReturn(Optional.of(po));
        when(supplierInvoiceRepository.findByTenantIdAndVendorIdAndInvoiceNumber(tenantId, vendor.getId(), request.invoiceNumber())).thenReturn(Optional.empty());
        when(purchaseOrderLineItemRepository.findById(poLine.getId())).thenReturn(Optional.of(poLine));
        when(goodsReceiptLineItemRepository.findById(grLine.getId())).thenReturn(Optional.of(grLine));
        when(goodsReceiptLineItemRepository.sumAcceptedQuantityByPoLineId(poLine.getId())).thenReturn(new BigDecimal("10.0000"));

        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        SupplierInvoiceResponse response = matchingService.createAndEvaluateInvoice(request);

        assertThat(response.matchStatus()).isEqualTo(InvoiceMatchStatus.DISCREPANCY_HOLD);
        assertThat(response.status()).isEqualTo(InvoiceStatus.SUBMITTED);
        assertThat(response.discrepancyReason()).contains("Price Discrepancy");

        // Should NOT commit budget on discrepancy
        verify(budgetService, never()).commitBudget(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-07-06: Quantity variance puts invoice on DISCREPANCY_HOLD")
    void shouldPutOnDiscrepancyHoldWhenQuantityExceedsAccepted() {
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                po.getId(),
                "INV-2026-003",
                "ettn-qty-var",
                LocalDate.now(),
                InvoiceType.SATIS,
                InvoiceProfile.TICARI_FATURA,
                List.of(new CreateInvoiceLineItemRequest(
                        poLine.getId(),
                        grLine.getId(),
                        new BigDecimal("12.0000"), // Invoiced 12 > Accepted 10
                        new BigDecimal("10000.0000"),
                        new BigDecimal("20.00")
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(supplierInvoiceRepository.findByTenantIdAndEttn(tenantId, request.ettn())).thenReturn(Optional.empty());
        when(purchaseOrderRepository.findByIdAndTenantId(po.getId(), tenantId)).thenReturn(Optional.of(po));
        when(supplierInvoiceRepository.findByTenantIdAndVendorIdAndInvoiceNumber(tenantId, vendor.getId(), request.invoiceNumber())).thenReturn(Optional.empty());
        when(purchaseOrderLineItemRepository.findById(poLine.getId())).thenReturn(Optional.of(poLine));
        when(goodsReceiptLineItemRepository.findById(grLine.getId())).thenReturn(Optional.of(grLine));
        when(goodsReceiptLineItemRepository.sumAcceptedQuantityByPoLineId(poLine.getId())).thenReturn(new BigDecimal("10.0000"));

        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        SupplierInvoiceResponse response = matchingService.createAndEvaluateInvoice(request);

        assertThat(response.matchStatus()).isEqualTo(InvoiceMatchStatus.DISCREPANCY_HOLD);
        assertThat(response.status()).isEqualTo(InvoiceStatus.SUBMITTED);
        assertThat(response.discrepancyReason()).contains("Quantity Discrepancy");
    }

    @Test
    @DisplayName("TC-07-07: 2-Way Match for SERVICE items evaluates against PO quantity without requiring GRN")
    void shouldPerformTwoWayMatchForServiceItems() {
        PurchaseOrderLineItem serviceLine = new PurchaseOrderLineItem(
                tenant, po, null, 2, "AWS Cloud Hosting Annual", "SERVICE",
                new BigDecimal("1.0000"), "SERVICE", new BigDecimal("50000.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().plusDays(30)
        );
        serviceLine.setId(UUID.randomUUID());

        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                po.getId(),
                "INV-2026-004",
                "ettn-service",
                LocalDate.now(),
                InvoiceType.SATIS,
                InvoiceProfile.TICARI_FATURA,
                List.of(new CreateInvoiceLineItemRequest(
                        serviceLine.getId(),
                        null, // No GRN needed for service
                        new BigDecimal("1.0000"),
                        new BigDecimal("50000.0000"),
                        new BigDecimal("20.00")
                ))
        );

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(supplierInvoiceRepository.findByTenantIdAndEttn(tenantId, request.ettn())).thenReturn(Optional.empty());
        when(purchaseOrderRepository.findByIdAndTenantId(po.getId(), tenantId)).thenReturn(Optional.of(po));
        when(supplierInvoiceRepository.findByTenantIdAndVendorIdAndInvoiceNumber(tenantId, vendor.getId(), request.invoiceNumber())).thenReturn(Optional.empty());
        when(purchaseOrderLineItemRepository.findById(serviceLine.getId())).thenReturn(Optional.of(serviceLine));

        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        SupplierInvoiceResponse response = matchingService.createAndEvaluateInvoice(request);

        assertThat(response.matchStatus()).isEqualTo(InvoiceMatchStatus.AUTO_MATCHED);
        assertThat(response.status()).isEqualTo(InvoiceStatus.APPROVED_FOR_PAYMENT);

        // Goods receipt line repository should NOT be queried for SERVICE items
        verify(goodsReceiptLineItemRepository, never()).sumAcceptedQuantityByPoLineId(serviceLine.getId());
    }

    @Test
    @DisplayName("TC-07-08: Manager Override transitions DISCREPANCY_HOLD invoice to MANUALLY_MATCHED and commits budget")
    void shouldManagerOverrideInvoice() {
        SupplierInvoice heldInvoice = new SupplierInvoice(
                tenant, "INV-HELD-001", "ettn-held", LocalDate.now(), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                po, vendor, legalEntity, costCenter, "TRY",
                new BigDecimal("100000.00"), new BigDecimal("20000.00"), new BigDecimal("120000.00")
        );
        heldInvoice.setId(UUID.randomUUID());
        heldInvoice.setMatchStatus(InvoiceMatchStatus.DISCREPANCY_HOLD);

        ManagerOverrideRequest overrideRequest = new ManagerOverrideRequest("Price increase approved by VP of Procurement via email");

        when(userRepository.findByIdAndTenantId(accountant.getId(), tenantId)).thenReturn(Optional.of(accountant));
        when(supplierInvoiceRepository.findByIdAndTenantId(heldInvoice.getId(), tenantId)).thenReturn(Optional.of(heldInvoice));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(i -> i.getArgument(0));

        SupplierInvoiceResponse response = matchingService.managerOverride(heldInvoice.getId(), overrideRequest);

        assertThat(response.matchStatus()).isEqualTo(InvoiceMatchStatus.MANUALLY_MATCHED);
        assertThat(response.status()).isEqualTo(InvoiceStatus.APPROVED_FOR_PAYMENT);

        verify(budgetService).commitBudget(
                eq(budgetPool.getId()),
                argThat(amt -> amt.compareTo(new BigDecimal("120000.00")) == 0),
                eq(heldInvoice.getId()),
                eq("3WAY_MATCH_MANUAL_OVERRIDE_COMMIT"),
                contains("Manager override approved")
        );
    }

    @Test
    @DisplayName("TC-07-09: Commercial rejection marks invoice as REJECTED and CANCELLED")
    void shouldRejectInvoiceCommercially() {
        SupplierInvoice heldInvoice = new SupplierInvoice(
                tenant, "INV-HELD-002", "ettn-reject", LocalDate.now(), InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA,
                po, vendor, legalEntity, costCenter, "TRY",
                new BigDecimal("100000.00"), new BigDecimal("20000.00"), new BigDecimal("120000.00")
        );
        heldInvoice.setId(UUID.randomUUID());

        RejectInvoiceRequest rejectRequest = new RejectInvoiceRequest("Wrong tax rate applied (10% instead of 20%)");

        when(supplierInvoiceRepository.findByIdAndTenantId(heldInvoice.getId(), tenantId)).thenReturn(Optional.of(heldInvoice));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(i -> i.getArgument(0));

        SupplierInvoiceResponse response = matchingService.rejectInvoice(heldInvoice.getId(), rejectRequest);

        assertThat(response.matchStatus()).isEqualTo(InvoiceMatchStatus.REJECTED);
        assertThat(response.status()).isEqualTo(InvoiceStatus.CANCELLED);
    }
}
