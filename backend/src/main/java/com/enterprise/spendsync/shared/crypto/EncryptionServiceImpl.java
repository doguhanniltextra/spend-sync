package com.enterprise.spendsync.shared.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionServiceImpl.class);

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ENCRYPTION_PREFIX = "ENC:v1:";

    private static final int GCM_IV_LENGTH_BYTES = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH_BITS = 128; // 128 bit authentication tag

    private final SecretKey masterSecretKey;
    private final SecretKey blindIndexSecretKey;
    private final SecureRandom secureRandom;

    public EncryptionServiceImpl(CryptoProperties cryptoProperties) {
        this.secureRandom = new SecureRandom();

        byte[] keyBytes = HexFormat.of().parseHex(cryptoProperties.getMasterKeyHex());
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Master encryption key must be exactly 256 bits (32 bytes / 64 hex chars)");
        }
        this.masterSecretKey = new SecretKeySpec(keyBytes, ALGORITHM);

        byte[] saltBytes = cryptoProperties.getBlindIndexSalt().getBytes(StandardCharsets.UTF_8);
        this.blindIndexSecretKey = new SecretKeySpec(saltBytes, HMAC_ALGORITHM);
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }

        // Avoid double encryption if already encrypted
        if (plainText.startsWith(ENCRYPTION_PREFIX)) {
            return plainText;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterSecretKey, spec);

            byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = cipher.doFinal(plainBytes);

            // Buffer = IV (12 bytes) + Ciphertext with GCM Auth Tag
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherBytes);

            String base64Payload = Base64.getEncoder().encodeToString(byteBuffer.array());
            return ENCRYPTION_PREFIX + base64Payload;
        } catch (Exception e) {
            log.error("Failed to encrypt sensitive data with AES-256-GCM", e);
            throw new IllegalStateException("Cryptographic encryption failure", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return cipherText;
        }

        // Legacy cleartext backward-compatibility
        if (!cipherText.startsWith(ENCRYPTION_PREFIX)) {
            return cipherText;
        }

        try {
            String base64Payload = cipherText.substring(ENCRYPTION_PREFIX.length());
            byte[] cipherBuffer = Base64.getDecoder().decode(base64Payload);

            if (cipherBuffer.length < GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted payload size");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherBuffer);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterSecretKey, spec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt sensitive data with AES-256-GCM (possible corruption or tampering)", e);
            throw new IllegalStateException("Cryptographic decryption failure", e);
        }
    }

    @Override
    public String generateBlindIndex(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }

        // Normalize text before hashing (uppercase, no whitespace)
        String normalized = plainText.trim().replaceAll("\\s+", "").toUpperCase();

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(blindIndexSecretKey);
            byte[] hashBytes = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            log.error("Failed to generate deterministic blind index", e);
            throw new IllegalStateException("Blind index calculation failure", e);
        }
    }
}
