# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[1.0.0]: https://github.com/doguhanniltextra/spend-sync/releases/tag/v1.0.0
