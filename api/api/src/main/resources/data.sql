-- Your existing role inserts
INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_CLERK') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_CUSTOMER') ON CONFLICT (name) DO NOTHING;

-- Add this: Insert a default Admin User (Password is 'password')
-- We use a subquery to find the ID of ROLE_ADMIN automatically
INSERT INTO users (username, password, role_id)
SELECT 'admin@autohub.co.zw', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.7uAvIJq', id
FROM roles WHERE name = 'ROLE_ADMIN'
ON CONFLICT (username) DO NOTHING;