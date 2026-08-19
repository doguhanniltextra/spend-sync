-- 1. PO 1 Line Items
INSERT INTO purchase_order_line_items (id, purchase_order_id, tenant_id, line_number, item_description, item_category, quantity, unit_of_measure, unit_price, total_price, over_delivery_tolerance_pct, under_delivery_tolerance_pct, created_at, updated_at)
VALUES
('b1000000-0000-0000-0000-000000000101', 'b1000000-0000-0000-0000-000000000100', '79ef8bff-1d87-4088-ab87-935989a568d5', 1, 'AWS EKS Managed Node Instances (m6i.4xlarge x4)', 'SOFTWARE_SAAS', 4.00, 'HOUR', 7500.0000, 30000.0000, 0.00, 0.00, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
('b1000000-0000-0000-0000-000000000102', 'b1000000-0000-0000-0000-000000000100', '79ef8bff-1d87-4088-ab87-935989a568d5', 2, 'Amazon EBS gp3 High-IOPS Provisioned Storage 10TB', 'SOFTWARE_SAAS', 1.00, 'SET', 15000.0000, 15000.0000, 0.00, 0.00, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours')
ON CONFLICT (id) DO NOTHING;

-- 2. PO 2 Line Items
INSERT INTO purchase_order_line_items (id, purchase_order_id, tenant_id, line_number, item_description, item_category, quantity, unit_of_measure, unit_price, total_price, over_delivery_tolerance_pct, under_delivery_tolerance_pct, created_at, updated_at)
VALUES
('b2000000-0000-0000-0000-000000000201', 'b2000000-0000-0000-0000-000000000200', '79ef8bff-1d87-4088-ab87-935989a568d5', 1, 'Apple MacBook Pro 16" M3 Max 64GB RAM 1TB SSD Space Black', 'IT_HARDWARE', 2.00, 'PIECE', 64000.0000, 128000.0000, 0.00, 0.00, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours')
ON CONFLICT (id) DO NOTHING;
