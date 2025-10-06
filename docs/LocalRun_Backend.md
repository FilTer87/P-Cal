# Running Backend Locally

This guide explains how to run the P-Cal backend on your local machine without Docker.

## Prerequisites

- **Java 17+** installed
- **Maven 3.6+** installed
- **PostgreSQL** running locally on port `5432`
- Database `calendar_db` created with user credentials

## Setup (First Time Only)

### 1. Configure Application Profile

Copy the example configuration and customize it:

```bash
cd backend/src/main/resources
cp application-local.example.yml application-local.yml
```

Edit `application-local.yml` with your credentials:
- Database connection (username, password)
- SMTP settings (Gmail app password recommended)
- NTFY auth token (optional, for push notifications)
- JWT secret (change for production)

### 2. Prepare Database

Make sure PostgreSQL is running and the database exists:

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database and user
CREATE DATABASE calendar_db;
CREATE USER pcal_user WITH PASSWORD 'pcal_pass';
GRANT ALL PRIVILEGES ON DATABASE calendar_db TO pcal_user;
```

Run migrations (if not already applied):

```bash
# From project root
psql -U pcal_user -d calendar_db -f database/migrations/V1__initial_schema.sql
# ... apply other migration files in order
```

## Running the Backend

### Option 1: Maven Spring Boot Plugin

```bash
# From backend directory
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

### Option 2: Packaged JAR

```bash
# Build the application
cd backend
mvn clean package -DskipTests

# Run the JAR
SPRING_PROFILES_ACTIVE=local java -jar target/backend-*.jar
```

### Option 3: Auto-activate Profile (Recommended)

Create `backend/.mvn/jvm.config` with:

```
-Dspring.profiles.active=local
```

Then simply run:

```bash
cd backend
mvn spring-boot:run
```

## Verify Backend is Running

- Health check: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- API base URL: http://localhost:8080/api

## Common Issues

**Database connection refused**
- Verify PostgreSQL is running: `systemctl status postgresql` (Linux) or check Services (Windows)
- Check port 5432 is not blocked by firewall

**Email sending fails**
- For Gmail: enable 2FA and create an [App Password](https://myaccount.google.com/apppasswords)
- Set `EMAIL_ENABLED=false` in config to disable email features

**Port 8080 already in use**
- Check if another instance is running: `lsof -i :8080` (Unix) or `netstat -ano | findstr :8080` (Windows)
- Change port in `application-local.yml`: `server.port: 8081`

## Environment Variables Override

You can override any configuration via environment variables:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/other_db \
DATABASE_USERNAME=other_user \
SPRING_PROFILES_ACTIVE=local \
mvn spring-boot:run
```

## Running Tests

```bash
cd backend
mvn test
```

Tests use an in-memory H2 database (no PostgreSQL required).

## Stopping the Backend

Press `Ctrl+C` in the terminal where the backend is running.
