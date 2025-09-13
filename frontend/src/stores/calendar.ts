import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { 
  startOfMonth, 
  endOfMonth, 
  startOfWeek, 
  endOfWeek, 
  startOfDay,
  endOfDay,
  addMonths, 
  addWeeks, 
  addDays, 
  isSameDay,
  isSameMonth,
  isToday,
  isWeekend,
  format
} from 'date-fns'
import { it } from 'date-fns/locale'
import type { 
  CalendarView, 
  CalendarState, 
  CalendarDate, 
  CalendarWeek, 
  CalendarMonth,
  DateRange
} from '../types/calendar'
import { useTasksStore } from './tasks'
import { useSettingsStore } from './settings'

export const useCalendarStore = defineStore('calendar', () => {
  // Get settings store for week start configuration
  const settingsStore = useSettingsStore()
  
  // State
  const currentDate = ref(new Date())
  const selectedDate = ref<Date | null>(null)
  const viewMode = ref<CalendarView>('month' as CalendarView)
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  
  // Configurable agenda days (can be modified in user settings)
  const agendaDays = ref(30)

  // Getters
  const currentYear = computed(() => currentDate.value.getFullYear())
  const currentMonth = computed(() => currentDate.value.getMonth())
  const currentMonthName = computed(() => 
    format(currentDate.value, 'MMMM yyyy', { locale: it })
  )

  const isCurrentMonth = computed(() => 
    isSameMonth(currentDate.value, new Date())
  )

  const isSelectedToday = computed(() => 
    selectedDate.value ? isSameDay(selectedDate.value, new Date()) : false
  )

  const viewDateRange = computed((): DateRange => {
    const start = startOfDay(currentDate.value)
    const end = endOfDay(currentDate.value)
    const weekStartsOn = settingsStore.weekStartDay

    switch (viewMode.value) {
      case 'day':
        return { start, end }
      case 'week':
        return {
          start: startOfWeek(currentDate.value, { weekStartsOn }),
          end: endOfWeek(currentDate.value, { weekStartsOn })
        }
      case 'month':
        return {
          start: startOfWeek(startOfMonth(currentDate.value), { weekStartsOn }),
          end: endOfWeek(endOfMonth(currentDate.value), { weekStartsOn })
        }
      case 'agenda':
        // Show tasks from today for the configured number of days
        return {
          start: startOfDay(new Date()),
          end: endOfDay(addDays(new Date(), agendaDays.value))
        }
      default:
        return { start, end }
    }
  })

  const calendarDays = computed((): CalendarDate[] => {
    const tasksStore = useTasksStore()
    const { start, end } = viewDateRange.value
    const days: CalendarDate[] = []
    
    let currentDay = new Date(start)
    
    while (currentDay <= end) {
      const dayTasks = tasksStore.getTasksByDate(format(currentDay, 'yyyy-MM-dd'))
      
      const dayData = {
        date: new Date(currentDay),
        dayOfMonth: currentDay.getDate(),
        isCurrentMonth: isSameMonth(currentDay, currentDate.value),
        isToday: isToday(currentDay),
        isSelected: selectedDate.value ? isSameDay(currentDay, selectedDate.value) : false,
        isWeekend: isWeekend(currentDay),
        tasks: dayTasks.map(task => {
          const taskDate = task.dueDate || task.startDatetime || task.startDateTime || task.endDatetime
          return {
            id: task.id,
            title: task.title,
            description: task.description,
            completed: task.completed,
            priority: task.priority,
            dueDate: taskDate || '',
            hasReminders: task.reminders?.length > 0 || false,
            isOverdue: taskDate ? new Date(taskDate) < new Date() && !task.completed : false
          }
        })
      }
      
      days.push(dayData)
      currentDay = addDays(currentDay, 1)
    }
    
    return days
  })

  const calendarWeeks = computed((): CalendarWeek[] => {
    const days = calendarDays.value
    const weeks: CalendarWeek[] = []
    
    for (let i = 0; i < days.length; i += 7) {
      weeks.push({
        weekNumber: Math.floor(i / 7) + 1,
        days: days.slice(i, i + 7)
      })
    }
    
    return weeks
  })

  const calendarMonth = computed((): CalendarMonth => {
    return {
      year: currentYear.value,
      month: currentMonth.value,
      monthName: currentMonthName.value,
      weeks: calendarWeeks.value,
      totalDays: calendarDays.value.length
    }
  })

  const todayTasks = computed(() => {
    const tasksStore = useTasksStore()
    return tasksStore.getTasksByDate(format(new Date(), 'yyyy-MM-dd'))
  })

  const selectedDateTasks = computed(() => {
    if (!selectedDate.value) return []
    const tasksStore = useTasksStore()
    return tasksStore.getTasksByDate(format(selectedDate.value, 'yyyy-MM-dd'))
  })

  // Navigation actions
  const goToToday = () => {
    currentDate.value = new Date()
    selectedDate.value = new Date()
  }

  const goToDate = (date: Date) => {
    currentDate.value = new Date(date)
    selectedDate.value = new Date(date)
  }

  const selectDate = (date: Date) => {
    selectedDate.value = new Date(date)
  }

  const clearSelection = () => {
    selectedDate.value = null
  }

  // View mode actions
  const setViewMode = (mode: CalendarView) => {
    viewMode.value = mode
    localStorage.setItem('calendarView', mode)
  }

  const initializeViewMode = () => {
    const savedView = localStorage.getItem('calendarView') as CalendarView
    if (savedView && ['month', 'week', 'day', 'agenda'].includes(savedView)) {
      viewMode.value = savedView
    }
  }

  // Navigation by view mode
  const navigatePrevious = () => {
    switch (viewMode.value) {
      case 'month':
        currentDate.value = addMonths(currentDate.value, -1)
        break
      case 'week':
        currentDate.value = addWeeks(currentDate.value, -1)
        break
      case 'day':
        currentDate.value = addDays(currentDate.value, -1)
        break
      case 'agenda':
        currentDate.value = addMonths(currentDate.value, -1)
        break
    }
  }

  const navigateNext = () => {
    switch (viewMode.value) {
      case 'month':
        currentDate.value = addMonths(currentDate.value, 1)
        break
      case 'week':
        currentDate.value = addWeeks(currentDate.value, 1)
        break
      case 'day':
        currentDate.value = addDays(currentDate.value, 1)
        break
      case 'agenda':
        currentDate.value = addMonths(currentDate.value, 1)
        break
    }
  }

  // Specific navigation methods
  const previousMonth = () => {
    currentDate.value = addMonths(currentDate.value, -1)
  }

  const nextMonth = () => {
    currentDate.value = addMonths(currentDate.value, 1)
  }

  const previousWeek = () => {
    currentDate.value = addWeeks(currentDate.value, -1)
  }

  const nextWeek = () => {
    currentDate.value = addWeeks(currentDate.value, 1)
  }

  const previousDay = () => {
    currentDate.value = addDays(currentDate.value, -1)
  }

  const nextDay = () => {
    currentDate.value = addDays(currentDate.value, 1)
  }

  // Utility methods
  const getCalendarState = (): CalendarState => ({
    currentDate: currentDate.value,
    selectedDate: selectedDate.value,
    viewMode: viewMode.value,
    isLoading: isLoading.value,
    error: error.value
  })

  const formatDisplayDate = (date: Date): string => {
    switch (viewMode.value) {
      case 'day':
        return format(date, 'EEEE, d MMMM yyyy', { locale: it })
      case 'week':
        const weekStart = startOfWeek(date, { weekStartsOn: 1 })
        const weekEnd = endOfWeek(date, { weekStartsOn: 1 })
        return `${format(weekStart, 'd MMM', { locale: it })} - ${format(weekEnd, 'd MMM yyyy', { locale: it })}`
      case 'month':
      case 'agenda':
        return format(date, 'MMMM yyyy', { locale: it })
      default:
        return format(date, 'd MMMM yyyy', { locale: it })
    }
  }

  const isDateInCurrentView = (date: Date): boolean => {
    const { start, end } = viewDateRange.value
    return date >= start && date <= end
  }

  return {
    // State
    currentDate,
    selectedDate,
    viewMode,
    isLoading,
    error,
    agendaDays,
    
    // Getters
    currentYear,
    currentMonth,
    currentMonthName,
    isCurrentMonth,
    isSelectedToday,
    viewDateRange,
    calendarDays,
    calendarWeeks,
    calendarMonth,
    todayTasks,
    selectedDateTasks,
    
    // Actions
    goToToday,
    goToDate,
    selectDate,
    clearSelection,
    setViewMode,
    initializeViewMode,
    navigatePrevious,
    navigateNext,
    previousMonth,
    nextMonth,
    previousWeek,
    nextWeek,
    previousDay,
    nextDay,
    getCalendarState,
    formatDisplayDate,
    isDateInCurrentView
  }
})