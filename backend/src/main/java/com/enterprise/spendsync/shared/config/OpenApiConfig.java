package com.enterprise.spendsync.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 & Swagger UI Configuration for SpendSync.
 * Includes Global X-Tenant-Id header security scheme for interactive testing.
 */
@Configuration
public class OpenApiConfig {

    public static final String TENANT_HEADER_SCHEME = "TenantHeader";

    @Bean
    public OpenAPI spendSyncOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SpendSync Enterprise API")
                        .description("Procure-to-Pay (P2P) Engine REST API Documentation with Multi-Tenancy and RBAC Security.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("SpendSync Platform Team")
                                .email("dev@spendsync.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(TENANT_HEADER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(TENANT_HEADER_SCHEME,
                                new SecurityScheme()
                                        .name("X-Tenant-Id")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Active Tenant UUID required for protected enterprise operations.")));
    }
}
