<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition ease-in duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="showModal"
        class="fixed inset-0 z-50 overflow-y-auto"
        role="dialog"
        aria-modal="true"
        aria-labelledby="session-timeout-title"
        aria-describedby="session-timeout-description"
      >
        <!-- Backdrop -->
        <div class="fixed inset-0 bg-black bg-opacity-50 backdrop-blur-sm"></div>

        <!-- Modal -->
        <div class="flex items-center justify-center min-h-screen p-4">
          <Transition
            enter-active-class="transition ease-out duration-200"
            enter-from-class="transform opacity-0 scale-95"
            enter-to-class="transform opacity-100 scale-100"
            leave-active-class="transition ease-in duration-150"
            leave-from-class="transform opacity-100 scale-100"
            leave-to-class="transform opacity-0 scale-95"
          >
            <div
              v-if="showModal"
              class="relative bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full mx-auto"
            >
              <!-- Header -->
              <div class="px-6 pt-6">
                <div class="flex items-center">
                  <!-- Warning Icon -->
                  <div class="flex-shrink-0">
                    <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-yellow-100 dark:bg-yellow-900/20">
                      <svg class="h-6 w-6 text-yellow-600 dark:text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-2.694-.833-3.464 0L3.35 16.5c-.77.833.192 2.5 1.732 2.5z" />
                      </svg>
                    </div>
                  </div>
                  
                  <!-- Title -->
                  <div class="ml-4">
                    <h3 id="session-timeout-title" class="text-lg font-semibold text-gray-900 dark:text-white">
                      Sessione in Scadenza
                    </h3>
                  </div>
                </div>
              </div>

              <!-- Content -->
              <div class="px-6 py-4">
                <div class="space-y-4">
                  <!-- Description -->
                  <p id="session-timeout-description" class="text-sm text-gray-600 dark:text-gray-400">
                    La tua sessione scadrà tra <strong class="text-gray-900 dark:text-white">{{ formatTime(timeRemaining) }}</strong>.
                    Vuoi estendere la sessione?
                  </p>

                  <!-- Progress Bar -->
                  <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                    <div 
                      :class="[
                        'h-2 rounded-full transition-all duration-1000 ease-linear',
                        timeRemaining > warningThreshold * 0.5 
                          ? 'bg-yellow-500' 
                          : timeRemaining > warningThreshold * 0.2
                            ? 'bg-orange-500'
                            : 'bg-red-500'
                      ]"
                      :style="{ width: `${progressPercentage}%` }"
                    ></div>
                  </div>

                  <!-- Last Activity Info -->
                  <div v-if="lastActivity" class="text-xs text-gray-500 dark:text-gray-400">
                    Ultima attività: {{ formatLastActivity(lastActivity) }}
                  </div>

                  <!-- Auto-logout Warning -->
                  <div v-if="timeRemaining <= autoLogoutWarning" class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-3">
                    <div class="flex">
                      <svg class="h-5 w-5 text-red-400 mr-2 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                      </svg>
                      <div class="text-sm">
                        <p class="font-medium text-red-800 dark:text-red-200">
                          Disconnessione automatica imminente!
                        </p>
                        <p class="text-red-700 dark:text-red-300 mt-1">
                          Sarai disconnesso automaticamente in {{ Math.ceil(timeRemaining / 1000) }} secondi.
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Actions -->
              <div class="px-6 pb-6">
                <div class="flex space-x-3">
                  <button
                    @click="extendSession"
                    :disabled="isExtending"
                    class="flex-1 inline-flex justify-center items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                    <LoadingSpinner v-if="isExtending" class="w-4 h-4 mr-2" />
                    {{ isExtending ? 'Estendendo...' : 'Estendi Sessione' }}
                  </button>

                  <button
                    @click="logoutNow"
                    :disabled="isLoggingOut"
                    class="flex-1 inline-flex justify-center items-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-sm font-medium rounded-md text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                    <LoadingSpinner v-if="isLoggingOut" class="w-4 h-4 mr-2" />
                    {{ isLoggingOut ? 'Disconnettendo...' : 'Disconnetti' }}
                  </button>
                </div>

                <!-- Keyboard Shortcuts Info -->
                <div class="mt-3 text-xs text-gray-500 dark:text-gray-400 text-center">
                  <kbd class="px-1 py-0.5 text-xs bg-gray-200 dark:bg-gray-700 rounded">Enter</kbd> per estendere · 
                  <kbd class="px-1 py-0.5 text-xs bg-gray-200 dark:bg-gray-700 rounded">Esc</kbd> per disconnettere
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useNotifications } from '@/composables/useNotifications'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'

// Props
interface Props {
  warningTime?: number // milliseconds before expiration to show warning
  autoLogoutTime?: number // milliseconds before expiration to auto-logout
  extendTime?: number // milliseconds to extend session
  checkInterval?: number // milliseconds between checks
}

