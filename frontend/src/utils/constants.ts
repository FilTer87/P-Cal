import type { TaskPriority } from '../types/task'
import type { CalendarView } from '../types/calendar'

/**
 * Application metadata
 */
export const APP_INFO = {
  name: 'P-Cal',
  version: '2.0.0',
  description: 'Sistema di gestione attività e calendario personale',
  author: 'Filippo Terenzi',
  website: '' // TODO
} as const

/**
 * API Configuration
 */
export const API_CONFIG = {
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 10000,
  retryAttempts: 3,
  retryDelay: 1000
} as const

/**
 * Authentication constants
 */
export const AUTH_CONFIG = {
  tokenKey: 'accessToken',
  refreshTokenKey: 'refreshToken',
  userKey: 'user',
  expiresAtKey: 'tokenExpiresAt',
  themeKey: 'theme',
  calendarViewKey: 'calendarView',
  refreshThreshold: 5 * 60 * 1000, // 5 minutes before expiry
  sessionTimeout: 30 * 60 * 1000 // 30 minutes
} as const

/**
 * Task priorities
 */
export const TASK_PRIORITIES: { value: TaskPriority; label: string; color: string; bgColor: string; borderColor: string }[] = [
  {
    value: 'LOW',
    label: 'Bassa',
    color: 'text-green-600 dark:text-green-400',
    bgColor: 'bg-green-50 dark:bg-green-900/20',
    borderColor: 'border-green-500'
  },
  {
    value: 'MEDIUM',
    label: 'Media',
    color: 'text-yellow-600 dark:text-yellow-400',
    bgColor: 'bg-yellow-50 dark:bg-yellow-900/20',
    borderColor: 'border-yellow-500'
  },
  {
    value: 'HIGH',
    label: 'Alta',
    color: 'text-orange-600 dark:text-orange-400',
    bgColor: 'bg-orange-50 dark:bg-orange-900/20',
    borderColor: 'border-orange-500'
  },
  {
    value: 'URGENT',
    label: 'Urgente',
    color: 'text-red-600 dark:text-red-400',
    bgColor: 'bg-red-50 dark:bg-red-900/20',
    borderColor: 'border-red-500'
  }
] as const

/**
 * Task priority colors mapping
 */
export const TASK_PRIORITY_COLORS: Record<TaskPriority, { light: string; dark: string; bg: string; border: string }> = {
  LOW: {
    light: 'text-green-600',
    dark: 'dark:text-green-400',
    bg: 'bg-green-50 dark:bg-green-900/20',
    border: 'border-green-500'
  },
  MEDIUM: {
    light: 'text-yellow-600',
    dark: 'dark:text-yellow-400',
    bg: 'bg-yellow-50 dark:bg-yellow-900/20',
    border: 'border-yellow-500'
  },
  HIGH: {
    light: 'text-orange-600',
    dark: 'dark:text-orange-400',
    bg: 'bg-orange-50 dark:bg-orange-900/20',
    border: 'border-orange-500'
  },
  URGENT: {
    light: 'text-red-600',
    dark: 'dark:text-red-400',
    bg: 'bg-red-50 dark:bg-red-900/20',
    border: 'border-red-500'
  }
} as const

/**
 * Calendar views
 */
export const CALENDAR_VIEWS: { value: CalendarView; label: string; icon: string; shortcut: string }[] = [
  {
    value: 'month',
    label: 'Mese',
    icon: 'CalendarIcon',
    shortcut: 'M'
  },
  {
    value: 'week',
    label: 'Settimana',
    icon: 'CalendarDaysIcon',
    shortcut: 'W'
  },
  {
    value: 'day',
    label: 'Giorno',
    icon: 'ClockIcon',
    shortcut: 'D'
  },
  {
    value: 'agenda',
    label: 'Agenda',
    icon: 'ListBulletIcon',
    shortcut: 'A'
  }
] as const

/**
 * Theme options
 */
export const THEME_OPTIONS = [
  { value: 'light', label: 'Chiaro', icon: 'SunIcon' },
  { value: 'dark', label: 'Scuro', icon: 'MoonIcon' },
  { value: 'system', label: 'Sistema', icon: 'ComputerDesktopIcon' }
] as const

/**
 * Date and time formats
 */
export const DATE_FORMATS = {
  display: 'dd/MM/yyyy',
  displayWithTime: 'dd/MM/yyyy HH:mm',
  input: 'yyyy-MM-dd',
  time: 'HH:mm',
  iso: 'yyyy-MM-dd\'T\'HH:mm:ss',
  api: 'yyyy-MM-dd\'T\'HH:mm:ss.SSSxxx',
  relative: 'relative',
  calendar: 'd MMMM yyyy',
  monthYear: 'MMMM yyyy',
  dayName: 'EEEE',
  dayNameShort: 'EEE',
  monthName: 'MMMM',
  monthNameShort: 'MMM'
} as const

