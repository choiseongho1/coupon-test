-- Insert test user
INSERT INTO users (id, email, password, name) VALUES 
(1, 'test@example.com', '$2a$10$yourHashedPasswordHere', 'Test User');

-- Insert test coupon
INSERT INTO coupons (id, code, description, discount_value, discount_type, max_uses, starts_at, expires_at) VALUES 
(1, 'TEST10', '10% off', 10.00, 'PERCENT', 100, '2025-01-01 00:00:00', '2025-12-31 23:59:59');

-- Insert user coupon
INSERT INTO user_coupons (user_id, coupon_id, used) VALUES (1, 1, false);
