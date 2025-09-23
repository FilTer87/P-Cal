# 🎯 Implementazione Preferenze Utente - Riepilogo Completo

## 📋 **Panoramica**
Implementazione completa del sistema di preferenze utente centralizzato, spostando tutte le preferenze da `/settings` a `/profile` come da Opzione A (Profile-Centric).

## 🗄️ **Modifiche Database**

### **Migration SQL**
**File:** `database/add_user_preferences.sql`

```sql
-- Nuove colonne aggiunte alla tabella users:
ALTER TABLE users ADD COLUMN theme VARCHAR(10) DEFAULT 'system';
ALTER TABLE users ADD COLUMN time_format VARCHAR(5) DEFAULT '24h';
ALTER TABLE users ADD COLUMN calendar_view VARCHAR(10) DEFAULT 'week';
ALTER TABLE users ADD COLUMN email_notifications BOOLEAN DEFAULT true;
ALTER TABLE users ADD COLUMN reminder_notifications BOOLEAN DEFAULT true;

-- Constraints per validare i valori
CHECK (theme IN ('light', 'dark', 'system'))
CHECK (time_format IN ('12h', '24h'))
CHECK (calendar_view IN ('month', 'week', 'day', 'agenda'))
```

## 🔧 **Modifiche Backend**

### **1. Entity User.java**
**Aggiunti nuovi campi:**
- `String theme` - Tema dell'interfaccia
- `String timeFormat` - Formato orario (12h/24h)
- `String calendarView` - Vista calendario predefinita
- `Boolean emailNotifications` - Notifiche email
- `Boolean reminderNotifications` - Notifiche promemoria

### **2. Nuovi DTO**
**File:** `dto/UserPreferencesRequest.java`
- DTO per ricevere aggiornamenti preferenze
- Validazione con annotation `@Pattern`

**File:** `dto/UserPreferencesResponse.java`
- DTO per rispondere con preferenze utente
- Factory method `fromUser(User user)`

### **3. Controller AuthController.java**
**Nuovi endpoint:**
```java
GET  /api/auth/preferences  - Ottieni preferenze utente
PUT  /api/auth/preferences  - Aggiorna preferenze utente
```

### **4. Service UserService.java**
**Nuovi metodi:**
- `getCurrentUserPreferences()` - Get preferenze utente corrente
- `updateCurrentUserPreferences(request)` - Update preferenze
- Validatori privati per theme, timeFormat, calendarView

## 🎨 **Modifiche Frontend**

### **1. UserProfile.vue - Migliorato**
**Nuove sezioni nel tab "Preferenze":**
- ✅ **Tema Applicazione**: Bottoni con icone (Sun/Moon/Desktop)
- ✅ **Preferenze App**: Formato orario, Vista predefinita, Fuso orario
- ✅ **Notifiche**: Email + Promemoria + NTFY avanzate (componente integrato)

**Caratteristiche:**
- Sincronizzazione automatica con `settingsStore`
- Gestione stati loading/errori
- Validazione form client-side

### **2. SettingsView.vue - Semplificato**
**Rimosse sezioni duplicate:**
- ❌ Gestione tema (spostata in Profile)
- ❌ Formato orario (spostato in Profile)
- ❌ Vista predefinita (spostata in Profile)
- ❌ NotificationSettings (spostato in Profile)

**Rimasto:**
- ✅ Solo "Inizio settimana" (configurazione sessione)
- ✅ Link informativo verso Profile per preferenze permanenti

### **3. authApi.ts - Esteso**
**Aggiornati metodi API:**
```typescript
updatePreferences(preferences: {
  theme?: 'light' | 'dark' | 'system'
  timezone?: string
  timeFormat?: '12h' | '24h'
  calendarView?: 'month' | 'week' | 'day' | 'agenda'
  emailNotifications?: boolean
  reminderNotifications?: boolean
})

getPreferences(): Promise<UserPreferencesResponse>
```

## 📊 **Struttura Finale**

### **Profile (`/profile`)** - Preferenze Utente Persistenti
```
├── 👤 Dati Personali (username, email, nome, avatar)
├── 🔒 Sicurezza (password, 2FA)
├── 🎨 Tema Applicazione (light/dark/system)
├── ⚙️ Preferenze App (formato orario, vista calendario, fuso orario)
├── 🔔 Notifiche (email, promemoria, NTFY avanzate)
└── ⚠️ Area Pericolo (esporta dati, elimina account)
```

### **Settings (`/settings`)** - Configurazioni Sessione
```
├── 📅 Calendario (solo inizio settimana)
└── 🔗 Link al Profile per preferenze permanenti
```

## 🔄 **Flusso di Sincronizzazione**

1. **Frontend → Backend**: Modifiche in Profile chiamano `PUT /api/auth/preferences`
2. **Database**: Preferenze salvate nella tabella `users`
3. **Settings Store**: Aggiornato automaticamente per coerenza app
4. **Tema**: Applicato immediatamente tramite `useTheme()`

## ✅ **Vantaggi Ottenuti**

### **UX Migliorata**
- ✅ Tutte le preferenze utente centralizzate in `/profile`
- ✅ Separazione logica: Profile=persistente, Settings=sessione
- ✅ Eliminata duplicazione e confusione

### **Architettura Pulita**
- ✅ Un solo endpoint `/api/auth/preferences`
- ✅ Modello dati coerente nella tabella `users`
- ✅ Codice frontend e backend più maintentibile

### **Performance**
- ✅ Nessun join aggiuntivo (tutto in tabella `users`)
- ✅ Query più veloci per preferenze
- ✅ Sincronizzazione automatica settings store

## 🚀 **Istruzioni Deployment**

### **1. Database**
```bash
# Eseguire la migration
psql -d privatecal -f database/add_user_preferences.sql
```

### **2. Backend**
```bash
# Compilare il progetto Java (Spring Boot)
cd backend
mvn clean compile
```

### **3. Frontend**
```bash
# Build del frontend Vue.js
cd frontend
npm run build
```

### **4. Test Endpoint**
```bash
# Test GET preferences
curl -H "Authorization: Bearer <JWT>" \
     http://localhost:8080/api/auth/preferences

# Test PUT preferences
curl -X PUT \
     -H "Authorization: Bearer <JWT>" \
     -H "Content-Type: application/json" \
     -d '{"theme":"dark","timeFormat":"12h"}' \
     http://localhost:8080/api/auth/preferences
```

## 🔧 **Endpoint API Finali**

| Metodo | Endpoint | Descrizione | Body |
|--------|----------|-------------|------|
| `GET` | `/api/auth/preferences` | Ottieni preferenze | - |
| `PUT` | `/api/auth/preferences` | Aggiorna preferenze | `UserPreferencesRequest` |
| `GET` | `/api/auth/me` | Profilo utente | - |
| `PUT` | `/api/auth/me` | Aggiorna profilo | `UserResponse` |

## ⚠️ **Note Importante**

1. **Compatibilità**: Tutti gli utenti esistenti riceveranno valori di default
2. **Validazione**: Sia client-side che server-side per valori preferenze
3. **Sicurezza**: Endpoint protetti da JWT authentication
4. **Estensibilità**: Facile aggiungere nuove preferenze in futuro

## 🎉 **Risultato Finale**

**✅ Implementazione Completa di Opzione A (Profile-Centric)**
- Tutte le preferenze utente centralizzate nel profilo
- Eliminata duplicazione di codice
- UX migliorata e architettura più pulita
- Backend e frontend pronti per produzione