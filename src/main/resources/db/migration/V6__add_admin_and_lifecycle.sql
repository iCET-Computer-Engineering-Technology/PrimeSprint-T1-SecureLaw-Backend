INSERT INTO roles (name)
VALUES ('ADMIN');
ON CONFLICT (name) DO NOTHING;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT TRUE;

ALTER TABLE users
ALTER COLUMN status SET DEFAULT 'ACTIVE';

INSERT INTO users(username, email, password, role_id, status)
VALUE(
'admin',
'admin@securelaw.com',
    '$2a$10$8.UnVuG9HHgffUDAlk8q6uy.8tJ/E.E8uH7t5.uA3Xp3n.E6pG6Wq',
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    'ACTIVE',
    TRUE
) ON CONFLICT (username) DO NOTHING;

