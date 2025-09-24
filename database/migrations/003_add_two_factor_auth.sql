-- Migration: 003 - Add Two-Factor Authentication
-- Description: Adds 2FA TOTP support to user authentication
-- Applied: 2FA feature implementation
-- Author: System

-- Add new columns for two-factor authentication
ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(255);

-- Add index for performance on 2FA queries
CREATE INDEX IF NOT EXISTS idx_users_two_factor_enabled
    ON users(two_factor_enabled)
    WHERE two_factor_enabled = true;

-- Update existing users to have 2FA disabled by default
UPDATE users
SET
    two_factor_enabled = COALESCE(two_factor_enabled, false),
    two_factor_secret = COALESCE(two_factor_secret, NULL)
WHERE
    two_factor_enabled IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN users.two_factor_enabled IS 'Indicates if two-factor authentication is enabled for the user';
COMMENT ON COLUMN users.two_factor_secret IS 'TOTP secret key for two-factor authentication (base32 encoded)';

-- Security note: two_factor_secret should be encrypted in production
-- Current implementation stores it in plaintext for development
-- TODO: Add encryption layer for production deployment