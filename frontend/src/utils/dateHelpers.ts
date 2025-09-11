import {
  format,
  formatDistance,
  formatDistanceToNow,
  formatRelative,
  isToday,
  isTomorrow,
  isYesterday,
  isThisWeek,
  isThisMonth,
  isThisYear,
  startOfDay,
  endOfDay,
  startOfWeek,
  endOfWeek,
  startOfMonth,
  endOfMonth,
  startOfYear,
  endOfYear,
  addDays,
  addWeeks,
  addMonths,
  addYears,
  subDays,
  subWeeks,
  subMonths,
  subYears,
  differenceInDays,
  differenceInHours,
  differenceInMinutes,
  differenceInCalendarDays,
  isBefore,
  isAfter,
  isSameDay,
  isSameWeek,
  isSameMonth,
  isSameYear,
  isWeekend,
  parseISO,
  isValid,
  getDay,
  getDaysInMonth,
  getWeek,
  getMonth,
  getYear
} from 'date-fns'
import { it } from 'date-fns/locale'

/**
 * Format date for display in Italian locale
 * @param date - Date object or UTC ISO string from backend
 * @param pattern - Format pattern (default: dd/MM/yyyy)
 * @returns Formatted date in local timezone
 */
export const formatDate = (date: Date | string, pattern = 'dd/MM/yyyy'): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return 'Data non valida'
  
  // parseISO automatically converts UTC string to local timezone
  return format(dateObj, pattern, { locale: it })
}

/**
 * Format datetime for display in Italian locale
 * @param date - Date object or UTC ISO string from backend
 * @param pattern - Format pattern (default: dd/MM/yyyy HH:mm)
 * @returns Formatted datetime in local timezone
 */
export const formatDateTime = (date: Date | string, pattern = 'dd/MM/yyyy HH:mm'): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return 'Data non valida'
  
  // parseISO automatically converts UTC string to local timezone
  return format(dateObj, pattern, { locale: it })
}

/**
 * Format time only
 * @param date - Date object or UTC ISO string from backend
 * @param pattern - Format pattern (default: HH:mm)
 * @returns Formatted time in local timezone
 */
export const formatTime = (date: Date | string, pattern = 'HH:mm'): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return 'Ora non valida'
  
  // parseISO automatically converts UTC string to local timezone
  return format(dateObj, pattern, { locale: it })
}

/**
 * Format date for HTML input fields
 */
export const formatDateForInput = (date: Date | string): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return ''
  
  return format(dateObj, 'yyyy-MM-dd')
}

/**
 * Format time for HTML input fields
 */
export const formatTimeForInput = (date: Date | string): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return ''
  
  return format(dateObj, 'HH:mm')
}

/**
 * Format datetime for ISO string
 */
export const formatDateTimeForAPI = (date: Date | string): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return ''
  
  return dateObj.toISOString()
}

/**
 * Format relative time (e.g., "2 hours ago", "in 3 days")
 */
export const formatRelativeTime = (date: Date | string): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return 'Data non valida'
  
  return formatDistanceToNow(dateObj, { 
    locale: it, 
    addSuffix: true,
    includeSeconds: true
  })
}

/**
 * Format distance between two dates
 */
export const formatDateDistance = (startDate: Date | string, endDate: Date | string): string => {
  const start = typeof startDate === 'string' ? parseISO(startDate) : startDate
  const end = typeof endDate === 'string' ? parseISO(endDate) : endDate
  
  if (!isValid(start) || !isValid(end)) return 'Date non valide'
  
  return formatDistance(start, end, { locale: it })
}

/**
 * Format date relative to today (e.g., "today", "yesterday", "Monday")
 */
export const formatRelativeDate = (date: Date | string): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return 'Data non valida'
  
  return formatRelative(dateObj, new Date(), { locale: it })
}

/**
 * Get human-readable date description
 */
export const getDateDescription = (date: Date | string): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return 'Data non valida'
  
  if (isToday(dateObj)) return 'Oggi'
  if (isTomorrow(dateObj)) return 'Domani'
  if (isYesterday(dateObj)) return 'Ieri'
  if (isThisWeek(dateObj, { weekStartsOn: 1 })) {
    return format(dateObj, 'EEEE', { locale: it })
  }
  if (isThisMonth(dateObj)) {
    return format(dateObj, 'EEEE d', { locale: it })
  }
  if (isThisYear(dateObj)) {
    return format(dateObj, 'd MMMM', { locale: it })
  }
  
  return format(dateObj, 'd MMMM yyyy', { locale: it })
}

