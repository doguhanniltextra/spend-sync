package com.enterprise.spendsync.intelligence.internal.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class PolicyDocumentRetriever {

    public record PolicyClause(
            String clauseCode,
            String title,
            String category,
            String content
    ) {}

    private static final List<PolicyClause> STATIC_KNOWLEDGE_BASE = List.of(
            new PolicyClause(
                    "PROC-POL-01",
                    "Mandatory 3-Quote Tender Rule",
                    "PROCUREMENT",
                    "All commercial purchases exceeding 50,000 TRY or 2,000 USD require at least 3 formal competitive supplier quotations before purchase order issuance."
            ),
            new PolicyClause(
                    "DOA-POL-04",
                    "Executive Four-Eyes Authorization",
                    "PAYMENTS",
                    "Payment batches releasing cash disbursements greater than 25,000 TRY require dual four-eyes authorization: AP Specialist compilation and CFO / Root User cryptographic release."
            ),
            new PolicyClause(
                    "SEC-POL-09",
                    "Cloud & IT Hardware Security Verification",
                    "IT_GOVERNANCE",
                    "Hardware compute servers and AI GPU accelerators must be routed to approved datacenter facilities (FAC-02 Gebze) and undergo physical serial inspection."
            ),
            new PolicyClause(
                    "LEG-POL-03",
                    "TTK Statutory Defect Notification Protocol",
                    "LEGAL_COMPLIANCE",
                    "Under Turkish Commercial Code (TTK) Article 23, visible freight defects or damaged shipment items must be formally registered and notified to the carrier and supplier within 8 calendar days."
            ),
            new PolicyClause(
                    "FIN-POL-12",
                    "Dynamic Discounting & Working Capital Optimization",
                    "TREASURY",
                    "Approved vendor invoices with early payment terms (2/10 Net 30) should be prioritized for settlement release within 10 days to capture 36.7% annualized APR cost savings."
            )
    );

    public List<PolicyClause> retrieveRelevantClauses(String queryOrContext) {
        if (queryOrContext == null || queryOrContext.isBlank()) {
            return STATIC_KNOWLEDGE_BASE;
        }

        String lower = queryOrContext.toLowerCase(Locale.ROOT);
        List<PolicyClause> matched = new ArrayList<>();

        for (PolicyClause clause : STATIC_KNOWLEDGE_BASE) {
            if (lower.contains("quote") || lower.contains("tender") || lower.contains("50") || lower.contains("po")) {
                if (clause.clauseCode().equals("PROC-POL-01")) matched.add(clause);
            }
            if (lower.contains("pay") || lower.contains("batch") || lower.contains("cfo") || lower.contains("disbursement")) {
                if (clause.clauseCode().equals("DOA-POL-04") || clause.clauseCode().equals("FIN-POL-12")) matched.add(clause);
            }
            if (lower.contains("gpu") || lower.contains("hardware") || lower.contains("server") || lower.contains("gebze")) {
                if (clause.clauseCode().equals("SEC-POL-09")) matched.add(clause);
            }
            if (lower.contains("defect") || lower.contains("ttk") || lower.contains("damage") || lower.contains("waybill")) {
                if (clause.clauseCode().equals("LEG-POL-03")) matched.add(clause);
            }
        }

        return matched.isEmpty() ? STATIC_KNOWLEDGE_BASE.subList(0, 3) : matched;
    }
}
