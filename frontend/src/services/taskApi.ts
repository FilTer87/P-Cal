import { apiClient } from './api'
import type { 
  Task, 
  CreateTaskRequest, 
  UpdateTaskRequest, 
  TaskFilters,
  TaskStats,
  TaskPriority
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
   */
  async getTasksPaginated(params?: PaginationParams & TaskFilters): Promise<PaginatedResponse<Task>> {
    return apiClient.get<PaginatedResponse<Task>>(`${API_ENDPOINTS.TASKS.BASE}/paginated`, { params })
  }

  /**
   * Get a specific task by ID
   */
  async getTask(id: number): Promise<Task> {
    return apiClient.get<Task>(API_ENDPOINTS.TASKS.BY_ID(id))
  }

  /**
   * Create a new task
   */
  async createTask(taskData: CreateTaskRequest): Promise<Task> {
    return apiClient.post<Task>(API_ENDPOINTS.TASKS.BASE, taskData)
  }

  /**
   * Update an existing task
   */
  async updateTask(id: number, taskData: UpdateTaskRequest): Promise<Task> {
    return apiClient.put<Task>(API_ENDPOINTS.TASKS.BY_ID(id), taskData)
  }

  /**
   * Delete a task
   */
  async deleteTask(id: number): Promise<void> {
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
   * Get tasks for a specific date
   */
  async getTasksByDate(date: string): Promise<Task[]> {
    return apiClient.get<Task[]>(`${API_ENDPOINTS.TASKS.BASE}/by-date/${date}`)
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
   * Get this week's tasks
   */
  async getThisWeekTasks(): Promise<Task[]> {
    return apiClient.get<Task[]>(`${API_ENDPOINTS.TASKS.BASE}/this-week`)
  }

  /**
   * Get upcoming tasks
   */
  async getUpcomingTasks(days = 7): Promise<Task[]> {
    return apiClient.get<Task[]>(`${API_ENDPOINTS.TASKS.BASE}/upcoming`, {
      params: { days }
    })
  }

  /**
   * Get tasks by priority
   */
  async getTasksByPriority(priority: TaskPriority): Promise<Task[]> {
    return apiClient.get<Task[]>(`${API_ENDPOINTS.TASKS.BASE}/by-priority/${priority}`)
  }

  /**
   * Bulk update tasks
   */
  async bulkUpdateTasks(taskIds: number[], updates: UpdateTaskRequest): Promise<Task[]> {
    return apiClient.patch<Task[]>(`${API_ENDPOINTS.TASKS.BASE}/bulk`, {
      taskIds,
      updates
    })
  }

  /**
   * Bulk delete tasks
   */
  async bulkDeleteTasks(taskIds: number[]): Promise<void> {
    return apiClient.delete<void>(`${API_ENDPOINTS.TASKS.BASE}/bulk`, {
      data: { taskIds }
    })
  }

  /**
   * Duplicate a task
   */
  async duplicateTask(id: number): Promise<Task> {
    return apiClient.post<Task>(`${API_ENDPOINTS.TASKS.BY_ID(id)}/duplicate`)
  }

  /**
   * Archive a task
   */
  async archiveTask(id: number): Promise<Task> {
    return apiClient.patch<Task>(`${API_ENDPOINTS.TASKS.BY_ID(id)}/archive`)
  }

  /**
   * Unarchive a task
   */
  async unarchiveTask(id: number): Promise<Task> {
    return apiClient.patch<Task>(`${API_ENDPOINTS.TASKS.BY_ID(id)}/unarchive`)
  }

  /**
   * Get archived tasks
   */
  async getArchivedTasks(params?: PaginationParams): Promise<Task[]> {
    return apiClient.get<Task[]>(`${API_ENDPOINTS.TASKS.BASE}/archived`, { params })
  }

  /**
   * Export tasks to CSV
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

  /**
   * Get task activity/history
   */
  async getTaskActivity(id: number): Promise<Array<{
    id: number
    action: string
    field?: string
    oldValue?: any
    newValue?: any
    timestamp: string
  }>> {
    return apiClient.get(`${API_ENDPOINTS.TASKS.BY_ID(id)}/activity`)
  }

  /**
   * Add comment to task
   */
  async addTaskComment(id: number, comment: string): Promise<{
    id: number
    comment: string
    createdAt: string
  }> {
    return apiClient.post(`${API_ENDPOINTS.TASKS.BY_ID(id)}/comments`, {
      comment
    })
  }

  /**
   * Get task comments
   */
  async getTaskComments(id: number): Promise<Array<{
    id: number
    comment: string
    createdAt: string
  }>> {
    return apiClient.get(`${API_ENDPOINTS.TASKS.BY_ID(id)}/comments`)
  }

  /**
   * Update task comment
   */
  async updateTaskComment(taskId: number, commentId: number, comment: string): Promise<void> {
    return apiClient.put<void>(`${API_ENDPOINTS.TASKS.BY_ID(taskId)}/comments/${commentId}`, {
      comment
    })
  }

  /**
   * Delete task comment
   */
  async deleteTaskComment(taskId: number, commentId: number): Promise<void> {
    return apiClient.delete<void>(`${API_ENDPOINTS.TASKS.BY_ID(taskId)}/comments/${commentId}`)
  }
}

export const taskApi = new TaskApi()