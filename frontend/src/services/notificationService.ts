/**
 * Browser Notification Service
 * Handles browser notifications with fallbacks and service worker integration
 */

export interface NotificationOptions {
  body?: string
  icon?: string
  image?: string
  badge?: string
  tag?: string
  data?: any
  requireInteraction?: boolean
  silent?: boolean
  timestamp?: number
  actions?: NotificationAction[]
  dir?: 'auto' | 'ltr' | 'rtl'
  lang?: string
  renotify?: boolean
  vibrate?: number[]
}

export interface NotificationPayload {
  title: string
  options?: NotificationOptions
}

export interface ServiceWorkerMessage {
  type: 'SHOW_NOTIFICATION' | 'NOTIFICATION_CLICKED' | 'NOTIFICATION_CLOSED'
  payload?: any
}

class BrowserNotificationService {
  private isSupported: boolean
  private permission: NotificationPermission = 'default'
  private serviceWorkerRegistration: ServiceWorkerRegistration | null = null
  private notificationQueue: NotificationPayload[] = []
  private maxQueueSize = 10

  constructor() {
    this.isSupported = 'Notification' in window
    this.permission = this.isSupported ? Notification.permission : 'denied'
    this.initializeServiceWorker()
  }

  /**
   * Check if browser notifications are supported
   */
  get supported(): boolean {
    return this.isSupported
  }

  /**
   * Get current notification permission status
   */
  get permissionStatus(): NotificationPermission {
    return this.permission
  }

  /**
   * Check if notifications are enabled (permission granted)
   */
  get enabled(): boolean {
    return this.permission === 'granted'
  }

  /**
   * Request notification permission from user
   */
  async requestPermission(): Promise<NotificationPermission> {
    if (!this.isSupported) {
      return 'denied'
    }

    if (this.permission === 'granted') {
      return this.permission
    }

    try {
      this.permission = await Notification.requestPermission()
      return this.permission
    } catch (error) {
      console.error('Error requesting notification permission:', error)
      this.permission = 'denied'
      return this.permission
    }
  }

  /**
   * Send a browser notification
   */
  async sendNotification(title: string, options: NotificationOptions = {}): Promise<Notification | void> {
    if (!this.isSupported) {
      console.warn('Browser notifications not supported')
      return
    }

    if (this.permission !== 'granted') {
      console.warn('Notification permission not granted')
      this.queueNotification({ title, options })
      return
    }

    try {
      // Use service worker notification if available (works when page is not active)
      if (this.serviceWorkerRegistration) {
        return this.sendServiceWorkerNotification(title, options)
      }

      // Fallback to direct browser notification
      return this.sendDirectNotification(title, options)
    } catch (error) {
      console.error('Error sending notification:', error)
      throw error
    }
  }

  /**
   * Send a reminder notification with calendar-specific formatting
   */
  async sendReminderNotification(params: {
    taskTitle: string
    taskId: number
    timeLeft: string
    dueDate: string
    baseUrl?: string
  }): Promise<Notification | void> {
    const { taskTitle, taskId, timeLeft, baseUrl = '' } = params

    const options: NotificationOptions = {
      body: `Il tuo task "${taskTitle}" inizia tra ${timeLeft}`,
      icon: `${baseUrl}/favicon.ico`,
      badge: `${baseUrl}/badge-icon.png`,
      tag: `reminder-${taskId}`,
      requireInteraction: true,
      data: {
        type: 'reminder',
        taskId,
        url: `${baseUrl}/tasks/${taskId}`
      },
      actions: [
        {
          action: 'view',
          title: 'Visualizza Task',
          icon: `${baseUrl}/icons/view.png`
        },
        {
          action: 'snooze',
          title: 'Posticipa 10min',
          icon: `${baseUrl}/icons/snooze.png`
        }
      ],
      vibrate: [200, 100, 200]
    }

    return this.sendNotification(`Promemoria: ${taskTitle}`, options)
  }

  /**
   * Clear all notifications with a specific tag
   */
  async clearNotifications(tag?: string): Promise<void> {
    if (!this.serviceWorkerRegistration) {
      return
    }

    try {
      const notifications = await this.serviceWorkerRegistration.getNotifications({
        tag
      })

      notifications.forEach(notification => notification.close())
    } catch (error) {
      console.error('Error clearing notifications:', error)
    }
  }

  /**
   * Get all active notifications
   */
  async getActiveNotifications(tag?: string): Promise<Notification[]> {
    if (!this.serviceWorkerRegistration) {
      return []
    }

    try {
      return await this.serviceWorkerRegistration.getNotifications({ tag })
    } catch (error) {
      console.error('Error getting active notifications:', error)
      return []
    }
  }

  /**
   * Schedule a notification (requires service worker)
   */
  async scheduleNotification(
    title: string, 
    options: NotificationOptions & { showTime: number }
  ): Promise<void> {
    if (!this.serviceWorkerRegistration) {
      throw new Error('Service worker required for scheduled notifications')
    }

    // Send message to service worker to schedule notification
    await this.sendMessageToServiceWorker({
      type: 'SCHEDULE_NOTIFICATION',
      payload: { title, options }
    })
  }

