package com.enterprise.spendsync.shared.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Flyway configuration providing profile-aware migration strategy.
 * In development/test environments, executes flyway.repair() to synchronize checksums before applying migrations.
 * In production profiles ("prod", "production", "staging"), flyway.repair() is skipped to uphold strict migration governance.
 */
@Configuration
public class FlywayConfig {

    private final Environment environment;

    public FlywayConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            boolean isProd = environment.acceptsProfiles(Profiles.of("prod", "production", "staging"));
            if (!isProd) {
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
