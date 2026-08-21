package com.enterprise.spendsync.shared.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce Redis-backed Sliding Window Rate Limiting on REST controller methods.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Unique key prefix for this rate limited operation (e.g. "auth:login", "catalog:search").
     */
    String key() default "default";

    /**
     * Maximum number of allowed requests within the defined time window.
     */
    int limit() default 10;

    /**
     * Sliding window duration in seconds (default: 60 seconds).
     */
    int periodSeconds() default 60;

    /**
     * Identifier strategy (IP, USER, or TENANT).
     */
    RateLimitType type() default RateLimitType.IP;
}
