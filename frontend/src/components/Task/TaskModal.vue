<template>
  <TransitionRoot 
    :show="isOpen" 
    as="template"
    @after-leave="handleAfterLeave"
  >
    <Dialog 
      as="div" 
      class="relative z-50"
      :initial-focus="initialFocusRef"
      @close="handleClose"
    >
      <!-- Backdrop -->
      <TransitionChild
        as="template"
        enter="ease-out duration-300"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="ease-in duration-200"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div class="fixed inset-0 bg-black bg-opacity-25 backdrop-blur-sm" />
      </TransitionChild>

      <!-- Modal Container -->
      <div class="fixed inset-0 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-0 sm:p-4">
          <TransitionChild
            as="template"
            enter="ease-out duration-300"
            enter-from="opacity-0 scale-95 sm:scale-95"
            enter-to="opacity-100 scale-100 sm:scale-100"
            leave="ease-in duration-200"
            leave-from="opacity-100 scale-100 sm:scale-100"
            leave-to="opacity-0 scale-95 sm:scale-95"
          >
            <DialogPanel 
              :class="[
                'relative w-full transform overflow-hidden transition-all',
                modalClasses
              ]"
            >
              <!-- Mobile: Full screen -->
              <div class="sm:hidden h-screen flex flex-col">
                <!-- Header -->
                <div :class="[
                  'flex items-center justify-between p-4 border-b',
                  headerClass,
                  'border-gray-200 dark:border-gray-700'
                ]">
                  <DialogTitle 
                    :class="['text-lg font-semibold', textClass]"
                    ref="initialFocusRef"
                    tabindex="-1"
                  >
                    {{ modalTitle }}
                  </DialogTitle>
                  
                  <button
                    @click="handleClose"
                    :class="[
                      'p-2 rounded-lg transition-colors',
                      'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300',
                      'hover:bg-gray-100 dark:hover:bg-gray-700',
                      'focus:outline-none focus:ring-2 focus:ring-blue-500'
                    ]"
                    :disabled="isFormLoading"
                  >
                    <span class="sr-only">Chiudi</span>
                    <XMarkIcon class="w-5 h-5" />
                  </button>
                </div>

                <!-- Content -->
                <div class="flex-1 overflow-y-auto">
                  <div class="p-4">
                    <TaskForm
                      ref="taskFormRef"
                      :task="task"
                      :initial-date="initialDate"
                      :initial-time="initialTime"
                      :show-reminder-presets="true"
                      @submit="handleFormSubmit"
                      @cancel="handleClose"
                      @success="handleFormSuccess"
                      @error="handleFormError"
                    />
                  </div>
                </div>
              </div>

              <!-- Desktop: Centered modal -->
              <div class="hidden sm:block">
                <!-- Header -->
                <div :class="[
                  'flex items-center justify-between p-6 border-b',
                  headerClass,
                  'border-gray-200 dark:border-gray-700'
                ]">
                  <DialogTitle 
                    :class="['text-xl font-semibold', textClass]"
                    ref="initialFocusRef"
                    tabindex="-1"
                  >
                    {{ modalTitle }}
                  </DialogTitle>
                  
                  <div class="flex items-center space-x-2">
                    <!-- Delete button (only in edit mode) -->
                    <button
                      v-if="isEditMode && showDeleteButton"
                      @click="handleDelete"
                      :disabled="isFormLoading || isDeleting"
                      :class="[
                        'px-3 py-1.5 text-sm font-medium rounded-lg transition-colors',
                        'text-red-600 dark:text-red-400 border border-red-300 dark:border-red-600',
                        'hover:bg-red-50 dark:hover:bg-red-900/20',
                        'focus:outline-none focus:ring-2 focus:ring-red-500',
                        'disabled:opacity-50 disabled:cursor-not-allowed'
                      ]"
                      :title="'Elimina attività'"
                    >
                      <TrashIcon v-if="!isDeleting" class="w-4 h-4 inline mr-1" />
                      <svg
                        v-if="isDeleting"
                        class="animate-spin -ml-1 mr-1 h-4 w-4 text-red-600"
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                      >
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      {{ isDeleting ? 'Eliminando...' : 'Elimina' }}
                    </button>

                    <!-- Close button -->
                    <button
                      @click="handleClose"
                      :class="[
                        'p-2 rounded-lg transition-colors',
                        'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300',
                        'hover:bg-gray-100 dark:hover:bg-gray-700',
                        'focus:outline-none focus:ring-2 focus:ring-blue-500'
                      ]"
                      :disabled="isFormLoading"
                    >
                      <span class="sr-only">Chiudi</span>
                      <XMarkIcon class="w-5 h-5" />
                    </button>
                  </div>
                </div>

                <!-- Content -->
                <div class="max-h-[calc(100vh-12rem)] overflow-y-auto">
                  <div class="p-6">
                    <TaskForm
                      ref="taskFormRef"
                      :task="task"
                      :initial-date="initialDate"
                      :initial-time="initialTime"
                      :show-reminder-presets="true"
                      @submit="handleFormSubmit"
                      @cancel="handleClose"
                      @success="handleFormSuccess"
                      @error="handleFormError"
                    />
                  </div>
                </div>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>

      <!-- Delete Confirmation Dialog -->
      <TransitionRoot :show="showDeleteConfirm" as="template">
        <Dialog as="div" class="relative z-60" @close="showDeleteConfirm = false">
          <TransitionChild
            as="template"
            enter="ease-out duration-300"
            enter-from="opacity-0"
            enter-to="opacity-100"
            leave="ease-in duration-200"
            leave-from="opacity-100"
            leave-to="opacity-0"
          >
            <div class="fixed inset-0 bg-black bg-opacity-25" />
          </TransitionChild>

          <div class="fixed inset-0 overflow-y-auto">
            <div class="flex min-h-full items-center justify-center p-4">
              <TransitionChild
                as="template"
                enter="ease-out duration-300"
                enter-from="opacity-0 scale-95"
                enter-to="opacity-100 scale-100"
                leave="ease-in duration-200"
                leave-from="opacity-100 scale-100"
                leave-to="opacity-0 scale-95"
              >
                <DialogPanel :class="[
                  'relative w-full max-w-md transform overflow-hidden rounded-lg p-6 shadow-xl transition-all',
                  cardClass,
                  'border border-gray-200 dark:border-gray-700'
                ]">
                  <div class="flex items-center">
                    <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-red-100 dark:bg-red-900/20">
                      <ExclamationTriangleIcon class="h-6 w-6 text-red-600 dark:text-red-400" />
                    </div>
                    <div class="ml-4">
                      <DialogTitle as="h3" :class="['text-lg font-medium', textClass]">
                        Elimina attività
                      </DialogTitle>
                      <div class="mt-2">
                        <p class="text-sm text-gray-500 dark:text-gray-400">
                          Sei sicuro di voler eliminare "{{ task?.title }}"? 
                          Questa azione non può essere annullata.
                        </p>
                      </div>
                    </div>
                  </div>

                  <div class="mt-6 flex justify-end space-x-3">
                    <button
                      @click="showDeleteConfirm = false"
                      :disabled="isDeleting"
                      :class="[
                        'px-4 py-2 text-sm font-medium border rounded-lg transition-colors',
                        'text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600',
                        'hover:bg-gray-50 dark:hover:bg-gray-700',
                        'focus:outline-none focus:ring-2 focus:ring-blue-500',
                        'disabled:opacity-50 disabled:cursor-not-allowed'
                      ]"
                    >
                      Annulla
                    </button>
                    
                    <button
                      @click="confirmDelete"
                      :disabled="isDeleting"
                      :class="[
                        'px-4 py-2 text-sm font-medium rounded-lg transition-colors flex items-center',
                        'bg-red-600 text-white hover:bg-red-700',
                        'focus:outline-none focus:ring-2 focus:ring-red-500',
                        'disabled:opacity-50 disabled:cursor-not-allowed'
                      ]"
                    >
                      <svg
                        v-if="isDeleting"
                        class="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                      >
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      {{ isDeleting ? 'Eliminando...' : 'Elimina' }}
                    </button>
                  </div>
                </DialogPanel>
              </TransitionChild>
            </div>
          </div>
        </Dialog>
      </TransitionRoot>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { useTasks } from '../../composables/useTasks'
