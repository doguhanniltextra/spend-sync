package com.enterprise.spendsync.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user who is already associated with a corporate tenant attempts to create another company.
 */
public class UserAlreadyHasCompanyException extends SpendSyncException {

    public UserAlreadyHasCompanyException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "USER_ALREADY_HAS_COMPANY");
    }
}
