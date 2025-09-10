export interface Task {
  id: number
  title: string
  description?: string
  completed: boolean
  priority: TaskPriority
  dueDate?: string
  startDate?: string
  endDate?: string
  location?: string
  color?: string
  isAllDay?: boolean
  createdAt: string
  updatedAt: string
  userId: number
  reminders: Reminder[]
}

export interface CreateTaskRequest {
  title: string
  description?: string
  priority: TaskPriority
  dueDate?: string
  startDate?: string
  endDate?: string
  location?: string
  color?: string
  isAllDay?: boolean
  reminders?: CreateReminderRequest[]
}

export interface UpdateTaskRequest {
  title?: string
  description?: string
  completed?: boolean
  priority?: TaskPriority
  dueDate?: string
  startDate?: string
  endDate?: string
  location?: string
  color?: string
  isAllDay?: boolean
}

export interface Reminder {
  id: number
  taskId: number
  reminderDateTime: string
  sent: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateReminderRequest {
  reminderDateTime: string
}

export interface UpdateReminderRequest {
  reminderDateTime?: string
  sent?: boolean
}

export enum TaskPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

export interface TaskFilters {
  completed?: boolean
  priority?: TaskPriority
  dueDateFrom?: string
  dueDateTo?: string
  search?: string
}

export interface TaskFormData {
  title: string
  description: string
  priority: TaskPriority
  dueDate: string
  dueTime: string
  startDate: string
  startTime: string
  endDate: string
  endTime: string
  location: string
  color: string
  isAllDay: boolean
  reminders: ReminderFormData[]
}

export interface ReminderFormData {
  id?: number
  date: string
  time: string
  reminderDateTime?: string
}

export interface TaskStats {
  total: number
  completed: number
  pending: number
  overdue: number
  today: number
  thisWeek: number
}

export interface DailyTasks {
  [date: string]: Task[]
}

// Task priority display configuration
export interface TaskPriorityConfig {
  label: string
  color: string
  bgColor: string
  borderColor: string
  icon?: string
}

export const TASK_PRIORITY_CONFIG: Record<TaskPriority, TaskPriorityConfig> = {
  [TaskPriority.LOW]: {
    label: 'Bassa',
    color: 'text-green-600 dark:text-green-400',
    bgColor: 'bg-green-50 dark:bg-green-900/20',
    borderColor: 'border-green-500'
  },
  [TaskPriority.MEDIUM]: {
    label: 'Media',
    color: 'text-yellow-600 dark:text-yellow-400',
    bgColor: 'bg-yellow-50 dark:bg-yellow-900/20',
    borderColor: 'border-yellow-500'
  },
  [TaskPriority.HIGH]: {
    label: 'Alta',
    color: 'text-orange-600 dark:text-orange-400',
    bgColor: 'bg-orange-50 dark:bg-orange-900/20',
    borderColor: 'border-orange-500'
  },
  [TaskPriority.URGENT]: {
    label: 'Urgente',
    color: 'text-red-600 dark:text-red-400',
    bgColor: 'bg-red-50 dark:bg-red-900/20',
    borderColor: 'border-red-500'
  }
}

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
  { key: 'dueDate', label: 'Data di scadenza', value: 'dueDate' },
  { key: 'priority', label: 'Priorità', value: 'priority' },
  { key: 'title', label: 'Titolo', value: 'title' },
  { key: 'createdAt', label: 'Data di creazione', value: 'createdAt' },
  { key: 'completed', label: 'Stato', value: 'completed' }
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