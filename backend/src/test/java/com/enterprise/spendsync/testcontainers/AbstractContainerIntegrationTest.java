package com.enterprise.spendsync.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Base integration test class utilizing Containerized Environment.
 * Automatically uses dynamic Testcontainers if available, or falls back to
 * local running Docker Compose containers (spendsync-postgres:5432 & spendsync-redis:6379).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractContainerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractContainerIntegrationTest.class);

    private static PostgreSQLContainer<?> postgresContainer;
    private static GenericContainer<?> redisContainer;
    private static boolean useTestcontainers = false;

    static {
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("spendsync_test_db")
                        .withUsername("spendsync_test")
                        .withPassword("test_secret")
                        .waitingFor(Wait.forListeningPort());

                redisContainer = new GenericContainer<>("redis:7.2-alpine")
                        .withExposedPorts(6379)
                        .waitingFor(Wait.forListeningPort());

                postgresContainer.start();
                redisContainer.start();
                useTestcontainers = true;
                log.info("Successfully started Testcontainers (PostgreSQL & Redis).");
            }
        } catch (Throwable t) {
            log.warn("Testcontainers Docker daemon not directly accessible ({}). Falling back to running Docker Compose instances.", t.getMessage());
            useTestcontainers = false;
        }
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        if (useTestcontainers && postgresContainer != null && redisContainer != null) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
            registry.add("spring.datasource.username", postgresContainer::getUsername);
            registry.add("spring.datasource.password", postgresContainer::getPassword);
            registry.add("spring.data.redis.host", redisContainer::getHost);
            registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
            registry.add("REDIS_HOST", redisContainer::getHost);
            registry.add("REDIS_PORT", () -> String.valueOf(redisContainer.getMappedPort(6379)));
        } else {
            // Fallback to active Docker Compose containers
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/spendsync_db");
            registry.add("spring.datasource.username", () -> "spendsync");
            registry.add("spring.datasource.password", () -> "spendsync_secret");
            registry.add("spring.data.redis.host", () -> "localhost");
            registry.add("spring.data.redis.port", () -> 6379);
            registry.add("REDIS_HOST", () -> "localhost");
            registry.add("REDIS_PORT", () -> "6379");
        }

        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }
}
