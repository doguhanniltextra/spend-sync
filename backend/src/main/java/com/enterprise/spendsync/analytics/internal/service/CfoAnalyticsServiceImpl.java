package com.enterprise.spendsync.analytics.internal.service;

import com.enterprise.spendsync.analytics.dto.*;
import com.enterprise.spendsync.budget.internal.domain.BudgetPool;
import com.enterprise.spendsync.budget.internal.repository.BudgetPoolRepository;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrder;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderStatus;
import com.enterprise.spendsync.purchasing.internal.repository.PurchaseOrderRepository;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CfoAnalyticsServiceImpl implements CfoAnalyticsService {

    private final BudgetPoolRepository budgetPoolRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;

    public CfoAnalyticsServiceImpl(
            BudgetPoolRepository budgetPoolRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierInvoiceRepository supplierInvoiceRepository
    ) {
        this.budgetPoolRepository = budgetPoolRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
    }

    @Override
    public CfoExecutiveDeckResponse getCfoExecutiveDeck() {
        UUID tenantId = TenantContext.getRequiredTenantId();

        // 1. Budget aggregates
        List<BudgetPool> pools = budgetPoolRepository.findAllByTenantId(tenantId);
        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal totalReserved = BigDecimal.ZERO;

        for (BudgetPool pool : pools) {
            if (pool.getAllocatedAmount() != null) totalAllocated = totalAllocated.add(pool.getAllocatedAmount());
            if (pool.getSpentAmount() != null) totalSpent = totalSpent.add(pool.getSpentAmount());
            if (pool.getReservedAmount() != null) totalReserved = totalReserved.add(pool.getReservedAmount());
        }

        BigDecimal totalCommitted = totalSpent.add(totalReserved);
        double overallUtilization = totalAllocated.compareTo(BigDecimal.ZERO) > 0
                ? totalCommitted.multiply(BigDecimal.valueOf(100))
                .divide(totalAllocated, 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        // 2. PO Analysis (Vendor Concentration & Category Spend)
        List<PurchaseOrder> activeOrders = purchaseOrderRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .filter(po -> po.getStatus() != PurchaseOrderStatus.CANCELLED)
                .toList();

        // Category Spend Distribution
        Map<String, BigDecimal> categorySpendMap = new HashMap<>();
        BigDecimal totalCategorySpend = BigDecimal.ZERO;

        for (PurchaseOrder po : activeOrders) {
            if (po.getLineItems() != null) {
                for (PurchaseOrderLineItem item : po.getLineItems()) {
                    String cat = item.getItemCategory() != null ? item.getItemCategory() : "GENERAL";
                    BigDecimal lineTot = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                    categorySpendMap.put(cat, categorySpendMap.getOrDefault(cat, BigDecimal.ZERO).add(lineTot));
                    totalCategorySpend = totalCategorySpend.add(lineTot);
                }
            }
        }

        BigDecimal finalTotalCatSpend = totalCategorySpend;
        List<CategorySpendDto> categoryDistribution = categorySpendMap.entrySet().stream()
                .map(e -> {
                    double share = finalTotalCatSpend.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(finalTotalCatSpend, 2, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return new CategorySpendDto(e.getKey(), e.getValue(), share);
                })
                .sorted(Comparator.comparing(CategorySpendDto::amount).reversed())
                .toList();

        // Top Vendors & Concentration
        Map<UUID, TopVendorAccumulator> vendorMap = new HashMap<>();
        BigDecimal totalVendorSpend = BigDecimal.ZERO;

        for (PurchaseOrder po : activeOrders) {
            if (po.getVendor() != null) {
                UUID vId = po.getVendor().getId();
                TopVendorAccumulator acc = vendorMap.computeIfAbsent(vId, k -> new TopVendorAccumulator(
                        vId,
                        po.getVendor().getName(),
                        po.getVendor().getTaxNumber(),
                        po.getVendor().getTier() != null ? po.getVendor().getTier().name() : "TIER_3_STANDARD"
                ));
                BigDecimal poTot = po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO;
                acc.volume = acc.volume.add(poTot);
                totalVendorSpend = totalVendorSpend.add(poTot);
            }
        }

        BigDecimal finalTotalVendorSpend = totalVendorSpend;
        List<TopVendorSpendDto> topVendors = vendorMap.values().stream()
                .sorted(Comparator.comparing((TopVendorAccumulator a) -> a.volume).reversed())
                .limit(5)
                .map(acc -> {
                    double share = finalTotalVendorSpend.compareTo(BigDecimal.ZERO) > 0
                            ? acc.volume.multiply(BigDecimal.valueOf(100))
                            .divide(finalTotalVendorSpend, 2, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    String risk = share >= 30.0 ? "HIGH" : share >= 15.0 ? "MEDIUM" : "LOW";
                    return new TopVendorSpendDto(
                            acc.vendorId,
                            acc.vendorName,
                            acc.taxNumber,
                            acc.tier,
                            acc.volume,
                            share,
                            risk
                    );
                })
                .toList();

        // 3. Invoice Match Integrity
        List<SupplierInvoice> invoices = supplierInvoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        long totalInvoices = invoices.size();
        long matchedInvoices = invoices.stream()
                .filter(i -> i.getMatchStatus() == InvoiceMatchStatus.AUTO_MATCHED || i.getMatchStatus() == InvoiceMatchStatus.MANUALLY_MATCHED)
                .count();
        long discrepancyInvoices = invoices.stream()
                .filter(i -> i.getMatchStatus() == InvoiceMatchStatus.DISCREPANCY_HOLD)
                .count();

        BigDecimal discrepancyBlockedAmount = invoices.stream()
                .filter(i -> i.getMatchStatus() == InvoiceMatchStatus.DISCREPANCY_HOLD)
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double firstTimeMatchRate = totalInvoices > 0
                ? (double) matchedInvoices / totalInvoices * 100.0
                : 100.0;

        ThreeWayMatchIntegrityDto matchIntegrity = new ThreeWayMatchIntegrityDto(
                totalInvoices,
                matchedInvoices,
                discrepancyInvoices,
                Math.round(firstTimeMatchRate * 100.0) / 100.0,
                discrepancyBlockedAmount
        );

        // 4. Cash Outflow Forecast (Next 4 Months)
        List<MonthlyOutflowDto> cashOutflowForecast = computeOutflowForecast(invoices, activeOrders);

        return new CfoExecutiveDeckResponse(
                totalSpent,
                totalCommitted,
                totalAllocated,
                overallUtilization,
                "TRY",
                categoryDistribution,
                cashOutflowForecast,
                topVendors,
                matchIntegrity
        );
    }

    private List<MonthlyOutflowDto> computeOutflowForecast(
            List<SupplierInvoice> invoices,
            List<PurchaseOrder> activeOrders
    ) {
        LocalDate now = LocalDate.now();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        List<MonthlyOutflowDto> result = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            LocalDate targetMonth = now.plusMonths(i);
            String monthKey = targetMonth.format(monthFormatter);
            String displayMonth = targetMonth.format(displayFormatter);

            // Invoices due in this month (estimated from invoiceDate + 30 days)
            BigDecimal dueInvoicesSum = invoices.stream()
                    .filter(inv -> {
                        if (inv.getStatus() == InvoiceStatus.PAID || inv.getStatus() == InvoiceStatus.CANCELLED) {
                            return false;
                        }
                        LocalDate estDueDate = inv.getInvoiceDate() != null ? inv.getInvoiceDate().plusDays(30) : now;
                        return estDueDate.format(monthFormatter).equals(monthKey);
                    })
                    .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Projected PO deliveries
            BigDecimal poDeliveriesSum = BigDecimal.ZERO;
            if (i > 0) {
                poDeliveriesSum = activeOrders.stream()
                        .filter(po -> po.getStatus() == PurchaseOrderStatus.ISSUED)
                        .map(po -> po.getTotalAmount() != null ? po.getTotalAmount().divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            BigDecimal totalOutflow = dueInvoicesSum.add(poDeliveriesSum);

            result.add(new MonthlyOutflowDto(
                    displayMonth,
                    dueInvoicesSum,
                    poDeliveriesSum,
                    totalOutflow
            ));
        }

        return result;
    }

    private static class TopVendorAccumulator {
        UUID vendorId;
        String vendorName;
        String taxNumber;
        String tier;
        BigDecimal volume = BigDecimal.ZERO;

        TopVendorAccumulator(UUID vendorId, String vendorName, String taxNumber, String tier) {
            this.vendorId = vendorId;
            this.vendorName = vendorName;
            this.taxNumber = taxNumber;
            this.tier = tier;
        }
    }
}
