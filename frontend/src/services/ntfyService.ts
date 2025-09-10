/**
 * NTFY Service for sending push notifications
 * Provides integration with NTFY push notification service
 */

export interface NTFYNotification {
  server: string
  topic: string
  title: string
  message: string
  tags?: string[]
  priority?: 1 | 2 | 3 | 4 | 5
  click?: string
  actions?: NTFYAction[]
  attach?: string
  filename?: string
  delay?: string
  email?: string
  icon?: string
}

export interface NTFYAction {
  action: 'view' | 'http' | 'broadcast'
  label: string
  url?: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  headers?: Record<string, string>
  body?: string
  intent?: string
  extras?: Record<string, any>
}

export interface NTFYResponse {
  id: string
  time: number
  event: string
  topic: string
  message?: string
  title?: string
  tags?: string[]
  priority?: number
  click?: string
  actions?: NTFYAction[]
}

export interface NTFYServiceConfig {
  defaultServer?: string
  defaultPriority?: 1 | 2 | 3 | 4 | 5
  timeout?: number
  retries?: number
  retryDelay?: number
}

class NTFYService {
  private config: NTFYServiceConfig
  private abortController: AbortController | null = null

  constructor(config: NTFYServiceConfig = {}) {
    this.config = {
      defaultServer: 'https://ntfy.sh',
      defaultPriority: 3,
      timeout: 10000,
      retries: 3,
      retryDelay: 1000,
      ...config
    }
  }

  /**
   * Send a notification via NTFY
   */
  async sendNotification(notification: NTFYNotification): Promise<NTFYResponse> {
    const url = `${notification.server}/${notification.topic}`
    
    // Prepare headers
    const headers: Record<string, string> = {
      'Content-Type': 'text/plain',
      'X-Title': notification.title,
      'X-Message': notification.message
    }

    if (notification.tags && notification.tags.length > 0) {
      headers['X-Tags'] = notification.tags.join(',')
    }

    if (notification.priority) {
      headers['X-Priority'] = notification.priority.toString()
    }

    if (notification.click) {
      headers['X-Click'] = notification.click
    }

    if (notification.actions && notification.actions.length > 0) {
      headers['X-Actions'] = this.serializeActions(notification.actions)
    }

    if (notification.attach) {
      headers['X-Attach'] = notification.attach
    }

    if (notification.filename) {
      headers['X-Filename'] = notification.filename
    }

    if (notification.delay) {
      headers['X-Delay'] = notification.delay
    }

    if (notification.email) {
      headers['X-Email'] = notification.email
    }

    if (notification.icon) {
      headers['X-Icon'] = notification.icon
    }

    return this.sendWithRetry(url, {
      method: 'POST',
      headers,
      body: notification.message,
      signal: this.getAbortSignal()
    })
  }

  /**
   * Send a reminder notification with calendar-specific formatting
   */
  async sendReminderNotification(params: {
    server: string
    topic: string
    taskTitle: string
    taskId: number
    timeLeft: string
    dueDate: string
    baseUrl?: string
  }): Promise<NTFYResponse> {
    const { server, topic, taskTitle, taskId, timeLeft, dueDate, baseUrl = '' } = params
    
    const notification: NTFYNotification = {
      server,
      topic,
      title: `Promemoria: ${taskTitle}`,
      message: `Il tuo task "${taskTitle}" inizia tra ${timeLeft}`,
      tags: ['📅', '⏰', 'reminder'],
      priority: 4, // High priority for reminders
      click: `${baseUrl}/tasks/${taskId}`,
      actions: [
        {
          action: 'view',
          label: 'Visualizza Task',
          url: `${baseUrl}/tasks/${taskId}`
        }
      ],
      icon: `${baseUrl}/favicon.ico`
    }

    return this.sendNotification(notification)
  }

  /**
   * Test connection to NTFY server
   */
  async testConnection(server: string): Promise<boolean> {
    try {
      const response = await fetch(`${server}/v1/health`, {
        method: 'GET',
        signal: this.getAbortSignal()
      })
      return response.ok
    } catch (error) {
      console.error('NTFY connection test failed:', error)
      return false
    }
  }

  /**
   * Subscribe to a topic (for receiving notifications in browser)
   */
  async subscribeToTopic(
    server: string, 
    topic: string, 
    callback: (notification: NTFYResponse) => void
  ): Promise<EventSource> {
    const url = `${server}/${topic}/sse`
    const eventSource = new EventSource(url)

    eventSource.onmessage = (event) => {
      try {
        const notification: NTFYResponse = JSON.parse(event.data)
        if (notification.event === 'message') {
          callback(notification)
        }
      } catch (error) {
        console.error('Error parsing NTFY notification:', error)
      }
    }

    eventSource.onerror = (error) => {
      console.error('NTFY EventSource error:', error)
    }

    return eventSource
  }

