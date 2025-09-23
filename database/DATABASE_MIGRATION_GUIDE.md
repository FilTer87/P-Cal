# 🐳 Sistema di Migrazione Database - Guida Completa

## 📋 **Panoramica**

Sistema automatico di migrazione database per PrivateCal che gestisce:
- ✅ **Migrazioni automatiche** al startup di Docker Compose
- ✅ **Tracking delle migrazioni** per evitare duplicazioni
- ✅ **Script di gestione** per operazioni manuali
- ✅ **Supporto dev/prod** con configurazioni separate
- ✅ **Rollback sicuro** e gestione errori

## 🏗️ **Architettura del Sistema**

```
database/
├── Dockerfile              # Custom PostgreSQL con sistema migrazione
├── docker-entrypoint.sh    # Entrypoint personalizzato
├── migrate.sql             # Setup tabella tracking migrazioni
├── run_migrations.sh       # Script esecuzione migrazioni
├── init.sql                # Schema iniziale (fallback)
└── migrations/             # Directory migrazioni
    ├── 001_initial_schema.sql
    ├── 002_add_user_preferences.sql
    └── ...

scripts/
└── migrate.sh              # Script di gestione utente
```

## 🚀 **Come Usare il Sistema**

### **1. Avvio Standard (Automatico)**

```bash
# Development (migrazioni automatiche)
docker-compose -f docker-compose.dev.yml up -d

# Production (migrazioni automatiche)
docker-compose -f docker-compose.prod.yml up -d

# Standard (migrazioni automatiche)
docker-compose up -d
```

**✨ Le migrazioni vengono eseguite automaticamente!**

### **2. Gestione Manuale delle Migrazioni**

```bash
# Rendere eseguibile lo script (solo la prima volta)
chmod +x scripts/migrate.sh

# Eseguire migrazioni manuali
./scripts/migrate.sh migrate          # Development
./scripts/migrate.sh migrate prod     # Production

# Verificare stato migrazioni
./scripts/migrate.sh status
./scripts/migrate.sh status prod

# Creare nuova migrazione
./scripts/migrate.sh create add_new_feature

# Reset database (ATTENZIONE: cancella tutto!)
./scripts/migrate.sh reset dev

# Vedere logs
./scripts/migrate.sh logs
```

### **3. Workflow Sviluppo**

```bash
# 1. Sviluppare nuova feature che richiede modifica DB
./scripts/migrate.sh create add_user_avatar

# 2. Editare il file creato in database/migrations/
# vim database/migrations/003_add_user_avatar.sql

# 3. Testare la migrazione
./scripts/migrate.sh migrate dev

# 4. Verificare che sia stata applicata
./scripts/migrate.sh status dev

# 5. Committare la migrazione nel repo
git add database/migrations/003_add_user_avatar.sql
git commit -m "Add user avatar support"
```

## 📁 **Struttura File di Migrazione**

### **Convenzioni Nomenclatura**

```
001_initial_schema.sql           # Schema iniziale
002_add_user_preferences.sql     # Preferenze utente
003_add_user_avatar.sql          # Avatar utente
004_create_notifications.sql     # Sistema notifiche
005_rollback_notifications.sql   # Rollback (se necessario)
```

### **Template Migrazione**

```sql
-- Migration: 003 - Add User Avatar
-- Description: Adds avatar support for user profiles
-- Applied: 2024-01-15
-- Author: Developer Name

-- Add avatar column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_uploaded_at TIMESTAMP;

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_users_avatar ON users(avatar_url) WHERE avatar_url IS NOT NULL;

-- Add constraint for valid URLs
ALTER TABLE users ADD CONSTRAINT IF NOT EXISTS check_avatar_url_valid
    CHECK (avatar_url IS NULL OR avatar_url ~ '^https?://');

-- Update existing users with default avatar
UPDATE users SET avatar_url = NULL WHERE avatar_url IS NULL;

-- Comments
COMMENT ON COLUMN users.avatar_url IS 'URL of user profile avatar image';
COMMENT ON COLUMN users.avatar_uploaded_at IS 'Timestamp when avatar was last updated';
```

## 🔧 **Configurazioni Docker Compose**

### **Development (docker-compose.dev.yml)**

```yaml
database:
  build:
    context: ./database
  environment:
    ENABLE_MIGRATIONS: "true"
  volumes:
    - ./database/migrations:/docker-entrypoint-initdb.d/migrations:ro
```

### **Production (docker-compose.prod.yml)**

```yaml
database:
  build:
    context: ./database
  environment:
    ENABLE_MIGRATIONS: "true"
    POSTGRES_DB: ${POSTGRES_DB}
    POSTGRES_USER: ${POSTGRES_USER}
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

### **Disabilitare Migrazioni (se necessario)**

```yaml
database:
  environment:
    ENABLE_MIGRATIONS: "false"  # Disabilita migrazioni automatiche
