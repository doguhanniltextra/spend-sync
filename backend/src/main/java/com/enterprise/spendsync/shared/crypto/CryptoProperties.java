package com.enterprise.spendsync.shared.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spendsync.security.crypto")
public class CryptoProperties {

    /**
     * Master Key for AES-256-GCM Field-Level Encryption.
     * In production, this is supplied via secure environment variables or Vault / AWS KMS.
     * Default provides a secure 256-bit Hex Key for development.
     */
    private String masterKeyHex = "9f8e7d6c5b4a3928170f1e2d3c4b5a6978899aabbccddeeff001122334455667";

    /**
     * Secret salt used for deterministic Blind Indexing (HMAC-SHA256).
     */
    private String blindIndexSalt = "spendsync_deterministic_blind_index_salt_v1";

    public String getMasterKeyHex() {
        return masterKeyHex;
    }

    public void setMasterKeyHex(String masterKeyHex) {
        this.masterKeyHex = masterKeyHex;
    }

    public String getBlindIndexSalt() {
        return blindIndexSalt;
    }

    public void setBlindIndexSalt(String blindIndexSalt) {
        this.blindIndexSalt = blindIndexSalt;
    }
}
