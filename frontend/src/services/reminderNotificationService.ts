/**
 * Reminder Notification Service
 * Handles automatic checking and notification of due reminders using SSE
 */

import { ref, computed } from 'vue'
import { useCustomToast } from '../composables/useCustomToast'
import { notificationService } from './notificationService'
import { reminderApi } from './reminderApi'
import type { Reminder } from '../types/task'

export interface ReminderNotificationState {
  isListening: boolean
  isConnected: boolean
  lastCheck: Date | null
  processedReminders: Set<number>
  eventSource: EventSource | null
}

class ReminderNotificationService {
  private state = ref<ReminderNotificationState>({
    isListening: false,
    isConnected: false,
    lastCheck: null,
    processedReminders: new Set(),
    eventSource: null
  })
  
  private checkInterval: number | null = null
  private readonly CHECK_INTERVAL_MS = 30000 // Check every 30 seconds
  private readonly NOTIFICATION_WINDOW_MS = 30000 // Show notification if reminder is within 30 seconds
  
  private toastService = useCustomToast()

  /**
   * Start listening for due reminders
   */
  async startListening(): Promise<void> {
    if (this.state.value.isListening) {
      console.log('🔔 Reminder notification service already running')
      return
    }

    console.log('🔔 Starting reminder notification service...')
    this.state.value.isListening = true
    
    // Start periodic checking (fallback method)
    this.startPeriodicCheck()
    
    // Try to connect to SSE endpoint (when available)
    await this.connectSSE()
  }

  /**
   * Stop listening for reminders
   */
  stopListening(): void {
    console.log('🔔 Stopping reminder notification service...')
    
    this.state.value.isListening = false
    this.state.value.isConnected = false
    
    // Stop periodic checking
    if (this.checkInterval) {
      clearInterval(this.checkInterval)
      this.checkInterval = null
    }
    
    // Close SSE connection
    if (this.state.value.eventSource) {
      this.state.value.eventSource.close()
      this.state.value.eventSource = null
    }
  }

  /**
   * Check for due reminders and show notifications
   */
  private async checkDueReminders(): Promise<void> {
    try {
      const reminders = await reminderApi.getPendingReminders()
      const now = new Date()
      
      console.debug(`🔔 Checking ${reminders.length} reminders at ${now.toLocaleTimeString()} (${now.toISOString()})`)
      
      for (const reminder of reminders) {
        const reminderTime = new Date(reminder.reminderTime)
        const timeDiff = reminderTime.getTime() - now.getTime()
        const minutesDiff = Math.round(timeDiff / (1000 * 60))
        
        console.debug(`🔔 Reminder ${reminder.id}: ${reminder.reminderTime} -> Local: ${reminderTime.toLocaleTimeString()} (diff: ${minutesDiff} min)`)
        
        if (this.shouldNotifyReminder(reminder, now)) {
          console.debug(`🔔 Triggering notification for reminder ${reminder.id}`)
          await this.showReminderNotification(reminder)
          this.state.value.processedReminders.add(reminder.id)
        }
      }
      
      this.state.value.lastCheck = now
    } catch (error) {
      console.error('❌ Error checking due reminders:', error)
    }
  }

  /**
   * Check if a reminder should trigger a notification
   */
  private shouldNotifyReminder(reminder: Reminder, now: Date): boolean {
    // Skip if already processed
    if (this.state.value.processedReminders.has(reminder.id)) {
      return false
    }
    
    // Skip if already sent
    if (reminder.isSent || reminder.sent) {
      return false
    }
    
    const reminderTime = new Date(reminder.reminderTime)
    const timeDiff = reminderTime.getTime() - now.getTime()
    
    // Show notification if reminder time is within the notification window (past due or within 1 minute)
    return timeDiff <= this.NOTIFICATION_WINDOW_MS && timeDiff >= -this.NOTIFICATION_WINDOW_MS
  }

