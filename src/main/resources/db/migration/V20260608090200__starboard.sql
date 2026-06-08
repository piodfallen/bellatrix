CREATE TABLE IF NOT EXISTS starboard_entries (
    original_message_id VARCHAR(32) PRIMARY KEY,
    starboard_message_id VARCHAR(32) NOT NULL,
    channel_id VARCHAR(32) NOT NULL,
    guild_id VARCHAR(32) NOT NULL,
    star_count INTEGER NOT NULL
);
