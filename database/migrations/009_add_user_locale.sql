-- Migration: 009 - Add User Locale
-- Description: Adds locale column to users table for internationalization
-- Applied: i18n language preference feature
-- Author: System

-- Add new column for user locale preference
-- NULL value indicates locale has not been set yet (will be auto-detected on first login)
ALTER TABLE users ADD COLUMN IF NOT EXISTS locale VARCHAR(10);

-- Add constraint to ensure valid locale format (xx-XX pattern)
-- This allows any valid BCP 47 language tag without requiring DB migration for new languages
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.constraint_column_usage
        WHERE constraint_name = 'check_locale_format'
        AND table_name = 'users'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT check_locale_format
            CHECK (locale IS NULL OR locale ~ '^[a-z]{2}-[A-Z]{2}$');
    END IF;
END $$;

-- Create index for better performance on locale queries
CREATE INDEX IF NOT EXISTS idx_users_locale
    ON users(locale);

-- Add comment for documentation
COMMENT ON COLUMN users.locale IS 'User interface language preference in BCP 47 format (e.g., it-IT, en-US, fr-FR). NULL means not yet set (auto-detect on first login)';
