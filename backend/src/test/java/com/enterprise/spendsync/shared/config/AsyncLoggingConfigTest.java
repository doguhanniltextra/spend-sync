package com.enterprise.spendsync.shared.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Async Logging TaskDecorator Unit Tests")
class AsyncLoggingConfigTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should copy MDC context from parent thread to child runnable and clear child context")
    void shouldPropagateMdcContextToAsyncThread() throws ExecutionException, InterruptedException, TimeoutException {
        TaskDecorator decorator = new AsyncLoggingConfig.MdcTaskDecorator();

        MDC.put("traceId", "async-trace-999");
        MDC.put("tenantId", "tenant-111");

        AtomicReference<String> childTraceId = new AtomicReference<>();
        AtomicReference<String> childTenantId = new AtomicReference<>();
        CompletableFuture<Void> future = new CompletableFuture<>();

        Runnable decorated = decorator.decorate(() -> {
            childTraceId.set(MDC.get("traceId"));
            childTenantId.set(MDC.get("tenantId"));
            future.complete(null);
        });

        // Run on a separate thread
        Thread thread = new Thread(decorated);
        thread.start();

        future.get(5, TimeUnit.SECONDS);

        assertThat(childTraceId.get()).isEqualTo("async-trace-999");
        assertThat(childTenantId.get()).isEqualTo("tenant-111");

        // Verify parent context remains untouched
        assertThat(MDC.get("traceId")).isEqualTo("async-trace-999");
    }

    @Test
    @DisplayName("Should handle empty parent context gracefully")
    void shouldHandleEmptyParentContext() throws ExecutionException, InterruptedException, TimeoutException {
        TaskDecorator decorator = new AsyncLoggingConfig.MdcTaskDecorator();
        MDC.clear();

        AtomicReference<String> childTrace = new AtomicReference<>("init");
        CompletableFuture<Void> future = new CompletableFuture<>();

        Runnable decorated = decorator.decorate(() -> {
            childTrace.set(MDC.get("traceId"));
            future.complete(null);
        });

        Thread thread = new Thread(decorated);
        thread.start();

        future.get(5, TimeUnit.SECONDS);

        assertThat(childTrace.get()).isNull();
    }
}
