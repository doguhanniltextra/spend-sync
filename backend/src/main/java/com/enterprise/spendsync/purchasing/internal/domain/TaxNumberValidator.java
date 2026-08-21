package com.enterprise.spendsync.purchasing.internal.domain;

/**
 * Enterprise Tax Number (VKN / TCKN) algorithmic validator for Turkish tax compliance.
 * - VKN (Vergi Kimlik Numarası): 10 digits, modulus 10 & power table validation.
 * - TCKN (T.C. Kimlik Numarası): 11 digits, non-zero first digit, 10th & 11th check digits.
 */
public final class TaxNumberValidator {

    private TaxNumberValidator() {
    }

    /**
     * Validates whether the given string is a valid VKN (10 digits) or TCKN (11 digits).
     */
    public static boolean isValid(String taxNumber) {
        if (taxNumber == null) {
            return false;
        }
        String clean = taxNumber.trim();
        if (clean.length() == 10) {
            return isValidVkn(clean);
        } else if (clean.length() == 11) {
            return isValidTckn(clean);
        }
        return false;
    }

    /**
     * Validates 10-digit Turkish Tax Identification Number (VKN) according to GİB algorithm.
     */
    public static boolean isValidVkn(String vkn) {
        if (vkn == null || !vkn.matches("^\\d{10}$")) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = Character.getNumericValue(vkn.charAt(i));
            int weight = (digit + 9 - i) % 10;
            int pow = (int) (weight * Math.pow(2, 9 - i)) % 9;
            if (weight != 0 && pow == 0) {
                pow = 9;
            }
            sum += pow;
        }

        int lastDigit = Character.getNumericValue(vkn.charAt(9));
        int checkDigit = (10 - (sum % 10)) % 10;
        return lastDigit == checkDigit;
    }

    /**
     * Validates 11-digit Turkish Republic Citizen ID (TCKN) checksum algorithm.
     */
    public static boolean isValidTckn(String tckn) {
        if (tckn == null || !tckn.matches("^[1-9]\\d{10}$")) {
            return false;
        }

        int[] d = new int[11];
        for (int i = 0; i < 11; i++) {
            d[i] = Character.getNumericValue(tckn.charAt(i));
        }

        int oddSum = d[0] + d[2] + d[4] + d[6] + d[8];
        int evenSum = d[1] + d[3] + d[5] + d[7];

        int digit10 = ((oddSum * 7) - evenSum) % 10;
        if (digit10 < 0) {
            digit10 += 10;
        }

        if (d[9] != digit10) {
            return false;
        }

        int totalFirst10Sum = 0;
        for (int i = 0; i < 10; i++) {
            totalFirst10Sum += d[i];
        }

        int digit11 = totalFirst10Sum % 10;
        return d[10] == digit11;
    }
}
