import { computed, ref, watch } from 'vue'
import { useCalendarStore } from '../stores/calendar'
import { useTasksStore } from '../stores/tasks'
import { useSettingsStore } from '../stores/settings'
import { format, addDays, startOfWeek } from 'date-fns'
import type { CalendarView } from '../types/calendar'
import type { Task } from '../types/task'

export function useCalendar() {
  const calendarStore = useCalendarStore()
  const tasksStore = useTasksStore()
  const settingsStore = useSettingsStore()

  // State
  const showTaskModal = ref(false)
  const selectedTask = ref<Task | null>(null)
  const showCreateTaskModal = ref(false)
  const draggedTask = ref<Task | null>(null)

  // Computed properties
  const currentDate = computed(() => calendarStore.currentDate)
  const selectedDate = computed(() => calendarStore.selectedDate)
  const viewMode = computed(() => calendarStore.viewMode)
  const agendaDays = computed(() => calendarStore.agendaDays)
  const isLoading = computed(() => calendarStore.isLoading)
  const currentMonthName = computed(() => calendarStore.currentMonthName)
  const calendarDays = computed(() => calendarStore.calendarDays)
  const calendarWeeks = computed(() => calendarStore.calendarWeeks)
  const calendarMonth = computed(() => calendarStore.calendarMonth)
  const todayTasks = computed(() => calendarStore.todayTasks)
  const selectedDateTasks = computed(() => calendarStore.selectedDateTasks)
  const viewDateRange = computed(() => calendarStore.viewDateRange)

  // Watch for date changes to fetch tasks
  watch(
    () => calendarStore.viewDateRange,
    async (newRange) => {
      if (newRange) {
        const startDate = format(newRange.start, 'yyyy-MM-dd')
        const endDate = format(newRange.end, 'yyyy-MM-dd')
        await tasksStore.fetchTasksByDateRange(startDate, endDate)
      }
    },
    { immediate: true }
  )

  // Navigation actions
  const goToToday = () => {
    calendarStore.goToToday()
  }

  const goToDate = (date: Date) => {
    calendarStore.goToDate(date)
  }

  const selectDate = (date: Date) => {
    calendarStore.selectDate(date)
  }

  const clearSelection = () => {
    calendarStore.clearSelection()
  }

  const navigatePrevious = () => {
    calendarStore.navigatePrevious()
  }

  const navigateNext = () => {
    calendarStore.navigateNext()
  }


  // View mode management
  const setViewMode = (mode: CalendarView) => {
    calendarStore.setViewMode(mode)
  }

  const initializeViewMode = () => {
    calendarStore.initializeViewMode()
  }

  const isMonthView = computed(() => viewMode.value === 'month')
  const isWeekView = computed(() => viewMode.value === 'week')
  const isDayView = computed(() => viewMode.value === 'day')
  const isAgendaView = computed(() => viewMode.value === 'agenda')

  // Task management
  const getTasksForDate = (date: Date): Task[] => {
    const dateString = format(date, 'yyyy-MM-dd')
    return tasksStore.getTasksByDate(dateString)
  }

  const hasTasksOnDate = (date: Date): boolean => {
    const dateString = format(date, 'yyyy-MM-dd')
    return tasksStore.hasTasksOnDate(dateString)
  }

  // Task modal management
  const openTaskModal = (task: Task) => {
    selectedTask.value = task
    showTaskModal.value = true
  }

  const closeTaskModal = () => {
    selectedTask.value = null
    showTaskModal.value = false
  }

  const openCreateTaskModal = (date?: Date) => {
    if (date) {
      selectDate(date)
    }
    showCreateTaskModal.value = true
  }

  const closeCreateTaskModal = () => {
    showCreateTaskModal.value = false
  }

  // Drag and drop functionality
  const startDragTask = (task: Task) => {
    draggedTask.value = task
  }

  const endDragTask = () => {
    draggedTask.value = null
  }

  const dropTaskOnDate = async (date: Date) => {
    if (!draggedTask.value) return false

    // Calculate the new start and end datetimes by moving the task to the new date
    // while preserving the time and duration
    const task = draggedTask.value
    const oldStart = new Date(task.startDatetimeLocal)
    const oldEnd = new Date(task.endDatetimeLocal)

    // Calculate the time difference to maintain task duration
    const duration = oldEnd.getTime() - oldStart.getTime()

    // Create new start datetime with the target date but original time
    const newStart = new Date(date)
    newStart.setHours(oldStart.getHours(), oldStart.getMinutes(), oldStart.getSeconds())

    // Calculate new end datetime
    const newEnd = new Date(newStart.getTime() + duration)

    // Format as ISO 8601 local datetime string (no timezone offset)
    const formatLocalDateTime = (d: Date) => {
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      const seconds = String(d.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
    }

    const success = await tasksStore.updateTask(task.id, {
      startDatetimeLocal: formatLocalDateTime(newStart),
      endDatetimeLocal: formatLocalDateTime(newEnd),
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
    })

    if (success) {
      endDragTask()
      return true
    }

    return false
  }

  // Date utilities
  const formatDisplayDate = (date: Date): string => {
    return calendarStore.formatDisplayDate(date)
  }

  const isDateInCurrentView = (date: Date): boolean => {
    return calendarStore.isDateInCurrentView(date)
  }

  const isToday = (date: Date): boolean => {
    const today = new Date()
    return format(date, 'yyyy-MM-dd') === format(today, 'yyyy-MM-dd')
  }

  const isSameDate = (date1: Date, date2: Date): boolean => {
    return format(date1, 'yyyy-MM-dd') === format(date2, 'yyyy-MM-dd')
  }

  const isWeekend = (date: Date): boolean => {
    const day = date.getDay()
    return day === 0 || day === 6 // Sunday or Saturday
  }

  const isCurrentMonth = (date: Date): boolean => {
    return format(date, 'yyyy-MM') === format(currentDate.value, 'yyyy-MM')
  }

  // Week utilities
  const getWeekDays = (date: Date): Date[] => {
    const start = startOfWeek(date, { weekStartsOn: settingsStore.weekStartDay })
    const days: Date[] = []

    for (let i = 0; i < 7; i++) {
      days.push(addDays(start, i))
    }

    // Debug logging for week days generation
    console.debug('📅 Week days generated:', days.map(d => `${format(d, 'yyyy-MM-dd')} (${format(d, 'E')})`).join(', '))

    return days
  }

  // Keyboard shortcuts
  const handleKeyboardNavigation = (event: KeyboardEvent) => {
    // Ignore shortcuts when user is typing in input/textarea or contenteditable
    const target = event.target as HTMLElement
    const isTyping = target.tagName === 'INPUT' ||
                     target.tagName === 'TEXTAREA' ||
                     target.isContentEditable

    if (event.ctrlKey || event.metaKey) {
      switch (event.key) {
        case 't':
        case 'T':
          event.preventDefault()
          goToToday()
          break
        case 'ArrowLeft':
          event.preventDefault()
          navigatePrevious()
          break
        case 'ArrowRight':
          event.preventDefault()
          navigateNext()
          break
      }
    } else {
      // Skip single-key shortcuts when user is typing
      if (isTyping) {
        return
      }

      switch (event.key) {
        case 'm':
        case 'M':
          setViewMode('month')
          break
        case 'w':
        case 'W':
          setViewMode('week')
          break
        case 'd':
        case 'D':
          setViewMode('day')
          break
        case 'a':
        case 'A':
          setViewMode('agenda')
          break
        case 'Escape':
          clearSelection()
          closeTaskModal()
          closeCreateTaskModal()
          break
      }
    }
  }

  return {
    // State
    showTaskModal,
    selectedTask,
    showCreateTaskModal,
    draggedTask,

    // Calendar state
    currentDate,
    selectedDate,
    viewMode,
    agendaDays,
    isLoading,
    currentMonthName,
    calendarDays,
    calendarWeeks,
    calendarMonth,
    todayTasks,
    selectedDateTasks,
    viewDateRange,

    // View mode computed
    isMonthView,
    isWeekView,
    isDayView,
    isAgendaView,

    // Navigation
    goToToday,
    goToDate,
    selectDate,
    clearSelection,
    navigatePrevious,
    navigateNext,

    // View mode
    setViewMode,
    initializeViewMode,

    // Task management
    getTasksForDate,
    hasTasksOnDate,

    // Modal management
    openTaskModal,
    closeTaskModal,
    openCreateTaskModal,
    closeCreateTaskModal,

    // Drag and drop
    startDragTask,
    endDragTask,
    dropTaskOnDate,

    // Date utilities
    formatDisplayDate,
    isDateInCurrentView,
    isToday,
    isSameDate,
    isWeekend,
    isCurrentMonth,
    getWeekDays,

    // Keyboard shortcuts
    handleKeyboardNavigation
  }
}