/**
 * Get day name in Italian
 */
export const getDayName = (date: Date | string, short = false): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return 'Giorno non valido'
  
  return format(dateObj, short ? 'EEE' : 'EEEE', { locale: it })
}

/**
 * Get month name in Italian
 */
export const getMonthName = (date: Date | string, short = false): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return 'Mese non valido'
  
  return format(dateObj, short ? 'MMM' : 'MMMM', { locale: it })
}

/**
 * Get month and year string
 */
export const getMonthYear = (date: Date | string): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return 'Data non valida'
  
  return format(dateObj, 'MMMM yyyy', { locale: it })
}

/**
 * Date range utilities
 */
export const getDateRange = {
  today: () => ({ start: startOfDay(new Date()), end: endOfDay(new Date()) }),
  yesterday: () => {
    const yesterday = subDays(new Date(), 1)
    return { start: startOfDay(yesterday), end: endOfDay(yesterday) }
  },
  thisWeek: () => ({ 
    start: startOfWeek(new Date(), { weekStartsOn: 1 }), 
    end: endOfWeek(new Date(), { weekStartsOn: 1 }) 
  }),
  lastWeek: () => {
    const lastWeek = subWeeks(new Date(), 1)
    return {
      start: startOfWeek(lastWeek, { weekStartsOn: 1 }),
      end: endOfWeek(lastWeek, { weekStartsOn: 1 })
    }
  },
  thisMonth: () => ({ start: startOfMonth(new Date()), end: endOfMonth(new Date()) }),
  lastMonth: () => {
    const lastMonth = subMonths(new Date(), 1)
    return { start: startOfMonth(lastMonth), end: endOfMonth(lastMonth) }
  },
  thisYear: () => ({ start: startOfYear(new Date()), end: endOfYear(new Date()) }),
  lastYear: () => {
    const lastYear = subYears(new Date(), 1)
    return { start: startOfYear(lastYear), end: endOfYear(lastYear) }
  },
  next7Days: () => ({ start: new Date(), end: addDays(new Date(), 7) }),
  next30Days: () => ({ start: new Date(), end: addDays(new Date(), 30) }),
  last7Days: () => ({ start: subDays(new Date(), 7), end: new Date() }),
  last30Days: () => ({ start: subDays(new Date(), 30), end: new Date() })
}

/**
 * Check if date is in range
 */
export const isDateInRange = (date: Date | string, start: Date | string, end: Date | string): boolean => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  const startObj = typeof start === 'string' ? parseISO(start) : start
  const endObj = typeof end === 'string' ? parseISO(end) : end
  
  if (!isValid(dateObj) || !isValid(startObj) || !isValid(endObj)) return false
  
  return dateObj >= startObj && dateObj <= endObj
}

/**
 * Get days difference
 */
export const getDaysDifference = (startDate: Date | string, endDate: Date | string): number => {
  const start = typeof startDate === 'string' ? parseISO(startDate) : startDate
  const end = typeof endDate === 'string' ? parseISO(endDate) : endDate
  
  if (!isValid(start) || !isValid(end)) return 0
  
  return differenceInCalendarDays(end, start)
}

/**
 * Get hours difference
 */
export const getHoursDifference = (startDate: Date | string, endDate: Date | string): number => {
  const start = typeof startDate === 'string' ? parseISO(startDate) : startDate
  const end = typeof endDate === 'string' ? parseISO(endDate) : endDate
  
  if (!isValid(start) || !isValid(end)) return 0
  
  return differenceInHours(end, start)
}

/**
 * Get minutes difference
 */
export const getMinutesDifference = (startDate: Date | string, endDate: Date | string): number => {
  const start = typeof startDate === 'string' ? parseISO(startDate) : startDate
  const end = typeof endDate === 'string' ? parseISO(endDate) : endDate
  
  if (!isValid(start) || !isValid(end)) return 0
  
  return differenceInMinutes(end, start)
}

/**
 * Calendar utilities
 */
