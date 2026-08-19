package com.enterprise.spendsync.shared.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA Attribute Converter for transparent column-level encryption (AES-256-GCM).
 * Converts entity cleartext strings to AES-GCM ciphertext on database INSERT/UPDATE
 * and transparently decrypts back to cleartext on database SELECT.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionService staticEncryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService encryptionService) {
        EncryptedStringConverter.staticEncryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (staticEncryptionService == null) {
            return attribute;
        }
        return staticEncryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (staticEncryptionService == null) {
            return dbData;
        }
        return staticEncryptionService.decrypt(dbData);
    }
}
