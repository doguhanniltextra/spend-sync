# SpendSync — Procurement & Spend Management System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7.2-red.svg)](https://redis.io/)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.5-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

SpendSync is an enterprise procurement and spend management platform built with Spring Boot, Java 21, Redis, and React. It covers standard purchasing workflows: purchase requisitions, approval chains, purchase orders, goods receiving, 3-way invoice matching, payment batches, and a self-service vendor portal.

---

<details open>
<summary><h3>🏛️ System Architecture</h3></summary>

The application is structured as a hardened Modular Monolith. It integrates **PostgreSQL 16** as the persistent relational source of truth and **Redis 7.2** for high-throughput L2 multi-TTL caching, distributed rate limiting, and concurrency control.

```mermaid
graph TB
    subgraph Clients["🌐 Clients & Tools"]
        SPA["React SPA (:5173)"]
        VP["Vendor Portal"]
        INSIGHT["RedisInsight GUI (:5540)"]
    end

    subgraph Security["🔒 Security & Interceptor Layer"]
        TF["TenantFilter"]
        AUTH["JwtAuthenticationFilter"]
        RATE["Redis RateLimiter"]
    end

    subgraph Monolith["⚙️ SpendSync Core Engine (Spring Boot 3.3 / Java 21)"]
        subgraph DomainModules["Domain Modules"]
            direction TB
            M_CORE["Core (Tenants / Users)"]
            M_BGT["Budget & Requisitions"]
            M_CAT["Catalog & Purchasing"]
            M_RCV["Receiving & 3-Way Match"]
            M_PAY["Payment & Invoices"]
            M_GOV["Audit & Analytics"]
        end

        subgraph Infra["Shared Infrastructure"]
            CACHE_MGR["RedisCacheManager & Redisson Lock"]
            EVENT_BUS["Domain Event Bus"]
        end
    end

    subgraph Storage["💾 Persistence & In-Memory Tier"]
        DB[("🐘 PostgreSQL 16<br/><small>Relational Source of Truth</small>")]
        REDIS[("⚡ Redis 7.2<br/><small>Cache, Rate Limits, Locks</small>")]
    end

    Clients --> Security
    Security --> DomainModules
    RATE -.->|Sliding Window Check| REDIS
    DomainModules <--> Infra
    DomainModules -->|JPA / Hibernate| DB
    CACHE_MGR <-->|Sub-millisecond Cache / Locks| REDIS
    INSIGHT -.->|Database Profiling :6379| REDIS
```

</details>

---

<details>
<summary><h3>🔄 Purchasing Lifecycle Pipeline</h3></summary>

```mermaid
sequenceDiagram
    autonumber
    actor User as User (Requester / Approver)
    participant Req as Requisition & Budget
    participant PO as Purchasing
    actor Vendor as Vendor & Receiving
    participant Match as 3-Way Matching
    participant Pay as Treasury & Payment

    User->>Req: Submit PR & Check Budget
    Req->>Req: Evaluate Approval Chain (DoA Limits)
    User->>Req: Approve PR

    Req->>PO: Generate PO (PO-YYYY-XXXXX)
    PO->>Vendor: Dispatch PO & Delivery
    Vendor->>PO: Dock Inspection & Goods Receipt
    Vendor->>Match: Submit e-Invoice (UBL-TR / XML)
    Match->>Match: Execute 3-Way Match (PO vs GRN vs Invoice)
    alt Match Success
        Match->>Pay: Approve for Payment
    else Discrepancy Found
        Match->>User: Flag Discrepancy Hold
    end

    Pay->>Pay: Create Payment Batch (ISO 20022)
    Pay->>Vendor: Process Bank Payment
```

</details>

---

<details>
<summary><h3>🛠️ Tech Stack</h3></summary>

| Layer | Technologies |
| :--- | :--- |
| **Backend Framework** | Java 21, Spring Boot 3.3.0, Spring Data JPA, Spring Security, Spring AOP |
| **In-Memory & Caching** | Redis 7.2, Redisson 3.31.0, Spring Data Redis, Jackson2 JSON Serializer |
| **Persistence** | PostgreSQL 16, Hibernate 6, HikariCP |
| **Security & Rate Limiting** | JWT (JJWT), BCrypt, RBAC, Redis ZSet Sliding Window Rate Limiting |
| **Events** | Spring Domain Events (`ApplicationEventPublisher`) |
| **API & Documentation** | SpringDoc OpenAPI 2.5, Swagger UI, Bean Validation |
| **Frontend Framework** | React 18.3, TypeScript 5.5, Vite 5.4 |
| **State & Styling** | TanStack React Query v5, Zustand, TailwindCSS, Lucide Icons, Axios |
| **Infrastructure & Tools** | Docker (Multi-stage Layered JAR), Docker Compose, RedisInsight |

</details>

---

<details>
<summary><h3>🚀 Quickstart & Local Setup</h3></summary>

#### Prerequisites
- Java 21+
- Node.js 18+
- Docker & Docker Compose

#### 1. Start Infrastructure (PostgreSQL 16, Redis 7.2 & RedisInsight)
```bash
docker compose -f docker/docker-compose.yml up -d
```
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- RedisInsight Web GUI: `http://localhost:5540`

#### 2. Launch Backend
```bash
cd backend
mvn clean spring-boot:run
```
- API Server: `http://localhost:8080`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui.html`
- Health Probes: `http://localhost:8080/actuator/health`

#### 3. Launch Frontend
```bash
cd frontend
npm install
npm run dev
```
- Web Application: `http://localhost:5173`

</details>

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
