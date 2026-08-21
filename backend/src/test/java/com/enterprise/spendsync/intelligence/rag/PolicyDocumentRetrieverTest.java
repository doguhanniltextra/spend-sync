package com.enterprise.spendsync.intelligence.rag;

import com.enterprise.spendsync.intelligence.internal.rag.PolicyDocumentRetriever;
import com.enterprise.spendsync.intelligence.internal.rag.PolicyDocumentRetriever.PolicyClause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyDocumentRetrieverTest {

    private PolicyDocumentRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new PolicyDocumentRetriever();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should return static knowledge base when query is null or blank")
    void shouldReturnStaticKnowledgeBaseForBlankQueries(String query) {
        List<PolicyClause> results = retriever.retrieveRelevantClauses(query);
        assertThat(results).hasSize(5);
    }

    @ParameterizedTest(name = "[{index}] Query: ''{0}'' -> Expected Clause: {1}")
    @CsvSource({
            "tender requirements for equipment, PROC-POL-01",
            "three quotes required over 50k,  PROC-POL-01",
            "po issuance procedure,           PROC-POL-01",
            "cfo payment batch release,       DOA-POL-04",
            "disbursement limits approval,    DOA-POL-04",
            "early pay discount working capital, FIN-POL-12",
            "datacenter server hardware gpu,  SEC-POL-09",
            "gebze facility compute cluster,  SEC-POL-09",
            "ttk defect notification timeline, LEG-POL-03",
            "damaged shipment freight waybill, LEG-POL-03"
    })
    @DisplayName("Should retrieve correct policy clause based on domain keywords")
    void shouldRetrieveCorrectClausesForKeywords(String query, String expectedClauseCode) {
        List<PolicyClause> results = retriever.retrieveRelevantClauses(query);
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(c -> c.clauseCode().equals(expectedClauseCode));
    }

    @Test
    @DisplayName("Should return default sublist when query matches no policy keywords")
    void shouldReturnDefaultSublistWhenNoKeywordsMatch() {
        List<PolicyClause> results = retriever.retrieveRelevantClauses("completely unrelated astronomy subject");
        assertThat(results).hasSize(3);
        assertThat(results.get(0).clauseCode()).isEqualTo("PROC-POL-01");
    }

    @Test
    @DisplayName("Should match multiple policy clauses when query spans multiple categories")
    void shouldMatchMultipleClausesForCrossDomainQuery() {
        List<PolicyClause> results = retriever.retrieveRelevantClauses("quote tender and defect damage");
        assertThat(results).extracting(PolicyClause::clauseCode)
                .contains("PROC-POL-01", "LEG-POL-03");
    }
}