const props = withDefaults(defineProps<Props>(), {
  warningTime: 5 * 60 * 1000, // 5 minutes
  autoLogoutTime: 30 * 1000, // 30 seconds
  extendTime: 30 * 60 * 1000, // 30 minutes
  checkInterval: 10 * 1000 // 10 seconds
})

// Composables
const { 
  isAuthenticated, 
  isTokenExpired,
  accessToken,
  extendSession,
  logout
} = useAuth()
const { showSuccess, showWarning } = useNotifications()

// State
const showModal = ref(false)
const timeRemaining = ref(0)
const isExtending = ref(false)
const isLoggingOut = ref(false)
const lastActivity = ref<Date | null>(null)
const sessionExtended = ref(false)

// Timers
let checkTimer: NodeJS.Timeout | null = null
let countdownTimer: NodeJS.Timeout | null = null
let autoLogoutTimer: NodeJS.Timeout | null = null

// Computed
const warningThreshold = computed(() => props.warningTime)
const autoLogoutWarning = computed(() => props.autoLogoutTime)

const progressPercentage = computed(() => {
  if (!warningThreshold.value || timeRemaining.value <= 0) return 0
  return Math.max(0, Math.min(100, (timeRemaining.value / warningThreshold.value) * 100))
})

// Token expiration tracking
const getTokenExpirationTime = (): number | null => {
  if (!accessToken.value || !isAuthenticated.value) return null
  
  try {
    // Decode JWT token to get expiration time
    const tokenParts = accessToken.value.split('.')
    if (tokenParts.length !== 3) return null
    
    const payload = JSON.parse(atob(tokenParts[1]))
    return payload.exp ? payload.exp * 1000 : null // Convert to milliseconds
  } catch (error) {
    console.error('Failed to decode token:', error)
    return null
  }
}

// Time formatting
const formatTime = (milliseconds: number): string => {
  const seconds = Math.ceil(milliseconds / 1000)
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60

  if (minutes > 0) {
    return `${minutes} minuto${minutes > 1 ? 'i' : ''} e ${remainingSeconds} secondo${remainingSeconds !== 1 ? 'i' : ''}`
  } else {
    return `${seconds} secondo${seconds !== 1 ? 'i' : ''}`
  }
}

const formatLastActivity = (date: Date): string => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  
  if (minutes === 0) {
    return 'meno di un minuto fa'
  } else if (minutes === 1) {
    return '1 minuto fa'
  } else {
    return `${minutes} minuti fa`
  }
}

// Activity tracking
const updateLastActivity = () => {
  lastActivity.value = new Date()
}

const trackActivity = () => {
  updateLastActivity()
}

// Session management
const checkSessionTimeout = () => {
  if (!isAuthenticated.value) {
    hideModal()
    return
  }

  const expirationTime = getTokenExpirationTime()
  if (!expirationTime) return

  const now = Date.now()
  const timeUntilExpiration = expirationTime - now

  // If already expired, logout immediately
  if (timeUntilExpiration <= 0 || isTokenExpired.value) {
    handleSessionExpired()
    return
  }

  // Show warning if within warning threshold
  if (timeUntilExpiration <= warningThreshold.value && !showModal.value) {
    showSessionWarning(timeUntilExpiration)
  } else if (showModal.value) {
    // Update time remaining if modal is already shown
    timeRemaining.value = timeUntilExpiration
  }

  // Auto-logout if within auto-logout threshold
  if (timeUntilExpiration <= autoLogoutWarning.value && !autoLogoutTimer) {
    startAutoLogoutTimer(timeUntilExpiration)
  }
}

const showSessionWarning = (remainingTime: number) => {
  timeRemaining.value = remainingTime
  showModal.value = true
  sessionExtended.value = false
  
  startCountdownTimer()
  
  // Focus management for accessibility
  setTimeout(() => {
    const extendButton = document.querySelector('[data-focus="extend-session"]') as HTMLElement
    extendButton?.focus()
  }, 100)
}

const hideModal = () => {
  showModal.value = false
  clearTimers()
}

const startCountdownTimer = () => {
  if (countdownTimer) clearInterval(countdownTimer)
  
  countdownTimer = setInterval(() => {
    timeRemaining.value -= 1000
    
    if (timeRemaining.value <= 0) {
      handleSessionExpired()
    }
  }, 1000)
}

const startAutoLogoutTimer = (remainingTime: number) => {
  if (autoLogoutTimer) clearTimeout(autoLogoutTimer)
  
  autoLogoutTimer = setTimeout(() => {
    handleSessionExpired()
  }, remainingTime)
}

