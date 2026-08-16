package com.enterprise.spendsync.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to create a company with a name or slug that already exists.
 */
public class CompanyAlreadyExistsException extends SpendSyncException {

    public CompanyAlreadyExistsException(String companyName) {
        super("A company with name or identifier '" + companyName + "' already exists.", HttpStatus.CONFLICT, "COMPANY_ALREADY_EXISTS");
    }
}
