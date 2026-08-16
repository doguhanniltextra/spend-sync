package com.enterprise.spendsync.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to register with an already existing email address.
 */
public class EmailAlreadyExistsException extends SpendSyncException {

    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists.", HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
    }
}
