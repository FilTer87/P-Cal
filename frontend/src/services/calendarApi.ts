import { apiClient } from './api'

/**
 * Import result from backend
 */
export interface CalendarImportResult {
  success: boolean
  totalParsed: number
  successCount: number
  failedCount: number
  errors?: string[]
  warnings?: string[]
  importedTasks?: any[]
  error?: string  // For general error messages
}

/**
 * Calendar statistics
 */
export interface CalendarStats {
  totalTasks: number
  calendarName: string
  exportAvailable: boolean
  importAvailable: boolean
}

/**
 * Calendar API service for CalDAV import/export operations
 */
class CalendarApiService {
  private readonly baseUrl = '/calendar'

  /**
   * Export calendar to .ics file
   * Downloads the file automatically
   */
  async exportCalendar(): Promise<{ blob: Blob; filename: string }> {
    try {
      const response = await apiClient.getRaw<Blob>(`${this.baseUrl}/export`, {
        responseType: 'blob'
      })

      // Extract filename from Content-Disposition header
      const contentDisposition = response.headers['content-disposition']
      let filename = 'calendar.ics'

      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?(.+)"?/)
        if (filenameMatch && filenameMatch[1]) {
          filename = filenameMatch[1]
        }
      }

      return {
        blob: response.data,
        filename
      }
    } catch (error) {
      console.error('Failed to export calendar:', error)
      throw error
    }
  }

  /**
   * Import calendar from .ics file
   * @param file - The .ics file to import
   */
  async importCalendar(file: File): Promise<CalendarImportResult> {
    try {
      const formData = new FormData()
      formData.append('file', file)

      const response = await apiClient.postRaw<CalendarImportResult>(
        `${this.baseUrl}/import`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      )

      return response.data
    } catch (error) {
      console.error('Failed to import calendar:', error)
      throw error
    }
  }

  /**
   * Get calendar statistics
   */
  async getCalendarStats(): Promise<CalendarStats> {
    try {
      return await apiClient.get<CalendarStats>(`${this.baseUrl}/stats`)
    } catch (error) {
      console.error('Failed to get calendar stats:', error)
      throw error
    }
  }
}

// Export singleton instance
export const calendarApi = new CalendarApiService()
export default calendarApi
