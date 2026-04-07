CREATE TABLE chat_turns
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    chat_id     UUID        NOT NULL,
    user_prompt TEXT        NOT NULL,
    ai_response TEXT        NOT NULL,
    model_name  VARCHAR(255),
    latency_ms  BIGINT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_turn_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE
);

CREATE INDEX idx_turns_chat ON chat_turns (chat_id);

CREATE INDEX idx_turns_created_at ON chat_turns (created_at);