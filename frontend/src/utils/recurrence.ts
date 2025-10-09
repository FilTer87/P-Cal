import { RecurrenceFrequency, RecurrenceEndType } from '@/types/task'

export interface RecurrenceParams {
  frequency: RecurrenceFrequency
  interval?: number
  endType: RecurrenceEndType
  count?: number
  endDate?: string
  byDay?: string[] // For weekly recurrence: ['MO', 'WE', 'FR']
}

/**
 * Convert form data to RFC 5545 RRULE string
 */
export function buildRRule(params: RecurrenceParams): string {
  const parts: string[] = []

  // Frequency (required)
  parts.push(`FREQ=${params.frequency}`)

  // Interval (default is 1)
  if (params.interval && params.interval > 1) {
    parts.push(`INTERVAL=${params.interval}`)
  }

  // Days of week (for weekly recurrence)
  if (params.byDay && params.byDay.length > 0) {
    parts.push(`BYDAY=${params.byDay.join(',')}`)
  }

  // End condition
  if (params.endType === RecurrenceEndType.COUNT && params.count) {
    parts.push(`COUNT=${params.count}`)
  } else if (params.endType === RecurrenceEndType.DATE && params.endDate) {
    // Convert ISO date to RRULE format (YYYYMMDD)
    const date = new Date(params.endDate)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    parts.push(`UNTIL=${year}${month}${day}T235959Z`)
  }

  return parts.join(';')
}

/**
 * Parse RFC 5545 RRULE string to form-friendly structure
 */
export function parseRRule(rrule: string): RecurrenceParams | null {
  if (!rrule || rrule.trim() === '') {
    return null
  }

  const parts = rrule.split(';')
  const params: Partial<RecurrenceParams> = {
    interval: 1,
    endType: RecurrenceEndType.NEVER
  }

  for (const part of parts) {
    const [key, value] = part.split('=')

    switch (key) {
      case 'FREQ':
        params.frequency = value as RecurrenceFrequency
        break

      case 'INTERVAL':
        params.interval = parseInt(value, 10)
        break

      case 'COUNT':
        params.endType = RecurrenceEndType.COUNT
        params.count = parseInt(value, 10)
        break

      case 'UNTIL':
        params.endType = RecurrenceEndType.DATE
        // Parse RRULE date format (YYYYMMDDTHHMMSSZ) to ISO
        const year = value.substring(0, 4)
        const month = value.substring(4, 6)
        const day = value.substring(6, 8)
        params.endDate = `${year}-${month}-${day}`
        break

      case 'BYDAY':
        params.byDay = value.split(',')
        break
    }
  }

  if (!params.frequency) {
    return null
  }

  return params as RecurrenceParams
}

/**
 * Validate RRULE parameters
 */
export function validateRecurrenceParams(params: RecurrenceParams): string | null {
  if (!params.frequency) {
    return 'Frequency is required'
  }

  if (params.interval !== undefined && params.interval < 1) {
    return 'Interval must be at least 1'
  }

  if (params.endType === RecurrenceEndType.COUNT) {
    if (!params.count || params.count < 1) {
      return 'Count must be at least 1'
    }
    if (params.count > 999) {
      return 'Count cannot exceed 999'
    }
  }

  if (params.endType === RecurrenceEndType.DATE) {
    if (!params.endDate) {
      return 'End date is required'
    }
  }

  return null
}

/**
 * Get human-readable description of recurrence rule
 * Uses i18n for translation (to be called from component with $t)
 */
export function getRecurrenceDescription(
  rrule: string | null | undefined,
  t: (key: string, params?: Record<string, any>) => string
): string {
  if (!rrule) {
    return t('tasks.recurrenceEnd.never')
  }

  const params = parseRRule(rrule)
  if (!params) {
    return t('tasks.recurrenceEnd.never')
  }

  const parts: string[] = []

  // Frequency
  const freqKey = params.frequency.toLowerCase()
  parts.push(t(`tasks.recurrenceOptions.${freqKey}`))

  // Interval
  if (params.interval && params.interval > 1) {
    parts.push(`(${t('tasks.recurrenceInterval')} ${params.interval})`)
  }

  // Days of week
  if (params.byDay && params.byDay.length > 0) {
    const dayLabels = params.byDay.map(day => {
      const dayMap: Record<string, string> = {
        SU: t('dateTime.weekdays.short.sun'),
        MO: t('dateTime.weekdays.short.mon'),
        TU: t('dateTime.weekdays.short.tue'),
        WE: t('dateTime.weekdays.short.wed'),
        TH: t('dateTime.weekdays.short.thu'),
        FR: t('dateTime.weekdays.short.fri'),
        SA: t('dateTime.weekdays.short.sat')
      }
      return dayMap[day] || day
    })
    parts.push(`(${dayLabels.join(', ')})`)
  }

  // End condition
  if (params.endType === RecurrenceEndType.COUNT && params.count) {
    parts.push(`- ${params.count} ${t('tasks.recurrenceCount').toLowerCase()}`)
  } else if (params.endType === RecurrenceEndType.DATE && params.endDate) {
    parts.push(`- ${t('tasks.recurrenceEndType').toLowerCase()} ${params.endDate}`)
  }

  return parts.join(' ')
}
