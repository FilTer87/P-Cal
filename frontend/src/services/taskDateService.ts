/**
 * Centralized service for handling task date transformations
 * NEW: Direct local time handling - backend now manages timezone/DST
 */

import type { Task, CreateTaskRequest, UpdateTaskRequest, TaskFormData } from '../types/task'
import { buildRRule, parseRRule } from '../utils/recurrence'
import { RecurrenceEndType } from '../types/task'

/**
 * Get user's IANA timezone
 */
export function getUserTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}

/**
 * Combine date and time into ISO 8601 local datetime string
 * Example: "2025-10-20" + "15:00" => "2025-10-20T15:00:00"
 */
function combineDateTime(date: string, time: string): string {
  return `${date}T${time}:00`
}

/**
 * Extract date from ISO 8601 local datetime
 * Example: "2025-10-20T15:00:00" => "2025-10-20"
 */
function extractDate(datetime: string): string {
  return datetime.split('T')[0]
}

/**
 * Extract time from ISO 8601 local datetime
 * Example: "2025-10-20T15:00:00" => "15:00"
 */
function extractTime(datetime: string): string {
  const timePart = datetime.split('T')[1]
  if (!timePart) return '00:00'
  return timePart.substring(0, 5) // HH:MM
}

/**
 * Convert offset value and unit to minutes
 */
function convertToMinutes(offsetValue: number, offsetUnit: string): number {
  const multipliers: Record<string, number> = {
    minutes: 1,
    hours: 60,
    days: 24 * 60
  }
  return offsetValue * (multipliers[offsetUnit] || 1)
}

/**
 * Convert minutes to offset value and unit for form display
 */
function convertFromMinutes(offsetMinutes: number): { offsetValue: number, offsetUnit: string } {
  if (offsetMinutes >= 24 * 60 && offsetMinutes % (24 * 60) === 0) {
    return { offsetValue: offsetMinutes / (24 * 60), offsetUnit: 'days' }
  } else if (offsetMinutes >= 60 && offsetMinutes % 60 === 0) {
    return { offsetValue: offsetMinutes / 60, offsetUnit: 'hours' }
  } else {
    return { offsetValue: offsetMinutes, offsetUnit: 'minutes' }
  }
}

/**
 * Transform task form data to backend format for creation
 * NEW: Direct local time - no timezone conversion needed
 */
export function transformTaskForCreation(formData: TaskFormData): CreateTaskRequest {
  // Combine date and time into local datetime strings
  const startDatetimeLocal = combineDateTime(formData.startDate, formData.startTime)
  const endDatetimeLocal = formData.isAllDay
    ? combineDateTime(formData.startDate, '23:59') // End of start day for all-day events
    : combineDateTime(formData.endDate, formData.endTime)

  // Build recurrence rule if task is recurring
  let recurrenceRule: string | undefined
  let recurrenceEnd: string | undefined

  if (formData.isRecurring && formData.recurrenceFrequency) {
    const rruleParams = {
      frequency: formData.recurrenceFrequency,
      interval: formData.recurrenceInterval || 1,
      endType: formData.recurrenceEndType || RecurrenceEndType.NEVER,
      count: formData.recurrenceCount,
      endDate: formData.recurrenceEndDate,
      byDay: formData.recurrenceByDay
    }
    recurrenceRule = buildRRule(rruleParams)

    // Set recurrenceEnd if using DATE end type
    if (formData.recurrenceEndType === RecurrenceEndType.DATE && formData.recurrenceEndDate) {
      recurrenceEnd = combineDateTime(formData.recurrenceEndDate, '23:59')
    }
  }

  return {
    title: formData.title.trim(),
    description: formData.description?.trim() || undefined,
    startDatetimeLocal,
    endDatetimeLocal,
    timezone: getUserTimezone(),
    location: formData.location?.trim() || undefined,
    color: formData.color,
    isAllDay: formData.isAllDay,
    recurrenceRule,
    recurrenceEnd,
    reminders: formData.reminders?.map(reminder => {
      // Priority: calculate from offsetValue/offsetUnit if present (user may have changed these)
      // Fallback: use pre-calculated values
      const offsetMinutes = (reminder.offsetValue && reminder.offsetUnit ?
                            convertToMinutes(reminder.offsetValue, reminder.offsetUnit) :
                            reminder.offsetMinutes || reminder.reminderOffsetMinutes) || 15
      return {
        id: reminder.id,  // Include ID if present (for updates)
        reminderOffsetMinutes: offsetMinutes,
        notificationType: reminder.notificationType
      }
    }) || []
  }
}

/**
 * Transform task form data to backend format for update
 * NEW: Direct local time - no timezone conversion needed
 */
