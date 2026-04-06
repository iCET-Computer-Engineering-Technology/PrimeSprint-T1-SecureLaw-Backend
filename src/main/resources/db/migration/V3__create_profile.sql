CREATE TABLE profiles
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID UNIQUE NOT NULL,
    display_name VARCHAR(150),
    created_at   TIMESTAMP        DEFAULT now(),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);