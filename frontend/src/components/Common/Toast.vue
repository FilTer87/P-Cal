<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition ease-out duration-300"
      enter-from-class="opacity-0 translate-y-2"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition ease-in duration-200"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 translate-y-2"
    >
      <div
        v-if="show"
        class="fixed top-4 right-4 z-50 max-w-sm w-full"
        role="alert"
        aria-live="assertive"
      >
        <div
          :class="[
            'rounded-lg shadow-lg border p-4 transition-all duration-200',
            toastTypeClasses[type]
          ]"
        >
          <div class="flex items-start">
            <!-- Icon -->
            <div class="flex-shrink-0">
              <CheckCircleIcon 
                v-if="type === 'success'" 
                class="h-5 w-5" 
                :class="iconClasses[type]" 
              />
              <ExclamationCircleIcon 
                v-else-if="type === 'error'" 
                class="h-5 w-5" 
                :class="iconClasses[type]" 
              />
              <ExclamationTriangleIcon 
                v-else-if="type === 'warning'" 
                class="h-5 w-5" 
                :class="iconClasses[type]" 
              />
              <InformationCircleIcon 
                v-else-if="type === 'info'" 
                class="h-5 w-5" 
                :class="iconClasses[type]" 
              />
              <BellIcon 
                v-else-if="type === 'reminder'" 
                class="h-5 w-5" 
                :class="iconClasses[type]" 
              />
            </div>

            <!-- Content -->
            <div class="ml-3 flex-1">
              <h3 
                v-if="title" 
                :class="[
                  'text-sm font-medium',
                  titleClasses[type]
                ]"
              >
                {{ title }}
              </h3>
              <div 
                :class="[
                  'text-sm',
                  title ? 'mt-1' : '',
                  messageClasses[type]
                ]"
              >
                {{ message }}
              </div>
              
              <!-- Action Button -->
              <div v-if="actionText && actionCallback" class="mt-3">
                <button
                  @click="handleAction"
                  :class="[
                    'text-sm font-medium underline hover:no-underline focus:outline-none',
                    actionClasses[type]
                  ]"
                >
                  {{ actionText }}
                </button>
              </div>
            </div>

            <!-- Close Button -->
            <div class="ml-4 flex-shrink-0 flex">
              <button
                @click="close"
                :class="[
                  'inline-flex rounded-md p-1 hover:opacity-75 focus:outline-none focus:ring-2 focus:ring-offset-2',
                  closeButtonClasses[type]
                ]"
                aria-label="Chiudi notifica"
              >
                <XMarkIcon class="h-4 w-4" />
              </button>
            </div>
          </div>

          <!-- Progress Bar (for auto-dismiss) -->
          <div 
            v-if="autoDismiss && duration"
            class="mt-3 bg-black bg-opacity-10 rounded-full h-1 overflow-hidden"
          >
            <div
              class="h-full bg-current transition-all linear"
              :style="{
                width: `${progressWidth}%`,
                transitionDuration: `${duration}ms`
              }"
            />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  CheckCircleIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  BellIcon,
  XMarkIcon
} from '@heroicons/vue/24/outline'

export type ToastType = 'success' | 'error' | 'warning' | 'info' | 'reminder'

interface Props {
  type: ToastType
  title?: string
  message: string
  autoDismiss?: boolean
  duration?: number
  actionText?: string
  actionCallback?: () => void
  onClose?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  type: 'info',
  autoDismiss: true,
  duration: 5000
})

const show = ref(true)
const progressWidth = ref(0)
let dismissTimer: ReturnType<typeof setTimeout> | null = null
let progressTimer: ReturnType<typeof setInterval> | null = null

// Style configurations
const toastTypeClasses = {
  success: 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800',
  error: 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800',
  warning: 'bg-yellow-50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-800',
  info: 'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800',
  reminder: 'bg-purple-50 dark:bg-purple-900/20 border-purple-200 dark:border-purple-800'
}

