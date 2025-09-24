-- Migration: 004 - Add Week Start Day Preference
-- Description: Adds week_start_day preference to users table
-- Applied: Week start day preference feature
-- Author: System

-- Add new column for week start day preference
ALTER TABLE users ADD COLUMN IF NOT EXISTS week_start_day INTEGER DEFAULT 1;

-- Add constraint to ensure valid values (0 = Sunday, 1 = Monday)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.constraint_column_usage
        WHERE constraint_name = 'check_week_start_day_valid'
        AND table_name = 'users'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT check_week_start_day_valid
            CHECK (week_start_day IN (0, 1));
    END IF;
END $$;

-- Update existing users with default preference (Monday = 1)
UPDATE users
SET week_start_day = COALESCE(week_start_day, 1)
WHERE week_start_day IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN users.week_start_day IS 'Week start day preference: 0 = Sunday, 1 = Monday';