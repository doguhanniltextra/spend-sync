package com.enterprise.spendsync.shared.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Standard enterprise error response payload.
 */
public record ErrorResponse(
        String errorCode,
        String message,
        int status,
        Instant timestamp,
        Map<String, String> validationErrors
) {
    public ErrorResponse(String errorCode, String message, int status) {
        this(errorCode, message, status, Instant.now(), null);
    }

    public ErrorResponse(String errorCode, String message, int status, Map<String, String> validationErrors) {
        this(errorCode, message, status, Instant.now(), validationErrors);
    }
}
