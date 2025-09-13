import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.mount('#app')

// Register Service Worker for notifications
if ('serviceWorker' in navigator) {
  window.addEventListener('load', async () => {
    try {
      const registration = await navigator.serviceWorker.register('/sw-notifications.js')
      console.log('🔔 Notification Service Worker registered successfully:', registration.scope)

      // Listen for service worker messages
      navigator.serviceWorker.addEventListener('message', (event) => {
        console.log('Message from service worker:', event.data)
        
        if (!event.data) return
        
        const { type, payload } = event.data
        
        switch (type) {
          case 'NOTIFICATION_CLICKED':
            console.log('Notification clicked:', payload)
            if (payload?.action === 'view' && payload?.data?.url) {
              // Navigate to the task
              router.push(payload.data.url)
            }
            break
          case 'NOTIFICATION_CLOSED':
            console.log('Notification closed:', payload)
            // Handle notification close
            break
          case 'NAVIGATE_TO':
            // Handle navigation request from service worker
            if (payload?.url) {
              router.push(payload.url)
            }
            break
        }
      })

      // Add test function for development
      if (import.meta.env.DEV) {
        (window as any).testNotification = async () => {
          if (registration.active) {
            const channel = new MessageChannel()
            channel.port1.onmessage = (event) => {
              console.log('Test notification result:', event.data)
            }
            
            registration.active.postMessage({
              type: 'SHOW_NOTIFICATION',
              payload: {
                title: 'Test P-Cal Notification',
                body: 'Questa è una notifica di test da P-Cal',
                icon: '/favicon.ico',
                tag: 'test-notification',
                data: { test: true }
              }
            }, [channel.port2])
          }
        }
        
        console.log('💡 Use window.testNotification() to test notifications in dev mode')
      }

    } catch (error) {
      console.error('❌ Service Worker registration failed:', error)
    }
  })
}