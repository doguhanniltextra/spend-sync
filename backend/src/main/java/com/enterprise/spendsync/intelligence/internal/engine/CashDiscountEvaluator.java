package com.enterprise.spendsync.intelligence.internal.engine;

import com.enterprise.spendsync.intelligence.dto.CashDiscountOpportunityDto;
import com.enterprise.spendsync.matching.internal.domain.InvoiceStatus;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CashDiscountEvaluator {

    private static final BigDecimal DEFAULT_DISCOUNT_PERCENT = new BigDecimal("2.00");
    private static final int DEFAULT_NET_TERMS_DAYS = 30;
    private static final int DEFAULT_DISCOUNT_TERMS_DAYS = 10;

    private final SupplierInvoiceRepository supplierInvoiceRepository;

    public CashDiscountEvaluator(SupplierInvoiceRepository supplierInvoiceRepository) {
        this.supplierInvoiceRepository = supplierInvoiceRepository;
    }

    public List<CashDiscountOpportunityDto> evaluateDiscountOpportunities(UUID tenantId) {
        LocalDate today = LocalDate.now();

        List<SupplierInvoice> invoices = supplierInvoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.APPROVED_FOR_PAYMENT ||
                               inv.getStatus() == InvoiceStatus.SUBMITTED)
                .toList();

        List<CashDiscountOpportunityDto> results = new ArrayList<>();
        BigDecimal annualizedApr = SpendIntelligenceCalculator.calculateAnnualizedApr(
                DEFAULT_DISCOUNT_PERCENT, DEFAULT_NET_TERMS_DAYS, DEFAULT_DISCOUNT_TERMS_DAYS
        );

        for (SupplierInvoice inv : invoices) {
            BigDecimal gross = inv.getTotalAmount();
            if (gross == null || gross.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            LocalDate invoiceDate = inv.getInvoiceDate() != null ? inv.getInvoiceDate() : today;
            LocalDate dueDate = invoiceDate.plusDays(30);
            LocalDate discountDeadline = invoiceDate.plusDays(10);

            BigDecimal potentialSavings = SpendIntelligenceCalculator.calculateCashDiscount(gross, DEFAULT_DISCOUNT_PERCENT);
            BigDecimal netPayable = gross.subtract(potentialSavings);

            results.add(new CashDiscountOpportunityDto(
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    inv.getVendor() != null ? inv.getVendor().getName() : "Approved Vendor",
                    gross.setScale(2, RoundingMode.HALF_UP),
                    inv.getCurrency() != null ? inv.getCurrency() : "TRY",
                    dueDate,
                    discountDeadline,
                    DEFAULT_DISCOUNT_PERCENT,
                    potentialSavings.setScale(2, RoundingMode.HALF_UP),
                    netPayable.setScale(2, RoundingMode.HALF_UP),
                    annualizedApr.setScale(2, RoundingMode.HALF_UP)
            ));
        }

        return results;
    }
}
