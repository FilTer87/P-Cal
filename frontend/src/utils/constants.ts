import { CalendarView } from '../types/calendar'

/**
 * Calendar views configuration
 * Note: Labels are now i18n keys, use t('calendar.views.month') etc. in components
 */
export const CALENDAR_VIEWS: { value: CalendarView; labelKey: string; icon: string; shortcut: string }[] = [
  {
    value: CalendarView.MONTH,
    labelKey: 'calendar.views.month',
    icon: 'CalendarIcon',
    shortcut: 'M'
  },
  {
    value: CalendarView.WEEK,
    labelKey: 'calendar.views.week',
    icon: 'CalendarDaysIcon',
    shortcut: 'W'
  },
  {
    value: CalendarView.DAY,
    labelKey: 'calendar.views.day',
    icon: 'ClockIcon',
    shortcut: 'D'
  },
  {
    value: CalendarView.AGENDA,
    labelKey: 'calendar.views.agenda',
    icon: 'ListBulletIcon',
    shortcut: 'A'
  }
] as const

/**
 * @deprecated LOCALE_STRINGS is deprecated. Use i18n keys instead:
 * - For dates: use date-fns with locale from i18n
 * - For text: use t('dateTime.today'), t('dateTime.yesterday'), etc.
 * This will be removed in a future version.
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