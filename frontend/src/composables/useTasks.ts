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
  TaskPriority 
} from '../types/task'
import { format, addMinutes, parseISO } from 'date-fns'

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

  const filterByPriority = (priority: TaskPriority) => {
    setFilters({ priority })
  }

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

  const getTasksByPriority = (priority: TaskPriority): Task[] => {
    return tasksStore.getTasksByPriority(priority)
  }

  const getUpcomingTasks = (days = 7): Task[] => {
    return tasksStore.getUpcomingTasks(days)
  }

  const hasTasksOnDate = (date: string): boolean => {
    return tasksStore.hasTasksOnDate(date)
  }

  // Form helpers
  const createEmptyTaskForm = (): TaskFormData => ({
    title: '',
    description: '',
    priority: 'MEDIUM' as TaskPriority,
    dueDate: format(new Date(), 'yyyy-MM-dd'),
    dueTime: '09:00',
    reminders: []
  })

  const createTaskFormFromTask = (task: Task): TaskFormData => {
    const dueDateTime = task.dueDate ? new Date(task.dueDate) : new Date()
    
    return {
      title: task.title,
      description: task.description || '',
      priority: task.priority,
      dueDate: format(dueDateTime, 'yyyy-MM-dd'),
      dueTime: format(dueDateTime, 'HH:mm'),
      reminders: task.reminders.map(reminder => ({
        id: reminder.id,
        date: format(new Date(reminder.reminderDateTime), 'yyyy-MM-dd'),
        time: format(new Date(reminder.reminderDateTime), 'HH:mm'),
        reminderDateTime: reminder.reminderDateTime
      }))
    }
  }

  const convertFormToTaskRequest = (formData: TaskFormData): CreateTaskRequest => {
    const dueDateTime = formData.dueDate && formData.dueTime
      ? `${formData.dueDate}T${formData.dueTime}:00`
      : undefined

    return {
      title: formData.title.trim(),
      description: formData.description.trim() || undefined,
      priority: formData.priority,
      dueDate: dueDateTime,
      reminders: formData.reminders.map(reminder => ({
        reminderDateTime: reminder.reminderDateTime || `${reminder.date}T${reminder.time}:00`
      }))
    }
  }

  const convertFormToUpdateRequest = (formData: TaskFormData): UpdateTaskRequest => {
    const dueDateTime = formData.dueDate && formData.dueTime
      ? `${formData.dueDate}T${formData.dueTime}:00`
      : undefined

    return {
      title: formData.title.trim(),
      description: formData.description.trim() || undefined,
      priority: formData.priority,
      dueDate: dueDateTime
    }
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

    if (taskData.dueDate) {
      const dueDate = new Date(taskData.dueDate)
      if (isNaN(dueDate.getTime())) {
        errors.dueDate = 'Data di scadenza non valida'
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

    if (formData.dueDate) {
      const dueDate = new Date(formData.dueDate)
      if (isNaN(dueDate.getTime())) {
        errors.dueDate = 'Data di scadenza non valida'
      }
    }

    if (formData.dueTime && !/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(formData.dueTime)) {
      errors.dueTime = 'Ora non valida (formato HH:MM)'
    }

    // Validate reminders
    formData.reminders.forEach((reminder, index) => {
      if (reminder.date && isNaN(new Date(reminder.date).getTime())) {
        errors[`reminder_${index}_date`] = 'Data del promemoria non valida'
      }

      if (reminder.time && !/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(reminder.time)) {
        errors[`reminder_${index}_time`] = 'Ora del promemoria non valida'
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

  const bulkUpdatePriority = async (taskIds: number[], priority: TaskPriority): Promise<boolean> => {
    isFormLoading.value = true

    try {
      await Promise.all(taskIds.map(id => updateTask(id, { priority })))
      showSuccess(`Priorità aggiornata per ${taskIds.length} attività`)
      return true
    } catch (error: any) {
      showError('Errore nell\'aggiornamento della priorità')
      return false
    } finally {
      isFormLoading.value = false
    }
  }

  // Utility functions
  const isDueSoon = (task: Task, hours = 24): boolean => {
    if (!task.dueDate || task.completed) return false
    
    const dueDate = new Date(task.dueDate)
    const now = new Date()
    const diffHours = (dueDate.getTime() - now.getTime()) / (1000 * 60 * 60)
    
    return diffHours > 0 && diffHours <= hours
  }

  const isOverdue = (task: Task): boolean => {
    if (!task.dueDate || task.completed) return false
    
    const dueDate = new Date(task.dueDate)
    const now = new Date()
    
    return dueDate < now
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
    filterByPriority,
    filterByCompletion,
    filterByDateRange,

    // Getters
    getTaskById,
    getTasksByDate,
    getTasksByPriority,
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
    bulkUpdatePriority,

    // Utilities
    isDueSoon,
    isOverdue,
    getTaskStatusColor,
    refreshTasks
  }
}