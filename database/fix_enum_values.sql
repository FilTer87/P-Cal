-- Fix NotificationType enum values from lowercase to uppercase
-- This script corrects existing data to match the Java enum values

alter table reminders drop constraint notification_type_valid;

-- Update existing reminder notification_type values
UPDATE reminders SET notification_type = 'PUSH' WHERE notification_type = 'push';
UPDATE reminders SET notification_type = 'EMAIL' WHERE notification_type = 'email';

alter table reminders add constraint notification_type_valid CHECK (notification_type IN ('PUSH', 'EMAIL'));

-- Verify the update
SELECT notification_type, COUNT(*) as count 
FROM reminders 
GROUP BY notification_type;

-- Show updated reminder data
SELECT id, task_id, notification_type, reminder_time, is_sent 
FROM reminders 
ORDER BY id;