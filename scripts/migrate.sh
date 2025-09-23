#!/bin/bash

# PrivateCal Migration Management Script
# Easy script to manage database migrations with Docker Compose

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
COMPOSE_FILE="docker-compose.yml"
DEV_COMPOSE_FILE="docker-compose.dev.yml"
DB_CONTAINER="privatecal-db"
MIGRATOR_CONTAINER="privatecal-migrator"

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Show usage
usage() {
    echo "PrivateCal Database Migration Management"
    echo "========================================"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  migrate [env]     Run pending migrations (env: dev|prod, default: dev)"
    echo "  status [env]      Show migration status"
    echo "  create <name>     Create new migration file"
    echo "  rollback [env]    Rollback last migration (manual)"
    echo "  reset [env]       Reset database (DANGER: destroys all data)"
    echo "  logs [env]        Show migration logs"
    echo ""
    echo "Examples:"
    echo "  $0 migrate              # Run migrations in development"
    echo "  $0 migrate prod         # Run migrations in production"
    echo "  $0 status               # Check migration status"
    echo "  $0 create add_user_settings  # Create new migration"
    echo "  $0 reset dev            # Reset development database"
    echo ""
}

# Determine environment and compose file
get_env_config() {
    local env=${1:-dev}

    if [ "$env" = "prod" ]; then
        COMPOSE_FILE="docker-compose.prod.yml"
        DB_CONTAINER="privatecal-db-prod"
        MIGRATOR_CONTAINER="privatecal-migrator-prod"
    elif [ "$env" = "dev" ]; then
        COMPOSE_FILE="docker-compose.dev.yml"
        DB_CONTAINER="privatecal-db-dev"
        MIGRATOR_CONTAINER="privatecal-migrator-dev"
    else
        COMPOSE_FILE="docker-compose.yml"
        DB_CONTAINER="privatecal-db"
        MIGRATOR_CONTAINER="privatecal-migrator"
    fi

    log_info "Using environment: $env"
    log_info "Using compose file: $COMPOSE_FILE"
}

# Check if Docker Compose is available
check_docker() {
    if ! command -v docker-compose >/dev/null 2>&1; then
        log_error "docker-compose is not installed or not in PATH"
        exit 1
    fi

    if ! docker info >/dev/null 2>&1; then
        log_error "Docker is not running or not accessible"
        exit 1
    fi
}

# Wait for database container to be healthy
wait_for_db() {
    local max_attempts=30
    local attempt=1

    log_info "Waiting for database container to be healthy..."

    while [ $attempt -le $max_attempts ]; do
        if docker-compose -f "$COMPOSE_FILE" ps "$DB_CONTAINER" | grep -q "healthy"; then
            log_success "Database container is healthy!"
            return 0
        fi

        log_info "Attempt $attempt/$max_attempts - waiting for database..."
        sleep 2
        attempt=$((attempt + 1))
    done

    log_error "Database container failed to become healthy!"
    return 1
}

# Run migrations
run_migrations() {
    local env=${1:-dev}
    get_env_config "$env"

    log_info "Running database migrations..."

    # Start database if not running
    if ! docker-compose -f "$COMPOSE_FILE" ps "$DB_CONTAINER" | grep -q "Up"; then
        log_info "Starting database container..."
        docker-compose -f "$COMPOSE_FILE" up -d database
    fi

    # Wait for database to be ready
    wait_for_db

    # Run migrations using the migrator service
    if [ -f "$COMPOSE_FILE" ] && docker-compose -f "$COMPOSE_FILE" config | grep -q "migrator"; then
        log_info "Running migrations via migrator service..."
        docker-compose -f "$COMPOSE_FILE" run --rm migrator
    else
        # Fallback: run migrations directly in database container
        log_info "Running migrations directly in database container..."
        docker-compose -f "$COMPOSE_FILE" exec "$DB_CONTAINER" /usr/local/bin/run_migrations.sh
    fi

    log_success "Migrations completed!"
}