/**
 * Localization settings
 */
export const LOCALE_CONFIG = {
  code: 'it-IT',
  language: 'it',
  country: 'IT',
  currency: 'EUR',
  timezone: 'Europe/Rome',
  dateFormat: 'dd/MM/yyyy',
  timeFormat: 'HH:mm',
  // firstDayOfWeek now managed by settings store
  weekendDays: [0, 6] // Sunday, Saturday
} as const

/**
 * Italian locale strings
 * Note: weekdaysShort and weekdaysMin are now generated dynamically based on user settings
 */
export const LOCALE_STRINGS = {
  months: [
    'Gennaio', 'Febbraio', 'Marzo', 'Aprile', 'Maggio', 'Giugno',
    'Luglio', 'Agosto', 'Settembre', 'Ottobre', 'Novembre', 'Dicembre'
  ],
  monthsShort: [
    'Gen', 'Feb', 'Mar', 'Apr', 'Mag', 'Giu',
    'Lug', 'Ago', 'Set', 'Ott', 'Nov', 'Dic'
  ],
  weekdays: [
    'Domenica', 'Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato'
  ],
  today: 'Oggi',
  yesterday: 'Ieri',
  tomorrow: 'Domani',
  thisWeek: 'Questa settimana',
  lastWeek: 'Settimana scorsa',
  nextWeek: 'Settimana prossima',
  thisMonth: 'Questo mese',
  lastMonth: 'Mese scorso',
  nextMonth: 'Mese prossimo',
  thisYear: 'Quest\'anno',
  lastYear: 'Anno scorso',
  nextYear: 'Anno prossimo'
} as const

/**
 * Reminder presets (in minutes before due date)
 */
export const REMINDER_PRESETS = [
  { id: '5min', label: '5 minuti prima', minutes: 5 },
  { id: '15min', label: '15 minuti prima', minutes: 15 },
  { id: '30min', label: '30 minuti prima', minutes: 30 },
  { id: '1hour', label: '1 ora prima', minutes: 60 },
  { id: '2hours', label: '2 ore prima', minutes: 120 },
  { id: '4hours', label: '4 ore prima', minutes: 240 },
  { id: '1day', label: '1 giorno prima', minutes: 1440 },
  { id: '2days', label: '2 giorni prima', minutes: 2880 },
  { id: '1week', label: '1 settimana prima', minutes: 10080 }
] as const

/**
 * Default reminder times
 */
export const DEFAULT_REMINDER_OFFSETS = [30, 60, 1440] // 30 minutes, 1 hour, 1 day

/**
 * Quick time options for task due times
 */
export const QUICK_TIME_OPTIONS = [
  { label: '09:00', value: '09:00' },
  { label: '10:00', value: '10:00' },
  { label: '11:00', value: '11:00' },
  { label: '12:00', value: '12:00' },
  { label: '13:00', value: '13:00' },
  { label: '14:00', value: '14:00' },
  { label: '15:00', value: '15:00' },
  { label: '16:00', value: '16:00' },
  { label: '17:00', value: '17:00' },
  { label: '18:00', value: '18:00' }
] as const

/**
 * Pagination settings
 */
export const PAGINATION = {
  defaultPageSize: 20,
  pageSizeOptions: [10, 20, 50, 100],
  maxPageSize: 100
} as const

/**
 * UI Constants
 */
export const UI_CONSTANTS = {
  sidebarWidth: '320px',
  sidebarMinWidth: '280px',
  sidebarMaxWidth: '400px',
  headerHeight: '64px',
  footerHeight: '40px',
  modalMaxWidth: '600px',
  mobileBreakpoint: '768px',
  tabletBreakpoint: '1024px',
  desktopBreakpoint: '1280px'
} as const

/**
 * Animation durations (in milliseconds)
 */
export const ANIMATION_DURATION = {
  fast: 150,
  normal: 250,
  slow: 350,
  transition: 200,
  modal: 300,
  toast: 250,
  loading: 1000
} as const

/**
 * Z-index layers
 */
export const Z_INDEX = {
  base: 0,
  dropdown: 1000,
  sticky: 1020,
  fixed: 1030,
  modalBackdrop: 1040,
  modal: 1050,
  popover: 1060,
  tooltip: 1070,
  toast: 1080,
  loading: 1090
} as const

/**
 * Keyboard shortcuts
 */
