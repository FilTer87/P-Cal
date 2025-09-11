import { ref, computed } from 'vue'
import { useTasksStore } from '../stores/tasks'
import { useNotifications } from './useNotifications'
import type { 
  Task, 
  CreateTaskRequest, 
  UpdateTaskRequest, 
  TaskFilters,
  TaskFormData,
  ReminderFormData,
  NotificationType 
} from '../types/task'
import { format, addMinutes, parseISO } from 'date-fns'
import { 
  transformTaskFromBackend,
  transformTasksFromBackend,
  transformTaskForCreation, 
  transformTaskForUpdate,
  transformTaskToFormData
} from '../services/taskDateService'

export function useTasks() {
  const tasksStore = useTasksStore()
  const { showSuccess, showError, showTaskCompleted, showTaskCreated } = useNotifications()

  // Form state
  const isFormLoading = ref(false)
  const formErrors = ref<Record<string, string>>({})

  // Computed properties
  const tasks = computed(() => tasksStore.filteredTasks)
  const allTasks = computed(() => tasksStore.tasks)
  const completedTasks = computed(() => tasksStore.completedTasks)
  const pendingTasks = computed(() => tasksStore.pendingTasks)
  const overdueTasks = computed(() => tasksStore.overdueTasks)
  const todayTasks = computed(() => tasksStore.todayTasks)
  const thisWeekTasks = computed(() => tasksStore.thisWeekTasks)
  const urgentTasks = computed(() => tasksStore.urgentTasks)
  const highPriorityTasks = computed(() => tasksStore.highPriorityTasks)
  const taskStats = computed(() => tasksStore.taskStats)
  const isLoading = computed(() => tasksStore.isLoading)
  const error = computed(() => tasksStore.error)
  const searchQuery = computed(() => tasksStore.searchQuery)
  const filters = computed(() => tasksStore.filters)

  // Actions
  const fetchTasks = async (force = false) => {
    await tasksStore.fetchTasks(force)
  }

  const fetchOverdueTasks = async () => {
    return await tasksStore.fetchOverdueTasks()
  }

  const fetchTodayTasks = async () => {
    return await tasksStore.fetchTodayTasks()
  }

  const fetchTaskStats = async () => {
    return await tasksStore.fetchTaskStats()
  }

  const refreshStatistics = async () => {
    await tasksStore.refreshStatistics()
  }

  const createTask = async (taskData: CreateTaskRequest): Promise<Task | null> => {
    isFormLoading.value = true
    formErrors.value = {}

    try {
      const validation = validateTaskData(taskData)
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
      showError(error.message || 'Errore nella creazione dell\'attività')
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
      showError(error.message || 'Errore nell\'aggiornamento dell\'attività')
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
      showError(error.message || 'Errore nell\'eliminazione dell\'attività')
      return false
    } finally {
      isFormLoading.value = false
    }
  }

  const toggleTaskCompletion = async (taskId: number): Promise<boolean> => {
    const task = getTaskById(taskId)
    if (!task) return false

    const success = await tasksStore.toggleTaskCompletion(taskId)
    if (success && !task.completed) {
      showTaskCompleted(task.title)
    }
    return success
  }

  const markTaskCompleted = async (taskId: number): Promise<boolean> => {
    const task = getTaskById(taskId)
    const success = await tasksStore.markTaskCompleted(taskId)
    if (success && task) {
      showTaskCompleted(task.title)
    }
    return success
  }

  const markTaskPending = async (taskId: number): Promise<boolean> => {
    return await tasksStore.markTaskPending(taskId)
  }

  // Search and filters
  const searchTasks = async (query: string) => {
    await tasksStore.searchTasks(query)
  }

  const clearSearch = () => {
    tasksStore.clearSearch()
  }

  const setFilters = (newFilters: TaskFilters) => {
    tasksStore.setFilters(newFilters)
  }

  const clearFilters = () => {
    tasksStore.clearFilters()
  }

  // Note: Priority filtering removed as it's no longer part of the Task model
  // const filterByPriority = (priority: TaskPriority) => {
  //   setFilters({ priority })
  // }

  const filterByCompletion = (completed: boolean) => {
    setFilters({ completed })
  }

  const filterByDateRange = (fromDate: string, toDate: string) => {
    setFilters({ dueDateFrom: fromDate, dueDateTo: toDate })
  }

  // Getters
  const getTaskById = (taskId: number): Task | undefined => {
    return tasksStore.getTaskById(taskId)
  }

  const getTasksByDate = (date: string): Task[] => {
    return tasksStore.getTasksByDate(date)
  }

  // Note: Priority-based queries removed as it's no longer part of the Task model
  // const getTasksByPriority = (priority: TaskPriority): Task[] => {
  //   return tasksStore.getTasksByPriority(priority)
  // }

  const getUpcomingTasks = (days = 7): Task[] => {
    return tasksStore.getUpcomingTasks(days)
  }

  const hasTasksOnDate = (date: string): boolean => {
    return tasksStore.hasTasksOnDate(date)
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

  // Validation
  const validateTaskData = (taskData: CreateTaskRequest | TaskFormData): {
    isValid: boolean
    errors: Record<string, string>
  } => {
    const errors: Record<string, string> = {}

    if (!taskData.title?.trim()) {
      errors.title = 'Il titolo è obbligatorio'
    } else if (taskData.title.trim().length > 255) {
      errors.title = 'Il titolo non può superare i 255 caratteri'
    }

    if (taskData.description && taskData.description.length > 1000) {
      errors.description = 'La descrizione non può superare i 1000 caratteri'
    }

    // Validate start and end datetime
    if ('startDate' in taskData && taskData.startDate) {
      const startDate = new Date(taskData.startDate)
      if (isNaN(startDate.getTime())) {
        errors.startDate = 'Data di inizio non valida'
      }
    }
    
    if ('endDate' in taskData && taskData.endDate) {
      const endDate = new Date(taskData.endDate)
      if (isNaN(endDate.getTime())) {
        errors.endDate = 'Data di fine non valida'
      }
    }
    
    // Validate that end is after start
    if ('startDate' in taskData && 'endDate' in taskData && 
        taskData.startDate && taskData.endDate) {
      const start = new Date(`${taskData.startDate}T${taskData.startTime || '00:00'}:00`)
      const end = new Date(`${taskData.endDate}T${taskData.endTime || '23:59'}:00`)
      
      if (end <= start) {
        errors.endDate = 'La data di fine deve essere successiva alla data di inizio'
      }
    }

    return {
      isValid: Object.keys(errors).length === 0,
      errors
    }
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
    
    if (!formData.isAllDay && formData.startTime && !/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(formData.startTime)) {
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
    
    if (!formData.isAllDay && formData.endTime && !/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(formData.endTime)) {
      errors.endTime = 'Ora di fine non valida (formato HH:MM)'
    }
    
    // Validate that end is after start
    if (formData.startDate && formData.endDate) {
      const startTimeStr = !formData.isAllDay ? (formData.startTime || '00:00') : '00:00'
      const endTimeStr = !formData.isAllDay ? (formData.endTime || '23:59') : '23:59'
      
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
        endIsAfterStart: end > start,
        isAllDay: formData.isAllDay
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

  // Bulk operations
  const bulkMarkCompleted = async (taskIds: number[]): Promise<boolean> => {
    isFormLoading.value = true

    try {
      await Promise.all(taskIds.map(id => markTaskCompleted(id)))
      showSuccess(`${taskIds.length} attività completate`)
      return true
    } catch (error: any) {
      showError('Errore nelle operazioni multiple')
      return false
    } finally {
      isFormLoading.value = false
    }
  }

  const bulkDelete = async (taskIds: number[]): Promise<boolean> => {
    isFormLoading.value = true

    try {
      await Promise.all(taskIds.map(id => deleteTask(id)))
      showSuccess(`${taskIds.length} attività eliminate`)
      return true
    } catch (error: any) {
      showError('Errore nelle operazioni multiple')
      return false
    } finally {
      isFormLoading.value = false
    }
  }

  // Note: Bulk priority update removed as priority is no longer part of the Task model
  // const bulkUpdatePriority = async (taskIds: number[], priority: TaskPriority): Promise<boolean> => {
  //   isFormLoading.value = true
  //   try {
  //     await Promise.all(taskIds.map(id => updateTask(id, { priority })))
  //     showSuccess(`Priorità aggiornata per ${taskIds.length} attività`)
  //     return true
  //   } catch (error: any) {
  //     showError('Errore nell\'aggiornamento della priorità')
  //     return false
  //   } finally {
  //     isFormLoading.value = false
  //   }
  // }

  // Utility functions
  const isDueSoon = (task: Task, hours = 24): boolean => {
    if (task.completed) return false
    
    const startDate = new Date(task.startDatetime)
    const now = new Date()
    const diffHours = (startDate.getTime() - now.getTime()) / (1000 * 60 * 60)
    
    return diffHours > 0 && diffHours <= hours
  }

  const isOverdue = (task: Task): boolean => {
    if (task.completed) return false
    
    const endDate = new Date(task.endDatetime)
    const now = new Date()
    
    return endDate < now
  }

  const getTaskStatusColor = (task: Task): string => {
    if (task.completed) return 'green'
    if (isOverdue(task)) return 'red'
    if (isDueSoon(task)) return 'yellow'
    return 'blue'
  }

  const refreshTasks = () => {
    tasksStore.refreshTasks()
  }

  return {
    // State
    isFormLoading,
    formErrors,

    // Computed
    tasks,
    allTasks,
    completedTasks,
    pendingTasks,
    overdueTasks,
    todayTasks,
    thisWeekTasks,
    urgentTasks,
    highPriorityTasks,
    taskStats,
    isLoading,
    error,
    searchQuery,
    filters,

    // Actions
    fetchTasks,
    fetchOverdueTasks,
    fetchTodayTasks,
    fetchTaskStats,
    refreshStatistics,
    createTask,
    updateTask,
    deleteTask,
    toggleTaskCompletion,
    markTaskCompleted,
    markTaskPending,

    // Search and filters
    searchTasks,
    clearSearch,
    setFilters,
    clearFilters,
    // filterByPriority, // Removed as priority no longer exists
    filterByCompletion,
    filterByDateRange,

    // Getters
    getTaskById,
    getTasksByDate,
    // getTasksByPriority, // Removed as priority no longer exists
    getUpcomingTasks,
    hasTasksOnDate,

    // Form helpers
    createEmptyTaskForm,
    createTaskFormFromTask,
    convertFormToTaskRequest,
    convertFormToUpdateRequest,

    // Validation
    validateTaskData,
    validateTaskForm,

    // Reminder helpers
    addReminderToForm,
    removeReminderFromForm,
    createReminderFromPreset,

    // Bulk operations
    bulkMarkCompleted,
    bulkDelete,
    // bulkUpdatePriority, // Removed as priority no longer exists

    // Utilities
    isDueSoon,
    isOverdue,
    getTaskStatusColor,
    refreshTasks
  }
}