const clearTimers = () => {
  if (checkTimer) {
    clearInterval(checkTimer)
    checkTimer = null
  }
  
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  
  if (autoLogoutTimer) {
    clearTimeout(autoLogoutTimer)
    autoLogoutTimer = null
  }
}

// Actions
const handleExtendSession = async () => {
  if (isExtending.value) return

  try {
    isExtending.value = true
    
    await extendSession()
    
    sessionExtended.value = true
    hideModal()
    
    showSuccess('Sessione estesa con successo!')
    
    // Restart monitoring
    startSessionMonitoring()
    
  } catch (error) {
    console.error('Failed to extend session:', error)
    showWarning('Impossibile estendere la sessione. Potresti dover riaccedere.')
  } finally {
    isExtending.value = false
  }
}

const handleLogoutNow = async () => {
  if (isLoggingOut.value) return

  try {
    isLoggingOut.value = true
    hideModal()
    
    await logout()
    
  } catch (error) {
    console.error('Logout failed:', error)
  } finally {
    isLoggingOut.value = false
  }
}

const handleSessionExpired = async () => {
  hideModal()
  
  try {
    await logout()
    showWarning('Sessione scaduta. Effettua nuovamente l\'accesso.')
  } catch (error) {
    console.error('Auto-logout failed:', error)
  }
}

// Public methods (exposed via defineExpose)
const extendSessionAction = () => {
  handleExtendSession()
}

const logoutNow = () => {
  handleLogoutNow()
}

// Keyboard shortcuts
const handleKeydown = (event: KeyboardEvent) => {
  if (!showModal.value) return

  switch (event.key) {
    case 'Enter':
      event.preventDefault()
      handleExtendSession()
      break
    case 'Escape':
      event.preventDefault()
      handleLogoutNow()
      break
  }
}

// Activity listeners
const activityEvents = [
  'mousedown',
  'mousemove',
  'keypress',
  'scroll',
  'touchstart',
  'click'
]

const addActivityListeners = () => {
  activityEvents.forEach(event => {
    document.addEventListener(event, trackActivity, true)
  })
}

const removeActivityListeners = () => {
  activityEvents.forEach(event => {
    document.removeEventListener(event, trackActivity, true)
  })
}

// Session monitoring
const startSessionMonitoring = () => {
  if (checkTimer) clearInterval(checkTimer)
  
  checkTimer = setInterval(checkSessionTimeout, props.checkInterval)
  
  // Initial check
  checkSessionTimeout()
}

const stopSessionMonitoring = () => {
  clearTimers()
}

// Watchers
watch(isAuthenticated, (authenticated) => {
  if (authenticated) {
    startSessionMonitoring()
    addActivityListeners()
    updateLastActivity()
  } else {
    stopSessionMonitoring()
    removeActivityListeners()
    hideModal()
  }
})

// Lifecycle
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  
  if (isAuthenticated.value) {
    startSessionMonitoring()
    addActivityListeners()
    updateLastActivity()
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  removeActivityListeners()
  stopSessionMonitoring()
})

// Expose public methods
defineExpose({
  extendSession: extendSessionAction,
  logout: logoutNow,
  hide: hideModal
})
</script>

<style scoped>
/* Modal backdrop blur */
.backdrop-blur-sm {
  backdrop-filter: blur(4px);
}

/* Progress bar animations */
.transition-all {
  transition-property: all;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
}

/* Loading spinner animation */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* Warning pulse animation */
@keyframes warning-pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
}

.bg-red-50 {
  animation: warning-pulse 2s infinite;
}

/* Focus styles */
button:focus {
  @apply outline-none ring-2 ring-blue-500 ring-offset-2 dark:ring-offset-gray-800;
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .bg-gray-200 {
    @apply bg-gray-800;
  }
  
  .text-gray-600 {
    @apply text-black;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .transition-all,
  .transition {
    transition: none !important;
  }
  
  @keyframes warning-pulse {
    0%, 100% {
      opacity: 1;
    }
  }
}

/* Mobile responsiveness */
@media (max-width: 640px) {
  .max-w-md {
    @apply mx-4;
  }
  
  .flex.space-x-3 {
    @apply flex-col space-x-0 space-y-2;
  }
}

/* Keyboard shortcut styling */
kbd {
  @apply inline-block px-1 py-0.5 text-xs font-mono bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded shadow-sm;
}

/* Progress bar color transitions */
.bg-yellow-500 {
  transition: background-color 0.3s ease;
}

.bg-orange-500 {
  transition: background-color 0.3s ease;
}

.bg-red-500 {
  transition: background-color 0.3s ease;
}

/* Modal entrance animation */
@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-content {
  animation: modalSlideIn 0.3s ease-out;
}

/* Accessibility improvements */
[role="dialog"] {
  @apply focus:outline-none;
}

/* Print styles */
@media print {
  .fixed.inset-0 {
    @apply hidden;
  }
}
</style>