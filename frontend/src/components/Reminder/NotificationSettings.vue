<template>
  <div class="notification-settings">
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100">
            {{ t('notifications.title') }}
          </h3>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
            {{ t('notifications.subtitle') }}
          </p>
        </div>
        <BellIcon class="h-6 w-6 text-gray-400" />
      </div>

      <!-- Global Notifications Toggle -->
      <div class="mb-6">
        <div class="flex items-center justify-between">
          <div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100">
              {{ t('notifications.enableNotifications') }}
            </h4>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('notifications.enableNotificationsDesc') }}
            </p>
          </div>
          <button
            @click="toggleNotifications"
            :class="[
              'relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
              settings.enabled ? 'bg-blue-600' : 'bg-gray-200 dark:bg-gray-700'
            ]"
          >
            <span
              :class="[
                'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                settings.enabled ? 'translate-x-5' : 'translate-x-0'
              ]"
            />
          </button>
        </div>
      </div>

      <!-- Browser Notifications -->
      <div class="mb-6">
        <div class="flex items-center justify-between mb-3">
          <div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100">
              {{ t('notifications.browserNotifications') }}
            </h4>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('notifications.browserNotificationsDesc') }}
            </p>
          </div>
          <div class="flex items-center space-x-2">
            <span
              :class="[
                'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
                browserPermission === 'granted' 
                  ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                  : browserPermission === 'denied'
                  ? 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                  : 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200'
              ]"
            >
              {{ getBrowserPermissionLabel(browserPermission) }}
            </span>
            <button
              v-if="browserPermission !== 'granted'"
              @click="requestBrowserPermissions"
              :disabled="isRequestingPermissions"
              class="px-3 py-1 text-xs font-medium text-blue-600 bg-blue-50 rounded-md hover:bg-blue-100 dark:bg-blue-900 dark:text-blue-300 dark:hover:bg-blue-800 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 transition-colors duration-200"
            >
              <LoadingSpinner v-if="isRequestingPermissions" size="small" class="mr-1" />
              {{ t('notifications.requestPermission') }}
            </button>
          </div>
        </div>
      </div>

      <!-- NTFY Settings -->
      <div class="mb-6">
        <div class="flex items-center justify-between mb-3">
          <div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100">
              {{ t('notifications.pushNotifications') }}
            </h4>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('notifications.pushNotificationsDesc') }}
            </p>
          </div>
          <button
            @click="settings.ntfyEnabled = !settings.ntfyEnabled"
            :class="[
              'relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
              settings.ntfyEnabled ? 'bg-blue-600' : 'bg-gray-200 dark:bg-gray-700'
            ]"
          >
            <span
              :class="[
                'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                settings.ntfyEnabled ? 'translate-x-5' : 'translate-x-0'
              ]"
            />
          </button>
        </div>

        <!-- NTFY Configuration -->
        <div v-if="settings.ntfyEnabled" class="space-y-4 p-4 bg-gray-50 dark:bg-gray-900 rounded-lg">
          <div>
            <label for="ntfy-server" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('notifications.ntfyServer') }}
            </label>
            <input
              id="ntfy-server"
              v-model="settings.ntfyServer"
              type="url"
              :placeholder="t('notifications.serverPlaceholder')"
              readonly
              class="block w-full rounded-md border-gray-300 dark:border-gray-600 bg-gray-100 dark:bg-gray-600 text-gray-900 dark:text-gray-100 shadow-sm sm:text-sm cursor-not-allowed"
            />
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('notifications.ntfyServerReadonly') }}
            </p>
          </div>

          <div>
            <label for="ntfy-topic" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('notifications.personalTopic') }}
            </label>
            <div class="flex space-x-2">
              <input
                id="ntfy-topic"
                v-model="settings.ntfyTopic"
                type="text"
                :placeholder="t('notifications.topicPlaceholder')"
                class="flex-1 block rounded-md border-gray-300 dark:border-gray-600 bg-gray-100 dark:bg-gray-600 text-gray-900 dark:text-gray-100 shadow-sm sm:text-sm cursor-not-allowed"
                readonly
              />
              <button
                @click="generateNewTopic"
                :disabled="isUpdatingTopic || !notificationConfig"
                class="px-3 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
                :title="t('notifications.generateNewTopic')"
              >
                <LoadingSpinner v-if="isUpdatingTopic" size="small" class="h-4 w-4" />
                <ArrowPathIcon v-else class="h-4 w-4" />
              </button>
            </div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('notifications.topicDescription') }}
            </p>
          </div>

          <!-- NTFY QR Code -->
          <div class="flex items-center justify-between p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
            <div class="flex-1">
              <p class="text-sm font-medium text-blue-800 dark:text-blue-200">
                {{ t('notifications.configureNtfy') }}
              </p>
              <p class="text-xs text-blue-600 dark:text-blue-300 mt-1">
                {{ t('notifications.configureNtfyDesc') }}
              </p>
            </div>
            <button
              @click="showQRCode = !showQRCode"
              class="px-3 py-1 text-xs font-medium text-blue-600 bg-white dark:bg-blue-900 rounded-md hover:bg-blue-50 dark:hover:bg-blue-800 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors duration-200"
            >
              {{ showQRCode ? t('notifications.hideQR') : t('notifications.showQR') }}
            </button>
          </div>

          <!-- QR Code Display -->
          <div v-if="showQRCode" class="flex justify-center p-4 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg">
            <div class="text-center">
              <div class="w-48 h-48 bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-center mb-3">
                <QrCodeIcon class="h-24 w-24 text-gray-400" />
              </div>
              <p class="text-sm text-gray-600 dark:text-gray-400">
                Codice QR per: {{ ntfySubscriptionUrl }}
              </p>
              <button
                @click="copyTopicUrl"
                class="mt-2 px-3 py-1 text-xs font-medium text-blue-600 bg-blue-50 rounded-md hover:bg-blue-100 dark:bg-blue-900 dark:text-blue-300 dark:hover:bg-blue-800 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors duration-200"
              >
                {{ copied ? t('notifications.copied') : t('notifications.copyUrl') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Telegram Settings -->
      <div class="mb-6">
        <div class="flex items-center justify-between mb-3">
          <div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100">
              {{ t('telegram.title') }}
            </h4>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('telegram.description') }}
            </p>
          </div>
        </div>

        <!-- Telegram Setup Component -->
        <TelegramSetup
          :bot-username="telegramBotUsername"
          @registered="onTelegramRegistered"
          @unlinked="onTelegramUnlinked"
        />
      </div>

      <!-- Test Notifications -->
      <div class="mb-6">
        <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100 mb-3">
          {{ t('notifications.testNotifications') }}
        </h4>
        <div class="flex space-x-3">
          <button
            @click="testBrowserNotification"
            :disabled="!settings.enabled || browserPermission !== 'granted' || !!isTesting"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
          >
            <LoadingSpinner v-if="isTesting === 'browser'" size="small" class="mr-2" />
            {{ t('notifications.testBrowser') }}
          </button>
          <button
            @click="testNTFYNotification"
            :disabled="!canTestNTFY || !!isTesting"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
          >
            <LoadingSpinner v-if="isTesting === 'ntfy'" size="small" class="mr-2" />
            {{ t('notifications.testNtfy') }}
          </button>
        </div>
      </div>

      <!-- Save Settings -->
      <div class="flex justify-end space-x-3 pt-6 border-t border-gray-200 dark:border-gray-700">
        <button
          @click="resetSettings"
          class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors duration-200"
        >
          {{ t('notifications.reset') }}
        </button>
        <button
          @click="saveSettings"
          :disabled="isSaving"
          class="px-4 py-2 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
        >
          <LoadingSpinner v-if="isSaving" size="small" class="mr-2" />
          {{ t('notifications.saveSettings') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuth } from '../../composables/useAuth'
import { useCustomToast } from '../../composables/useCustomToast'
import { useNotificationPermissions } from '../../composables/useNotificationPermissions'
import LoadingSpinner from '../Common/LoadingSpinner.vue'
import TelegramSetup from '../Telegram/TelegramSetup.vue'
import {
  BellIcon,
  QrCodeIcon,
  ArrowPathIcon
} from '@heroicons/vue/24/outline'

// i18n
const { t } = useI18n()

interface NotificationSettings {
  enabled: boolean
  ntfyEnabled: boolean
  ntfyServer: string
  ntfyTopic: string
  browserEnabled: boolean
}

interface NotificationConfig {
  ntfyServerUrl: string
  ntfyTopicPrefix: string
  enabledProviders: string[]
  supportsPush: boolean
  supportsEmail: boolean
}

const { user } = useAuth()
const { showSuccess, showError } = useCustomToast()
const {
  permission: browserPermission,
  requestPermission,
  sendBrowserNotification
} = useNotificationPermissions()

// Local state
const settings = ref<NotificationSettings>({
  enabled: true,
  ntfyEnabled: false,
  ntfyServer: '',
  ntfyTopic: '',
  browserEnabled: true
})

const notificationConfig = ref<NotificationConfig | null>(null)
const subscriptionUrl = ref<string>('')
const showQRCode = ref(false)
const copied = ref(false)
const isSaving = ref(false)
const isTesting = ref<'browser' | 'ntfy' | null>(null)
const isRequestingPermissions = ref(false)
const isLoadingConfig = ref(false)
const isUpdatingTopic = ref(false)
const telegramBotUsername = ref<string>('')

// Computed properties
const ntfySubscriptionUrl = computed(() => {
  return subscriptionUrl.value || 'Non disponibile'
})

const canTestNTFY = computed(() => {
  return settings.value.enabled &&
         settings.value.ntfyEnabled &&
         notificationConfig.value?.supportsPush &&
         settings.value.ntfyTopic
})

// API Methods
const fetchNotificationConfig = async () => {
  isLoadingConfig.value = true
  try {
    const response = await fetch('/api/notifications/config', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      }
    })

    if (response.ok) {
      notificationConfig.value = await response.json()
      settings.value.ntfyServer = notificationConfig.value?.ntfyServerUrl || ''
      settings.value.ntfyEnabled = notificationConfig.value?.supportsPush || false
    }
  } catch (error) {
    console.error('Error fetching notification config:', error)
    showError(t('notifications.errorLoadingConfig'))
  } finally {
    isLoadingConfig.value = false
  }
}

