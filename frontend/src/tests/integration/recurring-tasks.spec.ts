import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { buildRRule, parseRRule } from '../../utils/recurrence'
import { transformTaskForCreation, transformTaskToFormData } from '../../services/taskDateService'
import { RecurrenceFrequency, RecurrenceEndType, type Task, type TaskFormData } from '../../types/task'

describe('Recurring Tasks Integration', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  const createMockTask = (overrides: Partial<Task> = {}): Task => ({
    id: 1,
    title: 'Team Sync',
    description: '',
    startDatetimeLocal: '2025-10-07T10:00:00Z',
    endDatetimeLocal: '2025-10-07T11:00:00Z',
    location: '',
    color: '#3788d8',
    isRecurring: false,
    userId: 1,
    createdAt: '2025-10-01T00:00:00Z',
    updatedAt: '2025-10-01T00:00:00Z',
    reminders: [],
    ...overrides
  })

  describe('Create weekly recurring event', () => {
    it('should create weekly recurring event with specific days', () => {
      // User fills form
      const formData: TaskFormData = {
        title: 'Team Sync',
        description: '',
        startDate: '2025-10-07',
        startTime: '10:00',
        endDate: '2025-10-07',
        endTime: '11:00',
        location: '',
        color: '#3788d8',
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY,
        recurrenceInterval: 1,
        recurrenceEndType: RecurrenceEndType.COUNT,
        recurrenceCount: 8,
        recurrenceByDay: ['MO', 'WE'],
        reminders: []
      }

      // Transform to API request
      const request = transformTaskForCreation(formData)

      // Verify RRULE was built correctly
      expect(request.recurrenceRule).toBe('FREQ=WEEKLY;BYDAY=MO,WE;COUNT=8')
      expect(request.title).toBe('Team Sync')
      expect(request.startDatetimeLocal).toBeDefined()
      expect(request.endDatetimeLocal).toBeDefined()

      // Verify RRULE can be parsed back
      const parsed = parseRRule(request.recurrenceRule!)
      expect(parsed?.frequency).toBe(RecurrenceFrequency.WEEKLY)
      expect(parsed?.byDay).toEqual(['MO', 'WE'])
      expect(parsed?.count).toBe(8)
    })

    it('should handle weekly recurrence with end date', () => {
      const formData: TaskFormData = {
        title: 'Weekly Review',
        description: '',
        startDate: '2025-10-01',
        startTime: '14:00',
        endDate: '2025-10-01',
        endTime: '15:00',
        location: '',
        color: '#3788d8',
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY,
        recurrenceInterval: 1,
        recurrenceEndType: RecurrenceEndType.DATE,
        recurrenceEndDate: '2025-10-31',
        reminders: []
      }

      const request = transformTaskForCreation(formData)

      expect(request.recurrenceRule).toBe('FREQ=WEEKLY;UNTIL=20251031T235959Z')
      expect(request.recurrenceEnd).toBeDefined()

      const parsed = parseRRule(request.recurrenceRule!)
      expect(parsed?.endType).toBe(RecurrenceEndType.DATE)
      expect(parsed?.endDate).toBe('2025-10-31')
    })
  })

  describe('Create daily recurring event', () => {
    it('should create daily recurring event', () => {
      const formData: TaskFormData = {
        title: 'Daily Standup',
        description: '',
        startDate: '2025-10-01',
        startTime: '09:00',
        endDate: '2025-10-01',
        endTime: '09:30',
        location: '',
        color: '#3788d8',
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY,
        recurrenceInterval: 1,
        recurrenceEndType: RecurrenceEndType.COUNT,
        recurrenceCount: 7,
        reminders: []
      }

      const request = transformTaskForCreation(formData)

      expect(request.recurrenceRule).toBe('FREQ=DAILY;COUNT=7')

      const parsed = parseRRule(request.recurrenceRule!)
      expect(parsed?.frequency).toBe(RecurrenceFrequency.DAILY)
      expect(parsed?.count).toBe(7)
    })
  })

  describe('Create monthly recurring event', () => {
    it('should create monthly recurring event', () => {
      const formData: TaskFormData = {
        title: 'Monthly Report',
        description: '',
        startDate: '2025-10-01',
        startTime: '09:00',
        endDate: '2025-10-01',
        endTime: '10:00',
        location: '',
        color: '#3788d8',
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.MONTHLY,
        recurrenceInterval: 1,
        recurrenceEndType: RecurrenceEndType.COUNT,
        recurrenceCount: 12,
        reminders: []
      }

      const request = transformTaskForCreation(formData)

      expect(request.recurrenceRule).toBe('FREQ=MONTHLY;COUNT=12')

      const parsed = parseRRule(request.recurrenceRule!)
      expect(parsed?.frequency).toBe(RecurrenceFrequency.MONTHLY)
      expect(parsed?.count).toBe(12)
    })
  })

  describe('Edit recurring event', () => {
    it('should load recurring task data into form', () => {
      const task = createMockTask({
        title: 'Team Sync',
        recurrenceRule: 'FREQ=WEEKLY;BYDAY=MO,WE;COUNT=8',
        isRecurring: true
      })

      const formData = transformTaskToFormData(task)

      expect(formData.isRecurring).toBe(true)
      expect(formData.recurrenceFrequency).toBe(RecurrenceFrequency.WEEKLY)
      expect(formData.recurrenceByDay).toEqual(['MO', 'WE'])
      expect(formData.recurrenceEndType).toBe(RecurrenceEndType.COUNT)
      expect(formData.recurrenceCount).toBe(8)
    })

    it('should update recurring event', () => {
      // Load existing task
      const task = createMockTask({
        recurrenceRule: 'FREQ=DAILY;COUNT=7',
        isRecurring: true
      })

      const formData = transformTaskToFormData(task)

      // User changes to weekly
      formData.recurrenceFrequency = RecurrenceFrequency.WEEKLY
      formData.recurrenceByDay = ['MO', 'WE', 'FR']
      formData.recurrenceCount = 12

      const request = transformTaskForCreation(formData)

      expect(request.recurrenceRule).toBe('FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=12')
    })

    it('should convert recurring event to non-recurring', () => {
      const task = createMockTask({
        recurrenceRule: 'FREQ=DAILY;COUNT=7',
        isRecurring: true
      })

      const formData = transformTaskToFormData(task)

      // User disables recurrence
      formData.isRecurring = false
      formData.recurrenceFrequency = undefined
      formData.recurrenceByDay = undefined

      const request = transformTaskForCreation(formData)

      expect(request.recurrenceRule).toBeUndefined()
      expect(request.recurrenceEnd).toBeUndefined()
    })

    it('should convert non-recurring event to recurring', () => {
      const task = createMockTask({
        isRecurring: false
      })

      const formData = transformTaskToFormData(task)

      // User enables recurrence
      formData.isRecurring = true
      formData.recurrenceFrequency = RecurrenceFrequency.WEEKLY
      formData.recurrenceInterval = 1
      formData.recurrenceEndType = RecurrenceEndType.COUNT
      formData.recurrenceCount = 4

      const request = transformTaskForCreation(formData)

      expect(request.recurrenceRule).toBe('FREQ=WEEKLY;COUNT=4')
    })
  })

  describe('Complex recurrence patterns', () => {
    it('should handle bi-weekly recurrence', () => {
      const formData: TaskFormData = {
        title: 'Bi-weekly Sprint Planning',
        description: '',
        startDate: '2025-10-06',
        startTime: '14:00',
        endDate: '2025-10-06',
        endTime: '16:00',
        location: '',
        color: '#3788d8',
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY,
        recurrenceInterval: 2,
        recurrenceEndType: RecurrenceEndType.COUNT,
        recurrenceCount: 6,
        reminders: []
      }

      const request = transformTaskForCreation(formData)

      expect(request.recurrenceRule).toBe('FREQ=WEEKLY;INTERVAL=2;COUNT=6')

      const parsed = parseRRule(request.recurrenceRule!)
      expect(parsed?.interval).toBe(2)
    })

    it('should handle quarterly recurrence', () => {
      const formData: TaskFormData = {
        title: 'Quarterly Review',
        description: '',
        startDate: '2025-10-01',
        startTime: '10:00',
        endDate: '2025-10-01',
        endTime: '12:00',
        location: '',
        color: '#3788d8',
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.MONTHLY,
        recurrenceInterval: 3,
        recurrenceEndType: RecurrenceEndType.COUNT,
        recurrenceCount: 4,
        reminders: []
      }

      const request = transformTaskForCreation(formData)

      expect(request.recurrenceRule).toBe('FREQ=MONTHLY;INTERVAL=3;COUNT=4')
    })

    it('should handle weekday-only recurrence', () => {
      const formData: TaskFormData = {
        title: 'Workday Task',
        description: '',
        startDate: '2025-10-06',
        startTime: '09:00',
        endDate: '2025-10-06',
        endTime: '09:30',
        location: '',
        color: '#3788d8',
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY,
        recurrenceInterval: 1,
        recurrenceEndType: RecurrenceEndType.NEVER,
        recurrenceByDay: ['MO', 'TU', 'WE', 'TH', 'FR'],
        reminders: []
      }

      const request = transformTaskForCreation(formData)

      expect(request.recurrenceRule).toBe('FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR')

      const parsed = parseRRule(request.recurrenceRule!)
      expect(parsed?.byDay).toEqual(['MO', 'TU', 'WE', 'TH', 'FR'])
      expect(parsed?.endType).toBe(RecurrenceEndType.NEVER)
    })
  })

  describe('Edge cases', () => {
    it('should handle recurrence with no end (infinite)', () => {
      const formData: TaskFormData = {
        title: 'Infinite Recurrence',
        description: '',
        startDate: '2025-10-01',
        startTime: '10:00',
        endDate: '2025-10-01',
        endTime: '11:00',
        location: '',
        color: '#3788d8',
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY,
        recurrenceInterval: 1,
        recurrenceEndType: RecurrenceEndType.NEVER,
        reminders: []
      }

      const request = transformTaskForCreation(formData)

      expect(request.recurrenceRule).toBe('FREQ=DAILY')
      expect(request.recurrenceRule).not.toContain('COUNT')
      expect(request.recurrenceRule).not.toContain('UNTIL')
      expect(request.recurrenceEnd).toBeUndefined()
    })

    it('should preserve task properties when adding recurrence', () => {
      const formData: TaskFormData = {
        title: 'Important Meeting',
        description: 'Quarterly business review',
        startDate: '2025-10-15',
        startTime: '14:00',
        endDate: '2025-10-15',
        endTime: '16:00',
        location: 'Conference Room A',
        color: '#ef4444',
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.MONTHLY,
        recurrenceInterval: 3,
        recurrenceEndType: RecurrenceEndType.COUNT,
        recurrenceCount: 4,
        reminders: [
          {
            offsetMinutes: 60,
            offsetValue: 1,
            offsetUnit: 'hours',
            notificationType: 'PUSH' as any
          }
        ]
      }

      const request = transformTaskForCreation(formData)

      expect(request.title).toBe('Important Meeting')
      expect(request.description).toBe('Quarterly business review')
      expect(request.location).toBe('Conference Room A')
      expect(request.color).toBe('#ef4444')
      expect(request.reminders).toHaveLength(1)
      expect(request.recurrenceRule).toBe('FREQ=MONTHLY;INTERVAL=3;COUNT=4')
    })

    it('should handle round-trip transformation correctly', () => {
      // Create task with recurrence
      const originalForm: TaskFormData = {
        title: 'Weekly Team Sync',
        description: '',
        startDate: '2025-10-07',
        startTime: '10:00',
        endDate: '2025-10-07',
        endTime: '11:00',
        location: '',
        color: '#3788d8',
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY,
        recurrenceInterval: 1,
        recurrenceEndType: RecurrenceEndType.COUNT,
        recurrenceCount: 8,
        recurrenceByDay: ['MO', 'WE'],
        reminders: []
      }

      // Transform to API request
      const request = transformTaskForCreation(originalForm)

      // Simulate backend response
      const task = createMockTask({
        title: request.title,
        recurrenceRule: request.recurrenceRule,
        recurrenceEnd: request.recurrenceEnd,
        isRecurring: true
      })

      // Transform back to form
      const loadedForm = transformTaskToFormData(task)

      // Verify all recurrence data is preserved
      expect(loadedForm.isRecurring).toBe(true)
      expect(loadedForm.recurrenceFrequency).toBe(RecurrenceFrequency.WEEKLY)
      expect(loadedForm.recurrenceInterval).toBe(1)
      expect(loadedForm.recurrenceEndType).toBe(RecurrenceEndType.COUNT)
      expect(loadedForm.recurrenceCount).toBe(8)
      expect(loadedForm.recurrenceByDay).toEqual(['MO', 'WE'])
    })
  })
})
