export interface Task {
  id: number
  occurrenceId?: string // Unique ID for recurring task occurrences (format: "taskId-timestamp")
  title: string
  description?: string
  startDatetime: string
  endDatetime: string
  location?: string
  color: string
  recurrenceRule?: string
  recurrenceEnd?: string
  isRecurring: boolean
  createdAt: string
  updatedAt: string
  userId: number
  reminders: Reminder[]
  // Temporary properties for calendar views
  _splitIndex?: string | number  // String for date-based splits, number for index-based splits
  _visualStartTime?: string
  _visualEndTime?: string
}

export interface CreateTaskRequest {
  title: string
  description?: string
  startDatetime: string
  endDatetime: string
  location?: string
  color?: string
  recurrenceRule?: string
  recurrenceEnd?: string
  reminders?: CreateReminderRequest[]
}

export interface UpdateTaskRequest {
  title?: string
  description?: string
  startDatetime?: string
  endDatetime?: string
  location?: string
  color?: string
  recurrenceRule?: string
  recurrenceEnd?: string
  reminders?: CreateReminderRequest[]
}

export interface Reminder {
  id: number
  taskId: number
  taskTitle?: string
  reminderTime: string
  reminderOffsetMinutes: number
  isSent: boolean
  sent?: boolean // for backward compatibility
  notificationType: NotificationType
  createdAt: string
  isDue?: boolean
  isOverdue?: boolean
  description?: string
  minutesUntilDue?: number
  formattedTimeUntilDue?: string
  pushNotification?: boolean
  emailNotification?: boolean
  status?: string
  active?: boolean
}

export interface CreateReminderRequest {
  id?: number  // Optional: if present, updates existing reminder; if absent, creates new one
  reminderOffsetMinutes: number
  notificationType?: NotificationType
}

export interface UpdateReminderRequest {
  reminderOffsetMinutes?: number
  notificationType?: NotificationType
  sent?: boolean
}

export enum NotificationType {
  PUSH = 'PUSH',
  EMAIL = 'EMAIL'
}

export enum RecurrenceFrequency {
  DAILY = 'DAILY',
  WEEKLY = 'WEEKLY',
  MONTHLY = 'MONTHLY',
  YEARLY = 'YEARLY'
}

export enum RecurrenceEndType {
  NEVER = 'NEVER',
  COUNT = 'COUNT',
  DATE = 'DATE'
}

export interface TaskFilters {
  startDateFrom?: string
  startDateTo?: string
  search?: string
}

export interface TaskFormData {
  title: string
  description: string
  startDate: string
  startTime: string
  endDate: string
  endTime: string
  location: string
  color: string
  isRecurring: boolean
  recurrenceFrequency?: RecurrenceFrequency
  recurrenceInterval?: number
  recurrenceEndType?: RecurrenceEndType
  recurrenceCount?: number
  recurrenceEndDate?: string
  recurrenceByDay?: string[]
  reminders: ReminderFormData[]
}

export interface ReminderFormData {
  id?: number
  offsetMinutes: number
  offsetValue?: number
  offsetUnit?: string
  reminderOffsetMinutes?: number
  notificationType: NotificationType
  label?: string
}

export interface TaskStats {
  total: number
  today: number
  thisWeek: number
}

export interface DailyTasks {
  [date: string]: Task[]
}

// Notification type display configuration
export interface NotificationTypeConfig {
  label: string
  icon: string
}

export const NOTIFICATION_TYPE_CONFIG: Record<NotificationType, NotificationTypeConfig> = {
  [NotificationType.PUSH]: {
    label: 'Notifica Push',
    icon: '🔔'
  },
  [NotificationType.EMAIL]: {
    label: 'Email',
    icon: '📧'
  }
}

