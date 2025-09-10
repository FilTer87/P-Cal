/**
 * Notification Permissions Composable
 * Manages browser notification permissions and provides reactive state
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { 
  notificationService, 
  sendBrowserNotification,
  type NotificationOptions 
} from '../services/notificationService'

export function useNotificationPermissions() {
  // Reactive state
  const permission = ref<NotificationPermission>('default')
  const isSupported = ref(false)
  const isRequesting = ref(false)
  const lastError = ref<string | null>(null)

  // Computed properties
  const isGranted = computed(() => permission.value === 'granted')
  const isDenied = computed(() => permission.value === 'denied')
  const isDefault = computed(() => permission.value === 'default')
  const canRequest = computed(() => isSupported.value && isDefault.value)
  const isEnabled = computed(() => isSupported.value && isGranted.value)

  // Permission status labels (Italian)
  const permissionLabel = computed(() => {
    switch (permission.value) {
      case 'granted':
        return 'Autorizzato'
      case 'denied':
        return 'Negato'
      case 'default':
        return 'Non richiesto'
      default:
        return 'Sconosciuto'
    }
  })

  const permissionDescription = computed(() => {
    switch (permission.value) {
      case 'granted':
        return 'Le notifiche sono autorizzate e funzioneranno correttamente.'
      case 'denied':
        return 'Le notifiche sono state negate. Puoi abilitarle nelle impostazioni del browser.'
      case 'default':
        return 'Le notifiche non sono ancora state richieste. Clicca per autorizzarle.'
      default:
        return 'Stato delle notifiche sconosciuto.'
    }
  })

  // Methods
  const updatePermissionStatus = () => {
    permission.value = notificationService.permissionStatus
    isSupported.value = notificationService.supported
    lastError.value = null
  }

  const requestPermission = async (): Promise<NotificationPermission> => {
    if (!isSupported.value) {
      const error = 'Le notifiche non sono supportate da questo browser'
      lastError.value = error
      throw new Error(error)
    }

    if (isRequesting.value) {
      throw new Error('Richiesta permessi già in corso')
    }

    if (isGranted.value) {
      return permission.value
    }

    isRequesting.value = true
    lastError.value = null

    try {
      const result = await notificationService.requestPermission()
      permission.value = result
      
      if (result === 'denied') {
        lastError.value = 'Permesso negato dall\'utente'
      } else if (result === 'granted') {
        // Process any queued notifications
        await notificationService.processQueuedNotifications()
      }
      
      return result
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Errore nella richiesta permessi'
      lastError.value = errorMessage
      console.error('Error requesting notification permission:', error)
      throw error
    } finally {
      isRequesting.value = false
    }
  }

  const sendBrowserNotification = async (
    title: string, 
    options?: NotificationOptions
  ): Promise<Notification | void> => {
    if (!isEnabled.value) {
      throw new Error('Notifiche non autorizzate o non supportate')
    }

    try {
      return await notificationService.sendNotification(title, options)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Errore nell\'invio della notifica'
      lastError.value = errorMessage
      throw error
    }
  }

  const sendTestNotification = async (): Promise<void> => {
    await sendBrowserNotification(
      'Test Notifica PrivateCal',
      {
        body: 'Questa è una notifica di test per verificare che tutto funzioni correttamente.',
        icon: '/favicon.ico',
        tag: 'test-notification',
        requireInteraction: false
      }
    )
  }

  const clearError = () => {
    lastError.value = null
  }

  // Browser-specific permission guides
  const getPermissionGuide = () => {
    const userAgent = navigator.userAgent.toLowerCase()
    
    if (userAgent.includes('chrome')) {
      return {
        browser: 'Chrome',
        steps: [
          'Clicca sull\'icona del lucchetto nella barra degli indirizzi',
          'Seleziona "Notifiche" e scegli "Consenti"',
          'Ricarica la pagina per applicare le modifiche'
        ]
      }
    } else if (userAgent.includes('firefox')) {
      return {
        browser: 'Firefox',
        steps: [
          'Clicca sull\'icona dello scudo nella barra degli indirizzi',
          'Seleziona "Notifiche" e scegli "Consenti"',
          'Ricarica la pagina per applicare le modifiche'
        ]
      }
    } else if (userAgent.includes('safari')) {
      return {
        browser: 'Safari',
        steps: [
          'Vai in Safari > Preferenze > Siti web',
          'Seleziona "Notifiche" nella barra laterale sinistra',
          'Trova questo sito e scegli "Consenti"'
        ]
      }
    } else if (userAgent.includes('edge')) {
      return {
        browser: 'Edge',
        steps: [
          'Clicca sull\'icona del lucchetto nella barra degli indirizzi',
          'Seleziona "Notifiche" e scegli "Consenti"',
          'Ricarica la pagina per applicare le modifiche'
        ]
      }
    }

    return {
      browser: 'Browser',
      steps: [
        'Cerca l\'icona delle impostazioni nella barra degli indirizzi',
        'Trova le impostazioni per le notifiche',
        'Autorizza le notifiche per questo sito'
      ]
    }
  }

  // Event listeners for permission changes
  const handlePermissionChange = () => {
    updatePermissionStatus()
  }

  // Lifecycle
  onMounted(() => {
    updatePermissionStatus()
    
    // Listen for permission changes (not widely supported yet)
    if ('permissions' in navigator) {
      navigator.permissions.query({ name: 'notifications' as PermissionName })
        .then(permissionStatus => {
          permissionStatus.addEventListener('change', handlePermissionChange)
        })
        .catch(() => {
          // Permissions API not supported, fallback to periodic checking
          const interval = setInterval(updatePermissionStatus, 1000)
          setTimeout(() => clearInterval(interval), 10000) // Check for 10 seconds
        })
    }
  })

  onUnmounted(() => {
    // Cleanup if needed
    if ('permissions' in navigator) {
      navigator.permissions.query({ name: 'notifications' as PermissionName })
        .then(permissionStatus => {
          permissionStatus.removeEventListener('change', handlePermissionChange)
        })
        .catch(() => {
          // Ignore cleanup errors
        })
    }
  })

  return {
    // State
    permission,
    isSupported,
    isRequesting,
    lastError,
    
    // Computed
    isGranted,
    isDenied,
    isDefault,
    canRequest,
    isEnabled,
    permissionLabel,
    permissionDescription,
    
    // Methods
    requestPermission,
    sendBrowserNotification,
    sendTestNotification,
    clearError,
    getPermissionGuide,
    updatePermissionStatus
  }
}