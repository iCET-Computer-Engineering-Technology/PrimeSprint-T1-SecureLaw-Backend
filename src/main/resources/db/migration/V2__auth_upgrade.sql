CREATE EXTENSION IF NOT EXISTS "pgcrypto";

ALTER TABLE users
ADD COLUMN IF NOT EXISTS password_change_at TIMESTAMP;

CREATE TABLE refresh_tokens
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL,
    token_hash   VARCHAR(64) NOT NULL,
    issued_at    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMP   NOT NULL,
    revoked      BOOLEAN          DEFAULT FALSE,
    last_used_at TIMESTAMP,
    replaced_by  UUID,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE access_token_denylist
(
    jti        VARCHAR(255) PRIMARY KEY,
    user_id    UUID      NOT NULL,
    expires_at TIMESTAMP NOT NULL,
);