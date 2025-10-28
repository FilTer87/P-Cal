-- Migration: 019 - Add Floating Time Support
-- Description: Adds local datetime and timezone columns for proper DST handling
-- Applied: 2025-10-28
-- Author: System
--
-- This migration implements "floating time" semantics where:
-- - Tasks maintain their local time (e.g., 15:00) regardless of DST changes
-- - Each task stores its timezone for proper UTC conversion when needed
-- - Notifications and CalDAV sync use UTC calculated from local time + current timezone rules

-- Add new columns to tasks table
ALTER TABLE tasks
    ADD COLUMN start_datetime_local TIMESTAMP,
    ADD COLUMN end_datetime_local TIMESTAMP,
    ADD COLUMN task_timezone VARCHAR(50);

-- Migrate existing data: convert UTC timestamps to local time using user's timezone
-- This preserves the "wall clock time" users see in their timezone
UPDATE tasks t
SET
    start_datetime_local = (t.start_datetime AT TIME ZONE 'UTC' AT TIME ZONE u.timezone)::TIMESTAMP,
    end_datetime_local = (t.end_datetime AT TIME ZONE 'UTC' AT TIME ZONE u.timezone)::TIMESTAMP,
    task_timezone = u.timezone
FROM users u
WHERE t.user_id = u.id;

-- Make new columns NOT NULL after data migration
ALTER TABLE tasks
    ALTER COLUMN start_datetime_local SET NOT NULL,
    ALTER COLUMN end_datetime_local SET NOT NULL,
    ALTER COLUMN task_timezone SET NOT NULL;

-- Add check constraint for valid timezone format (IANA timezone database)
-- This ensures only valid timezones like 'Europe/Rome', 'America/New_York', etc.
ALTER TABLE tasks
    ADD CONSTRAINT tasks_timezone_format CHECK (
        task_timezone ~ '^[A-Za-z]+/[A-Za-z_]+$' OR task_timezone = 'UTC'
    );

-- Keep existing UTC columns for backward compatibility during migration
-- They will be used by CalDAV and notification services until full migration
-- We'll deprecate them in a future migration after testing

-- Add comment to old columns indicating they're deprecated
COMMENT ON COLUMN tasks.start_datetime IS 'DEPRECATED: Use start_datetime_local + task_timezone instead. Will be removed in future migration.';
COMMENT ON COLUMN tasks.end_datetime IS 'DEPRECATED: Use end_datetime_local + task_timezone instead. Will be removed in future migration.';

-- Update indexes to use new columns for performance
CREATE INDEX IF NOT EXISTS idx_tasks_user_datetime_local ON tasks(user_id, start_datetime_local);
CREATE INDEX IF NOT EXISTS idx_tasks_datetime_local_range ON tasks(start_datetime_local, end_datetime_local);
CREATE INDEX IF NOT EXISTS idx_tasks_timezone ON tasks(task_timezone);

-- Update the tasks_with_users view to include new columns
DROP VIEW IF EXISTS tasks_with_users CASCADE;
CREATE OR REPLACE VIEW tasks_with_users AS
SELECT
    t.id,
    t.title,
    t.description,
    t.start_datetime,
    t.end_datetime,
    t.start_datetime_local,
    t.end_datetime_local,
    t.task_timezone,
    t.color,
    t.location,
    t.created_at,
    t.updated_at,
    u.username,
    u.email,
    u.first_name,
    u.last_name,
    u.timezone as user_timezone
FROM tasks t
JOIN users u ON t.user_id = u.id;

-- Create helper function to convert local time to UTC for a given timezone
-- This will be used by notification scheduler and CalDAV export
CREATE OR REPLACE FUNCTION local_to_utc(
    local_time TIMESTAMP,
    tz VARCHAR
) RETURNS TIMESTAMP WITH TIME ZONE AS $$
BEGIN
    RETURN (local_time AT TIME ZONE tz) AT TIME ZONE 'UTC';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Create helper function to get current UTC time for a task's local time
-- Useful for calculating when to send notifications considering current DST rules
CREATE OR REPLACE FUNCTION task_utc_time(
    task_id BIGINT
) RETURNS TIMESTAMP WITH TIME ZONE AS $$
DECLARE
    local_time TIMESTAMP;
    tz VARCHAR;
BEGIN
    SELECT start_datetime_local, task_timezone INTO local_time, tz
    FROM tasks
    WHERE id = task_id;

    RETURN local_to_utc(local_time, tz);
END;
$$ LANGUAGE plpgsql STABLE;

-- Update pending_reminders view to use new floating time logic
-- Reminders should be calculated based on current timezone rules
DROP VIEW IF EXISTS pending_reminders CASCADE;
CREATE OR REPLACE VIEW pending_reminders AS
SELECT
    r.id,
    r.task_id,
    r.reminder_time,
    r.reminder_offset_minutes,
    r.notification_type,
    t.title as task_title,
    t.start_datetime as task_start,
    t.start_datetime_local as task_start_local,
    t.task_timezone,
    -- Calculate actual UTC time for task considering current DST rules
    local_to_utc(t.start_datetime_local, t.task_timezone) as task_start_utc_current,
    u.username,
    u.email
FROM reminders r
JOIN tasks t ON r.task_id = t.id
JOIN users u ON t.user_id = u.id
WHERE r.is_sent = FALSE
AND r.reminder_time <= CURRENT_TIMESTAMP;

-- Add helpful comments
COMMENT ON COLUMN tasks.start_datetime_local IS 'Local datetime without timezone (e.g., 2025-10-20 15:00:00). This is the "wall clock" time that does not change with DST.';
COMMENT ON COLUMN tasks.end_datetime_local IS 'Local datetime without timezone (e.g., 2025-10-20 16:00:00). This is the "wall clock" time that does not change with DST.';
COMMENT ON COLUMN tasks.task_timezone IS 'IANA timezone identifier (e.g., Europe/Rome, America/New_York). Used to convert local time to UTC when needed.';
COMMENT ON FUNCTION local_to_utc IS 'Converts a timezone-naive timestamp to UTC considering the timezone rules (including DST) at that moment in time.';
COMMENT ON FUNCTION task_utc_time IS 'Returns the current UTC equivalent of a task''s local time, accounting for current DST rules.';
