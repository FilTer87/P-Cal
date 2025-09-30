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

-- Grant permissions to the database user (dynamically using current_user)
-- The user is automatically set by PostgreSQL based on POSTGRES_USER env var
GRANT SELECT, INSERT, UPDATE ON schema_migrations TO CURRENT_USER;
GRANT USAGE, SELECT ON SEQUENCE schema_migrations_id_seq TO CURRENT_USER;

\echo 'Migration tracking system setup completed successfully'