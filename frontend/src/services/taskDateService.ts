/**
 * Centralized service for handling task date transformations
 * Automatically converts between UTC (backend) and local timezone (frontend)
 */

import { localDateTimeToUTC, localDateToUTC, utcToLocalDate, utcToLocalTime } from '../utils/timezone'
import type { Task, CreateTaskRequest, UpdateTaskRequest, TaskFormData } from '../types/task'

/**
 * Transform task data from backend (UTC) to frontend (local) format
 */
export function transformTaskFromBackend(task: Task): Task {
  return {
    ...task,
    // Keep original UTC times for API calls, but add local display fields
    _displayStartDate: task.startDatetime ? utcToLocalDate(task.startDatetime) : undefined,
    _displayStartTime: task.startDatetime ? utcToLocalTime(task.startDatetime) : undefined,
    _displayEndDate: task.endDatetime ? utcToLocalDate(task.endDatetime) : undefined,
    _displayEndTime: task.endDatetime ? utcToLocalTime(task.endDatetime) : undefined,
  }
}

/**
 * Transform task form data from frontend (local) to backend (UTC) format for creation
 */
export function transformTaskForCreation(formData: TaskFormData): CreateTaskRequest {
  const startDatetime = formData.isAllDay 
    ? localDateToUTC(formData.startDate)
    : localDateTimeToUTC(formData.startDate, formData.startTime)
    
  const endDatetime = formData.isAllDay
    ? localDateToUTC(formData.endDate) 
    : localDateTimeToUTC(formData.endDate, formData.endTime)

  return {
    title: formData.title.trim(),
    description: formData.description?.trim() || undefined,
    startDatetime,
    endDatetime,
    location: formData.location?.trim() || undefined,
    color: formData.color,
    isAllDay: formData.isAllDay,
    reminders: formData.reminders?.map(reminder => ({
      reminderOffsetMinutes: reminder.offsetMinutes || reminder.reminderOffsetMinutes,
      notificationType: reminder.notificationType
    })) || []
  }
}

/**
 * Transform task form data from frontend (local) to backend (UTC) format for update
 */
export function transformTaskForUpdate(formData: TaskFormData): UpdateTaskRequest {
  const startDatetime = formData.isAllDay 
    ? localDateToUTC(formData.startDate)
    : localDateTimeToUTC(formData.startDate, formData.startTime)
    
  const endDatetime = formData.isAllDay
    ? localDateToUTC(formData.endDate) 
    : localDateTimeToUTC(formData.endDate, formData.endTime)

  return {
    title: formData.title.trim(),
    description: formData.description?.trim() || undefined,
    startDatetime,
    endDatetime,
    location: formData.location?.trim() || undefined,
    color: formData.color,
    isAllDay: formData.isAllDay,
    reminders: formData.reminders?.map(reminder => ({
      reminderOffsetMinutes: reminder.offsetMinutes || reminder.reminderOffsetMinutes,
      notificationType: reminder.notificationType
    })) || []
  }
}

/**
 * Transform task data from backend to form data for editing
 */
export function transformTaskToFormData(task: Task): TaskFormData {
  return {
    title: task.title || '',
    description: task.description || '',
    startDate: task.startDatetime ? utcToLocalDate(task.startDatetime) : '',
    startTime: task.startDatetime ? utcToLocalTime(task.startDatetime) : '',
    endDate: task.endDatetime ? utcToLocalDate(task.endDatetime) : '',
    endTime: task.endDatetime ? utcToLocalTime(task.endDatetime) : '',
    location: task.location || '',
    color: task.color || '#3788d8',
    isAllDay: task.isAllDay || false,
    reminders: task.reminders?.map(reminder => ({
      offsetMinutes: reminder.reminderOffsetMinutes || 15,
      reminderOffsetMinutes: reminder.reminderOffsetMinutes || 15,
      notificationType: reminder.notificationType || 'PUSH'
    })) || []
  }
}

/**
 * Transform simple task data for quick add functionality
 */
export function transformQuickTaskData(data: {
  title: string
  date: string
  time: string
  isAllDay: boolean
  color: string
}): CreateTaskRequest {
  const startDatetime = data.isAllDay 
    ? localDateToUTC(data.date)
    : localDateTimeToUTC(data.date, data.time)
    
  // For quick add, end time is 1 hour after start
  const [hour, minute] = data.time.split(':').map(n => parseInt(n))
  const endHour = hour + 1
  const endTime = `${endHour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`
  
  const endDatetime = data.isAllDay
    ? localDateToUTC(data.date) 
    : localDateTimeToUTC(data.date, endTime)

  return {
    title: data.title.trim(),
    startDatetime,
    endDatetime,
    color: data.color,
    isAllDay: data.isAllDay,
    reminders: []
  }
}

/**
 * Batch transform tasks from backend
 */
export function transformTasksFromBackend(tasks: Task[]): Task[] {
  return tasks.map(transformTaskFromBackend)
}