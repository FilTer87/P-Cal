-- Migration: 017 - Add Calendars table for multi-calendar support
-- Description: Creates calendars table to support multiple calendars per user
--              Prepares architecture for CalDAV server with URL structure /caldav/{user}/{calendar}/
--              All existing tasks will be associated with a "default" calendar per user
-- Applied: [PENDING]
-- Author: System

-- Create calendars table
CREATE TABLE IF NOT EXISTS calendars (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description TEXT,
    color VARCHAR(7) DEFAULT '#3788d8',
    is_default BOOLEAN DEFAULT FALSE,
    is_visible BOOLEAN DEFAULT TRUE,
    timezone VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT calendars_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT calendars_slug_format CHECK (slug ~ '^[a-z0-9-]+$'),
    CONSTRAINT calendars_user_slug_unique UNIQUE (user_id, slug),
    CONSTRAINT calendars_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT calendars_slug_not_empty CHECK (LENGTH(TRIM(slug)) > 0)
);

-- Add calendar_id to tasks table (nullable initially for migration)
ALTER TABLE tasks
ADD COLUMN calendar_id BIGINT REFERENCES calendars(id) ON DELETE CASCADE;

-- Create indexes for performance
CREATE INDEX idx_calendars_user_id ON calendars(user_id);
CREATE INDEX idx_calendars_slug ON calendars(user_id, slug);
CREATE INDEX idx_calendars_is_default ON calendars(user_id, is_default) WHERE is_default = TRUE;
CREATE INDEX idx_tasks_calendar_id ON tasks(calendar_id);

-- Create trigger for calendars updated_at
DROP TRIGGER IF EXISTS update_calendars_updated_at ON calendars;
CREATE TRIGGER update_calendars_updated_at
    BEFORE UPDATE ON calendars
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create default calendar for each existing user
-- This ensures backward compatibility with existing tasks
INSERT INTO calendars (user_id, name, slug, description, color, is_default, timezone)
SELECT
    u.id,
    'Default Calendar',
    'default',
    'Your default calendar',
    '#3788d8',
    TRUE,
    u.timezone
FROM users u;

-- Associate all existing tasks with their user's default calendar
UPDATE tasks t
SET calendar_id = (
    SELECT c.id
    FROM calendars c
    WHERE c.user_id = t.user_id
    AND c.is_default = TRUE
    LIMIT 1
);

-- Now make calendar_id NOT NULL (all tasks have been migrated)
ALTER TABLE tasks
ALTER COLUMN calendar_id SET NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE calendars IS 'User calendars - supports multiple calendars per user for CalDAV integration';
COMMENT ON COLUMN calendars.slug IS 'URL-safe calendar identifier for CalDAV URLs (e.g., /caldav/user/default/)';
COMMENT ON COLUMN calendars.is_default IS 'Default calendar for user - used for backward compatibility and as fallback';
COMMENT ON COLUMN calendars.is_visible IS 'Whether calendar is visible in UI (for future archive feature)';
COMMENT ON COLUMN tasks.calendar_id IS 'Foreign key to calendars table - every task belongs to one calendar';

-- Create view for tasks with calendar information
CREATE OR REPLACE VIEW tasks_with_calendar AS
SELECT
    t.id,
    t.user_id,
    t.title,
    t.description,
    t.start_datetime,
    t.end_datetime,
    t.color,
    t.is_all_day,
    t.location,
    t.uid,
    t.recurrence_rule,
    t.recurrence_end,
    t.recurrence_exceptions,
    t.created_at,
    t.updated_at,
    c.id AS calendar_id,
    c.name AS calendar_name,
    c.slug AS calendar_slug,
    c.color AS calendar_color
FROM tasks t
INNER JOIN calendars c ON t.calendar_id = c.id;

COMMENT ON VIEW tasks_with_calendar IS 'Denormalized view of tasks with calendar information for easier queries';
