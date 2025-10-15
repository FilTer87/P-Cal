import type { CalendarView } from '../types/calendar'

/**
 * Calendar views configuration
 * Note: Labels are now i18n keys, use t('calendar.views.month') etc. in components
 */
export const CALENDAR_VIEWS: { value: CalendarView; labelKey: string; icon: string; shortcut: string }[] = [
  {
    value: 'month',
    labelKey: 'calendar.views.month',
    icon: 'CalendarIcon',
    shortcut: 'M'
  },
  {
    value: 'week',
    labelKey: 'calendar.views.week',
    icon: 'CalendarDaysIcon',
    shortcut: 'W'
  },
  {
    value: 'day',
    labelKey: 'calendar.views.day',
    icon: 'ClockIcon',
    shortcut: 'D'
  },
  {
    value: 'agenda',
    labelKey: 'calendar.views.agenda',
    icon: 'ListBulletIcon',
    shortcut: 'A'
  }
] as const

/**
 * Default settings
 */
export const DEFAULT_SETTINGS = {
  theme: 'system' as const,
  calendarView: 'month' as const,
  startOfWeek: 1, // Monday
  timeFormat: '24h' as const,
  notifications: true,
  emailNotifications: true,
  reminderSound: true,
  autoSave: true,
  pageSize: 20
} as const