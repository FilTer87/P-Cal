import apiClient from './api'

export interface Calendar {
  id: number
  name: string
  slug: string
  color: string
  timezone: string
  isDefault: boolean
  taskCount: number
  caldavPath: string
}

export interface CalendarRequest {
  name: string
  slug: string
  color?: string
  timezone?: string
  isDefault?: boolean
}

class CalendarsApiService {
  private baseUrl = '/calendars'

  /**
   * Get all calendars for current user
   */
  async getAllCalendars(): Promise<Calendar[]> {
    const response = await apiClient.get<Calendar[]>(this.baseUrl)
    return response
  }

  /**
   * Get calendar by ID
   */
  async getCalendarById(id: number): Promise<Calendar> {
    const response = await apiClient.get<Calendar>(`${this.baseUrl}/${id}`)
    return response
  }

  /**
   * Create new calendar
   */
  async createCalendar(request: CalendarRequest): Promise<Calendar> {
    const response = await apiClient.post<Calendar>(this.baseUrl, request)
    return response
  }

  /**
   * Update calendar
   */
  async updateCalendar(id: number, request: CalendarRequest): Promise<Calendar> {
    const response = await apiClient.put<Calendar>(`${this.baseUrl}/${id}`, request)
    return response
  }

  /**
   * Delete calendar
   */
  async deleteCalendar(id: number): Promise<void> {
    await apiClient.delete(`${this.baseUrl}/${id}`)
  }
}

export const calendarsApi = new CalendarsApiService()
