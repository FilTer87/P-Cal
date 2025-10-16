-- Migration: Add Telegram support
-- Description: Adds telegram_chat_id to users table, creates telegram_registration_tokens table,
--              and updates notification_type constraint to include TELEGRAM

-- Add telegram_chat_id column to users table
ALTER TABLE users
ADD COLUMN telegram_chat_id VARCHAR(100) NULL,
ADD CONSTRAINT users_telegram_chat_id_unique UNIQUE (telegram_chat_id);

COMMENT ON COLUMN users.telegram_chat_id IS 'Telegram Chat ID for sending notifications via Telegram bot';

-- Create telegram_registration_tokens table for temporary registration tokens
CREATE TABLE telegram_registration_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(100) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT telegram_registration_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_telegram_registration_tokens_token ON telegram_registration_tokens(token);
CREATE INDEX idx_telegram_registration_tokens_user_id ON telegram_registration_tokens(user_id);
CREATE INDEX idx_telegram_registration_tokens_expires_at ON telegram_registration_tokens(expires_at);

COMMENT ON TABLE telegram_registration_tokens IS 'Temporary tokens for registering Telegram chat IDs with user accounts';
COMMENT ON COLUMN telegram_registration_tokens.token IS 'Unique registration token shown to user';
COMMENT ON COLUMN telegram_registration_tokens.expires_at IS 'Token expiration timestamp (typically 10 minutes)';
COMMENT ON COLUMN telegram_registration_tokens.used IS 'Whether the token has been used successfully';

-- Update notification_type constraint in reminders table to include TELEGRAM
-- First, drop the existing constraint if it exists
ALTER TABLE reminders DROP CONSTRAINT IF EXISTS reminders_notification_type_check;

-- Add new constraint with TELEGRAM support
ALTER TABLE reminders
ADD CONSTRAINT reminders_notification_type_check
CHECK (notification_type IN ('PUSH', 'EMAIL', 'TELEGRAM'));

COMMENT ON CONSTRAINT reminders_notification_type_check ON reminders IS 'Valid notification types: PUSH (NTFY), EMAIL, TELEGRAM';

-- Note: PUSH is maintained for backward compatibility and represents NTFY notifications
