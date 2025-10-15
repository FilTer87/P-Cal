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
import { i18nGlobal } from '../i18n'

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

  // Permission status labels
  const permissionLabel = computed(() => {
    const t = i18nGlobal.t
    switch (permission.value) {
      case 'granted':
        return t('composables.useNotificationPermissions.permissionLabels.granted')
      case 'denied':
        return t('composables.useNotificationPermissions.permissionLabels.denied')
      case 'default':
        return t('composables.useNotificationPermissions.permissionLabels.default')
      default:
        return t('composables.useNotificationPermissions.permissionLabels.unknown')
    }
  })

  const permissionDescription = computed(() => {
    const t = i18nGlobal.t
    switch (permission.value) {
      case 'granted':
        return t('composables.useNotificationPermissions.permissionDescriptions.granted')
      case 'denied':
        return t('composables.useNotificationPermissions.permissionDescriptions.denied')
      case 'default':
        return t('composables.useNotificationPermissions.permissionDescriptions.default')
      default:
        return t('composables.useNotificationPermissions.permissionDescriptions.unknown')
    }
  })

  // Methods
  const updatePermissionStatus = () => {
    permission.value = notificationService.permissionStatus
    isSupported.value = notificationService.supported
    lastError.value = null
  }

  const requestPermission = async (): Promise<NotificationPermission> => {
    const t = i18nGlobal.t
    if (!isSupported.value) {
      const error = t('composables.useNotificationPermissions.errors.notSupported')
      lastError.value = error
      throw new Error(error)
    }

    if (isRequesting.value) {
      throw new Error(t('composables.useNotificationPermissions.errors.requesting'))
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
        lastError.value = t('composables.useNotificationPermissions.errors.permissionDenied')
      } else if (result === 'granted') {
        // Process any queued notifications
        await notificationService.processQueuedNotifications()
      }

      return result
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : t('composables.useNotificationPermissions.errors.requestError')
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
      throw new Error(i18nGlobal.t('composables.useNotificationPermissions.errors.notAuthorized'))
    }

    try {
      return await notificationService.sendNotification(title, options)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : i18nGlobal.t('composables.useNotificationPermissions.errors.sendError')
      lastError.value = errorMessage
      throw error
    }
  }

  const sendTestNotification = async (): Promise<void> => {
    const t = i18nGlobal.t
    await sendBrowserNotification(
      t('composables.useNotificationPermissions.testNotification.title'),
      {
        body: t('composables.useNotificationPermissions.testNotification.body'),
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
    const t = i18nGlobal.t

    if (userAgent.includes('chrome')) {
      return {
        browser: 'Chrome',
        steps: [
          t('composables.useNotificationPermissions.guide.chrome.step1'),
          t('composables.useNotificationPermissions.guide.chrome.step2'),
          t('composables.useNotificationPermissions.guide.chrome.step3')
        ]
      }
    } else if (userAgent.includes('firefox')) {
      return {
        browser: 'Firefox',
        steps: [
          t('composables.useNotificationPermissions.guide.firefox.step1'),
          t('composables.useNotificationPermissions.guide.firefox.step2'),
          t('composables.useNotificationPermissions.guide.firefox.step3')
        ]
      }
    } else if (userAgent.includes('safari')) {
      return {
        browser: 'Safari',
        steps: [
          t('composables.useNotificationPermissions.guide.safari.step1'),
          t('composables.useNotificationPermissions.guide.safari.step2'),
          t('composables.useNotificationPermissions.guide.safari.step3')
        ]
      }
    } else if (userAgent.includes('edge')) {
      return {
        browser: 'Edge',
        steps: [
          t('composables.useNotificationPermissions.guide.edge.step1'),
          t('composables.useNotificationPermissions.guide.edge.step2'),
          t('composables.useNotificationPermissions.guide.edge.step3')
        ]
      }
    }

    return {
      browser: 'Browser',
      steps: [
        t('composables.useNotificationPermissions.guide.default.step1'),
        t('composables.useNotificationPermissions.guide.default.step2'),
        t('composables.useNotificationPermissions.guide.default.step3')
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