<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition ease-out duration-300"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition ease-in duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div 
        v-if="modelValue"
        class="fixed inset-0 z-50 overflow-y-auto"
        aria-labelledby="modal-title"
        role="dialog"
        aria-modal="true"
      >
        <!-- Backdrop -->
        <div 
          class="fixed inset-0 bg-black bg-opacity-50 backdrop-blur-sm transition-opacity"
          @click="handleBackdropClick"
        />

        <!-- Modal Container -->
        <div class="flex min-h-full items-center justify-center p-4">
          <Transition
            enter-active-class="transition ease-out duration-300"
            enter-from-class="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
            enter-to-class="opacity-100 translate-y-0 sm:scale-100"
            leave-active-class="transition ease-in duration-200"
            leave-from-class="opacity-100 translate-y-0 sm:scale-100"
            leave-to-class="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
          >
            <div
              v-if="modelValue"
              ref="modalRef"
              :class="[
                'relative transform overflow-hidden rounded-lg bg-white dark:bg-gray-800 shadow-xl transition-all',
                sizeClasses[size],
                'max-h-[90vh] overflow-y-auto'
              ]"
              @keydown.esc="handleEscape"
              tabindex="-1"
            >
              <!-- Header -->
              <div 
                v-if="title || hasHeaderSlot"
                class="flex items-center justify-between p-6 pb-4 border-b border-gray-200 dark:border-gray-700"
              >
                <div class="flex-1">
                  <slot name="header">
                    <h3 
                      id="modal-title"
                      class="text-lg font-semibold leading-6 text-gray-900 dark:text-gray-100"
                    >
                      {{ title }}
                    </h3>
                    <p 
                      v-if="subtitle"
                      class="mt-1 text-sm text-gray-500 dark:text-gray-400"
                    >
                      {{ subtitle }}
                    </p>
                  </slot>
                </div>
                
                <!-- Close Button -->
                <button
                  v-if="showCloseButton"
                  @click="close"
                  class="ml-4 inline-flex items-center justify-center rounded-md p-2 text-gray-400 hover:text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-blue-500 transition-colors duration-200"
                  aria-label="Chiudi"
                >
                  <XMarkIcon class="h-5 w-5" />
                </button>
              </div>

              <!-- Content -->
              <div 
                :class="[
                  'p-6',
                  !title && !hasHeaderSlot ? 'pt-6' : 'pt-4',
                  !hasFooterSlot ? 'pb-6' : 'pb-4'
                ]"
              >
                <slot />
              </div>

              <!-- Footer -->
              <div 
                v-if="hasFooterSlot"
                class="flex justify-end space-x-3 bg-gray-50 dark:bg-gray-900 px-6 py-4"
              >
                <slot name="footer" />
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted, onUnmounted, useSlots } from 'vue'
import { XMarkIcon } from '@heroicons/vue/24/outline'

export type ModalSize = 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'full'

interface Props {
  modelValue: boolean
  title?: string
  subtitle?: string
  size?: ModalSize
  closeOnBackdrop?: boolean
  closeOnEscape?: boolean
  showCloseButton?: boolean
  persistent?: boolean
}

interface Emits {
  (event: 'update:modelValue', value: boolean): void
  (event: 'close'): void
  (event: 'opened'): void
  (event: 'closed'): void
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  closeOnBackdrop: true,
  closeOnEscape: true,
  showCloseButton: true,
  persistent: false
})

const emit = defineEmits<Emits>()
const slots = useSlots()

const modalRef = ref<HTMLElement>()
const previouslyFocusedElement = ref<HTMLElement | null>(null)

// Computed properties
const sizeClasses: Record<ModalSize, string> = {
  sm: 'w-full max-w-sm',
  md: 'w-full max-w-md',
  lg: 'w-full max-w-lg',
  xl: 'w-full max-w-xl',
  '2xl': 'w-full max-w-2xl',
  full: 'w-full max-w-full h-full'
}

const hasHeaderSlot = computed(() => !!slots.header)
const hasFooterSlot = computed(() => !!slots.footer)

// Methods
const close = () => {
  if (props.persistent) return
  
  emit('update:modelValue', false)
  emit('close')
}

const handleBackdropClick = () => {
  if (props.closeOnBackdrop && !props.persistent) {
    close()
  }
}

const handleEscape = () => {
  if (props.closeOnEscape && !props.persistent) {
    close()
  }
}

const focusModal = async () => {
  await nextTick()
  if (modalRef.value) {
    modalRef.value.focus()
  }
}

const trapFocus = (event: KeyboardEvent) => {
  if (!modalRef.value || event.key !== 'Tab') return

  const focusableElements = modalRef.value.querySelectorAll(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  )

  const firstElement = focusableElements[0] as HTMLElement
  const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement

  if (event.shiftKey) {
    // Shift + Tab
    if (document.activeElement === firstElement) {
      event.preventDefault()
      lastElement.focus()
    }
  } else {
    // Tab
    if (document.activeElement === lastElement) {
      event.preventDefault()
      firstElement.focus()
    }
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  if (props.modelValue) {
    if (event.key === 'Escape') {
      handleEscape()
    } else {
      trapFocus(event)
    }
  }
}

const storePreviousFocus = () => {
  previouslyFocusedElement.value = document.activeElement as HTMLElement
}

const restorePreviousFocus = () => {
  if (previouslyFocusedElement.value && previouslyFocusedElement.value.focus) {
    previouslyFocusedElement.value.focus()
  }
}

const preventBodyScroll = (prevent: boolean) => {
  if (prevent) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
}

// Watchers
watch(() => props.modelValue, async (isOpen) => {
  if (isOpen) {
    storePreviousFocus()
    preventBodyScroll(true)
    await focusModal()
    emit('opened')
  } else {
    preventBodyScroll(false)
    restorePreviousFocus()
    emit('closed')
  }
})

// Lifecycle
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  
  // If modal is open on mount
  if (props.modelValue) {
    storePreviousFocus()
    preventBodyScroll(true)
    focusModal()
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  preventBodyScroll(false)
})

// Expose methods for external control
defineExpose({
  close,
  focus: focusModal
})
</script>

<style scoped>
/* Ensure backdrop blur works on all browsers */
.backdrop-blur-sm {
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

/* Smooth scroll for modal content */
.overflow-y-auto {
  scrollbar-width: thin;
  scrollbar-color: rgb(156 163 175) transparent;
}

.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: transparent;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background-color: rgb(156 163 175);
  border-radius: 3px;
}

.dark .overflow-y-auto::-webkit-scrollbar-thumb {
  background-color: rgb(75 85 99);
}

/* Ensure modal is above other elements */
.z-50 {
  z-index: 50;
}

/* Mobile optimizations */
@media (max-width: 640px) {
  .p-4 {
    @apply p-2;
  }
  
  
  .max-h-[90vh] {
    @apply max-h-screen sm:max-h-[90vh];
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .bg-opacity-50 {
    @apply bg-opacity-80;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .transition {
    transition: none !important;
  }
}
</style>