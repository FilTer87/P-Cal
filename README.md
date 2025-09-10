# PrivateCal v2 - Calendario Personale

## 📅 Panoramica del Progetto

PrivateCal v2 è una web application calendario completa simile a Google Calendar con funzionalità avanzate di gestione task, reminder personalizzati e notifiche push in tempo reale tramite NTFY. L'applicazione supporta accesso multi-utente con un'interfaccia moderna, responsive e completamente localizzata in italiano.

## 🚀 Caratteristiche Principali

### ✨ **Gestione Calendario**
- **4 Modalità di visualizzazione**: Anno, Mese, Settimana, Giorno
- **Navigazione intuitiva** con controlli prev/next e date picker
- **Responsive design** ottimizzato per mobile, tablet e desktop
- **Tema scuro/chiaro** con rilevamento automatico delle preferenze sistema

### 📋 **Gestione Task Avanzata**
- **CRUD completo** per task con validazione real-time
- **Categorizzazione** con colori personalizzabili
- **Task tutto il giorno** e con orari specifici
- **Localizzazione** e descrizioni dettagliate
- **Rilevamento conflitti** per task sovrapposti
- **Statistiche** e analytics integrati

### ⏰ **Sistema Reminder Intelligente**
- **Reminder multipli** per task con preset comuni (5min, 15min, 1h, 1g, 1s)
- **Notifiche push NTFY** in tempo reale
- **Notifiche browser** come fallback
- **Processamento automatico** in background ogni 30 secondi
- **Personalizzazione** completa delle preferenze notifiche

### 🔐 **Autenticazione Sicura**
- **JWT stateless** con token access (15min) e refresh (7 giorni)
- **Registrazione** con validazione completa
- **Gestione profilo** utente integrata
- **Isolamento dati** completo tra utenti
- **Hash password** con BCrypt di forza 12

## 🛠 Stack Tecnologico

### **Frontend**
- **Vue.js 3** con Composition API e `<script setup>`
- **TypeScript** per type safety completo  
- **Tailwind CSS** per styling responsive e moderno
- **Pinia** per state management reattivo
- **Vue Router** con navigation guards
- **Axios** con interceptors automatici
- **Headless UI** per componenti accessibili

### **Backend**
- **Java 17** con SpringBoot 3.2
- **Spring Security** per autenticazione JWT
- **Spring Data JPA** con Hibernate
- **PostgreSQL** per persistenza dati
- **Maven** per build management
- **Scheduled Jobs** per processamento background

### **Infrastruttura**
- **Docker** e Docker Compose per containerizzazione
- **Nginx** per reverse proxy e static files
- **NTFY** per notifiche push real-time
- **PostgreSQL** con connection pooling HikariCP

## 📁 Struttura del Progetto

```
PrivateCal_v2/
├── backend/                    # SpringBoot application
│   ├── src/main/java/com/privatecal/
│   │   ├── entity/            # JPA entities (User, Task, Reminder)
│   │   ├── repository/        # Data access layer
│   │   ├── service/           # Business logic layer
│   │   ├── controller/        # REST API endpoints
│   │   ├── security/          # JWT authentication & security
│   │   ├── dto/              # Data transfer objects
│   │   └── config/           # Configuration classes
│   ├── src/main/resources/   # Application configurations
│   ├── pom.xml              # Maven dependencies
│   └── Dockerfile           # Container configuration
├── frontend/                 # Vue.js 3 application
│   ├── src/
│   │   ├── components/       # Vue components organizzati per feature
│   │   │   ├── Calendar/     # Componenti calendario (Month, Week, Day, etc.)
│   │   │   ├── Task/         # Gestione task (Modal, Form, Card, List, etc.)
│   │   │   ├── Reminder/     # Sistema reminder (Form, List, Presets, etc.)
│   │   │   ├── Auth/         # Autenticazione (Login, Register, Profile)
│   │   │   └── Common/       # Componenti riutilizzabili (Modal, Toast, etc.)
│   │   ├── views/           # Pagine principali (Calendar, Login, Register)
│   │   ├── stores/          # Pinia stores (auth, calendar, tasks, theme)
│   │   ├── composables/     # Business logic riutilizzabile
│   │   ├── services/        # API clients e HTTP services
│   │   ├── types/          # TypeScript type definitions
│   │   └── utils/          # Utility functions e helpers
│   ├── package.json        # NPM dependencies
│   └── Dockerfile         # Container configuration
├── database/
│   └── init.sql           # Schema e dati di esempio PostgreSQL
├── docker-compose.yml     # Multi-container orchestration
├── CLAUDE.md             # Documentazione per Claude Code
└── README.md             # Questa documentazione
```

