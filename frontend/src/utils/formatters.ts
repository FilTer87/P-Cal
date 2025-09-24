import { formatDate, formatDateTime, formatTime, formatRelativeTime, getDateDescription } from './dateHelpers'
import type { TaskPriority } from '../types/task'
import { TASK_PRIORITIES, LOCALE_STRINGS } from './constants'

/**
 * Format task priority for display
 */
export const formatTaskPriority = (priority: TaskPriority): string => {
  const priorityConfig = TASK_PRIORITIES.find(p => p.value === priority)
  return priorityConfig?.label || priority
}


/**
 * Format file size in human readable format
 */
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * Format number with thousand separators (Italian format)
 */
export const formatNumber = (num: number): string => {
  return new Intl.NumberFormat('it-IT').format(num)
}

/**
 * Format currency (Euro)
 */
export const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat('it-IT', {
    style: 'currency',
    currency: 'EUR'
  }).format(amount)
}

/**
 * Format percentage
 */
export const formatPercentage = (value: number, decimals = 1): string => {
  return new Intl.NumberFormat('it-IT', {
    style: 'percent',
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  }).format(value / 100)
}

/**
 * Format duration in minutes to human readable format
 */
export const formatDuration = (minutes: number): string => {
  if (minutes < 60) {
    return `${minutes} ${minutes === 1 ? 'minuto' : 'minuti'}`
  }
  
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  
  if (hours < 24) {
    if (remainingMinutes === 0) {
      return `${hours} ${hours === 1 ? 'ora' : 'ore'}`
    }
    return `${hours}h ${remainingMinutes}m`
  }
  
  const days = Math.floor(hours / 24)
  const remainingHours = hours % 24
  
  if (remainingHours === 0) {
    return `${days} ${days === 1 ? 'giorno' : 'giorni'}`
  }
  
  return `${days}g ${remainingHours}h`
}

/**
 * Format time ago/until in Italian
 */
export const formatTimeAgo = (date: Date | string): string => {
  return formatRelativeTime(date)
}

/**
 * Format task due date with context
 */
export const formatTaskDueDate = (dueDate: string): {
  text: string
  color: 'green' | 'blue' | 'yellow' | 'red' | 'gray'
  isPast: boolean
} => {
  if (!dueDate) {
    return {
      text: 'Nessuna scadenza',
      color: 'gray',
      isPast: false
    }
  }

  const now = new Date()
  const due = new Date(dueDate)
  const isPast = due < now
  const description = getDateDescription(due)

  if (isPast) {
    const timeAgo = formatTimeAgo(due).replace('fa', 'fa')
    return {
      text: `Passato di ${timeAgo}`,
      color: 'gray',
      isPast: true
    }
  }

  const timeUntil = formatTimeAgo(due).replace('tra', '').trim()
  return {
    text: `Scade ${description.toLowerCase()} (${timeUntil})`,
    color: due.getTime() - now.getTime() <= 24 * 60 * 60 * 1000 ? 'yellow' : 'blue',
    isPast: false
  }
}

/**
 * Format reminder time with context
 */
export const formatReminderTime = (reminderDateTime: string, sent = false): {
  text: string
  color: 'green' | 'blue' | 'yellow' | 'red' | 'gray'
} => {
  if (sent) {
    return {
      text: `Inviato il ${formatDateTime(reminderDateTime)}`,
      color: 'green'
    }
  }
  
  const now = new Date()
  const reminder = new Date(reminderDateTime)
  const isPast = reminder < now
  
  if (isPast) {
    return {
      text: `Doveva essere inviato ${formatTimeAgo(reminder)}`,
      color: 'red'
    }
  }
  
  const description = getDateDescription(reminder)
  const timeUntil = formatTimeAgo(reminder).replace('tra', '').trim()
  
  return {
    text: `${description} (${timeUntil})`,
    color: reminder.getTime() - now.getTime() <= 60 * 60 * 1000 ? 'yellow' : 'blue' // 1 hour threshold
  }
}

/**
 * Format user name with fallbacks
 */
export const formatUserName = (user: {
  firstName?: string
  lastName?: string
  username: string
}): string => {
  const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ')
  return fullName || user.username
}

/**
 * Format user initials
 */
export const formatUserInitials = (user: {
  firstName?: string
  lastName?: string
  username: string
}): string => {
  if (user.firstName && user.lastName) {
    return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase()
  }
  
  if (user.firstName) {
    return user.firstName.slice(0, 2).toUpperCase()
  }
  
  return user.username.slice(0, 2).toUpperCase()
}

/**
 * Format text for display with length limit
 */
export const formatText = (text: string, maxLength = 100): string => {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength - 3) + '...'
}

/**
 * Format list of items with proper conjunctions
 */
export const formatList = (items: string[], conjunction = 'e'): string => {
  if (items.length === 0) return ''
  if (items.length === 1) return items[0]
  if (items.length === 2) return items.join(` ${conjunction} `)
  
  const allButLast = items.slice(0, -1).join(', ')
  const last = items[items.length - 1]
  
  return `${allButLast} ${conjunction} ${last}`
}

/**
 * Format search query highlighting
 */