# Show migration status
show_status() {
    local env=${1:-dev}
    get_env_config "$env"

    log_info "Checking migration status..."

    if ! docker-compose -f "$COMPOSE_FILE" ps "$DB_CONTAINER" | grep -q "healthy"; then
        log_warning "Database container is not running or not healthy"
        log_info "Starting database..."
        docker-compose -f "$COMPOSE_FILE" up -d database
        wait_for_db
    fi

    log_info "Migration history:"
    docker-compose -f "$COMPOSE_FILE" exec -T "$DB_CONTAINER" psql -U calendar_user -d calendar_db -c \
        "SELECT migration_name, applied_at, execution_time_ms, success FROM schema_migrations ORDER BY applied_at DESC LIMIT 10;" \
        2>/dev/null || log_warning "Could not retrieve migration status (table may not exist yet)"
}

# Create new migration
create_migration() {
    local name="$1"

    if [ -z "$name" ]; then
        log_error "Migration name is required"
        echo "Usage: $0 create <migration_name>"
        exit 1
    fi

    # Generate migration number
    local migration_count=$(find database/migrations -name "*.sql" 2>/dev/null | wc -l)
    local migration_number=$(printf "%03d" $((migration_count + 1)))
    local migration_filename="${migration_number}_${name}.sql"
    local migration_path="database/migrations/$migration_filename"

    log_info "Creating new migration: $migration_filename"

    # Create migration file
    cat > "$migration_path" << EOF
-- Migration: $migration_number - $name
-- Description: Add description here
-- Applied: $(date)
-- Author: $(whoami)

-- Add your migration SQL here
-- Example:
-- ALTER TABLE users ADD COLUMN new_field VARCHAR(255);

-- Remember to:
-- 1. Use IF NOT EXISTS for CREATE statements
-- 2. Check if columns/constraints already exist before adding
-- 3. Include rollback instructions in comments if needed

EOF

    log_success "Migration created: $migration_path"
    log_info "Edit the file to add your migration SQL"
}

# Reset database (DANGER)
reset_database() {
    local env=${1:-dev}
    get_env_config "$env"

    log_warning "🚨 DATABASE RESET WARNING 🚨"
    log_warning "This will completely destroy all data in the $env database!"
    log_warning "This action cannot be undone!"
    echo ""
    read -p "Type 'YES I UNDERSTAND' to continue: " confirmation

    if [ "$confirmation" != "YES I UNDERSTAND" ]; then
        log_info "Reset cancelled"
        exit 0
    fi

    log_info "Stopping containers..."
    docker-compose -f "$COMPOSE_FILE" down

    log_info "Removing database volume..."
    if [ "$env" = "prod" ]; then
        docker volume rm privatecal_postgres_data_prod 2>/dev/null || true
    elif [ "$env" = "dev" ]; then
        docker volume rm privatecal_postgres_data_dev 2>/dev/null || true
    else
        docker volume rm privatecal_postgres_data 2>/dev/null || true
    fi

    log_info "Starting fresh database..."
    docker-compose -f "$COMPOSE_FILE" up -d database

    wait_for_db

    log_success "Database reset completed!"
    log_info "Migrations will run automatically on next startup"
}

# Show logs
show_logs() {
    local env=${1:-dev}
    get_env_config "$env"

    log_info "Showing database logs..."
    docker-compose -f "$COMPOSE_FILE" logs -f "$DB_CONTAINER"
}

# Main function
main() {
    local command="$1"
    shift || true

    check_docker

    case "$command" in
        migrate)
            run_migrations "$@"
            ;;
        status)
            show_status "$@"
            ;;
        create)
            create_migration "$@"
            ;;
        reset)
            reset_database "$@"
            ;;
        logs)
            show_logs "$@"
            ;;
        rollback)
            log_warning "Rollback is not automated. Please create a new migration to undo changes."
            log_info "Use: $0 create rollback_migration_name"
            ;;
        help|--help|-h)
            usage
            ;;
        "")
            log_error "No command specified"
            usage
            exit 1
            ;;
        *)
            log_error "Unknown command: $command"
            usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"