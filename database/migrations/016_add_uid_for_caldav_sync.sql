-- Migration: 016 - Add UID column for CalDAV synchronization
-- Description: Adds UID (unique identifier) column to tasks table for iCalendar RFC 5545 compliance
--              Enables duplicate detection during import and proper CalDAV sync
-- Applied: [PENDING]
-- Author: System

-- Add UID column to tasks table (nullable initially for existing tasks)
ALTER TABLE tasks
ADD COLUMN uid VARCHAR(255);

-- Create unique index on (user_id, uid) to prevent duplicates
-- Note: UID must be unique per user, but can be NULL for legacy tasks
CREATE UNIQUE INDEX idx_tasks_user_uid ON tasks(user_id, uid)
WHERE uid IS NOT NULL;

-- Generate UIDs for existing tasks using deterministic format
-- Format: privatecal-[user_id]-[task_id]-[hash]
-- This ensures existing tasks get valid UIDs for export compatibility
UPDATE tasks
SET uid = CONCAT(
    'privatecal-',
    user_id,
    '-',
    id,
    '-',
    SUBSTRING(MD5(CONCAT(COALESCE(title, ''), COALESCE(start_datetime::text, ''))), 1, 8)
)
WHERE uid IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN tasks.uid IS 'iCalendar UID (RFC 5545) for synchronization and duplicate detection. Format: domain@host or generated hash';

-- Create index for faster lookups during import
CREATE INDEX idx_tasks_uid ON tasks(uid)
WHERE uid IS NOT NULL;
