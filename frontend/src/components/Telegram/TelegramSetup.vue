<template>
  <div class="telegram-setup bg-gray-50 dark:bg-gray-900 rounded-lg p-4">
    <!-- Registration Status -->
    <div v-if="isRegistered" class="mb-4">
      <div class="flex items-center space-x-2 text-green-600 dark:text-green-400">
        <CheckCircleIcon class="h-5 w-5" />
        <span class="text-sm font-medium">{{ t('telegram.registered') }}</span>
      </div>
      <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
        {{ t('telegram.registeredDesc') }}
      </p>
    </div>

    <!-- Registration Guide (when not registered) -->
    <div v-else>
      <!-- Step 1: Generate Token -->
      <div class="mb-4">
        <div class="flex items-start space-x-2">
          <div class="flex-shrink-0 w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 flex items-center justify-center text-xs font-semibold">
            1
          </div>
          <div class="flex-1">
            <h5 class="text-sm font-medium text-gray-900 dark:text-gray-100">
              {{ t('telegram.step1Title') }}
            </h5>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('telegram.step1Desc') }}
            </p>
            <button
              @click="generateToken"
              :disabled="isGenerating || !!registrationToken"
              class="mt-2 px-3 py-1.5 text-xs font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
            >
              <LoadingSpinner v-if="isGenerating" size="small" class="mr-1" />
              {{ registrationToken ? t('telegram.tokenGenerated') : t('telegram.generateToken') }}
            </button>
          </div>
        </div>
      </div>

      <!-- Step 2: Token Display -->
      <div v-if="registrationToken" class="mb-4">
        <div class="flex items-start space-x-2">
          <div class="flex-shrink-0 w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 flex items-center justify-center text-xs font-semibold">
            2
          </div>
          <div class="flex-1">
            <h5 class="text-sm font-medium text-gray-900 dark:text-gray-100">
              {{ t('telegram.step2Title') }}
            </h5>
            <div class="mt-2 p-3 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-md">
              <div class="flex items-center justify-between">
                <code class="text-sm font-mono text-blue-600 dark:text-blue-400">{{ registrationToken }}</code>
                <button
                  @click="copyToken"
                  class="ml-2 p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 focus:outline-none"
                  :title="t('telegram.copyToken')"
                >
                  <ClipboardDocumentIcon v-if="!tokenCopied" class="h-4 w-4" />
                  <CheckIcon v-else class="h-4 w-4 text-green-500" />
                </button>
              </div>
            </div>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('telegram.tokenExpiry') }}
            </p>
          </div>
        </div>
      </div>

      <!-- Step 3: Open Telegram -->
      <div v-if="registrationToken" class="mb-4">
        <div class="flex items-start space-x-2">
          <div class="flex-shrink-0 w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 flex items-center justify-center text-xs font-semibold">
            3
          </div>
          <div class="flex-1">
            <h5 class="text-sm font-medium text-gray-900 dark:text-gray-100">
              {{ t('telegram.step3Title') }}
            </h5>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('telegram.step3Desc') }}
            </p>
            <div class="mt-2 p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
              <code class="text-sm font-mono text-blue-600 dark:text-blue-400">/start {{ registrationToken }}</code>
            </div>
            <a
              :href="telegramBotUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="mt-2 inline-flex items-center px-3 py-1.5 text-xs font-medium text-blue-600 bg-blue-50 rounded-md hover:bg-blue-100 dark:bg-blue-900 dark:text-blue-300 dark:hover:bg-blue-800 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors duration-200"
            >
              {{ t('telegram.openBot') }}
              <ArrowTopRightOnSquareIcon class="ml-1 h-3 w-3" />
            </a>
          </div>
        </div>
      </div>

      <!-- Auto-refresh status check -->
      <div v-if="registrationToken && isCheckingStatus" class="flex items-center space-x-2 text-sm text-gray-500 dark:text-gray-400">
        <LoadingSpinner size="small" />
        <span>{{ t('telegram.checkingStatus') }}</span>
      </div>
    </div>

    <!-- Unlink Button (when registered) -->
    <div v-if="isRegistered" class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
      <button
        @click="unlinkTelegram"
        :disabled="isUnlinking"
        class="px-3 py-1.5 text-xs font-medium text-red-600 bg-red-50 rounded-md hover:bg-red-100 dark:bg-red-900/20 dark:text-red-400 dark:hover:bg-red-900/30 focus:outline-none focus:ring-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
      >
        <LoadingSpinner v-if="isUnlinking" size="small" class="mr-1" />
        {{ t('telegram.unlink') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCustomToast } from '../../composables/useCustomToast'
import LoadingSpinner from '../Common/LoadingSpinner.vue'
import {
  CheckCircleIcon,
  ClipboardDocumentIcon,
  CheckIcon,
  ArrowTopRightOnSquareIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()
const { showSuccess, showError } = useCustomToast()

// Props
const props = defineProps<{
  botUsername?: string
}>()

// Emits
const emit = defineEmits<{
  (e: 'registered'): void
  (e: 'unlinked'): void
}>()

// State
const isRegistered = ref(false)
const registrationToken = ref<string | null>(null)
const tokenCopied = ref(false)
const isGenerating = ref(false)
const isCheckingStatus = ref(false)
const isUnlinking = ref(false)
let statusCheckInterval: number | null = null

// Computed
const telegramBotUrl = computed(() => {
  const username = props.botUsername || 'your_bot'
  return `https://t.me/${username}?start=${registrationToken.value || ''}`
})

// Methods
const checkRegistrationStatus = async () => {
  try {
    const response = await fetch('/api/telegram/status', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      }
    })

    if (response.ok) {
      const data = await response.json()
      isRegistered.value = data.registered

      if (data.registered && registrationToken.value) {
        // User just registered, clear token and stop checking
        registrationToken.value = null
        stopStatusCheck()
        showSuccess(t('telegram.registrationSuccess'))
        emit('registered')
      }
    }
  } catch (error) {
    console.error('Error checking Telegram status:', error)
  }
}

