package com.enterprise.spendsync.shared.crypto;

public interface EncryptionService {

    /**
     * Encrypts plaintext data using AES-256-GCM.
     * Returns a versioned, base64-encoded string: ENC:v1:<iv+ciphertext+tag>
     */
    String encrypt(String plainText);

    /**
     * Decrypts an AES-256-GCM encrypted string.
     * If the input is not encrypted (e.g. legacy cleartext data), returns input gracefully.
     */
    String decrypt(String cipherText);

    /**
     * Generates a deterministic HMAC-SHA256 blind index hash for searchable encrypted columns.
     */
    String generateBlindIndex(String plainText);
}
