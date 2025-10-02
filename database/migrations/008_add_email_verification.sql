-- Migration: 008 - Add Email Verification
-- Description: Adds email verification functionality to users and creates email_verification_tokens table
-- Applied: TBD
-- Author: System

-- Add email_verified column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Create email_verification_tokens table
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_token ON email_verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id ON email_verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_expiry_date ON email_verification_tokens(expiry_date);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_used ON email_verification_tokens(used);

-- Create composite index for common queries
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_token_valid
ON email_verification_tokens(token, used, expiry_date)
WHERE used = false;

-- Add comments for documentation
COMMENT ON COLUMN users.email_verified IS 'Whether the user has verified their email address';
COMMENT ON TABLE email_verification_tokens IS 'Stores email verification tokens for users';
COMMENT ON COLUMN email_verification_tokens.token IS 'Unique UUID token for email verification';
COMMENT ON COLUMN email_verification_tokens.user_id IS 'Reference to user verifying their email';
COMMENT ON COLUMN email_verification_tokens.expiry_date IS 'When the token expires (typically 48 hours)';
COMMENT ON COLUMN email_verification_tokens.used IS 'Whether the token has been used (one-time use)';
COMMENT ON COLUMN email_verification_tokens.created_at IS 'When the token was created';
