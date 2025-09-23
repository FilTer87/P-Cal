-- Database Migration System for PrivateCal
-- This script creates the migration tracking system and runs pending migrations

\echo 'Setting up migration tracking system...'

-- Create migration tracking table if it doesn't exist
CREATE TABLE IF NOT EXISTS schema_migrations (
    id SERIAL PRIMARY KEY,
    migration_name VARCHAR(255) NOT NULL UNIQUE,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    checksum VARCHAR(64),
    success BOOLEAN DEFAULT TRUE,
    execution_time_ms INTEGER,
    notes TEXT
);

\echo 'Migration table created successfully'

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_schema_migrations_name ON schema_migrations(migration_name);
CREATE INDEX IF NOT EXISTS idx_schema_migrations_applied ON schema_migrations(applied_at);

\echo 'Migration indexes created successfully'

-- Comment on table
COMMENT ON TABLE schema_migrations IS 'Tracks database migrations for PrivateCal application';

-- Grant permissions only if user exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'calendar_user') THEN
        GRANT SELECT, INSERT, UPDATE ON schema_migrations TO calendar_user;
        GRANT USAGE, SELECT ON SEQUENCE schema_migrations_id_seq TO calendar_user;
        RAISE NOTICE 'Permissions granted to calendar_user';
    ELSE
        RAISE NOTICE 'User calendar_user does not exist, skipping permissions';
    END IF;
END $$;

\echo 'Migration tracking system setup completed successfully'