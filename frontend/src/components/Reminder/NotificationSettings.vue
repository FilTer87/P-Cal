<template>
  <div class="notification-settings">
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100">
            Impostazioni Notifiche
          </h3>
          <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Configura come ricevere i promemoria per le tue attività
          </p>
        </div>
        <BellIcon class="h-6 w-6 text-gray-400" />
      </div>

      <!-- Global Notifications Toggle -->
      <div class="mb-6">
        <div class="flex items-center justify-between">
          <div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100">
              Abilita Notifiche
            </h4>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Attiva o disattiva tutte le notifiche per i promemoria
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
              Notifiche Browser
            </h4>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Mostra notifiche nel browser quando è aperto
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
              Richiedi Permesso
            </button>
          </div>
        </div>
      </div>

      <!-- NTFY Settings -->
      <div class="mb-6">
        <div class="flex items-center justify-between mb-3">
          <div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100">
              Notifiche Push (NTFY)
            </h4>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Ricevi notifiche anche quando il browser è chiuso
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
              Server NTFY
            </label>
            <input
              id="ntfy-server"
              v-model="settings.ntfyServer"
              type="url"
              placeholder="https://ntfy.sh"
              class="block w-full rounded-md border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              URL del server NTFY (predefinito: ntfy.sh)
            </p>
          </div>

          <div>
            <label for="ntfy-topic" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Topic Personale
            </label>
            <div class="flex space-x-2">
              <input
                id="ntfy-topic"
                v-model="settings.ntfyTopic"
                type="text"
                :placeholder="defaultTopic"
                class="flex-1 block rounded-md border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                readonly
              />
              <button
                @click="generateNewTopic"
                class="px-3 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
                title="Genera nuovo topic"
              >
                <ArrowPathIcon class="h-4 w-4" />
              </button>
            </div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Topic univoco per le tue notifiche (generato automaticamente)
            </p>
          </div>

          <!-- NTFY QR Code -->
          <div class="flex items-center justify-between p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
            <div class="flex-1">
              <p class="text-sm font-medium text-blue-800 dark:text-blue-200">
                Configura l'app NTFY
              </p>
              <p class="text-xs text-blue-600 dark:text-blue-300 mt-1">
                Scannerizza il codice QR o aggiungi manualmente il topic
              </p>
            </div>
            <button
              @click="showQRCode = !showQRCode"
              class="px-3 py-1 text-xs font-medium text-blue-600 bg-white dark:bg-blue-900 rounded-md hover:bg-blue-50 dark:hover:bg-blue-800 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors duration-200"
            >
              {{ showQRCode ? 'Nascondi QR' : 'Mostra QR' }}
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
                {{ copied ? 'Copiato!' : 'Copia URL' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Test Notifications -->
      <div class="mb-6">
        <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100 mb-3">
          Test Notifiche
        </h4>
        <div class="flex space-x-3">
          <button
            @click="testBrowserNotification"
            :disabled="!settings.enabled || browserPermission !== 'granted' || isTesting"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
          >
            <LoadingSpinner v-if="isTesting === 'browser'" size="small" class="mr-2" />
            Test Browser
          </button>
          <button
            @click="testNTFYNotification"
            :disabled="!settings.enabled || !settings.ntfyEnabled || !settings.ntfyServer || isTesting"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
          >
            <LoadingSpinner v-if="isTesting === 'ntfy'" size="small" class="mr-2" />
            Test NTFY
          </button>
        </div>
      </div>

      <!-- Save Settings -->
      <div class="flex justify-end space-x-3 pt-6 border-t border-gray-200 dark:border-gray-700">
        <button
          @click="resetSettings"
          class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors duration-200"
        >
          Reset
        </button>
        <button
          @click="saveSettings"
          :disabled="isSaving"
          class="px-4 py-2 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
        >
          <LoadingSpinner v-if="isSaving" size="small" class="mr-2" />
          Salva Impostazioni
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '../../composables/useAuth'
import { useCustomToast } from '../../composables/useCustomToast'
import { useNTFY } from '../../composables/useNTFY'
import { useNotificationPermissions } from '../../composables/useNotificationPermissions'
import LoadingSpinner from '../Common/LoadingSpinner.vue'
import {
  BellIcon,
  QrCodeIcon,
  ArrowPathIcon
} from '@heroicons/vue/24/outline'

interface NotificationSettings {
  enabled: boolean
  ntfyEnabled: boolean
  ntfyServer: string
  ntfyTopic: string
  browserEnabled: boolean
}

const { user } = useAuth()
const { showSuccess, showError } = useCustomToast()
const { testConnection, sendNotification } = useNTFY()
const { 
  permission: browserPermission, 
  requestPermission, 
  sendBrowserNotification 
} = useNotificationPermissions()

// Local state
const settings = ref<NotificationSettings>({
  enabled: true,
  ntfyEnabled: false,
  ntfyServer: 'https://ntfy.sh',
  ntfyTopic: '',
  browserEnabled: true
})

const showQRCode = ref(false)
const copied = ref(false)
const isSaving = ref(false)
const isTesting = ref<'browser' | 'ntfy' | null>(null)
const isRequestingPermissions = ref(false)

// Computed properties
const defaultTopic = computed(() => {
  return user.value ? `calendar-user-${user.value.id}-${generateRandomId()}` : 'calendar-user'
})

const ntfySubscriptionUrl = computed(() => {
  const topic = settings.value.ntfyTopic || defaultTopic.value
  return `${settings.value.ntfyServer}/${topic}`
})

// Methods
const generateRandomId = (): string => {
  return Math.random().toString(36).substr(2, 9)
}

const generateNewTopic = () => {
  settings.value.ntfyTopic = defaultTopic.value
}

const getBrowserPermissionLabel = (permission: NotificationPermission): string => {
  switch (permission) {
    case 'granted': return 'Autorizzato'
    case 'denied': return 'Negato'
    case 'default': return 'Non richiesto'
    default: return 'Sconosciuto'
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
      showSuccess('Permessi browser autorizzati!')
    } else {
      showError('Permessi browser non autorizzati')
    }
  } catch (error) {
    showError('Errore nella richiesta permessi browser')
  } finally {
    isRequestingPermissions.value = false
  }
}

const testBrowserNotification = async () => {
  if (browserPermission.value !== 'granted') {
    showError('Permessi browser non autorizzati')
    return
  }

  isTesting.value = 'browser'
  try {
    await sendBrowserNotification(
      'Test Notifica Browser',
      'Questo è un test delle notifiche browser per PrivateCal',
      {
        icon: '/favicon.ico',
        tag: 'test-notification'
      }
    )
    showSuccess('Notifica browser inviata!')
  } catch (error) {
    showError('Errore nell\'invio della notifica browser')
  } finally {
    isTesting.value = null
  }
}

const testNTFYNotification = async () => {
  if (!settings.value.ntfyServer || !settings.value.ntfyTopic) {
    showError('Configurazione NTFY incompleta')
    return
  }

  isTesting.value = 'ntfy'
  try {
    // Test connection first
    const isConnected = await testConnection(settings.value.ntfyServer)
    if (!isConnected) {
      throw new Error('Connessione al server NTFY fallita')
    }

    // Send test notification
    await sendNotification({
      server: settings.value.ntfyServer,
      topic: settings.value.ntfyTopic,
      title: 'Test Notifica NTFY',
      message: 'Questo è un test delle notifiche NTFY per PrivateCal',
      tags: ['test', '🧪'],
      priority: 3
    })

    showSuccess('Notifica NTFY inviata con successo!')
  } catch (error) {
    console.error('NTFY test error:', error)
    showError('Errore nell\'invio della notifica NTFY: ' + (error as Error).message)
  } finally {
    isTesting.value = null
  }
}

const copyTopicUrl = async () => {
  try {
    await navigator.clipboard.writeText(ntfySubscriptionUrl.value)
    copied.value = true
    showSuccess('URL copiato negli appunti!')
    setTimeout(() => {
      copied.value = false
    }, 3000)
  } catch (error) {
    showError('Errore nella copia dell\'URL')
  }
}

const saveSettings = async () => {
  isSaving.value = true
  try {
    // Save to localStorage for now
    localStorage.setItem('notificationSettings', JSON.stringify(settings.value))
    showSuccess('Impostazioni salvate con successo!')
  } catch (error) {
    showError('Errore nel salvataggio delle impostazioni')
  } finally {
    isSaving.value = false
  }
}

const loadSettings = () => {
  try {
    const saved = localStorage.getItem('notificationSettings')
    if (saved) {
      const parsedSettings = JSON.parse(saved)
      settings.value = { ...settings.value, ...parsedSettings }
    }
    
    // Generate topic if not present
    if (!settings.value.ntfyTopic) {
      settings.value.ntfyTopic = defaultTopic.value
    }
  } catch (error) {
    console.error('Error loading notification settings:', error)
  }
}

const resetSettings = () => {
  settings.value = {
    enabled: true,
    ntfyEnabled: false,
    ntfyServer: 'https://ntfy.sh',
    ntfyTopic: defaultTopic.value,
    browserEnabled: true
  }
  showSuccess('Impostazioni ripristinate')
}

// Lifecycle
onMounted(() => {
  loadSettings()
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