  /**
   * Get topic statistics
   */
  async getTopicStats(server: string, topic: string): Promise<any> {
    try {
      const response = await fetch(`${server}/${topic}/stats`, {
        method: 'GET',
        signal: this.getAbortSignal()
      })
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
      
      return await response.json()
    } catch (error) {
      console.error('Failed to get topic stats:', error)
      throw error
    }
  }

  /**
   * Generate a secure topic name for a user
   */
  generateUserTopic(userId: number): string {
    const timestamp = Date.now()
    const random = Math.random().toString(36).substr(2, 9)
    return `privatecal-user-${userId}-${timestamp}-${random}`
  }

  /**
   * Validate NTFY server URL
   */
  validateServerUrl(url: string): boolean {
    try {
      const parsed = new URL(url)
      return ['http:', 'https:'].includes(parsed.protocol)
    } catch {
      return false
    }
  }

  /**
   * Cancel all pending requests
   */
  cancelRequests(): void {
    if (this.abortController) {
      this.abortController.abort()
      this.abortController = null
    }
  }

  /**
   * Update service configuration
   */
  updateConfig(newConfig: Partial<NTFYServiceConfig>): void {
    this.config = { ...this.config, ...newConfig }
  }

  // Private methods

  private async sendWithRetry(url: string, options: RequestInit): Promise<NTFYResponse> {
    let lastError: Error | null = null
    
    for (let attempt = 1; attempt <= (this.config.retries || 3); attempt++) {
      try {
        const response = await fetch(url, {
          ...options,
          signal: options.signal
        })

        if (!response.ok) {
          const errorText = await response.text()
          throw new Error(`HTTP ${response.status}: ${errorText || response.statusText}`)
        }

        // NTFY returns empty response for successful sends
        // We'll create a mock response for consistency
        return {
          id: `mock_${Date.now()}`,
          time: Date.now(),
          event: 'message',
          topic: this.extractTopicFromUrl(url)
        }
        
      } catch (error) {
        lastError = error as Error
        
        // Don't retry if request was aborted
        if (error instanceof Error && error.name === 'AbortError') {
          throw error
        }
        
        // Don't retry on client errors (4xx)
        if (error instanceof Error && error.message.includes('HTTP 4')) {
          throw error
        }
        
        // Wait before retry (except on last attempt)
        if (attempt < (this.config.retries || 3)) {
          await this.delay((this.config.retryDelay || 1000) * attempt)
        }
      }
    }

    throw lastError || new Error('All retry attempts failed')
  }

  private serializeActions(actions: NTFYAction[]): string {
    return actions.map(action => {
      const parts = [`action=${action.action}`, `label=${action.label}`]
      
      if (action.url) parts.push(`url=${action.url}`)
      if (action.method) parts.push(`method=${action.method}`)
      if (action.body) parts.push(`body=${action.body}`)
      if (action.intent) parts.push(`intent=${action.intent}`)
      
      if (action.headers) {
        Object.entries(action.headers).forEach(([key, value]) => {
          parts.push(`headers.${key}=${value}`)
        })
      }
      
      if (action.extras) {
        Object.entries(action.extras).forEach(([key, value]) => {
          parts.push(`extras.${key}=${value}`)
        })
      }
      
      return parts.join(', ')
    }).join('; ')
  }

  private extractTopicFromUrl(url: string): string {
    try {
      const parsed = new URL(url)
      return parsed.pathname.split('/')[1] || 'unknown'
    } catch {
      return 'unknown'
    }
  }

  private getAbortSignal(): AbortSignal {
    // Create new abort controller if none exists
    if (!this.abortController) {
      this.abortController = new AbortController()
      
      // Auto-timeout
      setTimeout(() => {
        this.abortController?.abort()
      }, this.config.timeout || 10000)
    }
    
    return this.abortController.signal
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms))
  }
}

// Singleton instance
export const ntfyService = new NTFYService()

// Export class for custom instances
export { NTFYService }

// Convenience functions
export const sendNotification = (notification: NTFYNotification) => 
  ntfyService.sendNotification(notification)

export const sendReminderNotification = (params: Parameters<typeof ntfyService.sendReminderNotification>[0]) =>
  ntfyService.sendReminderNotification(params)

export const testConnection = (server: string) =>
  ntfyService.testConnection(server)

export const generateUserTopic = (userId: number) =>
  ntfyService.generateUserTopic(userId)

export const validateServerUrl = (url: string) =>
  ntfyService.validateServerUrl(url)