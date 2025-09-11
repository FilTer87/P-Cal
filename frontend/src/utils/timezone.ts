/**
 * Timezone utility functions for consistent datetime handling
 */

/**
 * Convert local datetime string to UTC ISO string for backend
 * @param date - Date in YYYY-MM-DD format
 * @param time - Time in HH:MM format
 * @returns ISO string in UTC timezone
 */
export function localDateTimeToUTC(date: string, time: string): string {
  if (!date || !time) {
    throw new Error('Date and time are required')
  }
  
  // Create Date object in local timezone
  const localDateTime = new Date(`${date}T${time}:00`)
  
  // Convert to UTC ISO string
  return localDateTime.toISOString()
}

/**
 * Convert local datetime to UTC for all-day events
 * @param date - Date in YYYY-MM-DD format
 * @returns ISO string for start of day in UTC
 */
export function localDateToUTC(date: string): string {
  if (!date) {
    throw new Error('Date is required')
  }
  
  // For all-day events, use start of day in local timezone
  const localDate = new Date(`${date}T00:00:00`)
  return localDate.toISOString()
}

/**
 * Convert UTC ISO string to local date for form inputs
 * @param utcString - UTC ISO string from backend
 * @returns Date in YYYY-MM-DD format
 */
export function utcToLocalDate(utcString: string): string {
  if (!utcString) return ''
  
  const date = new Date(utcString)
  return date.getFullYear() + '-' + 
         String(date.getMonth() + 1).padStart(2, '0') + '-' + 
         String(date.getDate()).padStart(2, '0')
}

/**
 * Convert UTC ISO string to local time for form inputs
 * @param utcString - UTC ISO string from backend
 * @returns Time in HH:MM format
 */
export function utcToLocalTime(utcString: string): string {
  if (!utcString) return ''
  
  const date = new Date(utcString)
  return String(date.getHours()).padStart(2, '0') + ':' + 
         String(date.getMinutes()).padStart(2, '0')
}

/**
 * Get current timezone name for display
 * @returns Timezone string (e.g., "Europe/Rome")
 */
export function getCurrentTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}

/**
 * Get current timezone offset for debugging
 * @returns Offset in minutes
 */
export function getTimezoneOffset(): number {
  return new Date().getTimezoneOffset()
}

/**
 * Format UTC datetime for local display
 * @param utcString - UTC ISO string
 * @param options - Intl.DateTimeFormatOptions
 * @returns Formatted local datetime string
 */
export function formatUTCForLocal(
  utcString: string, 
  options: Intl.DateTimeFormatOptions = {}
): string {
  if (!utcString) return ''
  
  const date = new Date(utcString)
  return new Intl.DateTimeFormat('it-IT', {
    timeZone: getCurrentTimezone(),
    ...options
  }).format(date)
}