## 🚀 Quick Start

### **Prerequisiti**
- Docker e Docker Compose installati
- Porte 80 (frontend), 8080 (backend), 5432 (database) disponibili

### **Avvio Rapido**
```bash
# Clona o scarica il progetto
cd PrivateCal_v2

# Avvia tutti i servizi con Docker Compose
docker-compose up --build

# L'applicazione sarà disponibile su:
# Frontend: http://localhost
# Backend API: http://localhost:8080
# Database: localhost:5432
```

### **Sviluppo Locale**

#### **Backend (SpringBoot)**
```bash
cd backend

# Avvia il database PostgreSQL
docker run --name privatecal-db -e POSTGRES_DB=calendar_db -e POSTGRES_USER=calendar_user -e POSTGRES_PASSWORD=calendar_pass -p 5432:5432 -d postgres:15-alpine

# Avvia l'applicazione
./mvnw spring-boot:run

# L'API sarà disponibile su http://localhost:8080
```

#### **Frontend (Vue.js)**
```bash
cd frontend

# Installa le dipendenze
npm install

# Avvia il development server
npm run dev

# L'applicazione sarà disponibile su http://localhost:5173
```

## 📡 API Endpoints

### **Autenticazione (`/api/auth`)**
- `POST /api/auth/login` - Login utente
- `POST /api/auth/register` - Registrazione nuovo utente
- `POST /api/auth/refresh` - Refresh token JWT
- `GET /api/auth/me` - Profilo utente corrente
- `PUT /api/auth/me` - Aggiornamento profilo
- `POST /api/auth/change-password` - Cambio password

### **Task Management (`/api/tasks`)**
- `GET /api/tasks` - Lista task utente (con filtri data)
- `POST /api/tasks` - Creazione nuovo task
- `GET /api/tasks/{id}` - Dettaglio task specifico
- `PUT /api/tasks/{id}` - Aggiornamento task
- `DELETE /api/tasks/{id}` - Eliminazione task
- `GET /api/tasks/today` - Task di oggi
- `GET /api/tasks/upcoming` - Task prossimi

### **Reminder System (`/api/reminders`)**
- `POST /api/reminders/task/{taskId}` - Creazione reminder
- `GET /api/reminders/upcoming` - Reminder prossimi
- `PUT /api/reminders/{id}` - Aggiornamento reminder
- `DELETE /api/reminders/{id}` - Eliminazione reminder

## 🔧 Configurazione

### **Variabili d'Ambiente Backend**
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/calendar_db
DATABASE_USERNAME=calendar_user
DATABASE_PASSWORD=calendar_pass

# JWT Security
JWT_SECRET=privatecal-jwt-secret-key-2024-very-secure

# NTFY Notifications
NTFY_SERVER_URL=https://ntfy.sh

# Application Profile
SPRING_PROFILES_ACTIVE=development
```

### **Configurazione Frontend**
```bash
# API Backend
VITE_API_BASE_URL=http://localhost:8080/api

# NTFY Configuration
VITE_NTFY_SERVER=https://ntfy.sh
VITE_NTFY_TOPIC_PREFIX=calendar-user-
```

## 🎨 Personalizzazione Tema

L'applicazione supporta completamente dark/light mode con:
- **Rilevamento automatico** preferenze sistema
- **Toggle manuale** nell'interfaccia utente
- **Persistenza** delle preferenze nel localStorage
- **Transizioni fluide** tra i temi
- **Colori personalizzabili** per le categorie task

## 📱 NTFY Push Notifications

### **Configurazione**
1. **Server NTFY**: Usa il server pubblico `https://ntfy.sh` o un'istanza privata
2. **Topic Format**: `calendar-user-{userId}-{timestamp}-{random}` per sicurezza
3. **Notifiche Strutturate**: Con titolo, messaggio, tag e azioni

