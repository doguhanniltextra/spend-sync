package com.enterprise.spendsync.payment.crypto;

import com.enterprise.spendsync.shared.crypto.CryptoProperties;
import com.enterprise.spendsync.shared.crypto.EncryptionServiceImpl;
import com.enterprise.spendsync.shared.crypto.MaskingUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AES-256-GCM Financial Cryptography & Masking Unit Tests (ISO 27001)")
class AesIbanCryptoServiceTest {

    private EncryptionServiceImpl encryptionService;
    // 32-byte / 256-bit hexadecimal test key
    private static final String TEST_MASTER_KEY_HEX = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String TEST_SALT = "SpendSync-Blind-Index-Salt-2026";

    @BeforeEach
    void setUp() {
        CryptoProperties props = new CryptoProperties();
        props.setMasterKeyHex(TEST_MASTER_KEY_HEX);
        props.setBlindIndexSalt(TEST_SALT);
        encryptionService = new EncryptionServiceImpl(props);
    }

    @Test
    @DisplayName("TC-08-04: Cleartext IBAN is encrypted to AES-256-GCM ciphertext starting with ENC:v1:")
    void shouldEncryptCleartextIban() {
        String plainIban = "TR330006200000012345678901";

        String cipherText = encryptionService.encrypt(plainIban);

        assertThat(cipherText).isNotNull();
        assertThat(cipherText).startsWith("ENC:v1:");
        assertThat(cipherText).isNotEqualTo(plainIban);
    }

    @Test
    @DisplayName("TC-08-05: AES-256-GCM ciphertext is decrypted back to exact original IBAN without loss")
    void shouldDecryptCiphertextToOriginalIban() {
        String plainIban = "TR330006200000012345678901";

        String cipherText = encryptionService.encrypt(plainIban);
        String decrypted = encryptionService.decrypt(cipherText);

        assertThat(decrypted).isEqualTo(plainIban);
    }

    @Test
    @DisplayName("Should prevent double encryption when text already starts with ENC:v1:")
    void shouldPreventDoubleEncryption() {
        String plainIban = "TR330006200000012345678901";
        String cipherText1 = encryptionService.encrypt(plainIban);
        String cipherText2 = encryptionService.encrypt(cipherText1);

        assertThat(cipherText2).isEqualTo(cipherText1);
    }

    @Test
    @DisplayName("Should transparently return legacy unencrypted text on decrypt")
    void shouldReturnLegacyCleartextOnDecrypt() {
        String legacyText = "TR112233445566778899001122";
        String decrypted = encryptionService.decrypt(legacyText);

        assertThat(decrypted).isEqualTo(legacyText);
    }

    @Test
    @DisplayName("Should generate deterministic blind index HMAC-SHA256 hash for database indexing")
    void shouldGenerateDeterministicBlindIndex() {
        String iban1 = "TR33 0006 2000 0001 2345 6789 01";
        String iban2 = "tr330006200000012345678901";

        String blindIndex1 = encryptionService.generateBlindIndex(iban1);
        String blindIndex2 = encryptionService.generateBlindIndex(iban2);

        assertThat(blindIndex1).isNotNull().isEqualTo(blindIndex2);
        assertThat(blindIndex1).hasSize(64); // SHA-256 hex length
    }

    @Test
    @DisplayName("TC-08-06: MaskingUtils masks IBAN preserving first 4 and last 4 characters")
    void shouldMaskIbanPreservingHeaderAndTail() {
        String rawIban = "TR330006200000012345678901";
        String masked = MaskingUtils.maskIban(rawIban);

        assertThat(masked).isEqualTo("TR33 **** **** **** 8901");
    }

    @Test
    @DisplayName("MaskingUtils masks Tax Number (VKN) preserving first 2 and last 4 characters")
    void shouldMaskTaxNumber() {
        String vkn = "9988775258";
        String masked = MaskingUtils.maskTaxNumber(vkn);

        assertThat(masked).isEqualTo("99****5258");
    }

    @Test
    @DisplayName("MaskingUtils masks email address preserving first and last char of local part")
    void shouldMaskEmail() {
        String email = "muhasebe@abcteknoloji.com";
        String masked = MaskingUtils.maskEmail(email);

        assertThat(masked).isEqualTo("m***e@abcteknoloji.com");
    }

    @Test
    @DisplayName("Should reject invalid key lengths not equal to 32 bytes (64 hex)")
    void shouldRejectInvalidKeyLengths() {
        CryptoProperties props = new CryptoProperties();
        props.setMasterKeyHex("0123456789abcdef"); // only 8 bytes
        props.setBlindIndexSalt(TEST_SALT);

        assertThatThrownBy(() -> new EncryptionServiceImpl(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Master encryption key must be exactly 256 bits");
    }
}
