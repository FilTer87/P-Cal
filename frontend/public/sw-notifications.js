/**
 * Service Worker for P-Cal Notifications
 * Handles background notifications and click events
 */

const CACHE_NAME = 'privatecal-notifications-v1'

// Install event
self.addEventListener('install', (event) => {
  console.log('P-Cal Notification Service Worker installed')
  self.skipWaiting()
})

// Activate event
self.addEventListener('activate', (event) => {
  console.log('P-Cal Notification Service Worker activated')
  event.waitUntil(self.clients.claim())
})

// Handle notification display
self.addEventListener('notificationclick', (event) => {
  console.log('Notification clicked:', event)
  
  const notification = event.notification
  const action = event.action
  const data = notification.data || {}
  
  notification.close()
  
  // Handle different actions
  if (action === 'view' || !action) {
    // Default action or view action
    const urlToOpen = data.url || '/'
    
    event.waitUntil(
      clients.matchAll({
        type: 'window',
        includeUncontrolled: true
      }).then((clientList) => {
        // Try to focus existing window
        for (const client of clientList) {
          if (client.url.startsWith(self.location.origin)) {
            return client.focus().then(() => {
              // Navigate to the task if URL is different
              if (urlToOpen !== '/') {
                return client.postMessage({
                  type: 'NAVIGATE_TO',
                  url: urlToOpen
                })
              }
            })
          }
        }
        
        // No existing window found, open new one
        return clients.openWindow(urlToOpen)
      })
    )
  }
  
  // Send message to all clients about the click
  event.waitUntil(
    clients.matchAll().then((clientList) => {
      clientList.forEach((client) => {
        client.postMessage({
          type: 'NOTIFICATION_CLICKED',
          payload: {
            action,
            data: notification.data
          }
        })
      })
    })
  )
})

// Handle notification close
self.addEventListener('notificationclose', (event) => {
  console.log('Notification closed:', event)
  
  const notification = event.notification
  
  // Send message to all clients about the close
  event.waitUntil(
    clients.matchAll().then((clientList) => {
      clientList.forEach((client) => {
        client.postMessage({
          type: 'NOTIFICATION_CLOSED',
          payload: {
            data: notification.data
          }
        })
      })
    })
  )
})

// Handle messages from main thread
self.addEventListener('message', (event) => {
  const { type, payload } = event.data
  const port = event.ports[0]
  
  switch (type) {
    case 'SHOW_NOTIFICATION':
      showNotification(payload)
        .then(() => port.postMessage({ success: true }))
        .catch((error) => port.postMessage({ error: error.message }))
      break
      
    case 'SCHEDULE_NOTIFICATION':
      scheduleNotification(payload)
        .then(() => port.postMessage({ success: true }))
        .catch((error) => port.postMessage({ error: error.message }))
      break
      
    case 'CANCEL_NOTIFICATION':
      cancelNotification(payload.tag)
        .then(() => port.postMessage({ success: true }))
        .catch((error) => port.postMessage({ error: error.message }))
      break
      
    default:
      port.postMessage({ error: 'Unknown message type' })
  }
})

// Helper functions
async function showNotification(options) {
  const {
    title,
    body,
    icon = '/favicon.ico',
    badge = '/badge-icon.png',
    tag,
    data = {},
    requireInteraction = false,
    actions = [],
    vibrate = [200, 100, 200]
  } = options
  
  return self.registration.showNotification(title, {
    body,
    icon,
    badge,
    tag,
    data,
    requireInteraction,
    actions: actions.map(action => ({
      action: action.action,
      title: action.title,
      icon: action.icon
    })),
    vibrate,
    timestamp: Date.now()
  })
}

async function scheduleNotification(options) {
  const { showTime, ...notificationOptions } = options
  const delay = showTime - Date.now()
  
  if (delay > 0) {
    setTimeout(() => {
      showNotification(notificationOptions)
    }, delay)
  } else {
    // Show immediately if time has passed
    return showNotification(notificationOptions)
  }
}

async function cancelNotification(tag) {
  const notifications = await self.registration.getNotifications({ tag })
  notifications.forEach(notification => notification.close())
}

// Background sync for reminder notifications (if supported)
if ('sync' in self.registration) {
  self.addEventListener('sync', (event) => {
    if (event.tag === 'reminder-sync') {
      event.waitUntil(syncReminders())
    }
  })
}

async function syncReminders() {
  try {
    // This would typically fetch pending reminders from your API
    // and show notifications for any that are due
    console.log('Syncing reminders...')
    
    // Example: fetch pending reminders
    // const response = await fetch('/api/reminders/pending')
    // const reminders = await response.json()
    
    // Show notifications for due reminders
    // for (const reminder of reminders) {
    //   if (isReminderDue(reminder)) {
    //     await showReminderNotification(reminder)
    //   }
    // }
  } catch (error) {
    console.error('Error syncing reminders:', error)
  }
}

// Push notifications (for NTFY integration or web push)
self.addEventListener('push', (event) => {
  console.log('Push message received:', event)
  
  if (event.data) {
    try {
      const data = event.data.json()
      
      event.waitUntil(
        showNotification({
          title: data.title || 'P-Cal',
          body: data.message || data.body || 'Hai una nuova notifica',
          icon: data.icon || '/favicon.ico',
          badge: '/badge-icon.png',
          tag: data.tag || 'push-notification',
          data: data,
          requireInteraction: true,
          actions: data.actions || []
        })
      )
    } catch (error) {
      console.error('Error parsing push data:', error)
      
      // Show generic notification
      event.waitUntil(
        showNotification({
          title: 'P-Cal',
          body: 'Hai una nuova notifica',
          icon: '/favicon.ico',
          badge: '/badge-icon.png'
        })
      )
    }
  }
})

// Utility functions
function isReminderDue(reminder) {
  const reminderTime = new Date(reminder.reminderDateTime)
  const now = new Date()
  return reminderTime <= now && !reminder.sent
}

function createReminderNotification(reminder) {
  return {
    title: `Promemoria: ${reminder.task?.title || 'Task'}`,
    body: `Il tuo task inizia tra ${getTimeUntilDue(reminder.task?.dueDate)}`,
    icon: '/favicon.ico',
    badge: '/badge-icon.png',
    tag: `reminder-${reminder.id}`,
    data: {
      type: 'reminder',
      reminderId: reminder.id,
      taskId: reminder.taskId,
      url: `/tasks/${reminder.taskId}`
    },
    requireInteraction: true,
    actions: [
      {
        action: 'view',
        title: 'Visualizza Task',
        icon: '/icons/view.png'
      },
      {
        action: 'snooze',
        title: 'Posticipa 10min',
        icon: '/icons/snooze.png'
      }
    ]
  }
}

function getTimeUntilDue(dueDate) {
  if (!dueDate) return 'breve'
  
  const due = new Date(dueDate)
  const now = new Date()
  const diffMinutes = Math.floor((due.getTime() - now.getTime()) / (1000 * 60))
  
  if (diffMinutes <= 0) return 'ora'
  if (diffMinutes < 60) return `${diffMinutes} minuti`
  if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)} ore`
  return `${Math.floor(diffMinutes / 1440)} giorni`
}