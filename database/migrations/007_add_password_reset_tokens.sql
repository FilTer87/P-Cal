-- Migration: 007 - Add Password Reset Tokens
-- Description: Creates password_reset_tokens table for password reset functionality
-- Applied: TBD
-- Author: System

-- Create password_reset_tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expiry_date ON password_reset_tokens(expiry_date);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_used ON password_reset_tokens(used);

-- Create composite index for common queries
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token_valid
ON password_reset_tokens(token, used, expiry_date)
WHERE used = false;

-- Add comments for documentation
COMMENT ON TABLE password_reset_tokens IS 'Stores password reset tokens for users';
COMMENT ON COLUMN password_reset_tokens.token IS 'Unique UUID token for password reset';
COMMENT ON COLUMN password_reset_tokens.user_id IS 'Reference to user requesting password reset';
COMMENT ON COLUMN password_reset_tokens.expiry_date IS 'When the token expires (typically 1 hour)';
COMMENT ON COLUMN password_reset_tokens.used IS 'Whether the token has been used (one-time use)';
COMMENT ON COLUMN password_reset_tokens.created_at IS 'When the token was created';