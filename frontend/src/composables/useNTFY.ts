/**
 * NTFY Composable
 * Manages NTFY push notification service integration with reactive state
 */

import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { 
  ntfyService,
  type NTFYNotification,
  type NTFYResponse,
  generateUserTopic,
  validateServerUrl,
  testConnection as testNTFYConnection
} from '../services/ntfyService'
import { useAuth } from './useAuth'

interface NTFYSettings {
  enabled: boolean
  server: string
  topic: string
  priority: 1 | 2 | 3 | 4 | 5
  testMode: boolean
}

export function useNTFY() {
  const { user } = useAuth()

  // Reactive state
  const settings = ref<NTFYSettings>({
    enabled: false,
    server: 'https://ntfy.sh',
    topic: '',
    priority: 3,
    testMode: false
  })

  const isConnected = ref(false)
  const isConnecting = ref(false)
  const isSending = ref(false)
  const lastError = ref<string | null>(null)
  const lastResponse = ref<NTFYResponse | null>(null)
  const connectionStatus = ref<'disconnected' | 'connecting' | 'connected' | 'error'>('disconnected')
  
  // Event source for real-time notifications
  const eventSource = ref<EventSource | null>(null)
  const subscribedTopic = ref<string | null>(null)

  // Computed properties
  const isConfigured = computed(() => {
    return !!(settings.value.server && settings.value.topic)
  })

  const isReady = computed(() => {
    return settings.value.enabled && isConfigured.value && isConnected.value
  })

  const subscriptionUrl = computed(() => {
    if (!isConfigured.value) return ''
    return `${settings.value.server}/${settings.value.topic}`
  })

  const qrCodeUrl = computed(() => {
    if (!isConfigured.value) return ''
    // Generate QR code URL for NTFY app subscription
    const subscribeUrl = `${settings.value.server}/${settings.value.topic}`
    return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(subscribeUrl)}`
  })

  // Methods
  const updateSettings = (newSettings: Partial<NTFYSettings>) => {
    settings.value = { ...settings.value, ...newSettings }
    saveSettings()
    
    // Reconnect if server or topic changed
    if (newSettings.server || newSettings.topic) {
      reconnect()
    }
  }

  const generateTopic = (): string => {
    if (!user.value) {
      throw new Error('User not authenticated')
    }
    
    const topic = generateUserTopic(user.value.id)
    settings.value.topic = topic
    saveSettings()
    return topic
  }

  const testConnection = async (server?: string): Promise<boolean> => {
    const serverUrl = server || settings.value.server
    
    if (!validateServerUrl(serverUrl)) {
      lastError.value = 'URL server non valido'
      return false
    }

    isConnecting.value = true
    connectionStatus.value = 'connecting'
    lastError.value = null

    try {
      const result = await testNTFYConnection(serverUrl)
      isConnected.value = result
      connectionStatus.value = result ? 'connected' : 'error'
      
      if (!result) {
        lastError.value = 'Impossibile connettersi al server NTFY'
      }
      
      return result
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Errore di connessione'
      lastError.value = errorMessage
      isConnected.value = false
      connectionStatus.value = 'error'
      console.error('NTFY connection test failed:', error)
      return false
    } finally {
      isConnecting.value = false
    }
  }

  const sendNotification = async (notification: Omit<NTFYNotification, 'server' | 'topic'>): Promise<NTFYResponse | null> => {
    if (!isReady.value) {
      throw new Error('NTFY non configurato o non connesso')
    }

    isSending.value = true
    lastError.value = null

    try {
      const fullNotification: NTFYNotification = {
        ...notification,
        server: settings.value.server,
        topic: settings.value.topic,
        priority: notification.priority || settings.value.priority
      }

      const response = await ntfyService.sendNotification(fullNotification)
      lastResponse.value = response
      return response
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Errore nell\'invio della notifica'
      lastError.value = errorMessage
      console.error('NTFY send notification failed:', error)
      return null
    } finally {
      isSending.value = false
    }
  }

  const sendReminderNotification = async (params: {
    taskTitle: string
    taskId: number
    timeLeft: string
    dueDate: string
    baseUrl?: string
  }): Promise<NTFYResponse | null> => {
    if (!isReady.value) {
      throw new Error('NTFY non configurato o non connesso')
    }

    const fullParams = {
      ...params,
      server: settings.value.server,
      topic: settings.value.topic,
      baseUrl: params.baseUrl || window.location.origin
    }

    try {
      isSending.value = true
      const response = await ntfyService.sendReminderNotification(fullParams)
      lastResponse.value = response
      return response
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Errore nell\'invio del promemoria'
      lastError.value = errorMessage
      console.error('NTFY send reminder failed:', error)
      return null
    } finally {
      isSending.value = false
    }
  }

  const sendTestNotification = async (): Promise<boolean> => {
    try {
      const response = await sendNotification({
        title: 'Test PrivateCal',
        message: 'Questa è una notifica di test da PrivateCal. Se la ricevi, la configurazione funziona correttamente!',
        tags: ['test', '🧪'],
        priority: 3,
        click: window.location.origin
      })
      
      return !!response
    } catch (error) {
      console.error('Test notification failed:', error)
      return false
    }
  }

  const subscribe = async (onNotification?: (notification: NTFYResponse) => void): Promise<boolean> => {
    if (!isConfigured.value) {
      lastError.value = 'NTFY non configurato'
      return false
    }

    if (eventSource.value && subscribedTopic.value === settings.value.topic) {
      return true // Already subscribed to the same topic
    }

    // Close existing subscription
    unsubscribe()

    try {
      eventSource.value = await ntfyService.subscribeToTopic(
        settings.value.server,
        settings.value.topic,
        (notification) => {
          // Handle received notification
          onNotification?.(notification)
          
          // Store last received notification
          lastResponse.value = notification
        }
      )

      subscribedTopic.value = settings.value.topic
      return true
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Errore nella sottoscrizione'
      lastError.value = errorMessage
      console.error('NTFY subscription failed:', error)
      return false
    }
  }

  const unsubscribe = () => {
    if (eventSource.value) {
      eventSource.value.close()
      eventSource.value = null
    }
    subscribedTopic.value = null
  }

  const reconnect = async () => {
    unsubscribe()
    
    if (settings.value.enabled && isConfigured.value) {
      const connected = await testConnection()
      if (connected) {
        await subscribe()
      }
    }
  }

  const clearError = () => {
    lastError.value = null
  }

  const reset = () => {
    unsubscribe()
    settings.value = {
      enabled: false,
      server: 'https://ntfy.sh',
      topic: '',
      priority: 3,
      testMode: false
    }
    isConnected.value = false
    connectionStatus.value = 'disconnected'
    lastError.value = null
    lastResponse.value = null
    saveSettings()
  }

  // Storage methods
  const saveSettings = () => {
    try {
      localStorage.setItem('ntfy-settings', JSON.stringify(settings.value))
    } catch (error) {
      console.error('Failed to save NTFY settings:', error)
    }
  }

  const loadSettings = () => {
    try {
      const saved = localStorage.getItem('ntfy-settings')
      if (saved) {
        const parsedSettings = JSON.parse(saved)
        settings.value = { ...settings.value, ...parsedSettings }
        
        // Generate topic if not present and user is available
        if (!settings.value.topic && user.value) {
          generateTopic()
        }
      }
    } catch (error) {
      console.error('Failed to load NTFY settings:', error)
    }
  }

  // NTFY app integration helpers
  const getAppDownloadLinks = () => ({
    android: 'https://play.google.com/store/apps/details?id=io.heckel.ntfy',
    ios: 'https://apps.apple.com/us/app/ntfy/id1625396347',
    fdroid: 'https://f-droid.org/en/packages/io.heckel.ntfy/'
  })

  const copySubscriptionUrl = async (): Promise<boolean> => {
    if (!isConfigured.value) return false
    
    try {
      await navigator.clipboard.writeText(subscriptionUrl.value)
      return true
    } catch (error) {
      console.error('Failed to copy subscription URL:', error)
      return false
    }
  }

  // Statistics and monitoring
  const getTopicStats = async () => {
    if (!isConfigured.value) return null
    
    try {
      return await ntfyService.getTopicStats(settings.value.server, settings.value.topic)
    } catch (error) {
      console.error('Failed to get topic stats:', error)
      return null
    }
  }

  // Watchers
  watch(() => settings.value.enabled, (enabled) => {
    if (enabled && isConfigured.value) {
      reconnect()
    } else {
      unsubscribe()
    }
  })

  watch(() => user.value, (newUser) => {
    if (newUser && !settings.value.topic) {
      generateTopic()
    }
  })

  // Lifecycle
  onMounted(() => {
    loadSettings()
    
    // Auto-connect if enabled and configured
    if (settings.value.enabled && isConfigured.value) {
      reconnect()
    }
  })

  onUnmounted(() => {
    unsubscribe()
    ntfyService.cancelRequests()
  })

  return {
    // State
    settings,
    isConnected,
    isConnecting,
    isSending,
    lastError,
    lastResponse,
    connectionStatus,
    subscribedTopic,
    
    // Computed
    isConfigured,
    isReady,
    subscriptionUrl,
    qrCodeUrl,
    
    // Methods
    updateSettings,
    generateTopic,
    testConnection,
    sendNotification,
    sendReminderNotification,
    sendTestNotification,
    subscribe,
    unsubscribe,
    reconnect,
    clearError,
    reset,
    saveSettings,
    loadSettings,
    getAppDownloadLinks,
    copySubscriptionUrl,
    getTopicStats
  }
}