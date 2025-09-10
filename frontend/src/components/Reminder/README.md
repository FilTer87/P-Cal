# PrivateCal Reminder System

Un sistema completo di promemoria con notifiche push NTFY e fallback per notifiche browser.

## Componenti Implementati

### Componenti Reminder

#### 1. **ReminderList.vue**
Lista dei promemoria per un task specifico con funzionalità complete:

- **Props:**
  - `taskId: number` - ID del task
  - `taskDueDate?: string` - Data di scadenza del task

- **Funzionalità:**
  - Visualizza tutti i promemoria per un task
  - Ordinamento per stato (inviati/pendenti) e data
  - Indicatori di stato visivi (inviato, in attesa, scaduto)
  - Azioni rapide (modifica, elimina, posticipa)
  - Integrazione con preset per aggiunta veloce
  - Tempo rimanente fino al promemoria

#### 2. **ReminderForm.vue**
Form per creare/modificare promemoria con due modalità:

- **Props:**
  - `taskId: number` - ID del task
  - `taskDueDate?: string` - Data di scadenza del task
  - `reminder?: Reminder | null` - Promemoria da modificare

- **Modalità:**
  - **Offset Mode**: Tempo prima della scadenza (es. 15 minuti prima)
  - **Absolute Mode**: Data e ora specifica

- **Funzionalità:**
  - Preset comuni (5min, 15min, 1h, 1 giorno, ecc.)
  - Validazione intelligente (no promemoria nel passato)
  - Anteprima del promemoria
  - Supporto per diverse unità temporali

#### 3. **ReminderPresets.vue**
Selezione rapida di promemoria predefiniti:

- **Props:**
  - `taskDueDate?: string` - Data di scadenza
  - `existingReminders?: Reminder[]` - Promemoria esistenti

- **Funzionalità:**
  - 9 preset predefiniti (5min a 1 settimana)
  - Input personalizzato per tempi custom
  - Selezione multipla per più promemoria
  - Prevenzione duplicati
  - Visualizzazione conflitti

#### 4. **NotificationSettings.vue**
Configurazione completa delle notifiche:

- **Funzionalità Browser:**
  - Toggle globale notifiche
  - Richiesta permessi browser
  - Test notifiche browser
  - Guida per abilitazione per browser

- **Configurazione NTFY:**
  - URL server NTFY personalizzabile
  - Generazione topic sicuri
  - Codice QR per app mobile
  - Test connessione e invio
  - Statistiche topic

### Componenti Common

#### 1. **Toast.vue**
Sistema di notifiche toast con 5 tipi:

- **Tipi:** `success`, `error`, `warning`, `info`, `reminder`
- **Funzionalità:**
  - Auto-dismiss configurabile
  - Barra di progresso
  - Azioni personalizzate
  - Animazioni fluide
  - Supporto dark mode

#### 2. **Modal.vue**
Modal riutilizzabile con accessibilità:

- **Dimensioni:** `sm`, `md`, `lg`, `xl`, `2xl`, `full`
- **Funzionalità:**
  - Focus trap
  - Chiusura con ESC/backdrop
  - Modalità persistente
  - Slot header/footer
  - Scrolling contenuto

#### 3. **LoadingSpinner.vue**
Indicatore di caricamento personalizzabile:

- **Dimensioni:** `xs`, `small`, `medium`, `large`, `xl`
- **Colori:** `blue`, `green`, `red`, `yellow`, `purple`, `gray`, `white`
- **Funzionalità:**
  - Etichette opzionali
  - Reduced motion support
  - High contrast support

#### 4. **ConfirmDialog.vue**
Dialog di conferma con varianti:

- **Varianti:** `default`, `danger`, `warning`
- **Funzionalità:**
  - Checkbox di conferma obbligatoria
  - Dettagli espandibili
  - Stati di processing
  - Prevenzione chiusura durante operazioni

## Servizi

### 1. **ntfyService.ts**
Servizio completo per integrazioni NTFY:

```typescript
// Invio notifica semplice
await sendNotification({
  server: 'https://ntfy.sh',
  topic: 'calendar-user-123',
  title: 'Test',
  message: 'Messaggio di test',
  tags: ['test'],
  priority: 3
})

// Invio promemoria formattato
await sendReminderNotification({
  server: 'https://ntfy.sh',
  topic: 'calendar-user-123',
  taskTitle: 'Riunione importante',
  taskId: 456,
  timeLeft: '15 minuti',
  dueDate: '2024-01-15T14:00:00Z',
  baseUrl: 'https://mycalendar.com'
})
```

**Funzionalità:**
- Retry automatici con backoff
- Validazione URL server
- Test connessione
- Generazione topic sicuri
- Sottoscrizione real-time
- Timeout configurabili

### 2. **notificationService.ts**
Servizio notifiche browser con Service Worker:

```typescript
// Richiesta permessi
const permission = await requestNotificationPermission()

// Invio notifica
await sendBrowserNotification('Titolo', {
  body: 'Corpo della notifica',
  icon: '/favicon.ico',
  tag: 'unique-tag',
  requireInteraction: true,
  actions: [{
    action: 'view',
    title: 'Visualizza'
  }]
})

// Gestione click
notificationService.setupNotificationHandlers({
  onNotificationClick: (data) => {
    console.log('Notifica cliccata:', data)
  },
  onActionClick: (action, data) => {
    console.log('Azione:', action, data)
  }
})
```

