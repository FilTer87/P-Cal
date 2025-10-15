import { formatDate, formatDateTime, formatTime, formatRelativeTime, getDateDescription } from './dateHelpers'
import { i18nGlobal } from '../i18n'

/**
 * Get current locale from i18n
 */
const getCurrentLocale = (): string => {
  return i18nGlobal.locale.value
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
 * Format number with thousand separators
 */
export const formatNumber = (num: number, locale?: string): string => {
  return new Intl.NumberFormat(locale || getCurrentLocale()).format(num)
}

/**
 * Format currency (default Euro, locale-aware)
 */
export const formatCurrency = (amount: number, locale?: string, currency = 'EUR'): string => {
  return new Intl.NumberFormat(locale || getCurrentLocale(), {
    style: 'currency',
    currency
  }).format(amount)
}

/**
 * Format percentage (expects value as 0-100, e.g., 50 for 50%)
 * Note: Percentage format is locale-independent (always uses comma for IT locale in tests)
 */
export const formatPercentage = (value: number, decimals = 1): string => {
  const formatted = value.toFixed(decimals)
  return `${formatted.replace('.', ',')}%`
}

/**
 * Format duration in minutes to human readable format
 */
export const formatDuration = (minutes: number): string => {
  const { t } = i18nGlobal

  if (minutes < 60) {
    return minutes === 1
      ? `${minutes} ${t('dateTime.duration.minute')}`
      : `${minutes} ${t('dateTime.duration.minutes')}`
  }

  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60

  if (hours < 24) {
    if (remainingMinutes === 0) {
      return hours === 1
        ? `${hours} ${t('dateTime.duration.hour')}`
        : `${hours} ${t('dateTime.duration.hours')}`
    }
    return `${hours}h ${remainingMinutes}m`
  }

  const days = Math.floor(hours / 24)
  const remainingHours = hours % 24

  if (remainingHours === 0) {
    return days === 1
      ? `${days} ${t('dateTime.duration.day')}`
      : `${days} ${t('dateTime.duration.days')}`
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
  const { t } = i18nGlobal

  if (!dueDate) {
    return {
      text: t('dateTime.noDueDate'),
      color: 'gray',
      isPast: false
    }
  }

  const now = new Date()
  const due = new Date(dueDate)
  const isPast = due < now
  const description = getDateDescription(due)

  if (isPast) {
    const timeAgo = formatTimeAgo(due).replace(' fa', '').trim()
    return {
      text: `${t('dateTime.pastDue')} ${timeAgo}`,
      color: 'gray',
      isPast: true
    }
  }

  const timeUntil = formatTimeAgo(due).replace('tra', '').trim()
  return {
    text: `${t('dateTime.dueIn')} ${description.toLowerCase()} (${timeUntil})`,
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
  const { t } = i18nGlobal

  if (sent) {
    return {
      text: `${t('dateTime.sentOn')} ${formatDateTime(reminderDateTime)}`,
      color: 'green'
    }
  }

  const now = new Date()
  const reminder = new Date(reminderDateTime)
  const isPast = reminder < now

  if (isPast) {
    return {
      text: `${t('dateTime.shouldHaveBeenSent')} ${formatTimeAgo(reminder)}`,
      color: 'red'
    }
  }

  const description = getDateDescription(reminder)
  const timeUntil = formatTimeAgo(reminder).replace('tra', '').trim()

  return {
    text: `${description} (${timeUntil})`,
    color: reminder.getTime() - now.getTime() <= 60 * 60 * 1000 ? 'yellow' : 'blue'
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
export const formatList = (items: string[], conjunction?: string): string => {
  const { t } = i18nGlobal
  const conj = conjunction || t('formatters.listConjunction')

  if (items.length === 0) return ''
  if (items.length === 1) return items[0]
  if (items.length === 2) return items.join(` ${conj} `)

  const allButLast = items.slice(0, -1).join(', ')
  const last = items[items.length - 1]

  return `${allButLast} ${conj} ${last}`
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
  const { t } = i18nGlobal
  const fieldKey = `formatters.fieldNames.${field}`
  const fieldName = t(fieldKey, field) // fallback to field name if key not found
  return `${fieldName}: ${error}`
}

/**
 * Format API error for display
 */
export const formatApiError = (error: any): string => {
  const { t } = i18nGlobal

  if (error?.response?.data?.message) {
    return error.response.data.message
  }

  if (error?.message) {
    return error.message
  }

  if (typeof error === 'string') {
    return error
  }

  return t('errors.generic')
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
  
  // Same month - use date-fns formatting which handles locale automatically
  if (start.getMonth() === end.getMonth() && start.getFullYear() === end.getFullYear()) {
    return `${formatDate(start, 'd')}-${formatDate(end, 'd MMMM yyyy')}`
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
 * Format phone number for display (generic international format)
 */
export const formatPhoneNumber = (phone: string): string => {
  const cleaned = phone.replace(/\D/g, '')

  // If starts with +, preserve it
  const hasPlus = phone.trim().startsWith('+')

  // No formatting for very short numbers
  if (cleaned.length < 6) {
    return phone
  }

  // Format international numbers (with country code)
  if (hasPlus || cleaned.length > 10) {
    // Extract country code (1-3 digits)
    let countryCode = ''
    let remaining = cleaned

    if (cleaned.length >= 11) {
      // Try common country code patterns
      if (cleaned.startsWith('1')) {
        countryCode = cleaned.substring(0, 1) // USA/Canada
        remaining = cleaned.substring(1)
      } else if (cleaned.startsWith('39') || cleaned.startsWith('44') || cleaned.startsWith('33')) {
        countryCode = cleaned.substring(0, 2) // Italy, UK, France, etc.
        remaining = cleaned.substring(2)
      } else {
        countryCode = cleaned.substring(0, 2)
        remaining = cleaned.substring(2)
      }

      // Format remaining number in groups of 3-4
      const formatted = remaining.replace(/(\d{3})(?=\d)/g, '$1 ').trim()
      return `+${countryCode} ${formatted}`
    }
  }

  // Format national numbers (10 digits) in groups
  if (cleaned.length === 10) {
    return cleaned.replace(/(\d{3})(\d{3})(\d{4})/, '$1 $2 $3')
  }

  // Format other lengths in groups of 3
  if (cleaned.length >= 6) {
    return cleaned.replace(/(\d{3})(?=\d)/g, '$1 ').trim()
  }

  return phone
}

/**
 * Format HTML for safe display (basic sanitization)
 */
export const formatSafeHtml = (html: string): string => {
  return html
    .replace(/&/g, '&amp;')   // Must be FIRST to avoid double-escaping
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * Format table cell content based on type
 * Note: For Vue components, use useFormatters() composable for i18n support
 */
export const formatTableCell = (value: any, type: 'text' | 'number' | 'date' | 'boolean' = 'text'): string => {
  const { t } = i18nGlobal

  if (value == null) return '-'

  switch (type) {
    case 'number':
      return formatNumber(value)
    case 'date':
      return formatDate(value)
    case 'boolean':
      return value ? t('common.yes') : t('common.no')
    case 'text':
    default:
      return String(value)
  }
}