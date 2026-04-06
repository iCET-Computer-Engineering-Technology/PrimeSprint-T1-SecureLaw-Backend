CREATE TABLE chats
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    profile_id UUID        NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chat_profile FOREIGN KEY (profile_id) REFERENCES profiles (id) ON DELETE CASCADE
);

CREATE INDEX idx_chats_profile_id ON chats (profile_id);

CREATE INDEX idx_chats_created_at ON chats (created_at);