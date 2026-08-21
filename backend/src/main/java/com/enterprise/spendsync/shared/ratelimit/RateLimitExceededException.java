package com.enterprise.spendsync.shared.ratelimit;

import com.enterprise.spendsync.shared.exception.SpendSyncException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a client exceeds the allowed request rate threshold (HTTP 429).
 */
public class RateLimitExceededException extends SpendSyncException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(String message, int retryAfterSeconds) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