  /**
   * Cancel a scheduled notification
   */
  async cancelScheduledNotification(tag: string): Promise<void> {
    if (!this.serviceWorkerRegistration) {
      return
    }

    await this.sendMessageToServiceWorker({
      type: 'CANCEL_NOTIFICATION',
      payload: { tag }
    })
  }

  /**
   * Update notification badge count
   */
  async updateBadge(count: number): Promise<void> {
    if ('setAppBadge' in navigator) {
      try {
        if (count > 0) {
          await (navigator as any).setAppBadge(count)
        } else {
          await (navigator as any).clearAppBadge()
        }
      } catch (error) {
        console.error('Error updating app badge:', error)
      }
    }
  }

  /**
   * Clear notification badge
   */
  async clearBadge(): Promise<void> {
    return this.updateBadge(0)
  }

  /**
   * Process queued notifications (after permission granted)
   */
  async processQueuedNotifications(): Promise<void> {
    if (this.permission !== 'granted' || this.notificationQueue.length === 0) {
      return
    }

    const queue = [...this.notificationQueue]
    this.notificationQueue = []

    for (const notification of queue) {
      try {
        await this.sendNotification(notification.title, notification.options)
        // Small delay between notifications to avoid overwhelming
        await new Promise(resolve => setTimeout(resolve, 500))
      } catch (error) {
        console.error('Error processing queued notification:', error)
      }
    }
  }

  /**
   * Set up notification click handlers
   */
  setupNotificationHandlers(handlers: {
    onNotificationClick?: (data: any) => void
    onNotificationClose?: (data: any) => void
    onActionClick?: (action: string, data: any) => void
  }): void {
    if (!this.serviceWorkerRegistration) {
      return
    }

    // Listen for messages from service worker
    navigator.serviceWorker.addEventListener('message', (event) => {
      const { type, payload } = event.data as ServiceWorkerMessage

      switch (type) {
        case 'NOTIFICATION_CLICKED':
          handlers.onNotificationClick?.(payload.data)
          if (payload.action) {
            handlers.onActionClick?.(payload.action, payload.data)
          }
          break

        case 'NOTIFICATION_CLOSED':
          handlers.onNotificationClose?.(payload.data)
          break
      }
    })
  }

  // Private methods

  private async initializeServiceWorker(): Promise<void> {
    if (!('serviceWorker' in navigator)) {
      console.warn('Service Worker not supported')
      return
    }

    try {
      this.serviceWorkerRegistration = await navigator.serviceWorker.ready
    } catch (error) {
      console.error('Error initializing service worker:', error)
    }
  }

  private async sendServiceWorkerNotification(
    title: string, 
    options: NotificationOptions
  ): Promise<void> {
    if (!this.serviceWorkerRegistration) {
      throw new Error('Service worker not available')
    }

    await this.serviceWorkerRegistration.showNotification(title, {
      ...options,
      icon: options.icon || '/favicon.ico',
      badge: options.badge || '/badge-icon.png'
    })
  }

  private sendDirectNotification(
    title: string, 
    options: NotificationOptions
  ): Notification {
    const notification = new Notification(title, {
      ...options,
      icon: options.icon || '/favicon.ico'
    })

    // Set up event listeners
    notification.onclick = () => {
      if (options.data?.url) {
        window.open(options.data.url, '_blank')
      }
      notification.close()
    }

    notification.onerror = (error) => {
      console.error('Notification error:', error)
    }

    // Auto-close after delay if not requiring interaction
    if (!options.requireInteraction) {
      setTimeout(() => {
        notification.close()
      }, 5000)
    }

    return notification
  }

  private queueNotification(notification: NotificationPayload): void {
    if (this.notificationQueue.length >= this.maxQueueSize) {
      this.notificationQueue.shift() // Remove oldest
    }
    this.notificationQueue.push(notification)
  }

  private async sendMessageToServiceWorker(message: ServiceWorkerMessage): Promise<void> {
    if (!this.serviceWorkerRegistration?.active) {
      throw new Error('Service worker not active')
    }

    return new Promise((resolve, reject) => {
      const messageChannel = new MessageChannel()
      
      messageChannel.port1.onmessage = (event) => {
        if (event.data.error) {
          reject(new Error(event.data.error))
        } else {
          resolve(event.data)
        }
      }

      this.serviceWorkerRegistration!.active!.postMessage(message, [messageChannel.port2])
      
      // Timeout after 5 seconds
      setTimeout(() => {
        reject(new Error('Service worker message timeout'))
      }, 5000)
    })
  }
}

// Singleton instance
export const notificationService = new BrowserNotificationService()

// Export class for custom instances
export { BrowserNotificationService }

// Convenience functions
export const requestNotificationPermission = () => 
  notificationService.requestPermission()

export const sendBrowserNotification = (title: string, options?: NotificationOptions) =>
  notificationService.sendNotification(title, options)

export const sendReminderNotification = (params: Parameters<typeof notificationService.sendReminderNotification>[0]) =>
  notificationService.sendReminderNotification(params)

export const clearNotifications = (tag?: string) =>
  notificationService.clearNotifications(tag)

export const updateNotificationBadge = (count: number) =>
  notificationService.updateBadge(count)

export const isNotificationSupported = () =>
  notificationService.supported

export const getNotificationPermission = () =>
  notificationService.permissionStatus