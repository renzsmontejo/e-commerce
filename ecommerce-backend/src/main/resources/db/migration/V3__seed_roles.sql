-- Roles only: no default user credentials or hard-coded role IDs.
INSERT INTO roles (name, description, created_at)
SELECT 'CUSTOMER', 'Regular customer account', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='CUSTOMER');
INSERT INTO roles (name, description, created_at)
SELECT 'ADMIN', 'Store administrator', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='ADMIN');