const fetchSubscriptionUrl = async () => {
  try {
    const response = await fetch('/api/notifications/ntfy/subscription-url', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      }
    })

    const data = await response.json()
    if (data.success) {
      subscriptionUrl.value = data.subscriptionUrl
      settings.value.ntfyTopic = data.topic
    }
  } catch (error) {
    console.error('Error fetching subscription URL:', error)
  }
}

const updateNtfyTopic = async (newTopic: string) => {
  isUpdatingTopic.value = true
  try {
    const response = await fetch('/api/notifications/ntfy/topic', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: JSON.stringify({ topic: newTopic })
    })

    const data = await response.json()
    if (data.success) {
      settings.value.ntfyTopic = data.topic
      await fetchSubscriptionUrl() // Refresh subscription URL
      showSuccess(t('notifications.topicUpdatedSuccess'))
      return true
    } else {
      showError(data.error || t('notifications.errorUpdatingTopic'))
      return false
    }
  } catch (error) {
    console.error('Error updating NTFY topic:', error)
    showError(t('notifications.errorUpdatingTopic'))
    return false
  } finally {
    isUpdatingTopic.value = false
  }
}

// Methods
const generateNewTopic = async () => {
  if (!notificationConfig.value) return

  const userId = user.value?.id
  if (!userId) return

  const randomSuffix = Math.random().toString(36).substr(2, 10)
  const newTopic = `${notificationConfig.value.ntfyTopicPrefix}${userId}-${randomSuffix}`

  await updateNtfyTopic(newTopic)
}