const generateToken = async () => {
  isGenerating.value = true
  try {
    const response = await fetch('/api/telegram/generate-token', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      }
    })

    if (response.ok) {
      const data = await response.json()
      registrationToken.value = data.token
      showSuccess(t('telegram.tokenGeneratedSuccess'))

      // Start checking status every 3 seconds
      startStatusCheck()
    } else {
      const error = await response.json()
      showError(error.message || t('telegram.errorGeneratingToken'))
    }
  } catch (error) {
    console.error('Error generating token:', error)
    showError(t('telegram.errorGeneratingToken'))
  } finally {
    isGenerating.value = false
  }
}

const copyToken = async () => {
  if (!registrationToken.value) return

  try {
    await navigator.clipboard.writeText(registrationToken.value)
    tokenCopied.value = true
    showSuccess(t('telegram.tokenCopied'))
    setTimeout(() => {
      tokenCopied.value = false
    }, 3000)
  } catch (error) {
    showError(t('telegram.errorCopyingToken'))
  }
}

const unlinkTelegram = async () => {
  if (!confirm(t('telegram.confirmUnlink'))) return

  isUnlinking.value = true
  try {
    const response = await fetch('/api/telegram/unlink', {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      }
    })

    if (response.ok) {
      isRegistered.value = false
      showSuccess(t('telegram.unlinkSuccess'))
      emit('unlinked')
    } else {
      const error = await response.json()
      showError(error.message || t('telegram.errorUnlinking'))
    }
  } catch (error) {
    console.error('Error unlinking Telegram:', error)
    showError(t('telegram.errorUnlinking'))
  } finally {
    isUnlinking.value = false
  }
}

const startStatusCheck = () => {
  if (statusCheckInterval) return

  isCheckingStatus.value = true
  statusCheckInterval = window.setInterval(() => {
    checkRegistrationStatus()
  }, 3000) as unknown as number
}

const stopStatusCheck = () => {
  if (statusCheckInterval) {
    clearInterval(statusCheckInterval)
    statusCheckInterval = null
  }
  isCheckingStatus.value = false
}

// Lifecycle
onMounted(async () => {
  await checkRegistrationStatus()
})

onUnmounted(() => {
  stopStatusCheck()
})
</script>

<style scoped>
.telegram-setup {
  max-width: 100%;
}
</style>
