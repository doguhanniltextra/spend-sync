package com.enterprise.spendsync.shared.crypto;

/**
 * ISO/IEC 27001:2022 Control A.8.11 (Data Masking) utility for sensitive financial and PII fields.
 */
public final class MaskingUtils {

    private MaskingUtils() {}

    /**
     * Partially masks an IBAN preserving country & control digits (first 4) and terminal digits (last 4).
     * Example: TR330006200000012345678901 -> TR33 **** **** **** 8901
     */
    public static String maskIban(String iban) {
        if (iban == null || iban.isBlank()) {
            return iban;
        }

        String cleaned = iban.replaceAll("\\s+", "").trim();
        if (cleaned.length() <= 8) {
            return "****";
        }

        String prefix = cleaned.substring(0, 4);
        String suffix = cleaned.substring(cleaned.length() - 4);

        return prefix + " **** **** **** " + suffix;
    }

    /**
     * Partially masks a Tax Number (VKN / TCKN).
     * Example: 9988775258 -> 99****5258
     */
    public static String maskTaxNumber(String taxNumber) {
        if (taxNumber == null || taxNumber.isBlank()) {
            return taxNumber;
        }

        String cleaned = taxNumber.trim();
        if (cleaned.length() <= 4) {
            return "****";
        }

        String prefix = cleaned.substring(0, 2);
        String suffix = cleaned.substring(cleaned.length() - 4);
        return prefix + "****" + suffix;
    }

    /**
     * Partially masks a National Identification Number (TCKN).
     */
    public static String maskNationalId(String nationalId) {
        return maskTaxNumber(nationalId);
    }

    /**
     * Partially masks an email address.
     * Example: muhasebe@abcteknoloji.com -> m***e@abcteknoloji.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@", 2);
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return "*@" + domain;
        }

        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + domain;
    }
}
