#!/bin/bash

# Custom PostgreSQL entrypoint with migration support
# This script extends the standard PostgreSQL entrypoint to support migrations

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INIT]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Check if we need to run database initialization
should_init_db() {
    [ -z "$(ls -A "$PGDATA" 2>/dev/null)" ]
}

# Run the standard PostgreSQL entrypoint
run_postgres_entrypoint() {
    exec docker-entrypoint.sh "$@"
}

# Post-initialization hook to run migrations
post_init_migrations() {
    if [ "${ENABLE_MIGRATIONS:-true}" = "true" ]; then
        log_info "Post-initialization: Running database migrations..."

        # Wait a bit for PostgreSQL to be fully ready
        sleep 6

        # Set environment variables for migration script
        export DB_HOST="localhost"
        export DB_PORT="${POSTGRES_PORT:-5432}"
        export DB_NAME="${POSTGRES_DB}"
        export DB_USER="${POSTGRES_USER}"
        export DB_PASSWORD="${POSTGRES_PASSWORD}"

        # Run migrations in background after PostgreSQL is ready
        # Don't run in background (&) to ensure migrations complete
        /usr/local/bin/run_migrations.sh

        log_success "Post-initialization migrations completed"
    else
        log_info "Migrations disabled via ENABLE_MIGRATIONS=false"
    fi
}

# Main entrypoint logic
main() {
    log_info "PrivateCal Database Container Starting..."

    # Check if this is first-time initialization
    if should_init_db; then
        log_info "First-time database initialization detected"
        export POSTGRES_INIT_MODE="true"

        # Run standard PostgreSQL initialization
        # Use async approach to run migrations after init
        {
            # Wait for PostgreSQL to be ready after initialization
            sleep 20
            post_init_migrations
        } &

        MIGRATION_PID=$!

        # Start PostgreSQL with standard entrypoint
        run_postgres_entrypoint "$@"
    else
        log_info "Database already initialized, checking for pending migrations..."

        # Start PostgreSQL in background
        run_postgres_entrypoint "$@" &
        POSTGRES_PID=$!

        # Wait for PostgreSQL to be ready
        sleep 6

        # Run migrations
        if [ "${ENABLE_MIGRATIONS:-true}" = "true" ]; then
            export DB_HOST="localhost"
            export DB_PORT="${POSTGRES_PORT:-5432}"
            export DB_NAME="${POSTGRES_DB}"
            export DB_USER="${POSTGRES_USER}"
            export DB_PASSWORD="${POSTGRES_PASSWORD}"

            /usr/local/bin/run_migrations.sh
        fi

        # Keep PostgreSQL running
        wait $POSTGRES_PID
    fi
}

# Execute main function
main "$@"