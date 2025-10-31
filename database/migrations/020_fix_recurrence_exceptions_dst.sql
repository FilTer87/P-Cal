-- Migration: 020 - Fix Recurrence Exceptions for DST Handling
-- Description: Resets recurrence_exceptions due to bug in DST offset calculation
-- Background: Previous implementation converted LocalDateTime to UTC naively,
--             causing EXDATE mismatches across DST transitions. The fix ensures
--             EXDATE uses the actual UTC instant of generated occurrences.
-- Applied: 2025-10-31
-- Author: System

-- IMPORTANT: This migration resets all existing recurrence exceptions.
-- Users will need to re-cancel specific occurrences from their calendar app.
-- This is necessary because the old exceptions were stored with incorrect UTC offsets.

-- Log affected tasks before resetting
DO $$
DECLARE
    affected_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO affected_count
    FROM tasks
    WHERE recurrence_exceptions IS NOT NULL AND recurrence_exceptions != '';

    IF affected_count > 0 THEN
        RAISE NOTICE 'Resetting recurrence_exceptions for % task(s) due to DST bug fix', affected_count;
        RAISE NOTICE 'Affected tasks will need their cancelled occurrences to be re-created';
    END IF;
END $$;

-- Create backup table with old exceptions (for reference/rollback if needed)
CREATE TABLE IF NOT EXISTS tasks_recurrence_exceptions_backup_20251031 AS
SELECT
    uid,
    title,
    recurrence_rule,
    recurrence_exceptions,
    task_timezone,
    updated_at
FROM tasks
WHERE recurrence_exceptions IS NOT NULL AND recurrence_exceptions != '';

-- Add comment to backup table
COMMENT ON TABLE tasks_recurrence_exceptions_backup_20251031 IS
'Backup of recurrence_exceptions before DST fix migration (2025-10-31).
Old exceptions were stored with incorrect UTC offsets for occurrences across DST transitions.
This backup is kept for reference and can be used for manual recovery if needed.';

-- Reset recurrence_exceptions to NULL
-- Users will re-cancel occurrences using the fixed implementation
UPDATE tasks
SET
    recurrence_exceptions = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE recurrence_exceptions IS NOT NULL AND recurrence_exceptions != '';

-- Log completion
DO $$
DECLARE
    backup_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO backup_count
    FROM tasks_recurrence_exceptions_backup_20251031;

    RAISE NOTICE 'Migration completed successfully';
    RAISE NOTICE 'Backed up % task(s) to tasks_recurrence_exceptions_backup_20251031', backup_count;
    RAISE NOTICE 'Users can now re-cancel specific occurrences with correct DST handling';
END $$;
