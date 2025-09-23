-- Migration: 002 - Add User Preferences
-- Description: Adds user preferences columns to users table
-- Applied: User preferences feature
-- Author: System

-- Add new columns for user preferences
ALTER TABLE users ADD COLUMN IF NOT EXISTS theme VARCHAR(10) DEFAULT 'system';
ALTER TABLE users ADD COLUMN IF NOT EXISTS time_format VARCHAR(5) DEFAULT '24h';
ALTER TABLE users ADD COLUMN IF NOT EXISTS calendar_view VARCHAR(10) DEFAULT 'week';
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_notifications BOOLEAN DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS reminder_notifications BOOLEAN DEFAULT true;

-- Add constraints to ensure valid values
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.constraint_column_usage
        WHERE constraint_name = 'check_theme_valid'
        AND table_name = 'users'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT check_theme_valid
            CHECK (theme IN ('light', 'dark', 'system'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.constraint_column_usage
        WHERE constraint_name = 'check_time_format_valid'
        AND table_name = 'users'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT check_time_format_valid
            CHECK (time_format IN ('12h', '24h'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.constraint_column_usage
        WHERE constraint_name = 'check_calendar_view_valid'
        AND table_name = 'users'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT check_calendar_view_valid
            CHECK (calendar_view IN ('month', 'week', 'day', 'agenda'));
    END IF;
END $$;

-- Create partial index for better performance on preference queries
CREATE INDEX IF NOT EXISTS idx_users_preferences
    ON users(theme, time_format, calendar_view);

-- Update existing users with default preferences (if any exist without these columns)
UPDATE users
SET
    theme = COALESCE(theme, 'system'),
    time_format = COALESCE(time_format, '24h'),
    calendar_view = COALESCE(calendar_view, 'week'),
    email_notifications = COALESCE(email_notifications, true),
    reminder_notifications = COALESCE(reminder_notifications, true)
WHERE
    theme IS NULL
    OR time_format IS NULL
    OR calendar_view IS NULL
    OR email_notifications IS NULL
    OR reminder_notifications IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN users.theme IS 'User interface theme preference: light, dark, or system';
COMMENT ON COLUMN users.time_format IS 'Time display format preference: 12h or 24h';
COMMENT ON COLUMN users.calendar_view IS 'Default calendar view: month, week, day, or agenda';
COMMENT ON COLUMN users.email_notifications IS 'Enable/disable email notifications';
COMMENT ON COLUMN users.reminder_notifications IS 'Enable/disable reminder notifications';