# P-Cal Installation Guide

## Requirements

- Git
- Docker / Podman
- Docker Compose


## Quick Start

Get P-Cal up and running in 3 minutes:

```bash
# 1. Clone the repository
git clone https://github.com/FilTer87/P-Cal.git
cd p-cal

# 2. Create and configure environment file
cp .env.example .env
nano .env  # Edit at least JWT_SECRET, DATABASE_USERNAME, and DATABASE_PASSWORD

# 3. Start the application
docker compose up --build -d

# 4. Access the application
# Frontend: http://localhost
# Backend API: http://localhost:8080/api
```

That's it! The application will automatically:
- Create the database with initial schema
- Run all migrations
- Start the backend API
- Serve the frontend

Default test user (created automatically):
- Username: `demo_user` / Password: `password`

---

## Environment Configuration

### Configuration File

Copy `.env.example` to `.env` and configure the following variables:

```bash
cp .env.example .env
```

### Required Variables

**Security & Database (MUST be changed for production):**

| Variable | Description | Example |
|----------|-------------|---------|
| `JWT_SECRET` | Secret key for JWT token generation. Use a strong random string (32+ chars) | `your-super-secret-jwt-key-change-me-in-production` |
| `DATABASE_USERNAME` | PostgreSQL database username | `pcal_user` |
| `DATABASE_PASSWORD` | PostgreSQL database password | `secure_password_123` |

**Notifications (Required for reminders to work):**

Configure **at least one** notification method:

**Email notifications:**
| Variable | Description | Example |
|----------|-------------|---------|
| `EMAIL_ENABLED` | Enable email functionality | `true` |
| `EMAIL_VERIFICATION` | Require email verification after registration | `false` |
| `MAIL_HOST` | SMTP server hostname | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP server port | `587` |
| `MAIL_USERNAME` | SMTP username (email address) | `your.email@gmail.com` |
| `MAIL_PASSWORD` | SMTP password | Use [App Password](https://support.google.com/accounts/answer/185833) for Gmail |
| `EMAIL_FROM_ADDRESS` | From email address | `noreply@p-cal.me` |

**OR Push notifications (ntfy):**
| Variable | Description | Example |
|----------|-------------|---------|
| `NTFY_ENABLED` | Enable push notifications | `true` |
| `NTFY_SERVER_URL` | Ntfy server for push notifications | `https://ntfy.sh` (public) or your self-hosted instance |
| `NTFY_AUTH_TOKEN` | Authentication token | Optional, only for custom servers with auth |

### Recommended Variables

**Should be configured for your environment:**

| Variable | Description | Default | Notes |
|----------|-------------|---------|-------|
| `CORS_ALLOWED_ORIGINS` | Allowed origins for CORS | `http://localhost*` | Add your domain(s): `https://yourdomain.com,https://*.yourdomain.com` |
| `APP_BASE_URL` | Application public URL | `http://localhost` | Used in email links. Change to `https://yourdomain.com` in production |

### Optional Variables

**These can be customized if needed:**

| Variable | Description | Default | Notes |
|----------|-------------|---------|-------|
| `NTFY_TOPIC_PREFIX` | Prefix for ntfy topics | `p-cal-` | Customize if running multiple instances |
| `VITE_APP_NAME` | Application name | `P-Cal` | Shown in browser and emails |
| `VITE_DEFAULT_THEME` | Default theme | `system` | Options: `light`, `dark`, `system` |

### Advanced Variables (Leave as default)

**Do not change unless you know what you're doing:**

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | Database connection URL | `jdbc:postgresql://privatecal-db:5432/calendar_db` |
| `DATABASE_NAME` | Database name | `calendar_db` |
| `SPRING_PROFILES_ACTIVE` | Spring Boot profile | `docker` |
| `SERVER_PORT` | Backend server port | `8080` |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token lifetime (ms) | `900000` (15 min) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token lifetime (ms) | `604800000` (7 days) |
| `VITE_API_URL` | Frontend API URL | `/api` |

---

### Production Deployment

#### Using a Reverse Proxy (Recommended)

If you're deploying behind Nginx, Traefik, or Nginx Proxy Manager:

1. **Configure CORS origins** with your domain:
   ```bash
   CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://*.yourdomain.com
   ```

2. **Set the public URL**:
   ```bash
   APP_BASE_URL=https://yourdomain.com
   ```

3. **Use strong credentials**:
   ```bash
   JWT_SECRET=$(openssl rand -base64 32)
   DATABASE_PASSWORD=$(openssl rand -base64 24)
   ```

4. **Ensure your reverse proxy passes these headers**:
   ```nginx
   proxy_set_header Host $host;
   proxy_set_header X-Real-IP $remote_addr;
   proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
   proxy_set_header X-Forwarded-Proto $scheme;
   ```

#### Email Configuration with Gmail

1. Enable 2-Factor Authentication on your Gmail account
2. Go to: [Google Account](https://myaccount.google.com) → Security → 2-Step Verification → App passwords
3. Generate an app password for "Mail"
4. Configure in `.env`:
   ```bash
   EMAIL_ENABLED=true
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your.email@gmail.com
   MAIL_PASSWORD=your-16-char-app-password
   EMAIL_FROM_ADDRESS=your.email@gmail.com
   ```

#### NTFY server Configuration
P-Cal uses [NTFY](https://github.com/binwiederhier/ntfy), an easy to use, functional and self-hostable push-notification service.

**Configuration example:**
```bash
NTFY_SERVER_URL=https://your-ntfy-server.com
NTFY_TOPIC_PREFIX=your-app-prefix-
NTFY_AUTH_TOKEN=your-auth-token  # Optional for hosted servers with auth
```

See [NTFY Documentation](https://docs.ntfy.sh/) for server setup and details

---

### Troubleshooting

#### Database connection failed
- Ensure `DATABASE_USERNAME` and `DATABASE_PASSWORD` match in `.env`
- Run `docker compose down -v && docker compose up --build -d` to recreate the database

#### CORS errors in browser
- Add your domain to `CORS_ALLOWED_ORIGINS`
- Use patterns like `https://*.yourdomain.com` for subdomains
- Restart backend: `docker compose restart backend`

#### Email not sending
- For Gmail, ensure you're using an App Password, not your regular password
- Check `EMAIL_ENABLED=true` in `.env`
- Verify SMTP settings with your email provider

#### Frontend shows "Connection refused"
- Check that `VITE_API_URL=/api` (relative path for Docker deployment)
- Ensure backend is healthy: `docker compose ps`

---

### Managing the Application

```bash
# View logs
docker compose logs -f

# Restart services
docker compose restart

# Stop application
docker compose down

# Update to latest version
git pull
docker compose down
docker compose up --build -d

# Backup database
docker exec privatecal-db pg_dump -U <your_db_user> -d calendar_db > backup.sql

# Restore database
cat backup.sql | docker exec -i privatecal-db psql -U <your_db_user> -d calendar_db
```

---

### Support

For issues and questions:
- GitHub Issues: [Report a bug](https://github.com/FilTer87/P-Cal/issues)
- Full Documentation: Work In Progress
