package com.enterprise.spendsync.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution capability across SpendSync modules.
 * Powers automated approval escalations, notification retention cleanup, etc.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
