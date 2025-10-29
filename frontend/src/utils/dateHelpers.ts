import {
  format,
  formatDistanceToNow,
  isToday,
  isTomorrow,
  isYesterday,
  isThisWeek,
  isThisMonth,
  isThisYear,
  parseISO,
  isValid,
} from 'date-fns'
import { it, enUS, es, type Locale } from 'date-fns/locale'
import { i18nGlobal } from '../i18n'

// Map of supported locales for date-fns
const dateFnsLocales: Record<string, Locale> = {
  'it-IT': it,
  'en-US': enUS,
  'es-ES': es
}

/**
 * Get date-fns locale from current i18n locale
 */
export const getDateFnsLocale = (): Locale => {
  const currentLocale = i18nGlobal.locale.value
  return dateFnsLocales[currentLocale] || it
}

/**
 * Format date for display with locale support
 * @param date - Date object or UTC ISO string from backend
 * @param pattern - Format pattern (default: dd/MM/yyyy)
 * @returns Formatted date in local timezone
 */
export const formatDate = (date: Date | string, pattern = 'dd/MM/yyyy'): string => {
  const { t } = i18nGlobal
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return t('errors.invalidDate')

  // parseISO automatically converts UTC string to local timezone
  return format(dateObj, pattern, { locale: getDateFnsLocale() })
}

/**
 * Format datetime for display with locale support
 * @param date - Date object or UTC ISO string from backend
 * @param pattern - Format pattern (default: dd/MM/yyyy HH:mm)
 * @returns Formatted datetime in local timezone
 */
export const formatDateTime = (date: Date | string, pattern = 'dd/MM/yyyy HH:mm'): string => {
  const { t } = i18nGlobal
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return t('errors.invalidDate')

  // parseISO automatically converts UTC string to local timezone
  return format(dateObj, pattern, { locale: getDateFnsLocale() })
}

/**
 * Format time only with locale support
 * @param date - Date object or UTC ISO string from backend
 * @param pattern - Format pattern (default: HH:mm)
 * @returns Formatted time in local timezone
 */
export const formatTime = (date: Date | string, pattern = 'HH:mm'): string => {
  const { t } = i18nGlobal
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return t('errors.invalidTime')

  // parseISO automatically converts UTC string to local timezone
  return format(dateObj, pattern, { locale: getDateFnsLocale() })
}

/**
 * Format weekday name with locale support
 * @param date - Date object
 * @param short - Use short format (e.g. "Mon" instead of "Monday")
 * @returns Localized weekday name
 */
export const formatWeekday = (date: Date, short = false): string => {
  const pattern = short ? 'EEE' : 'EEEE'
  return format(date, pattern, { locale: getDateFnsLocale() })
}

/**
 * Format relative time (e.g., "2 hours ago", "in 3 days")
 */
export const formatRelativeTime = (date: Date | string): string => {
  const { t } = i18nGlobal
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return t('errors.invalidDate')

  return formatDistanceToNow(dateObj, {
    locale: getDateFnsLocale(),
    addSuffix: true,
    includeSeconds: true
  })
}


/**
 * Get human-readable date description
 */
export const getDateDescription = (date: Date | string): string => {
  const { t } = i18nGlobal
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return t('errors.invalidDate')

  if (isToday(dateObj)) return t('dateTime.today')
  if (isTomorrow(dateObj)) return t('dateTime.tomorrow')
  if (isYesterday(dateObj)) return t('dateTime.yesterday')
  if (isThisWeek(dateObj, { weekStartsOn: 1 })) {
    return format(dateObj, 'EEEE', { locale: getDateFnsLocale() })
  }
  if (isThisMonth(dateObj)) {
    return format(dateObj, 'EEEE d', { locale: getDateFnsLocale() })
  }
  if (isThisYear(dateObj)) {
    return format(dateObj, 'd MMMM', { locale: getDateFnsLocale() })
  }

  return format(dateObj, 'd MMMM yyyy', { locale: getDateFnsLocale() })
}

/**
 * Get day name with locale support
 */
export const getDayName = (date: Date | string, short = false): string => {
  const { t } = i18nGlobal
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return t('errors.invalidDate')

  return format(dateObj, short ? 'EEE' : 'EEEE', { locale: getDateFnsLocale() })
}

/**
 * Date validation utilities
 */
export const isValidDate = (date: any): boolean => {
  if (!date) return false
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  return isValid(dateObj)
}

export const isValidDateString = (dateString: string): boolean => {
  if (!dateString) return false
  const dateObj = parseISO(dateString)
  return isValid(dateObj)
}

export const isValidTimeString = (timeString: string): boolean => {
  if (!timeString) return false
  const timeRegex = /^([01]?[0-9]|2[0-3]):[0-5][0-9]$/
  return timeRegex.test(timeString)
}

export { isToday }
