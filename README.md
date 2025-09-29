# P-Cal 📅

Una moderna applicazione web di calendario personale con gestione avanzata di task, reminder intelligenti e sistema di notifiche multi-canale.

## ✨ Funzionalità Principali

### 📊 **Gestione Calendario Completa**
- **Viste multiple**: Mese, Settimana, Giorno e Agenda
- **Gestione task avanzata** con colori personalizzabili e descrizioni
- **Visualizzazione temporale intelligente** con distinzione attività passate/future
- **Griglia oraria precisa** nella vista settimana con indicatori visivi per attività fuori schermo
- **Tooltips informativi** per task con testo abbreviato

### ⏰ **Sistema di Reminder Avanzato**
- **Notifiche multi-canale**: Email e NTFY server
- **Reminder multipli** per ogni attività
- **Scheduling flessibile** (minuti, ore, giorni prima dell'evento)
- **Gestione automatica** delle notifiche scadute

### 🔐 **Sicurezza e Autenticazione**
- **Autenticazione JWT** con refresh token automatico
- **2FA (Two-Factor Authentication)** tramite app TOTP
- **Reset password sicuro** via email
- **Hashing BCrypt** per le password
- **Isolamento completo** dei dati per utente

### 👤 **Gestione Utente e Preferenze**
- **Registrazione e login** con validazione completa
- **Profilo utente** modificabile (nome, email, password)
- **Preferenze personalizzabili**:
  - Tema (chiaro/scuro/automatico)
  - Fuso orario
  - Formato orario (12h/24h)
  - Primo giorno della settimana
  - Abilitazione notifiche
  - Topic NTFY personalizzato
- **Export dati completo** (GDPR-friendly)
- **Eliminazione account** con cancellazione dati

### 🎨 **Esperienza Utente**
- **Design responsivo** ottimizzato per desktop e mobile
- **Interfaccia moderna** con Tailwind CSS
- **Tema adattivo** al sistema operativo
- **Performance ottimizzate** con lazy loading
- **Attività passate** nascondibili nelle viste giorno/agenda

## 🏗️ Architettura

### Frontend
- **Vue.js 3** con Composition API
- **TypeScript** per type safety
- **Tailwind CSS** per styling moderno
- **Pinia** per state management
- **Vite** per build veloce
- **Vue Router** per navigazione SPA

### Backend
- **Spring Boot 3.2** con Java 17
- **Spring Security** per autenticazione/autorizzazione
- **JPA/Hibernate** per ORM
- **PostgreSQL** come database principale
- **Lombok** per riduzione boilerplate
- **Maven** per gestione dipendenze

### Infrastructure
- **Docker** e **Docker Compose** per containerizzazione
- **PostgreSQL** con script di migrazione automatici
- **Nginx** come reverse proxy (production)
- **Server NTFY** per notifiche push

## 🚀 Quick Start

### Prerequisiti
- Docker e Docker Compose
- Git

### Installazione

1. **Clona il repository**
   ```bash
   git clone <your-repository-url>
   cd PrivateCal_v2
   ```

2. **Configura l'ambiente**
   ```bash
   cp .env.example .env
   # Modifica .env con le tue configurazioni
   ```

3. **Avvia l'applicazione**
   ```bash
   docker-compose up --build
   ```

4. **Accedi all'applicazione**
   - **Frontend**: http://localhost:3000
   - **Backend API**: http://localhost:8080/api
   - **Swagger UI**: http://localhost:8080/swagger-ui.html
   - **Health Check**: http://localhost:8080/actuator/health

## ⚙️ Configurazione

### File .env principale
```bash
# Database PostgreSQL
DATABASE_URL=jdbc:postgresql://database:5432/privatecal_db
DATABASE_USERNAME=privatecal_user
DATABASE_PASSWORD=your_secure_password

# JWT Security
JWT_SECRET=your-super-secret-jwt-key-change-in-production-min-256-bits
JWT_ACCESS_TOKEN_EXPIRATION=86400000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Email Configuration (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@privatecal.com

# NTFY Notifications
NTFY_SERVER_URL=https://ntfy.sh
NTFY_TOPIC_PREFIX=privatecal-user-
NTFY_AUTH_TOKEN=

# Application
APP_BASE_URL=http://localhost:3000
```

### Configurazione NTFY
P-Cal supporta notifiche tramite [NTFY](https://github.com/binwiederhier/ntfy), un servizio di notifiche push semplice e self-hosted.

**Opzioni di configurazione:**
- Usa il server pubblico `https://ntfy.sh` (default)
- Self-host il tuo server NTFY per privacy completa
- Configura autenticazione per topic privati

**Esempio configurazione:**
```bash
NTFY_SERVER_URL=https://your-ntfy-server.com
NTFY_TOPIC_PREFIX=your-app-prefix-
NTFY_AUTH_TOKEN=your-auth-token  # Opzionale per server privati
```

Per maggiori dettagli: [NTFY Documentation](https://docs.ntfy.sh/)

## 🛠️ Sviluppo

### Sviluppo locale senza Docker

#### Backend
```bash
cd backend
./mvnw spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
```

#### Database
Assicurati di avere PostgreSQL in esecuzione e configura le variabili d'ambiente.

### Testing
```bash
# Backend tests
cd backend && ./mvnw test

# Frontend tests
cd frontend && npm run test

# Linting
cd frontend && npm run lint
```

### Build di produzione
```bash
# Frontend build
cd frontend && npm run build

# Backend build
cd backend && ./mvnw clean package

# Docker build completo
docker-compose -f docker-compose.prod.yml up --build
```

## 📡 API Endpoints

### Autenticazione
- `POST /api/auth/register` - Registrazione utente
- `POST /api/auth/login` - Login con credenziali
- `POST /api/auth/refresh` - Refresh token JWT
- `POST /api/auth/logout` - Logout utente
- `POST /api/auth/forgot-password` - Richiesta reset password
- `POST /api/auth/reset-password` - Reset password con token

### Two-Factor Authentication
- `POST /api/auth/2fa/setup` - Configurazione 2FA
- `POST /api/auth/2fa/verify` - Verifica codice 2FA
- `POST /api/auth/2fa/disable` - Disabilitazione 2FA

### Gestione Task
- `GET /api/tasks` - Lista task dell'utente
- `POST /api/tasks` - Creazione nuovo task
- `GET /api/tasks/{id}` - Dettagli task specifico
- `PUT /api/tasks/{id}` - Aggiornamento task
- `DELETE /api/tasks/{id}` - Eliminazione task
- `GET /api/tasks/today` - Task di oggi
- `GET /api/tasks/upcoming` - Task futuri
- `GET /api/tasks/overdue` - Task scaduti
- `GET /api/tasks/statistics` - Statistiche personali
- `POST /api/tasks/{id}/clone` - Duplicazione task

### Gestione Reminder
- `GET /api/reminders` - Lista reminder utente
- `POST /api/reminders/task/{taskId}` - Creazione reminder per task
- `PUT /api/reminders/{id}` - Aggiornamento reminder
- `DELETE /api/reminders/{id}` - Eliminazione reminder

### Profilo Utente
- `GET /api/users/profile` - Dati profilo utente
- `PUT /api/users/profile` - Aggiornamento profilo
- `GET /api/users/preferences` - Preferenze utente
- `PUT /api/users/preferences` - Aggiornamento preferenze
- `GET /api/users/export` - Export dati utente
- `DELETE /api/users/account` - Eliminazione account

## 🎯 Roadmap Future

### 🔧 Funzionalità in Valutazione
- **Categorie task** con filtri di visualizzazione
- **Task ricorrenti** con pattern personalizzabili
- **Multilingua** (Italiano/Inglese)
- **Drag & Drop** per spostamento task nelle griglie
- **Gestione sessioni avanzata**
- **Notifiche aggiuntive**: Gotify, Slack, Telegram
- **Import dati** da altri calendar
- **Verifica email** post-registrazione

### 📱 Espansioni Tecniche
- **API mobile** per app native
- **Calendario condiviso** multi-utente
- **Plugin system** per integrazioni
- **Dark/Light mode** avanzato per componenti

## 🔒 Sicurezza

### Implementazioni Correnti
- **Password hashing** con BCrypt (strength 12)
- **JWT tokens** con scadenza configurabile
- **Input validation** completa lato server
- **CORS protection** configurabile
- **Rate limiting** su endpoint sensibili
- **Two-Factor Authentication** TOTP
- **Secure password reset** via email temporizzato
- **Data isolation** completo per utente

### Best Practices di Produzione
```bash
# Cambia assolutamente in produzione
JWT_SECRET=your-production-secret-min-256-bits

# Usa password robuste
DATABASE_PASSWORD=complex-database-password

# Configura CORS appropriatamente
CORS_ALLOWED_ORIGINS=https://your-domain.com

# Abilita HTTPS
USE_HTTPS=true

# Configura email sicura
MAIL_PASSWORD=app-specific-password
```

## 🐳 Deploy in Produzione

### Con Docker Compose
```bash
# Production build
docker-compose -f docker-compose.prod.yml up -d

# Con SSL/HTTPS
docker-compose -f docker-compose.prod.yml -f docker-compose.ssl.yml up -d
```

### Struttura Container
- **Frontend**: Nginx con build ottimizzato
- **Backend**: Spring Boot con JVM tuning
- **Database**: PostgreSQL con persistenza
- **Reverse Proxy**: Nginx con SSL termination

## ⚡ Performance

### Ottimizzazioni Frontend
- **Lazy loading** componenti e route
- **Code splitting** automatico con Vite
- **Tree shaking** per bundle ridotti
- **Progressive loading** per viste calendario
- **Caching intelligente** delle API calls

### Ottimizzazioni Backend
- **JPA optimizations** con lazy/eager appropriati
- **Database indexing** su query frequenti
- **Connection pooling** configurato
- **Query pagination** per liste grandi
- **Caching L2** per entità statiche

## 📄 Licenza

Questo progetto è rilasciato sotto **MIT License**.

```
MIT License - Uso libero per progetti personali e commerciali
Vedi LICENSE file per dettagli completi
```

## 👥 Contributi

I contributi sono benvenuti! Per contribuire:

1. **Fork** del progetto
2. **Crea feature branch** (`git checkout -b feature/AmazingFeature`)
3. **Commit** modifiche (`git commit -m 'Add AmazingFeature'`)
4. **Push** al branch (`git push origin feature/AmazingFeature`)
5. **Apri Pull Request**

### Standard di Sviluppo
- **TypeScript** obbligatorio per frontend
- **Spring Boot conventions** per backend
- **Test coverage** minimo 70%
- **API documentation** con OpenAPI/Swagger
- **Code review** obbligatorio per merge

## 📞 Supporto e Community

### Documentazione
- **API docs**: Swagger UI integrato
- **Code comments**: Javadoc e TSDoc
- **Architecture docs**: In `/docs` directory

### Reporting Issues
- 🐛 **Bug reports**: Usa issue template
- 💡 **Feature requests**: Discussione prima implementazione
- ❓ **Questions**: Tag appropriati per Q&A

### Versioning
Utilizziamo **Semantic Versioning** (SemVer):
- `MAJOR.MINOR.PATCH`
- Breaking changes incrementano MAJOR
- Nuove features incrementano MINOR
- Bug fixes incrementano PATCH

---

## 🏷️ Status Progetto

**Versione attuale**: `2.0.0-beta`
**Stato**: In fase di testing pre-release
**Tipo**: Progetto open-source personale

**Sviluppato con ❤️ per una gestione del tempo efficace e moderna**