### **Esempio Notifica**
```json
{
  "topic": "calendar-user-123-1703123456-abc",
  "title": "Promemoria: Meeting importante",
  "message": "Il tuo task 'Meeting importante' inizia tra 15 minuti",
  "tags": ["📅", "⏰", "reminder"],
  "priority": 4,
  "click": "https://your-calendar.com/tasks/456",
  "actions": [{
    "action": "view",
    "label": "Visualizza Task",
    "url": "https://your-calendar.com/tasks/456"
  }]
}
```

## 🛡 Sicurezza

### **Implementata**
- **JWT Tokens** con scadenza automatica (15min access, 7d refresh)
- **Password Hashing** con BCrypt strength 12
- **CORS Protection** con origini specifiche
- **User Isolation** completo a livello database
- **Input Validation** lato server e client
- **XSS Prevention** con sanitizzazione automatica

### **Best Practices**
- **HTTPS Required** per produzione
- **Rate Limiting** sugli endpoint pubblici
- **Regular Security Updates** per dipendenze
- **Environment Secrets** never hard-coded

## 📊 Performance

### **Frontend Optimizations**
- **Code Splitting** automatico con Vite
- **Virtual Scrolling** per liste lunghe
- **Debounced Search** per performance API
- **Optimistic Updates** per UX fluida
- **Component Lazy Loading** per bundle size

### **Backend Optimizations**
- **Connection Pooling** HikariCP per PostgreSQL
- **JPA Query Optimization** con indexes strategici
- **Scheduled Background Jobs** per reminder processing
- **Caching Strategy** per query frequenti

## 🧪 Testing

### **Frontend Testing**
```bash
cd frontend
npm run test          # Unit tests con Vitest
npm run e2e          # E2E tests con Cypress
npm run lint         # ESLint + Prettier
npm run type-check   # TypeScript validation
```

### **Backend Testing**
```bash
cd backend
./mvnw test                    # Unit + Integration tests
./mvnw test -Dtest=AuthTest    # Test specifici
./mvnw spring-boot:run -Dspring.profiles.active=test
```

## 🚀 Deployment

### **Produzione con Docker**
```bash
# Build e avvio produzione
docker-compose -f docker-compose.prod.yml up -d

# Monitoring logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Backup database
docker exec privatecal-db pg_dump -U calendar_user calendar_db > backup.sql
```

### **Environment Setup**
1. **SSL/TLS** configurazione per HTTPS
2. **Domain Setup** con DNS appropriato
3. **Environment Variables** per secrets produzione
4. **Database Backup** strategy e monitoring
5. **Log Aggregation** per debugging produzione

## 🐛 Troubleshooting

### **Problemi Comuni**

#### **Backend non si avvia**
- Verificare che PostgreSQL sia in esecuzione
- Controllare le credenziali database in `application.yml`
- Verificare la porta 8080 non sia occupata

#### **Frontend build fallisce**
- Aggiornare Node.js alla versione 18+
- Cancellare `node_modules` e `package-lock.json`, poi `npm install`
- Verificare le variabili d'ambiente in `.env`

#### **Notifiche NTFY non funzionano**
- Controllare la connettività al server NTFY
- Verificare il formato del topic generato
- Testare le notifiche browser come fallback

#### **Login non funziona**
- Verificare la configurazione CORS nel backend
- Controllare i JWT secrets siano configurati
- Verificare che i timestamps server/client siano sincronizzati

## 🤝 Contributori

Questo progetto è sviluppato per dimostrare le capability complete di sviluppo full-stack moderno con le migliori pratiche di sicurezza, performance e user experience.

### **Contribuire**
1. Fork del repository
2. Creare feature branch (`git checkout -b feature/nuova-funzionalita`)
3. Commit changes (`git commit -am 'Aggiungi nuova funzionalità'`)
4. Push al branch (`git push origin feature/nuova-funzionalita`)
5. Creare Pull Request

## 📄 Licenza

Questo progetto è rilasciato sotto licenza MIT. Vedi il file `LICENSE` per dettagli completi.

## 📞 Supporto

Per supporto tecnico, bug reports o richieste di feature:
- Aprire issue su GitHub
- Contattare il team di sviluppo
- Consultare la documentazione in `/docs`

---

**PrivateCal v2** - Developed with ❤️ using modern web technologies