const getBrowserPermissionLabel = (permission: NotificationPermission): string => {
  switch (permission) {
    case 'granted': return t('notifications.permissionGranted')
    case 'denied': return t('notifications.permissionDenied')
    case 'default': return t('notifications.permissionDefault')
    default: return t('notifications.permissionDefault')
  }
}

const toggleNotifications = () => {
  settings.value.enabled = !settings.value.enabled
}

const requestBrowserPermissions = async () => {
  isRequestingPermissions.value = true
  try {
    await requestPermission()
    if (browserPermission.value === 'granted') {
      showSuccess(t('notifications.permissionGrantedSuccess'))
    } else {
      showError(t('notifications.permissionDeniedError'))
    }
  } catch (error) {
    showError(t('notifications.errorRequestingPermission'))
  } finally {
    isRequestingPermissions.value = false
  }
}

const testBrowserNotification = async () => {
  if (browserPermission.value !== 'granted') {
    showError(t('notifications.permissionDeniedError'))
    return
  }

  isTesting.value = 'browser'
  try {
    await sendBrowserNotification(
      t('notifications.testBrowserNotificationTitle'),
      {
        body: t('notifications.testBrowserNotificationBody'),
        icon: '/favicon.ico',
        tag: 'test-notification'
      }
    )
    showSuccess(t('notifications.browserNotificationSent'))
  } catch (error) {
    showError(t('notifications.errorSendingBrowserNotification'))
  } finally {
    isTesting.value = null
  }
}

