package com.enterprise.spendsync.shared.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.RepairResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Flyway Migration Governance Configuration.
 *
 * <h2>Governance Contract</h2>
 * <ol>
 *   <li><strong>Normal startup (all profiles):</strong> only {@code migrate()} runs.
 *       No automatic repair is ever performed on startup.</li>
 *   <li><strong>Checksum deviation:</strong> if a committed migration file is modified
 *       after it has already been applied, Flyway will throw a
 *       {@code FlywayValidateException} and the application will REFUSE to start.
 *       This is intentional — a deviation must be reviewed by a human.</li>
 *   <li><strong>Repair:</strong> only runs when the system property
 *       {@code FLYWAY_REPAIR=true} is explicitly passed by an operator.
 *       This must NEVER be set in any automated startup or CI pipeline.</li>
 * </ol>
 *
 * <h2>How to run a repair intentionally (operator-only)</h2>
 * <pre>
 *   FLYWAY_REPAIR=true java -jar app.jar
 *   # or via Docker:
 *   docker run -e FLYWAY_REPAIR=true spendsync-backend:latest
 * </pre>
 *
 * <p>Every repair execution is logged at WARN level so it is visible in audit trails.
 *
 * <h2>Out-of-order migration policy</h2>
 * {@code out-of-order} is intentionally disabled in this class to prevent silent
 * migration ordering surprises. If you need it enabled, do so explicitly in
 * {@code application-dev.yml} only.
 */
@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    /**
     * Environment variable / system property that must be explicitly set to {@code "true"}
     * by an operator to authorise a repair run.  It is NEVER set automatically.
     */
    public static final String REPAIR_FLAG = "FLYWAY_REPAIR";

    private final Environment environment;

    public FlywayConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            boolean repairRequested = isRepairRequested();

            if (repairRequested) {
                log.warn("╔══════════════════════════════════════════════════════════════╗");
                log.warn("║  FLYWAY REPAIR — OPERATOR-INITIATED MAINTENANCE MODE         ║");
                log.warn("║  FLYWAY_REPAIR=true was explicitly set by an operator.       ║");
                log.warn("║  Repairing flyway_schema_history checksums NOW.              ║");
                log.warn("║  This action MUST be reviewed in the audit trail.            ║");
                log.warn("╚══════════════════════════════════════════════════════════════╝");

                RepairResult result = flyway.repair();

                if (result.repairActions != null && !result.repairActions.isEmpty()) {
                    log.warn("[FLYWAY-REPAIR] Repair actions applied: {}", result.repairActions);
                } else {
                    log.warn("[FLYWAY-REPAIR] Repair completed — no corrective actions were needed.");
                }
            }

            // Always run migrate after optional repair.
            // Flyway's validate() runs automatically before migrate() and will throw
            // FlywayValidateException if any checksum mismatch is detected,
            // causing the application to refuse startup — this is the intended fail-fast behaviour.
            log.info("[FLYWAY] Running migration — strategy: migrate-only, repair: {}", repairRequested);
            flyway.migrate();
        };
    }

    /**
     * Returns {@code true} only when the {@value #REPAIR_FLAG} environment variable
     * or system property is explicitly set to {@code "true"} (case-insensitive).
     *
     * <p>Checks both OS environment variables and JVM system properties so that
     * the flag can be injected either via {@code docker run -e FLYWAY_REPAIR=true}
     * or {@code java -DFLYWAY_REPAIR=true -jar app.jar}.
     */
    private boolean isRepairRequested() {
        // 1. JVM system property (highest precedence)
        String sysProp = System.getProperty(REPAIR_FLAG);
        if ("true".equalsIgnoreCase(sysProp)) {
            return true;
        }
        // 2. OS environment variable
        String envVar = System.getenv(REPAIR_FLAG);
        if ("true".equalsIgnoreCase(envVar)) {
            return true;
        }
        // 3. Spring Environment (application.yml / bootstrap.yml — must be explicit, not defaulted)
        String springProp = environment.getProperty(REPAIR_FLAG);
        return "true".equalsIgnoreCase(springProp);
    }
}
