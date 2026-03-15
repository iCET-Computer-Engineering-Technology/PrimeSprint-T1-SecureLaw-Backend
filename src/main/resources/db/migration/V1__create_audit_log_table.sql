
CREATE TABLE audit_log (
    id VARCHAR(250) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    target VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    template_id VARCHAR(100),
    mask_counts JSONB,
    model_used VARCHAR(50),
    response_time BIGINT,
    details TEXT
);

CREATE INDEX idx_audit_user
ON audit_log(user_id);

CREATE INDEX idx_audit_timestamp
ON audit_log(timestamp);

CREATE INDEX idx_audit_action
ON audit_log(action);