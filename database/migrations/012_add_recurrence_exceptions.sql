-- Migration: 012 - Add Recurrence Exceptions Support
-- Description: Adds recurrence_exceptions field for EXDATE (RFC 5545) to exclude specific occurrences
-- Applied: CalDAV integration Phase 3 - Single occurrence edit support
-- Author: System

-- Add recurrence_exceptions column to store EXDATE values
-- Format: Comma-separated list of ISO 8601 datetime strings
-- Example: "2025-10-17T10:00:00Z,2025-10-24T10:00:00Z"
-- NULL means no exceptions (all occurrences are valid)
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS recurrence_exceptions TEXT;

-- Add comment for documentation
COMMENT ON COLUMN tasks.recurrence_exceptions IS 'EXDATE values (RFC 5545): Comma-separated list of ISO 8601 datetime strings for excluded occurrences. Used when editing single occurrence of recurring task. Example: 2025-10-17T10:00:00Z,2025-10-24T10:00:00Z';