// Common reminder presets (in minutes before event)
export const REMINDER_PRESETS = [
  { label: '5 minuti prima', minutes: 5 },
  { label: '10 minuti prima', minutes: 10 },
  { label: '15 minuti prima', minutes: 15 },
  { label: '30 minuti prima', minutes: 30 },
  { label: '1 ora prima', minutes: 60 },
  { label: '2 ore prima', minutes: 120 },
  { label: '1 giorno prima', minutes: 24 * 60 },
  { label: '1 settimana prima', minutes: 7 * 24 * 60 }
]

// Calendar color options for tasks
export interface CalendarColor {
  name: string
  value: string
  bgColor: string
  textColor: string
  borderColor: string
}

export const CALENDAR_COLORS: CalendarColor[] = [
  { name: 'Blu', value: '#3b82f6', bgColor: 'bg-blue-100 dark:bg-blue-900/20', textColor: 'text-blue-800 dark:text-blue-200', borderColor: 'border-blue-500' },
  { name: 'Verde', value: '#10b981', bgColor: 'bg-emerald-100 dark:bg-emerald-900/20', textColor: 'text-emerald-800 dark:text-emerald-200', borderColor: 'border-emerald-500' },
  { name: 'Rosso', value: '#ef4444', bgColor: 'bg-red-100 dark:bg-red-900/20', textColor: 'text-red-800 dark:text-red-200', borderColor: 'border-red-500' },
  { name: 'Giallo', value: '#f59e0b', bgColor: 'bg-amber-100 dark:bg-amber-900/20', textColor: 'text-amber-800 dark:text-amber-200', borderColor: 'border-amber-500' },
  { name: 'Viola', value: '#8b5cf6', bgColor: 'bg-violet-100 dark:bg-violet-900/20', textColor: 'text-violet-800 dark:text-violet-200', borderColor: 'border-violet-500' },
  { name: 'Rosa', value: '#ec4899', bgColor: 'bg-pink-100 dark:bg-pink-900/20', textColor: 'text-pink-800 dark:text-pink-200', borderColor: 'border-pink-500' },
  { name: 'Indaco', value: '#6366f1', bgColor: 'bg-indigo-100 dark:bg-indigo-900/20', textColor: 'text-indigo-800 dark:text-indigo-200', borderColor: 'border-indigo-500' },
  { name: 'Teal', value: '#14b8a6', bgColor: 'bg-teal-100 dark:bg-teal-900/20', textColor: 'text-teal-800 dark:text-teal-200', borderColor: 'border-teal-500' },
  { name: 'Arancione', value: '#f97316', bgColor: 'bg-orange-100 dark:bg-orange-900/20', textColor: 'text-orange-800 dark:text-orange-200', borderColor: 'border-orange-500' },
  { name: 'Grigio', value: '#6b7280', bgColor: 'bg-gray-100 dark:bg-gray-900/20', textColor: 'text-gray-800 dark:text-gray-200', borderColor: 'border-gray-500' }
]

// Task view modes
export type TaskViewMode = 'calendar' | 'list' | 'kanban'

// Task sorting options
export interface TaskSortOption {
  key: string
  label: string
  value: keyof Task | 'custom'
}

export const TASK_SORT_OPTIONS: TaskSortOption[] = [
  { key: 'startDatetime', label: 'Data di inizio', value: 'startDatetime' },
  { key: 'endDatetime', label: 'Data di fine', value: 'endDatetime' },
  { key: 'title', label: 'Titolo', value: 'title' },
  { key: 'createdAt', label: 'Data di creazione', value: 'createdAt' }
]

// Validation messages in Italian
export const VALIDATION_MESSAGES = {
  required: 'Questo campo è obbligatorio',
  minLength: (min: number) => `Minimo ${min} caratteri richiesti`,
  maxLength: (max: number) => `Massimo ${max} caratteri consentiti`,
  invalidDate: 'Data non valida',
  invalidTime: 'Ora non valida (formato HH:MM)',
  invalidColor: 'Colore non valido',
  endBeforeStart: 'La data di fine deve essere successiva alla data di inizio',
  pastDate: 'La data non può essere nel passato',
  conflict: 'Conflitto con un altro evento'
}