-- P-Cal Database Schema
-- PostgreSQL initialization script

-- Create database extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
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
CREATE TABLE tasks (
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
CREATE TABLE reminders (
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
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

CREATE INDEX idx_tasks_user_datetime ON tasks(user_id, start_datetime);
CREATE INDEX idx_tasks_user_id ON tasks(user_id);
CREATE INDEX idx_tasks_datetime_range ON tasks(start_datetime, end_datetime);

CREATE INDEX idx_reminders_task_id ON reminders(task_id);
CREATE INDEX idx_reminders_time_sent ON reminders(reminder_time, is_sent);
CREATE INDEX idx_reminders_unsent ON reminders(is_sent) WHERE is_sent = FALSE;

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for automatic updated_at updates
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tasks_updated_at 
    BEFORE UPDATE ON tasks 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data for development
INSERT INTO users (username, email, password_hash, first_name, last_name) VALUES 
('demo_user', 'demo@p-cal.me', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Demo', 'User'),
('john_doe', 'john@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'John', 'Doe');

-- Sample tasks (demo data)
INSERT INTO tasks (user_id, title, description, start_datetime, end_datetime, color) VALUES 
(1, 'Meeting con team di sviluppo', 'Discussione roadmap Q4', '2024-09-15 09:00:00', '2024-09-15 10:30:00', '#3788d8'),
(1, 'Presentazione progetto', 'Demo P-Cal al cliente', '2024-09-16 14:00:00', '2024-09-16 15:00:00', '#f59e0b'),
(1, 'Code review', 'Revisione backend API', '2024-09-17 11:00:00', '2024-09-17 12:00:00', '#10b981'),
(2, 'Appuntamento medico', 'Controllo di routine', '2024-09-18 16:00:00', '2024-09-18 17:00:00', '#ef4444');

-- Sample reminders
INSERT INTO reminders (task_id, reminder_time, reminder_offset_minutes) VALUES 
(1, '2024-09-15 08:45:00', 15),
(1, '2024-09-15 08:30:00', 30),
(2, '2024-09-16 13:45:00', 15),
(3, '2024-09-17 10:30:00', 30);

-- Create view for tasks with user information (useful for admin purposes)
CREATE VIEW tasks_with_users AS
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
CREATE VIEW pending_reminders AS
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

-- Grant permissions to the database user (dynamically using current_user)
-- The user is automatically set by PostgreSQL based on POSTGRES_USER env var
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO CURRENT_USER;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO CURRENT_USER;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO CURRENT_USER;