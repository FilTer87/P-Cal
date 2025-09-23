#!/bin/bash

# PrivateCal Database Migration Runner
# This script automatically runs pending database migrations

set -e

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-calendar_db}"
DB_USER="${DB_USER:-calendar_user}"
DB_PASSWORD="${DB_PASSWORD:-calendar_pass}"
MIGRATIONS_DIR="/docker-entrypoint-initdb.d/migrations"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Wait for database to be ready
wait_for_db() {
    log_info "Waiting for database to be ready..."
    local retries=30
    while [ $retries -gt 0 ]; do
        if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
            log_success "Database is ready!"
            return 0
        fi
        log_info "Database not ready, waiting... ($retries retries left)"
        retries=$((retries - 1))
        sleep 2
    done
    log_error "Database failed to become ready!"
    exit 1
}

# Execute SQL file
execute_sql() {
    local sql_file="$1"
    local description="$2"

    log_info "Executing: $description"
    log_info "SQL file: $sql_file"

    # Check if file exists
    if [ ! -f "$sql_file" ]; then
        log_error "SQL file not found: $sql_file"
        return 1
    fi

    # Execute with verbose error output for debugging
    local error_output
    if error_output=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$sql_file" 2>&1); then
        log_success "✓ $description completed"
        return 0
    else
        log_error "✗ $description failed"
        log_error "Error details: $error_output"
        return 1
    fi
}

# Check if migration was already applied
is_migration_applied() {
    local migration_name="$1"

    local count=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
        "SELECT COUNT(*) FROM schema_migrations WHERE migration_name = '$migration_name' AND success = true;" 2>/dev/null | tr -d ' ')

    [ "$count" = "1" ]
}

# Record migration as applied
record_migration() {
    local migration_name="$1"
    local execution_time="$2"
    local notes="$3"

    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c \
        "INSERT INTO schema_migrations (migration_name, execution_time_ms, notes)
         VALUES ('$migration_name', $execution_time, '$notes')
         ON CONFLICT (migration_name) DO UPDATE SET
         applied_at = CURRENT_TIMESTAMP,
         execution_time_ms = $execution_time,
         success = true,
         notes = '$notes';" >/dev/null 2>&1
}

# Run a single migration
run_migration() {
    local migration_file="$1"
    local migration_name=$(basename "$migration_file" .sql)

    if is_migration_applied "$migration_name"; then
        log_info "Migration $migration_name already applied, skipping"
        return 0
    fi

    log_info "Running migration: $migration_name"

    local start_time=$(date +%s%3N)

    if execute_sql "$migration_file" "Migration $migration_name"; then
        local end_time=$(date +%s%3N)
        local execution_time=$((end_time - start_time))

        record_migration "$migration_name" "$execution_time" "Applied automatically via migration runner"
        log_success "Migration $migration_name completed in ${execution_time}ms"
        return 0
    else
        log_error "Migration $migration_name failed!"
        return 1
    fi
}

# Main migration function
run_migrations() {
    log_info "Starting database migration process..."

    # Wait for database
    wait_for_db

    # Initialize migration system
    log_info "Initializing migration tracking system..."

    # Try different possible locations for migrate.sql
    local migrate_sql_file=""
    for possible_path in "/docker-entrypoint-initdb.d/migrate.sql" "/docker-entrypoint-initdb.d/00-migrate.sql" "migrate.sql"; do
        if [ -f "$possible_path" ]; then
            migrate_sql_file="$possible_path"
            break
        fi
    done

    if [ -z "$migrate_sql_file" ]; then
        log_error "Could not find migrate.sql file"
        exit 1
    fi

    log_info "Using migrate file: $migrate_sql_file"
    if ! execute_sql "$migrate_sql_file" "Migration tracking system setup"; then
        log_error "Failed to initialize migration system!"
        exit 1
    fi

    # Check if migrations directory exists
    if [ ! -d "$MIGRATIONS_DIR" ]; then
        log_warning "Migrations directory not found: $MIGRATIONS_DIR"
        log_info "Creating empty migrations directory..."
        mkdir -p "$MIGRATIONS_DIR"
        return 0
    fi

    # Count available migrations
    local migration_count=$(find "$MIGRATIONS_DIR" -name "*.sql" | wc -l)
    log_info "Found $migration_count migration(s) to process"

    if [ "$migration_count" -eq 0 ]; then
        log_info "No migrations to run"
        return 0
    fi

    # Run migrations in order
    local success_count=0
    local failed_count=0

    for migration_file in $(find "$MIGRATIONS_DIR" -name "*.sql" | sort); do
        if run_migration "$migration_file"; then
            success_count=$((success_count + 1))
        else
            failed_count=$((failed_count + 1))
            log_error "Migration failed, stopping execution"
            break
        fi
    done

    # Summary
    log_info "Migration process completed"
    log_success "Successful migrations: $success_count"
    if [ "$failed_count" -gt 0 ]; then
        log_error "Failed migrations: $failed_count"
        exit 1
    fi

    log_success "All migrations completed successfully!"
}

# Script entry point
main() {
    log_info "PrivateCal Database Migration Runner"
    log_info "=================================="

    # Check if running in init mode (database initialization)
    if [ "${POSTGRES_INIT_MODE:-false}" = "true" ]; then
        log_info "Running in PostgreSQL initialization mode"
        # In init mode, just run the base schema
        if [ -f "/docker-entrypoint-initdb.d/init.sql" ]; then
            execute_sql "/docker-entrypoint-initdb.d/init.sql" "Initial schema setup"
        fi
    else
        # Normal migration mode
        run_migrations
    fi
}

# Run main function
main "$@"