const iconClasses = {
  success: 'text-green-400 dark:text-green-300',
  error: 'text-red-400 dark:text-red-300',
  warning: 'text-yellow-400 dark:text-yellow-300',
  info: 'text-blue-400 dark:text-blue-300',
  reminder: 'text-purple-400 dark:text-purple-300'
}

const titleClasses = {
  success: 'text-green-800 dark:text-green-200',
  error: 'text-red-800 dark:text-red-200',
  warning: 'text-yellow-800 dark:text-yellow-200',
  info: 'text-blue-800 dark:text-blue-200',
  reminder: 'text-purple-800 dark:text-purple-200'
}

const messageClasses = {
  success: 'text-green-700 dark:text-green-300',
  error: 'text-red-700 dark:text-red-300',
  warning: 'text-yellow-700 dark:text-yellow-300',
  info: 'text-blue-700 dark:text-blue-300',
  reminder: 'text-purple-700 dark:text-purple-300'
}

const actionClasses = {
  success: 'text-green-600 dark:text-green-400 hover:text-green-500 dark:hover:text-green-300',
  error: 'text-red-600 dark:text-red-400 hover:text-red-500 dark:hover:text-red-300',
  warning: 'text-yellow-600 dark:text-yellow-400 hover:text-yellow-500 dark:hover:text-yellow-300',
  info: 'text-blue-600 dark:text-blue-400 hover:text-blue-500 dark:hover:text-blue-300',
  reminder: 'text-purple-600 dark:text-purple-400 hover:text-purple-500 dark:hover:text-purple-300'
}

const closeButtonClasses = {
  success: 'text-green-500 dark:text-green-400 focus:ring-green-500',
  error: 'text-red-500 dark:text-red-400 focus:ring-red-500',
  warning: 'text-yellow-500 dark:text-yellow-400 focus:ring-yellow-500',
  info: 'text-blue-500 dark:text-blue-400 focus:ring-blue-500',
  reminder: 'text-purple-500 dark:text-purple-400 focus:ring-purple-500'
}

// Methods
const close = () => {
  show.value = false
  cleanupTimers()
  setTimeout(() => {
    props.onClose?.()
  }, 300) // Wait for transition to complete
}

const handleAction = () => {
  props.actionCallback?.()
  close()
}

const cleanupTimers = () => {
  if (dismissTimer) {
    clearTimeout(dismissTimer)
    dismissTimer = null
  }
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

const startAutoDismiss = () => {
  if (!props.autoDismiss || !props.duration) return

  // Start progress animation
  progressWidth.value = 100
  
  const progressStep = 100 / (props.duration / 50) // Update every 50ms
  let currentProgress = 100

  progressTimer = setInterval(() => {
    currentProgress -= progressStep
    if (currentProgress <= 0) {
      progressWidth.value = 0
      cleanupTimers()
    } else {
      progressWidth.value = currentProgress
    }
  }, 50)

  // Auto dismiss
  dismissTimer = setTimeout(() => {
    close()
  }, props.duration)
}

const pauseAutoDismiss = () => {
  cleanupTimers()
}

const resumeAutoDismiss = () => {
  startAutoDismiss()
}

// Lifecycle
onMounted(() => {
  startAutoDismiss()
})

onUnmounted(() => {
  cleanupTimers()
})

// Export methods for parent component control
defineExpose({
  close,
  pauseAutoDismiss,
  resumeAutoDismiss
})
</script>

<style scoped>
/* Ensure smooth transitions */
.transition-all {
  transition-property: all;
}

/* Custom progress bar animation */
.linear {
  transition-timing-function: linear;
}

/* Toast container positioning */
.toast-container {
  pointer-events: auto;
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .fixed.top-4.right-4 {
    @apply top-2 right-2 left-2 max-w-none;
  }
}

/* Dark mode enhancements */
@media (prefers-color-scheme: dark) {
  .shadow-lg {
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.3), 0 4px 6px -2px rgba(0, 0, 0, 0.2);
  }
}
</style>