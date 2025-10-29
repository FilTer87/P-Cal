import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { format, isAfter, isBefore, isToday, startOfDay, endOfDay } from 'date-fns'
import type {
  Task,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskFilters,
  DailyTasks
} from '../types/task'
import { taskApi } from '../services/taskApi'
import { useCustomToast } from '../composables/useCustomToast'
import { i18nGlobal } from '../i18n'
import { getTaskKey } from '../utils/recurrence'

export const useTasksStore = defineStore('tasks', () => {
  // State
  const tasks = ref<Task[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const filters = ref<TaskFilters>({})
  const searchQuery = ref('')
  const isInitialized = ref(false)
  
  // Cached statistics from dedicated API endpoints
  const cachedTodayTasks = ref<Task[]>([])

  // Getters
  const filteredTasks = computed(() => {
    let filtered = [...(tasks.value || [])].filter(task => task !== null && task !== undefined)

    // Apply date range filter (using startDatetimeLocal)
    if (filters.value.startDateFrom) {
      const fromDate = new Date(filters.value.startDateFrom)
      filtered = filtered.filter(task =>
        task.startDatetimeLocal && new Date(task.startDatetimeLocal) >= fromDate
      )
    }

    if (filters.value.startDateTo) {
      const toDate = new Date(filters.value.startDateTo)
      filtered = filtered.filter(task =>
        task.startDatetimeLocal && new Date(task.startDatetimeLocal) <= toDate
      )
    }

    // Apply search filter
    if (searchQuery.value.trim()) {
      const query = searchQuery.value.toLowerCase()
      filtered = filtered.filter(task =>
        task.title.toLowerCase().includes(query) ||
        (task.description && task.description.toLowerCase().includes(query))
      )
    }

    return filtered.sort((a, b) => {
      // Sort by start date (soonest first)
      if (a.startDatetimeLocal && b.startDatetimeLocal) {
        return new Date(a.startDatetimeLocal).getTime() - new Date(b.startDatetimeLocal).getTime()
      }

      if (a.startDatetimeLocal && !b.startDatetimeLocal) return -1
      if (!a.startDatetimeLocal && b.startDatetimeLocal) return 1

      // Sort by title alphabetically
      return a.title.localeCompare(b.title)
    })
  })


  const todayTasks = computed(() =>
    (cachedTodayTasks.value && cachedTodayTasks.value.length > 0)
      ? cachedTodayTasks.value
      : (tasks.value || []).filter(task => {
          if (!task || !task.startDatetimeLocal || !task.endDatetimeLocal) return false
          // Task is today if it starts today or spans today
          const startDate = new Date(task.startDatetimeLocal)
          const endDate = new Date(task.endDatetimeLocal)
          const today = new Date()

          return isToday(startDate) ||
            (startDate <= today && endDate >= startOfDay(today))
        })
  )

  const thisWeekTasks = computed(() => {
    const now = new Date()
    const weekStart = startOfDay(now)
    const weekEnd = new Date(now)
    weekEnd.setDate(weekEnd.getDate() + 7)

    return (tasks.value || []).filter(task => {
      if (!task || !task.startDatetimeLocal) return false
      const startDate = new Date(task.startDatetimeLocal)
      return isAfter(startDate, weekStart) && isBefore(startDate, weekEnd)
    })
  })


  const tasksByDate = computed((): DailyTasks => {
    const dailyTasks: DailyTasks = {};

    (tasks.value || []).forEach(task => {
      // Use startDatetimeLocal for task date grouping (already in local time)
      if (task && task.startDatetimeLocal) {
        // Parse local datetime string (e.g., "2025-10-20T15:00:00")
        const taskDate = new Date(task.startDatetimeLocal)

        // Debug logging
        console.debug(`🕐 Task ${task.id} (${task.title}): ${task.startDatetimeLocal} -> Local: ${taskDate.toLocaleString()}, Date key: ${format(taskDate, 'yyyy-MM-dd')}`)

        const dateKey = format(taskDate, 'yyyy-MM-dd')
        if (!dailyTasks[dateKey]) {
          dailyTasks[dateKey] = []
        }
        dailyTasks[dateKey].push(task)
      }
    })

    return dailyTasks
  })

  // Urgent tasks removed as priority no longer exists in Task model
  const urgentTasks = computed(() => [])

  // High priority tasks removed as priority no longer exists in Task model
  const highPriorityTasks = computed(() => [])

  // Actions
  const { showSuccess, showError } = useCustomToast()

  const fetchTasks = async (force = false) => {
    if (!force && isInitialized.value) return

    isLoading.value = true
    error.value = null

    try {
      const response = await taskApi.getTasks()
      tasks.value = response
      isInitialized.value = true
    } catch (err: any) {
      error.value = err.message || i18nGlobal.t('stores.tasks.loadError')
    } finally {
      isLoading.value = false
    }
  }

  const fetchTasksByDateRange = async (startDate: string, endDate: string) => {
    isLoading.value = true
    error.value = null

    try {
      const response = await taskApi.getTasksByDateRange(startDate, endDate)

      // Ensure response is an array
      const tasksArray = Array.isArray(response) ? response : []

      // Update tasks with fetched data (merge with existing)
      // Use occurrenceId for recurring tasks to allow multiple occurrences of the same task
      const existingTaskKeys = new Set<string | number>()

      // Ensure tasks.value is always an array
      if (!Array.isArray(tasks.value)) {
        tasks.value = []
      }

      // Track existing task keys
      const currentTasks = tasks.value || []
      currentTasks.forEach(task => {
        existingTaskKeys.add(getTaskKey(task))
      })

      // Filter new tasks to avoid duplicates
      const newTasks = tasksArray.filter(task => {
        const taskKey = getTaskKey(task)
        return !existingTaskKeys.has(taskKey)
      })

      // If we're adding expanded tasks (with occurrenceId), remove non-expanded versions
      const expandedTaskIds = new Set<string>()
      tasksArray.forEach(task => {
        if (task.occurrenceId) {
          expandedTaskIds.add(task.id)
        }
      })

      // Remove non-expanded versions if we have expanded versions coming in
      if (expandedTaskIds.size > 0) {
        tasks.value = tasks.value.filter(task => {
          // Keep task if it's not being replaced by expanded version
          // (either different id, or has occurrenceId itself)
          return !expandedTaskIds.has(task.id) || task.occurrenceId
        })
      }

      tasks.value = [...tasks.value, ...newTasks]

      console.debug(`📅 Added ${newTasks.length} new tasks to store. Total tasks: ${tasks.value.length}`)
    } catch (err: any) {
      console.error('📅 Error fetching tasks by date range:', err)
      error.value = err.message || 'Errore nel caricamento delle attività'
    } finally {
      isLoading.value = false
    }
  }

  const fetchTaskById = async (taskId: string): Promise<Task | null> => {
    isLoading.value = true
    error.value = null

    try {
      const task = await taskApi.getTaskById(taskId)

      if (task && task.id) {
        // Add to store if not already present
        const exists = tasks.value.some(t => t.id === taskId)
        if (!exists) {
          tasks.value.push(task)
        }
        return task
      }
      return null
    } catch (err: any) {
      console.error('📅 Error fetching task by ID:', err)
      error.value = err.message || 'Errore nel caricamento dell\'attività'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const createTask = async (taskData: CreateTaskRequest): Promise<Task | null> => {
    isLoading.value = true
    error.value = null

    try {
      const newTask = await taskApi.createTask(taskData)
      
      // Ensure we have a valid task before adding to array
      if (newTask && newTask.id && newTask.startDatetimeLocal && newTask.endDatetimeLocal) {
        tasks.value.unshift(newTask)
        console.log('✅ Task created successfully:', newTask)
      } else {
        console.error('❌ Invalid task data received from API:', newTask)
        throw new Error('Dati task non validi ricevuti dal server')
      }
      
      return newTask
    } catch (err: any) {
      error.value = err.message || 'Errore nella creazione dell\'attività'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const updateTask = async (taskId: string, taskData: UpdateTaskRequest, occurrenceStart?: string): Promise<Task | null> => {
    isLoading.value = true
    error.value = null

    try {
      const updatedTask = await taskApi.updateTask(taskId, taskData, occurrenceStart)

      // Ensure we have a valid task before updating
      if (updatedTask && updatedTask.id) {
        const index = (tasks.value || []).findIndex(task => task && task.id === taskId)
        if (index !== -1) {
          tasks.value[index] = updatedTask
        }
        console.log('✅ Task updated successfully:', updatedTask)
      } else {
        console.error('❌ Invalid task data received from API:', updatedTask)
        throw new Error('Dati task non validi ricevuti dal server')
      }

      return updatedTask
    } catch (err: any) {
      error.value = err.message || 'Errore nell\'aggiornamento dell\'attività'
      return null
    } finally {
      isLoading.value = false
    }
  }

  const deleteTask = async (taskId: string): Promise<boolean> => {
    isLoading.value = true
    error.value = null

    try {
      await taskApi.deleteTask(taskId)
      tasks.value = (tasks.value || []).filter(task => task.id !== taskId)
      return true
    } catch (err: any) {
      error.value = err.message || 'Errore nell\'eliminazione dell\'attività'
      return false
    } finally {
      isLoading.value = false
    }
  }


  const searchTasks = async (query: string) => {
    if (!query.trim()) {
      searchQuery.value = ''
      return
    }

    isLoading.value = true
    searchQuery.value = query

    try {
      const results = await taskApi.searchTasks(query)
      // For search results, we might want to display them separately
      // or merge with existing tasks - depending on UI requirements
      tasks.value = results
    } catch (err: any) {
      error.value = err.message || 'Errore nella ricerca'
    } finally {
      isLoading.value = false
    }
  }

  const clearSearch = () => {
    searchQuery.value = ''
    fetchTasks()
  }

  const setFilters = (newFilters: TaskFilters) => {
    filters.value = { ...filters.value, ...newFilters }
  }

  const clearFilters = () => {
    filters.value = {}
    searchQuery.value = ''
  }

  const getTaskById = (taskId: string): Task | undefined => {
    return (tasks.value || []).find(task => task.id === taskId)
  }

  const getTasksByDate = (date: string): Task[] => {
    return tasksByDate.value[date] || []
  }

  // getTasksByPriority removed as priority no longer exists in Task model
  const getTasksByPriority = (): Task[] => {
    return []
  }

  const hasTasksOnDate = (date: string): boolean => {
    return (tasksByDate.value[date]?.length || 0) > 0
  }

  const getUpcomingTasks = (days = 7): Task[] => {
    const futureDate = new Date()
    futureDate.setDate(futureDate.getDate() + days)

    return (tasks.value || []).filter(task =>
      task && task.startDatetimeLocal &&
      isAfter(new Date(task.startDatetimeLocal), new Date()) &&
      isBefore(new Date(task.startDatetimeLocal), futureDate)
    ).sort((a, b) =>
      new Date(a.startDatetimeLocal).getTime() - new Date(b.startDatetimeLocal).getTime()
    )
  }


  const fetchTodayTasks = async () => {
    try {
      console.log('📊 Fetching today tasks...')
      const today = await taskApi.getTodayTasks()
      console.log('📊 Today tasks response:', today)
      const todayArray = Array.isArray(today) ? today : []
      cachedTodayTasks.value = todayArray
      console.log(`📊 Cached ${todayArray.length} today tasks`)
      return todayArray
    } catch (err: any) {
      console.error('📊 Failed to fetch today tasks:', err)
      return []
    }
  }

  const refreshTasks = () => fetchTasks(true)

  const refreshStatistics = async () => {
    await fetchTodayTasks()
  }

  const resetStore = () => {
    tasks.value = []
    cachedTodayTasks.value = []
    isInitialized.value = false
    clearFilters()
  }

  // Listen for user changes (logout/login)
  if (typeof window !== 'undefined') {
    window.addEventListener('auth-user-changed', resetStore)
  }

  return {
    // State
    tasks,
    isLoading,
    error,
    filters,
    searchQuery,
    isInitialized,
    cachedTodayTasks,

    // Getters
    filteredTasks,
    todayTasks,
    thisWeekTasks,
    tasksByDate,
    urgentTasks,
    highPriorityTasks,

    // Actions
    fetchTasks,
    fetchTasksByDateRange,
    fetchTaskById,
    fetchTodayTasks,
    createTask,
    updateTask,
    deleteTask,
    searchTasks,
    clearSearch,
    setFilters,
    clearFilters,
    getTaskById,
    getTasksByDate,
    getTasksByPriority,
    hasTasksOnDate,
    getUpcomingTasks,
    refreshTasks,
    refreshStatistics
  }
})