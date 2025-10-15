import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { DEFAULT_SETTINGS } from '../utils/constants'
import { setLocale, type Locale, i18nGlobal } from '../i18n'

export type WeekStartDay = 0 | 1 // 0 = Sunday, 1 = Monday

export interface AppSettings {
  weekStartDay: WeekStartDay
  theme: 'light' | 'dark' | 'system'
  calendarView: 'month' | 'week' | 'day' | 'agenda'
  timeFormat: '12h' | '24h'
  locale: Locale
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
    locale: 'it-IT',
    notifications: DEFAULT_SETTINGS.notifications,
    reminderSound: DEFAULT_SETTINGS.reminderSound
  })

  // Computed properties
  const weekStartDay = computed(() => settings.value.weekStartDay)
  const timeFormat = computed(() => settings.value.timeFormat)
  
  const weekdaysShort = computed(() => {
    const t = i18nGlobal.t
    if (settings.value.weekStartDay === 0) {
      // Sunday first: Sun, Mon, Tue, Wed, Thu, Fri, Sat
      return [
        t('dateTime.weekdays.short.sun'),
        t('dateTime.weekdays.short.mon'),
        t('dateTime.weekdays.short.tue'),
        t('dateTime.weekdays.short.wed'),
        t('dateTime.weekdays.short.thu'),
        t('dateTime.weekdays.short.fri'),
        t('dateTime.weekdays.short.sat')
      ]
    } else {
      // Monday first: Mon, Tue, Wed, Thu, Fri, Sat, Sun
      return [
        t('dateTime.weekdays.short.mon'),
        t('dateTime.weekdays.short.tue'),
        t('dateTime.weekdays.short.wed'),
        t('dateTime.weekdays.short.thu'),
        t('dateTime.weekdays.short.fri'),
        t('dateTime.weekdays.short.sat'),
        t('dateTime.weekdays.short.sun')
      ]
    }
  })

  const weekdaysFull = computed(() => {
    const t = i18nGlobal.t
    if (settings.value.weekStartDay === 0) {
      // Sunday first
      return [
        t('dateTime.weekdays.full.sunday'),
        t('dateTime.weekdays.full.monday'),
        t('dateTime.weekdays.full.tuesday'),
        t('dateTime.weekdays.full.wednesday'),
        t('dateTime.weekdays.full.thursday'),
        t('dateTime.weekdays.full.friday'),
        t('dateTime.weekdays.full.saturday')
      ]
    } else {
      // Monday first
      return [
        t('dateTime.weekdays.full.monday'),
        t('dateTime.weekdays.full.tuesday'),
        t('dateTime.weekdays.full.wednesday'),
        t('dateTime.weekdays.full.thursday'),
        t('dateTime.weekdays.full.friday'),
        t('dateTime.weekdays.full.saturday'),
        t('dateTime.weekdays.full.sunday')
      ]
    }
  })

  // Time formatting utilities
  const formatTime = (date: Date | string): string => {
    const dateObj = typeof date === 'string' ? new Date(date) : date
    if (settings.value.timeFormat === '12h') {
      return dateObj.toLocaleTimeString('it-IT', { 
        hour: 'numeric', 
        minute: '2-digit',
        hour12: true 
      })
    } else {
      return dateObj.toLocaleTimeString('it-IT', { 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: false 
      })
    }
  }

  const formatHourLabel = (hour: number): string => {
    if (settings.value.timeFormat === '12h') {
      const period = hour < 12 ? 'AM' : 'PM'
      const displayHour = hour === 0 ? 12 : hour > 12 ? hour - 12 : hour
      return `${displayHour}:00 ${period}`
    } else {
      return `${String(hour).padStart(2, '0')}:00`
    }
  }

  const getTimeInputStep = (): string => {
    // Always use 24h format for HTML input (easier to parse)
    return '900' // 15 minutes
  }

  // Actions
  const loadSettings = () => {
    try {
      const stored = sessionStorage.getItem('app-settings')
      if (stored) {
        const parsedSettings = JSON.parse(stored) as Partial<AppSettings>

        // Validate and sanitize weekStartDay
        if (parsedSettings.weekStartDay !== undefined) {
          const day = Number(parsedSettings.weekStartDay)
          parsedSettings.weekStartDay = (day === 0 || day === 1) ? day as WeekStartDay : 1
        }

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

  const updateLocale = (locale: Locale) => {
    settings.value.locale = locale
    setLocale(locale)
    saveSettings()
    console.debug('🌐 Locale updated to:', locale)
  }

  const resetSettings = () => {
    settings.value = {
      weekStartDay: DEFAULT_SETTINGS.startOfWeek as WeekStartDay,
      theme: DEFAULT_SETTINGS.theme,
      calendarView: DEFAULT_SETTINGS.calendarView,
      timeFormat: DEFAULT_SETTINGS.timeFormat,
      locale: 'it-IT',
      notifications: DEFAULT_SETTINGS.notifications,
      reminderSound: DEFAULT_SETTINGS.reminderSound
    }
    saveSettings()
    console.debug('⚙️ Settings reset to defaults')
  }

  // Week start day options for UI
  const weekStartOptions = computed(() => {
    const t = i18nGlobal.t
    return [
      { value: 1, label: t('stores.settings.weekStart.monday'), description: t('stores.settings.weekStart.startsMonday') },
      { value: 0, label: t('stores.settings.weekStart.sunday'), description: t('stores.settings.weekStart.startsSunday') }
    ]
  })

  return {
    // State
    settings,

    // Computed
    weekStartDay,
    timeFormat,
    weekdaysShort,
    weekdaysFull,
    weekStartOptions,

    // Time formatting utilities
    formatTime,
    formatHourLabel,
    getTimeInputStep,

    // Actions
    loadSettings,
    saveSettings,
    updateWeekStartDay,
    updateTheme,
    updateCalendarView,
    updateTimeFormat,
    updateLocale,
    updateNotifications,
    updateReminderSound,
    resetSettings
  }
})