package com.enterprise.spendsync.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidInvitationTokenException extends SpendSyncException {

    public InvalidInvitationTokenException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_INVITATION_TOKEN");
    }
}