export const KEYBOARD_SHORTCUTS = {
  // Navigation
  today: { key: 'T', ctrl: true, description: 'Vai a oggi' },
  previousPeriod: { key: 'ArrowLeft', ctrl: true, description: 'Periodo precedente' },
  nextPeriod: { key: 'ArrowRight', ctrl: true, description: 'Periodo successivo' },
  
  // Views
  monthView: { key: 'M', description: 'Vista mese' },
  weekView: { key: 'W', description: 'Vista settimana' },
  dayView: { key: 'D', description: 'Vista giorno' },
  agendaView: { key: 'A', description: 'Vista agenda' },
  
  // Actions
  newTask: { key: 'N', ctrl: true, description: 'Nuova attività' },
  search: { key: 'F', ctrl: true, description: 'Cerca' },
  save: { key: 'S', ctrl: true, description: 'Salva' },
  cancel: { key: 'Escape', description: 'Annulla/Chiudi' },
  
  // Task actions
  completeTask: { key: 'Enter', ctrl: true, description: 'Completa attività' },
  editTask: { key: 'E', description: 'Modifica attività' },
  deleteTask: { key: 'Delete', description: 'Elimina attività' }
} as const

/**
 * Error messages
 */
export const ERROR_MESSAGES = {
  network: 'Errore di connessione. Verifica la tua connessione internet.',
  unauthorized: 'Accesso non autorizzato. Effettua nuovamente il login.',
  forbidden: 'Accesso negato.',
  notFound: 'Risorsa non trovata.',
  serverError: 'Errore interno del server. Riprova più tardi.',
  validation: 'Dati non validi. Verifica i campi e riprova.',
  timeout: 'Richiesta scaduta. Riprova più tardi.',
  unknown: 'Si è verificato un errore imprevisto.'
} as const

/**
 * Success messages
 */
export const SUCCESS_MESSAGES = {
  taskCreated: 'Attività creata con successo!',
  taskUpdated: 'Attività aggiornata con successo!',
  taskDeleted: 'Attività eliminata con successo!',
  taskCompleted: 'Attività completata!',
  reminderSet: 'Promemoria impostato!',
  loginSuccess: 'Accesso effettuato con successo!',
  logoutSuccess: 'Disconnesso con successo!',
  profileUpdated: 'Profilo aggiornato con successo!',
  settingsSaved: 'Impostazioni salvate!'
} as const

/**
 * Validation constraints
 */
export const VALIDATION_CONSTRAINTS = {
  task: {
    titleMinLength: 1,
    titleMaxLength: 255,
    descriptionMaxLength: 1000
  },
  user: {
    usernameMinLength: 3,
    usernameMaxLength: 50,
    passwordMinLength: 8,
    passwordMaxLength: 100,
    nameMaxLength: 100,
    emailMaxLength: 255
  }
} as const

/**
 * File upload constraints
 */
export const UPLOAD_CONSTRAINTS = {
  maxFileSize: 10 * 1024 * 1024, // 10MB
  allowedTypes: ['image/jpeg', 'image/png', 'image/gif', 'application/pdf'],
  allowedExtensions: ['.jpg', '.jpeg', '.png', '.gif', '.pdf']
} as const

/**
 * Cache settings
 */
export const CACHE_SETTINGS = {
  tasksCacheDuration: 5 * 60 * 1000, // 5 minutes
  userCacheDuration: 15 * 60 * 1000, // 15 minutes
  remindersCacheDuration: 2 * 60 * 1000, // 2 minutes
  maxCacheSize: 100 // Maximum number of cached items
} as const

/**
 * Feature flags
 */
export const FEATURES = {
  notifications: true,
  reminders: true,
  darkMode: true,
  exportData: true,
  importData: true,
  bulkOperations: true,
  dragAndDrop: true,
  keyboardShortcuts: true,
  offlineMode: false, // Future feature
  collaboration: false // Future feature
} as const

/**
 * External service URLs
 */
export const EXTERNAL_URLS = {
  documentation: 'https://docs.privatecal.app',
  support: 'https://support.privatecal.app',
  privacy: 'https://privatecal.app/privacy',
  terms: 'https://privatecal.app/terms',
  github: 'https://github.com/privatecal/privatecal',
  feedback: 'https://privatecal.app/feedback'
} as const

/**
 * Default settings
 */
export const DEFAULT_SETTINGS = {
  theme: 'system' as const,
  calendarView: 'month' as const,
  startOfWeek: 1, // Monday
  timeFormat: '24h' as const,
  notifications: true,
  emailNotifications: true,
  reminderSound: true,
  autoSave: true,
  pageSize: 20
} as const