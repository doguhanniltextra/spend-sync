package com.enterprise.spendsync.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when password does not meet corporate security complexity requirements (ISO 27001).
 */
public class InvalidPasswordException extends SpendSyncException {

    public InvalidPasswordException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_PASSWORD_POLICY");
    }
}
