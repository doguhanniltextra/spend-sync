package com.enterprise.spendsync.shared.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback CompositeConverter that masks sensitive personal, financial, and authentication data
 * (TCKN, VKN, IBAN, Credit Card PAN, Bearer Tokens, Passwords) in log messages.
 * Usage in Logback pattern: %mask(%msg)
 */
public class PiiMaskingConverter extends CompositeConverter<ILoggingEvent> {

    // TCKN: 11 digits (first 7 digits masked: *******8901)
    private static final Pattern TCKN_PATTERN = Pattern.compile("(?<!\\d)(\\d{7})(\\d{4})(?!\\d)");

    // VKN: 10 digits (middle 6 digits masked: 12******90)
    private static final Pattern VKN_PATTERN = Pattern.compile("(?<!\\d)(\\d{2})(\\d{6})(\\d{2})(?!\\d)");

    // IBAN: TR followed by 24 digits (with optional spaces: 4 + 4 + 4 + 4 + 4 + 4 + 2 = 26 chars)
    private static final Pattern IBAN_PATTERN = Pattern.compile("(?i)\\b(TR\\d{2})\\s*(\\d{4})\\s*(?:\\d{4}\\s*){4}(\\d{2})\\b");

    // Credit Card: 16 digits (masked: 4543 60** **** 7890)
    private static final Pattern CARD_PATTERN = Pattern.compile("(?<!\\d)(\\d{4})[\\s-]?(\\d{2})\\d{2}[\\s-]?\\d{4}[\\s-]?(\\d{4})(?!\\d)");

    // Bearer Token: Authorization header or JWT string
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*");

    // Sensitive JSON fields
    private static final Pattern SENSITIVE_JSON_PATTERN = Pattern.compile("(?i)\"(password|secret|accessToken|token|rawUblXml|cvv|pin)\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    protected String transform(ILoggingEvent event, String in) {
        if (in == null || in.isEmpty()) {
            return in;
        }
        return mask(in);
    }

    public static String mask(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text;

        // 1. Mask Bearer Tokens
        result = BEARER_PATTERN.matcher(result).replaceAll("Bearer ********");

        // 2. Mask Sensitive JSON Keys
        Matcher jsonMatcher = SENSITIVE_JSON_PATTERN.matcher(result);
        StringBuffer jsonSb = new StringBuffer();
        while (jsonMatcher.find()) {
            jsonMatcher.appendReplacement(jsonSb, "\"" + jsonMatcher.group(1) + "\":\"********\"");
        }
        jsonMatcher.appendTail(jsonSb);
        result = jsonSb.toString();

        // 3. Mask IBANs
        Matcher ibanMatcher = IBAN_PATTERN.matcher(result);
        StringBuffer ibanSb = new StringBuffer();
        while (ibanMatcher.find()) {
            ibanMatcher.appendReplacement(ibanSb, ibanMatcher.group(1).toUpperCase() + " " + ibanMatcher.group(2) + " **** **** **** " + ibanMatcher.group(3));
        }
        ibanMatcher.appendTail(ibanSb);
        result = ibanSb.toString();

        // 4. Mask Credit Card PANs
        Matcher cardMatcher = CARD_PATTERN.matcher(result);
        StringBuffer cardSb = new StringBuffer();
        while (cardMatcher.find()) {
            cardMatcher.appendReplacement(cardSb, cardMatcher.group(1) + " " + cardMatcher.group(2) + "** **** " + cardMatcher.group(3));
        }
        cardMatcher.appendTail(cardSb);
        result = cardSb.toString();

        // 5. Mask TCKN (11 digits)
        Matcher tcknMatcher = TCKN_PATTERN.matcher(result);
        StringBuffer tcknSb = new StringBuffer();
        while (tcknMatcher.find()) {
            tcknMatcher.appendReplacement(tcknSb, "*******" + tcknMatcher.group(2));
        }
        tcknMatcher.appendTail(tcknSb);
        result = tcknSb.toString();

        // 6. Mask VKN (10 digits)
        Matcher vknMatcher = VKN_PATTERN.matcher(result);
        StringBuffer vknSb = new StringBuffer();
        while (vknMatcher.find()) {
            vknMatcher.appendReplacement(vknSb, vknMatcher.group(1) + "******" + vknMatcher.group(3));
        }
        vknMatcher.appendTail(vknSb);
        result = vknSb.toString();

        return result;
    }
}