const testNTFYNotification = async () => {
  if (!canTestNTFY.value) {
    showError(t('notifications.ntfyConfigNotAvailable'))
    return
  }

  isTesting.value = 'ntfy'
  try {
    const response = await fetch('/api/notifications/test', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: JSON.stringify({
        type: 'PUSH'
        // No custom message - let backend use localized default
      })
    })

    const data = await response.json()
    if (data.success) {
      showSuccess(t('notifications.ntfyNotificationSentSuccess'))
    } else {
      showError(data.error || t('notifications.errorSendingNtfyNotification'))
    }
  } catch (error) {
    console.error('NTFY test error:', error)
    showError(t('notifications.errorSendingNtfyNotification'))
  } finally {
    isTesting.value = null
  }
}

const copyTopicUrl = async () => {
  try {
    await navigator.clipboard.writeText(ntfySubscriptionUrl.value)
    copied.value = true
    showSuccess(t('notifications.urlCopiedSuccess'))
    setTimeout(() => {
      copied.value = false
    }, 3000)
  } catch (error) {
    showError(t('notifications.errorCopyingUrl'))
  }
}

const saveSettings = async () => {
  isSaving.value = true
  try {
    // Save browser notification preferences to localStorage
    const browserSettings = {
      enabled: settings.value.enabled,
      browserEnabled: settings.value.browserEnabled
    }
    localStorage.setItem('notificationSettings', JSON.stringify(browserSettings))
    showSuccess(t('notifications.settingsSavedSuccess'))
  } catch (error) {
    showError(t('notifications.errorSavingSettings'))
  } finally {
    isSaving.value = false
  }
}

const loadSettings = async () => {
  try {
    // Load browser settings from localStorage
    const saved = localStorage.getItem('notificationSettings')
    if (saved) {
      const parsedSettings = JSON.parse(saved)
      settings.value.enabled = parsedSettings.enabled ?? true
      settings.value.browserEnabled = parsedSettings.browserEnabled ?? true
    }

    // Load notification configuration and user's NTFY topic from backend
    await fetchNotificationConfig()
    await fetchSubscriptionUrl()
  } catch (error) {
    console.error('Error loading notification settings:', error)
  }
}

const resetSettings = async () => {
  settings.value = {
    enabled: true,
    ntfyEnabled: false,
    ntfyServer: '',
    ntfyTopic: '',
    browserEnabled: true
  }

  localStorage.removeItem('notificationSettings')
  await loadSettings()
  showSuccess(t('notifications.settingsReset'))
}

const fetchTelegramBotInfo = async () => {
  try {
    const response = await fetch('/api/telegram/bot-info', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      }
    })

    if (response.ok) {
      const data = await response.json()
      telegramBotUsername.value = data.botUsername || ''
    }
  } catch (error) {
    console.error('Error fetching Telegram bot info:', error)
  }
}

const onTelegramRegistered = () => {
  showSuccess(t('telegram.registrationSuccess'))
}

const onTelegramUnlinked = () => {
  showSuccess(t('telegram.unlinkSuccess'))
}

// Lifecycle
onMounted(async () => {
  await loadSettings()
  await fetchTelegramBotInfo()
})
</script>

<style scoped>
.notification-settings {
  @apply w-full max-w-2xl mx-auto;
}

/* Custom toggle switch styling */
.toggle-switch {
  transition: all 0.2s ease-in-out;
}

/* QR Code placeholder styling */
.qr-placeholder {
  background: linear-gradient(45deg, #f3f4f6 25%, transparent 25%), 
              linear-gradient(-45deg, #f3f4f6 25%, transparent 25%), 
              linear-gradient(45deg, transparent 75%, #f3f4f6 75%), 
              linear-gradient(-45deg, transparent 75%, #f3f4f6 75%);
  background-size: 10px 10px;
  background-position: 0 0, 0 5px, 5px -5px, -5px 0px;
}

.dark .qr-placeholder {
  background: linear-gradient(45deg, #374151 25%, transparent 25%), 
              linear-gradient(-45deg, #374151 25%, transparent 25%), 
              linear-gradient(45deg, transparent 75%, #374151 75%), 
              linear-gradient(-45deg, transparent 75%, #374151 75%);
  background-size: 10px 10px;
  background-position: 0 0, 0 5px, 5px -5px, -5px 0px;
}
</style>