  /**
   * Show notification for a reminder
   */
  private async showReminderNotification(reminder: Reminder): Promise<void> {
    try {
      const reminderTime = new Date(reminder.reminderTime)
      const now = new Date()
      const isPastDue = reminderTime < now
      
      // Calculate task start time based on reminder time and offset
      const taskStartTime = new Date(reminderTime.getTime() + (reminder.reminderOffsetMinutes * 60 * 1000))
      const timeUntilTask = taskStartTime.getTime() - now.getTime()
      const minutesUntilTask = Math.round(timeUntilTask / (1000 * 60))
      
      let timeText = ''
      if (timeUntilTask < 0) {
        const minutesPastStart = Math.abs(minutesUntilTask)
        timeText = minutesPastStart === 0 ? 'ora' : `iniziato ${minutesPastStart} minuti fa`
      } else {
        timeText = minutesUntilTask === 0 ? 'inizia ora' : `inizia tra ${minutesUntilTask} minuti`
      }
      
      // Show browser notification if supported and permitted
      if (notificationService.enabled) {
        await notificationService.sendReminderNotification({
          taskTitle: reminder.taskTitle || `Task ID ${reminder.taskId}`,
          taskId: reminder.taskId,
          timeLeft: timeText,
          dueDate: reminder.reminderTime
        })
      } else {
        // Fallback to toast only if browser notifications are not available
        const title = isPastDue ? '⏰ Promemoria scaduto!' : '🔔 Promemoria'
        const message = `${reminder.taskTitle || 'Task'} ${timeText}`
        
        if (isPastDue || timeUntilTask < 0) {
          this.toastService.showWarning(message, { title })
        } else {
          this.toastService.showInfo(message, { title })
        }
      }
      
      // Dispatch custom event to notify other parts of the app that reminder was shown
      window.dispatchEvent(new CustomEvent('reminder-notified', { 
        detail: { reminderId: reminder.id } 
      }))
      
      console.debug(`🔔 Notification shown for reminder ${reminder.id}`)
    } catch (error) {
      console.error('❌ Error showing reminder notification:', error)
    }
  }

  /**
   * Start periodic checking (fallback method)
   */
  private startPeriodicCheck(): void {
    // Initial check
    this.checkDueReminders()
    
    // Set up interval
    this.checkInterval = window.setInterval(() => {
      if (this.state.value.isListening) {
        this.checkDueReminders()
      }
    }, this.CHECK_INTERVAL_MS)
    
    console.log(`🔔 Periodic reminder checking started (every ${this.CHECK_INTERVAL_MS/1000}s)`)
  }

  /**
   * Connect to SSE endpoint for real-time reminders (future implementation)
   */
  private async connectSSE(): Promise<void> {
    // For now, just log that SSE is not implemented yet
    console.log('📡 SSE connection for reminders not implemented yet, using polling fallback')
    
    // TODO: Implement when backend SSE endpoint is available
    /*
    try {
      const eventSource = new EventSource('/api/reminders/events')
      
      eventSource.onopen = () => {
        console.log('📡 SSE connection opened for reminders')
        this.state.value.isConnected = true
        this.state.value.eventSource = eventSource
      }
      
      eventSource.onmessage = (event) => {
        try {
          const reminder: Reminder = JSON.parse(event.data)
          console.log('📡 Received reminder via SSE:', reminder)
          this.showReminderNotification(reminder)
        } catch (error) {
          console.error('❌ Error parsing SSE reminder data:', error)
        }
      }
      
      eventSource.onerror = (error) => {
        console.error('❌ SSE connection error:', error)
        this.state.value.isConnected = false
        
        // Fallback to polling if SSE fails
        if (!this.checkInterval) {
          this.startPeriodicCheck()
        }
      }
    } catch (error) {
      console.error('❌ Failed to establish SSE connection:', error)
    }
    */
  }

  /**
   * Get current service state
   */
  get status() {
    return computed(() => ({
      isListening: this.state.value.isListening,
      isConnected: this.state.value.isConnected,
      lastCheck: this.state.value.lastCheck,
      processedCount: this.state.value.processedReminders.size,
      method: this.state.value.isConnected ? 'SSE' : 'Polling'
    }))
  }

  /**
   * Clear processed reminders cache
   */
  clearProcessedCache(): void {
    this.state.value.processedReminders.clear()
    console.log('🔔 Processed reminders cache cleared')
  }

  /**
   * Force immediate check for due reminders
   */
  async forceCheck(): Promise<void> {
    console.log('🔔 Forcing immediate reminder check...')
    await this.checkDueReminders()
  }
}

// Singleton instance
export const reminderNotificationService = new ReminderNotificationService()

// Export for manual usage
export const startReminderNotifications = () => reminderNotificationService.startListening()
export const stopReminderNotifications = () => reminderNotificationService.stopListening()
export const forceReminderCheck = () => reminderNotificationService.forceCheck()