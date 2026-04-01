-- Seed test users (57 total): 12 seniors + 45 juniors
-- Default test password for all users is: password
-- BCrypt hash below matches "password".

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'SENIOR') THEN
        RAISE EXCEPTION 'Missing role: SENIOR';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'JUNIOR') THEN
        RAISE EXCEPTION 'Missing role: JUNIOR';
    END IF;
END $$;

WITH senior_role AS (
    SELECT id AS role_id
    FROM roles
    WHERE name = 'SENIOR'
)
INSERT INTO users (username, email, password, role_id, status, senior_id)
SELECT
    'seed_senior_' || lpad(gs::text, 2, '0') AS username,
    'seed.senior.' || lpad(gs::text, 2, '0') || '@example.com' AS email,
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOeP6n8fR6k9KuX3sI5OGucs5cjox96D.' AS password,
    sr.role_id,
    'ACTIVE',
    NULL
FROM generate_series(1, 12) AS gs
CROSS JOIN senior_role sr;

WITH junior_role AS (
    SELECT id AS role_id
    FROM roles
    WHERE name = 'JUNIOR'
),
senior_map AS (
    SELECT
        id,
        row_number() OVER (ORDER BY username) AS rn
    FROM users
    WHERE username LIKE 'seed_senior_%'
)
INSERT INTO users (username, email, password, role_id, status, senior_id)
SELECT
    'seed_junior_' || lpad(gs::text, 2, '0') AS username,
    'seed.junior.' || lpad(gs::text, 2, '0') || '@example.com' AS email,
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOeP6n8fR6k9KuX3sI5OGucs5cjox96D.' AS password,
    jr.role_id,
    'ACTIVE',
    sm.id AS senior_id
FROM generate_series(1, 45) AS gs
CROSS JOIN junior_role jr
JOIN senior_map sm ON sm.rn = ((gs - 1) % 12) + 1;

