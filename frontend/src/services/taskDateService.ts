/**
 * Centralized service for handling task date transformations
 * Automatically converts between UTC (backend) and local timezone (frontend)
 */

import { localDateTimeToUTC, localDateToUTC, utcToLocalDate, utcToLocalTime } from '../utils/timezone'
import type { Task, CreateTaskRequest, UpdateTaskRequest, TaskFormData } from '../types/task'
import { buildRRule, parseRRule } from '../utils/recurrence'
import { RecurrenceEndType } from '../types/task'

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
 * Transform task form data from frontend (local) to backend (UTC) format for creation
 */
export function transformTaskForCreation(formData: TaskFormData): CreateTaskRequest {
  const startDatetime = localDateTimeToUTC(formData.startDate, formData.startTime)
  const endDatetime = localDateTimeToUTC(formData.endDate, formData.endTime)

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
      recurrenceEnd = localDateTimeToUTC(formData.recurrenceEndDate, '23:59')
    }
  }

  return {
    title: formData.title.trim(),
    description: formData.description?.trim() || undefined,
    startDatetime,
    endDatetime,
    location: formData.location?.trim() || undefined,
    color: formData.color,
    recurrenceRule,
    recurrenceEnd,
    reminders: formData.reminders?.map(reminder => {
      const offsetMinutes = reminder.offsetMinutes ||
                           (reminder.offsetValue && reminder.offsetUnit ?
                            convertToMinutes(reminder.offsetValue, reminder.offsetUnit) :
                            reminder.reminderOffsetMinutes) || 15
      return {
        reminderOffsetMinutes: offsetMinutes,
        notificationType: reminder.notificationType
      }
    }) || []
  }
}

/**
 * Transform task form data from frontend (local) to backend (UTC) format for update
 */
export function transformTaskForUpdate(formData: TaskFormData): UpdateTaskRequest {
  const startDatetime = localDateTimeToUTC(formData.startDate, formData.startTime)
  const endDatetime = localDateTimeToUTC(formData.endDate, formData.endTime)

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
      recurrenceEnd = localDateTimeToUTC(formData.recurrenceEndDate, '23:59')
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
    startDatetime,
    endDatetime,
    location: formData.location?.trim() || undefined,
    color: formData.color,
    recurrenceRule,
    recurrenceEnd,
    reminders: formData.reminders?.map(reminder => {
      const offsetMinutes = reminder.offsetMinutes ||
                           (reminder.offsetValue && reminder.offsetUnit ?
                            convertToMinutes(reminder.offsetValue, reminder.offsetUnit) :
                            reminder.reminderOffsetMinutes) || 15
      return {
        reminderOffsetMinutes: offsetMinutes,
        notificationType: reminder.notificationType
      }
    }) || []
  }
}

/**
 * Transform task data from backend to form data for editing
 */
export function transformTaskToFormData(task: Task): TaskFormData {
  // Parse recurrence rule if present
  const recurrenceParams = task.recurrenceRule ? parseRRule(task.recurrenceRule) : null

  return {
    title: task.title || '',
    description: task.description || '',
    startDate: task.startDatetime ? utcToLocalDate(task.startDatetime) : '',
    startTime: task.startDatetime ? utcToLocalTime(task.startDatetime) : '',
    endDate: task.endDatetime ? utcToLocalDate(task.endDatetime) : '',
    endTime: task.endDatetime ? utcToLocalTime(task.endDatetime) : '',
    location: task.location || '',
    color: task.color || '#3788d8',
    isRecurring: task.isRecurring || false,
    recurrenceFrequency: recurrenceParams?.frequency,
    recurrenceInterval: recurrenceParams?.interval,
    recurrenceEndType: recurrenceParams?.endType,
    recurrenceCount: recurrenceParams?.count,
    recurrenceEndDate: recurrenceParams?.endDate ||
                      (task.recurrenceEnd ? utcToLocalDate(task.recurrenceEnd) : undefined),
    recurrenceByDay: recurrenceParams?.byDay,
    reminders: task.reminders?.map(reminder => {
      const offsetMinutes = reminder.reminderOffsetMinutes || 15
      const { offsetValue, offsetUnit } = convertFromMinutes(offsetMinutes)
      return {
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
 */
export function transformQuickTaskData(data: {
  title: string
  date: string
  time: string
  color: string
}): CreateTaskRequest {
  const startDatetime = localDateTimeToUTC(data.date, data.time)

  // For quick add, end time is 1 hour after start
  const [hour, minute] = data.time.split(':').map(n => parseInt(n))
  const endHour = hour + 1
  const endTime = `${endHour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`

  const endDatetime = localDateTimeToUTC(data.date, endTime)

  return {
    title: data.title.trim(),
    startDatetime,
    endDatetime,
    color: data.color,
    reminders: []
  }
}

