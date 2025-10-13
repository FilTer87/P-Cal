-- Migration: 010 - Add Recurrence Support
-- Description: Adds recurrence fields to tasks table for recurring events (RFC 5545 RRULE)
-- Applied: CalDAV integration Phase 1 - Recurring events
-- Author: System

-- Add recurrence_rule column for iCalendar RRULE format
-- NULL means non-recurring task (single occurrence)
-- Example: "FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=10"
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS recurrence_rule VARCHAR(500);

-- Add recurrence_end column for optional end date of recurrence series
-- NULL means no explicit end (relies on COUNT in RRULE or continues indefinitely)
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS recurrence_end TIMESTAMP;

-- Add constraint to ensure recurrence_end is after start_datetime
-- This constraint only applies when recurrence_end is not NULL
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.constraint_column_usage
        WHERE constraint_name = 'check_recurrence_end_after_start'
        AND table_name = 'tasks'
    ) THEN
        ALTER TABLE tasks ADD CONSTRAINT check_recurrence_end_after_start
            CHECK (recurrence_end IS NULL OR recurrence_end > start_datetime);
    END IF;
END $$;

-- Create index for performance on recurring tasks queries
-- Partial index only on tasks with recurrence_rule (most tasks are non-recurring)
CREATE INDEX IF NOT EXISTS idx_tasks_recurrence
    ON tasks(recurrence_rule)
    WHERE recurrence_rule IS NOT NULL;

-- Create composite index for date range queries on recurring tasks
CREATE INDEX IF NOT EXISTS idx_tasks_recurrence_dates
    ON tasks(start_datetime, recurrence_end)
    WHERE recurrence_rule IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN tasks.recurrence_rule IS 'iCalendar RRULE format (RFC 5545) for recurring events. NULL for single-occurrence tasks. Example: FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=10';
COMMENT ON COLUMN tasks.recurrence_end IS 'Optional end date for recurrence series. NULL means no explicit end date (relies on COUNT in RRULE or continues indefinitely)';
