-- Migration: 011 - Add Last Sent Occurrence to Reminders
-- Description: Adds last_sent_occurrence field to track which occurrence of recurring task was last processed
-- Applied: CalDAV integration Phase 2 - Recurring reminders support
-- Author: System

-- Add last_sent_occurrence column to track which occurrence was processed
-- NULL means reminder hasn't been sent yet or task is non-recurring
-- For recurring tasks, this stores the start_datetime of the occurrence that was just processed
ALTER TABLE reminders ADD COLUMN IF NOT EXISTS last_sent_occurrence TIMESTAMP;

-- Add comment for documentation
COMMENT ON COLUMN reminders.last_sent_occurrence IS 'For recurring tasks: stores the start_datetime of the last processed occurrence. Used to calculate next reminder time. NULL for non-recurring tasks or unsent reminders.';
