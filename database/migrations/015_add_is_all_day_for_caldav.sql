-- Migration: Add is_all_day column for CalDAV support
-- Date: 2025-10-21
-- Description: Re-adds the is_all_day boolean column to tasks table for CalDAV integration
--              This field distinguishes between timed events and all-day events
--              Used for proper import/export of iCalendar DATE vs DATE-TIME

-- Add is_all_day column to tasks table (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
        AND column_name = 'is_all_day'
    ) THEN
        ALTER TABLE tasks ADD COLUMN is_all_day BOOLEAN DEFAULT FALSE NOT NULL;
        RAISE NOTICE 'Column is_all_day added to tasks table';
    ELSE
        RAISE NOTICE 'Column is_all_day already exists in tasks table';
    END IF;
END $$;

-- Add index for queries filtering all-day events
CREATE INDEX IF NOT EXISTS idx_tasks_is_all_day ON tasks(is_all_day);

-- Update comment for documentation
COMMENT ON COLUMN tasks.is_all_day IS 'True for all-day events (DATE in iCalendar), false for timed events (DATE-TIME). Used for CalDAV import/export.';
