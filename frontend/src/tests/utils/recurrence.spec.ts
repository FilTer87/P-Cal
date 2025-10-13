import { describe, it, expect } from 'vitest'
import {
  buildRRule,
  parseRRule,
  validateRecurrenceParams,
  getRecurrenceDescription
} from '../../utils/recurrence'
import { RecurrenceFrequency, RecurrenceEndType } from '../../types/task'

describe('recurrence.ts', () => {
  describe('buildRRule', () => {
    it('should build daily RRULE with count', () => {
      const params = {
        frequency: RecurrenceFrequency.DAILY,
        interval: 1,
        endType: RecurrenceEndType.COUNT,
        count: 7
      }

      const rrule = buildRRule(params)
      expect(rrule).toBe('FREQ=DAILY;COUNT=7')
    })

    it('should build weekly RRULE with interval', () => {
      const params = {
        frequency: RecurrenceFrequency.WEEKLY,
        interval: 2,
        endType: RecurrenceEndType.NEVER
      }

      const rrule = buildRRule(params)
      expect(rrule).toBe('FREQ=WEEKLY;INTERVAL=2')
    })

    it('should build weekly RRULE with specific days', () => {
      const params = {
        frequency: RecurrenceFrequency.WEEKLY,
        interval: 1,
        endType: RecurrenceEndType.NEVER,
        byDay: ['MO', 'WE', 'FR']
      }

      const rrule = buildRRule(params)
      expect(rrule).toBe('FREQ=WEEKLY;BYDAY=MO,WE,FR')
    })

    it('should build monthly RRULE with UNTIL date', () => {
      const params = {
        frequency: RecurrenceFrequency.MONTHLY,
        interval: 1,
        endType: RecurrenceEndType.DATE,
        endDate: '2025-12-31'
      }

      const rrule = buildRRule(params)
      expect(rrule).toBe('FREQ=MONTHLY;UNTIL=20251231T235959Z')
    })

    it('should build yearly RRULE', () => {
      const params = {
        frequency: RecurrenceFrequency.YEARLY,
        interval: 1,
        endType: RecurrenceEndType.NEVER
      }

      const rrule = buildRRule(params)
      expect(rrule).toBe('FREQ=YEARLY')
    })

    it('should not include interval if it is 1', () => {
      const params = {
        frequency: RecurrenceFrequency.DAILY,
        interval: 1,
        endType: RecurrenceEndType.NEVER
      }

      const rrule = buildRRule(params)
      expect(rrule).toBe('FREQ=DAILY')
      expect(rrule).not.toContain('INTERVAL')
    })
  })

  describe('parseRRule', () => {
    it('should parse daily RRULE with count', () => {
      const rrule = 'FREQ=DAILY;COUNT=7'
      const params = parseRRule(rrule)

      expect(params).toEqual({
        frequency: RecurrenceFrequency.DAILY,
        interval: 1,
        endType: RecurrenceEndType.COUNT,
        count: 7
      })
    })

    it('should parse weekly RRULE with interval', () => {
      const rrule = 'FREQ=WEEKLY;INTERVAL=2'
      const params = parseRRule(rrule)

      expect(params).toEqual({
        frequency: RecurrenceFrequency.WEEKLY,
        interval: 2,
        endType: RecurrenceEndType.NEVER
      })
    })

    it('should parse weekly RRULE with days', () => {
      const rrule = 'FREQ=WEEKLY;BYDAY=MO,WE,FR'
      const params = parseRRule(rrule)

      expect(params?.frequency).toBe(RecurrenceFrequency.WEEKLY)
      expect(params?.byDay).toEqual(['MO', 'WE', 'FR'])
    })

    it('should parse monthly RRULE with UNTIL', () => {
      const rrule = 'FREQ=MONTHLY;UNTIL=20251231T235959Z'
      const params = parseRRule(rrule)

      expect(params?.frequency).toBe(RecurrenceFrequency.MONTHLY)
      expect(params?.endType).toBe(RecurrenceEndType.DATE)
      expect(params?.endDate).toBe('2025-12-31')
    })

    it('should return null for empty RRULE', () => {
      expect(parseRRule('')).toBeNull()
      expect(parseRRule('   ')).toBeNull()
    })

    it('should return null for invalid RRULE', () => {
      const params = parseRRule('INVALID')
      expect(params).toBeNull()
    })
  })

  describe('validateRecurrenceParams', () => {
    it('should validate valid daily recurrence', () => {
      const params = {
        frequency: RecurrenceFrequency.DAILY,
        interval: 1,
        endType: RecurrenceEndType.NEVER
      }

      const error = validateRecurrenceParams(params)
      expect(error).toBeNull()
    })

    it('should validate weekly recurrence with count', () => {
      const params = {
        frequency: RecurrenceFrequency.WEEKLY,
        interval: 1,
        endType: RecurrenceEndType.COUNT,
        count: 10
      }

      const error = validateRecurrenceParams(params)
      expect(error).toBeNull()
    })

    it('should validate monthly recurrence with end date', () => {
      const params = {
        frequency: RecurrenceFrequency.MONTHLY,
        interval: 1,
        endType: RecurrenceEndType.DATE,
        endDate: '2025-12-31'
      }

      const error = validateRecurrenceParams(params)
      expect(error).toBeNull()
    })

    it('should reject missing frequency', () => {
      const params = {
        frequency: '' as any,
        interval: 1,
        endType: RecurrenceEndType.NEVER
      }

      const error = validateRecurrenceParams(params)
      expect(error).toBe('Frequency is required')
    })

    it('should reject invalid interval', () => {
      const params = {
        frequency: RecurrenceFrequency.DAILY,
        interval: 0,
        endType: RecurrenceEndType.NEVER
      }

      const error = validateRecurrenceParams(params)
      expect(error).toBe('Interval must be at least 1')
    })

    it('should reject COUNT without count value', () => {
      const params = {
        frequency: RecurrenceFrequency.DAILY,
        interval: 1,
        endType: RecurrenceEndType.COUNT
      }

      const error = validateRecurrenceParams(params)
      expect(error).toBe('Count must be at least 1')
    })

    it('should reject COUNT with zero', () => {
      const params = {
        frequency: RecurrenceFrequency.DAILY,
        interval: 1,
        endType: RecurrenceEndType.COUNT,
        count: 0
      }

      const error = validateRecurrenceParams(params)
      expect(error).toBe('Count must be at least 1')
    })

    it('should reject COUNT exceeding 999', () => {
      const params = {
        frequency: RecurrenceFrequency.DAILY,
        interval: 1,
        endType: RecurrenceEndType.COUNT,
        count: 1000
      }

      const error = validateRecurrenceParams(params)
      expect(error).toBe('Count cannot exceed 999')
    })

    it('should reject DATE without end date', () => {
      const params = {
        frequency: RecurrenceFrequency.DAILY,
        interval: 1,
        endType: RecurrenceEndType.DATE
      }

      const error = validateRecurrenceParams(params)
      expect(error).toBe('End date is required')
    })
  })

  describe('getRecurrenceDescription', () => {
    const mockT = (key: string, params?: Record<string, any>) => {
      const translations: Record<string, string> = {
        'tasks.recurrenceOptions.daily': 'Daily',
        'tasks.recurrenceOptions.weekly': 'Weekly',
        'tasks.recurrenceOptions.monthly': 'Monthly',
        'tasks.recurrenceOptions.yearly': 'Yearly',
        'tasks.recurrenceInterval': 'Repeat every',
        'tasks.recurrenceCount': 'occurrences',
        'tasks.recurrenceEndType': 'ends',
        'tasks.recurrenceEnd.never': 'Never',
        'dateTime.weekdays.short.mon': 'Mon',
        'dateTime.weekdays.short.tue': 'Tue',
        'dateTime.weekdays.short.wed': 'Wed',
        'dateTime.weekdays.short.thu': 'Thu',
        'dateTime.weekdays.short.fri': 'Fri',
        'dateTime.weekdays.short.sat': 'Sat',
        'dateTime.weekdays.short.sun': 'Sun'
      }
      return translations[key] || key
    }

    it('should return "Never" for null RRULE', () => {
      const description = getRecurrenceDescription(null, mockT)
      expect(description).toBe('Never')
    })

    it('should describe daily recurrence', () => {
      const rrule = 'FREQ=DAILY;COUNT=7'
      const description = getRecurrenceDescription(rrule, mockT)
      expect(description).toContain('Daily')
      expect(description).toContain('7')
    })

    it('should describe weekly recurrence with days', () => {
      const rrule = 'FREQ=WEEKLY;BYDAY=MO,WE,FR'
      const description = getRecurrenceDescription(rrule, mockT)
      expect(description).toContain('Weekly')
      expect(description).toContain('Mon')
      expect(description).toContain('Wed')
      expect(description).toContain('Fri')
    })

    it('should describe recurrence with interval', () => {
      const rrule = 'FREQ=WEEKLY;INTERVAL=2'
      const description = getRecurrenceDescription(rrule, mockT)
      expect(description).toContain('Weekly')
      expect(description).toContain('Repeat every')
      expect(description).toContain('2')
    })

    it('should describe recurrence with end date', () => {
      const rrule = 'FREQ=MONTHLY;UNTIL=20251231T235959Z'
      const description = getRecurrenceDescription(rrule, mockT)
      expect(description).toContain('Monthly')
      expect(description).toContain('2025-12-31')
    })
  })

  describe('Round-trip conversion', () => {
    it('should convert params -> RRULE -> params correctly', () => {
      const originalParams = {
        frequency: RecurrenceFrequency.WEEKLY,
        interval: 2,
        endType: RecurrenceEndType.COUNT,
        count: 10,
        byDay: ['MO', 'WE', 'FR']
      }

      const rrule = buildRRule(originalParams)
      const parsedParams = parseRRule(rrule)

      expect(parsedParams).toEqual(originalParams)
    })

    it('should handle UNTIL date round-trip', () => {
      const originalParams = {
        frequency: RecurrenceFrequency.MONTHLY,
        interval: 1,
        endType: RecurrenceEndType.DATE,
        endDate: '2025-12-31'
      }

      const rrule = buildRRule(originalParams)
      const parsedParams = parseRRule(rrule)

      expect(parsedParams?.frequency).toBe(originalParams.frequency)
      expect(parsedParams?.endType).toBe(originalParams.endType)
      expect(parsedParams?.endDate).toBe(originalParams.endDate)
    })
  })
})
