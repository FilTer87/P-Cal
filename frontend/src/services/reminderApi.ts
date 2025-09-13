import { apiClient } from './api'
import type { 
  Reminder, 
  CreateReminderRequest, 
  UpdateReminderRequest 
} from '../types/task'
import type { PaginatedResponse, PaginationParams } from '../types/api'
import { API_ENDPOINTS } from '../types/api'

export class ReminderApi {
  /**
   * Get all reminders for a specific task
   */
  async getTaskReminders(taskId: number): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(API_ENDPOINTS.REMINDERS.BY_TASK(taskId))
  }

  /**
   * Get all reminders for the authenticated user
   */
  async getAllReminders(params?: PaginationParams): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(API_ENDPOINTS.REMINDERS.BASE, { params })
  }

  /**
   * Get reminders with pagination
   */
  async getRemindersPaginated(params?: PaginationParams): Promise<PaginatedResponse<Reminder>> {
    return apiClient.get<PaginatedResponse<Reminder>>(`${API_ENDPOINTS.REMINDERS.BASE}/paginated`, { params })
  }

  /**
   * Get a specific reminder by ID
   */
  async getReminder(id: number): Promise<Reminder> {
    return apiClient.get<Reminder>(API_ENDPOINTS.REMINDERS.BY_ID(id))
  }

  /**
   * Create a new reminder for a task
   */
  async createReminder(taskId: number, reminderData: CreateReminderRequest): Promise<Reminder> {
    return apiClient.post<Reminder>(API_ENDPOINTS.REMINDERS.BY_TASK(taskId), reminderData)
  }

  /**
   * Update an existing reminder
   */
  async updateReminder(id: number, reminderData: UpdateReminderRequest): Promise<Reminder> {
    return apiClient.put<Reminder>(API_ENDPOINTS.REMINDERS.BY_ID(id), reminderData)
  }

  /**
   * Delete a reminder
   */
  async deleteReminder(id: number): Promise<void> {
    return apiClient.delete<void>(API_ENDPOINTS.REMINDERS.BY_ID(id))
  }

  /**
   * Mark reminder as sent
   */
  async markReminderSent(id: number): Promise<Reminder> {
    return apiClient.patch<Reminder>(`${API_ENDPOINTS.REMINDERS.BY_ID(id)}/mark-sent`)
  }

  /**
   * Get upcoming reminders
   */
  async getUpcomingReminders(hours = 24): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(API_ENDPOINTS.REMINDERS.UPCOMING, {
      params: { hours }
    })
  }

  /**
   * Get overdue reminders (reminders that should have been sent)
   */
  async getOverdueReminders(): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/overdue`)
  }

  /**
   * Get sent reminders
   */
  async getSentReminders(params?: PaginationParams): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/sent`, { params })
  }

  /**
   * Get pending reminders
   */
  async getPendingReminders(params?: PaginationParams): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/upcoming`, { params })
  }

  /**
   * Get reminders by date range
   */
  async getRemindersByDateRange(startDate: string, endDate: string): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/date-range`, {
      params: { startDate, endDate }
    })
  }

  /**
   * Get reminders for a specific date
   */
  async getRemindersByDate(date: string): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/by-date/${date}`)
  }

  /**
   * Get today's reminders
   */
  async getTodayReminders(): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/today`)
  }

  /**
   * Get this week's reminders
   */
  async getThisWeekReminders(): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/this-week`)
  }

  /**
   * Bulk create reminders for a task
   */
  async bulkCreateReminders(taskId: number, reminders: CreateReminderRequest[]): Promise<Reminder[]> {
    return apiClient.post<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BY_TASK(taskId)}/bulk`, {
      reminders
    })
  }

  /**
   * Bulk update reminders
   */
  async bulkUpdateReminders(reminderIds: number[], updates: UpdateReminderRequest): Promise<Reminder[]> {
    return apiClient.patch<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/bulk`, {
      reminderIds,
      updates
    })
  }

  /**
   * Bulk delete reminders
   */
  async bulkDeleteReminders(reminderIds: number[]): Promise<void> {
    return apiClient.delete<void>(`${API_ENDPOINTS.REMINDERS.BASE}/bulk`, {
      data: { reminderIds }
    })
  }

  /**
   * Bulk mark reminders as sent
   */
  async bulkMarkRemindersSent(reminderIds: number[]): Promise<Reminder[]> {
    return apiClient.patch<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/bulk/mark-sent`, {
      reminderIds
    })
  }

  /**
   * Snooze a reminder (postpone it by specified minutes)
   */
  async snoozeReminder(id: number, minutes: number): Promise<Reminder> {
    return apiClient.patch<Reminder>(`${API_ENDPOINTS.REMINDERS.BY_ID(id)}/snooze`, {
      minutes
    })
  }

  /**
   * Test reminder notification (send immediately for testing)
   */
  async testReminder(id: number): Promise<void> {
    return apiClient.post<void>(`${API_ENDPOINTS.REMINDERS.BY_ID(id)}/test`)
  }

  /**
   * Get reminder statistics
   */
  async getReminderStats(): Promise<{
    total: number
    sent: number
    pending: number
    overdue: number
    today: number
    thisWeek: number
  }> {
    return apiClient.get(`${API_ENDPOINTS.REMINDERS.BASE}/stats`)
  }

  /**
   * Get reminder templates/presets
   */
  async getReminderPresets(): Promise<Array<{
    id: string
    name: string
    description: string
    offsetMinutes: number
  }>> {
    return apiClient.get(`${API_ENDPOINTS.REMINDERS.BASE}/presets`)
  }

  /**
   * Create reminder from preset
   */
  async createReminderFromPreset(taskId: number, presetId: string, dueDate: string): Promise<Reminder> {
    return apiClient.post<Reminder>(`${API_ENDPOINTS.REMINDERS.BY_TASK(taskId)}/from-preset`, {
      presetId,
      dueDate
    })
  }

  /**
   * Update reminder preferences
   */
  async updateReminderPreferences(preferences: {
    emailNotifications?: boolean
    pushNotifications?: boolean
    defaultReminderOffsets?: number[]
    reminderSound?: string
  }): Promise<void> {
    return apiClient.put<void>(`${API_ENDPOINTS.REMINDERS.BASE}/preferences`, preferences)
  }

  /**
   * Get reminder preferences
   */
  async getReminderPreferences(): Promise<{
    emailNotifications: boolean
    pushNotifications: boolean
    defaultReminderOffsets: number[]
    reminderSound: string
  }> {
    return apiClient.get(`${API_ENDPOINTS.REMINDERS.BASE}/preferences`)
  }

  /**
   * Get reminder history for a task
   */
  async getReminderHistory(taskId: number): Promise<Array<{
    id: number
    action: string
    reminderDateTime: string
    sentAt?: string
    createdAt: string
  }>> {
    return apiClient.get(`${API_ENDPOINTS.REMINDERS.BY_TASK(taskId)}/history`)
  }

  /**
   * Duplicate reminders from one task to another
   */
  async duplicateReminders(sourceTaskId: number, targetTaskId: number): Promise<Reminder[]> {
    return apiClient.post<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/duplicate`, {
      sourceTaskId,
      targetTaskId
    })
  }

  /**
   * Export reminders to calendar format (iCal)
   */
  async exportRemindersToCalendar(taskIds?: number[]): Promise<Blob> {
    const response = await apiClient.getRaw(`${API_ENDPOINTS.REMINDERS.BASE}/export/calendar`, {
      params: { taskIds: taskIds?.join(',') },
      responseType: 'blob'
    })
    return response.data
  }
}

export const reminderApi = new ReminderApi()