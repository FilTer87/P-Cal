-- Migration: 001 - Initial Schema
-- Description: Creates initial database structure for PrivateCal
-- Applied: Initial setup
-- Author: System

-- This is the original init.sql content
-- Moved here for better migration tracking

-- Create database extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    timezone VARCHAR(50) DEFAULT 'UTC',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    start_datetime TIMESTAMP NOT NULL,
    end_datetime TIMESTAMP NOT NULL,
    color VARCHAR(7) DEFAULT '#3788d8',
    is_all_day BOOLEAN DEFAULT FALSE,
    location VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT tasks_end_after_start CHECK (end_datetime > start_datetime),
    CONSTRAINT tasks_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

-- Reminders table
CREATE TABLE IF NOT EXISTS reminders (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    reminder_time TIMESTAMP NOT NULL,
    reminder_offset_minutes INTEGER NOT NULL,
    is_sent BOOLEAN DEFAULT FALSE,
    notification_type VARCHAR(20) DEFAULT 'PUSH',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT reminder_offset_positive CHECK (reminder_offset_minutes >= 0),
    CONSTRAINT notification_type_valid CHECK (notification_type IN ('PUSH', 'EMAIL'))
);

-- Indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

CREATE INDEX IF NOT EXISTS idx_tasks_user_datetime ON tasks(user_id, start_datetime);
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_datetime_range ON tasks(start_datetime, end_datetime);

CREATE INDEX IF NOT EXISTS idx_reminders_task_id ON reminders(task_id);
CREATE INDEX IF NOT EXISTS idx_reminders_time_sent ON reminders(reminder_time, is_sent);
CREATE INDEX IF NOT EXISTS idx_reminders_unsent ON reminders(is_sent) WHERE is_sent = FALSE;

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for automatic updated_at updates
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_tasks_updated_at ON tasks;
CREATE TRIGGER update_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create view for tasks with user information (useful for admin purposes)
CREATE OR REPLACE VIEW tasks_with_users AS
SELECT
    t.id,
    t.title,
    t.description,
    t.start_datetime,
    t.end_datetime,
    t.color,
    t.is_all_day,
    t.location,
    t.created_at,
    t.updated_at,
    u.username,
    u.email,
    u.first_name,
    u.last_name
FROM tasks t
JOIN users u ON t.user_id = u.id;

-- Create view for pending reminders
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