export const formatSearchHighlight = (text: string, query: string): string => {
  if (!query || !text) return text
  
  const regex = new RegExp(`(${query})`, 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

/**
 * Format validation error message
 */
export const formatValidationError = (field: string, error: string): string => {
  const fieldNames: Record<string, string> = {
    title: 'Titolo',
    description: 'Descrizione',
    priority: 'Priorità',
    dueDate: 'Data di scadenza',
    dueTime: 'Orario di scadenza',
    username: 'Nome utente',
    email: 'Email',
    password: 'Password',
    confirmPassword: 'Conferma password',
    firstName: 'Nome',
    lastName: 'Cognome'
  }
  
  const fieldName = fieldNames[field] || field
  return `${fieldName}: ${error}`
}

/**
 * Format API error for display
 */
export const formatApiError = (error: any): string => {
  if (error?.response?.data?.message) {
    return error.response.data.message
  }
  
  if (error?.message) {
    return error.message
  }
  
  if (typeof error === 'string') {
    return error
  }
  
  return 'Si è verificato un errore imprevisto'
}

/**
 * Format calendar event for display
 */
export const formatCalendarEvent = (task: {
  title: string
  priority: TaskPriority
  dueDate?: string
}): {
  title: string
  className: string
  color: string
} => {
  const priorityConfig = TASK_PRIORITIES.find(p => p.value === task.priority)

  return {
    title: task.title,
    className: `priority-${task.priority.toLowerCase()}`,
    color: priorityConfig?.color.includes('red') ? '#ef4444' :
           priorityConfig?.color.includes('orange') ? '#f97316' :
           priorityConfig?.color.includes('yellow') ? '#eab308' : '#3b82f6'
  }
}

/**
 * Format date range for display
 */
export const formatDateRange = (startDate: Date | string, endDate: Date | string): string => {
  const start = typeof startDate === 'string' ? new Date(startDate) : startDate
  const end = typeof endDate === 'string' ? new Date(endDate) : endDate
  
  // Same day
  if (formatDate(start, 'yyyy-MM-dd') === formatDate(end, 'yyyy-MM-dd')) {
    return `${formatDate(start)} ${formatTime(start)}-${formatTime(end)}`
  }
  
  // Same month
  if (start.getMonth() === end.getMonth() && start.getFullYear() === end.getFullYear()) {
    return `${formatDate(start, 'd')}-${formatDate(end)} ${LOCALE_STRINGS.months[start.getMonth()]} ${start.getFullYear()}`
  }
  
  // Same year
  if (start.getFullYear() === end.getFullYear()) {
    return `${formatDate(start, 'd MMM')} - ${formatDate(end, 'd MMM yyyy')}`
  }
  
  // Different years
  return `${formatDate(start)} - ${formatDate(end)}`
}

/**
 * Format statistics for display
 */
export const formatStatistic = (value: number, total: number, label: string): {
  value: string
  percentage: string
  label: string
} => {
  const percentage = total > 0 ? (value / total) * 100 : 0
  
  return {
    value: formatNumber(value),
    percentage: formatPercentage(percentage),
    label
  }
}

/**
 * Format keyboard shortcut for display
 */
export const formatKeyboardShortcut = (shortcut: {
  key: string
  ctrl?: boolean
  alt?: boolean
  shift?: boolean
}): string => {
  const keys: string[] = []
  
  if (shortcut.ctrl) keys.push('Ctrl')
  if (shortcut.alt) keys.push('Alt')
  if (shortcut.shift) keys.push('Shift')
  
  keys.push(shortcut.key)
  
  return keys.join(' + ')
}

/**
 * Format breadcrumb path
 */
export const formatBreadcrumb = (path: string[]): string => {
  return path.join(' > ')
}

/**
 * Format notification message with interpolation
 */
export const formatNotificationMessage = (template: string, data: Record<string, any>): string => {
  return template.replace(/\{\{(\w+)\}\}/g, (match, key) => {
    return data[key]?.toString() || match
  })
}

/**
 * Format URL slug from text
 */
export const formatSlug = (text: string): string => {
  return text
    .toLowerCase()
    .replace(/[àáâäãåą]/g, 'a')
    .replace(/[èéêë]/g, 'e')
    .replace(/[ìíîï]/g, 'i')
    .replace(/[òóôöõø]/g, 'o')
    .replace(/[ùúûü]/g, 'u')
    .replace(/[ç]/g, 'c')
    .replace(/[ñ]/g, 'n')
    .replace(/[^a-z0-9]/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
}

/**
 * Format phone number for display (Italian format)
 */
export const formatPhoneNumber = (phone: string): string => {
  const cleaned = phone.replace(/\D/g, '')
  
  if (cleaned.startsWith('39')) {
    // Italian international format
    const national = cleaned.substring(2)
    if (national.length === 10) {
      return `+39 ${national.substring(0, 3)} ${national.substring(3, 6)} ${national.substring(6)}`
    }
  }
  
  if (cleaned.length === 10) {
    // Italian national format
    return `${cleaned.substring(0, 3)} ${cleaned.substring(3, 6)} ${cleaned.substring(6)}`
  }
  
  return phone // Return original if format is unknown
}

/**
 * Format HTML for safe display (basic sanitization)
 */
export const formatSafeHtml = (html: string): string => {
  return html
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/&/g, '&amp;')
}

/**
 * Format table cell content based on type
 */
export const formatTableCell = (value: any, type: 'text' | 'number' | 'date' | 'boolean' | 'priority' = 'text'): string => {
  if (value == null) return '-'
  
  switch (type) {
    case 'number':
      return formatNumber(value)
    case 'date':
      return formatDate(value)
    case 'boolean':
      return value ? 'Sì' : 'No'
    case 'priority':
      return formatTaskPriority(value)
    case 'text':
    default:
      return String(value)
  }
}