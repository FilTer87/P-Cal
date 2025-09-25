-- Migration: Add ntfy_topic column to users table
-- Date: 2025-09-24
-- Description: Adds ntfy_topic field for NTFY push notifications and populates existing users

-- Add ntfy_topic column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS ntfy_topic VARCHAR(100) UNIQUE;

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_users_ntfy_topic ON users(ntfy_topic);

-- Update existing users with migration topic format
-- This uses the format: ${prefix}-${userId}-migrated
-- The prefix will be read from environment variable in the application
UPDATE users
SET ntfy_topic = CONCAT('calendar-user-', id, '-migrated')
WHERE ntfy_topic IS NULL;