#!/bin/bash

# P-Cal Database Backup Script
# This script creates backups of the PostgreSQL database

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Configuration (from environment variables or defaults)
DB_CONTAINER="${DB_CONTAINER:-privatecal-db}"
DB_NAME="${DB_NAME:-${DATABASE_NAME:-calendar_db}}"
DB_USER="${DB_USER:-${DATABASE_USERNAME:-calendar_user}}"
BACKUP_DIR="./data/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="privatecal_backup_${TIMESTAMP}.sql"

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Function to create backup
create_backup() {
    print_status "Creating database backup..."
    
    if ! docker ps -q -f name="$DB_CONTAINER" >/dev/null 2>&1; then
        print_error "Database container '$DB_CONTAINER' is not running!"
        exit 1
    fi
    
    # Create the backup
    docker exec "$DB_CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" > "$BACKUP_DIR/$BACKUP_FILE"
    
    if [ $? -eq 0 ]; then
        print_success "Backup created: $BACKUP_DIR/$BACKUP_FILE"
        
        # Compress the backup
        gzip "$BACKUP_DIR/$BACKUP_FILE"
        print_success "Backup compressed: $BACKUP_DIR/$BACKUP_FILE.gz"
        
        # Show backup size
        BACKUP_SIZE=$(du -h "$BACKUP_DIR/$BACKUP_FILE.gz" | cut -f1)
        print_status "Backup size: $BACKUP_SIZE"
    else
        print_error "Backup failed!"
        exit 1
    fi
}

# Function to restore backup
restore_backup() {
    local backup_file="$1"
    
    if [ -z "$backup_file" ]; then
        print_error "Please specify backup file to restore"
        echo "Usage: $0 restore <backup_file>"
        exit 1
    fi
    
    if [ ! -f "$backup_file" ]; then
        print_error "Backup file not found: $backup_file"
        exit 1
    fi
    
    print_status "Restoring database from: $backup_file"
    
    # Check if file is compressed
    if [[ "$backup_file" == *.gz ]]; then
        print_status "Decompressing backup file..."
        zcat "$backup_file" | docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME"
    else
        cat "$backup_file" | docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME"
    fi
    
    if [ $? -eq 0 ]; then
        print_success "Database restored successfully!"
    else
        print_error "Restore failed!"
        exit 1
    fi
}

# Function to list backups
list_backups() {
    print_status "Available backups:"
    ls -lah "$BACKUP_DIR"/privatecal_backup_*.sql* 2>/dev/null || print_status "No backups found"
}

# Function to cleanup old backups
cleanup_old_backups() {
    local keep_days=${1:-7}
    
    print_status "Cleaning up backups older than $keep_days days..."
    
    find "$BACKUP_DIR" -name "privatecal_backup_*.sql*" -type f -mtime +$keep_days -delete
    
    print_success "Cleanup completed"
}

# Function to validate database
validate_database() {
    print_status "Validating database..."
    
    # Check if container is running
    if ! docker ps -q -f name="$DB_CONTAINER" >/dev/null 2>&1; then
        print_error "Database container is not running!"
        return 1
    fi
    
    # Check database connectivity
    if docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
        print_success "Database is accessible"
    else
        print_error "Cannot connect to database!"
        return 1
    fi
    
    # Check table count
    TABLE_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';" | xargs)
    print_status "Tables in database: $TABLE_COUNT"
    
    # Check user count
    USER_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT count(*) FROM users;" | xargs)
    print_status "Users in database: $USER_COUNT"
    
    # Check task count
    TASK_COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT count(*) FROM tasks;" | xargs)
    print_status "Tasks in database: $TASK_COUNT"
    
    print_success "Database validation completed"
}

# Function to show database statistics
show_stats() {
    print_status "Database Statistics:"
    
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "
    SELECT 
        'Users' as table_name, 
        count(*) as record_count,
        pg_size_pretty(pg_total_relation_size('users')) as size
    FROM users
    UNION ALL
    SELECT 
        'Tasks' as table_name, 
        count(*) as record_count,
        pg_size_pretty(pg_total_relation_size('tasks')) as size
    FROM tasks
    UNION ALL
    SELECT 
        'Reminders' as table_name, 
        count(*) as record_count,
        pg_size_pretty(pg_total_relation_size('reminders')) as size
    FROM reminders
    ORDER BY table_name;
    "
    
    print_status "Database Size:"
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT pg_size_pretty(pg_database_size('$DB_NAME'));"
}

# Function to export data as JSON
export_json() {
    local output_dir="./data/exports"
    mkdir -p "$output_dir"
    
    print_status "Exporting data as JSON..."
    
    # Export users (without passwords)
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "COPY (SELECT row_to_json(t) FROM (SELECT id, username, email, first_name, last_name, timezone, created_at, updated_at FROM users) t) TO STDOUT;" > "$output_dir/users_${TIMESTAMP}.json"
    
    # Export tasks
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "COPY (SELECT row_to_json(t) FROM (SELECT * FROM tasks) t) TO STDOUT;" > "$output_dir/tasks_${TIMESTAMP}.json"
    
    # Export reminders
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "COPY (SELECT row_to_json(t) FROM (SELECT * FROM reminders) t) TO STDOUT;" > "$output_dir/reminders_${TIMESTAMP}.json"
    
    print_success "JSON export completed in $output_dir"
}

# Main function
main() {
    case "$1" in
        "backup"|"")
            create_backup
            ;;
        "restore")
            restore_backup "$2"
            ;;
        "list")
            list_backups
            ;;
        "cleanup")
            cleanup_old_backups "$2"
            ;;
        "validate")
            validate_database
            ;;
        "stats")
            show_stats
            ;;
        "export-json")
            export_json
            ;;
        "help")
            echo "P-Cal Database Backup Script"
            echo ""
            echo "Usage: $0 [command] [options]"
            echo ""
            echo "Commands:"
            echo "  backup           Create a new backup (default)"
            echo "  restore <file>   Restore from backup file"
            echo "  list             List available backups"
            echo "  cleanup [days]   Remove backups older than N days (default: 7)"
            echo "  validate         Check database connectivity and basic stats"
            echo "  stats            Show detailed database statistics"
            echo "  export-json      Export data as JSON files"
            echo "  help             Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                           # Create backup"
            echo "  $0 restore backup.sql.gz     # Restore from backup"
            echo "  $0 cleanup 30                # Keep only last 30 days"
            ;;
        *)
            print_error "Unknown command: $1"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Check if docker is available
if ! command -v docker >/dev/null 2>&1; then
    print_error "Docker is not installed or not in PATH"
    exit 1
fi

# Run main function
main "$@"