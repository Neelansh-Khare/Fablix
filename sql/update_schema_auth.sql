ALTER TABLE customers ADD COLUMN IF NOT EXISTS salt VARCHAR(32) DEFAULT NULL;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'customer';

INSERT INTO creditcards (id, first_name, last_name, expiration)
VALUES ('admin001', 'Admin', 'User', '2030-01-01')
ON CONFLICT (id) DO NOTHING;

INSERT INTO customers (first_name, last_name, cc_id, address, email, password, salt, role)
VALUES ('Admin', 'User', 'admin001', '123 Admin St', 'admin@fabflix.com', 'admin123', NULL, 'admin')
ON CONFLICT (email) DO NOTHING;
