package com.enterprise.spendsync.vendorportal.service;

import com.enterprise.spendsync.audit.internal.service.AuditService;
import com.enterprise.spendsync.core.internal.domain.CostCenter;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.matching.internal.domain.InvoiceDiscrepancy;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceProfile;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceType;
import com.enterprise.spendsync.matching.internal.domain.MatchType;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoiceLineItem;
import com.enterprise.spendsync.matching.internal.repository.InvoiceDiscrepancyRepository;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceLineItemRepository;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.matching.internal.service.GibXsltRendererService;
import com.enterprise.spendsync.matching.internal.service.UblTrInvoiceParserService;
import com.enterprise.spendsync.purchasing.internal.domain.Incoterms;
import com.enterprise.spendsync.purchasing.internal.domain.PaymentTerms;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import com.enterprise.spendsync.vendorportal.dto.PoFlipInvoiceRequest;
import com.enterprise.spendsync.vendorportal.dto.SupplierInvoiceDetailResponse;
import com.enterprise.spendsync.vendorportal.dto.SupplierInvoiceResponse;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorUser;
import com.enterprise.spendsync.vendorportal.internal.repository.VendorUserRepository;
import com.enterprise.spendsync.vendorportal.internal.service.VendorInvoiceServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
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
public class VendorInvoiceServiceComprehensiveTest {

    @Mock private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock private SupplierInvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private InvoiceDiscrepancyRepository discrepancyRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private VendorUserRepository vendorUserRepository;
    @Mock private UblTrInvoiceParserService ublParserService;
    @Mock private GibXsltRendererService xsltRendererService;
    @Mock private AuditService auditService;

    @InjectMocks
    private VendorInvoiceServiceImpl invoiceService;

    private UUID tenantId;
    private UUID vendorId;
    private UUID vendorUserId;
    private UUID poId;
    private Tenant tenant;
    private Vendor vendor;
    private VendorUser vendorUser;
    private LegalEntity legalEntity;
    private CostCenter costCenter;
    private PurchaseOrder purchaseOrder;
    private PurchaseOrderLineItem poLine;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        vendorUserId = UUID.randomUUID();
        poId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        tenant = new Tenant("Invoice Tenant", "inv-tenant");
        tenant.setId(tenantId);

        legalEntity = new LegalEntity(tenant, "Legal Entity A.Ş.", "LE-01", "1234567890", "TRY", "Istanbul", "TR");
        legalEntity.setId(UUID.randomUUID());

        costCenter = new CostCenter(tenant, legalEntity, "CC-01", "Operations");
        costCenter.setId(UUID.randomUUID());

        vendor = new Vendor(tenant, "Supplier Co", "1234567890", "Istanbul", VendorCategory.IT_HARDWARE,
                VendorTier.TIER_1_STRATEGIC, true, "orders@sup.com", "555-1234",
                "Address", "Istanbul", "TR", PaymentTerms.NET_30, "Garanti", "TR0000000000");
        vendor.setId(vendorId);

        vendorUser = new VendorUser(tenant, vendor, "user@sup.com", "hash", "User Name", "555-4321", RoleType.VENDOR_ADMIN, true);
        vendorUser.setId(vendorUserId);

