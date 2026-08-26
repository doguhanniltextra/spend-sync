package com.enterprise.spendsync.shared.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Standard enterprise error response payload returned for all HTTP error conditions (4xx and 5xx).
 *
 * <p>Key security contract: the {@code message} field must NEVER contain internal exception messages,
 * SQL details, stack traces, or any diagnostic artefact. Only a pre-approved human-readable string
 * and the {@code traceId} should be surfaced to the caller. All diagnostic information is persisted
 * exclusively in the server-side log under the same {@code traceId}.
 *
 * @param errorCode        Machine-readable code identifying the error category (e.g. RESOURCE_NOT_FOUND).
 * @param message          Safe, user-facing description that reveals no internals.
 * @param status           HTTP status code (mirrors the HTTP response status).
 * @param timestamp        UTC instant at which the error was generated.
 * @param traceId          Correlation / trace identifier propagated from {@code MdcLoggingFilter}.
 * @param path             Request URI that triggered the error.
 * @param validationErrors Field-level validation errors, only populated for 400 validation failures.
 */
public record ErrorResponse(
        String errorCode,
        String message,
        int status,
        Instant timestamp,
        String traceId,
        String path,
        Map<String, String> validationErrors
) {
    /** Constructor for error responses without a known path or validation errors. */
    public ErrorResponse(String errorCode, String message, int status, String traceId) {
        this(errorCode, message, status, Instant.now(), traceId, null, null);
    }

    /** Constructor for error responses with a request path and no validation errors. */
    public ErrorResponse(String errorCode, String message, int status, String traceId, String path) {
        this(errorCode, message, status, Instant.now(), traceId, path, null);
    }

    /** Constructor for 400 validation failures that include a field-error map. */
    public ErrorResponse(String errorCode, String message, int status,
                         String traceId, String path, Map<String, String> validationErrors) {
        this(errorCode, message, status, Instant.now(), traceId, path, validationErrors);
    }

    /**
     * Legacy compatibility constructor (no traceId / path). Kept to avoid breaking any
     * existing call sites that have not been migrated yet.
     *
     * @deprecated Prefer constructors that supply traceId and path.
     */
    @Deprecated
    public ErrorResponse(String errorCode, String message, int status) {
        this(errorCode, message, status, Instant.now(), null, null, null);
    }

    /**
     * Legacy compatibility constructor (no traceId / path, with validation errors).
     *
     * @deprecated Prefer constructors that supply traceId and path.
     */
    @Deprecated
    public ErrorResponse(String errorCode, String message, int status, Map<String, String> validationErrors) {
        this(errorCode, message, status, Instant.now(), null, null, validationErrors);
    }
}