export const getCalendarMonth = (date: Date | string) => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return null
  
  const start = startOfWeek(startOfMonth(dateObj), { weekStartsOn: 1 })
  const end = endOfWeek(endOfMonth(dateObj), { weekStartsOn: 1 })
  
  const days: Date[] = []
  let current = new Date(start)
  
  while (current <= end) {
    days.push(new Date(current))
    current = addDays(current, 1)
  }
  
  return {
    month: getMonth(dateObj),
    year: getYear(dateObj),
    monthName: getMonthName(dateObj),
    daysInMonth: getDaysInMonth(dateObj),
    firstDay: startOfMonth(dateObj),
    lastDay: endOfMonth(dateObj),
    calendarDays: days,
    weeksInMonth: Math.ceil(days.length / 7)
  }
}

/**
 * Get week information
 */
export const getWeekInfo = (date: Date | string) => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return null
  
  const weekStart = startOfWeek(dateObj, { weekStartsOn: 1 })
  const weekEnd = endOfWeek(dateObj, { weekStartsOn: 1 })
  
  const days: Date[] = []
  for (let i = 0; i < 7; i++) {
    days.push(addDays(weekStart, i))
  }
  
  return {
    weekNumber: getWeek(dateObj, { weekStartsOn: 1 }),
    weekStart,
    weekEnd,
    days,
    isCurrentWeek: isThisWeek(dateObj, { weekStartsOn: 1 })
  }
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

/**
 * Date comparison utilities
 */
export const compareDates = {
  isBefore: (date1: Date | string, date2: Date | string): boolean => {
    const d1 = typeof date1 === 'string' ? parseISO(date1) : date1
    const d2 = typeof date2 === 'string' ? parseISO(date2) : date2
    if (!isValid(d1) || !isValid(d2)) return false
    return isBefore(d1, d2)
  },
  
  isAfter: (date1: Date | string, date2: Date | string): boolean => {
    const d1 = typeof date1 === 'string' ? parseISO(date1) : date1
    const d2 = typeof date2 === 'string' ? parseISO(date2) : date2
    if (!isValid(d1) || !isValid(d2)) return false
    return isAfter(d1, d2)
  },
  
  isSame: (date1: Date | string, date2: Date | string, unit: 'day' | 'week' | 'month' | 'year' = 'day'): boolean => {
    const d1 = typeof date1 === 'string' ? parseISO(date1) : date1
    const d2 = typeof date2 === 'string' ? parseISO(date2) : date2
    if (!isValid(d1) || !isValid(d2)) return false
    
    switch (unit) {
      case 'day': return isSameDay(d1, d2)
      case 'week': return isSameWeek(d1, d2, { weekStartsOn: 1 })
      case 'month': return isSameMonth(d1, d2)
      case 'year': return isSameYear(d1, d2)
      default: return isSameDay(d1, d2)
    }
  }
}

/**
 * Time zone utilities
 */
export const getTimezone = (): string => {
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}

export const formatInTimezone = (date: Date | string, timezone: string, pattern = 'yyyy-MM-dd HH:mm'): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return 'Data non valida'
  
  return new Intl.DateTimeFormat('it-IT', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(dateObj)
}

/**
 * Business day utilities
 */
export const isBusinessDay = (date: Date | string): boolean => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return false
  
  return !isWeekend(dateObj)
}

export const getNextBusinessDay = (date: Date | string): Date => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return new Date()
  
  let nextDay = addDays(dateObj, 1)
  while (isWeekend(nextDay)) {
    nextDay = addDays(nextDay, 1)
  }
  
  return nextDay
}

export const getPreviousBusinessDay = (date: Date | string): Date => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date
  if (!isValid(dateObj)) return new Date()
  
  let prevDay = subDays(dateObj, 1)
  while (isWeekend(prevDay)) {
    prevDay = subDays(prevDay, 1)
  }
  
  return prevDay
}

/**
 * Create date from date and time strings
 */
export const createDateFromDateAndTime = (dateString: string, timeString: string): Date | null => {
  if (!isValidDateString(dateString) || !isValidTimeString(timeString)) {
    return null
  }
  
  const dateTimeString = `${dateString}T${timeString}:00`
  const dateObj = parseISO(dateTimeString)
  
  return isValid(dateObj) ? dateObj : null
}

/**
 * Get current date and time in different formats
 */
export const now = {
  date: () => new Date(),
  dateString: () => formatDateForInput(new Date()),
  timeString: () => formatTimeForInput(new Date()),
  dateTimeString: () => new Date().toISOString(),
  timestamp: () => Date.now(),
  formatted: (pattern = 'dd/MM/yyyy HH:mm') => formatDateTime(new Date(), pattern)
}

export { isToday }
