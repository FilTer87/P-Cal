import { CalendarView } from '../types/calendar'

/**
 * Calendar views
 */
export const CALENDAR_VIEWS: { value: CalendarView; label: string; icon: string; shortcut: string }[] = [
  {
    value: CalendarView.MONTH,
    label: 'Mese',
    icon: 'CalendarIcon',
    shortcut: 'M'
  },
  {
    value: CalendarView.WEEK,
    label: 'Settimana',
    icon: 'CalendarDaysIcon',
    shortcut: 'W'
  },
  {
    value: CalendarView.DAY,
    label: 'Giorno',
    icon: 'ClockIcon',
    shortcut: 'D'
  },
  {
    value: CalendarView.AGENDA,
    label: 'Agenda',
    icon: 'ListBulletIcon',
    shortcut: 'A'
  }
] as const

/**
 * Italian locale strings
 * Note: weekdaysShort and weekdaysMin are now generated dynamically based on user settings
 */
export const LOCALE_STRINGS = {
  months: [
    'Gennaio', 'Febbraio', 'Marzo', 'Aprile', 'Maggio', 'Giugno',
    'Luglio', 'Agosto', 'Settembre', 'Ottobre', 'Novembre', 'Dicembre'
  ],
  monthsShort: [
    'Gen', 'Feb', 'Mar', 'Apr', 'Mag', 'Giu',
    'Lug', 'Ago', 'Set', 'Ott', 'Nov', 'Dic'
  ],
  weekdays: [
    'Domenica', 'Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato'
  ],
  today: 'Oggi',
  yesterday: 'Ieri',
  tomorrow: 'Domani',
  thisWeek: 'Questa settimana',
  lastWeek: 'Settimana scorsa',
  nextWeek: 'Settimana prossima',
  thisMonth: 'Questo mese',
  lastMonth: 'Mese scorso',
  nextMonth: 'Mese prossimo',
  thisYear: 'Quest\'anno',
  lastYear: 'Anno scorso',
  nextYear: 'Anno prossimo'
} as const

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