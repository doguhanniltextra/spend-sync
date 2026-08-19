-- Clean previous tenant-specific data
DELETE FROM requisition_approval_steps WHERE tenant_id = '79ef8bff-1d87-4088-ab87-935989a568d5';
DELETE FROM requisition_line_items WHERE tenant_id = '79ef8bff-1d87-4088-ab87-935989a568d5';
DELETE FROM purchase_requisitions WHERE tenant_id = '79ef8bff-1d87-4088-ab87-935989a568d5';
DELETE FROM approval_authority_limits WHERE tenant_id = '79ef8bff-1d87-4088-ab87-935989a568d5';
DELETE FROM budget_pools WHERE tenant_id = '79ef8bff-1d87-4088-ab87-935989a568d5';
DELETE FROM cost_centers WHERE tenant_id = '79ef8bff-1d87-4088-ab87-935989a568d5';
DELETE FROM facilities WHERE tenant_id = '79ef8bff-1d87-4088-ab87-935989a568d5';
DELETE FROM user_assigned_legal_entities WHERE user_id IN (SELECT id FROM users WHERE tenant_id = '79ef8bff-1d87-4088-ab87-935989a568d5' AND email != 'cfo@spendsync.com');
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE tenant_id = '79ef8bff-1d87-4088-ab87-935989a568d5' AND email != 'cfo@spendsync.com');
DELETE FROM users WHERE tenant_id = '79ef8bff-1d87-4088-ab87-935989a568d5' AND email != 'cfo@spendsync.com';

-- 1. Legal Entities
UPDATE legal_entities 
SET name = 'SpendSync Global Holding A.Ş.', registered_address = 'Büyükdere Cad. No:199 Maslak, Sariyer/Istanbul', updated_at = NOW()
WHERE id = '09e3a0db-e890-4b01-b966-98026fb26fc7';

