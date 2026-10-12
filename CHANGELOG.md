# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **AWS Budget & FinOps Cost Alerting**: Multi-tier AWS budget monitoring (overall monthly $50, RDS $20, ECS $15) with Amazon SNS fanout, Go Lambda handler on AWS Graviton (`arm64`, `provided.al2023`), and formatted email delivery via Amazon SES with direct SNS fallback.
- **Automated CI/CD Changelog Enforcement**: Pull Request workflow verifying Keep a Changelog syntax and ensuring code changes are documented before merging.

### Fixed
- **ECS Service Connect Cloud Map Naming Collision (INC-018)**: Disambiguated Service Connect entity name (`discovery_name = "backend-connect"`) to resolve HTTP 400 collision with standalone Route 53 service discovery while preserving `client_alias` for internal Prometheus metrics scraping.

## [1.0.0] - 2026-08-24

### Added
- **Core Procurement Workflow**: End-to-end purchasing lifecycle including Purchase Requisitions (PR), Delegation of Authority (DoA) approvals, Purchase Orders (PO), and Goods Receipt Notes (GRN).
- **Automated 3-Way Matching**: Intelligent automated invoice matching across PO, GRN, and vendor e-Invoices (UBL-TR / XML support).
- **Payment & Treasury Engine**: Payment batch orchestration with ISO 20022 schema compliance.
- **Self-Service Vendor Portal**: Dedicated vendor interface for quote submissions, PO acknowledgement, and delivery tracking.
- **Enterprise Caching & Concurrency**: Redis 7.2 L2 multi-TTL cache manager and Redisson pessimistic/distributed locking for concurrent budget allocations.
- **Traffic Control & Security**: Redis ZSet sliding-window distributed rate limiter, JWT authentication, and tenant isolation filter.
- **Observability & Logging**: Structured JSON logging, MDC-based distributed tracing, and PII masking interceptors.
- **Database Migrations**: Domain-driven atomic Flyway migration scripts with baseline schema management.
- **Documentation & Tools**: Interactive Swagger/OpenAPI 2.5 docs, HTML documentation portal, and developer quickstart HTTP client scripts.
- **Automated Test Suite**: 512 automated unit and integration tests (78.46% Line Coverage, 54.08% Branch Coverage) with Testcontainers (PostgreSQL 16 & Redis 7.2).

[Unreleased]: https://github.com/doguhanniltextra/spend-sync/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/doguhanniltextra/spend-sync/releases/tag/v1.0.0
