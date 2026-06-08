CREATE TABLE IF NOT EXISTS staff_tasks (
    task_id BIGSERIAL PRIMARY KEY,
    guild_id VARCHAR(32) NOT NULL,
    channel_id VARCHAR(32) NOT NULL,
    message_id VARCHAR(32),
    thread_id VARCHAR(32),
    created_by VARCHAR(32) NOT NULL,
    assigned_to VARCHAR(32),
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    resolved_at BIGINT,
    resolved_by VARCHAR(32)
);