INSERT INTO legal_entities (id, tenant_id, name, company_code, tax_number, tax_office, base_currency, registered_address, country, is_active, created_at, updated_at)
VALUES 
('2a74c102-1234-4b01-b966-98026fb26fc8', '79ef8bff-1d87-4088-ab87-935989a568d5', 'SpendSync Technology Solutions Ltd.', 'SS-UK-02', 'GB99823145', 'London City', 'USD', '100 Bishopsgate, London EC2N 4AG', 'GB', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = NOW();

-- 2. Enterprise Users (Password: Password123!)
INSERT INTO users (id, tenant_id, email, password_hash, first_name, last_name, job_title, employee_id, country, timezone, preferred_language, is_active, is_email_verified, failed_login_attempts, created_at, updated_at)
VALUES
('b1111111-1111-1111-1111-111111111111', '79ef8bff-1d87-4088-ab87-935989a568d5', 'eng.director@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'Alex', 'Carter', 'Director of Engineering', 'EMP-102', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW()),
('b2222222-2222-2222-2222-222222222222', '79ef8bff-1d87-4088-ab87-935989a568d5', 'devops.lead@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'Sarah', 'Connor', 'Lead DevOps & SRE', 'EMP-103', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW()),
('b3333333-3333-3333-3333-333333333333', '79ef8bff-1d87-4088-ab87-935989a568d5', 'procurement.head@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'Elena', 'Rostova', 'Head of Global Procurement', 'EMP-104', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW()),
('b4444444-4444-4444-4444-444444444444', '79ef8bff-1d87-4088-ab87-935989a568d5', 'ap.specialist@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'David', 'Miller', 'Senior AP Specialist', 'EMP-105', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW()),
('b5555555-5555-5555-5555-555555555555', '79ef8bff-1d87-4088-ab87-935989a568d5', 'senior.dev@spendsync.com', '$2a$12$CO5xCKN9VjrBFilVNw92d.LPBCKM0Re4V1Io4MEzvJuUO4Ys8B/Ku', 'James', 'Wilson', 'Staff Software Engineer', 'EMP-106', 'TR', 'UTC', 'tr', true, true, 0, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email, updated_at = NOW();

-- 3. User Roles
INSERT INTO user_roles (user_id, role)
VALUES
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

-- 4. User Assigned Legal Entities
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

-- 5. Facilities
INSERT INTO facilities (id, tenant_id, legal_entity_id, name, facility_code, facility_type, shipping_address, contact_person, contact_phone, is_active, created_at, updated_at)
VALUES
('f1111111-1111-1111-1111-111111111111', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'Maslak Financial Center HQ', 'FAC-01', 'OFFICE', 'Maslak Mah. Dereboyu 2 Cad. No:1 Sariyer/Istanbul', 'Ahmet Yilmaz', '+902123330001', true, NOW(), NOW()),
('f2222222-2222-2222-2222-222222222222', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'Gebze R&D Logistics & Tech Center', 'FAC-02', 'WAREHOUSE', 'GOSB Teknopark Cad. No:41 Gebze/Kocaeli', 'Mehmet Kaya', '+902626480002', true, NOW(), NOW()),
('f3333333-3333-3333-3333-333333333333', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'Ankara Government & Enterprise Office', 'FAC-03', 'OFFICE', 'Sogutozu Mah. 2176 Cad. No:7 Cankaya/Ankara', 'Zeynep Demir', '+903124440003', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = NOW();

-- 6. Cost Centers
INSERT INTO cost_centers (id, tenant_id, legal_entity_id, code, name, manager_user_id, is_active, created_at, updated_at)
VALUES
('c1000000-0000-0000-0000-000000000100', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'CC-100', 'Executive Strategy & Finance', 'a872e3fc-dd6d-4154-82e9-3018e544aab9', true, NOW(), NOW()),
('c2000000-0000-0000-0000-000000000200', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'CC-200', 'Core Engineering & R&D', 'b1111111-1111-1111-1111-111111111111', true, NOW(), NOW()),
('c3000000-0000-0000-0000-000000000300', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'CC-300', 'Cloud & Infrastructure Operations', 'b2222222-2222-2222-2222-222222222222', true, NOW(), NOW()),
('c4000000-0000-0000-0000-000000000400', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'CC-400', 'Global Marketing & Growth', 'a872e3fc-dd6d-4154-82e9-3018e544aab9', true, NOW(), NOW()),
('c5000000-0000-0000-0000-000000000500', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'CC-500', 'Corporate Facilities & Procurement', 'b3333333-3333-3333-3333-333333333333', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = NOW();

-- 7. Annual Budget Pools (2026)
INSERT INTO budget_pools (id, tenant_id, legal_entity_id, cost_center_id, fiscal_year, period_type, period_value, status, enforcement_mode, tolerance_percentage, allocated_amount, spent_amount, reserved_amount, currency, created_at, updated_at)
VALUES
('a1000000-0000-0000-0000-000000000100', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c1000000-0000-0000-0000-000000000100', 2026, 'ANNUAL', 'ANNUAL', 'ACTIVE', 'HARD_STOP', 0.00, 5000000.0000, 450000.0000, 250000.0000, 'TRY', NOW(), NOW()),
('a2000000-0000-0000-0000-000000000200', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c2000000-0000-0000-0000-000000000200', 2026, 'ANNUAL', 'ANNUAL', 'ACTIVE', 'HARD_STOP', 0.00, 12000000.0000, 3200000.0000, 1800000.0000, 'TRY', NOW(), NOW()),
('a3000000-0000-0000-0000-000000000300', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c3000000-0000-0000-0000-000000000300', 2026, 'ANNUAL', 'ANNUAL', 'ACTIVE', 'HARD_STOP', 0.00, 8000000.0000, 4100000.0000, 1200000.0000, 'TRY', NOW(), NOW()),
('a4000000-0000-0000-0000-000000000400', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c4000000-0000-0000-0000-000000000400', 2026, 'ANNUAL', 'ANNUAL', 'ACTIVE', 'HARD_STOP', 0.00, 3500000.0000, 1900000.0000, 400000.0000, 'TRY', NOW(), NOW()),
('a5000000-0000-0000-0000-000000000500', '79ef8bff-1d87-4088-ab87-935989a568d5', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c5000000-0000-0000-0000-000000000500', 2026, 'ANNUAL', 'ANNUAL', 'ACTIVE', 'HARD_STOP', 0.00, 4000000.0000, 850000.0000, 300000.0000, 'TRY', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET allocated_amount = EXCLUDED.allocated_amount, updated_at = NOW();

-- 8. Approval Authority Limits (DoA Matrix)
INSERT INTO approval_authority_limits (id, tenant_id, user_id, legal_entity_id, cost_center_id, approval_level, min_amount, max_amount, currency, is_active, created_at, updated_at)
VALUES
('e1111111-1111-1111-1111-111111111111', '79ef8bff-1d87-4088-ab87-935989a568d5', 'b2222222-2222-2222-2222-222222222222', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c3000000-0000-0000-0000-000000000300', 1, 0.0000, 50000.0000, 'TRY', true, NOW(), NOW()),
('e2222222-2222-2222-2222-222222222222', '79ef8bff-1d87-4088-ab87-935989a568d5', 'b1111111-1111-1111-1111-111111111111', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c2000000-0000-0000-0000-000000000200', 2, 0.0000, 75000.0000, 'TRY', true, NOW(), NOW()),
('e3333333-3333-3333-3333-333333333333', '79ef8bff-1d87-4088-ab87-935989a568d5', 'b3333333-3333-3333-3333-333333333333', '09e3a0db-e890-4b01-b966-98026fb26fc7', NULL, 2, 0.0000, 250000.0000, 'TRY', true, NOW(), NOW()),
('e4444444-4444-4444-4444-444444444444', '79ef8bff-1d87-4088-ab87-935989a568d5', 'a872e3fc-dd6d-4154-82e9-3018e544aab9', '09e3a0db-e890-4b01-b966-98026fb26fc7', NULL, 99, 0.0000, NULL, 'TRY', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET max_amount = EXCLUDED.max_amount, updated_at = NOW();

-- 9. Purchase Requisitions & Line Items & Steps
-- PR 1: Cloud Expansion (Pending CFO Approval)
INSERT INTO purchase_requisitions (id, tenant_id, requisition_number, requisitioner_id, legal_entity_id, cost_center_id, delivery_facility_id, budget_pool_id, status, total_amount, currency, title, justification, created_at, updated_at)
VALUES
('d1111111-1111-1111-1111-111111111111', '79ef8bff-1d87-4088-ab87-935989a568d5', 'PR-20260819-0001', 'b2222222-2222-2222-2222-222222222222', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c3000000-0000-0000-0000-000000000300', 'f2222222-2222-2222-2222-222222222222', 'a3000000-0000-0000-0000-000000000300', 'PENDING_APPROVAL', 45000.0000, 'TRY', 'Q3 AWS & Kubernetes Cloud Infrastructure Capacity Expansion', 'Production cluster traffic increased by 35% following enterprise customer onboarding. Node group scaling required.', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours');

INSERT INTO requisition_line_items (id, requisition_id, tenant_id, line_number, item_description, item_category, quantity, unit_of_measure, unit_price, total_price, created_at, updated_at)
VALUES
('01111111-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', '79ef8bff-1d87-4088-ab87-935989a568d5', 1, 'AWS EKS Managed Node Instances (m6i.4xlarge x4)', 'SOFTWARE_LICENSE', 4.00, 'HOUR', 7500.0000, 30000.0000, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
('01111111-1111-1111-1111-111111111112', 'd1111111-1111-1111-1111-111111111111', '79ef8bff-1d87-4088-ab87-935989a568d5', 2, 'Amazon EBS gp3 High-IOPS Provisioned Storage 10TB', 'SOFTWARE_LICENSE', 1.00, 'SET', 15000.0000, 15000.0000, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours');

INSERT INTO requisition_approval_steps (id, requisition_id, tenant_id, step_order, approver_id, approval_level, status, created_at, updated_at)
VALUES
('11111111-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', '79ef8bff-1d87-4088-ab87-935989a568d5', 1, 'a872e3fc-dd6d-4154-82e9-3018e544aab9', 99, 'PENDING', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours');

-- PR 2: Workstations Upgrade (Pending Step 2 CFO)
INSERT INTO purchase_requisitions (id, tenant_id, requisition_number, requisitioner_id, legal_entity_id, cost_center_id, delivery_facility_id, budget_pool_id, status, total_amount, currency, title, justification, created_at, updated_at)
VALUES
('d2222222-2222-2222-2222-222222222222', '79ef8bff-1d87-4088-ab87-935989a568d5', 'PR-20260819-0002', 'b5555555-5555-5555-5555-555555555555', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c2000000-0000-0000-0000-000000000200', 'f1111111-1111-1111-1111-111111111111', 'a2000000-0000-0000-0000-000000000200', 'PENDING_APPROVAL', 128000.0000, 'TRY', 'Engineering Team M3 Max High-Performance Workstation Upgrades', 'Local AI LLM quantization and Docker containerization benchmarks require 64GB RAM machines for frontend & backend teams.', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours');

INSERT INTO requisition_line_items (id, requisition_id, tenant_id, line_number, item_description, item_category, quantity, unit_of_measure, unit_price, total_price, created_at, updated_at)
VALUES
('02222222-2222-2222-2222-222222222221', 'd2222222-2222-2222-2222-222222222222', '79ef8bff-1d87-4088-ab87-935989a568d5', 1, 'Apple MacBook Pro 16" M3 Max 64GB RAM 1TB SSD Space Black', 'IT_HARDWARE', 2.00, 'PIECE', 64000.0000, 128000.0000, NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours');

INSERT INTO requisition_approval_steps (id, requisition_id, tenant_id, step_order, approver_id, approval_level, status, decision_note, decided_at, created_at, updated_at)
VALUES
('12222222-2222-2222-2222-222222222221', 'd2222222-2222-2222-2222-222222222222', '79ef8bff-1d87-4088-ab87-935989a568d5', 1, 'b1111111-1111-1111-1111-111111111111', 2, 'APPROVED', 'Approved for core engineering high-priority deliverables.', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '3 hours'),
('12222222-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', '79ef8bff-1d87-4088-ab87-935989a568d5', 2, 'a872e3fc-dd6d-4154-82e9-3018e544aab9', 99, 'PENDING', NULL, NULL, NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours');

-- PR 3: Annual Monitoring Tool (Approved)
INSERT INTO purchase_requisitions (id, tenant_id, requisition_number, requisitioner_id, legal_entity_id, cost_center_id, delivery_facility_id, budget_pool_id, status, total_amount, currency, title, justification, approved_at, created_at, updated_at)
VALUES
('d3333333-3333-3333-3333-333333333333', '79ef8bff-1d87-4088-ab87-935989a568d5', 'PR-20260819-0003', 'b2222222-2222-2222-2222-222222222222', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c3000000-0000-0000-0000-000000000300', 'f1111111-1111-1111-1111-111111111111', 'a3000000-0000-0000-0000-000000000300', 'APPROVED', 92000.0000, 'TRY', 'Datadog Enterprise APM & SIEM Security Monitoring Renewal', 'Mandatory enterprise compliance monitoring for ISO 27001 audit readiness.', NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day');

INSERT INTO requisition_line_items (id, requisition_id, tenant_id, line_number, item_description, item_category, quantity, unit_of_measure, unit_price, total_price, created_at, updated_at)
VALUES
('03333333-3333-3333-3333-333333333331', 'd3333333-3333-3333-3333-333333333333', '79ef8bff-1d87-4088-ab87-935989a568d5', 1, 'Datadog APM Pro Host Annual Enterprise License', 'SOFTWARE_LICENSE', 1.00, 'SET', 92000.0000, 92000.0000, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

INSERT INTO requisition_approval_steps (id, requisition_id, tenant_id, step_order, approver_id, approval_level, status, decision_note, decided_at, created_at, updated_at)
VALUES
('13333333-3333-3333-3333-333333333331', 'd3333333-3333-3333-3333-333333333333', '79ef8bff-1d87-4088-ab87-935989a568d5', 1, 'a872e3fc-dd6d-4154-82e9-3018e544aab9', 99, 'APPROVED', 'Approved as per annual IT compliance budget plan.', NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day');

-- PR 4: Video Conferencing (Rejected)
INSERT INTO purchase_requisitions (id, tenant_id, requisition_number, requisitioner_id, legal_entity_id, cost_center_id, delivery_facility_id, budget_pool_id, status, total_amount, currency, title, justification, rejection_reason, created_at, updated_at)
VALUES
('d4444444-4444-4444-4444-444444444444', '79ef8bff-1d87-4088-ab87-935989a568d5', 'PR-20260819-0004', 'b3333333-3333-3333-3333-333333333333', '09e3a0db-e890-4b01-b966-98026fb26fc7', 'c5000000-0000-0000-0000-000000000500', 'f1111111-1111-1111-1111-111111111111', 'a5000000-0000-0000-0000-000000000500', 'REJECTED', 15000.0000, 'TRY', 'Executive Boardroom Video Conferencing Hardware Setup', 'Replacement of older 1080p hardware with 4K PTZ camera system.', 'Alternative vendor identified providing identical hardware at 40% lower cost.', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days');

INSERT INTO requisition_line_items (id, requisition_id, tenant_id, line_number, item_description, item_category, quantity, unit_of_measure, unit_price, total_price, created_at, updated_at)
VALUES
('04444444-4444-4444-4444-444444444441', 'd4444444-4444-4444-4444-444444444444', '79ef8bff-1d87-4088-ab87-935989a568d5', 1, 'Logitech Rally Plus 4K ConferenceCam Bar', 'OFFICE_SUPPLIES', 1.00, 'SET', 15000.0000, 15000.0000, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days');

INSERT INTO requisition_approval_steps (id, requisition_id, tenant_id, step_order, approver_id, approval_level, status, decision_note, decided_at, created_at, updated_at)
VALUES
('14444444-4444-4444-4444-444444444441', 'd4444444-4444-4444-4444-444444444444', '79ef8bff-1d87-4088-ab87-935989a568d5', 1, 'a872e3fc-dd6d-4154-82e9-3018e544aab9', 99, 'REJECTED', 'Alternative vendor identified providing identical hardware at 40% lower cost.', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days');
