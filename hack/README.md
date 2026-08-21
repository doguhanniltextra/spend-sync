# SpendSync Hack & Developer Quickstart Toolkit

This directory contains rapid development scripts, local stack starters, and ready-to-use HTTP REST client collections for testing all SpendSync API endpoints.

---

## Directory Structure

```
hack/
├── README.md                          # Developer guide and environment references
├── dev-start.ps1                      # Windows PowerShell full-stack launcher
├── dev-start.sh                       # Linux / macOS / WSL full-stack launcher
└── http/                              # REST Client (IntelliJ / VS Code) request files
    ├── 01_auth_and_org.http           # Authentication, company, and organization management
    ├── 02_catalog_and_requisition.http# Catalog search, CSV import/export, PR and approval flow
    ├── 03_purchasing_and_receiving.http# PO issuance, ASN waybill dispatch, Goods Receipt (GRN)
    ├── 04_matching_and_invoicing.http # PO-Flip e-invoice, UBL XML upload, 3-Way match, XSLT
    ├── 05_vendor_portal_finance.http  # Early pay (2% cash discount), SOA, BA-BS reconciliation
    └── 06_intelligence_and_analytics.http # CFO Executive Pulse, RAG Policy Copilot, What-If
```

---

## 1. Quick Start

### Windows (PowerShell):
```powershell
# 1. Start Docker Infrastructure (Postgres & Redis)
docker compose up -d

# 2. Start Backend (Port 8080)
cd backend
mvn spring-boot:run

# 3. Start Frontend (Port 5173 - In separate terminal)
cd ../frontend
npm run dev
```

### Linux / macOS:
```bash
chmod +x hack/dev-start.sh
./hack/dev-start.sh
```

---

## 2. Seed Test Accounts & Role Matrix

Default password for all test accounts: `Password123!`

| Email | Primary Role | Title / Department | Signing Limit (DoA) |
|---|---|---|---|
| `cfo@spendsync.com` | `ROOT_USER`, `APPROVER` | Chief Financial Officer | Unlimited (CFO) |
| `procurement.head@spendsync.com` | `PROCUREMENT` | Head of Global Procurement | 250,000 TRY |
| `eng.director@spendsync.com` | `APPROVER`, `REQUISITIONER` | Director of Engineering | 75,000 TRY |
| `ap.specialist@spendsync.com` | `AP_SPECIALIST` | Senior AP Specialist | Operational |
| `senior.dev@spendsync.com` | `REQUISITIONER` | Staff Software Engineer | Standard Requisitioner |
| `vendor@apple-dist.com` | `VENDOR_ADMIN` | Vendor Portal Administrator | Strategic Vendor |

---

## 3. Using HTTP Request Files (.http)

Use IntelliJ IDEA Ultimate, VS Code (with REST Client or Thunder Client extension), or WebStorm to execute requests in `hack/http/`.

### Automatic Token Chaining:
When you run `Login as CFO` in `01_auth_and_org.http`, the response JWT token is automatically captured into the `{{cfoToken}}` variable and reused across subsequent request files.
