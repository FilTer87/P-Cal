import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { format, isAfter, isBefore, isToday, startOfDay, endOfDay } from 'date-fns'
import type { 
  Task, 
  CreateTaskRequest, 
  UpdateTaskRequest, 
  TaskFilters, 
  TaskStats,
  DailyTasks
} from '../types/task'
import { taskApi } from '../services/taskApi'
import { useNotifications } from '../composables/useNotifications'

export const useTasksStore = defineStore('tasks', () => {
  // State
  const tasks = ref<Task[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const filters = ref<TaskFilters>({})
  const searchQuery = ref('')
  const isInitialized = ref(false)
  
  // Cached statistics from dedicated API endpoints
  const cachedOverdueTasks = ref<Task[]>([])
  const cachedTodayTasks = ref<Task[]>([])
  const cachedStats = ref<TaskStats | null>(null)

  // Getters
  const filteredTasks = computed(() => {
    let filtered = [...(tasks.value || [])].filter(task => task !== null && task !== undefined)

    // Apply completion filter
    if (filters.value.completed !== undefined) {
      filtered = filtered.filter(task => task.completed === filters.value.completed)
    }

    // Apply date range filter (using startDatetime)
    if (filters.value.startDateFrom) {
      const fromDate = new Date(filters.value.startDateFrom)
      filtered = filtered.filter(task => 
        task.startDatetime && new Date(task.startDatetime) >= fromDate
      )
    }

    if (filters.value.startDateTo) {
      const toDate = new Date(filters.value.startDateTo)
      filtered = filtered.filter(task => 
        task.startDatetime && new Date(task.startDatetime) <= toDate
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
      // Sort by completion status (incomplete first)
      if (a.completed !== b.completed) {
        return a.completed ? 1 : -1
      }
      
      // Sort by start date (soonest first)
      if (a.startDatetime && b.startDatetime) {
        return new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime()
      }
      
      if (a.startDatetime && !b.startDatetime) return -1
      if (!a.startDatetime && b.startDatetime) return 1
      
      // Sort by title alphabetically
      return a.title.localeCompare(b.title)
    })
  })

  const completedTasks = computed(() => 
    (tasks.value || []).filter(task => task && task.completed)
  )

  const pendingTasks = computed(() => 
    (tasks.value || []).filter(task => task && !task.completed)
  )

  // Note: This computed property only works on locally loaded tasks
  // For accurate overdue count, use fetchOverdueTasks() 
  const overdueTasks = computed(() => 
    (tasks.value || []).filter(task => {
      if (!task || task.completed) return false
      // Task is overdue if end time has passed
      return task.endDatetime && isBefore(new Date(task.endDatetime), new Date())
    })
  )

  const todayTasks = computed(() => 
    (cachedTodayTasks.value && cachedTodayTasks.value.length > 0)
      ? cachedTodayTasks.value 
      : (tasks.value || []).filter(task => {
          if (!task || !task.startDatetime || !task.endDatetime) return false
          // Task is today if it starts today or spans today
          const startDate = new Date(task.startDatetime)
          const endDate = new Date(task.endDatetime)
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
      if (!task || !task.startDatetime) return false
      const startDate = new Date(task.startDatetime)
      return isAfter(startDate, weekStart) && isBefore(startDate, weekEnd)
    })
  })

  const taskStats = computed((): TaskStats => {
    // Calculate local stats
    const localStats = {
      total: tasks.value?.length || 0,
      completed: completedTasks.value?.length || 0,
      pending: pendingTasks.value?.length || 0,
      overdue: (cachedOverdueTasks.value && Array.isArray(cachedOverdueTasks.value)) ? cachedOverdueTasks.value.length : (overdueTasks.value?.length || 0),
      today: (cachedTodayTasks.value && Array.isArray(cachedTodayTasks.value)) ? cachedTodayTasks.value.length : (todayTasks.value?.length || 0),
      thisWeek: thisWeekTasks.value?.length || 0
    }
    
    // Merge stats, prioritizing fresh cached data from server when available
    const result = {
      total: localStats.total, // Always use local count of loaded tasks
      completed: localStats.completed, // Always use local count of completed tasks  
      pending: localStats.pending, // Always use local count of pending tasks
      overdue: localStats.overdue, // Use cached from dedicated API, fallback to local
      today: localStats.today, // Use cached from dedicated API, fallback to local
      thisWeek: localStats.thisWeek // Always use local calculation
    }
    
    console.log('📊 taskStats computed:', result, {
      cachedStats: cachedStats.value,
      localStats,
      cachedOverdue: cachedOverdueTasks.value?.length,
      cachedToday: cachedTodayTasks.value?.length,
      localTasks: tasks.value?.length
    })
    
    return result
  })

  const tasksByDate = computed((): DailyTasks => {
    const dailyTasks: DailyTasks = {};
    
    (tasks.value || []).forEach(task => {
      // Use startDatetime for task date grouping
      if (task && task.startDatetime) {
        const dateKey = format(new Date(task.startDatetime), 'yyyy-MM-dd')
        if (!dailyTasks[dateKey]) {
          dailyTasks[dateKey] = []
        }
        dailyTasks[dateKey].push(task)
        console.log(`📅 Added task "${task.title}" to date ${dateKey}`)
      } else {
        console.log(`📅 Task is null or has no startDatetime:`, task)
      }
    })
    
    return dailyTasks
  })

  // Urgent tasks removed as priority no longer exists in Task model
  const urgentTasks = computed(() => [])

  // High priority tasks removed as priority no longer exists in Task model
  const highPriorityTasks = computed(() => [])

  // Actions
  const { showSuccess, showError } = useNotifications()

  const fetchTasks = async (force = false) => {
    if (!force && isInitialized.value) return

    isLoading.value = true
    error.value = null

    try {
      const response = await taskApi.getTasks()
      tasks.value = response
      isInitialized.value = true
    } catch (err: any) {
      error.value = err.message || 'Errore nel caricamento delle attività'
      showError('Errore nel caricamento delle attività')
    } finally {
      isLoading.value = false
    }
  }

  const fetchTasksByDateRange = async (startDate: string, endDate: string) => {
    isLoading.value = true
    error.value = null

    console.log(`📅 Fetching tasks for date range: ${startDate} to ${endDate}`)

    try {
      const response = await taskApi.getTasksByDateRange(startDate, endDate)
      console.log(`📅 Raw API response:`, response)
      console.log(`📅 Response type:`, typeof response)
      console.log(`📅 Is Array:`, Array.isArray(response))
      
      // Ensure response is an array
      const tasksArray = Array.isArray(response) ? response : []
      console.log(`📅 Found ${tasksArray.length} tasks in range:`, tasksArray.map(t => `${t.title} (${t.startDatetime || t.startDateTime})`))
      
      // Update tasks with fetched data (merge with existing)
      const existingTaskIds = new Set((tasks.value || []).map(task => task.id))
      const newTasks = tasksArray.filter(task => !existingTaskIds.has(task.id))
      
      // Ensure tasks.value is always an array
      if (!Array.isArray(tasks.value)) {
        tasks.value = []
      }
      
      tasks.value = [...tasks.value, ...newTasks]
      
      console.log(`📅 Added ${newTasks.length} new tasks to store. Total tasks: ${tasks.value.length}`)
    } catch (err: any) {
      console.error('📅 Error fetching tasks by date range:', err)
      error.value = err.message || 'Errore nel caricamento delle attività'
      showError('Errore nel caricamento delle attività per il periodo selezionato')
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
      if (newTask && newTask.id && newTask.startDatetime && newTask.endDatetime) {
        tasks.value.unshift(newTask)
        console.log('✅ Task created successfully:', newTask)
      } else {
        console.error('❌ Invalid task data received from API:', newTask)
        throw new Error('Dati task non validi ricevuti dal server')
      }
      
      showSuccess('Attività creata con successo!')
      return newTask
    } catch (err: any) {
      error.value = err.message || 'Errore nella creazione dell\'attività'
      showError('Errore nella creazione dell\'attività')
      return null
    } finally {
      isLoading.value = false
    }
  }

  const updateTask = async (taskId: number, taskData: UpdateTaskRequest): Promise<Task | null> => {
    isLoading.value = true
    error.value = null

    try {
      const updatedTask = await taskApi.updateTask(taskId, taskData)
      
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
      
      showSuccess('Attività aggiornata con successo!')
      return updatedTask
    } catch (err: any) {
      error.value = err.message || 'Errore nell\'aggiornamento dell\'attività'
      showError('Errore nell\'aggiornamento dell\'attività')
      return null
    } finally {
      isLoading.value = false
    }
  }

  const deleteTask = async (taskId: number): Promise<boolean> => {
    isLoading.value = true
    error.value = null

    try {
      await taskApi.deleteTask(taskId)
      tasks.value = (tasks.value || []).filter(task => task.id !== taskId)
      showSuccess('Attività eliminata con successo!')
      return true
    } catch (err: any) {
      error.value = err.message || 'Errore nell\'eliminazione dell\'attività'
      showError('Errore nell\'eliminazione dell\'attività')
      return false
    } finally {
      isLoading.value = false
    }
  }

  const toggleTaskCompletion = async (taskId: number): Promise<boolean> => {
    const task = (tasks.value || []).find(t => t.id === taskId)
    if (!task) return false

    return await updateTask(taskId, { completed: !task.completed }) !== null
  }

  const markTaskCompleted = async (taskId: number): Promise<boolean> => {
    return await updateTask(taskId, { completed: true }) !== null
  }

  const markTaskPending = async (taskId: number): Promise<boolean> => {
    return await updateTask(taskId, { completed: false }) !== null
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
      showError('Errore nella ricerca delle attività')
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

  const getTaskById = (taskId: number): Task | undefined => {
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
      task && task.startDatetime && 
      !task.completed &&
      isAfter(new Date(task.startDatetime), new Date()) &&
      isBefore(new Date(task.startDatetime), futureDate)
    ).sort((a, b) => 
      new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime()
    )
  }

  const fetchOverdueTasks = async () => {
    try {
      console.log('📊 Fetching overdue tasks...')
      const overdue = await taskApi.getOverdueTasks()
      console.log('📊 Overdue tasks response:', overdue)
      const overdueArray = Array.isArray(overdue) ? overdue : []
      cachedOverdueTasks.value = overdueArray
      console.log(`📊 Cached ${overdueArray.length} overdue tasks`)
      return overdueArray
    } catch (err: any) {
      console.error('📊 Failed to fetch overdue tasks:', err)
      return []
    }
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

  const fetchTaskStats = async () => {
    try {
      console.log('📊 Fetching task stats...')
      const stats = await taskApi.getTaskStats()
      console.log('📊 Task stats response:', stats)
      cachedStats.value = stats
      return stats
    } catch (err: any) {
      console.error('📊 Failed to fetch task stats:', err)
      return null
    }
  }

  const refreshTasks = () => fetchTasks(true)
  
  const refreshStatistics = async () => {
    console.log('📊 Starting refresh statistics...')
    await Promise.all([
      fetchOverdueTasks(),
      fetchTodayTasks(),
      fetchTaskStats()
    ])
    console.log('📊 Statistics refresh completed. Final cached values:', {
      overdue: cachedOverdueTasks.value?.length || 0,
      today: cachedTodayTasks.value?.length || 0,
      stats: cachedStats.value
    })
  }

  return {
    // State
    tasks,
    isLoading,
    error,
    filters,
    searchQuery,
    isInitialized,
    cachedOverdueTasks,
    cachedTodayTasks,
    cachedStats,
    
    // Getters
    filteredTasks,
    completedTasks,
    pendingTasks,
    overdueTasks,
    todayTasks,
    thisWeekTasks,
    taskStats,
    tasksByDate,
    urgentTasks,
    highPriorityTasks,
    
    // Actions
    fetchTasks,
    fetchTasksByDateRange,
    fetchOverdueTasks,
    fetchTodayTasks,
    fetchTaskStats,
    createTask,
    updateTask,
    deleteTask,
    toggleTaskCompletion,
    markTaskCompleted,
    markTaskPending,
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