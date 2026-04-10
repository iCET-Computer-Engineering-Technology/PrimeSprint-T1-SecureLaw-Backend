CREATE TABLE password_reset_tokens
(
	id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
	user_id    UUID        NOT NULL,
	token      VARCHAR(255) NOT NULL UNIQUE,
	expires_at TIMESTAMPTZ NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	used_at    TIMESTAMPTZ,

	CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
	CONSTRAINT chk_password_reset_expiry CHECK (expires_at > created_at)
);