import { useTheme } from '../../composables/useTheme'
import { useNotifications } from '../../composables/useNotifications'
import {
  Dialog,
  DialogPanel,
  DialogTitle,
  TransitionChild,
  TransitionRoot
} from '@headlessui/vue'
import type { Task, CreateTaskRequest, UpdateTaskRequest } from '../../types/task'

// Components
import TaskForm from './TaskForm.vue'

// Icons
import { XMarkIcon, TrashIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'

// Props
interface Props {
  isOpen: boolean
  task?: Task
  initialDate?: string
  initialTime?: string
  showDeleteButton?: boolean
  preventClose?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isOpen: false,
  showDeleteButton: true,
  preventClose: false
})

// Emits
const emit = defineEmits<{
  'update:isOpen': [value: boolean]
  'close': []
  'task-created': [task: Task]
  'task-updated': [task: Task]
  'task-deleted': [taskId: number]
  'form-submit': [data: CreateTaskRequest | UpdateTaskRequest]
}>()

// Composables
const { deleteTask, isFormLoading } = useTasks()
const { textClass, cardClass } = useTheme()
const { showSuccess, showError } = useNotifications()

// State
const taskFormRef = ref<InstanceType<typeof TaskForm> | null>(null)
const initialFocusRef = ref<HTMLElement | null>(null)
const showDeleteConfirm = ref(false)
const isDeleting = ref(false)

