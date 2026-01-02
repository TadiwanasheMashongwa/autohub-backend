-- 1. Ensure the roles exist
INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_CLERK') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_CUSTOMER') ON CONFLICT (name) DO NOTHING;

-- 2. Clear and re-insert the admin user to ensure the BCrypt hash is correct
DELETE FROM users WHERE username = 'admin@autohub.co.zw';

-- Password is 'password' (BCrypt hash)
INSERT INTO users (username, password, role_id)
SELECT 'admin@autohub.co.zw', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.7uAvIJq', id
FROM roles WHERE name = 'ROLE_ADMIN';