export function transformTaskForUpdate(formData: TaskFormData): UpdateTaskRequest {
  // Combine date and time into local datetime strings
  const startDatetimeLocal = combineDateTime(formData.startDate, formData.startTime)
  const endDatetimeLocal = formData.isAllDay
    ? combineDateTime(formData.startDate, '23:59') // End of start day for all-day events
    : combineDateTime(formData.endDate, formData.endTime)

  // Build recurrence rule if task is recurring
  let recurrenceRule: string | undefined
  let recurrenceEnd: string | undefined

  if (formData.isRecurring && formData.recurrenceFrequency) {
    const rruleParams = {
      frequency: formData.recurrenceFrequency,
      interval: formData.recurrenceInterval || 1,
      endType: formData.recurrenceEndType || RecurrenceEndType.NEVER,
      count: formData.recurrenceCount,
      endDate: formData.recurrenceEndDate,
      byDay: formData.recurrenceByDay
    }
    recurrenceRule = buildRRule(rruleParams)

    // Set recurrenceEnd if using DATE end type
    if (formData.recurrenceEndType === RecurrenceEndType.DATE && formData.recurrenceEndDate) {
      recurrenceEnd = combineDateTime(formData.recurrenceEndDate, '23:59')
    } else {
      recurrenceEnd = undefined
    }
  } else {
    // Clear recurrence if not recurring
    recurrenceRule = undefined
    recurrenceEnd = undefined
  }

  return {
    title: formData.title.trim(),
    description: formData.description?.trim() || undefined,
    startDatetimeLocal,
    endDatetimeLocal,
    timezone: getUserTimezone(),
    location: formData.location?.trim() || undefined,
    color: formData.color,
    isAllDay: formData.isAllDay,
    recurrenceRule,
    recurrenceEnd,
    reminders: formData.reminders?.map(reminder => {
      // Priority: calculate from offsetValue/offsetUnit if present (user may have changed these)
      // Fallback: use pre-calculated values
      const offsetMinutes = (reminder.offsetValue && reminder.offsetUnit ?
                            convertToMinutes(reminder.offsetValue, reminder.offsetUnit) :
                            reminder.offsetMinutes || reminder.reminderOffsetMinutes) || 15
      return {
        id: reminder.id,  // Include ID if present (for updates)
        reminderOffsetMinutes: offsetMinutes,
        notificationType: reminder.notificationType
      }
    }) || []
  }
}

/**
 * Transform task data from backend to form data for editing
 * NEW: Direct extraction from local datetime strings
 */
export function transformTaskToFormData(task: Task): TaskFormData {
  // Parse recurrence rule if present
  const recurrenceParams = task.recurrenceRule ? parseRRule(task.recurrenceRule) : null

  return {
    title: task.title || '',
    description: task.description || '',
    startDate: task.startDatetimeLocal ? extractDate(task.startDatetimeLocal) : '',
    startTime: task.startDatetimeLocal ? extractTime(task.startDatetimeLocal) : '',
    endDate: task.endDatetimeLocal ? extractDate(task.endDatetimeLocal) : '',
    endTime: task.endDatetimeLocal ? extractTime(task.endDatetimeLocal) : '',
    location: task.location || '',
    color: task.color || '#3788d8',
    isAllDay: task.isAllDay || false,
    isRecurring: task.isRecurring || false,
    recurrenceFrequency: recurrenceParams?.frequency,
    recurrenceInterval: recurrenceParams?.interval,
    recurrenceEndType: recurrenceParams?.endType,
    recurrenceCount: recurrenceParams?.count,
    recurrenceEndDate: recurrenceParams?.endDate ||
                      (task.recurrenceEnd ? extractDate(task.recurrenceEnd) : undefined),
    recurrenceByDay: recurrenceParams?.byDay,
    reminders: task.reminders?.map(reminder => {
      const offsetMinutes = reminder.reminderOffsetMinutes || 15
      const { offsetValue, offsetUnit } = convertFromMinutes(offsetMinutes)
      return {
        id: reminder.id,  // Include reminder ID for updates
        offsetMinutes,
        reminderOffsetMinutes: offsetMinutes,
        offsetValue,
        offsetUnit,
        notificationType: reminder.notificationType || 'PUSH'
      }
    }) || []
  }
}

/**
 * Transform simple task data for quick add functionality
 * NEW: Direct local time handling
 */
export function transformQuickTaskData(data: {
  title: string
  date: string
  time: string
  color: string
}): CreateTaskRequest {
  const startDatetimeLocal = combineDateTime(data.date, data.time)

  // For quick add, end time is 1 hour after start
  const [hour, minute] = data.time.split(':').map(n => parseInt(n))
  const endHour = hour + 1
  const endTime = `${endHour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`

  const endDatetimeLocal = combineDateTime(data.date, endTime)

  return {
    title: data.title.trim(),
    startDatetimeLocal,
    endDatetimeLocal,
    timezone: getUserTimezone(),
    color: data.color,
    reminders: []
  }
}

