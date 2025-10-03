import { useI18n } from 'vue-i18n'
import { formatDate, formatDateTime, formatTime, formatRelativeTime, getDateDescription } from '../utils/dateHelpers'

/**
 * Composable for i18n-aware formatters
 * Use this in Vue components for locale-aware formatting
 */
export function useFormatters() {
  const { t, locale } = useI18n()

  const formatDuration = (minutes: number): string => {
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

  const formatTaskDueDate = (dueDate: string): {
    text: string
    color: 'green' | 'blue' | 'yellow' | 'red' | 'gray'
    isPast: boolean
  } => {
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
      const timeAgo = formatRelativeTime(due).replace(' fa', '').trim()
      return {
        text: `${t('dateTime.pastDue')} ${timeAgo}`,
        color: 'gray',
        isPast: true
      }
    }

    const timeUntil = formatRelativeTime(due).replace('tra', '').trim()
    return {
      text: `${t('dateTime.dueIn')} ${description.toLowerCase()} (${timeUntil})`,
      color: due.getTime() - now.getTime() <= 24 * 60 * 60 * 1000 ? 'yellow' : 'blue',
      isPast: false
    }
  }

  const formatReminderTime = (reminderDateTime: string, sent = false): {
    text: string
    color: 'green' | 'blue' | 'yellow' | 'red' | 'gray'
  } => {
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
        text: `${t('dateTime.shouldHaveBeenSent')} ${formatRelativeTime(reminder)}`,
        color: 'red'
      }
    }

    const description = getDateDescription(reminder)
    const timeUntil = formatRelativeTime(reminder).replace('tra', '').trim()

    return {
      text: `${description} (${timeUntil})`,
      color: reminder.getTime() - now.getTime() <= 60 * 60 * 1000 ? 'yellow' : 'blue'
    }
  }

  const formatList = (items: string[], conjunctionKey = 'formatters.listConjunction'): string => {
    if (items.length === 0) return ''
    if (items.length === 1) return items[0]

    const conjunction = t(conjunctionKey)
    if (items.length === 2) return items.join(` ${conjunction} `)

    const allButLast = items.slice(0, -1).join(', ')
    const last = items[items.length - 1]

    return `${allButLast} ${conjunction} ${last}`
  }

  const formatValidationError = (field: string, error: string): string => {
    const fieldKey = `formatters.fieldNames.${field}`
    const fieldName = t(fieldKey, field) // fallback to field name if key not found
    return `${fieldName}: ${error}`
  }

  const formatApiError = (error: any): string => {
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

  const formatTableCell = (value: any, type: 'text' | 'number' | 'date' | 'boolean' = 'text'): string => {
    if (value == null) return '-'

    switch (type) {
      case 'number':
        return new Intl.NumberFormat(locale.value).format(value)
      case 'date':
        return formatDate(value)
      case 'boolean':
        return value ? t('common.yes') : t('common.no')
      case 'text':
      default:
        return String(value)
    }
  }

  return {
    formatDuration,
    formatTaskDueDate,
    formatReminderTime,
    formatList,
    formatValidationError,
    formatApiError,
    formatTableCell
  }
}
