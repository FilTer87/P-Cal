export enum CalendarView {
  MONTH = 'month',
  WEEK = 'week',
  DAY = 'day',
  AGENDA = 'agenda'
}

export interface CalendarDate {
  date: Date
  dayOfMonth: number
  isCurrentMonth: boolean
  isToday: boolean
  isSelected: boolean
  isWeekend: boolean
  tasks: CalendarTask[]
}

export interface CalendarTask {
  id: number
  title: string
  description?: string
  priority: string
  dueDate: string
  hasReminders: boolean
}

export interface CalendarWeek {
  weekNumber: number
  days: CalendarDate[]
}

export interface CalendarMonth {
  year: number
  month: number
  monthName: string
  weeks: CalendarWeek[]
  totalDays: number
}

export interface CalendarState {
  currentDate: Date
  selectedDate: Date | null
  viewMode: CalendarView
  isLoading: boolean
  error: string | null
}

export interface CalendarNavigation {
  previousMonth: () => void
  nextMonth: () => void
  previousWeek: () => void
  nextWeek: () => void
  previousDay: () => void
  nextDay: () => void
  goToToday: () => void
  goToDate: (date: Date) => void
}

export interface CalendarViewConfig {
  label: string
  icon: string
  shortcut?: string
}

export const CALENDAR_VIEW_CONFIG: Record<CalendarView, CalendarViewConfig> = {
  [CalendarView.MONTH]: {
    label: 'Mese',
    icon: 'CalendarIcon',
    shortcut: 'M'
  },
  [CalendarView.WEEK]: {
    label: 'Settimana',
    icon: 'CalendarDaysIcon',
    shortcut: 'W'
  },
  [CalendarView.DAY]: {
    label: 'Giorno',
    icon: 'ClockIcon',
    shortcut: 'D'
  },
  [CalendarView.AGENDA]: {
    label: 'Agenda',
    icon: 'ListBulletIcon',
    shortcut: 'A'
  }
}

export interface DateRange {
  start: Date
  end: Date
}

// Italian locale configuration
export const ITALIAN_LOCALE = {
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
  weekdaysShort: ['Dom', 'Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab'],
  weekdaysMin: ['D', 'L', 'M', 'M', 'G', 'V', 'S'],
  today: 'Oggi',
  clear: 'Cancella',
  dateFormat: 'dd/mm/yyyy',
  timeFormat: 'HH:mm',
  firstDayOfWeek: 1 // Monday
}