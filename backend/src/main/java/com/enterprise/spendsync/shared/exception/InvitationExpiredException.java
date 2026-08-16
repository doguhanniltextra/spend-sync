package com.enterprise.spendsync.shared.exception;

import org.springframework.http.HttpStatus;

public class InvitationExpiredException extends SpendSyncException {

    public InvitationExpiredException(String message) {
        super(message, HttpStatus.GONE, "INVITATION_EXPIRED");
    }
}