```

## 📊 **Tracking delle Migrazioni**

Il sistema crea automaticamente la tabella `schema_migrations`:

```sql
CREATE TABLE schema_migrations (
    id SERIAL PRIMARY KEY,
    migration_name VARCHAR(255) NOT NULL UNIQUE,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    checksum VARCHAR(64),
    success BOOLEAN DEFAULT TRUE,
    execution_time_ms INTEGER,
    notes TEXT
);
```

### **Query Utili**

```sql
-- Vedere tutte le migrazioni applicate
SELECT * FROM schema_migrations ORDER BY applied_at DESC;

-- Vedere migrazioni fallite
SELECT * FROM schema_migrations WHERE success = FALSE;

-- Vedere ultima migrazione
SELECT * FROM schema_migrations ORDER BY applied_at DESC LIMIT 1;

-- Verificare se migrazione specifica è applicata
SELECT EXISTS(
    SELECT 1 FROM schema_migrations
    WHERE migration_name = '002_add_user_preferences'
    AND success = TRUE
);
```

## ⚠️ **Best Practices**

### **✅ Cosa Fare**

1. **Sempre usare IF NOT EXISTS** per CREATE statements
2. **Verificare esistenza** prima di aggiungere colonne/constraint
3. **Includere rollback instructions** nei commenti
4. **Testare in development** prima di applicare in production
5. **Committare migrazioni** insieme al codice che le richiede
6. **Documentare bene** le modifiche nei commenti

### **❌ Cosa NON Fare**

1. **Non modificare** migrazioni già applicate
2. **Non eliminare** file di migrazione dal repo
3. **Non fare** modifiche distruttive senza backup
4. **Non applicare** migrazioni direttamente in production senza test
5. **Non rimuovere** la tabella `schema_migrations`

### **🔒 Gestione Rollback**

Il sistema non ha rollback automatico. Per annullare una migrazione:

```bash
# Creare migrazione di rollback
./scripts/migrate.sh create rollback_user_avatar

# Editare il file per annullare le modifiche
vim database/migrations/004_rollback_user_avatar.sql
```

Esempio rollback:
```sql
-- Migration: 004 - Rollback User Avatar
-- Description: Removes avatar support added in migration 003
-- Applied: 2024-01-16
-- Author: Developer Name

-- Remove avatar columns
ALTER TABLE users DROP COLUMN IF EXISTS avatar_url CASCADE;
ALTER TABLE users DROP COLUMN IF EXISTS avatar_uploaded_at CASCADE;

-- Remove related indexes (automatically dropped with columns)
-- Remove constraints (automatically dropped with columns)
```

## 🐛 **Troubleshooting**

### **Errore: "Migration already applied"**

```bash
# Verificare stato
./scripts/migrate.sh status

# Se necessario, rimuovere dalla tabella tracking
docker-compose exec database psql -U calendar_user -d calendar_db -c \
  "DELETE FROM schema_migrations WHERE migration_name = 'problematic_migration';"
```

### **Errore: "Database not ready"**

```bash
# Verificare salute container
docker-compose ps

# Vedere logs
./scripts/migrate.sh logs

# Riavviare database
docker-compose restart database
```

### **Errore: "Migration failed"**

```bash
# Vedere dettagli errore nei logs
docker-compose logs database

# Verificare sintassi SQL
psql -U calendar_user -d calendar_db -f database/migrations/problematic.sql

# Correggere e ri-applicare
```

## 🔄 **Scenario Comuni**

### **1. Nuovo Sviluppatore Setup**

```bash
git clone <repo>
cd privatecal
docker-compose -f docker-compose.dev.yml up -d
# Migrazioni vengono applicate automaticamente ✨
```

### **2. Deploy in Production**

```bash
# Pull latest code
git pull origin main

# Apply migrations (automatic on startup)
docker-compose -f docker-compose.prod.yml up -d

# Verify migrations
./scripts/migrate.sh status prod
```

### **3. Aggiungere Nuova Feature**

```bash
# Create migration
./scripts/migrate.sh create add_feature_x

# Edit migration file
vim database/migrations/xxx_add_feature_x.sql

# Test in development
./scripts/migrate.sh migrate dev

# Commit changes
git add database/migrations/xxx_add_feature_x.sql
git commit -m "Add feature X database support"
```

## 📈 **Monitoraggio e Logging**

### **Log Locations**

```bash
# Container logs
docker-compose logs database

# Migration specific logs
./scripts/migrate.sh logs

# Database query logs (se abilitati)
docker-compose exec database tail -f /var/log/postgresql/postgresql.log
```

### **Health Checks**

```bash
# Verificare salute database
docker-compose ps database

# Verificare connessione
docker-compose exec database pg_isready -U calendar_user -d calendar_db

# Test connessione applicazione
curl http://localhost:8080/actuator/health
```

## 🎯 **Risultato Finale**

**✅ Sistema di Migrazione Completo:**
- 🔄 **Automatico**: Migrazioni si applicano al startup
- 🛡️ **Sicuro**: Tracking per evitare duplicazioni
- 🚀 **Facile**: Script semplici per gestione
- 🔍 **Tracciabile**: Log completi di tutte le operazioni
- 🏗️ **Scalabile**: Supporta dev/staging/production
- 📦 **Docker-native**: Integrato con Docker Compose

**🎉 Zero configurazione manuale richiesta per nuovi ambienti!**