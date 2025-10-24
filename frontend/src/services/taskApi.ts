import { apiClient } from './api'
import type { 
  Task, 
  CreateTaskRequest, 
  UpdateTaskRequest, 
  TaskFilters,
  TaskStats,
} from '../types/task'
import type { PaginatedResponse, PaginationParams } from '../types/api'
import { API_ENDPOINTS } from '../types/api'

export class TaskApi {
  /**
   * Get all tasks for the authenticated user
   */
  async getTasks(params?: PaginationParams & TaskFilters): Promise<Task[]> {
    return apiClient.get<Task[]>(API_ENDPOINTS.TASKS.BASE, { params })
  }

  /**
   * Get tasks with pagination
   * // TODO - Unused
   */
  async getTasksPaginated(params?: PaginationParams & TaskFilters): Promise<PaginatedResponse<Task>> {
    return apiClient.get<PaginatedResponse<Task>>(`${API_ENDPOINTS.TASKS.BASE}/paginated`, { params })
  }


  /**
   * Create a new task
   */
  async createTask(taskData: CreateTaskRequest): Promise<Task> {
    return apiClient.post<Task>(API_ENDPOINTS.TASKS.BASE, taskData)
  }

  /**
   * Update an existing task
   * @param occurrenceStart Optional ISO 8601 datetime string for editing single occurrence of recurring task
   */
  async updateTask(id: string, taskData: UpdateTaskRequest, occurrenceStart?: string): Promise<Task> {
    const url = occurrenceStart
      ? `${API_ENDPOINTS.TASKS.BY_ID(id)}?occurrenceStart=${encodeURIComponent(occurrenceStart)}`
      : API_ENDPOINTS.TASKS.BY_ID(id)
    return apiClient.put<Task>(url, taskData)
  }

  /**
   * Get a single task by ID
   */
  async getTaskById(id: string): Promise<Task> {
    return apiClient.get<Task>(API_ENDPOINTS.TASKS.BY_ID(id))
  }

  /**
   * Delete a task
   */
  async deleteTask(id: string): Promise<void> {
    return apiClient.delete<void>(API_ENDPOINTS.TASKS.BY_ID(id))
  }

  /**
   * Get tasks by date range
   */
  async getTasksByDateRange(startDate: string, endDate: string): Promise<Task[]> {
    return apiClient.get<Task[]>(API_ENDPOINTS.TASKS.BY_DATE_RANGE, {
      params: { startDate, endDate }
    })
  }


  /**
   * Search tasks by title and description
   */
  async searchTasks(query: string, filters?: TaskFilters): Promise<Task[]> {
    return apiClient.get<Task[]>(API_ENDPOINTS.TASKS.SEARCH, {
      params: { query, ...filters }
    })
  }

  /**
   * Get task statistics
   */
  async getTaskStats(): Promise<TaskStats> {
    return apiClient.get<TaskStats>(API_ENDPOINTS.TASKS.STATS)
  }

  /**
   * Get today's tasks
   */
  async getTodayTasks(): Promise<Task[]> {
    return apiClient.get<Task[]>(`${API_ENDPOINTS.TASKS.BASE}/today`)
  }










  /**
   * Export tasks to CSV
   * // TODO - Unused
   */
  async exportTasks(filters?: TaskFilters): Promise<Blob> {
    const response = await apiClient.getRaw(`${API_ENDPOINTS.TASKS.BASE}/export`, {
      params: filters,
      responseType: 'blob'
    })
    return response.data
  }

  /**
   * Import tasks from CSV
   * // TODO - Unused
   */
  async importTasks(file: File): Promise<{
    imported: number
    skipped: number
    errors: string[]
  }> {
    const formData = new FormData()
    formData.append('file', file)

    return apiClient.post(`${API_ENDPOINTS.TASKS.BASE}/import`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }





}

export const taskApi = new TaskApi()