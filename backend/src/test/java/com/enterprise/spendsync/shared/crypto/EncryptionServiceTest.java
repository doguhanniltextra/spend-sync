package com.enterprise.spendsync.shared.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        CryptoProperties properties = new CryptoProperties();
        properties.setMasterKeyHex("9f8e7d6c5b4a3928170f1e2d3c4b5a6978899aabbccddeeff001122334455667");
        properties.setBlindIndexSalt("test_salt_v1");
        this.encryptionService = new EncryptionServiceImpl(properties);
    }

    @Test
    @DisplayName("Should successfully encrypt and decrypt IBAN with AES-256-GCM")
    void shouldEncryptAndDecryptIban() {
        String originalIban = "TR330006200000012345678901";

        String cipherText = encryptionService.encrypt(originalIban);

        assertThat(cipherText)
                .isNotNull()
                .startsWith("ENC:v1:")
                .isNotEqualTo(originalIban);

        String decrypted = encryptionService.decrypt(cipherText);
        assertThat(decrypted).isEqualTo(originalIban);
    }

    @Test
    @DisplayName("Should produce different ciphertexts for same plaintext (Random IV / Nonce)")
    void shouldProduceDifferentCiphertextsDueToRandomIv() {
        String originalIban = "TR330006200000012345678901";

        String cipher1 = encryptionService.encrypt(originalIban);
        String cipher2 = encryptionService.encrypt(originalIban);

        assertThat(cipher1).isNotEqualTo(cipher2);
        assertThat(encryptionService.decrypt(cipher1)).isEqualTo(originalIban);
        assertThat(encryptionService.decrypt(cipher2)).isEqualTo(originalIban);
    }

    @Test
    @DisplayName("Should detect tampering and fail decryption on modified ciphertext")
    void shouldFailOnTamperedCiphertext() {
        String originalIban = "TR330006200000012345678901";
        String cipherText = encryptionService.encrypt(originalIban);

        // Tamper with one character in payload
        char lastChar = cipherText.charAt(cipherText.length() - 2);
        char tamperedChar = (lastChar == 'A') ? 'B' : 'A';
        String tamperedCipher = cipherText.substring(0, cipherText.length() - 2) + tamperedChar + cipherText.substring(cipherText.length() - 1);

        assertThatThrownBy(() -> encryptionService.decrypt(tamperedCipher))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should transparently return cleartext when reading legacy unencrypted data")
    void shouldHandleLegacyCleartext() {
        String legacyIban = "TR110001100000098765432100";
        String decrypted = encryptionService.decrypt(legacyIban);
        assertThat(decrypted).isEqualTo(legacyIban);
    }

    @Test
    @DisplayName("Should generate deterministic Blind Index hash for searchable lookups")
    void shouldGenerateDeterministicBlindIndex() {
        String iban1 = "TR33 0006 2000 0001 2345 6789 01";
        String iban2 = "tr330006200000012345678901";

        String hash1 = encryptionService.generateBlindIndex(iban1);
        String hash2 = encryptionService.generateBlindIndex(iban2);

        assertThat(hash1)
                .isNotNull()
                .hasSize(64) // SHA-256 Hex
                .isEqualTo(hash2);
    }

    @Test
    @DisplayName("Should correctly format data masking conforming to ISO 27001 Control A.8.11")
    void shouldMaskSensitiveFieldsCorrectly() {
        assertThat(MaskingUtils.maskIban("TR330006200000012345678901"))
                .isEqualTo("TR33 **** **** **** 8901");

        assertThat(MaskingUtils.maskTaxNumber("9988775258"))
                .isEqualTo("99****5258");

        assertThat(MaskingUtils.maskEmail("muhasebe@abcteknoloji.com.tr"))
                .isEqualTo("m***e@abcteknoloji.com.tr");
    }
}
