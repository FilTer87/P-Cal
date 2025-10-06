<template>
  <Modal
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="title"
    size="sm"
    :persistent="isProcessing"
    :close-on-backdrop="!isProcessing"
    :close-on-escape="!isProcessing"
    :show-close-button="!isProcessing"
  >
    <!-- Content -->
    <div class="space-y-4">
      <!-- Icon -->
      <div class="flex justify-center">
        <div
          :class="[
            'rounded-full p-3',
            variantStyles[variant].iconBg
          ]"
        >
          <ExclamationTriangleIcon
            v-if="variant === 'danger'"
            :class="['h-6 w-6', variantStyles[variant].iconColor]"
          />
          <QuestionMarkCircleIcon
            v-else-if="variant === 'warning'"
            :class="['h-6 w-6', variantStyles[variant].iconColor]"
          />
          <InformationCircleIcon
            v-else
            :class="['h-6 w-6', variantStyles[variant].iconColor]"
          />
        </div>
      </div>

      <!-- Message -->
      <div class="text-center">
        <p class="text-sm text-gray-600 dark:text-gray-300 leading-relaxed">
          {{ message }}
        </p>
      </div>

      <!-- Additional Details -->
      <div v-if="details" class="text-center">
        <details class="text-left">
          <summary class="cursor-pointer text-xs text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200">
            {{ t('common.showDetails') }}
          </summary>
          <div class="mt-2 p-3 bg-gray-50 dark:bg-gray-900 rounded-md">
            <pre class="text-xs text-gray-600 dark:text-gray-400 whitespace-pre-wrap">{{ details }}</pre>
          </div>
        </details>
      </div>

      <!-- Checkbox for confirmations -->
      <div v-if="requireConfirmation" class="flex items-center space-x-2">
        <input
          id="confirm-checkbox"
          v-model="isConfirmed"
          type="checkbox"
          class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 dark:border-gray-600 rounded"
        />
        <label for="confirm-checkbox" class="text-sm text-gray-600 dark:text-gray-300">
          {{ displayConfirmationText }}
        </label>
      </div>
    </div>

    <!-- Footer Actions -->
    <template #footer>
      <button
        @click="handleCancel"
        :disabled="isProcessing"
        class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
      >
        {{ displayCancelText }}
      </button>

      <button
        @click="handleConfirm"
        :disabled="isProcessing || (requireConfirmation && !isConfirmed)"
        :class="[
          'px-4 py-2 text-sm font-medium rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200',
          variantStyles[variant].button,
          isProcessing ? 'cursor-not-allowed' : ''
        ]"
      >
        <LoadingSpinner
          v-if="isProcessing"
          size="small"
          color="white"
          class="mr-2"
        />
        {{ isProcessing ? displayProcessingText : displayConfirmText }}
      </button>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Modal from './Modal.vue'
import LoadingSpinner from './LoadingSpinner.vue'
import {
  ExclamationTriangleIcon,
  QuestionMarkCircleIcon,
  InformationCircleIcon
} from '@heroicons/vue/24/outline'

// i18n
const { t } = useI18n()

export type ConfirmVariant = 'default' | 'danger' | 'warning'

interface Props {
  modelValue: boolean
  title: string
  message: string
  details?: string
  variant?: ConfirmVariant
  confirmText?: string
  cancelText?: string
  processingText?: string
  requireConfirmation?: boolean
  confirmationText?: string
  isProcessing?: boolean
}

interface Emits {
  (event: 'update:modelValue', value: boolean): void
  (event: 'confirm'): void
  (event: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  confirmText: undefined,
  cancelText: undefined,
  processingText: undefined,
  requireConfirmation: false,
  isProcessing: false
})

// Computed properties for default text values with i18n
const displayConfirmText = computed(() => props.confirmText || t('common.confirm'))
const displayCancelText = computed(() => props.cancelText || t('common.cancel'))
const displayProcessingText = computed(() => props.processingText || t('common.processing'))
const displayConfirmationText = computed(() => props.confirmationText || t('common.confirmProceed'))

const emit = defineEmits<Emits>()

// Local state
const isConfirmed = ref(false)

// Style configurations
const variantStyles: Record<ConfirmVariant, {
  iconBg: string
  iconColor: string
  button: string
}> = {
  default: {
    iconBg: 'bg-blue-100 dark:bg-blue-900/20',
    iconColor: 'text-blue-600 dark:text-blue-400',
    button: 'bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500'
  },
  danger: {
    iconBg: 'bg-red-100 dark:bg-red-900/20',
    iconColor: 'text-red-600 dark:text-red-400',
    button: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500'
  },
  warning: {
    iconBg: 'bg-yellow-100 dark:bg-yellow-900/20',
    iconColor: 'text-yellow-600 dark:text-yellow-400',
    button: 'bg-yellow-600 text-white hover:bg-yellow-700 focus:ring-yellow-500'
  }
}

// Methods
const handleConfirm = () => {
  if (props.requireConfirmation && !isConfirmed.value) return
  if (props.isProcessing) return
  
  emit('confirm')
}

const handleCancel = () => {
  if (props.isProcessing) return
  
  emit('cancel')
  emit('update:modelValue', false)
}

// Reset confirmation state when dialog closes
const resetState = () => {
  isConfirmed.value = false
}

// Watch for dialog close to reset state
watch(() => props.modelValue, (newValue) => {
  if (!newValue) {
    resetState()
  }
})
</script>

<style scoped>
/* Custom checkbox styles */
input[type="checkbox"]:checked {
  background-image: url("data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='white' xmlns='http://www.w3.org/2000/svg'%3e%3cpath d='m13.854 3.646-7.5 7.5a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6 10.293l7.146-7.147a.5.5 0 0 1 .708.708z'/%3e%3c/svg%3e");
}

/* Details element styling */
details[open] summary {
  margin-bottom: 0.5rem;
}

details summary {
  list-style: none;
  outline: none;
}

details summary::-webkit-details-marker {
  display: none;
}

details summary::before {
  content: '▶';
  margin-right: 0.5rem;
  transition: transform 0.2s ease-in-out;
}

details[open] summary::before {
  transform: rotate(90deg);
}

/* Pre element for details */
pre {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.75rem;
  line-height: 1.4;
  max-height: 200px;
  overflow-y: auto;
}

/* Scrollbar styling for details */
pre::-webkit-scrollbar {
  width: 4px;
}

pre::-webkit-scrollbar-track {
  background: transparent;
}

pre::-webkit-scrollbar-thumb {
  background-color: rgba(156, 163, 175, 0.5);
  border-radius: 2px;
}

.dark pre::-webkit-scrollbar-thumb {
  background-color: rgba(75, 85, 99, 0.5);
}

/* Focus states for better accessibility */
button:focus,
input:focus,
details summary:focus {
  outline: 2px solid rgb(59 130 246);
  outline-offset: 2px;
}

/* Button hover effects */
button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.dark button:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* Transition effects */
button {
  transition: all 0.2s ease-in-out;
}

/* Disabled state improvements */
button:disabled {
  transform: none !important;
  box-shadow: none !important;
}
</style>