package com.enterprise.spendsync.testcontainers;

import com.enterprise.spendsync.core.internal.domain.RoleType;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.domain.User;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.core.internal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class RateLimitContainerTest extends AbstractContainerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "ratelimit.user@spendsync.test";
    private static final String TEST_PASSWORD = "Password123!";

    @BeforeEach
    void setUp() {
        // Flush all rate limit keys in real Redis container before each test
        Set<String> keys = stringRedisTemplate.keys("spendsync:ratelimit:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }

        // Seed test user if not exists
        if (userRepository.findByEmail(TEST_EMAIL).isEmpty()) {
            Tenant tenant = tenantRepository.findBySlug("test-tenant")
                    .orElseGet(() -> tenantRepository.save(new Tenant("Test Tenant", "test-tenant")));

            User user = new User();
            user.setEmail(TEST_EMAIL);
            user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
            user.setFirstName("Rate");
            user.setLastName("Limiter");
            user.setTenant(tenant);
            user.setRoles(Set.of(RoleType.ROOT_USER));
            userRepository.save(user);
        }
    }

    @Test
    @DisplayName("TC-CT-01 & TC-CT-02: Should allow 5 requests and block 6th with HTTP 429 on real Redis ZSet")
    void shouldEnforceSlidingWindowRateLimitOnRealRedis() throws Exception {
        String loginPayload = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", TEST_EMAIL, TEST_PASSWORD);

        // 1. First 5 requests must pass successfully (HTTP 200 OK)
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginPayload))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Limit", "5"))
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(5 - i)));
        }

        // 2. 6th request must be intercepted by RateLimitAspect (HTTP 429 Too Many Requests)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        // 3. Verify Redis ZSet state in container
        Set<String> rateLimitKeys = stringRedisTemplate.keys("spendsync:ratelimit:login:*");
        assertThat(rateLimitKeys).isNotEmpty();

        String activeKey = rateLimitKeys.iterator().next();
        Long zSetCount = stringRedisTemplate.opsForZSet().zCard(activeKey);
        assertThat(zSetCount).isEqualTo(5L);
    }
}
