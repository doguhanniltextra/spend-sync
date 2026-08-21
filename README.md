# SpendSync — Enterprise Procure-to-Pay (P2P) Engine

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.5-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Kafka-3.7-black.svg)](https://kafka.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

SpendSync is a full-stack **Procure-to-Pay (P2P)** enterprise engine designed as a **Modular Monolith** with **Domain-Driven Design (DDD)** and **Event-Driven Architecture (EDA)**. It manages the complete procurement lifecycle: budget encumbrance, dynamic approval workflows (DAG), purchase orders, dock receiving, touchless 3-way invoice matching, treasury payment runs, and vendor self-service e-invoicing.

---

## 🏛️ High-Level System Architecture (HLD)

The application is structured into **11 decoupled bounded contexts** within a single deployable artifact. Modules communicate across domain boundaries primarily via asynchronous domain events (`ApplicationEventPublisher` / Apache Kafka).

```mermaid
graph TB
    subgraph Clients["Clients & Gateways"]
        WEB["React 18 SPA (Vite / TailwindCSS)"]
        PORTAL["Vendor Portal SPA"]
        EDI["e-Invoice / EDI Integrators"]
    end

    subgraph Security["Security & Tenancy Interceptors"]
        TF["TenantFilter (ThreadLocal Context)"]
        JWT["JwtAuthenticationFilter (HMAC-SHA512)"]
        RBAC["RolePermissionRegistry & Method Security"]
    end

    subgraph CoreEngine["SpendSync Modular Monolith"]
        subgraph ModCore["core"]
            TEN["Tenant & LegalEntity"]
            CC["CostCenter & Facility"]
            USR["User & SubAccount"]
        end

        subgraph ModBudget["budget"]
            BP["BudgetPool Ledger"]
            ENC["Encumbrance Engine"]
            TOL["Tolerance & Overrun Rules"]
        end

        subgraph ModReq["requisition"]
            PR["Purchase Requisition"]
            DAG["Approval Chain DAG"]
            LIMIT["DoA Signing Thresholds"]
        end

        subgraph ModCat["catalog"]
            ITEM["Item Master"]
            PRICE["Contract Pricing"]
        end

        subgraph ModPurch["purchasing"]
            PO["Purchase Order"]
            VEN["Vendor & VKN/TCKN Engine"]
            REV["Revision & Differential Budget"]
        end

        subgraph ModRec["receiving"]
            GRN["Goods Receipt (GRN)"]
            ODT["Over-Delivery Evaluator"]
            QC["Dock Inspection & Rejection"]
        end

        subgraph ModMatch["matching"]
            M3["Touchless 3-Way Matcher"]
            INV["Supplier Invoice"]
            HOLD["Discrepancy Hold Manager"]
        end

        subgraph ModPay["payment"]
            RUN["Payment Batch Engine"]
            XML["ISO 20022 pain.001 Generator"]
            AES["AES-256-GCM IBAN Vault"]
        end

        subgraph ModVP["vendorportal"]
            VPA["Magic Link Auth"]
            FLIP["PO-Flip Invoicing"]
            TAX["Withholding Tax Engine"]
            REC["Form BS Reconciliation"]
        end

        subgraph ModAudit["audit"]
            AUD["Append-Only Audit Ledger"]
            MASK["Regex Data Masker"]
            CHK["SHA-256 Checksum Validator"]
        end

        subgraph ModIntel["intelligence & analytics"]
            ANOM["Price Anomaly Detector (>50%)"]
            DUP["Duplicate Risk Scorer"]
            DECK["CFO Live Analytics Deck"]
        end
    end

    subgraph EventLayer["Event Layer"]
        BUS["Spring Domain Event Bus"]
        KAFKA["Apache Kafka Broker"]
    end

    subgraph Storage["Persistence Layer"]
        PG[("PostgreSQL 16")]
    end

    Clients --> Security
    Security --> CoreEngine
    CoreEngine <--> EventLayer
    CoreEngine --> Storage
    EventLayer --> KAFKA
```

---

## 🔄 End-to-End P2P Execution Pipeline

```mermaid
sequenceDiagram
    autonumber
    actor Req as Requisitioner
    actor App as Approver / CFO
    participant PR as requisition
    participant BGT as budget
    participant PO as purchasing
    actor Ven as Vendor
    participant RCV as receiving
    participant MTC as matching
    participant PAY as payment
    participant AUD as audit

    Req->>PR: Create PR (Items & Cost Center)
    PR->>BGT: Check & Reserve Funds (Encumbrance)
    BGT-->>PR: Funds Reserved
    PR->>PR: Build Dynamic Approval DAG
    App->>PR: Approve PR (Self-Approval Prohibited)
    PR->>BGT: Commit Reserved Funds
    PR->>AUD: Publish RequisitionApprovedEvent

    PR->>PO: Generate PO (PO-YYYY-XXXXX)
    PO->>PO: Validate Vendor Tax ID (VKN/TCKN)
    PO->>Ven: Dispatch PO (Email / Portal)
    PO->>AUD: Publish PurchaseOrderIssuedEvent

    Ven->>RCV: Delivery & Waybill
    RCV->>RCV: Verify Over-Delivery Tolerances & Quality
    RCV->>PO: Update Fulfillment (FULFILLED / PARTIAL)
    RCV->>AUD: Publish GoodsReceivedEvent

    Ven->>MTC: Submit Invoice (PO-Flip / UBL-TR)
    MTC->>MTC: Execute 3-Way Match Algorithm
    alt 3-Way Match Success
        MTC->>MTC: Status -> APPROVED_FOR_PAYMENT
        MTC->>BGT: Convert to Spent Funds
    else Discrepancy Found
        MTC->>MTC: Status -> DISCREPANCY_HOLD
    end
    MTC->>AUD: Publish InvoiceMatchedEvent

    PAY->>PAY: Aggregate Due Invoices into Batch
    App->>PAY: Approve Payment Batch (4-Eyes Principle)
    PAY->>PAY: Decrypt IBAN (AES-256-GCM) & Generate pain.001 XML
    PAY->>Ven: Dispatch Bank Transfer
    PAY->>AUD: Publish PaymentDispatchedEvent
```

---

## ⚙️ Core Technical Mechanics

### 1. Multi-Tenancy & Data Isolation
- **ThreadLocal Storage:** `TenantContext` holds current tenant ID per request.
- **HTTP Header Interception:** `TenantFilter` parses `X-Tenant-ID` with strict UUID validation and guaranteed `finally` cleanup.
- **Query Scoping:** Spring Data JPA repositories scope entities by `tenant_id`.

### 2. Encumbrance Accounting & Bütçe Havuzları
- **Fund Lifecycle:** `Allocated` $\rightarrow$ `Reserved` (on PR creation) $\rightarrow$ `Committed` (on PO issuance) $\rightarrow$ `Spent` (on invoice match).
- **Enforcement Modes:** `HARD_STOP` (strict ceiling), `TOLERANCE` (percentage-based dynamic ceiling), `SOFT_ALERT`.
- **Deadlock-Free Transfers:** Inter-pool balance adjustments acquire locks deterministically ordered by `costCenterId`.

### 3. Dynamic Approval Matrix (DAG) & Delegation of Authority (DoA)
- **Hierarchy-Aware Evaluation:** Evaluates cost center signing limits (e.g., Staff = 0, Lead = 50k, Director = 75k, CFO = Unlimited).
- **Segregation of Duties (SoD):** Enforces 4-eyes principle; prevents creator from approving their own PR or Payment Batch (`SOD_VIOLATION_SELF_APPROVAL`).

### 4. Touchless 3-Way Matching Engine
- **Evaluation Vector:** Compares **Purchase Order Line Items**, **Goods Receipt Accepted Quantities**, and **Supplier Invoice Lines**.
- **Tolerance Enforcement:** Configurable unit price (+-2%) and quantity discrepancy thresholds.
- **Automated Routing:** Matches without discrepancies transition directly to `APPROVED_FOR_PAYMENT`; exceptions trigger `DISCREPANCY_HOLD`.

### 5. Cryptography & Statutory Integrations
- **IBAN Encryption:** Vendor bank account IBANs are encrypted at rest using `AES-256-GCM` with random 12-byte IVs.
- **Tax Number Verification:** Algorithms validate 10-digit VKN (modulo-9/powers-of-2) and 11-digit TCKN check digits.
- **Withholding Tax (KDV Tevkifatı):** Computes statutory VAT deductions for standard GİB codes (`601`, `608`, `627`, `610`).
- **ISO 20022 Generation:** Produces `pain.001.001.03` Customer Credit Transfer Initiation XML messages.
- **Digital Reconciliation Seals:** Computes SHA-256 digital signatures for Form BS monthly reconciliation batches.

### 6. Append-Only Audit Trail
- **Immutability:** `AuditLog` entity has no update or delete operations (append-only ledger).
- **Isolation:** Saved via `@Transactional(propagation = Propagation.REQUIRES_NEW)` to persist logs even if parent transaction aborts.
- **Sensitive Data Masking:** Regex filters mask credentials (`"password": "********"`, `"token": "********"`).
- **Tamper-Evidence:** Each log entry stores a SHA-256 hash computed over `tenant:correlationId:action:entityType:entityId:amount:createdAt:actorId`.

---

## 🛠️ Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Framework** | Java 21 (LTS), Spring Boot 3.3.0, Spring Data JPA, Spring Security |
| **Persistence & Messaging** | PostgreSQL 16, Hibernate 6, HikariCP, Apache Kafka 3.7 (KRaft) |
| **Security & Crypto** | JJWT (HMAC-SHA512), AES-256-GCM, BCrypt |
| **API & Documentation** | SpringDoc OpenAPI 2.5, Swagger UI, Bean Validation |
| **Frontend Framework** | React 18.3, TypeScript 5.5, Vite 5.4 |
| **State & Styling** | TanStack React Query v5, Zustand, TailwindCSS, Lucide Icons, Axios |
| **Infrastructure** | Docker, Docker Compose |

---

## 🚀 Quickstart

### Prerequisites
- Java 21+
- Node.js 18+
- Docker & Docker Compose

### 1. Start Infrastructure (PostgreSQL & Kafka)
```bash
docker compose -f docker/docker-compose.yml up -d
```

### 2. Launch Backend
```bash
cd backend
mvn clean spring-boot:run
```
- API Server: `http://localhost:8080`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui.html`

### 3. Launch Frontend
```bash
cd frontend
npm install
npm run dev
```
- Web Application: `http://localhost:5173`

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

