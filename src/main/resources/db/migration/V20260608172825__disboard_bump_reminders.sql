CREATE TABLE IF NOT EXISTS disboard_bump_reminders (
    guild_id VARCHAR(32) PRIMARY KEY,
    channel_id VARCHAR(32) NOT NULL,
    role_id VARCHAR(32) NOT NULL,
    last_bump_at BIGINT NOT NULL,
    next_reminder_at BIGINT NOT NULL
);
