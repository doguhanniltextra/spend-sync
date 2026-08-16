package com.enterprise.spendsync.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a protected resource is accessed without a valid X-Tenant-Id HTTP header.
 */
public class MissingTenantHeaderException extends SpendSyncException {

    public MissingTenantHeaderException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "MISSING_TENANT_HEADER");
    }
}