        purchaseOrder = new PurchaseOrder(tenant, "PO-2026-001", null, legalEntity, costCenter, null, vendor,
                Incoterms.DAP, "TRY", PaymentTerms.NET_30, "Notes", null);
        purchaseOrder.setId(poId);
        purchaseOrder.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);

        poLine = new PurchaseOrderLineItem(tenant, purchaseOrder, null, 1, "Cloud Server Hosting", "IT_SERVICE",
                new BigDecimal("10"), "MONTH", new BigDecimal("1000.00"), null, null, null);
        poLine.setId(UUID.randomUUID());
        purchaseOrder.addLineItem(poLine);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =========================================================================
    // 1. createPoFlipInvoice
    // =========================================================================

    @Test
    @DisplayName("Should create 2-Way matched PO-Flip invoice for all-services category")
    void shouldCreatePoFlipInvoiceForServices() {
        PoFlipInvoiceRequest.PoFlipLineItemDto lineDto = new PoFlipInvoiceRequest.PoFlipLineItemDto(
                poLine.getId(), new BigDecimal("5"), new BigDecimal("20.00"), "608", "7/10");

        PoFlipInvoiceRequest request = new PoFlipInvoiceRequest(
                "GIB2026000000100", UUID.randomUUID().toString(), InvoiceProfile.TICARI_FATURA,
                InvoiceType.SATIS, LocalDate.now(), List.of(lineDto));

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(poId, tenantId, vendorId)).thenReturn(Optional.of(purchaseOrder));
        when(supplierInvoiceRepository.existsByTenantIdAndEttn(eq(tenantId), any())).thenReturn(false);
        when(supplierInvoiceRepository.existsByTenantIdAndVendorIdAndInvoiceNumber(eq(tenantId), eq(vendorId), any())).thenReturn(false);
        when(invoiceLineItemRepository.findAllByTenantIdAndPurchaseOrderLineItemId(eq(tenantId), eq(poLine.getId()))).thenReturn(List.of());
        when(supplierInvoiceRepository.save(any())).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            ReflectionTestUtils.setField(inv, "id", UUID.randomUUID());
            return inv;
        });

        SupplierInvoiceResponse response = invoiceService.createPoFlipInvoice(poId, request, vendorUserId);

        assertThat(response).isNotNull();
        assertThat(response.invoiceNumber()).isEqualTo("GIB2026000000100");
        assertThat(response.matchType()).isEqualTo("TWO_WAY");
        assertThat(response.matchStatus()).isEqualTo("AUTO_MATCHED");
        assertThat(response.status()).isEqualTo("APPROVED_FOR_PAYMENT");
        verify(auditService).recordAuditLog(any());
    }

    @Test
    @DisplayName("Should create 3-Way matched PO-Flip invoice for goods when PO is PARTIALLY_RECEIVED")
    void shouldCreatePoFlipInvoiceForGoodsWithPartialReceipt() {
        PurchaseOrderLineItem goodsLine = new PurchaseOrderLineItem(tenant, purchaseOrder, null, 1, "Hardware Server", "HARDWARE",
                new BigDecimal("10"), "EA", new BigDecimal("1000.00"), null, null, null);
        goodsLine.setId(UUID.randomUUID());
        purchaseOrder.getLineItems().clear();
        purchaseOrder.addLineItem(goodsLine);

        PoFlipInvoiceRequest.PoFlipLineItemDto lineDto = new PoFlipInvoiceRequest.PoFlipLineItemDto(
                goodsLine.getId(), new BigDecimal("10"), new BigDecimal("20.00"), null, null);

        PoFlipInvoiceRequest request = new PoFlipInvoiceRequest(
                "GIB2026000000101", UUID.randomUUID().toString(), InvoiceProfile.TICARI_FATURA,
                InvoiceType.SATIS, LocalDate.now(), List.of(lineDto));

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(poId, tenantId, vendorId)).thenReturn(Optional.of(purchaseOrder));
        when(supplierInvoiceRepository.existsByTenantIdAndEttn(eq(tenantId), any())).thenReturn(false);
        when(supplierInvoiceRepository.existsByTenantIdAndVendorIdAndInvoiceNumber(eq(tenantId), eq(vendorId), any())).thenReturn(false);
        when(invoiceLineItemRepository.findAllByTenantIdAndPurchaseOrderLineItemId(eq(tenantId), eq(goodsLine.getId()))).thenReturn(List.of());
        when(supplierInvoiceRepository.save(any())).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            ReflectionTestUtils.setField(inv, "id", UUID.randomUUID());
            return inv;
        });

        SupplierInvoiceResponse response = invoiceService.createPoFlipInvoice(poId, request, vendorUserId);

        assertThat(response).isNotNull();
        assertThat(response.matchType()).isEqualTo("THREE_WAY");
        assertThat(response.matchStatus()).isEqualTo("AUTO_MATCHED");
        assertThat(response.status()).isEqualTo("APPROVED_FOR_PAYMENT");
    }

    @Test
    @DisplayName("Should set PENDING_RECEIPT when goods invoice created against ISSUED PO")
    void shouldSetPendingReceiptWhenPoNotReceived() {
        PurchaseOrderLineItem goodsLine = new PurchaseOrderLineItem(tenant, purchaseOrder, null, 1, "Hardware Server", "HARDWARE",
                new BigDecimal("10"), "EA", new BigDecimal("1000.00"), null, null, null);
        goodsLine.setId(UUID.randomUUID());
        purchaseOrder.getLineItems().clear();
        purchaseOrder.addLineItem(goodsLine);
        purchaseOrder.setStatus(PurchaseOrderStatus.ISSUED); // Not received yet

        PoFlipInvoiceRequest.PoFlipLineItemDto lineDto = new PoFlipInvoiceRequest.PoFlipLineItemDto(
                goodsLine.getId(), new BigDecimal("10"), new BigDecimal("20.00"), null, null);

        PoFlipInvoiceRequest request = new PoFlipInvoiceRequest(
                "GIB2026000000102", UUID.randomUUID().toString(), InvoiceProfile.TICARI_FATURA,
                InvoiceType.SATIS, LocalDate.now(), List.of(lineDto));

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(poId, tenantId, vendorId)).thenReturn(Optional.of(purchaseOrder));
        when(supplierInvoiceRepository.existsByTenantIdAndEttn(eq(tenantId), any())).thenReturn(false);
        when(supplierInvoiceRepository.existsByTenantIdAndVendorIdAndInvoiceNumber(eq(tenantId), eq(vendorId), any())).thenReturn(false);
        when(invoiceLineItemRepository.findAllByTenantIdAndPurchaseOrderLineItemId(eq(tenantId), eq(goodsLine.getId()))).thenReturn(List.of());
        when(supplierInvoiceRepository.save(any())).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            ReflectionTestUtils.setField(inv, "id", UUID.randomUUID());
            return inv;
        });

        SupplierInvoiceResponse response = invoiceService.createPoFlipInvoice(poId, request, vendorUserId);

        assertThat(response.matchStatus()).isEqualTo("PENDING_RECEIPT");
        assertThat(response.status()).isEqualTo("SUBMITTED");
    }

    @Test
    @DisplayName("Should throw 409 when ETTN already exists")
    void shouldThrowConflictWhenEttnExists() {
        PoFlipInvoiceRequest request = new PoFlipInvoiceRequest(
                "GIB2026000000103", "dup-ettn", InvoiceProfile.TICARI_FATURA,
                InvoiceType.SATIS, LocalDate.now(), List.of());

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(poId, tenantId, vendorId)).thenReturn(Optional.of(purchaseOrder));
        when(supplierInvoiceRepository.existsByTenantIdAndEttn(tenantId, "dup-ettn")).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.createPoFlipInvoice(poId, request, vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should throw 400 when invoiced quantity exceeds remaining quantity")
    void shouldThrowWhenInvoicedQuantityExceedsCeiling() {
        PoFlipInvoiceRequest.PoFlipLineItemDto lineDto = new PoFlipInvoiceRequest.PoFlipLineItemDto(
                poLine.getId(), new BigDecimal("15"), new BigDecimal("20.00"), null, null);

        PoFlipInvoiceRequest request = new PoFlipInvoiceRequest(
                "GIB2026000000104", UUID.randomUUID().toString(), InvoiceProfile.TICARI_FATURA,
                InvoiceType.SATIS, LocalDate.now(), List.of(lineDto));

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(purchaseOrderRepository.findByIdAndTenantIdAndVendorId(poId, tenantId, vendorId)).thenReturn(Optional.of(purchaseOrder));
        when(supplierInvoiceRepository.existsByTenantIdAndEttn(eq(tenantId), any())).thenReturn(false);
        when(supplierInvoiceRepository.existsByTenantIdAndVendorIdAndInvoiceNumber(eq(tenantId), eq(vendorId), any())).thenReturn(false);
        when(invoiceLineItemRepository.findAllByTenantIdAndPurchaseOrderLineItemId(eq(tenantId), eq(poLine.getId()))).thenReturn(List.of());

        assertThatThrownBy(() -> invoiceService.createPoFlipInvoice(poId, request, vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot invoice quantity 15");
    }

    // =========================================================================
    // 2. uploadUblXmlInvoice
    // =========================================================================

    @Test
    @DisplayName("Should upload UBL XML and detect PRICE_DISCREPANCY when price exceeds PO")
    void shouldDetectPriceDiscrepancyOnUblUpload() throws Exception {
        MockMultipartFile xmlFile = new MockMultipartFile("file", "invoice.xml", "application/xml",
                "<Invoice><ID>GIB-UBL-1</ID></Invoice>".getBytes());

        UblTrInvoiceParserService.ParsedUblLineItem ublLine = new UblTrInvoiceParserService.ParsedUblLineItem(
                1, "Cloud Server Hosting", new BigDecimal("10"), "MONTH",
                new BigDecimal("1500.00"), // Price exceeds PO price 1000.00
                new BigDecimal("20.00"), new BigDecimal("3000.00"), null, null, BigDecimal.ZERO, new BigDecimal("18000.00"));

        UblTrInvoiceParserService.ParsedUblInvoice parsedInvoice = new UblTrInvoiceParserService.ParsedUblInvoice(
                "GIB-UBL-1", UUID.randomUUID().toString(), "TICARI_FATURA", "SATIS", LocalDate.now(),
                "TRY", "PO-2026-001", "1234567890", "9876543210", new BigDecimal("15000.00"), new BigDecimal("3000.00"),
                BigDecimal.ZERO, new BigDecimal("18000.00"), new BigDecimal("18000.00"), null, List.of(ublLine));

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(ublParserService.parseUblXml(any())).thenReturn(parsedInvoice);
        when(supplierInvoiceRepository.existsByTenantIdAndEttn(eq(tenantId), any())).thenReturn(false);
        when(purchaseOrderRepository.findByPoNumberAndTenantId("PO-2026-001", tenantId)).thenReturn(Optional.of(purchaseOrder));
        when(supplierInvoiceRepository.save(any())).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            ReflectionTestUtils.setField(inv, "id", UUID.randomUUID());
            return inv;
        });

        SupplierInvoiceResponse response = invoiceService.uploadUblXmlInvoice(xmlFile, vendorUserId);

        assertThat(response).isNotNull();
        assertThat(response.matchStatus()).isEqualTo("PRICE_DISCREPANCY");
        assertThat(response.status()).isEqualTo("SUBMITTED");
        verify(auditService).recordAuditLog(any());
    }

    @Test
    @DisplayName("Should throw 400 when uploaded file is empty or null")
    void shouldThrowWhenFileEmpty() {
        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.xml", "application/xml", new byte[0]);

        assertThatThrownBy(() -> invoiceService.uploadUblXmlInvoice(emptyFile, vendorUserId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("XML file is required");
    }

    // =========================================================================
    // 3. getVendorInvoices, getVendorInvoiceDetail & getInvoiceHtml
    // =========================================================================

    @Test
    @DisplayName("Should get vendor invoices with and without status filter")
    void shouldGetVendorInvoices() {
        SupplierInvoice invoice = new SupplierInvoice(tenant, "INV-1", "ettn-1", LocalDate.now(),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, purchaseOrder, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("1000.00"), new BigDecimal("200.00"), BigDecimal.ZERO,
                new BigDecimal("1200.00"), new BigDecimal("1200.00"), MatchType.THREE_WAY);

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findAllByTenantIdAndVendorIdAndStatusOrderByCreatedAtDesc(tenantId, vendorId, InvoiceStatus.APPROVED_FOR_PAYMENT))
                .thenReturn(List.of(invoice));
        when(supplierInvoiceRepository.findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId))
                .thenReturn(List.of(invoice));

        List<SupplierInvoiceResponse> filtered = invoiceService.getVendorInvoices(InvoiceStatus.APPROVED_FOR_PAYMENT, vendorUserId);
        assertThat(filtered).hasSize(1);

        List<SupplierInvoiceResponse> all = invoiceService.getVendorInvoices(null, vendorUserId);
        assertThat(all).hasSize(1);
    }

    @Test
    @DisplayName("Should get full invoice details with line items and discrepancies")
    void shouldGetVendorInvoiceDetail() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice(tenant, "INV-DET", "ettn-det", LocalDate.now(),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, purchaseOrder, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("1000.00"), new BigDecimal("200.00"), BigDecimal.ZERO,
                new BigDecimal("1200.00"), new BigDecimal("1200.00"), MatchType.THREE_WAY);
        invoice.setId(invoiceId);

        SupplierInvoiceLineItem item = new SupplierInvoiceLineItem(tenant, poLine, null, new BigDecimal("1"),
                new BigDecimal("1000.00"), new BigDecimal("20.00"), new BigDecimal("200.00"), new BigDecimal("1200.00"));
        invoice.addLineItem(item);

        InvoiceDiscrepancy disc = new InvoiceDiscrepancy(tenant, invoice, "PRICE_VARIANCE", "1000", "1500", new BigDecimal("500.00"), new BigDecimal("50.00"));
        invoice.addDiscrepancy(disc);

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, tenantId, vendorId)).thenReturn(Optional.of(invoice));

        SupplierInvoiceDetailResponse detail = invoiceService.getVendorInvoiceDetail(invoiceId, vendorUserId);

        assertThat(detail).isNotNull();
        assertThat(detail.invoiceNumber()).isEqualTo("INV-DET");
        assertThat(detail.lineItems()).hasSize(1);
        assertThat(detail.discrepancies()).hasSize(1);
    }

    @Test
    @DisplayName("Should render invoice HTML via xsltRendererService")
    void shouldGetInvoiceHtml() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice(tenant, "INV-HTML", "ettn-h", LocalDate.now(),
                InvoiceType.SATIS, InvoiceProfile.TICARI_FATURA, purchaseOrder, vendor, legalEntity, costCenter,
                "TRY", new BigDecimal("1000.00"), new BigDecimal("200.00"), BigDecimal.ZERO,
                new BigDecimal("1200.00"), new BigDecimal("1200.00"), MatchType.THREE_WAY);

        when(vendorUserRepository.findById(vendorUserId)).thenReturn(Optional.of(vendorUser));
        when(supplierInvoiceRepository.findByIdAndTenantIdAndVendorId(invoiceId, tenantId, vendorId)).thenReturn(Optional.of(invoice));
        when(xsltRendererService.renderInvoiceHtml(invoice)).thenReturn("<html><body>Invoice HTML</body></html>");

        String html = invoiceService.getInvoiceHtml(invoiceId, vendorUserId);

        assertThat(html).contains("Invoice HTML");
        verify(xsltRendererService).renderInvoiceHtml(invoice);
    }
}
