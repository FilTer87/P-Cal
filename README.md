# P-Cal 📅

Una moderna applicazione web di calendario personale con gestione task, reminder e notifiche push.

## ✨ Caratteristiche

- 📊 **Dashboard completa** con statistiche delle attività
- ~~📅 **Viste multiple**: Mese, Settimana, Giorno, Agenda~~
- ~~✅ **Gestione task** con priorità e stati di completamento~~
- ⏰ **Sistema di reminder** con notifiche push via NTFY
- 🎨 **Tema scuro/chiaro** con preferenze sistema
- 🔐 **Autenticazione JWT** sicura con refresh token
- 📱 **Design responsivo** ottimizzato per mobile
- 🐳 **Deployment Docker** completo

## 🏗️ Architettura

### Frontend
- **Vue.js 3**
- **TypeScript**
- **Tailwind CSS**
- **Pinia**
- **Vite**

### Backend
- **Spring Boot 3**
- **Spring Security**
- **JPA/Hibernate**
- **PostgreSQL**
- **Maven**

### Infrastructure
- **Docker** / **Docker Compose**
- **Nginx** come reverse proxy
- **NTFY** server per push notifications

## 🚀 Quick Start

### Prerequisiti
- Docker e Docker Compose
- Git

### Installazione

1. **Clona il repository**
   ```bash
   git clone https://your-gitea-instance.com/username/privatecal-v2.git
   cd privatecal-v2
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
   - Frontend: http://localhost
   - Backend API: http://localhost:8080/api
   - Health Check: http://localhost:8080/actuator/health
   - API Documentation (Swagger): http://localhost:8080/swagger-ui.html
   - OpenAPI JSON: http://localhost:8080/v3/api-docs

## ⚙️ Configurazione

### Variabili d'ambiente
Le principali configurazioni sono gestite tramite variabili d'ambiente. Copia `.env.example` in `.env` e modifica i valori:

```bash
# Database
DATABASE_URL=jdbc:postgresql://database:5432/calendar_db
DATABASE_USERNAME=calendar_user
DATABASE_PASSWORD=your_secure_password

# JWT Security
JWT_SECRET=your-super-secret-jwt-key-change-in-production

# NTFY Notifications
NTFY_SERVER_URL=https://ntfy.sh
NTFY_TOPIC_PREFIX=calendar-user-
```

### Personalizzazione tema
Il tema può essere configurato tramite le variabili frontend:
```bash
VITE_DEFAULT_THEME=system  # light, dark, system
```

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

### Testing
```bash
# Backend tests
cd backend && ./mvnw test

# Frontend tests
cd frontend && npm run test
```

### Build di produzione
```bash
# Frontend
cd frontend && npm run build

# Backend
cd backend && ./mvnw clean package
```

## 📡 API Endpoints

### Autenticazione
- `POST /api/auth/login` - Login utente
- `POST /api/auth/register` - Registrazione utente
- `POST /api/auth/refresh` - Refresh token

### Task Management
- `GET /api/tasks` - Lista task
- `POST /api/tasks` - Crea task
- `GET /api/tasks/{id}` - Dettagli task
- `PUT /api/tasks/{id}` - Aggiorna task
- `DELETE /api/tasks/{id}` - Elimina task
- `GET /api/tasks/overdue` - Task in ritardo
- `GET /api/tasks/stats` - Statistiche task

### Reminder
- `GET /api/tasks/{taskId}/reminders` - Lista reminder
- `POST /api/tasks/{taskId}/reminders` - Crea reminder

## 🎯 Roadmap

- [ ] **Recurring tasks** - Task ricorrenti
- [ ] **Calendar sharing** - Condivisione calendario  
- [ ] **Import/Export** - ICS support
- [ ] **Email notifications** - Alternative a NTFY
- [ ] **Mobile app** - React Native
- [ ] **Team collaboration** - Multi-user calendars

## 🐛 Bug Report & Feature Request

Per segnalare bug o richiedere nuove funzionalità, apri una issue nel repository.

## 🔒 Sicurezza

- Le password sono hashate con BCrypt
- JWT token con scadenza configurabile
- Validazione input lato server
- Protezione CORS configurabile
- Isolamento dati per utente

### Note di sicurezza importanti
- Cambia `JWT_SECRET` in produzione
- Usa password robuste per il database
- Abilita HTTPS in produzione
- Limita CORS agli domini necessari

## 📄 Licenza

Questo progetto è rilasciato sotto la [MIT License](LICENSE).

```
MIT License - vedere il file LICENSE per i dettagli completi
```

## 👥 Contributi

I contributi sono benvenuti! Per contribuire:

1. Fork del progetto
2. Crea un branch per la feature (`git checkout -b feature/AmazingFeature`)
3. Commit delle modifiche (`git commit -m 'Add some AmazingFeature'`)
4. Push al branch (`git push origin feature/AmazingFeature`)
5. Apri una Pull Request

### Standard di sviluppo
- Usa TypeScript per il frontend
- Segui le convenzioni Spring Boot per il backend  
- Test coverage minimo 70%
- Documenta le API con commenti

## 🏷️ Versioning

Usiamo [SemVer](http://semver.org/) per il versioning. Per le versioni disponibili, vedi i [tags su questo repository](https://your-gitea-instance.com/username/privatecal-v2/releases).

## ⚡ Performance

- Frontend con lazy loading
- API con paginazione
- Database con indici ottimizzati
- Build ottimizzato con Vite
- Docker multi-stage builds

## 📞 Supporto

Per supporto tecnico:
- 📚 Consulta la documentazione in `/docs`
- 🐛 Apri una issue per bug
- 💡 Discord/Forum per discussioni generali

---

**Sviluppato con ❤️ per la gestione del tempo personale**