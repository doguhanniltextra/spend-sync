package com.enterprise.spendsync.shared.tenant;

import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TenantContext Unit Tests (Multi-Tenancy & ThreadLocal Safety)")
class TenantContextTest {

    @BeforeEach
    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should set and get tenant ID from ThreadLocal")
    void shouldSetAndGetTenantId() {
        UUID expectedTenantId = UUID.randomUUID();

        TenantContext.setTenantId(expectedTenantId);

        Optional<UUID> actualTenantId = TenantContext.getTenantId();
        assertThat(actualTenantId).isPresent().contains(expectedTenantId);
        assertThat(TenantContext.getRequiredTenantId()).isEqualTo(expectedTenantId);
    }

    @Test
    @DisplayName("Should clear tenant ID and return empty")
    void shouldClearTenantId() {
        TenantContext.setTenantId(UUID.randomUUID());
        TenantContext.clear();

        assertThat(TenantContext.getTenantId()).isEmpty();
        assertThatThrownBy(TenantContext::getRequiredTenantId)
                .isInstanceOf(SpendSyncException.class)
                .hasMessageContaining("No active tenant context found");
    }

    @Test
    @DisplayName("Should fallback to UserPrincipal tenantId when ThreadLocal is empty")
    void shouldFallbackToSecurityContextPrincipal() {
        UUID principalTenantId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(),
                principalTenantId,
                null,
                "USER",
                "test@spendsync.com",
                null,
                "Test User",
                true,
                Set.of(),
                Set.of()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Set.of())
        );

        Optional<UUID> resolvedTenantId = TenantContext.getTenantId();

        assertThat(resolvedTenantId).isPresent().contains(principalTenantId);
    }

    @Test
    @DisplayName("Should isolate tenant IDs between concurrent worker threads")
    void shouldIsolateTenantIdsAcrossThreads() throws InterruptedException {
        UUID mainTenantId = UUID.randomUUID();
        UUID workerTenantId = UUID.randomUUID();

        TenantContext.setTenantId(mainTenantId);

        AtomicReference<UUID> workerResolvedTenant = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            TenantContext.setTenantId(workerTenantId);
            workerResolvedTenant.set(TenantContext.getRequiredTenantId());
            TenantContext.clear();
            latch.countDown();
        });

        worker.start();
        latch.await();

        // Main thread should still have mainTenantId unchanged
        assertThat(TenantContext.getRequiredTenantId()).isEqualTo(mainTenantId);
        assertThat(workerResolvedTenant.get()).isEqualTo(workerTenantId);
    }
}
