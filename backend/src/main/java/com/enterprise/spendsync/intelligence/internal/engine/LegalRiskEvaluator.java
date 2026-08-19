package com.enterprise.spendsync.intelligence.internal.engine;

import com.enterprise.spendsync.intelligence.domain.InsightSeverity;
import com.enterprise.spendsync.intelligence.domain.InsightType;
import com.enterprise.spendsync.intelligence.dto.RecommendationCardDto;
import com.enterprise.spendsync.matching.internal.domain.InvoiceMatchStatus;
import com.enterprise.spendsync.matching.internal.domain.SupplierInvoice;
import com.enterprise.spendsync.matching.internal.repository.SupplierInvoiceRepository;
import com.enterprise.spendsync.receiving.internal.domain.GoodsReceipt;
import com.enterprise.spendsync.receiving.internal.repository.GoodsReceiptRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class LegalRiskEvaluator {

    private static final int TTK_STATUTORY_LIMIT_DAYS = 8;

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;

    public LegalRiskEvaluator(
            GoodsReceiptRepository goodsReceiptRepository,
            SupplierInvoiceRepository supplierInvoiceRepository) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
    }

    public List<RecommendationCardDto> evaluateLegalRisks(UUID tenantId) {
        LocalDate today = LocalDate.now();
        List<RecommendationCardDto> riskCards = new ArrayList<>();

        // Check 3-Way Match discrepancy holds
        List<SupplierInvoice> holdInvoices = supplierInvoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .filter(inv -> inv.getMatchStatus() == InvoiceMatchStatus.DISCREPANCY_HOLD)
                .toList();

        for (SupplierInvoice inv : holdInvoices) {
            riskCards.add(new RecommendationCardDto(
                    UUID.randomUUID(),
                    InsightType.DISCREPANCY_LEGAL_RISK,
                    InsightSeverity.CRITICAL,
                    "3-Way Match Discrepancy Hold",
                    String.format("Invoice %s holds tolerance mismatch against PO.", inv.getInvoiceNumber()),
                    "Immediate AP review required to resolve price/quantity exception before payment release.",
                    "Inspect 3-Way Match",
                    "/matching",
                    inv.getTotalAmount(),
                    inv.getCurrency() != null ? inv.getCurrency() : "TRY",
                    "TTK Art. 23 / SOX 404 Controls"
            ));
        }

        // Check Goods Receipts for damaged/rejected lines approaching 8-day TTK statutory limit
        List<GoodsReceipt> receipts = goodsReceiptRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        for (GoodsReceipt gr : receipts) {
            LocalDate waybillDate = gr.getWaybillDate() != null ? gr.getWaybillDate() : today;
            int remainingDays = SpendIntelligenceCalculator.calculateRemainingStatutoryNoticeDays(
                    waybillDate, today, TTK_STATUTORY_LIMIT_DAYS
            );

            boolean hasRejection = gr.getLineItems() != null && gr.getLineItems().stream()
                    .anyMatch(l -> l.getRejectedQuantity() != null && l.getRejectedQuantity().compareTo(BigDecimal.ZERO) > 0);

            if (hasRejection && remainingDays <= 3) {
                riskCards.add(new RecommendationCardDto(
                        UUID.randomUUID(),
                        InsightType.DISCREPANCY_LEGAL_RISK,
                        remainingDays <= 1 ? InsightSeverity.CRITICAL : InsightSeverity.WARNING,
                        "TTK 8-Day Defect Notice Deadline",
                        String.format("Goods Receipt %s has rejected items. %d days left for formal legal notice.",
                                gr.getReceiptNumber(), remainingDays),
                        "Turkish Commercial Code Art. 23 mandates formal defect notification to vendor within 8 days of receipt.",
                        "View Goods Receipt",
                        "/receiving",
                        BigDecimal.valueOf(remainingDays),
                        "DAYS",
                        "TTK Art. 23(1)(c)"
                ));
            }
        }

        return riskCards;
    }
}
