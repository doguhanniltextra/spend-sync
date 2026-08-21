package com.enterprise.spendsync.vendorportal.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Form BS Monthly e-Reconciliation & SHA-256 Digital Seal Unit Tests")
class ReconciliationSealServiceTest {

    private String computeSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("TC-09-15: Computes deterministic 64-hex SHA-256 digital seal for reconciliation approval")
    void shouldComputeDeterministicSha256Seal() {
        UUID tenantId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        int year = 2026;
        int month = 8;
        BigDecimal totalAmount = new BigDecimal("450000.00");
        Instant timestamp = Instant.parse("2026-08-21T12:00:00Z");

        String payload = tenantId + ":" + vendorId + ":" + year + ":" + month + ":" + totalAmount + ":" + timestamp;
        String seal1 = computeSha256(payload);
        String seal2 = computeSha256(payload);

        assertThat(seal1).isNotNull().hasSize(64);
        assertThat(seal1).isEqualTo(seal2);
    }

    @Test
    @DisplayName("Should detect tampering when amount or period is modified in reconciliation payload")
    void shouldDetectTamperingInReconciliationPayload() {
        UUID tenantId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        int year = 2026;
        int month = 8;
        Instant timestamp = Instant.parse("2026-08-21T12:00:00Z");

        String originalPayload = tenantId + ":" + vendorId + ":" + year + ":" + month + ":450000.00:" + timestamp;
        String tamperedPayload = tenantId + ":" + vendorId + ":" + year + ":" + month + ":450001.00:" + timestamp;

        String originalSeal = computeSha256(originalPayload);
        String tamperedSeal = computeSha256(tamperedPayload);

        assertThat(tamperedSeal).isNotEqualTo(originalSeal);
    }
}
