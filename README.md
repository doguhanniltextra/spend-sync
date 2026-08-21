# SpendSync — Procurement & Spend Management System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.5-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

SpendSync is a procurement and spend management application built with Spring Boot and React. It covers standard purchasing workflows: purchase requisitions, approval chains, purchase orders, goods receiving, 3-way invoice matching, payment batches, and a self-service vendor portal.

---

<details open>
<summary><h3>🏛️ System Architecture</h3></summary>

The application is structured into modular domain packages within a Spring Boot backend, communicating through in-memory Spring Domain Events (`ApplicationEventPublisher`).

```mermaid
graph TB
    subgraph Clients["Clients"]
        SPA["Web Application (React / Vite)"]
        VP["Vendor Portal"]
    end

    subgraph Security["Security & Context Layer"]
        TF["TenantFilter (Multi-Tenancy)"]
        AUTH["JwtAuthenticationFilter & RBAC"]
    end

    subgraph Monolith["SpendSync Core Engine"]
        subgraph DomainModules["Application Modules"]
            M_CORE["core<br/><small>Tenants, Legal Entities, Users</small>"]
            M_BGT["budget & requisition<br/><small>Budget Pools, PR Approvals</small>"]
            M_CAT["catalog & purchasing<br/><small>Item Master, PO Lifecycle</small>"]
            M_RCV["receiving & matching<br/><small>Goods Receipts, 3-Way Match</small>"]
            M_PAY["payment & vendorportal<br/><small>Payment Runs, Vendor Portal</small>"]
            M_GOV["audit & analytics<br/><small>Audit Logs, Analytics</small>"]
        end

        EVENT_BUS["Domain Event Bus (ApplicationEventPublisher)"]
    end

    subgraph Storage["Database"]
        DB[("PostgreSQL 16")]
    end

    Clients --> Security
    Security --> Monolith
    DomainModules <--> EVENT_BUS
    Monolith --> DB
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
    Req->>Req: Evaluate Approval Chain
    User->>Req: Approve PR

    Req->>PO: Generate PO (PO-YYYY-XXXXX)
    PO->>Vendor: Dispatch PO & Delivery
    Vendor->>PO: Dock Inspection & Goods Receipt

    Vendor->>Match: Submit Invoice
    Match->>Match: Execute 3-Way Match (PO vs GRN vs Invoice)
    alt Match Success
        Match->>Pay: Approve for Payment
    else Discrepancy Found
        Match->>User: Flag Discrepancy Hold
    end

    Pay->>Pay: Create Payment Batch
    Pay->>Vendor: Process Bank Payment
```

</details>

---

<details>
<summary><h3>🛠️ Tech Stack</h3></summary>

| Layer | Technologies |
| :--- | :--- |
| **Backend Framework** | Java 21, Spring Boot 3.3.0, Spring Data JPA, Spring Security |
| **Persistence** | PostgreSQL 16, Hibernate 6, HikariCP |
| **Security & Auth** | JWT, BCrypt, Role-Based Access Control |
| **Events** | Spring Domain Events (`ApplicationEventPublisher`) |
| **API & Documentation** | SpringDoc OpenAPI 2.5, Swagger UI, Bean Validation |
| **Frontend Framework** | React 18.3, TypeScript 5.5, Vite 5.4 |
| **State & Styling** | TanStack React Query v5, Zustand, TailwindCSS, Lucide Icons, Axios |
| **Infrastructure** | Docker, Docker Compose |

</details>

---

<details>
<summary><h3>🚀 Quickstart & Local Setup</h3></summary>

#### Prerequisites
- Java 21+
- Node.js 18+
- Docker & Docker Compose

#### 1. Start Database (PostgreSQL)
```bash
docker compose -f docker/docker-compose.yml up -d
```

#### 2. Launch Backend
```bash
cd backend
mvn clean spring-boot:run
```
- API Server: `http://localhost:8080`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui.html`

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
