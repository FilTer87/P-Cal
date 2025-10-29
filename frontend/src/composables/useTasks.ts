import { ref, computed } from 'vue'
import { useTasksStore } from '../stores/tasks'
import { useCustomToast } from './useCustomToast'
import type {
  Task,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskFormData,
} from '../types/task'
import { format } from 'date-fns'
import {
  transformTaskForCreation,
  transformTaskForUpdate,
  transformTaskToFormData
} from '../services/taskDateService'
import { i18nGlobal } from '../i18n'

export function useTasks() {
  const tasksStore = useTasksStore()
  const { showSuccess, showError } = useCustomToast()

  const showTaskCompleted = (taskTitle: string) => {
    showSuccess(i18nGlobal.t('composables.useTasks.taskCompleted', { title: taskTitle }))
  }

  const showTaskCreated = (taskTitle: string) => {
    showSuccess(i18nGlobal.t('composables.useTasks.taskCreated', { title: taskTitle }))
  }

  // Form state
  const isFormLoading = ref(false)
  const formErrors = ref<Record<string, string>>({})

  // Only keep the computed properties that are actually used
  const allTasks = computed(() => tasksStore.tasks)

  // Keep only the wrapper functions that are actually used
  const fetchTasks = async (force = false) => {
    await tasksStore.fetchTasks(force)
  }

  const fetchTasksByDateRange = async (startDate: string, endDate: string) => {
    await tasksStore.fetchTasksByDateRange(startDate, endDate)
  }

  const fetchTaskById = async (taskId: string): Promise<Task | null> => {
    return await tasksStore.fetchTaskById(taskId)
  }

  const refreshStatistics = async () => {
    await tasksStore.refreshStatistics()
  }

  const createTask = async (taskData: CreateTaskRequest): Promise<Task | null> => {
    isFormLoading.value = true
    formErrors.value = {}

    try {
      // For API validation, convert to form and validate
      const formData = {
        title: taskData.title,
        description: taskData.description || '',
        startDate: taskData.startDatetimeLocal ? taskData.startDatetimeLocal.split('T')[0] : '',
        startTime: taskData.startDatetimeLocal ? taskData.startDatetimeLocal.split('T')[1]?.substring(0, 5) || '00:00' : '',
        endDate: taskData.endDatetimeLocal ? taskData.endDatetimeLocal.split('T')[0] : '',
        endTime: taskData.endDatetimeLocal ? taskData.endDatetimeLocal.split('T')[1]?.substring(0, 5) || '23:59' : '',
        location: taskData.location || '',
        color: taskData.color || '#3788d8',
        isRecurring: taskData.recurrenceRule || false,
        isAllDay: taskData.isAllDay || false,
        reminders: []
      } as TaskFormData
      const validation = validateTaskForm(formData)
      if (!validation.isValid) {
        formErrors.value = validation.errors
        return null
      }

      const task = await tasksStore.createTask(taskData)
      if (task) {
        showTaskCreated(task.title)
        return task
      }
      return null
    } catch (error: any) {
      return null
    } finally {
      isFormLoading.value = false
    }
  }

  const updateTask = async (taskId: string, taskData: UpdateTaskRequest, occurrenceStart?: string): Promise<Task | null> => {
    isFormLoading.value = true
    formErrors.value = {}

    try {
      const task = await tasksStore.updateTask(taskId, taskData, occurrenceStart)
      if (task) {
        showSuccess(i18nGlobal.t('composables.useTasks.taskUpdated'))
        return task
      }
      return null
    } catch (error: any) {
      return null
    } finally {
      isFormLoading.value = false
    }
  }

  const deleteTask = async (taskId: string): Promise<boolean> => {
    isFormLoading.value = true

    try {
      const success = await tasksStore.deleteTask(taskId)
      if (success) {
        showSuccess(i18nGlobal.t('composables.useTasks.taskDeleted'))
      }
      return success
    } catch (error: any) {
      return false
    } finally {
      isFormLoading.value = false
    }
  }




  // Form helpers
  const createEmptyTaskForm = (): TaskFormData => {
    const now = new Date()
    const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000)

    return {
      title: '',
      description: '',
      startDate: format(now, 'yyyy-MM-dd'),
      startTime: format(now, 'HH:mm'),
      endDate: format(oneHourLater, 'yyyy-MM-dd'),
      endTime: format(oneHourLater, 'HH:mm'),
      location: '',
      color: '#3788d8',
      isAllDay: false,
      isRecurring: false,
      recurrenceFrequency: undefined,
      recurrenceInterval: undefined,
      recurrenceEndType: undefined,
      recurrenceCount: undefined,
      recurrenceEndDate: undefined,
      recurrenceByDay: undefined,
      reminders: []
    }
  }

  const createTaskFormFromTask = (task: Task): TaskFormData => {
    return transformTaskToFormData(task)
  }

  const convertFormToTaskRequest = (formData: TaskFormData): CreateTaskRequest => {
    return transformTaskForCreation(formData)
  }

  const convertFormToUpdateRequest = (formData: TaskFormData): UpdateTaskRequest => {
    return transformTaskForUpdate(formData)
  }


  const validateTaskForm = (formData: TaskFormData): {
    isValid: boolean
    errors: Record<string, string>
  } => {
    const errors: Record<string, string> = {}
    const t = i18nGlobal.t

    if (!formData.title.trim()) {
      errors.title = t('composables.useTasks.validation.titleRequired')
    } else if (formData.title.trim().length > 255) {
      errors.title = t('composables.useTasks.validation.titleTooLong')
    }

    if (formData.description && formData.description.length > 1000) {
      errors.description = t('composables.useTasks.validation.descriptionTooLong')
    }

    // Validate start date/time
    if (!formData.startDate) {
      errors.startDate = t('composables.useTasks.validation.startDateRequired')
    } else {
      const startDate = new Date(formData.startDate)
      if (isNaN(startDate.getTime())) {
        errors.startDate = t('composables.useTasks.validation.startDateInvalid')
      }
    }

    if (formData.startTime && !/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(formData.startTime)) {
      errors.startTime = t('composables.useTasks.validation.startTimeInvalid')
    }

    // Validate end date/time (only for non-all-day events)
    if (!formData.isAllDay) {
      if (!formData.endDate) {
        errors.endDate = t('composables.useTasks.validation.endDateRequired')
      } else {
        const endDate = new Date(formData.endDate)
        if (isNaN(endDate.getTime())) {
          errors.endDate = t('composables.useTasks.validation.endDateInvalid')
        }
      }

      if (formData.endTime && !/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(formData.endTime)) {
        errors.endTime = t('composables.useTasks.validation.endTimeInvalid')
      }
    }

    // Validate that end is after start (only for non-all-day events)
    if (!formData.isAllDay && formData.startDate && formData.endDate) {
      const startTimeStr = formData.startTime || '00:00'
      const endTimeStr = formData.endTime || '23:59'
      
      // Create dates more safely
      const start = new Date(formData.startDate + 'T' + startTimeStr + ':00.000Z')
      const end = new Date(formData.endDate + 'T' + endTimeStr + ':00.000Z')
      
      console.log('🔍 Date validation debug:', {
        startDate: formData.startDate,
        startTime: startTimeStr,
        endDate: formData.endDate,
        endTime: endTimeStr,
        startDateTime: start.toISOString(),
        endDateTime: end.toISOString(),
        endIsAfterStart: end > start
      })
      
      // Check if dates are valid
      if (isNaN(start.getTime()) || isNaN(end.getTime())) {
        errors.endDate = t('composables.useTasks.validation.datesInvalid')
      } else if (end <= start) {
        errors.endDate = t('composables.useTasks.validation.endBeforeStart')
      }
    }

    // Validate color format
    if (formData.color && !/^#[0-9A-Fa-f]{6}$/.test(formData.color)) {
      errors.color = t('composables.useTasks.validation.colorInvalid')
    }

    // Validate location length
    if (formData.location && formData.location.length > 200) {
      errors.location = t('composables.useTasks.validation.locationTooLong')
    }

    // Validate reminders
    formData.reminders.forEach((reminder, index) => {
      if (reminder.offsetMinutes < 0) {
        errors[`reminder_${index}`] = t('composables.useTasks.validation.reminderInvalid')
      }
    })

    return {
      isValid: Object.keys(errors).length === 0,
      errors
    }
  }

  // Utility functions
  const isDueSoon = (task: Task, hours = 24): boolean => {
    // Parse local datetime string (e.g., "2025-10-20T15:00:00")
    const startDate = new Date(task.startDatetimeLocal)
    const now = new Date()
    const diffHours = (startDate.getTime() - now.getTime()) / (1000 * 60 * 60)

    return diffHours > 0 && diffHours <= hours
  }

  const isPastEvent = (task: Task): boolean => {
    // Parse local datetime string (e.g., "2025-10-20T16:00:00")
    const endDate = new Date(task.endDatetimeLocal)
    const now = new Date()

    return endDate < now
  }

  const getTaskStatusColor = (task: Task): string => {
    if (isDueSoon(task)) return 'yellow'
    return task.color || 'blue'
  }


  return {
    // State
    isFormLoading,
    formErrors,

    // Computed (only used ones)
    allTasks,

    // Actions (only used ones)
    fetchTasks,
    fetchTasksByDateRange,
    fetchTaskById,
    refreshStatistics,
    createTask,
    updateTask,
    deleteTask,

    // Form helpers
    createEmptyTaskForm,
    createTaskFormFromTask,
    convertFormToTaskRequest,
    convertFormToUpdateRequest,

    // Validation
    validateTaskForm,

    // Utilities (only used ones)
    isDueSoon,
    isPastEvent,
    getTaskStatusColor,

    // Getters (used by CalendarView)
    getTaskById: (taskId: string) => tasksStore.getTaskById(taskId),
    getTasksForDate: (date: string) => tasksStore.getTasksByDate(date)
  }
}