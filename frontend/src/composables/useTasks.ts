import { ref, computed } from 'vue'
import { useTasksStore } from '../stores/tasks'
import { useCustomToast } from './useCustomToast'
import type { 
  Task, 
  CreateTaskRequest, 
  UpdateTaskRequest, 
  TaskFormData,
  ReminderFormData
} from '../types/task'
import { format, addMinutes, parseISO } from 'date-fns'
import { 
  transformTaskForCreation, 
  transformTaskForUpdate,
  transformTaskToFormData
} from '../services/taskDateService'

export function useTasks() {
  const tasksStore = useTasksStore()
  const { showSuccess, showError } = useCustomToast()

  const showTaskCompleted = (taskTitle: string) => {
    showSuccess(`Attività "${taskTitle}" completata!`)
  }

  const showTaskCreated = (taskTitle: string) => {
    showSuccess(`Attività "${taskTitle}" creata con successo!`)
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
        startDate: taskData.startDatetime ? format(new Date(taskData.startDatetime), 'yyyy-MM-dd') : '',
        startTime: taskData.startDatetime ? format(new Date(taskData.startDatetime), 'HH:mm') : '',
        endDate: taskData.endDatetime ? format(new Date(taskData.endDatetime), 'yyyy-MM-dd') : '',
        endTime: taskData.endDatetime ? format(new Date(taskData.endDatetime), 'HH:mm') : '',
        location: taskData.location || '',
        color: taskData.color || '#3788d8',
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

  const updateTask = async (taskId: number, taskData: UpdateTaskRequest): Promise<Task | null> => {
    isFormLoading.value = true
    formErrors.value = {}

    try {
      const task = await tasksStore.updateTask(taskId, taskData)
      if (task) {
        showSuccess('Attività aggiornata con successo!')
        return task
      }
      return null
    } catch (error: any) {
      return null
    } finally {
      isFormLoading.value = false
    }
  }

  const deleteTask = async (taskId: number): Promise<boolean> => {
    isFormLoading.value = true

    try {
      const success = await tasksStore.deleteTask(taskId)
      if (success) {
        showSuccess('Attività eliminata con successo!')
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

    if (!formData.title.trim()) {
      errors.title = 'Il titolo è obbligatorio'
    } else if (formData.title.trim().length > 255) {
      errors.title = 'Il titolo non può superare i 255 caratteri'
    }

    if (formData.description && formData.description.length > 1000) {
      errors.description = 'La descrizione non può superare i 1000 caratteri'
    }

    // Validate start date/time
    if (!formData.startDate) {
      errors.startDate = 'La data di inizio è obbligatoria'
    } else {
      const startDate = new Date(formData.startDate)
      if (isNaN(startDate.getTime())) {
        errors.startDate = 'Data di inizio non valida'
      }
    }
    
    if (formData.startTime && !/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(formData.startTime)) {
      errors.startTime = 'Ora di inizio non valida (formato HH:MM)'
    }
    
    // Validate end date/time
    if (!formData.endDate) {
      errors.endDate = 'La data di fine è obbligatoria'
    } else {
      const endDate = new Date(formData.endDate)
      if (isNaN(endDate.getTime())) {
        errors.endDate = 'Data di fine non valida'
      }
    }
    
    if (formData.endTime && !/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(formData.endTime)) {
      errors.endTime = 'Ora di fine non valida (formato HH:MM)'
    }
    
    // Validate that end is after start
    if (formData.startDate && formData.endDate) {
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
        errors.endDate = 'Date non valide'
      } else if (end <= start) {
        errors.endDate = 'La data/ora di fine deve essere successiva alla data/ora di inizio'
      }
    }
    
    // Validate color format
    if (formData.color && !/^#[0-9A-Fa-f]{6}$/.test(formData.color)) {
      errors.color = 'Colore non valido (deve essere in formato #RRGGBB)'
    }
    
    // Validate location length
    if (formData.location && formData.location.length > 200) {
      errors.location = 'Il luogo non può superare i 200 caratteri'
    }

    // Validate reminders
    formData.reminders.forEach((reminder, index) => {
      if (reminder.offsetMinutes < 0) {
        errors[`reminder_${index}`] = 'Il tempo del promemoria deve essere positivo'
      }
    })

    return {
      isValid: Object.keys(errors).length === 0,
      errors
    }
  }

  // Reminder helpers
  const addReminderToForm = (formData: TaskFormData): ReminderFormData => {
    const newReminder: ReminderFormData = {
      date: formData.dueDate,
      time: '09:00'
    }
    formData.reminders.push(newReminder)
    return newReminder
  }

  const removeReminderFromForm = (formData: TaskFormData, index: number) => {
    formData.reminders.splice(index, 1)
  }

  const createReminderFromPreset = (dueDate: string, dueTime: string, offsetMinutes: number): ReminderFormData => {
    const dueDateTime = parseISO(`${dueDate}T${dueTime}:00`)
    const reminderDateTime = addMinutes(dueDateTime, -offsetMinutes)
    
    return {
      date: format(reminderDateTime, 'yyyy-MM-dd'),
      time: format(reminderDateTime, 'HH:mm'),
      reminderDateTime: reminderDateTime.toISOString()
    }
  }


  // Utility functions
  const isDueSoon = (task: Task, hours = 24): boolean => {
    const startDate = new Date(task.startDatetime)
    const now = new Date()
    const diffHours = (startDate.getTime() - now.getTime()) / (1000 * 60 * 60)

    return diffHours > 0 && diffHours <= hours
  }

  const isPastEvent = (task: Task): boolean => {
    const endDate = new Date(task.endDatetime)
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

    // Reminder helpers (keep for form functionality)
    addReminderToForm,
    removeReminderFromForm,
    createReminderFromPreset,

    // Utilities (only used ones)
    isDueSoon,
    isPastEvent,
    getTaskStatusColor,

    // Getters (used by CalendarView)
    getTaskById: (taskId: number) => tasksStore.getTaskById(taskId),
    getTasksForDate: (date: string) => tasksStore.getTasksByDate(date)
  }
}