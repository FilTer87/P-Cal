// CalendarView as string literal union (used throughout the app)
export type CalendarView = 'month' | 'week' | 'day' | 'agenda'

import type { Task } from './task'

export interface CalendarDate {
  date: Date
  dayOfMonth: number
  isCurrentMonth: boolean
  isToday: boolean
  isSelected: boolean
  isWeekend: boolean
  tasks: Task[]
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

export interface DateRange {
  start: Date
  end: Date
}
