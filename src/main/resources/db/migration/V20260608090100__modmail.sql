CREATE TABLE IF NOT EXISTS modmail_threads (
    user_id VARCHAR(32) NOT NULL,
    thread_id VARCHAR(32) PRIMARY KEY,
    parent_message_id VARCHAR(32),
    last_sender VARCHAR(16),
    assigned_staff_id VARCHAR(32),
    created_at BIGINT NOT NULL,
    last_message_at BIGINT NOT NULL,
    closed BOOLEAN NOT NULL DEFAULT FALSE
);
