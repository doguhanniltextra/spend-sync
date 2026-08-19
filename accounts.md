# SpendSync Enterprise Accounts & Environment Directory

> [!NOTE]
> **Tenant ID:** `79ef8bff-1d87-4088-ab87-935989a568d5` (SpendSync Global Inc.)  
> **Default Password for All Users:** `Password123!`  
> **API Server:** `http://localhost:8080`  
> **Web Application:** `http://localhost:5173`  

---

## 👥 1. Enterprise User Accounts & Role Matrix

Use these credentials to test different personas and authorization flows across the Procure-to-Pay (P2P) lifecycle.

| Full Name | Email | Password | Primary Role | Title / Department | Signing Limit (DoA) | Managed Cost Center |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: |
| **Doguhan Admin** | `cfo@spendsync.com` | `Password123!` | `ROOT_USER`, `APPROVER` | Chief Financial Officer | **Unlimited (CFO)** | `CC-100`, `CC-400` |
| **Alex Carter** | `eng.director@spendsync.com` | `Password123!` | `APPROVER`, `REQUISITIONER` | Director of Engineering | **75.000,00 TRY** | `CC-200` |
| **Sarah Connor** | `devops.lead@spendsync.com` | `Password123!` | `APPROVER`, `REQUISITIONER` | Lead DevOps & Cloud SRE | **50.000,00 TRY** | `CC-300` |
| **Elena Rostova** | `procurement.head@spendsync.com` | `Password123!` | `PROCUREMENT`, `APPROVER` | Head of Global Procurement | **250.000,00 TRY** | `CC-500` |
| **David Miller** | `ap.specialist@spendsync.com` | `Password123!` | `AP_SPECIALIST`, `ACCOUNT_USER` | Senior AP Specialist | *Operational* | — |
| **James Wilson** | `senior.dev@spendsync.com` | `Password123!` | `REQUISITIONER` | Staff Software Engineer | *Standard Requisitioner* | — |

---

## 🏢 2. Legal Entities & Facilities

### Legal Entities (Tüzel Kişilikler)
1. **SpendSync Global Holding A.Ş.**
   - **ID:** `09e3a0db-e890-4b01-b966-98026fb26fc7`
   - **Company Code:** `SS-TR-01`
   - **Currency:** `TRY`
   - **Address:** Büyükdere Cad. No:199 Maslak, Sariyer / Istanbul

2. **SpendSync Technology Solutions Ltd.**
   - **ID:** `2a74c102-1234-4b01-b966-98026fb26fc8`
   - **Company Code:** `SS-UK-02`
   - **Currency:** `USD`
   - **Address:** 100 Bishopsgate, London EC2N 4AG / United Kingdom

### Delivery Facilities (Tesisler)
- **`FAC-01` • Maslak Financial Center HQ** (Ofis / Maslak, İstanbul)
- **`FAC-02` • Gebze R&D Logistics & Tech Center** (Depo & Veri Merkezi / Gebze, Kocaeli)
- **`FAC-03` • Ankara Government & Enterprise Office** (Ofis / Söğütözü, Ankara)

---

## 📊 3. Cost Centers & 2026 Budget Ledger

| Code | Cost Center Name | Manager | 2026 Allocated | Spent Amount | Reserved / Committed | Remaining Available |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: |
| **`CC-100`** | Executive Strategy & Finance | `Doguhan Admin` | **5.000.000,00 TRY** | 450.000,00 TRY | 250.000,00 TRY | **4.300.000,00 TRY** |
| **`CC-200`** | Core Engineering & R&D | `Alex Carter` | **12.000.000,00 TRY** | 3.200.000,00 TRY | 1.800.000,00 TRY | **7.000.000,00 TRY** |
| **`CC-300`** | Cloud & Infrastructure Operations | `Sarah Connor` | **8.000.000,00 TRY** | 4.100.000,00 TRY | 1.200.000,00 TRY | **2.700.000,00 TRY** |
| **`CC-400`** | Global Marketing & Growth | `Doguhan Admin` | **3.500.000,00 TRY** | 1.900.000,00 TRY | 400.000,00 TRY | **1.200.000,00 TRY** |
| **`CC-500`** | Corporate Facilities & Procurement | `Elena Rostova` | **4.000.000,00 TRY** | 850.000,00 TRY | 300.000,00 TRY | **2.850.000,00 TRY** |

---

## 📋 4. Sample Purchase Requisitions (Live Database Records)

| PR Number | Title | Requester | Cost Center | Total Amount | Status | Approval Workflow State |
| :--- | :--- | :--- | :---: | :---: | :---: | :--- |
| **`PR-20260819-0001`** | Q3 AWS & Kubernetes Cloud Infrastructure Capacity Expansion | `Sarah Connor` | `CC-300` | **45.000,00 TRY** | ⏱️ `PENDING_APPROVAL` | Step 1: `cfo@spendsync.com` (Pending Signature) |
| **`PR-20260819-0002`** | Engineering Team M3 Max High-Performance Workstation Upgrades | `James Wilson` | `CC-200` | **128.000,00 TRY** | ⏱️ `PENDING_APPROVAL` | Step 1: `Alex Carter` (Approved) → Step 2: `cfo@spendsync.com` (Pending Signature) |
| **`PR-20260819-0003`** | Datadog Enterprise APM & SIEM Security Monitoring Renewal | `Sarah Connor` | `CC-300` | **92.000,00 TRY** | ✅ `APPROVED` | Step 1: `cfo@spendsync.com` (Approved) |
| **`PR-20260819-0004`** | Executive Boardroom Video Conferencing Hardware Setup | `Elena Rostova` | `CC-500` | **15.000,00 TRY** | ❌ `REJECTED` | Step 1: `cfo@spendsync.com` (Rejected: *"Alternative vendor identified at 40% lower cost."*) |

---

## 🧪 5. Testing Persona Cheat-Sheet

1. **Testing Executive Decision Pulse (CFO Persona):**
   - Login: `cfo@spendsync.com` / `Password123!`
   - See total enterprise spend (32.5M TRY pool), budget burn rate, and approve `PR-20260819-0001` / `PR-20260819-0002` directly in the **Approval Queue** (`/approvals`).

2. **Testing Department Tier-1 Approval (Engineering Director):**
   - Login: `eng.director@spendsync.com` / `Password123!`
   - See Engineering cost center (`CC-200`) requests and approve within 75.000 TRY limit.

3. **Testing PR Creation & Live Budget Bar (DevOps SRE Lead):**
   - Login: `devops.lead@spendsync.com` / `Password123!`
   - Navigate to `/requisitions/new`, select `CC-300`, and watch the live budget progress bar calculate remaining funds in real-time.

4. **Testing SoD Violation Guard (Requisitioner):**
   - Login: `senior.dev@spendsync.com` / `Password123!`
   - Create a requisition and attempt to self-approve; observe strict Segregation of Duties prevention.
