import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { DEFAULT_SETTINGS, LOCALE_STRINGS } from '../utils/constants'

export type WeekStartDay = 0 | 1 // 0 = Sunday, 1 = Monday

export interface AppSettings {
  weekStartDay: WeekStartDay
  theme: 'light' | 'dark' | 'system'
  calendarView: 'month' | 'week' | 'day' | 'agenda'
  timeFormat: '12h' | '24h'
  notifications: boolean
  reminderSound: boolean
}

export const useSettingsStore = defineStore('settings', () => {
  // Settings state
  const settings = ref<AppSettings>({
    weekStartDay: DEFAULT_SETTINGS.startOfWeek as WeekStartDay,
    theme: DEFAULT_SETTINGS.theme,
    calendarView: DEFAULT_SETTINGS.calendarView,
    timeFormat: DEFAULT_SETTINGS.timeFormat,
    notifications: DEFAULT_SETTINGS.notifications,
    reminderSound: DEFAULT_SETTINGS.reminderSound
  })

  // Computed properties
  const weekStartDay = computed(() => settings.value.weekStartDay)
  
  const weekdaysShort = computed(() => {
    if (settings.value.weekStartDay === 0) {
      // Sunday first: Dom, Lun, Mar, Mer, Gio, Ven, Sab
      return ['Dom', 'Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab']
    } else {
      // Monday first: Lun, Mar, Mer, Gio, Ven, Sab, Dom  
      return ['Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab', 'Dom']
    }
  })

  const weekdaysFull = computed(() => {
    if (settings.value.weekStartDay === 0) {
      // Sunday first
      return ['Domenica', 'Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato']
    } else {
      // Monday first
      return ['Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato', 'Domenica']
    }
  })

  // Actions
  const loadSettings = () => {
    try {
      const stored = sessionStorage.getItem('app-settings')
      if (stored) {
        const parsedSettings = JSON.parse(stored) as Partial<AppSettings>
        
        // Merge with defaults to ensure all properties exist
        settings.value = {
          ...settings.value,
          ...parsedSettings
        }
        
        console.debug('⚙️ Settings loaded from sessionStorage:', settings.value)
      } else {
        console.debug('⚙️ No stored settings found, using defaults:', settings.value)
      }
    } catch (error) {
      console.error('❌ Failed to load settings from sessionStorage:', error)
      // Keep defaults if loading fails
    }
  }

  const saveSettings = () => {
    try {
      sessionStorage.setItem('app-settings', JSON.stringify(settings.value))
      console.debug('⚙️ Settings saved to sessionStorage:', settings.value)
    } catch (error) {
      console.error('❌ Failed to save settings to sessionStorage:', error)
    }
  }

  const updateWeekStartDay = (day: WeekStartDay) => {
    settings.value.weekStartDay = day
    saveSettings()
    console.debug('📅 Week start day updated to:', day === 0 ? 'Sunday' : 'Monday')
  }

  const updateTheme = (theme: AppSettings['theme']) => {
    settings.value.theme = theme
    saveSettings()
  }

  const updateCalendarView = (view: AppSettings['calendarView']) => {
    settings.value.calendarView = view
    saveSettings()
  }

  const updateTimeFormat = (format: AppSettings['timeFormat']) => {
    settings.value.timeFormat = format
    saveSettings()
  }

  const updateNotifications = (enabled: boolean) => {
    settings.value.notifications = enabled
    saveSettings()
  }

  const updateReminderSound = (enabled: boolean) => {
    settings.value.reminderSound = enabled
    saveSettings()
  }

  const resetSettings = () => {
    settings.value = {
      weekStartDay: DEFAULT_SETTINGS.startOfWeek as WeekStartDay,
      theme: DEFAULT_SETTINGS.theme,
      calendarView: DEFAULT_SETTINGS.calendarView,
      timeFormat: DEFAULT_SETTINGS.timeFormat,
      notifications: DEFAULT_SETTINGS.notifications,
      reminderSound: DEFAULT_SETTINGS.reminderSound
    }
    saveSettings()
    console.debug('⚙️ Settings reset to defaults')
  }

  // Week start day options for UI
  const weekStartOptions = [
    { value: 1, label: 'Lunedì', description: 'La settimana inizia di lunedì' },
    { value: 0, label: 'Domenica', description: 'La settimana inizia di domenica' }
  ] as const

  return {
    // State
    settings,
    
    // Computed
    weekStartDay,
    weekdaysShort,
    weekdaysFull,
    weekStartOptions,
    
    // Actions
    loadSettings,
    saveSettings,
    updateWeekStartDay,
    updateTheme,
    updateCalendarView,
    updateTimeFormat,
    updateNotifications,
    updateReminderSound,
    resetSettings
  }
})