-- Development/demo data only. Run through tools/seed-demo.ps1.
-- Stable base demo IDs are 1001-1099. Expanded fixtures use reserved ranges
-- 200001+ (customers/products), 300001+ (inventory), and 400001+ (sales).

SET FOREIGN_KEY_CHECKS = 0;

DROP TEMPORARY TABLE IF EXISTS demo_seed_digits_hundreds;
DROP TEMPORARY TABLE IF EXISTS demo_seed_digits_tens;
DROP TEMPORARY TABLE IF EXISTS demo_seed_digits_ones;
CREATE TEMPORARY TABLE demo_seed_digits_hundreds (n INT NOT NULL PRIMARY KEY);
CREATE TEMPORARY TABLE demo_seed_digits_tens (n INT NOT NULL PRIMARY KEY);
CREATE TEMPORARY TABLE demo_seed_digits_ones (n INT NOT NULL PRIMARY KEY);
INSERT INTO demo_seed_digits_hundreds (n)
VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);
INSERT INTO demo_seed_digits_tens (n)
VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);
INSERT INTO demo_seed_digits_ones (n)
VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

DROP TEMPORARY TABLE IF EXISTS demo_seed_numbers;
CREATE TEMPORARY TABLE demo_seed_numbers (n INT NOT NULL PRIMARY KEY);
INSERT INTO demo_seed_numbers (n)
SELECT hundreds.n * 100 + tens.n * 10 + ones.n
FROM demo_seed_digits_hundreds hundreds
CROSS JOIN demo_seed_digits_tens tens
CROSS JOIN demo_seed_digits_ones ones;

START TRANSACTION;

USE userdb;

INSERT INTO users (
    id, created_at, email, full_name, password, phone, role, updated_at, verified
) VALUES
    (1001, NOW(6), 'store.admin@seed.local', 'Demo Store Admin',
     '$2b$10$GVPUMD0qspt9bBCoNelxP.xyOK/fhLIPqf3zRMiayMALYVr66YgKu',
     '+910000000001', 1, NOW(6), 1),
    (1002, NOW(6), 'branch.manager@seed.local', 'Demo Branch Manager',
     '$2b$10$GVPUMD0qspt9bBCoNelxP.xyOK/fhLIPqf3zRMiayMALYVr66YgKu',
     '+910000000002', 3, NOW(6), 1),
    (1003, NOW(6), 'cashier@seed.local', 'Demo Cashier',
     '$2b$10$GVPUMD0qspt9bBCoNelxP.xyOK/fhLIPqf3zRMiayMALYVr66YgKu',
     '+910000000003', 4, NOW(6), 1),
    (1004, NOW(6), 'cashier.two@seed.local', 'Demo Evening Cashier',
     '$2b$10$GVPUMD0qspt9bBCoNelxP.xyOK/fhLIPqf3zRMiayMALYVr66YgKu',
     '+910000000004', 4, NOW(6), 1),
    (1005, NOW(6), 'store.manager@seed.local', 'Demo Store Manager',
     '$2b$10$GVPUMD0qspt9bBCoNelxP.xyOK/fhLIPqf3zRMiayMALYVr66YgKu',
     '+910000000005', 2, NOW(6), 1),
    (1006, NOW(6), 'customer@seed.local', 'Demo Account Customer',
     '$2b$10$GVPUMD0qspt9bBCoNelxP.xyOK/fhLIPqf3zRMiayMALYVr66YgKu',
     '+910000000006', 6, NOW(6), 1)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    phone = VALUES(phone),
    role = VALUES(role),
    password = VALUES(password),
    verified = 1;

INSERT INTO stores (
    id, brand, address, email, phone, created_at, description, status,
    store_type, updated_at, store_admin_id
) VALUES
(
    1001, 'Demo Retail', '1 Demo Market Street', 'store.admin@seed.local',
    '+910000000001', NOW(6), 'Development demonstration store', 'ACTIVE',
    'RETAIL', NOW(6), 1001
),
(
    1002, 'Demo Express', '22 Demo Station Road', 'store.manager@seed.local',
    '+910000000005', NOW(6), 'Second development demonstration store', 'ACTIVE',
    'CONVENIENCE', NOW(6), 1005
)
ON DUPLICATE KEY UPDATE
    brand = VALUES(brand),
    store_admin_id = VALUES(store_admin_id),
    status = VALUES(status);