**Funzionalità:**
- Fallback automatico browser/service worker
- Gestione coda notifiche
- Badge app
- Vibrazione mobile
- Pianificazione notifiche
- Background sync

## Composables

### 1. **useNotificationPermissions.ts**
Gestione reattiva permessi browser:

```typescript
const {
  permission,
  isGranted,
  requestPermission,
  sendTestNotification,
  getPermissionGuide
} = useNotificationPermissions()
```

### 2. **useNTFY.ts**
Integrazione reattiva NTFY:

```typescript
const {
  settings,
  isReady,
  testConnection,
  sendNotification,
  generateTopic,
  subscribe
} = useNTFY()
```

## Integrazione

### 1. Aggiungere a Task Modal

```vue
<template>
  <TaskModal>
    <!-- Form task esistente -->
    
    <!-- Sezione Promemoria -->
    <ReminderList
      :task-id="taskId"
      :task-due-date="task.dueDate"
    />
  </TaskModal>
</template>

<script setup>
import { ReminderList } from '@/components/Reminder'
</script>
```

### 2. Configurazione Globale

```vue
<template>
  <div id="app">
    <!-- App content -->
    
    <!-- Toast Container -->
    <div id="toast-container"></div>
  </div>
  
  <!-- Settings Modal -->
  <NotificationSettings v-if="showSettings" />
</template>

<script setup>
import { NotificationSettings } from '@/components/Reminder'
</script>
```

### 3. Service Worker Registration

```typescript
// main.ts o app.ts
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw-notifications.js')
    .then(registration => {
      console.log('SW registrato:', registration)
    })
    .catch(error => {
      console.error('Errore SW:', error)
    })
}
```

## Formato Notifiche NTFY

### Promemoria Standard
```json
{
  "topic": "calendar-user-123",
  "title": "Promemoria: Riunione Team",
  "message": "Il tuo task 'Riunione Team' inizia tra 15 minuti",
  "tags": ["📅", "⏰", "reminder"],
  "priority": 4,
  "click": "https://calendar.app/tasks/456",
  "actions": [{
    "action": "view",
    "label": "Visualizza Task",
    "url": "https://calendar.app/tasks/456"
  }],
  "icon": "https://calendar.app/favicon.ico"
}
```

## Preset Promemoria

| Preset | Tempo | Descrizione |
|--------|-------|-------------|
| 5min | 5 minuti | Promemoria immediato |
| 15min | 15 minuti | Preparazione veloce |
| 30min | 30 minuti | Preparazione standard |
| 1h | 1 ora | Preparazione approfondita |
| 2h | 2 ore | Mezza giornata prima |
| 4h | 4 ore | Pianificazione giornata |
| 1d | 1 giorno | Promemoria giorno prima |
| 3d | 3 giorni | Preparazione settimanale |
| 1w | 1 settimana | Pianificazione mensile |

## Personalizzazione

### Temi
Tutti i componenti supportano automaticamente:
- Dark/Light mode via Tailwind CSS
- Variabili CSS personalizzabili
- Responsive design completo

### Lingue
Attualmente supportato:
- 🇮🇹 Italiano (completo)

Facile espansione per altre lingue modificando le stringhe nei componenti.

### NTFY Server
Configurazione server personalizzata:
- Server pubblico: `https://ntfy.sh` (default)
- Server privato: `https://your-ntfy.domain.com`
- Server locale: `http://localhost:80`

## API Integration

Il sistema si integra con le API esistenti di PrivateCal:
- `useReminders()` composable per operazioni CRUD
- `reminderApi` service per chiamate HTTP
- Tipi TypeScript completi in `types/task.ts`

## Sicurezza

### Topic NTFY
- Generazione sicura: `privatecal-user-{userId}-{timestamp}-{random}`
- Non predictable da utenti esterni
- Cambio automatico se compromesso

### Permessi Browser
- Richiesta esplicita utente
- Gestione graceful dei rifiuti
- Fallback sempre disponibile

## Performance

### Ottimizzazioni
- Lazy loading componenti
- Debounce input utente
- Cache locale impostazioni
- Service Worker per background tasks
- Minimizzazione richieste API

### Bundle Size
- Import selettivi componenti
- Tree shaking Tailwind CSS
- Compressione SVG icone
- Code splitting per routes

## Accessibilità

### Conformità WCAG 2.1 AA
- Keyboard navigation completa
- Screen reader support
- High contrast mode
- Reduced motion support
- Focus indicators visibili
- ARIA labels appropriate

### Internazionalizzazione
- RTL support ready
- Numero/data formattazione
- Timezone handling
- Messaggi localizzati

## Browser Support

### Moderni (Full Support)
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

### Legacy (Fallback)
- Chrome 60-89
- Firefox 60-87
- Safari 12-13
- IE 11 (notifiche disabilitate)

## Troubleshooting

### Problemi Comuni

**Notifiche non funzionano:**
1. Verificare permessi browser
2. Testare connessione NTFY
3. Controllare console errori
4. Verificare service worker

**NTFY non riceve:**
1. Controllare URL server
2. Testare topic esistenza
3. Verificare firewall/proxy
4. Provare server alternativo

**Performance lente:**
1. Disabilitare notifiche test
2. Ridurre frequenza polling
3. Pulire cache browser
4. Aggiornare service worker

Per supporto dettagliato, consultare la documentazione API o aprire issue nel repository.