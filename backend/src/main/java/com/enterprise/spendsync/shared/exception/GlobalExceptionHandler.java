package com.enterprise.spendsync.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global REST exception handler mapping domain and infrastructure exceptions
 * to a standardised, secure {@link ErrorResponse} JSON contract.
 *
 * <h2>Security Contract</h2>
 * <ul>
 *   <li>5xx responses MUST NOT contain {@code ex.getMessage()}, SQL snippets,
 *       class names, stack frames, or any internal diagnostic artefact.</li>
 *   <li>Every response MUST carry a {@code traceId} so callers can correlate
 *       a reported incident with the server-side log entry.</li>
 *   <li>All diagnostic detail (full stack trace, raw exception message, SQL)
 *       is recorded exclusively in the structured server log under the same
 *       {@code traceId}.</li>
 * </ul>
 *
 * <h2>Handler Hierarchy</h2>
 * Most-specific handlers are declared first.  The catch-all {@link Exception}
 * handler is always last and never leaks internals.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ── Safe, user-facing messages (no internals, ever) ─────────────────────── */

    private static final String MSG_INTERNAL =
            "An unexpected internal server error occurred. " +
            "Please contact support with the trace ID.";

    private static final String MSG_NOT_FOUND =
            "The requested resource was not found.";

    private static final String MSG_ACCESS_DENIED =
            "You do not have sufficient permissions to perform this action.";

    private static final String MSG_AUTHENTICATION =
            "Authentication failed or token is invalid or expired.";

    private static final String MSG_CONFLICT =
            "A data conflict occurred. The resource may already exist.";

    private static final String MSG_MALFORMED_REQUEST =
            "Malformed JSON request or invalid parameter type.";

    private static final String MSG_METHOD_NOT_ALLOWED =
            "HTTP method not supported for this endpoint.";

    private static final String MSG_MEDIA_TYPE =
            "Content-Type is not supported. Please use application/json.";

    private static final String MSG_VALIDATION_FAILED =
            "Input validation failed for one or more fields.";

    /* ── Helpers ──────────────────────────────────────────────────────────────── */

    /**
     * Resolves the current request's trace ID from MDC (populated by
     * {@link com.enterprise.spendsync.shared.filter.MdcLoggingFilter}).
     * Falls back to a freshly generated UUID if MDC is absent (e.g. async context).
     */
    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
    }

    private String resolvePath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : null;
    }

    private ResponseEntity<ErrorResponse> build(String errorCode, String safeMessage,
                                                 HttpStatus status, String traceId, String path) {
        ErrorResponse body = new ErrorResponse(errorCode, safeMessage, status.value(), traceId, path);
        return ResponseEntity.status(status).body(body);
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Domain / Business Exceptions  (SpendSyncException hierarchy — 4xx)
     * ═══════════════════════════════════════════════════════════════════════════ */

    /**
     * Handles all typed domain exceptions ({@link SpendSyncException} subclasses).
     * The domain exception's own {@code message} IS exposed here because it is
     * authored by the application developer and contains no infrastructure detail.
     */
    @ExceptionHandler(SpendSyncException.class)
    public ResponseEntity<ErrorResponse> handleSpendSyncException(SpendSyncException ex,
                                                                   HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("[{}] Business exception at {}: [{}] {}",
                traceId, resolvePath(request), ex.getErrorCode(), ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getHttpStatus().value(),
                traceId,
                resolvePath(request)
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Validation Errors  (400)
     * ═══════════════════════════════════════════════════════════════════════════ */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        String traceId = resolveTraceId();
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("[{}] Validation failure at {}: {} field error(s)",
                traceId, resolvePath(request), fieldErrors.size());

        ErrorResponse body = new ErrorResponse(
                "VALIDATION_FAILED",
                MSG_VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST.value(),
                traceId,
                resolvePath(request),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Malformed Request  (400)
     * ═══════════════════════════════════════════════════════════════════════════ */

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex,
                                                                  HttpServletRequest request) {
        String traceId = resolveTraceId();
        // Log at WARN with the original message for diagnostics; never surface it to the caller.
        log.warn("[{}] Malformed request at {}: {}", traceId, resolvePath(request), ex.getMessage());
        return build("MALFORMED_REQUEST", MSG_MALFORMED_REQUEST,
                HttpStatus.BAD_REQUEST, traceId, resolvePath(request));
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Authentication Failures  (401)
     * ═══════════════════════════════════════════════════════════════════════════ */

    @ExceptionHandler({
            AuthenticationException.class,
            BadCredentialsException.class
    })
    public ResponseEntity<ErrorResponse> handleAuthentication(RuntimeException ex,
                                                               HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("[{}] Authentication failure at {}: {}", traceId, resolvePath(request), ex.getMessage());
        return build("AUTHENTICATION_REQUIRED", MSG_AUTHENTICATION,
                HttpStatus.UNAUTHORIZED, traceId, resolvePath(request));
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Authorisation Failures  (403)
     * ═══════════════════════════════════════════════════════════════════════════ */

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                             HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("[{}] Access denied at {}: {}", traceId, resolvePath(request), ex.getMessage());
        return build("ACCESS_DENIED", MSG_ACCESS_DENIED,
                HttpStatus.FORBIDDEN, traceId, resolvePath(request));
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Resource Not Found  (404)
     * ═══════════════════════════════════════════════════════════════════════════ */

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex,
                                                                HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("[{}] No resource found at {}: {}", traceId, resolvePath(request), ex.getMessage());
        return build("RESOURCE_NOT_FOUND", MSG_NOT_FOUND,
                HttpStatus.NOT_FOUND, traceId, resolvePath(request));
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Method Not Allowed  (405)
     * ═══════════════════════════════════════════════════════════════════════════ */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("[{}] Method not allowed at {}: {}", traceId, resolvePath(request), ex.getMessage());
        return build("METHOD_NOT_ALLOWED", MSG_METHOD_NOT_ALLOWED,
                HttpStatus.METHOD_NOT_ALLOWED, traceId, resolvePath(request));
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Data Integrity Conflicts  (409)
     * ═══════════════════════════════════════════════════════════════════════════ */

    /**
     * Catches database-layer unique constraint and FK violations.
     * The raw SQL error message is intentionally NOT forwarded to the caller.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                        HttpServletRequest request) {
        String traceId = resolveTraceId();
        // Log the full cause chain (may contain SQL) only to the server log.
        log.warn("[{}] Data integrity violation at {}: {}", traceId, resolvePath(request), ex.getMessage());
        return build("DATA_CONFLICT", MSG_CONFLICT,
                HttpStatus.CONFLICT, traceId, resolvePath(request));
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Unsupported Media Type  (415)
     * ═══════════════════════════════════════════════════════════════════════════ */

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                                                      HttpServletRequest request) {
        String traceId = resolveTraceId();
        log.warn("[{}] Unsupported media type at {}: {}", traceId, resolvePath(request), ex.getMessage());
        return build("UNSUPPORTED_MEDIA_TYPE", MSG_MEDIA_TYPE,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, traceId, resolvePath(request));
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Catch-All — MUST always be last and MUST NOT leak internals  (500)
     * ═══════════════════════════════════════════════════════════════════════════ */

    /**
     * Fallback handler for every {@link Exception} that was not matched by a
     * more-specific handler above.
     *
     * <p><strong>Security rule:</strong> {@code ex.getMessage()} is NEVER placed
     * in the response body. It may contain SQL, file paths, class names, or other
     * infrastructure detail. All diagnostic information is captured in the log with
     * the full stack trace and the same {@code traceId} that appears in the response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex,
                                                                  HttpServletRequest request) {
        String traceId = resolveTraceId();
        // Full stack trace recorded server-side; traceId is the caller's handle to this event.
        log.error("[{}] Unhandled exception at {} [{}]: ",
                traceId, resolvePath(request), ex.getClass().getSimpleName(), ex);
        return build("INTERNAL_SERVER_ERROR", MSG_INTERNAL,
                HttpStatus.INTERNAL_SERVER_ERROR, traceId, resolvePath(request));
    }
}
