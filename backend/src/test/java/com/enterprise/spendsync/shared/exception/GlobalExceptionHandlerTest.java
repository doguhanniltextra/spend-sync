package com.enterprise.spendsync.shared.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <h2>Key Acceptance Criteria Verified</h2>
 * <ol>
 *   <li>HTTP 500 responses NEVER contain {@code ex.getMessage()}, SQL snippets,
 *       stack traces, class names, or any internal diagnostic artefact.</li>
 *   <li>Every response body carries a non-blank {@code traceId}.</li>
 *   <li>The {@code errorCode} and HTTP status are correct for each handler.</li>
 *   <li>Validation (400) responses include a {@code validationErrors} field map.</li>
 *   <li>Conflict (409) responses for {@link DataIntegrityViolationException}
 *       do not forward the raw SQL message to the caller.</li>
 * </ol>
 */
@DisplayName("GlobalExceptionHandler — Secure Error Response Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/v1/test");
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  5xx — Catch-All / Internal Server Error
     * ═══════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("500 Internal Server Error — Information Disclosure Prevention")
    class CatchAllHandlerTests {

        @Test
        @DisplayName("Should return 500 with safe message and MUST NOT expose ex.getMessage()")
        void shouldNotExposeExceptionMessage() {
            String sensitiveMessage = "secret db url: postgres://admin:s3cr3t@prod-db/spendsync";
            Exception ex = new RuntimeException(sensitiveMessage);

            ResponseEntity<ErrorResponse> response = handler.handleGeneralException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.message()).doesNotContain(sensitiveMessage);
            assertThat(body.message()).doesNotContain("postgres");
            assertThat(body.message()).doesNotContain("s3cr3t");
            assertThat(body.message()).doesNotContain("db");
            assertThat(body.errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        }

        @Test
        @DisplayName("Should return 500 with safe message and MUST NOT expose NullPointerException class")
        void shouldNotExposeNullPointerClass() {
            NullPointerException ex = new NullPointerException(
                    "Cannot invoke \"com.enterprise.spendsync.budget.BudgetPool.getId()\" on null");

            ResponseEntity<ErrorResponse> response = handler.handleGeneralException(ex, request);

            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.message()).doesNotContain("BudgetPool");
            assertThat(body.message()).doesNotContain("NullPointerException");
            assertThat(body.message()).doesNotContain("getId");
            assertThat(body.errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        }

        @Test
        @DisplayName("Should include a non-blank traceId in every 500 response")
        void shouldIncludeTraceIdInInternalError() {
            ResponseEntity<ErrorResponse> response =
                    handler.handleGeneralException(new RuntimeException("boom"), request);

            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.traceId()).isNotBlank();
        }

        @Test
        @DisplayName("Should propagate existing MDC traceId into the 500 response")
        void shouldPropagateExistingMdcTraceId() {
            String knownTraceId = "existing-trace-00123";
            MDC.put("traceId", knownTraceId);

            ResponseEntity<ErrorResponse> response =
                    handler.handleGeneralException(new RuntimeException("failure"), request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().traceId()).isEqualTo(knownTraceId);
        }

        @Test
        @DisplayName("Should generate a new traceId when MDC is empty")
        void shouldGenerateTraceIdWhenMdcIsEmpty() {
            MDC.clear(); // ensure empty

            ResponseEntity<ErrorResponse> response =
                    handler.handleGeneralException(new RuntimeException("fail"), request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().traceId()).isNotBlank();
        }

        @Test
        @DisplayName("Should include request path in the 500 response")
        void shouldIncludePathInInternalError() {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/invoices/match");

            ResponseEntity<ErrorResponse> response =
                    handler.handleGeneralException(new RuntimeException("oops"), req);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().path()).isEqualTo("/api/v1/invoices/match");
        }
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  409 Conflict — DataIntegrityViolation (SQL leakage prevention)
     * ═══════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("409 Conflict — SQL Message Non-Disclosure")
    class DataConflictHandlerTests {

        @Test
        @DisplayName("Should return 409 for DataIntegrityViolationException and MUST NOT expose SQL")
        void shouldNotExposeSqlInConflictResponse() {
            String rawSql = "ERROR: duplicate key value violates unique constraint " +
                            "\"uk_vendor_tax_number\" Detail: Key (tax_number, tenant_id)=(1234567890, " +
                            "79ef8bff-1d87-4088-ab87-935989a568d5) already exists.";
            DataIntegrityViolationException ex = new DataIntegrityViolationException(rawSql);

            ResponseEntity<ErrorResponse> response =
                    handler.handleDataIntegrityViolation(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.errorCode()).isEqualTo("DATA_CONFLICT");
            assertThat(body.message()).doesNotContain("uk_vendor_tax_number");
            assertThat(body.message()).doesNotContain("tax_number");
            assertThat(body.message()).doesNotContain("tenant_id");
            assertThat(body.message()).doesNotContain("duplicate key");
        }

        @Test
        @DisplayName("Should include traceId in conflict response")
        void shouldIncludeTraceIdInConflict() {
            ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                    new DataIntegrityViolationException("constraint violation"), request);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().traceId()).isNotBlank();
        }
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  400 Validation — Field Error Map
     * ═══════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("400 Validation — Field Error Consistency")
    class ValidationHandlerTests {

        @Test
        @DisplayName("Should return 400 with field-level validation errors and VALIDATION_FAILED code")
        void shouldReturnValidationErrors() {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(
                    new FieldError("loginRequest", "email", "must not be blank"),
                    new FieldError("loginRequest", "password", "must not be blank")
            ));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.errorCode()).isEqualTo("VALIDATION_FAILED");
            assertThat(body.validationErrors()).containsKey("email");
            assertThat(body.validationErrors()).containsKey("password");
            assertThat(body.validationErrors().get("email")).isEqualTo("must not be blank");
            assertThat(body.traceId()).isNotBlank();
        }
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  400 Malformed Request
     * ═══════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("400 Malformed Request — No Internal Detail Exposed")
    class MalformedRequestHandlerTests {

        @Test
        @DisplayName("Should return 400 MALFORMED_REQUEST for unreadable HTTP message")
        void shouldReturnMalformedRequestForUnreadableMessage() {
            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("JSON parse error: internal detail", null, null);

            ResponseEntity<ErrorResponse> response = handler.handleMalformedRequest(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.errorCode()).isEqualTo("MALFORMED_REQUEST");
            assertThat(body.message()).doesNotContain("JSON parse error");
            assertThat(body.message()).doesNotContain("internal detail");
            assertThat(body.traceId()).isNotBlank();
        }
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  401 Authentication Failures
     * ═══════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("401 Authentication — Credential Detail Non-Disclosure")
    class AuthenticationHandlerTests {

        @Test
        @DisplayName("Should return 401 for BadCredentialsException and NOT expose credentials")
        void shouldReturn401ForBadCredentials() {
            BadCredentialsException ex = new BadCredentialsException("Bad credentials: user=admin@corp.com");

            ResponseEntity<ErrorResponse> response = handler.handleAuthentication(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.errorCode()).isEqualTo("AUTHENTICATION_REQUIRED");
            // Safe message must not forward the email or credential detail
            assertThat(body.message()).doesNotContain("admin@corp.com");
            assertThat(body.traceId()).isNotBlank();
        }
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  403 Access Denied
     * ═══════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("403 Forbidden — Access Denied Handling")
    class AccessDeniedHandlerTests {

        @Test
        @DisplayName("Should return 403 and ACCESS_DENIED code for AccessDeniedException")
        void shouldReturn403ForAccessDenied() {
            AccessDeniedException ex = new AccessDeniedException("Access is denied");

            ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.errorCode()).isEqualTo("ACCESS_DENIED");
            assertThat(body.traceId()).isNotBlank();
        }
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  405 Method Not Allowed
     * ═══════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("405 Method Not Allowed")
    class MethodNotAllowedHandlerTests {

        @Test
        @DisplayName("Should return 405 and METHOD_NOT_ALLOWED code")
        void shouldReturn405ForMethodNotAllowed() {
            HttpRequestMethodNotSupportedException ex =
                    new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST"));

            ResponseEntity<ErrorResponse> response = handler.handleMethodNotAllowed(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.errorCode()).isEqualTo("METHOD_NOT_ALLOWED");
            assertThat(body.traceId()).isNotBlank();
        }
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Domain exceptions (SpendSyncException hierarchy) — errorCode propagated
     * ═══════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("Domain exceptions — SpendSyncException hierarchy")
    class DomainExceptionHandlerTests {

        @Test
        @DisplayName("Should return 404 and RESOURCE_NOT_FOUND for ResourceNotFoundException")
        void shouldReturn404ForResourceNotFoundException() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Budget with id 999 not found");

            ResponseEntity<ErrorResponse> response = handler.handleSpendSyncException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.errorCode()).isEqualTo("RESOURCE_NOT_FOUND");
            assertThat(body.message()).isEqualTo("Budget with id 999 not found");
            assertThat(body.traceId()).isNotBlank();
        }

        @Test
        @DisplayName("Should return 409 and COMPANY_ALREADY_EXISTS for CompanyAlreadyExistsException")
        void shouldReturn409ForCompanyAlreadyExists() {
            CompanyAlreadyExistsException ex = new CompanyAlreadyExistsException("Acme Corp");

            ResponseEntity<ErrorResponse> response = handler.handleSpendSyncException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.errorCode()).isEqualTo("COMPANY_ALREADY_EXISTS");
            assertThat(body.traceId()).isNotBlank();
        }
    }

    /* ═══════════════════════════════════════════════════════════════════════════
     *  Response Schema Consistency — all errors must include traceId + path
     * ═══════════════════════════════════════════════════════════════════════════ */

    @Nested
    @DisplayName("Response Schema Consistency — traceId and path always present")
    class ResponseSchemaConsistencyTests {

        @Test
        @DisplayName("All handlers must include non-null traceId — 403 handler check")
        void allHandlersMustIncludeTraceId_403() {
            var response = handler.handleAccessDenied(new AccessDeniedException("denied"), request);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().traceId()).isNotNull().isNotBlank();
            assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
        }

        @Test
        @DisplayName("All handlers must include non-null traceId — 500 handler check")
        void allHandlersMustIncludeTraceId_500() {
            var response = handler.handleGeneralException(new RuntimeException("fail"), request);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().traceId()).isNotNull().isNotBlank();
            assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
        }

        @Test
        @DisplayName("All handlers must include non-null traceId — 409 handler check")
        void allHandlersMustIncludeTraceId_409() {
            var response = handler.handleDataIntegrityViolation(
                    new DataIntegrityViolationException("violation"), request);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().traceId()).isNotNull().isNotBlank();
            assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
        }

        @Test
        @DisplayName("timestamp must always be present")
        void allHandlersMustIncludeTimestamp() {
            var response = handler.handleGeneralException(new RuntimeException("fail"), request);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }
}
