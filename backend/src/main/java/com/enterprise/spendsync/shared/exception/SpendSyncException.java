package com.enterprise.spendsync.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Base domain exception for SpendSync enterprise engine.
 */
public abstract class SpendSyncException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public SpendSyncException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
