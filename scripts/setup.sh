#!/bin/bash

# PrivateCal v2 Setup Script
# This script sets up the development environment for PrivateCal v2

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check requirements
check_requirements() {
    print_status "Checking system requirements..."
    
    local missing_requirements=()
    
    # Check Docker
    if ! command_exists docker; then
        missing_requirements+=("Docker")
    fi
    
    # Check Docker Compose
    if ! command_exists docker-compose && ! docker compose version >/dev/null 2>&1; then
        missing_requirements+=("Docker Compose")
    fi
    
    # Check Node.js (for local development)
    if ! command_exists node; then
        print_warning "Node.js not found - required for local frontend development"
    else
        NODE_VERSION=$(node --version | cut -d'v' -f2)
        if [ "$(printf '%s\n' "18.0.0" "$NODE_VERSION" | sort -V | head -n1)" = "18.0.0" ]; then
            print_success "Node.js version $NODE_VERSION detected"
        else
            print_warning "Node.js version $NODE_VERSION detected. Recommended: 18.0.0 or higher"
        fi
    fi
    
    # Check Java (for local development)
    if ! command_exists java; then
        print_warning "Java not found - required for local backend development"
    else
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ]; then
            print_success "Java version $JAVA_VERSION detected"
        else
            print_warning "Java version $JAVA_VERSION detected. Required: 17 or higher"
        fi
    fi
    
    if [ ${#missing_requirements[@]} -ne 0 ]; then
        print_error "Missing requirements: ${missing_requirements[*]}"
        print_error "Please install the missing requirements and run this script again."
        exit 1
    fi
    
    print_success "All requirements met!"
}

# Function to setup environment files
setup_environment() {
    print_status "Setting up environment files..."
    
    if [ ! -f ".env" ]; then
        if [ -f ".env.example" ]; then
            cp .env.example .env
            print_success "Created .env file from .env.example"
            print_warning "Please edit .env file with your specific configuration"
        else
            print_error ".env.example not found!"
            exit 1
        fi
    else
        print_success ".env file already exists"
    fi
    
    # Create frontend .env if it doesn't exist
    if [ ! -f "frontend/.env" ]; then
        cat > frontend/.env << EOL
VITE_API_BASE_URL=http://localhost:8080/api
VITE_NTFY_SERVER_URL=https://ntfy.sh
VITE_NTFY_TOPIC_PREFIX=calendar-user-
VITE_APP_NAME=PrivateCal v2
VITE_DEFAULT_THEME=system
EOL
        print_success "Created frontend/.env file"
    else
        print_success "Frontend .env file already exists"
    fi
}

# Function to create necessary directories
create_directories() {
    print_status "Creating necessary directories..."
    
    directories=(
        "logs"
        "data/postgres"
        "data/backups" 
        "ssl"
        "secrets"
        "monitoring"
    )
    
    for dir in "${directories[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            print_success "Created directory: $dir"
        fi
    done
}

# Function to setup database
setup_database() {
    print_status "Setting up database..."
    
    # Check if PostgreSQL container is already running
    if docker ps -q -f name=privatecal-db >/dev/null 2>&1; then
        print_success "Database container already running"
        return
    fi
    
    # Start PostgreSQL container for development
    docker run -d \
        --name privatecal-db-dev \
        -e POSTGRES_DB=calendar_db \
        -e POSTGRES_USER=calendar_user \
        -e POSTGRES_PASSWORD=calendar_pass \
        -p 5432:5432 \
        -v "$(pwd)/database/init.sql:/docker-entrypoint-initdb.d/init.sql:ro" \
        -v "$(pwd)/data/postgres:/var/lib/postgresql/data" \
        postgres:15-alpine
    
    print_success "Database container started"
    print_status "Waiting for database to be ready..."
    
    # Wait for database to be ready
    for i in {1..30}; do
        if docker exec privatecal-db-dev pg_isready -U calendar_user -d calendar_db >/dev/null 2>&1; then
            print_success "Database is ready!"
            break
        fi
        sleep 2
        if [ $i -eq 30 ]; then
            print_error "Database failed to start within 60 seconds"
            exit 1
        fi
    done
}

# Function to setup frontend dependencies
setup_frontend() {
    print_status "Setting up frontend dependencies..."
    
    cd frontend
    
    if [ ! -d "node_modules" ]; then
        print_status "Installing frontend dependencies..."
        npm install
        print_success "Frontend dependencies installed"
    else
        print_success "Frontend dependencies already installed"
    fi
    
    cd ..
}

# Function to setup backend dependencies
setup_backend() {
    print_status "Setting up backend dependencies..."
    
    cd backend
    
    if [ ! -d "target" ]; then
        print_status "Building backend application..."
        ./mvnw clean install -DskipTests
        print_success "Backend application built"
    else
        print_success "Backend already built"
    fi
    
    cd ..
}

# Function to generate SSL certificates for development
generate_ssl_certs() {
    if [ ! -f "ssl/cert.pem" ] || [ ! -f "ssl/key.pem" ]; then
        print_status "Generating SSL certificates for development..."
        
        openssl req -x509 -newkey rsa:4096 -keyout ssl/key.pem -out ssl/cert.pem -days 365 -nodes \
            -subj "/C=IT/ST=Italy/L=Rome/O=PrivateCal/OU=Development/CN=localhost"
        
        print_success "SSL certificates generated"
    else
        print_success "SSL certificates already exist"
    fi
}

# Function to create systemd service files
create_systemd_services() {
    print_status "Creating systemd service files..."
    
    cat > privatecal.service << EOL
[Unit]
Description=PrivateCal v2 Application
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$(pwd)
ExecStart=/usr/bin/docker-compose up -d
ExecStop=/usr/bin/docker-compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOL
    
    print_success "Created privatecal.service file"
    print_status "To install the service, run:"
    print_status "sudo cp privatecal.service /etc/systemd/system/"
    print_status "sudo systemctl daemon-reload"
    print_status "sudo systemctl enable privatecal"
}

# Function to run health checks
run_health_checks() {
    print_status "Running health checks..."
    
    # Check if Docker daemon is running
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker daemon is not running"
        return 1
    fi
    
    # Check if ports are available
    local ports=(80 8080 5432)
    for port in "${ports[@]}"; do
        if netstat -tuln 2>/dev/null | grep ":$port " >/dev/null 2>&1; then
            print_warning "Port $port is already in use"
        fi
    done
    
    print_success "Health checks completed"
}

# Function to display next steps
show_next_steps() {
    print_success "Setup completed successfully!"
    echo
    print_status "Next steps:"
    echo "  1. Edit .env file with your specific configuration"
    echo "  2. Start the application with: docker-compose up -d"
    echo "  3. Access the application at: http://localhost"
    echo "  4. Access the API documentation at: http://localhost:8080/actuator"
    echo
    print_status "For development:"
    echo "  Frontend: cd frontend && npm run dev"
    echo "  Backend: cd backend && ./mvnw spring-boot:run"
    echo
    print_status "For production deployment:"
    echo "  docker-compose -f docker-compose.prod.yml up -d"
    echo
    print_status "Useful commands:"
    echo "  - View logs: docker-compose logs -f"
    echo "  - Stop application: docker-compose down"
    echo "  - Rebuild: docker-compose up --build"
    echo "  - Database backup: ./scripts/backup.sh"
}

# Main setup function
main() {
    echo "=================================================="
    echo "       PrivateCal v2 Setup Script"
    echo "=================================================="
    echo
    
    check_requirements
    setup_environment
    create_directories
    
    # Ask user what they want to setup
    echo
    print_status "What would you like to setup?"
    echo "1) Full Docker setup (recommended)"
    echo "2) Development setup (local frontend + backend)"
    echo "3) Database only"
    echo "4) Production setup"
    
    read -p "Enter your choice (1-4): " choice
    
    case $choice in
        1)
            print_status "Setting up full Docker environment..."
            generate_ssl_certs
            run_health_checks
            docker-compose up --build -d
            print_success "Docker setup complete!"
            ;;
        2)
            print_status "Setting up development environment..."
            setup_database
            setup_frontend
            setup_backend
            print_success "Development setup complete!"
            ;;
        3)
            print_status "Setting up database only..."
            setup_database
            print_success "Database setup complete!"
            ;;
        4)
            print_status "Setting up production environment..."
            generate_ssl_certs
            create_systemd_services
            print_status "Production files created. Please review configuration files."
            ;;
        *)
            print_error "Invalid choice. Exiting."
            exit 1
            ;;
    esac
    
    show_next_steps
}

# Run main function
main "$@"