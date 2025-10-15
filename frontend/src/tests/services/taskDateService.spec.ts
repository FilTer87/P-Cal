import { describe, it, expect } from 'vitest'
import { transformTaskForCreation, transformTaskForUpdate, transformTaskToFormData } from '../../services/taskDateService'
import { NotificationType, RecurrenceFrequency, RecurrenceEndType } from '../../types/task'
import type { Task, TaskFormData } from '../../types/task'

describe('taskDateService - Reminder Transformations', () => {
  describe('Regression: Reminder updates should be preserved', () => {
    it('should calculate offsetMinutes from offsetValue/offsetUnit when user modifies them', () => {
      // Simulate a form with a reminder that was modified by the user
      const formData: TaskFormData = {
        title: 'Test Task',
        description: 'Test Description',
        startDate: '2025-10-15',
        startTime: '10:00',
        endDate: '2025-10-15',
        endTime: '11:00',
        location: '',
        color: '#3b82f6',
        isRecurring: false,
        reminders: [
          {
            id: 1, // Existing reminder ID
            offsetMinutes: 15, // Old static value (should be ignored)
            offsetValue: 30, // User changed to 30
            offsetUnit: 'minutes', // User kept minutes
            reminderOffsetMinutes: 15, // Old value
            notificationType: NotificationType.PUSH
          }
        ]
      }

      const result = transformTaskForCreation(formData)

      // Should use the NEW values from offsetValue/offsetUnit (30 minutes)
      // NOT the old static offsetMinutes (15)
      expect(result.reminders).toHaveLength(1)
      expect(result.reminders[0].reminderOffsetMinutes).toBe(30)
      expect(result.reminders[0].notificationType).toBe(NotificationType.PUSH)
      expect(result.reminders[0].id).toBe(1) // Should preserve ID for updates
    })

    it('should correctly convert hours to minutes when user changes unit', () => {
      const formData: TaskFormData = {
        title: 'Test Task',
        description: '',
        startDate: '2025-10-15',
        startTime: '10:00',
        endDate: '2025-10-15',
        endTime: '11:00',
        location: '',
        color: '#3b82f6',
        isRecurring: false,
        reminders: [
          {
            id: 2,
            offsetMinutes: 10, // Old: 10 minutes
            offsetValue: 1, // User changed to 1 hour
            offsetUnit: 'hours', // Changed unit
            reminderOffsetMinutes: 10,
            notificationType: NotificationType.EMAIL
          }
        ]
      }

      const result = transformTaskForUpdate(formData)

      expect(result.reminders).toHaveLength(1)
      expect(result.reminders[0].reminderOffsetMinutes).toBe(60) // 1 hour = 60 minutes
      expect(result.reminders[0].notificationType).toBe(NotificationType.EMAIL)
    })

    it('should correctly convert days to minutes', () => {
      const formData: TaskFormData = {
        title: 'Test Task',
        description: '',
        startDate: '2025-10-15',
        startTime: '10:00',
        endDate: '2025-10-15',
        endTime: '11:00',
        location: '',
        color: '#3b82f6',
        isRecurring: false,
        reminders: [
          {
            offsetMinutes: 60,
            offsetValue: 1,
            offsetUnit: 'days',
            notificationType: NotificationType.PUSH
          }
        ]
      }

      const result = transformTaskForCreation(formData)

      expect(result.reminders[0].reminderOffsetMinutes).toBe(1440) // 1 day = 1440 minutes
    })

    it('should handle multiple reminders with different modifications', () => {
      const formData: TaskFormData = {
        title: 'Test Task',
        description: '',
        startDate: '2025-10-15',
        startTime: '10:00',
        endDate: '2025-10-15',
        endTime: '11:00',
        location: '',
        color: '#3b82f6',
        isRecurring: false,
        reminders: [
          {
            id: 1,
            offsetMinutes: 15, // Modified
            offsetValue: 30,
            offsetUnit: 'minutes',
            notificationType: NotificationType.PUSH
          },
          {
            id: 2,
            offsetMinutes: 60, // Not modified, should stay 60
            offsetValue: 60,
            offsetUnit: 'minutes',
            notificationType: NotificationType.EMAIL
          },
          {
            // New reminder (no ID)
            offsetMinutes: 10,
            offsetValue: 10,
            offsetUnit: 'minutes',
            notificationType: NotificationType.PUSH
          }
        ]
      }

      const result = transformTaskForUpdate(formData)

      expect(result.reminders).toHaveLength(3)

      // First reminder: should use new value
      expect(result.reminders[0].id).toBe(1)
      expect(result.reminders[0].reminderOffsetMinutes).toBe(30)

      // Second reminder: should keep same value
      expect(result.reminders[1].id).toBe(2)
      expect(result.reminders[1].reminderOffsetMinutes).toBe(60)

      // Third reminder: new one (no ID)
      expect(result.reminders[2].id).toBeUndefined()
      expect(result.reminders[2].reminderOffsetMinutes).toBe(10)
    })

    it('should handle notification type changes', () => {
      const formData: TaskFormData = {
        title: 'Test Task',
        description: '',
        startDate: '2025-10-15',
        startTime: '10:00',
        endDate: '2025-10-15',
        endTime: '11:00',
        location: '',
        color: '#3b82f6',
        isRecurring: false,
        reminders: [
          {
            id: 1,
            offsetMinutes: 15,
            offsetValue: 15,
            offsetUnit: 'minutes',
            notificationType: NotificationType.EMAIL // Changed from PUSH to EMAIL
          }
        ]
      }

      const result = transformTaskForUpdate(formData)

      expect(result.reminders[0].notificationType).toBe(NotificationType.EMAIL)
    })

    it('should use default value when offsetValue/offsetUnit are missing', () => {
      const formData: TaskFormData = {
        title: 'Test Task',
        description: '',
        startDate: '2025-10-15',
        startTime: '10:00',
        endDate: '2025-10-15',
        endTime: '11:00',
        location: '',
        color: '#3b82f6',
        isRecurring: false,
        reminders: [
          {
            // Missing offsetValue and offsetUnit
            notificationType: NotificationType.PUSH
          } as any
        ]
      }

      const result = transformTaskForCreation(formData)

      // Should use default value of 15 minutes
      expect(result.reminders[0].reminderOffsetMinutes).toBe(15)
    })
  })

  describe('transformTaskToFormData - should preserve reminder IDs', () => {
    it('should include reminder IDs when transforming task to form data', () => {
      const task: Task = {
        id: 1,
        title: 'Test Task',
        description: 'Test',
        startDatetime: '2025-10-15T08:00:00.000Z',
        endDatetime: '2025-10-15T09:00:00.000Z',
        location: '',
        color: '#3b82f6',
        isRecurring: false,
        createdAt: '2025-10-15T08:00:00.000Z',
        updatedAt: '2025-10-15T08:00:00.000Z',
        userId: 1,
        reminders: [
          {
            id: 100,
            taskId: 1,
            reminderTime: '2025-10-15T07:45:00.000Z',
            reminderOffsetMinutes: 15,
            isSent: false,
            notificationType: NotificationType.PUSH,
            createdAt: '2025-10-15T08:00:00.000Z'
          },
          {
            id: 101,
            taskId: 1,
            reminderTime: '2025-10-15T07:00:00.000Z',
            reminderOffsetMinutes: 60,
            isSent: false,
            notificationType: NotificationType.EMAIL,
            createdAt: '2025-10-15T08:00:00.000Z'
          }
        ]
      }

      const formData = transformTaskToFormData(task)

      expect(formData.reminders).toHaveLength(2)

      // First reminder should have ID preserved
      expect(formData.reminders[0].id).toBe(100)
      expect(formData.reminders[0].offsetMinutes).toBe(15)
      expect(formData.reminders[0].offsetValue).toBe(15)
      expect(formData.reminders[0].offsetUnit).toBe('minutes')

      // Second reminder should have ID preserved and convert to hours
      expect(formData.reminders[1].id).toBe(101)
      expect(formData.reminders[1].offsetMinutes).toBe(60)
      expect(formData.reminders[1].offsetValue).toBe(1)
      expect(formData.reminders[1].offsetUnit).toBe('hours')
    })
  })

  describe('Edge cases', () => {
    it('should handle empty reminders array', () => {
      const formData: TaskFormData = {
        title: 'Test Task',
        description: '',
        startDate: '2025-10-15',
        startTime: '10:00',
        endDate: '2025-10-15',
        endTime: '11:00',
        location: '',
        color: '#3b82f6',
        isRecurring: false,
        reminders: []
      }

      const result = transformTaskForCreation(formData)

      expect(result.reminders).toEqual([])
    })

    it('should handle undefined reminders', () => {
      const formData: TaskFormData = {
        title: 'Test Task',
        description: '',
        startDate: '2025-10-15',
        startTime: '10:00',
        endDate: '2025-10-15',
        endTime: '11:00',
        location: '',
        color: '#3b82f6',
        isRecurring: false,
        reminders: undefined as any
      }

      const result = transformTaskForCreation(formData)

      expect(result.reminders).toEqual([])
    })
  })
})
