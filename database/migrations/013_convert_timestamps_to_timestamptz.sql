-- Migration: 013 - Convert TIMESTAMP to TIMESTAMP WITH TIME ZONE
-- Description: Converts all datetime columns to use TIMESTAMP WITH TIME ZONE for proper UTC handling
-- Applied: 2025-10-14
-- Author: System

-- Convert tasks table timestamps
ALTER TABLE tasks
    ALTER COLUMN start_datetime TYPE TIMESTAMP WITH TIME ZONE USING start_datetime AT TIME ZONE 'UTC',
    ALTER COLUMN end_datetime TYPE TIMESTAMP WITH TIME ZONE USING end_datetime AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC';

-- Convert reminders table timestamps
ALTER TABLE reminders
    ALTER COLUMN reminder_time TYPE TIMESTAMP WITH TIME ZONE USING reminder_time AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC';

-- Convert reminder last_sent_occurrence if exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'reminders' AND column_name = 'last_sent_occurrence'
    ) THEN
        ALTER TABLE reminders
            ALTER COLUMN last_sent_occurrence TYPE TIMESTAMP WITH TIME ZONE USING last_sent_occurrence AT TIME ZONE 'UTC';
    END IF;
END $$;

-- Convert users table timestamps
ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC';

-- Convert recurrence_end if exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tasks' AND column_name = 'recurrence_end'
    ) THEN
        ALTER TABLE tasks
            ALTER COLUMN recurrence_end TYPE TIMESTAMP WITH TIME ZONE USING recurrence_end AT TIME ZONE 'UTC';
    END IF;
END $$;

-- Recreate views with new timestamp types
DROP VIEW IF EXISTS tasks_with_users CASCADE;
CREATE OR REPLACE VIEW tasks_with_users AS
SELECT
    t.id,
    t.title,
    t.description,
    t.start_datetime,
    t.end_datetime,
    t.color,
    t.location,
    t.created_at,
    t.updated_at,
    u.username,
    u.email,
    u.first_name,
    u.last_name
FROM tasks t
JOIN users u ON t.user_id = u.id;

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
    u.username,
    u.email
FROM reminders r
JOIN tasks t ON r.task_id = t.id
JOIN users u ON t.user_id = u.id
WHERE r.is_sent = FALSE
AND r.reminder_time <= CURRENT_TIMESTAMP;