INSERT INTO branches (
    id, address, close_time, created_at, email, name, open_time, phone,
    updated_at, manager_id, store_id, max_discount_percent
) VALUES (
    1001, '1 Demo Market Street', '22:00:00', NOW(6),
    'branch.manager@seed.local', 'Demo Main Branch', '08:00:00',
    '+910000000010', NOW(6), 1002, 1001, 15.00
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    manager_id = VALUES(manager_id),
    store_id = VALUES(store_id);

INSERT INTO branches (
    id, address, close_time, created_at, email, name, open_time, phone,
    updated_at, manager_id, store_id, max_discount_percent
) VALUES
(
    1002, '18 Demo Market Street', '21:00:00', NOW(6),
    'branch.north@seed.local', 'Demo North Branch', '08:30:00',
    '+910000000011', NOW(6), NULL, 1001, 10.00
),
(
    1003, '35 Demo Market Street', '21:00:00', NOW(6),
    'branch.south@seed.local', 'Demo South Branch', '09:00:00',
    '+910000000012', NOW(6), NULL, 1001, 10.00
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    store_id = VALUES(store_id),
    max_discount_percent = VALUES(max_discount_percent);

DELETE FROM branch_working_days WHERE branch_id = 1001;
INSERT INTO branch_working_days (branch_id, working_days)
VALUES (1001, 'MONDAY'), (1001, 'TUESDAY'), (1001, 'WEDNESDAY'),
       (1001, 'THURSDAY'), (1001, 'FRIDAY'), (1001, 'SATURDAY');

DELETE FROM branch_working_days WHERE branch_id IN (1002, 1003);
INSERT INTO branch_working_days (branch_id, working_days)
VALUES
    (1002, 'MONDAY'), (1002, 'TUESDAY'), (1002, 'WEDNESDAY'),
    (1002, 'THURSDAY'), (1002, 'FRIDAY'), (1002, 'SATURDAY'),
    (1003, 'MONDAY'), (1003, 'TUESDAY'), (1003, 'WEDNESDAY'),
    (1003, 'THURSDAY'), (1003, 'FRIDAY'), (1003, 'SATURDAY');

UPDATE users
SET store_id = 1001,
    branch_id = CASE WHEN id IN (1002, 1003, 1004) THEN 1001 ELSE branch_id END
WHERE id IN (1001, 1002, 1003, 1004, 1005, 1006);

UPDATE users
SET store_id = 1002, branch_id = NULL
WHERE id = 1005;

USE inventorydb;

INSERT INTO categories (id, name, store_id)
VALUES
    (1001, 'Beverages', 1001),
    (1002, 'Snacks', 1001),
    (1003, 'Dairy', 1001),
    (1004, 'Bakery', 1001),
    (1005, 'Household', 1001)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    store_id = VALUES(store_id);

INSERT INTO products (
    id, brand, created_at, description, image, mrp, name, selling_price,
    sku, store_id, updated_at, category_id
) VALUES
    (1001, 'Demo Brand', NOW(6), 'Roasted ground coffee', 'https://picsum.photos/seed/demo-coffee/640/480', 120.00, 'Demo Coffee', 99.00, 'DEMO-COFFEE-001', 1001, NOW(6), 1001),
    (1002, 'Demo Brand', NOW(6), 'Demo snack product', 'https://picsum.photos/seed/demo-snack/640/480', 60.00, 'Demo Snack', 49.00, 'DEMO-SNACK-001', 1001, NOW(6), 1002),
    (1003, 'Demo Brand', NOW(6), 'Demo household product', 'https://picsum.photos/seed/demo-household/640/480', 250.00, 'Demo Cleaner', 199.00, 'DEMO-CLEANER-001', 1001, NOW(6), 1005),
    (1004, 'Demo Brand', NOW(6), 'Salted potato chips', 'https://picsum.photos/seed/demo-chips/640/480', 60.00, 'Potato Chips', 49.00, 'DEMO-CHIPS-001', 1001, NOW(6), 1002),
    (1005, 'Demo Brand', NOW(6), 'Roasted mixed nuts', 'https://picsum.photos/seed/demo-nuts/640/480', 180.00, 'Mixed Nuts', 149.00, 'DEMO-NUTS-001', 1001, NOW(6), 1002),
    (1006, 'Demo Brand', NOW(6), 'Classic milk pack', 'https://picsum.photos/seed/demo-milk/640/480', 70.00, 'Fresh Milk', 62.00, 'DEMO-MILK-001', 1001, NOW(6), 1003),
    (1007, 'Demo Brand', NOW(6), 'Plain yogurt cup', 'https://picsum.photos/seed/demo-yogurt/640/480', 55.00, 'Plain Yogurt', 45.00, 'DEMO-YOGURT-001', 1001, NOW(6), 1003),
    (1008, 'Demo Brand', NOW(6), 'Whole wheat bread loaf', 'https://picsum.photos/seed/demo-bread/640/480', 55.00, 'Wheat Bread', 45.00, 'DEMO-BREAD-001', 1001, NOW(6), 1004),
    (1009, 'Demo Brand', NOW(6), 'Butter croissant', 'https://picsum.photos/seed/demo-croissant/640/480', 75.00, 'Croissant', 60.00, 'DEMO-CROISSANT-001', 1001, NOW(6), 1004),
    (1010, 'Demo Brand', NOW(6), 'Chocolate cookies', 'https://picsum.photos/seed/demo-cookies/640/480', 90.00, 'Chocolate Cookies', 75.00, 'DEMO-COOKIES-001', 1001, NOW(6), 1002),
    (1011, 'Demo Brand', NOW(6), 'Dishwashing liquid', 'https://picsum.photos/seed/demo-dishwash/640/480', 160.00, 'Dishwash Liquid', 135.00, 'DEMO-DISHWASH-001', 1001, NOW(6), 1005),
    (1012, 'Demo Brand', NOW(6), 'Multi-surface cleaner', 'https://picsum.photos/seed/demo-surface-cleaner/640/480', 250.00, 'Surface Cleaner', 199.00, 'DEMO-SURFACE-CLEANER-001', 1001, NOW(6), 1005),
    (1013, 'Demo Brand', NOW(6), 'Paper towel roll', 'https://picsum.photos/seed/demo-towel/640/480', 110.00, 'Paper Towels', 89.00, 'DEMO-TOWEL-001', 1001, NOW(6), 1005),
    (1014, 'Demo Brand', NOW(6), 'Hand soap refill', 'https://picsum.photos/seed/demo-soap/640/480', 95.00, 'Hand Soap', 79.00, 'DEMO-SOAP-001', 1001, NOW(6), 1005),
    (1015, 'Demo Brand', NOW(6), 'Green tea bags', 'https://picsum.photos/seed/demo-tea/640/480', 140.00, 'Green Tea', 115.00, 'DEMO-TEA-001', 1001, NOW(6), 1001)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    selling_price = VALUES(selling_price),
    category_id = VALUES(category_id),
    store_id = VALUES(store_id);

INSERT INTO inventories (id, branch_id, last_updated, quantity, product_id)
VALUES
    (1001, 1001, NOW(6), 120, 1001),
    (1002, 1001, NOW(6), 80, 1002),
    (1003, 1001, NOW(6), 8, 1003),
    (1004, 1001, NOW(6), 65, 1004),
    (1005, 1001, NOW(6), 6, 1005),
    (1006, 1001, NOW(6), 55, 1006),
    (1007, 1001, NOW(6), 9, 1007),
    (1008, 1001, NOW(6), 42, 1008),
    (1009, 1001, NOW(6), 18, 1009),
    (1010, 1001, NOW(6), 34, 1010),
    (1011, 1001, NOW(6), 27, 1011),
    (1012, 1001, NOW(6), 5, 1012),
    (1013, 1001, NOW(6), 24, 1013),
    (1014, 1001, NOW(6), 16, 1014),
    (1015, 1001, NOW(6), 11, 1015),
    (1016, 1002, NOW(6), 70, 1001),
    (1017, 1002, NOW(6), 48, 1004),
    (1018, 1002, NOW(6), 7, 1005),
    (1019, 1002, NOW(6), 35, 1006),
    (1020, 1002, NOW(6), 22, 1008),
    (1021, 1002, NOW(6), 15, 1010),
    (1022, 1002, NOW(6), 4, 1012),
    (1023, 1002, NOW(6), 20, 1013),
    (1024, 1002, NOW(6), 13, 1014),
    (1025, 1002, NOW(6), 9, 1015),
    (1026, 1003, NOW(6), 60, 1001),
    (1027, 1003, NOW(6), 42, 1004),
    (1028, 1003, NOW(6), 5, 1005),
    (1029, 1003, NOW(6), 30, 1006),
    (1030, 1003, NOW(6), 18, 1008),
    (1031, 1003, NOW(6), 12, 1010),
    (1032, 1003, NOW(6), 3, 1012),
    (1033, 1003, NOW(6), 18, 1013),
    (1034, 1003, NOW(6), 10, 1014),
    (1035, 1003, NOW(6), 8, 1015)
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    last_updated = VALUES(last_updated);

INSERT INTO stock_operations (
    operation_key, branch_id, product_id, quantity, operation_type, status, created_at
) VALUES
    ('demo-restock-1001-001', 1001, 1001, 20, 'RESTORE', 'APPLIED', DATE_SUB(NOW(6), INTERVAL 50 DAY)),
    ('demo-restock-1001-002', 1001, 1004, 15, 'RESTORE', 'APPLIED', DATE_SUB(NOW(6), INTERVAL 43 DAY)),
    ('demo-reserve-1001-003', 1001, 1005, 2, 'RESERVE', 'APPLIED', DATE_SUB(NOW(6), INTERVAL 35 DAY)),
    ('demo-restock-1002-004', 1002, 1001, 25, 'RESTORE', 'APPLIED', DATE_SUB(NOW(6), INTERVAL 30 DAY)),
    ('demo-restock-1002-005', 1002, 1012, 8, 'RESTORE', 'APPLIED', DATE_SUB(NOW(6), INTERVAL 24 DAY)),
    ('demo-reserve-1002-006', 1002, 1004, 3, 'RESERVE', 'APPLIED', DATE_SUB(NOW(6), INTERVAL 18 DAY)),
    ('demo-restock-1003-007', 1003, 1006, 18, 'RESTORE', 'APPLIED', DATE_SUB(NOW(6), INTERVAL 16 DAY)),
    ('demo-restock-1003-008', 1003, 1010, 12, 'RESTORE', 'APPLIED', DATE_SUB(NOW(6), INTERVAL 10 DAY)),
    ('demo-reserve-1003-009', 1003, 1012, 1, 'RESERVE', 'APPLIED', DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    ('demo-restock-1003-010', 1003, 1015, 6, 'RESTORE', 'APPLIED', DATE_SUB(NOW(6), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE
    branch_id = VALUES(branch_id),
    product_id = VALUES(product_id),
    quantity = VALUES(quantity),
    operation_type = VALUES(operation_type),
    status = VALUES(status);

INSERT INTO products_seq (next_val)
SELECT COALESCE(MAX(id), 0) + 1 FROM products
WHERE NOT EXISTS (SELECT 1 FROM products_seq);
UPDATE products_seq SET next_val = GREATEST(next_val, 1016);

INSERT INTO inventories_seq (next_val)
SELECT COALESCE(MAX(id), 0) + 1 FROM inventories
WHERE NOT EXISTS (SELECT 1 FROM inventories_seq);
UPDATE inventories_seq SET next_val = GREATEST(next_val, 1016);

USE salesdb;

INSERT INTO customer (
    id, branch_id, created_at, email, full_name, loyalty_points, phone,
    store_id, updated_at
) VALUES
    (1001, 1001, NOW(6), 'customer.one@seed.local', 'Demo Customer One', 120, '+910000001001', 1001, NOW(6)),
    (1002, 1001, NOW(6), 'customer.two@seed.local', 'Demo Customer Two', 60, '+910000001002', 1001, NOW(6)),
    (1003, 1001, NOW(6), 'customer.three@seed.local', 'Demo Customer Three', 85, '+910000001003', 1001, NOW(6)),
    (1004, 1001, NOW(6), 'customer.four@seed.local', 'Demo Customer Four', 40, '+910000001004', 1001, NOW(6)),
    (1005, 1001, NOW(6), 'customer.five@seed.local', 'Demo Customer Five', 175, '+910000001005', 1001, NOW(6)),
    (1006, 1001, NOW(6), 'customer.six@seed.local', 'Demo Customer Six', 25, '+910000001006', 1001, NOW(6)),
    (1007, 1001, NOW(6), 'customer.seven@seed.local', 'Demo Customer Seven', 95, '+910000001007', 1001, NOW(6)),
    (1008, 1001, NOW(6), 'customer.eight@seed.local', 'Demo Customer Eight', 55, '+910000001008', 1001, NOW(6)),
    (1009, 1001, NOW(6), 'customer.nine@seed.local', 'Demo Customer Nine', 210, '+910000001009', 1001, NOW(6)),
    (1010, 1001, NOW(6), 'customer.ten@seed.local', 'Demo Customer Ten', 70, '+910000001010', 1001, NOW(6)),
    (1011, 1001, NOW(6), 'customer.eleven@seed.local', 'Demo Customer Eleven', 35, '+910000001011', 1001, NOW(6)),
    (1012, 1001, NOW(6), 'customer.twelve@seed.local', 'Demo Customer Twelve', 130, '+910000001012', 1001, NOW(6)),
    (1013, 1002, NOW(6), 'customer.thirteen@seed.local', 'Demo Customer Thirteen', 45, '+910000001013', 1001, NOW(6)),
    (1014, 1003, NOW(6), 'customer.fourteen@seed.local', 'Demo Customer Fourteen', 90, '+910000001014', 1001, NOW(6))
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    loyalty_points = VALUES(loyalty_points),
    branch_id = VALUES(branch_id),
    store_id = VALUES(store_id);

INSERT INTO orders (
    id, average_item_price, branch_id, cashier_id, created_at, day_of_week,
    discount_amount, discount_percent, is_fraud, items_count, order_hour,
    payment_type, risk_score, score, shift, status, total_amount,
    total_quantity, customer_id, fraud_reason, fraud_status, idempotency_key,
    subtotal_amount, tax_rate, tax_amount, payment_settlement_status
) VALUES
    (1001, 99.00, 1001, 1003, DATE_SUB(NOW(6), INTERVAL 56 DAY), 'MONDAY', 0.00, 0.00, 0, 1, 9, 'CASH', 0.02, 0, 'MORNING', 'COMPLETED', 99.00, 1, 1001, NULL, 'NONE', '00000000-0000-4000-8000-000000001001', 99.00, 0.00, 0.00, 'CASH_RECORDED'),
    (1002, 57.00, 1004, 1002, DATE_SUB(NOW(6), INTERVAL 53 DAY), 'THURSDAY', 0.00, 0.00, 0, 2, 13, 'UPI', 0.01, 0, 'AFTERNOON', 'COMPLETED', 114.00, 2, 1002, NULL, 'NONE', '00000000-0000-4000-8000-000000001002', 114.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1003, 49.00, 1004, 1003, DATE_SUB(NOW(6), INTERVAL 49 DAY), 'MONDAY', 0.00, 0.00, 0, 1, 17, 'CARD', 0.03, 0, 'EVENING', 'COMPLETED', 49.00, 1, 1003, NULL, 'NONE', '00000000-0000-4000-8000-000000001003', 49.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1004, 159.00, 1004, 1005, DATE_SUB(NOW(6), INTERVAL 46 DAY), 'THURSDAY', 0.00, 0.00, 0, 2, 10, 'CASH', 0.02, 0, 'MORNING', 'COMPLETED', 159.00, 2, 1004, NULL, 'NONE', '00000000-0000-4000-8000-000000001004', 159.00, 0.00, 0.00, 'CASH_RECORDED'),
    (1005, 62.00, 1003, 1003, DATE_SUB(NOW(6), INTERVAL 42 DAY), 'MONDAY', 0.00, 0.00, 0, 1, 12, 'UPI', 0.01, 0, 'AFTERNOON', 'REFUND_PENDING_REVIEW', 62.00, 1, 1005, NULL, 'NONE', '00000000-0000-4000-8000-000000001005', 62.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1006, 124.00, 1003, 1004, DATE_SUB(NOW(6), INTERVAL 39 DAY), 'THURSDAY', 0.00, 0.00, 0, 2, 15, 'CARD', 0.02, 0, 'AFTERNOON', 'COMPLETED', 248.00, 2, 1006, NULL, 'NONE', '00000000-0000-4000-8000-000000001006', 248.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1007, 149.00, 1001, 1003, DATE_SUB(NOW(6), INTERVAL 35 DAY), 'MONDAY', 0.00, 0.00, 0, 1, 18, 'CASH', 0.02, 0, 'EVENING', 'COMPLETED', 149.00, 1, 1007, NULL, 'NONE', '00000000-0000-4000-8000-000000001007', 149.00, 0.00, 0.00, 'CASH_RECORDED'),
    (1008, 124.00, 1001, 1004, DATE_SUB(NOW(6), INTERVAL 32 DAY), 'THURSDAY', 0.00, 0.00, 0, 2, 11, 'UPI', 0.01, 0, 'MORNING', 'COMPLETED', 124.00, 2, 1008, NULL, 'NONE', '00000000-0000-4000-8000-000000001008', 124.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1009, 75.00, 1001, 1003, DATE_SUB(NOW(6), INTERVAL 28 DAY), 'MONDAY', 0.00, 0.00, 0, 1, 14, 'CARD', 0.01, 0, 'AFTERNOON', 'COMPLETED', 75.00, 1, 1009, NULL, 'NONE', '00000000-0000-4000-8000-000000001009', 75.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1010, 214.00, 1001, 1004, DATE_SUB(NOW(6), INTERVAL 25 DAY), 'THURSDAY', 0.00, 0.00, 0, 2, 16, 'CASH', 0.02, 0, 'EVENING', 'COMPLETED', 214.00, 2, 1010, NULL, 'NONE', '00000000-0000-4000-8000-000000001010', 214.00, 0.00, 0.00, 'CASH_RECORDED'),
    (1011, 45.00, 1001, 1003, DATE_SUB(NOW(6), INTERVAL 21 DAY), 'MONDAY', 0.00, 0.00, 0, 1, 10, 'UPI', 0.01, 0, 'MORNING', 'COMPLETED', 45.00, 1, 1011, NULL, 'NONE', '00000000-0000-4000-8000-000000001011', 45.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1012, 298.00, 1001, 1004, DATE_SUB(NOW(6), INTERVAL 18 DAY), 'THURSDAY', 0.00, 0.00, 0, 2, 13, 'CARD', 0.02, 0, 'AFTERNOON', 'COMPLETED', 298.00, 2, 1012, NULL, 'NONE', '00000000-0000-4000-8000-000000001012', 298.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1013, 199.00, 1001, 1003, DATE_SUB(NOW(6), INTERVAL 14 DAY), 'MONDAY', 0.00, 0.00, 0, 1, 17, 'CASH', 0.01, 0, 'EVENING', 'REFUND_PENDING_REVIEW', 199.00, 1, 1001, NULL, 'NONE', '00000000-0000-4000-8000-000000001013', 199.00, 0.00, 0.00, 'CASH_RECORDED'),
    (1014, 105.00, 1001, 1004, DATE_SUB(NOW(6), INTERVAL 11 DAY), 'THURSDAY', 0.00, 0.00, 0, 2, 9, 'UPI', 0.02, 0, 'MORNING', 'COMPLETED', 105.00, 2, 1002, NULL, 'NONE', '00000000-0000-4000-8000-000000001014', 105.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1015, 115.00, 1001, 1003, DATE_SUB(NOW(6), INTERVAL 9 DAY), 'SATURDAY', 0.00, 0.00, 0, 1, 12, 'CARD', 0.02, 0, 'AFTERNOON', 'COMPLETED', 115.00, 1, 1003, NULL, 'NONE', '00000000-0000-4000-8000-000000001015', 115.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1016, 89.00, 1001, 1004, DATE_SUB(NOW(6), INTERVAL 7 DAY), 'MONDAY', 0.00, 0.00, 0, 2, 15, 'CASH', 0.01, 0, 'AFTERNOON', 'COMPLETED', 178.00, 2, 1004, NULL, 'NONE', '00000000-0000-4000-8000-000000001016', 178.00, 0.00, 0.00, 'CASH_RECORDED'),
    (1017, 79.00, 1001, 1003, DATE_SUB(NOW(6), INTERVAL 5 DAY), 'WEDNESDAY', 0.01, 0.00, 0, 1, 18, 'UPI', 0.01, 0, 'EVENING', 'COMPLETED', 79.00, 1, 1005, NULL, 'NONE', '00000000-0000-4000-8000-000000001017', 79.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1018, 135.00, 1001, 1004, DATE_SUB(NOW(6), INTERVAL 4 DAY), 'THURSDAY', 0.01, 0.00, 0, 2, 11, 'CARD', 0.02, 0, 'MORNING', 'COMPLETED', 135.00, 2, 1006, NULL, 'NONE', '00000000-0000-4000-8000-000000001018', 135.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1019, 115.00, 1001, 1003, DATE_SUB(NOW(6), INTERVAL 3 DAY), 'FRIDAY', 0.00, 0.00, 0, 1, 13, 'CASH', 0.01, 0, 'AFTERNOON', 'COMPLETED', 115.00, 1, 1007, NULL, 'NONE', '00000000-0000-4000-8000-000000001019', 115.00, 0.00, 0.00, 'CASH_RECORDED'),
    (1020, 238.00, 1001, 1004, DATE_SUB(NOW(6), INTERVAL 2 DAY), 'SATURDAY', 0.01, 0.00, 0, 2, 16, 'UPI', 0.02, 0, 'EVENING', 'COMPLETED', 238.00, 2, 1008, NULL, 'NONE', '00000000-0000-4000-8000-000000001020', 238.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1021, 135.00, 1001, 1003, DATE_SUB(NOW(6), INTERVAL 1 DAY), 'SUNDAY', 0.00, 0.00, 0, 1, 10, 'CARD', 0.01, 0, 'MORNING', 'REFUND_PENDING_REVIEW', 135.00, 1, 1009, NULL, 'NONE', '00000000-0000-4000-8000-000000001021', 135.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1022, 71.50, 1001, 1004, NOW(6), 'SUNDAY', 0.00, 0.00, 0, 2, 12, 'CASH', 0.01, 0, 'AFTERNOON', 'COMPLETED', 143.00, 3, 1010, NULL, 'NONE', '00000000-0000-4000-8000-000000001022', 143.00, 0.00, 0.00, 'CASH_RECORDED'),
    (1023, 199.00, 1001, 1003, DATE_SUB(NOW(6), INTERVAL 1 DAY), 'SATURDAY', 0.00, 0.00, 0, 1, 15, 'UPI', 0.02, 0, 'AFTERNOON', 'COMPLETED', 199.00, 1, 1011, NULL, 'NONE', '00000000-0000-4000-8000-000000001023', 199.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1024, 59.00, 1001, 1004, NOW(6), 'SUNDAY', 0.00, 0.00, 0, 2, 17, 'CARD', 0.02, 0, 'EVENING', 'COMPLETED', 177.00, 3, 1012, NULL, 'NONE', '00000000-0000-4000-8000-000000001024', 177.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1025, 99.00, 1002, 1004, DATE_SUB(NOW(6), INTERVAL 12 DAY), 'WEDNESDAY', 0.00, 0.00, 0, 1, 11, 'CASH', 0.01, 0, 'MORNING', 'COMPLETED', 99.00, 1, 1013, NULL, 'NONE', '00000000-0000-4000-8000-000000001025', 99.00, 0.00, 0.00, 'CASH_RECORDED'),
    (1026, 98.00, 1002, 1004, DATE_SUB(NOW(6), INTERVAL 9 DAY), 'SATURDAY', 0.00, 0.00, 0, 2, 14, 'UPI', 0.01, 0, 'AFTERNOON', 'COMPLETED', 98.00, 2, 1013, NULL, 'NONE', '00000000-0000-4000-8000-000000001026', 98.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1027, 107.00, 1003, 1003, DATE_SUB(NOW(6), INTERVAL 8 DAY), 'SUNDAY', 0.00, 0.00, 0, 2, 10, 'CARD', 0.02, 0, 'MORNING', 'COMPLETED', 107.00, 2, 1014, NULL, 'NONE', '00000000-0000-4000-8000-000000001027', 107.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1028, 135.00, 1003, 1003, DATE_SUB(NOW(6), INTERVAL 5 DAY), 'WEDNESDAY', 0.00, 0.00, 0, 1, 16, 'CASH', 0.01, 0, 'EVENING', 'REFUND_PENDING_REVIEW', 135.00, 1, 1014, NULL, 'NONE', '00000000-0000-4000-8000-000000001028', 135.00, 0.00, 0.00, 'CASH_RECORDED'),
    (1029, 168.00, 1002, 1004, DATE_SUB(NOW(6), INTERVAL 3 DAY), 'FRIDAY', 0.00, 0.00, 0, 2, 13, 'CARD', 0.01, 0, 'AFTERNOON', 'COMPLETED', 168.00, 2, 1013, NULL, 'NONE', '00000000-0000-4000-8000-000000001029', 168.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED'),
    (1030, 115.00, 1003, 1003, DATE_SUB(NOW(6), INTERVAL 1 DAY), 'SATURDAY', 0.00, 0.00, 0, 1, 12, 'UPI', 0.01, 0, 'AFTERNOON', 'COMPLETED', 115.00, 1, 1014, NULL, 'NONE', '00000000-0000-4000-8000-000000001030', 115.00, 0.00, 0.00, 'EXTERNAL_CONFIRMATION_REQUIRED')
ON DUPLICATE KEY UPDATE
    created_at = VALUES(created_at),
    total_amount = VALUES(total_amount),
    total_quantity = VALUES(total_quantity),
    items_count = VALUES(items_count),
    customer_id = VALUES(customer_id),
    status = VALUES(status),
    payment_settlement_status = VALUES(payment_settlement_status);

INSERT INTO order_items (
    id, price, product_id, product_image, product_name, product_price,
    product_sku, quantity, order_id
) VALUES
    (1001, 99.00, 1001, 'https://picsum.photos/seed/demo-coffee/640/480', 'Demo Coffee', 99.00, 'DEMO-COFFEE-001', 1, 1001),
    (1002, 65.00, 1002, 'https://picsum.photos/seed/demo-orange/640/480', 'Orange Drink', 65.00, 'DEMO-ORANGE-001', 1, 1002),
    (1003, 49.00, 1004, 'https://picsum.photos/seed/demo-chips/640/480', 'Potato Chips', 49.00, 'DEMO-CHIPS-001', 1, 1002),
    (1004, 49.00, 1004, 'https://picsum.photos/seed/demo-chips/640/480', 'Potato Chips', 49.00, 'DEMO-CHIPS-001', 1, 1003),
    (1005, 99.00, 1001, 'https://picsum.photos/seed/demo-coffee/640/480', 'Demo Coffee', 99.00, 'DEMO-COFFEE-001', 1, 1004),
    (1006, 60.00, 1009, 'https://picsum.photos/seed/demo-croissant/640/480', 'Croissant', 60.00, 'DEMO-CROISSANT-001', 1, 1004),
    (1007, 62.00, 1006, 'https://picsum.photos/seed/demo-milk/640/480', 'Fresh Milk', 62.00, 'DEMO-MILK-001', 1, 1005),
    (1008, 199.00, 1012, 'https://picsum.photos/seed/demo-cleaner/640/480', 'Surface Cleaner', 199.00, 'DEMO-CLEANER-001', 1, 1006),
    (1009, 49.00, 1004, 'https://picsum.photos/seed/demo-chips/640/480', 'Potato Chips', 49.00, 'DEMO-CHIPS-001', 1, 1006),
    (1010, 149.00, 1005, 'https://picsum.photos/seed/demo-nuts/640/480', 'Mixed Nuts', 149.00, 'DEMO-NUTS-001', 1, 1007),
    (1011, 79.00, 1014, 'https://picsum.photos/seed/demo-soap/640/480', 'Hand Soap', 79.00, 'DEMO-SOAP-001', 1, 1008),
    (1012, 45.00, 1008, 'https://picsum.photos/seed/demo-bread/640/480', 'Wheat Bread', 45.00, 'DEMO-BREAD-001', 1, 1008),
    (1013, 75.00, 1010, 'https://picsum.photos/seed/demo-cookies/640/480', 'Chocolate Cookies', 75.00, 'DEMO-COOKIES-001', 1, 1009),
    (1014, 135.00, 1011, 'https://picsum.photos/seed/demo-dishwash/640/480', 'Dishwash Liquid', 135.00, 'DEMO-DISHWASH-001', 1, 1010),
    (1015, 79.00, 1014, 'https://picsum.photos/seed/demo-soap/640/480', 'Hand Soap', 79.00, 'DEMO-SOAP-001', 1, 1010),
    (1016, 45.00, 1007, 'https://picsum.photos/seed/demo-yogurt/640/480', 'Plain Yogurt', 45.00, 'DEMO-YOGURT-001', 1, 1011),
    (1017, 199.00, 1012, 'https://picsum.photos/seed/demo-cleaner/640/480', 'Surface Cleaner', 199.00, 'DEMO-CLEANER-001', 1, 1012),
    (1018, 99.00, 1001, 'https://picsum.photos/seed/demo-coffee/640/480', 'Demo Coffee', 99.00, 'DEMO-COFFEE-001', 1, 1012),
    (1019, 199.00, 1012, 'https://picsum.photos/seed/demo-cleaner/640/480', 'Surface Cleaner', 199.00, 'DEMO-CLEANER-001', 1, 1013),
    (1020, 60.00, 1009, 'https://picsum.photos/seed/demo-croissant/640/480', 'Croissant', 60.00, 'DEMO-CROISSANT-001', 1, 1014),
    (1021, 45.00, 1008, 'https://picsum.photos/seed/demo-bread/640/480', 'Wheat Bread', 45.00, 'DEMO-BREAD-001', 1, 1014),
    (1022, 115.00, 1015, 'https://picsum.photos/seed/demo-tea/640/480', 'Green Tea', 115.00, 'DEMO-TEA-001', 1, 1015),
    (1023, 79.00, 1014, 'https://picsum.photos/seed/demo-soap/640/480', 'Hand Soap', 79.00, 'DEMO-SOAP-001', 1, 1016),
    (1024, 99.00, 1001, 'https://picsum.photos/seed/demo-coffee/640/480', 'Demo Coffee', 99.00, 'DEMO-COFFEE-001', 1, 1016),
    (1025, 79.00, 1014, 'https://picsum.photos/seed/demo-soap/640/480', 'Hand Soap', 79.00, 'DEMO-SOAP-001', 1, 1017),
    (1026, 60.00, 1009, 'https://picsum.photos/seed/demo-croissant/640/480', 'Croissant', 60.00, 'DEMO-CROISSANT-001', 1, 1018),
    (1027, 75.00, 1010, 'https://picsum.photos/seed/demo-cookies/640/480', 'Chocolate Cookies', 75.00, 'DEMO-COOKIES-001', 1, 1018),
    (1028, 115.00, 1015, 'https://picsum.photos/seed/demo-tea/640/480', 'Green Tea', 115.00, 'DEMO-TEA-001', 1, 1019),
    (1029, 89.00, 1013, 'https://picsum.photos/seed/demo-towel/640/480', 'Paper Towels', 89.00, 'DEMO-TOWEL-001', 1, 1020),
    (1030, 149.00, 1005, 'https://picsum.photos/seed/demo-nuts/640/480', 'Mixed Nuts', 149.00, 'DEMO-NUTS-001', 1, 1020),
    (1031, 135.00, 1011, 'https://picsum.photos/seed/demo-dishwash/640/480', 'Dishwash Liquid', 135.00, 'DEMO-DISHWASH-001', 1, 1021),
    (1032, 45.00, 1008, 'https://picsum.photos/seed/demo-bread/640/480', 'Wheat Bread', 45.00, 'DEMO-BREAD-001', 1, 1022),
    (1033, 98.00, 1002, 'https://picsum.photos/seed/demo-snack/640/480', 'Demo Snack', 49.00, 'DEMO-SNACK-001', 2, 1022),
    (1034, 199.00, 1012, 'https://picsum.photos/seed/demo-cleaner/640/480', 'Surface Cleaner', 199.00, 'DEMO-CLEANER-001', 1, 1023),
    (1035, 79.00, 1014, 'https://picsum.photos/seed/demo-soap/640/480', 'Hand Soap', 79.00, 'DEMO-SOAP-001', 1, 1024),
    (1036, 98.00, 1004, 'https://picsum.photos/seed/demo-chips/640/480', 'Potato Chips', 49.00, 'DEMO-CHIPS-001', 2, 1024),
    (1037, 99.00, 1001, 'https://picsum.photos/seed/demo-coffee/640/480', 'Demo Coffee', 99.00, 'DEMO-COFFEE-001', 1, 1025),
    (1038, 98.00, 1004, 'https://picsum.photos/seed/demo-chips/640/480', 'Potato Chips', 49.00, 'DEMO-CHIPS-001', 2, 1026),
    (1039, 62.00, 1006, 'https://picsum.photos/seed/demo-milk/640/480', 'Fresh Milk', 62.00, 'DEMO-MILK-001', 1, 1027),
    (1040, 45.00, 1007, 'https://picsum.photos/seed/demo-yogurt/640/480', 'Plain Yogurt', 45.00, 'DEMO-YOGURT-001', 1, 1027),
    (1041, 135.00, 1011, 'https://picsum.photos/seed/demo-dishwash/640/480', 'Dishwash Liquid', 135.00, 'DEMO-DISHWASH-001', 1, 1028),
    (1042, 89.00, 1013, 'https://picsum.photos/seed/demo-towel/640/480', 'Paper Towels', 89.00, 'DEMO-TOWEL-001', 1, 1029),
    (1043, 79.00, 1014, 'https://picsum.photos/seed/demo-soap/640/480', 'Hand Soap', 79.00, 'DEMO-SOAP-001', 1, 1029),
    (1044, 115.00, 1015, 'https://picsum.photos/seed/demo-tea/640/480', 'Green Tea', 115.00, 'DEMO-TEA-001', 1, 1030)
ON DUPLICATE KEY UPDATE
    price = VALUES(price),
    product_id = VALUES(product_id),
    product_name = VALUES(product_name),
    product_price = VALUES(product_price),
    product_sku = VALUES(product_sku),
    quantity = VALUES(quantity),
    order_id = VALUES(order_id);

INSERT INTO shift_reports (
    id, branch_id, branch_name, card_sales, cash_sales, cashier_id,
    cashier_name, net_sales, shift_end, shift_start, total_orders,
    total_refunds, total_sales, upi_sales
) VALUES
    (1001, 1001, 'Demo Main Branch', 0.00, 99.00, 1003, 'Demo Cashier', 99.00, DATE_SUB(NOW(6), INTERVAL 56 DAY), DATE_SUB(NOW(6), INTERVAL 56 DAY) - INTERVAL 8 HOUR, 1, 0.00, 99.00, 0.00),
    (1002, 1001, 'Demo Main Branch', 49.00, 159.00, 1004, 'Demo Evening Cashier', 208.00, DATE_SUB(NOW(6), INTERVAL 42 DAY), DATE_SUB(NOW(6), INTERVAL 42 DAY) - INTERVAL 8 HOUR, 4, 0.00, 208.00, 0.00),
    (1003, 1001, 'Demo Main Branch', 199.00, 149.00, 1003, 'Demo Cashier', 348.00, DATE_SUB(NOW(6), INTERVAL 28 DAY), DATE_SUB(NOW(6), INTERVAL 28 DAY) - INTERVAL 8 HOUR, 4, 0.00, 348.00, 0.00),
    (1004, 1001, 'Demo Main Branch', 124.00, 214.00, 1004, 'Demo Evening Cashier', 338.00, DATE_SUB(NOW(6), INTERVAL 14 DAY), DATE_SUB(NOW(6), INTERVAL 14 DAY) - INTERVAL 8 HOUR, 4, 49.00, 387.00, 0.00),
    (1005, 1001, 'Demo Main Branch', 229.00, 199.00, 1003, 'Demo Cashier', 428.00, DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY) - INTERVAL 8 HOUR, 4, 199.00, 627.00, 0.00),
    (1006, 1001, 'Demo Main Branch', 135.00, 238.00, 1004, 'Demo Evening Cashier', 373.00, NOW(6), DATE_SUB(NOW(6), INTERVAL 8 HOUR), 7, 135.00, 508.00, 0.00),
    (1007, 1002, 'Demo North Branch', 98.00, 99.00, 1004, 'Demo Evening Cashier', 197.00, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY) - INTERVAL 8 HOUR, 2, 0.00, 197.00, 0.00),
    (1008, 1003, 'Demo South Branch', 107.00, 135.00, NULL, NULL, 242.00, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY) - INTERVAL 8 HOUR, 2, 135.00, 377.00, 0.00)
ON DUPLICATE KEY UPDATE
    total_sales = VALUES(total_sales),
    net_sales = VALUES(net_sales),
    total_orders = VALUES(total_orders),
    total_refunds = VALUES(total_refunds);

INSERT INTO refunds (
    id, amount, branch_id, cashier_id, cashier_name, created_at,
    payment_type, reason, order_id, shift_report_id, processing_status,
    payment_refund_status, idempotency_key
) VALUES
    (1001, 62.00, 1001, 1003, 'Demo Cashier', DATE_SUB(NOW(6), INTERVAL 13 DAY), 'UPI', 'Demo customer return', 1005, 1004, 'COMPLETED', 'MANUAL_REVIEW_REQUIRED', '00000000-0000-4000-8000-000000002001'),
    (1002, 199.00, 1001, 1003, 'Demo Cashier', DATE_SUB(NOW(6), INTERVAL 6 DAY), 'CASH', 'Damaged household item', 1013, 1005, 'COMPLETED', 'SUCCEEDED', '00000000-0000-4000-8000-000000002002'),
    (1003, 135.00, 1001, 1003, 'Demo Cashier', DATE_SUB(NOW(6), INTERVAL 1 DAY), 'CARD', 'Customer changed order', 1021, 1006, 'COMPLETED', 'MANUAL_REVIEW_REQUIRED', '00000000-0000-4000-8000-000000002003'),
    (1004, 135.00, 1003, 1003, 'Demo Cashier', DATE_SUB(NOW(6), INTERVAL 4 DAY), 'CASH', 'Demo branch return', 1028, 1008, 'COMPLETED', 'MANUAL_REVIEW_REQUIRED', '00000000-0000-4000-8000-000000002004')
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount),
    reason = VALUES(reason),
    order_id = VALUES(order_id),
    shift_report_id = VALUES(shift_report_id),
    payment_refund_status = VALUES(payment_refund_status);

INSERT INTO order_workflows (
    id, order_id, branch_id, state, failure_reason, created_at, updated_at, version
) VALUES
    (1001, 1025, 1002, 'COMPLETED', NULL,
     DATE_SUB(NOW(6), INTERVAL 12 DAY), DATE_SUB(NOW(6), INTERVAL 12 DAY), 0),
    (1002, 1026, 1002, 'COMPLETED', NULL,
     DATE_SUB(NOW(6), INTERVAL 9 DAY), DATE_SUB(NOW(6), INTERVAL 9 DAY), 0),
    (1003, 1027, 1003, 'COMPLETED', NULL,
     DATE_SUB(NOW(6), INTERVAL 8 DAY), DATE_SUB(NOW(6), INTERVAL 8 DAY), 0)
ON DUPLICATE KEY UPDATE
    order_id = VALUES(order_id),
    branch_id = VALUES(branch_id),
    state = VALUES(state),
    failure_reason = VALUES(failure_reason),
    updated_at = VALUES(updated_at);

INSERT INTO order_workflow_steps (
    id, workflow_id, product_id, quantity, reservation_operation_key,
    restoration_operation_key, reservation_state, restoration_state
) VALUES
    (1001, 1001, 1001, 1, 'demo-workflow-1001-reserve-1001',
     'demo-workflow-1001-restore-1001', 'RESERVED', 'PENDING'),
    (1002, 1002, 1004, 2, 'demo-workflow-1002-reserve-1004',
     'demo-workflow-1002-restore-1004', 'RESERVED', 'PENDING'),
    (1003, 1003, 1006, 1, 'demo-workflow-1003-reserve-1006',
     'demo-workflow-1003-restore-1006', 'RESERVED', 'PENDING'),
    (1004, 1003, 1007, 1, 'demo-workflow-1003-reserve-1007',
     'demo-workflow-1003-restore-1007', 'RESERVED', 'PENDING')
ON DUPLICATE KEY UPDATE
    workflow_id = VALUES(workflow_id),
    product_id = VALUES(product_id),
    quantity = VALUES(quantity),
    reservation_operation_key = VALUES(reservation_operation_key),
    restoration_operation_key = VALUES(restoration_operation_key),
    reservation_state = VALUES(reservation_state),
    restoration_state = VALUES(restoration_state);

-- Expand the same demo fixture without creating authentication users, stores,
-- or branches. All foreign IDs below point to the existing seeded records.
USE inventorydb;

INSERT INTO products (
    id, brand, created_at, description, image, mrp, name, selling_price,
    sku, store_id, updated_at, category_id
)
SELECT
    200000 + n,
    'Demo Assortment',
    NOW(6),
    CONCAT('Deterministic demo assortment product ', LPAD(n, 2, '0')),
    CONCAT('https://picsum.photos/seed/demo-assortment-', LPAD(n, 2, '0'), '/640/480'),
    ROUND(price.selling_price * 1.20, 2),
    CONCAT('Demo Assortment ', LPAD(n, 2, '0')),
    price.selling_price,
    CONCAT('DEMO-ASSORTMENT-', LPAD(n, 3, '0')),
    1001,
    NOW(6),
    1001 + MOD(n - 1, 5)
FROM (
    SELECT n,
        CASE n
            WHEN 1 THEN 350.00 WHEN 2 THEN 400.00 WHEN 3 THEN 450.00
            WHEN 4 THEN 475.00 WHEN 5 THEN 525.00 WHEN 6 THEN 550.00
            WHEN 7 THEN 600.00 WHEN 8 THEN 650.00
            WHEN 24 THEN 899.00 WHEN 25 THEN 1299.00
            ELSE ROUND(80 + MOD(n * 47, 671), 2)
        END AS selling_price
    FROM userdb.demo_seed_numbers
    WHERE n BETWEEN 1 AND 25
) price
ON DUPLICATE KEY UPDATE
    brand = VALUES(brand),
    description = VALUES(description),
    image = VALUES(image),
    mrp = VALUES(mrp),
    name = VALUES(name),
    selling_price = VALUES(selling_price),
    store_id = VALUES(store_id),
    category_id = VALUES(category_id);

DROP TEMPORARY TABLE IF EXISTS demo_seed_branch_slots;
CREATE TEMPORARY TABLE demo_seed_branch_slots AS
SELECT n FROM userdb.demo_seed_numbers WHERE n BETWEEN 1 AND 3;
DROP TEMPORARY TABLE IF EXISTS demo_seed_product_slots;
CREATE TEMPORARY TABLE demo_seed_product_slots AS
SELECT n FROM userdb.demo_seed_numbers WHERE n BETWEEN 1 AND 25;

INSERT INTO inventories (id, branch_id, last_updated, quantity, product_id)
SELECT
    300000 + ((branch_slot.n - 1) * 25) + product_slot.n,
    1000 + branch_slot.n,
    NOW(6),
    25 + MOD((product_slot.n * 13) + (branch_slot.n * 17), 96),
    200000 + product_slot.n
FROM demo_seed_branch_slots branch_slot
CROSS JOIN demo_seed_product_slots product_slot
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    branch_id = VALUES(branch_id),
    last_updated = VALUES(last_updated),
    quantity = VALUES(quantity),
    product_id = VALUES(product_id);

UPDATE products_seq SET next_val = GREATEST(next_val, 200026);
UPDATE inventories_seq SET next_val = GREATEST(next_val, 300076);

USE salesdb;

INSERT INTO customer (
    id, branch_id, created_at, email, full_name, loyalty_points, phone,
    store_id, updated_at
)
SELECT
    200000 + n,
    1001 + MOD(n - 1, 3),
    NOW(6),
    CONCAT('customer.', LPAD(14 + n, 3, '0'), '@seed.local'),
    CONCAT('Demo Customer ', 14 + n),
    0,
    CONCAT('+91000000', LPAD(2000 + n, 4, '0')),
    1001,
    NOW(6)
FROM userdb.demo_seed_numbers
WHERE n BETWEEN 1 AND 61
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    branch_id = VALUES(branch_id),
    store_id = VALUES(store_id);

DROP TEMPORARY TABLE IF EXISTS demo_seed_order_lines;
DROP TEMPORARY TABLE IF EXISTS demo_seed_line_numbers;
CREATE TEMPORARY TABLE demo_seed_line_numbers AS
SELECT n FROM userdb.demo_seed_numbers WHERE n BETWEEN 1 AND 8;
CREATE TEMPORARY TABLE demo_seed_order_lines AS
SELECT
    fixture.n AS order_no,
    400000 + fixture.n AS order_id,
    line_slot.n AS line_no,
    product.id AS product_id,
    product.name AS product_name,
    product.sku AS product_sku,
    product.image AS product_image,
    product.selling_price AS product_price,
    CASE
        WHEN fixture.n >= 30 AND MOD(fixture.n - 30, 50) = 0 THEN 1
        ELSE 1 + MOD(fixture.n + line_slot.n, 2)
    END AS quantity,
    ROUND(product.selling_price * CASE
        WHEN fixture.n >= 30 AND MOD(fixture.n - 30, 50) = 0 THEN 1
        ELSE 1 + MOD(fixture.n + line_slot.n, 2)
    END, 2) AS line_total
FROM userdb.demo_seed_numbers fixture
JOIN demo_seed_line_numbers line_slot
  ON line_slot.n BETWEEN 1 AND CASE
      WHEN fixture.n >= 30 AND MOD(fixture.n - 30, 50) = 0 THEN 8
      ELSE 3
  END
JOIN inventorydb.products product
  ON product.id = CASE
      WHEN fixture.n >= 30 AND MOD(fixture.n - 30, 50) = 0
          THEN 200000 + line_slot.n
      ELSE 200001 + MOD((fixture.n * 7) + (line_slot.n * 11), 25)
  END
WHERE fixture.n BETWEEN 1 AND 470;

INSERT INTO orders (
    id, average_item_price, branch_id, cashier_id, created_at, day_of_week,
    discount_amount, discount_percent, is_fraud, items_count, order_hour,
    payment_type, risk_score, score, shift, status, total_amount,
    total_quantity, customer_id, fraud_reason, fraud_status, idempotency_key,
    subtotal_amount, tax_rate, tax_amount, payment_settlement_status
)
SELECT
    400000 + summary.order_no,
    summary.average_item_price,
    1001 + MOD((summary.order_no - 1) + FLOOR((summary.order_no - 1) / 90), 3),
    CASE WHEN MOD(summary.order_no, 2) = 0 THEN 1003 ELSE 1004 END,
    DATE_ADD(
        DATE_SUB(CURDATE(), INTERVAL MOD(summary.order_no - 1, 90) DAY),
        INTERVAL (CASE WHEN MOD(summary.order_no, 2) = 0 THEN 9 ELSE 18 END) HOUR
    ) + INTERVAL MOD(summary.order_no * 13, 60) MINUTE,
    UPPER(DAYNAME(DATE_SUB(CURDATE(), INTERVAL MOD(summary.order_no - 1, 90) DAY))),
    summary.discount_amount,
    summary.discount_percent,
    summary.is_fraud,
    summary.items_count,
    CASE WHEN MOD(summary.order_no, 2) = 0 THEN 9 ELSE 18 END,
    CASE
        WHEN summary.is_fraud = 1 AND MOD(FLOOR(summary.order_no / 50), 2) = 0 THEN 'CASH'
        WHEN summary.is_fraud = 1 THEN 'CARD'
        WHEN MOD(summary.order_no, 3) = 0 THEN 'CASH'
        WHEN MOD(summary.order_no, 3) = 1 THEN 'CARD'
        ELSE 'UPI'
    END,
    CASE
        WHEN summary.is_fraud = 1 AND MOD(FLOOR(summary.order_no / 50), 2) = 0 THEN 74.5
        WHEN summary.is_fraud = 1 THEN 69.0
        ELSE 0.0
    END,
    0,
    CASE WHEN MOD(summary.order_no, 2) = 0 THEN 'Morning' ELSE 'Evening' END,
    CASE WHEN summary.order_no BETWEEN 10 AND 447
              AND MOD(summary.order_no - 10, 19) = 0
         THEN 'REFUNDED' ELSE 'COMPLETED' END,
    summary.subtotal_amount + summary.tax_amount - summary.discount_amount,
    summary.total_quantity,
    200001 + MOD(summary.order_no - 1, 61),
    CASE
        WHEN summary.is_fraud = 1 AND MOD(FLOOR(summary.order_no / 50), 2) = 0
            THEN 'AI model flagged this order; AI Probability: 74.5%'
        WHEN summary.is_fraud = 1
            THEN 'AI model flagged this order; AI Probability: 69.0%'
        ELSE NULL
    END,
    CASE WHEN summary.is_fraud = 1 THEN 'OPEN' ELSE 'NONE' END,
    CONCAT('00000000-0000-4000-8000-', LPAD(400000 + summary.order_no, 12, '0')),
    summary.subtotal_amount,
    18.0000,
    summary.tax_amount,
    CASE
        WHEN summary.is_fraud = 1 AND MOD(FLOOR(summary.order_no / 50), 2) = 0 THEN 'CASH_RECORDED'
        WHEN summary.is_fraud = 1 THEN 'EXTERNAL_CONFIRMATION_REQUIRED'
        WHEN MOD(summary.order_no, 3) = 0 THEN 'CASH_RECORDED'
        ELSE 'EXTERNAL_CONFIRMATION_REQUIRED'
    END
FROM (
    SELECT
        order_no,
        COUNT(*) AS items_count,
        SUM(quantity) AS total_quantity,
        ROUND(SUM(line_total), 2) AS subtotal_amount,
        ROUND(SUM(line_total) / SUM(quantity), 6) AS average_item_price,
        CASE
            WHEN order_no >= 30 AND MOD(order_no - 30, 50) = 0 THEN 1 ELSE 0
        END AS is_fraud,
        CASE
            WHEN order_no >= 30 AND MOD(order_no - 30, 50) = 0 THEN 0.00
            ELSE MOD(order_no, 3) * 5.00
        END AS discount_percent,
        ROUND(SUM(line_total) * 0.18, 2) AS tax_amount,
        ROUND(
            SUM(line_total) *
            CASE
                WHEN order_no >= 30 AND MOD(order_no - 30, 50) = 0 THEN 0.00
                ELSE MOD(order_no, 3) * 5.00
            END / 100,
            2
        ) AS discount_amount
    FROM demo_seed_order_lines
    GROUP BY order_no
) summary
ON DUPLICATE KEY UPDATE
    average_item_price = VALUES(average_item_price),
    branch_id = VALUES(branch_id),
    cashier_id = VALUES(cashier_id),
    created_at = VALUES(created_at),
    day_of_week = VALUES(day_of_week),
    discount_amount = VALUES(discount_amount),
    discount_percent = VALUES(discount_percent),
    is_fraud = VALUES(is_fraud),
    items_count = VALUES(items_count),
    order_hour = VALUES(order_hour),
    payment_type = VALUES(payment_type),
    risk_score = VALUES(risk_score),
    shift = VALUES(shift),
    status = VALUES(status),
    total_amount = VALUES(total_amount),
    total_quantity = VALUES(total_quantity),
    customer_id = VALUES(customer_id),
    fraud_reason = VALUES(fraud_reason),
    fraud_status = VALUES(fraud_status),
    subtotal_amount = VALUES(subtotal_amount),
    tax_rate = VALUES(tax_rate),
    tax_amount = VALUES(tax_amount),
    payment_settlement_status = VALUES(payment_settlement_status);

INSERT INTO order_items (
    id, price, product_id, product_image, product_name, product_price,
    product_sku, quantity, order_id
)
SELECT
    500000 + ((order_no - 1) * 8) + line_no,
    line_total,
    product_id,
    product_image,
    product_name,
    product_price,
    product_sku,
    quantity,
    order_id
FROM demo_seed_order_lines
ON DUPLICATE KEY UPDATE
    price = VALUES(price),
    product_id = VALUES(product_id),
    product_image = VALUES(product_image),
    product_name = VALUES(product_name),
    product_price = VALUES(product_price),
    product_sku = VALUES(product_sku),
    quantity = VALUES(quantity),
    order_id = VALUES(order_id);

INSERT INTO refunds (
    id, amount, branch_id, cashier_id, cashier_name, created_at,
    payment_type, reason, order_id, shift_report_id, processing_status,
    payment_refund_status, idempotency_key
)
SELECT
    600001 + FLOOR((fixture.n - 10) / 19),
    order_row.total_amount,
    order_row.branch_id,
    order_row.cashier_id,
    CASE WHEN order_row.cashier_id = 1003 THEN 'Demo Cashier' ELSE 'Demo Evening Cashier' END,
    DATE_ADD(order_row.created_at, INTERVAL 1 DAY),
    order_row.payment_type,
    CASE MOD(fixture.n, 3)
        WHEN 0 THEN 'Demo customer return'
        WHEN 1 THEN 'Damaged item return'
        ELSE 'Customer changed order'
    END,
    order_row.id,
    700001
        + DATEDIFF(CURDATE(), DATE(order_row.created_at)) * 6
        + (order_row.branch_id - 1001) * 2
        + (order_row.cashier_id - 1003),
    'COMPLETED',
    'MANUAL_REVIEW_REQUIRED',
    CONCAT('00000000-0000-4000-8000-', LPAD(600000 + FLOOR((fixture.n - 10) / 19) + 1, 12, '0'))
FROM userdb.demo_seed_numbers fixture
JOIN orders order_row ON order_row.id = 400000 + fixture.n
WHERE fixture.n BETWEEN 10 AND 447
  AND MOD(fixture.n - 10, 19) = 0
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount),
    branch_id = VALUES(branch_id),
    cashier_id = VALUES(cashier_id),
    cashier_name = VALUES(cashier_name),
    created_at = VALUES(created_at),
    payment_type = VALUES(payment_type),
    reason = VALUES(reason),
    order_id = VALUES(order_id),
    shift_report_id = VALUES(shift_report_id),
    processing_status = VALUES(processing_status),
    payment_refund_status = VALUES(payment_refund_status);

INSERT INTO refund_items (refund_id, order_item_id, product_id, quantity, amount)
SELECT
    refund_row.id,
    item_row.id,
    item_row.product_id,
    item_row.quantity,
    item_row.price
FROM refunds refund_row
JOIN order_items item_row ON item_row.order_id = refund_row.order_id
WHERE refund_row.id BETWEEN 600001 AND 600024
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    quantity = VALUES(quantity),
    amount = VALUES(amount);

INSERT INTO shift_reports (
    id, branch_id, branch_name, card_sales, cash_sales, cashier_id,
    cashier_name, net_sales, shift_end, shift_start, total_orders,
    total_refunds, total_sales, upi_sales
)
SELECT
    700001
        + DATEDIFF(CURDATE(), DATE(MIN(order_row.created_at))) * 6
        + (order_row.branch_id - 1001) * 2
        + (order_row.cashier_id - 1003),
    order_row.branch_id,
    CASE order_row.branch_id
        WHEN 1001 THEN 'Demo Main Branch'
        WHEN 1002 THEN 'Demo North Branch'
        ELSE 'Demo South Branch'
    END,
    ROUND(SUM(CASE WHEN order_row.payment_type = 'CARD' THEN order_row.total_amount ELSE 0 END), 2),
    ROUND(SUM(CASE WHEN order_row.payment_type = 'CASH' THEN order_row.total_amount ELSE 0 END), 2),
    order_row.cashier_id,
    CASE WHEN order_row.cashier_id = 1003 THEN 'Demo Cashier' ELSE 'Demo Evening Cashier' END,
    ROUND(SUM(order_row.total_amount) - SUM(COALESCE(refund_row.amount, 0)), 2),
    DATE(MIN(order_row.created_at)) + INTERVAL (CASE WHEN order_row.cashier_id = 1003 THEN 12 ELSE 22 END) HOUR,
    DATE(MIN(order_row.created_at)) + INTERVAL (CASE WHEN order_row.cashier_id = 1003 THEN 6 ELSE 17 END) HOUR,
    COUNT(DISTINCT order_row.id),
    ROUND(SUM(COALESCE(refund_row.amount, 0)), 2),
    ROUND(SUM(order_row.total_amount), 2),
    ROUND(SUM(CASE WHEN order_row.payment_type = 'UPI' THEN order_row.total_amount ELSE 0 END), 2)
FROM orders order_row
LEFT JOIN refunds refund_row
    ON refund_row.order_id = order_row.id
   AND refund_row.id BETWEEN 600001 AND 600024
WHERE order_row.id BETWEEN 400001 AND 400470
GROUP BY order_row.branch_id, DATE(order_row.created_at), order_row.cashier_id
ON DUPLICATE KEY UPDATE
    branch_id = VALUES(branch_id),
    branch_name = VALUES(branch_name),
    card_sales = VALUES(card_sales),
    cash_sales = VALUES(cash_sales),
    cashier_id = VALUES(cashier_id),
    cashier_name = VALUES(cashier_name),
    net_sales = VALUES(net_sales),
    shift_end = VALUES(shift_end),
    shift_start = VALUES(shift_start),
    total_orders = VALUES(total_orders),
    total_refunds = VALUES(total_refunds),
    total_sales = VALUES(total_sales),
    upi_sales = VALUES(upi_sales);

UPDATE orders order_row
JOIN refunds refund_row ON refund_row.order_id = order_row.id
SET order_row.status = 'REFUNDED'
WHERE order_row.id BETWEEN 400001 AND 400470
  AND refund_row.id BETWEEN 600001 AND 600024;

USE billingdb;

INSERT INTO subscription_plans (
    id, billing_cycle, created_at, description, enable_advanced_reports,
    enable_ecommerce, enable_integrations, enable_inventory,
    enable_invoice_branding, enable_multi_location, max_branches,
    max_products, max_users, name, price, priority_support, updated_at, active
) VALUES
    (1001, 'MONTHLY', NOW(6), 'Development starter plan', 0, 0, 0, 1, 0, 0, 1, 100, 5, 'Demo Starter', 499.00, 0, NOW(6), 1),
    (1002, 'MONTHLY', NOW(6), 'Development professional plan', 1, 1, 1, 1, 1, 1, 5, 500, 25, 'Demo Pro', 999.00, 1, NOW(6), 1),
    (1003, 'YEARLY', NOW(6), 'Development enterprise plan', 1, 1, 1, 1, 1, 1, 20, 2000, 100, 'Demo Enterprise', 9999.00, 1, NOW(6), 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    price = VALUES(price),
    active = VALUES(active);

DELETE FROM subscription_plan_extra_features
WHERE subscription_plan_id IN (1001, 1002, 1003);
INSERT INTO subscription_plan_extra_features (subscription_plan_id, extra_features)
VALUES
    (1001, 'Inventory tracking'),
    (1002, 'Historical analytics'),
    (1002, 'Inventory alerts'),
    (1003, 'Priority support'),
    (1003, 'Multi-location reporting');

INSERT INTO subscriptions (
    id, created_at, end_date, payment_gateway, payment_status, start_date,
    status, store_id, store_name, transaction_id, updated_at, plan_id
) VALUES
    (1001, NOW(6), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'DEMO', 'PAID', CURDATE(), 'ACTIVE', 1001, 'Demo Retail', 'DEMO-SUB-1001', NOW(6), 1002),
    (1002, DATE_SUB(NOW(6), INTERVAL 45 DAY), DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'DEMO', 'PAID', DATE_SUB(CURDATE(), INTERVAL 45 DAY), 'ACTIVE', 1001, 'Demo Retail', 'DEMO-SUB-1002', NOW(6), 1001),
    (1003, DATE_SUB(NOW(6), INTERVAL 120 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'DEMO', 'PAID', DATE_SUB(CURDATE(), INTERVAL 120 DAY), 'EXPIRED', 1001, 'Demo Retail', 'DEMO-SUB-1003', NOW(6), 1003),
    (1004, DATE_SUB(NOW(6), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 'DEMO', 'PAID', DATE_SUB(CURDATE(), INTERVAL 20 DAY), 'ACTIVE', 1002, 'Demo Outlet', 'DEMO-SUB-1004', NOW(6), 1001)
ON DUPLICATE KEY UPDATE
    end_date = VALUES(end_date),
    status = VALUES(status),
    payment_status = VALUES(payment_status),
    plan_id = VALUES(plan_id);

INSERT INTO payment_orders (
    id, amount, created_at, payment_link_id, payment_method, plan_id,
    status, store_id, user_email, user_id
) VALUES
    (1001, 999.00, DATE_SUB(NOW(6), INTERVAL 45 DAY),
     'demo-payment-link-1001', 'RAZORPAY', 1002, 'SUCCESS',
     1001, 'store.admin@seed.local', 1001),
    (1002, 499.00, DATE_SUB(NOW(6), INTERVAL 6 DAY),
     'demo-payment-link-1002', 'STRIPE', 1001, 'PENDING',
     1002, 'store.manager@seed.local', 1005),
    (1003, 9999.00, DATE_SUB(NOW(6), INTERVAL 90 DAY),
     'demo-payment-link-1003', 'RAZORPAY', 1003, 'FAILED',
     1001, 'store.admin@seed.local', 1001)
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount),
    payment_link_id = VALUES(payment_link_id),
    payment_method = VALUES(payment_method),
    plan_id = VALUES(plan_id),
    status = VALUES(status),
    store_id = VALUES(store_id),
    user_email = VALUES(user_email),
    user_id = VALUES(user_id);

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
DROP TEMPORARY TABLE IF EXISTS salesdb.demo_seed_order_lines;
DROP TEMPORARY TABLE IF EXISTS salesdb.demo_seed_line_numbers;
DROP TEMPORARY TABLE IF EXISTS inventorydb.demo_seed_branch_slots;
DROP TEMPORARY TABLE IF EXISTS inventorydb.demo_seed_product_slots;
DROP TEMPORARY TABLE IF EXISTS userdb.demo_seed_numbers;
DROP TEMPORARY TABLE IF EXISTS userdb.demo_seed_digits_hundreds;
DROP TEMPORARY TABLE IF EXISTS userdb.demo_seed_digits_tens;
DROP TEMPORARY TABLE IF EXISTS userdb.demo_seed_digits_ones;
