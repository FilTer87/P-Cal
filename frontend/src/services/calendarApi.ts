import { apiClient } from './api'

/**
 * Duplicate handling strategy
 */
export enum DuplicateStrategy {
  SKIP = 'SKIP',
  UPDATE = 'UPDATE',
  CREATE_ANYWAY = 'CREATE_ANYWAY'
}

/**
 * Information about a duplicate event
 */
export interface DuplicateEventInfo {
  uid: string
  title: string
  existingDate: string
  newDate: string
  contentChanged: boolean
}

/**
 * Preview response for import
 */
export interface ImportPreviewResponse {
  totalEvents: number
  newEvents: number
  duplicateEvents: number
  errorEvents: number
  duplicates: DuplicateEventInfo[]
}

/**
 * Import result from backend
 */
export interface CalendarImportResult {
  success: boolean
  totalParsed: number
  successCount: number
  failedCount: number
  createdCount?: number
  updatedCount?: number
  strategy?: string
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
   * Preview calendar import - analyze for duplicates without importing
   * @param file - The .ics file to analyze
   */
  async previewImport(file: File): Promise<ImportPreviewResponse> {
    try {
      const formData = new FormData()
      formData.append('file', file)

      const response = await apiClient.postRaw<ImportPreviewResponse>(
        `${this.baseUrl}/import/preview`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      )

      return response.data
    } catch (error) {
      console.error('Failed to preview import:', error)
      throw error
    }
  }

  /**
   * Confirm calendar import with duplicate handling strategy
   * @param file - The .ics file to import
   * @param strategy - How to handle duplicates (SKIP, UPDATE, CREATE_ANYWAY)
   */
  async confirmImport(file: File, strategy: DuplicateStrategy): Promise<CalendarImportResult> {
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('duplicateStrategy', strategy)

      const response = await apiClient.postRaw<CalendarImportResult>(
        `${this.baseUrl}/import/confirm`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      )

      return response.data
    } catch (error) {
      console.error('Failed to confirm import:', error)
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
