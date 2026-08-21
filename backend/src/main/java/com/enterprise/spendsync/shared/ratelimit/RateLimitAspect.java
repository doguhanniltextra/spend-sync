package com.enterprise.spendsync.shared.ratelimit;

import com.enterprise.spendsync.shared.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.UUID;

/**
 * Aspect implementing Redis-backed sliding-window rate limiting.
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);
    private final StringRedisTemplate stringRedisTemplate;

    public RateLimitAspect(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Around("@annotation(rateLimit)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String identifier = resolveClientIdentifier(rateLimit.type());
        String redisKey = String.format("spendsync:ratelimit:%s:%s", rateLimit.key(), identifier);

        long now = System.currentTimeMillis();
        long windowStart = now - (rateLimit.periodSeconds() * 1000L);

        try {
            // 1. Evict timestamps outside the sliding window
            stringRedisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            // 2. Count requests in the current sliding window
            Long currentRequests = stringRedisTemplate.opsForZSet().zCard(redisKey);
            long count = currentRequests != null ? currentRequests : 0L;

            if (count >= rateLimit.limit()) {
                log.warn("Rate limit exceeded for [{}] with key [{}]. Current: {} / Limit: {} in {}s",
                        identifier, redisKey, count, rateLimit.limit(), rateLimit.periodSeconds());

                HttpServletResponse response = getHttpServletResponse();
                if (response != null) {
                    response.setHeader("Retry-After", String.valueOf(rateLimit.periodSeconds()));
                    response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.limit()));
                    response.setHeader("X-RateLimit-Remaining", "0");
                }

                throw new RateLimitExceededException(
                        String.format("Too many requests. Rate limit is %d requests per %d seconds. Please retry after %d seconds.",
                                rateLimit.limit(), rateLimit.periodSeconds(), rateLimit.periodSeconds()),
                        rateLimit.periodSeconds()
                );
            }

            // 3. Add current request with timestamp score
            String member = now + ":" + UUID.randomUUID().toString().substring(0, 8);
            stringRedisTemplate.opsForZSet().add(redisKey, member, (double) now);
            stringRedisTemplate.expire(redisKey, Duration.ofSeconds(rateLimit.periodSeconds() + 10));

            HttpServletResponse response = getHttpServletResponse();
            if (response != null) {
                response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.limit()));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimit.limit() - count - 1));
            }

        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            // Fail-open strategy: If Redis fails, do not block users from logging in or using the app
            log.error("Redis Rate Limiter failed: {}. Allowing request (Fail-Open).", e.getMessage());
        }

        return joinPoint.proceed();
    }

    private String resolveClientIdentifier(RateLimitType type) {
        HttpServletRequest request = getHttpServletRequest();

        return switch (type) {
            case USER -> {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                    yield "user:" + auth.getName();
                }
                yield "ip:" + extractClientIp(request);
            }
            case TENANT -> {
                UUID tenantId = TenantContext.getTenantId().orElse(null);
                if (tenantId != null) {
                    yield "tenant:" + tenantId;
                }
                yield "ip:" + extractClientIp(request);
            }
            case IP -> "ip:" + extractClientIp(request);
        };
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "127.0.0.1";
    }

    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private HttpServletResponse getHttpServletResponse() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getResponse() : null;
    }
}