// Computed
const isEditMode = computed(() => !!props.task)
const modalTitle = computed(() => 
  isEditMode.value ? 'Modifica Attività' : 'Nuova Attività'
)

const modalClasses = computed(() => [
  // Mobile: full screen
  'sm:max-w-2xl',
  'sm:rounded-lg sm:shadow-xl',
  cardClass.value,
  'sm:border sm:border-gray-200 sm:dark:border-gray-700'
])

const headerClass = computed(() => [
  cardClass.value
])

// Watch for open state changes
watch(
  () => props.isOpen,
  async (isOpen) => {
    if (isOpen) {
      await nextTick()
      // Focus will be handled by HeadlessUI Dialog
    }
  }
)

// Methods
const handleClose = () => {
  if (props.preventClose && (isFormLoading.value || isDeleting.value)) {
    return
  }
  
  emit('update:isOpen', false)
  emit('close')
}

const handleAfterLeave = () => {
  // Reset form state after modal closes
  if (taskFormRef.value) {
    taskFormRef.value.resetForm()
  }
  showDeleteConfirm.value = false
  isDeleting.value = false
}

const handleFormSubmit = (data: CreateTaskRequest | UpdateTaskRequest) => {
  emit('form-submit', data)
}

const handleFormSuccess = (task: Task) => {
  if (isEditMode.value) {
    emit('task-updated', task)
    showSuccess('Attività aggiornata con successo!')
  } else {
    emit('task-created', task)
    showSuccess('Attività creata con successo!')
  }
  
  handleClose()
}

const handleFormError = (error: string) => {
  showError(error)
}

const handleDelete = () => {
  if (!props.task) return
  showDeleteConfirm.value = true
}

const confirmDelete = async () => {
  if (!props.task) return
  
  isDeleting.value = true
  
  try {
    const success = await deleteTask(props.task.id)
    
    if (success) {
      emit('task-deleted', props.task.id)
      showSuccess('Attività eliminata con successo!')
      handleClose()
    } else {
      showError('Errore nell\'eliminazione dell\'attività')
    }
  } catch (error: any) {
    showError(error.message || 'Errore nell\'eliminazione dell\'attività')
  } finally {
    isDeleting.value = false
    showDeleteConfirm.value = false
  }
}

// Keyboard shortcuts
const handleKeydown = (event: KeyboardEvent) => {
  if (!props.isOpen) return
  
  switch (event.key) {
    case 'Escape':
      if (!showDeleteConfirm.value && !props.preventClose) {
        event.preventDefault()
        handleClose()
      }
      break
    case 's':
    case 'S':
      if ((event.ctrlKey || event.metaKey) && taskFormRef.value) {
        event.preventDefault()
        if (taskFormRef.value.isFormValid) {
          // Trigger form submission
          const form = document.querySelector('.task-form form') as HTMLFormElement
          if (form) {
            form.requestSubmit()
          }
        }
      }
      break
  }
}

// Add keyboard event listeners
watch(
  () => props.isOpen,
  (isOpen) => {
    if (isOpen) {
      document.addEventListener('keydown', handleKeydown)
    } else {
      document.removeEventListener('keydown', handleKeydown)
    }
  }
)

// Cleanup on unmount
import { onUnmounted } from 'vue'
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

// Expose methods for parent components
defineExpose({
  close: handleClose,
  taskForm: computed(() => taskFormRef.value),
  isFormValid: computed(() => taskFormRef.value?.isFormValid || false),
  isLoading: computed(() => isFormLoading.value || isDeleting.value)
})
</script>

<style scoped>
/* Custom scrollbar for content areas */
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

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background-color: rgb(107 114 128);
}

/* Dark mode scrollbar */
.dark .overflow-y-auto {
  scrollbar-color: rgb(75 85 99) transparent;
}

.dark .overflow-y-auto::-webkit-scrollbar-thumb {
  background-color: rgb(75 85 99);
}

.dark .overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background-color: rgb(107 114 128);
}

/* Loading spinner animation */
@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* Mobile-specific styles */
@media (max-width: 640px) {
  .task-modal-mobile {
    height: 100vh;
    width: 100vw;
    border-radius: 0;
  }
}

/* Focus trap styling */
.task-modal :focus {
  outline: 2px solid rgb(59 130 246);
  outline-offset: 2px;
}

/* Backdrop blur enhancement */
.backdrop-blur-sm {
  backdrop-filter: blur(4px);
}

/* Modal transition improvements */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

@media (max-width: 640px) {
  .modal-enter-from,
  .modal-leave-to {
    transform: translateY(100%);
  }
}
</style>