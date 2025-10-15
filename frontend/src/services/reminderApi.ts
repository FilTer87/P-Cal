import { apiClient } from './api'
import type {
  Reminder,
  CreateReminderRequest,
  UpdateReminderRequest
} from '../types/task'
import type { PaginationParams } from '../types/api'
import { API_ENDPOINTS } from '../types/api'

export class ReminderApi {
  /**
   * Get all reminders for the authenticated user
   */
  async getAllReminders(params?: PaginationParams): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(API_ENDPOINTS.REMINDERS.BASE, { params })
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
   * Get pending reminders
   */
  async getPendingReminders(params?: PaginationParams): Promise<Reminder[]> {
    return apiClient.get<Reminder[]>(`${API_ENDPOINTS.REMINDERS.BASE}/upcoming`, { params })
  }
}

export const reminderApi = new ReminderApi()