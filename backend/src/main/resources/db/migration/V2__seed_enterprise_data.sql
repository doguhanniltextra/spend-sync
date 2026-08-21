-- =========================================================================
-- SpendSync Database Migration: V2__seed_enterprise_data.sql
-- Initial Enterprise Seed Data & Role Matrix
-- =========================================================================

-- 1. Master Tenant
INSERT INTO tenants (id, name, slug, is_active, subscription_tier, created_at, updated_at)
VALUES ('79ef8bff-1d87-4088-ab87-935989a568d5', 'SpendSync Global Inc.', 'spendsync-global', true, 'ENTERPRISE', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = NOW();

-- 2. Legal Entities
INSERT INTO legal_entities (id, tenant_id, name, company_code, tax_number, tax_office, base_currency, registered_address, country, is_active, created_at, updated_at)
VALUES 
('09e3a0db-e890-4b01-b966-98026fb26fc7', '79ef8bff-1d87-4088-ab87-935989a568d5', 'SpendSync Global Holding A.Ş.', 'SS-TR-01', '1234567890', 'Maslak', 'TRY', 'Büyükdere Cad. No:199 Maslak, Sariyer/Istanbul', 'TR', true, NOW(), NOW()),
('2a74c102-1234-4b01-b966-98026fb26fc8', '79ef8bff-1d87-4088-ab87-935989a568d5', 'SpendSync Technology Solutions Ltd.', 'SS-UK-02', 'GB99823145', 'London City', 'USD', '100 Bishopsgate, London EC2N 4AG', 'GB', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = NOW();

-- 3. Enterprise Users (Default Password: Password123!)
INSERT INTO users (id, tenant_id, email, password_hash, first_name, last_name, job_title, employee_id, country, timezone, preferred_language, is_active, is_email_verified, failed_login_attempts, created_at, updated_at)
VALUES
('a872e3fc-dd6d-4154-82e9-3018e544aab9', '79ef8bff-1d87-4088-ab87-935989a568d5', 'cfo@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'Doguhan', 'Admin', 'Chief Financial Officer', 'EMP-101', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW()),
('b1111111-1111-1111-1111-111111111111', '79ef8bff-1d87-4088-ab87-935989a568d5', 'eng.director@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'Alex', 'Carter', 'Director of Engineering', 'EMP-102', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW()),
('b2222222-2222-2222-2222-222222222222', '79ef8bff-1d87-4088-ab87-935989a568d5', 'devops.lead@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'Sarah', 'Connor', 'Lead DevOps & SRE', 'EMP-103', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW()),
('b3333333-3333-3333-3333-333333333333', '79ef8bff-1d87-4088-ab87-935989a568d5', 'procurement.head@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'Elena', 'Rostova', 'Head of Global Procurement', 'EMP-104', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW()),
('b4444444-4444-4444-4444-444444444444', '79ef8bff-1d87-4088-ab87-935989a568d5', 'ap.specialist@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'David', 'Miller', 'Senior AP Specialist', 'EMP-105', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW()),
('b5555555-5555-5555-5555-555555555555', '79ef8bff-1d87-4088-ab87-935989a568d5', 'senior.dev@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'James', 'Wilson', 'Staff Software Engineer', 'EMP-106', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email, updated_at = NOW();

-- 4. User Roles
INSERT INTO user_roles (user_id, role)
VALUES
('a872e3fc-dd6d-4154-82e9-3018e544aab9', 'ROOT_USER'),
('a872e3fc-dd6d-4154-82e9-3018e544aab9', 'APPROVER'),
('b1111111-1111-1111-1111-111111111111', 'APPROVER'),
('b1111111-1111-1111-1111-111111111111', 'REQUISITIONER'),
('b2222222-2222-2222-2222-222222222222', 'APPROVER'),
('b2222222-2222-2222-2222-222222222222', 'REQUISITIONER'),
('b3333333-3333-3333-3333-333333333333', 'PROCUREMENT'),
('b3333333-3333-3333-3333-333333333333', 'APPROVER'),
('b4444444-4444-4444-4444-444444444444', 'AP_SPECIALIST'),
('b4444444-4444-4444-4444-444444444444', 'ACCOUNT_USER'),
('b5555555-5555-5555-5555-555555555555', 'REQUISITIONER')
ON CONFLICT DO NOTHING;

-- 5. User Assigned Legal Entities
INSERT INTO user_assigned_legal_entities (user_id, legal_entity_id)
VALUES
('a872e3fc-dd6d-4154-82e9-3018e544aab9', '09e3a0db-e890-4b01-b966-98026fb26fc7'),
('a872e3fc-dd6d-4154-82e9-3018e544aab9', '2a74c102-1234-4b01-b966-98026fb26fc8'),
('b1111111-1111-1111-1111-111111111111', '09e3a0db-e890-4b01-b966-98026fb26fc7'),
('b2222222-2222-2222-2222-222222222222', '09e3a0db-e890-4b01-b966-98026fb26fc7'),
('b3333333-3333-3333-3333-333333333333', '09e3a0db-e890-4b01-b966-98026fb26fc7'),
('b4444444-4444-4444-4444-444444444444', '09e3a0db-e890-4b01-b966-98026fb26fc7'),
('b5555555-5555-5555-5555-555555555555', '09e3a0db-e890-4b01-b966-98026fb26fc7')
ON CONFLICT DO NOTHING;

-- 6. Facilities
INSERT INTO facilities (id, tenant_id, legal_entity_id, name, facility_code, facility_type, shipping_address, contact_person, contact_phone, is_active, created_at, updated_at)
VALUES
('f1111111-1111-1111-1111-111111111111', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'Maslak Financial Center HQ', 'FAC-01', 'OFFICE', 'Maslak Mah. Dereboyu 2 Cad. No:1 Sariyer/Istanbul', 'Ahmet Yilmaz', '+902123330001', true, NOW(), NOW()),
('f2222222-2222-2222-2222-222222222222', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'Gebze R&D Logistics & Tech Center', 'FAC-02', 'WAREHOUSE', 'GOSB Teknopark Cad. No:41 Gebze/Kocaeli', 'Mehmet Kaya', '+902626480002', true, NOW(), NOW()),
('f3333333-3333-3333-3333-333333333333', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'Ankara Government & Enterprise Office', 'FAC-03', 'OFFICE', 'Sogutozu Mah. 2176 Cad. No:7 Cankaya/Ankara', 'Zeynep Demir', '+903124440003', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = NOW();

-- 7. Cost Centers
INSERT INTO cost_centers (id, tenant_id, legal_entity_id, code, name, manager_user_id, is_active, created_at, updated_at)
VALUES
('c1000000-0000-0000-0000-000000000100', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'CC-100', 'Executive Strategy & Finance', 'a872e3fc-dd6d-4154-82e9-3018e544aab9', true, NOW(), NOW()),
('c2000000-0000-0000-0000-000000000200', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'CC-200', 'Core Engineering & R&D', 'b1111111-1111-1111-1111-111111111111', true, NOW(), NOW()),
('c3000000-0000-0000-0000-000000000300', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'CC-300', 'Cloud & Infrastructure Operations', 'b2222222-2222-2222-2222-222222222222', true, NOW(), NOW()),
('c4000000-0000-0000-0000-000000000400', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'CC-400', 'Global Marketing & Growth', 'a872e3fc-dd6d-4154-82e9-3018e544aab9', true, NOW(), NOW()),
('c5000000-0000-0000-0000-000000000500', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'CC-500', 'Corporate Facilities & Procurement', 'b3333333-3333-3333-3333-333333333333', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = NOW();

-- 8. Budget Pools (2026)
INSERT INTO budget_pools (id, tenant_id, legal_entity_id, cost_center_id, fiscal_year, period_type, period_value, status, enforcement_mode, tolerance_percentage, allocated_amount, spent_amount, reserved_amount, currency, created_at, updated_at)
VALUES
('a1000000-0000-0000-0000-000000000100', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c1000000-0000-0000-0000-000000000100', 2026, 'ANNUAL', 'ANNUAL', 'ACTIVE', 'HARD_STOP', 0.00, 5000000.0000, 450000.0000, 250000.0000, 'TRY', NOW(), NOW()),
('a2000000-0000-0000-0000-000000000200', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c2000000-0000-0000-0000-000000000200', 2026, 'ANNUAL', 'ANNUAL', 'ACTIVE', 'HARD_STOP', 0.00, 12000000.0000, 3200000.0000, 1800000.0000, 'TRY', NOW(), NOW()),
('a3000000-0000-0000-0000-000000000300', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c3000000-0000-0000-0000-000000000300', 2026, 'ANNUAL', 'ANNUAL', 'ACTIVE', 'HARD_STOP', 0.00, 8000000.0000, 4100000.0000, 1200000.0000, 'TRY', NOW(), NOW()),
('a4000000-0000-0000-0000-000000000400', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c4000000-0000-0000-0000-000000000400', 2026, 'ANNUAL', 'ANNUAL', 'ACTIVE', 'HARD_STOP', 0.00, 3500000.0000, 1900000.0000, 400000.0000, 'TRY', NOW(), NOW()),
('a5000000-0000-0000-0000-000000000500', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c5000000-0000-0000-0000-000000000500', 2026, 'ANNUAL', 'ANNUAL', 'ACTIVE', 'HARD_STOP', 0.00, 4000000.0000, 850000.0000, 300000.0000, 'TRY', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET allocated_amount = EXCLUDED.allocated_amount, updated_at = NOW();

-- 9. Approval Authority Limits (DoA Matrix)
INSERT INTO approval_authority_limits (id, tenant_id, user_id, legal_entity_id, cost_center_id, approval_level, min_amount, max_amount, currency, is_active, created_at, updated_at)
VALUES
('e1111111-1111-1111-1111-111111111111', '79ef8bff-1d87-4088-ab87-935989a568d5', 'b2222222-2222-2222-2222-222222222222', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c3000000-0000-0000-0000-000000000300', 1, 0.0000, 50000.0000, 'TRY', true, NOW(), NOW()),
('e2222222-2222-2222-2222-222222222222', '79ef8bff-1d87-4088-ab87-935989a568d5', 'b1111111-1111-1111-1111-111111111111', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c2000000-0000-0000-0000-000000000200', 2, 0.0000, 75000.0000, 'TRY', true, NOW(), NOW()),
('e3333333-3333-3333-3333-333333333333', '79ef8bff-1d87-4088-ab87-935989a568d5', 'b3333333-3333-3333-3333-333333333333', '09e3a0db-e890-4b01-b966-98026fb26fc7', NULL, 2, 0.0000, 250000.0000, 'TRY', true, NOW(), NOW()),
('e4444444-4444-4444-4444-444444444444', '79ef8bff-1d87-4088-ab87-935989a568d5', 'a872e3fc-dd6d-4154-82e9-3018e544aab9', '09e3a0db-e890-4b01-b966-98026fb26fc7', NULL, 99, 0.0000, NULL, 'TRY', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET max_amount = EXCLUDED.max_amount, updated_at = NOW();

-- 10. Preferred Vendors
INSERT INTO vendors (id, tenant_id, name, tax_number, tax_office, category, tier, is_einvoice_registered, order_email, phone_number, address, city, country, payment_terms, bank_name, iban, status, created_at, updated_at)
VALUES
('dd100000-0000-0000-0000-000000000100', '79ef8bff-1d87-4088-ab87-935989a568d5', 'Apple Distribution TR', '1112223334', 'Besiktas', 'IT_HARDWARE', 'TIER_1_STRATEGIC', true, 'orders@apple-dist.com', '+902123990001', 'Levent 199, Buyukdere Cad. No:199 Sisli/Istanbul', 'Istanbul', 'TR', 'NET_30', 'Garanti BBVA', 'TR330006200011112222333344', 'ACTIVE', NOW(), NOW()),
('dd200000-0000-0000-0000-000000000200', '79ef8bff-1d87-4088-ab87-935989a568d5', 'AWS EMEA SARL (Turkiye Branch)', '5556667778', 'Marmara', 'SOFTWARE_SAAS', 'TIER_1_STRATEGIC', true, 'billing@amazon.com', '+902128880002', 'River Plaza, Kat:11 Buyukdere Cad. Sisli/Istanbul', 'Istanbul', 'TR', 'NET_30', 'Is Bankasi', 'TR640006400000111122223333', 'ACTIVE', NOW(), NOW()),
('dd300000-0000-0000-0000-000000000300', '79ef8bff-1d87-4088-ab87-935989a568d5', 'Dell Technologies TR', '9998887776', 'Kadikoy', 'IT_HARDWARE', 'TIER_2_PREFERRED', true, 'enterprise@dell-tr.com', '+902165550003', 'Palladium Tower, Barbaros Mah. Atasehir/Istanbul', 'Istanbul', 'TR', 'NET_45', 'Akbank', 'TR120004600001234567890123', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = NOW();

-- 11. Vendor Users (Tedarikçi Girişi: vendor@apple-dist.com / Password123!)
INSERT INTO vendor_users (id, tenant_id, vendor_id, email, password_hash, full_name, phone_number, role, is_primary_contact, is_active, created_at, updated_at)
VALUES
('ee100000-0000-0000-0000-000000000100', '79ef8bff-1d87-4088-ab87-935989a568d5', 'dd100000-0000-0000-0000-000000000100', 'vendor@apple-dist.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'Can Yilmaz', '+905321112233', 'VENDOR_ADMIN', true, true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email, updated_at = NOW();

-- 12. Catalog Categories & Items
INSERT INTO catalog_categories (id, tenant_id, parent_id, code, name, description, icon_code, full_path, is_active, created_at, updated_at)
VALUES
('ca100000-0000-0000-0000-000000000100', '79ef8bff-1d87-4088-ab87-935989a568d5', NULL, 'CAT-IT', 'Information Technology', 'Enterprise IT Hardware & Infrastructure', 'Laptop', 'Information Technology', true, NOW(), NOW()),
('ca110000-0000-0000-0000-000000000110', '79ef8bff-1d87-4088-ab87-935989a568d5', 'ca100000-0000-0000-0000-000000000100', 'CAT-HW-LAPTOP', 'Laptops & Workstations', 'Engineering laptops and displays', 'Monitor', 'Information Technology > Laptops & Workstations', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = NOW();

INSERT INTO catalog_items (id, tenant_id, item_code, name, description, category_id, preferred_vendor_id, unit_price, currency, vat_rate, unit_of_measure, contract_reference, valid_from, valid_until, is_active, is_preferred, gl_account_code, created_by_user_id, created_at, updated_at)
VALUES
('cc100000-0000-0000-0000-000000000100', '79ef8bff-1d87-4088-ab87-935989a568d5', 'MAC-M3-14', 'MacBook Pro 14 M3 Pro (36GB RAM / 1TB SSD)', 'Apple Silicon M3 Pro high-performance workstation for core software engineering team', 'ca110000-0000-0000-0000-000000000110', 'dd100000-0000-0000-0000-000000000100', 85000.0000, 'TRY', 0.2000, 'PIECE', 'CNT-APPLE-2026', '2026-01-01', '2026-12-31', true, true, '770.01.001', 'b3333333-3333-3333-3333-333333333333', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = NOW();
