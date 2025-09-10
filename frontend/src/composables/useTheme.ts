import { computed } from 'vue'
import { useThemeStore, type ThemeMode } from '../stores/theme'

export function useTheme() {
  const themeStore = useThemeStore()

  // Computed properties
  const isDarkMode = computed(() => themeStore.isDarkMode)
  const themeMode = computed(() => themeStore.themeMode)
  const themeName = computed(() => themeStore.themeName)
  const themeIcon = computed(() => themeStore.themeIcon)
  const systemPrefersDark = computed(() => themeStore.systemPrefersDark)

  // Actions
  const initializeTheme = () => {
    themeStore.initializeTheme()
  }

  const setThemeMode = (mode: ThemeMode) => {
    themeStore.setThemeMode(mode)
  }

  const toggleTheme = () => {
    themeStore.toggleTheme()
  }

  const applyTheme = () => {
    themeStore.applyTheme()
  }

  // Utility functions
  const getThemeClass = (lightClass: string, darkClass: string) => {
    return themeStore.getThemeClass(lightClass, darkClass)
  }

  const getThemeValue = <T>(lightValue: T, darkValue: T): T => {
    return themeStore.getThemeValue(lightValue, darkValue)
  }

  // Theme-aware CSS classes
  const textClass = computed(() => 
    getThemeClass('text-gray-900', 'text-gray-100')
  )

  const bgClass = computed(() => 
    getThemeClass('bg-white', 'bg-gray-900')
  )

  const cardClass = computed(() => 
    getThemeClass('bg-white border-gray-200', 'bg-gray-800 border-gray-700')
  )

  const inputClass = computed(() => 
    getThemeClass(
      'bg-white border-gray-300 text-gray-900 placeholder-gray-400',
      'bg-gray-800 border-gray-600 text-gray-100 placeholder-gray-500'
    )
  )

  const buttonPrimaryClass = computed(() => 
    getThemeClass(
      'bg-blue-600 hover:bg-blue-700 text-white',
      'bg-blue-500 hover:bg-blue-600 text-white'
    )
  )

  const buttonSecondaryClass = computed(() => 
    getThemeClass(
      'bg-gray-100 hover:bg-gray-200 text-gray-900',
      'bg-gray-700 hover:bg-gray-600 text-gray-100'
    )
  )

  const sidebarClass = computed(() => 
    getThemeClass('bg-white border-gray-200', 'bg-gray-800 border-gray-700')
  )

  const modalClass = computed(() => 
    getThemeClass('bg-white', 'bg-gray-800')
  )

  // Calendar-specific theme classes
  const calendarDayClass = computed(() => 
    getThemeClass('bg-white hover:bg-gray-50', 'bg-gray-800 hover:bg-gray-700')
  )

  const calendarHeaderClass = computed(() => 
    getThemeClass('bg-gray-50 text-gray-700', 'bg-gray-700 text-gray-300')
  )

  const calendarTodayClass = computed(() => 
    getThemeClass('bg-blue-50 border-blue-200', 'bg-blue-900/20 border-blue-400')
  )

  const calendarSelectedClass = computed(() => 
    getThemeClass('bg-blue-100 border-blue-300', 'bg-blue-800/30 border-blue-500')
  )

  // Task-specific theme classes
  const taskCompletedClass = computed(() => 
    getThemeClass('text-green-600 bg-green-50', 'text-green-400 bg-green-900/20')
  )

  const taskOverdueClass = computed(() => 
    getThemeClass('text-red-600 bg-red-50', 'text-red-400 bg-red-900/20')
  )

  const taskPendingClass = computed(() => 
    getThemeClass('text-blue-600 bg-blue-50', 'text-blue-400 bg-blue-900/20')
  )

  // Animation and transition utilities
  const getTransitionClasses = () => {
    return 'transition-colors duration-200 ease-in-out'
  }

  const getThemeTransitionStyle = () => {
    return {
      transition: 'background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease'
    }
  }

  // Color utilities for dynamic styling
  const getTaskPriorityColors = (priority: string) => {
    const colors = {
      LOW: getThemeValue(
        { text: 'text-green-600', bg: 'bg-green-50', border: 'border-green-500' },
        { text: 'text-green-400', bg: 'bg-green-900/20', border: 'border-green-400' }
      ),
      MEDIUM: getThemeValue(
        { text: 'text-yellow-600', bg: 'bg-yellow-50', border: 'border-yellow-500' },
        { text: 'text-yellow-400', bg: 'bg-yellow-900/20', border: 'border-yellow-400' }
      ),
      HIGH: getThemeValue(
        { text: 'text-orange-600', bg: 'bg-orange-50', border: 'border-orange-500' },
        { text: 'text-orange-400', bg: 'bg-orange-900/20', border: 'border-orange-400' }
      ),
      URGENT: getThemeValue(
        { text: 'text-red-600', bg: 'bg-red-50', border: 'border-red-500' },
        { text: 'text-red-400', bg: 'bg-red-900/20', border: 'border-red-400' }
      )
    }

    return colors[priority as keyof typeof colors] || colors.MEDIUM
  }

  const getStatusColors = (status: 'completed' | 'pending' | 'overdue') => {
    const colors = {
      completed: getThemeValue(
        { text: 'text-green-600', bg: 'bg-green-100', ring: 'ring-green-500' },
        { text: 'text-green-400', bg: 'bg-green-900/20', ring: 'ring-green-400' }
      ),
      pending: getThemeValue(
        { text: 'text-blue-600', bg: 'bg-blue-100', ring: 'ring-blue-500' },
        { text: 'text-blue-400', bg: 'bg-blue-900/20', ring: 'ring-blue-400' }
      ),
      overdue: getThemeValue(
        { text: 'text-red-600', bg: 'bg-red-100', ring: 'ring-red-500' },
        { text: 'text-red-400', bg: 'bg-red-900/20', ring: 'ring-red-400' }
      )
    }

    return colors[status]
  }

  // Theme-aware icon colors
  const getIconColor = (variant: 'primary' | 'secondary' | 'success' | 'warning' | 'error' = 'primary') => {
    const colors = {
      primary: getThemeValue('text-blue-600', 'text-blue-400'),
      secondary: getThemeValue('text-gray-600', 'text-gray-400'),
      success: getThemeValue('text-green-600', 'text-green-400'),
      warning: getThemeValue('text-yellow-600', 'text-yellow-400'),
      error: getThemeValue('text-red-600', 'text-red-400')
    }

    return colors[variant]
  }

  return {
    // State
    isDarkMode,
    themeMode,
    themeName,
    themeIcon,
    systemPrefersDark,

    // Actions
    initializeTheme,
    setThemeMode,
    toggleTheme,
    applyTheme,

    // Utilities
    getThemeClass,
    getThemeValue,

    // Pre-computed classes
    textClass,
    bgClass,
    cardClass,
    inputClass,
    buttonPrimaryClass,
    buttonSecondaryClass,
    sidebarClass,
    modalClass,
    calendarDayClass,
    calendarHeaderClass,
    calendarTodayClass,
    calendarSelectedClass,
    taskCompletedClass,
    taskOverdueClass,
    taskPendingClass,

    // Dynamic utilities
    getTransitionClasses,
    getThemeTransitionStyle,
    getTaskPriorityColors,
    getStatusColors,
    getIconColor
  }
}