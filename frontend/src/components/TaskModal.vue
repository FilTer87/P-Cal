<template>
  <div v-if="show" class="modal-overlay" @click="handleBackdropClick">
    <div class="modal-content" @click.stop>
      <!-- Modal Header -->
      <div class="flex items-center justify-between mb-6">
        <h3 class="text-lg font-medium text-gray-900 dark:text-white">
          {{ isEditing ? 'Modifica Attività' : 'Nuova Attività' }}
        </h3>
        <button @click="closeModal" 
          class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md transition-colors">
          <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Form -->
      <form @submit.prevent="handleSubmit" class="space-y-3">
        <!-- Title -->
        <div>
          <label for="title" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Titolo <span class="text-red-500">*</span>
          </label>
          <input
            id="title"
            v-model="formData.title"
            type="text"
            :disabled="isFormLoading"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm placeholder-gray-400 dark:placeholder-gray-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.title }"
            placeholder="Inserisci il titolo dell'attività"
          />
          <p v-if="formErrors.title" class="mt-1 text-sm text-red-500">{{ formErrors.title }}</p>
        </div>

        <!-- Description -->
        <div>
          <label for="description" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Descrizione
          </label>
          <textarea
            id="description"
            v-model="formData.description"
            :disabled="isFormLoading"
            rows="3"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm placeholder-gray-400 dark:placeholder-gray-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.description }"
            placeholder="Aggiungi una descrizione (opzionale)"
          />
          <p v-if="formErrors.description" class="mt-1 text-sm text-red-500">{{ formErrors.description }}</p>
        </div>

        <!-- All Day Toggle -->
        <div>
          <label class="flex items-center space-x-2 mb-1">
            <input
              v-model="formData.isAllDay"
              type="checkbox"
              :disabled="isFormLoading"
              class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded disabled:opacity-50 disabled:cursor-not-allowed"
            />
            <span class="text-sm font-medium text-gray-700 dark:text-gray-300">
              Evento per tutta la giornata
            </span>
          </label>
        </div>

        <!-- Start Date and Time -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label for="startDate" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Data di inizio <span class="text-red-500">*</span>
            </label>
            <input
              id="startDate"
              v-model="formData.startDate"
              type="date"
              :disabled="isFormLoading"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.startDate }"
            />
            <p v-if="formErrors.startDate" class="mt-1 text-sm text-red-500">{{ formErrors.startDate }}</p>
          </div>
          
          <div v-if="!formData.isAllDay">
            <label for="startTime" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Ora di inizio
            </label>
            <input
              id="startTime"
              v-model="formData.startTime"
              type="time"
              :disabled="isFormLoading"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.startTime }"
            />
            <p v-if="formErrors.startTime" class="mt-1 text-sm text-red-500">{{ formErrors.startTime }}</p>
          </div>
        </div>

        <!-- End Date and Time -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label for="endDate" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Data di fine <span class="text-red-500">*</span>
            </label>
            <input
              id="endDate"
              v-model="formData.endDate"
              type="date"
              :disabled="isFormLoading"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.endDate }"
            />
            <p v-if="formErrors.endDate" class="mt-1 text-sm text-red-500">{{ formErrors.endDate }}</p>
          </div>
          
          <div v-if="!formData.isAllDay">
            <label for="endTime" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Ora di fine
            </label>
            <input
              id="endTime"
              v-model="formData.endTime"
              type="time"
              :disabled="isFormLoading"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.endTime }"
            />
            <p v-if="formErrors.endTime" class="mt-1 text-sm text-red-500">{{ formErrors.endTime }}</p>
          </div>
        </div>

        <!-- Location -->
        <div>
          <label for="location" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Luogo
          </label>
          <input
            id="location"
            v-model="formData.location"
            type="text"
            :disabled="isFormLoading"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm placeholder-gray-400 dark:placeholder-gray-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.location }"
            placeholder="Inserisci il luogo (opzionale)"
          />
          <p v-if="formErrors.location" class="mt-1 text-sm text-red-500">{{ formErrors.location }}</p>
        </div>

        <!-- Color -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Colore
          </label>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="colorOption in calendarColors"
              :key="colorOption.value"
              type="button"
              @click="formData.color = colorOption.value"
              :disabled="isFormLoading"
              class="w-8 h-8 rounded-full border-2 flex items-center justify-center disabled:opacity-50 disabled:cursor-not-allowed"
              :class="{
                'border-gray-800 dark:border-white': formData.color === colorOption.value,
                'border-gray-300 dark:border-gray-600': formData.color !== colorOption.value
              }"
              :style="{ backgroundColor: colorOption.value }"
              :title="colorOption.name"
            >
              <svg v-if="formData.color === colorOption.value" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Reminders Section -->
        <div>
          <div class="flex items-center justify-between mb-3">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Promemoria
            </label>
            <button
              type="button"
              @click="addReminder"
              :disabled="isFormLoading"
              class="inline-flex items-center px-3 py-1 text-xs font-medium text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
              Aggiungi Promemoria
            </button>
          </div>

          <div v-if="formData.reminders.length > 0" class="space-y-3">
            <div v-for="(reminder, index) in formData.reminders" :key="index" 
              class="flex items-center space-x-2 p-3 bg-gray-50 dark:bg-gray-700/50 rounded-md">
              <div class="flex-1">
                <div class="flex items-center space-x-2">
                  <span class="text-sm font-medium text-gray-700 dark:text-gray-300">
                    {{ formatReminderLabel(reminder.offsetMinutes) }}
                  </span>
                  <select
                    v-model="reminder.notificationType"
                    :disabled="isFormLoading"
                    class="px-2 py-1 text-xs border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:opacity-50"
                  >
                    <option v-for="(config, type) in notificationTypes" :key="type" :value="type">
                      {{ config.icon }} {{ config.label }}
                    </option>
                  </select>
                </div>
              </div>
              <button
                type="button"
                @click="removeReminder(index)"
                :disabled="isFormLoading"
                class="p-1 text-red-500 hover:text-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>

            <!-- Quick Reminder Presets -->
            <div class="flex flex-wrap gap-2">
              <button
                v-for="preset in reminderPresets"
                :key="preset.label"
                type="button"
                @click="addReminderPreset(preset.minutes)"
                :disabled="isFormLoading"
                class="px-2 py-1 text-xs bg-blue-100 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 rounded hover:bg-blue-200 dark:hover:bg-blue-900/40 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {{ preset.label }}
              </button>
            </div>
          </div>

          <!-- Reminder errors -->
          <div v-if="Object.keys(formErrors).some(key => key.startsWith('reminder_'))" class="mt-2">
            <p v-for="(error, key) in formErrors" :key="key" 
              v-show="key.startsWith('reminder_')"
              class="text-sm text-red-500">
              {{ error }}
            </p>
          </div>
        </div>

        <!-- Form Actions -->
        <div class="flex justify-end space-x-3 pt-4 border-t border-gray-200 dark:border-gray-600">
          <button
            type="button"
            @click="closeModal"
            :disabled="isFormLoading"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-gray-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Annulla
          </button>
          
          <button
            v-if="isEditing"
            type="button"
            @click="handleDelete"
            :disabled="isFormLoading"
            class="px-4 py-2 text-sm font-medium text-white bg-red-600 border border-transparent rounded-md shadow-sm hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <svg v-if="isFormLoading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white inline" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Elimina
          </button>
          
          <button
            type="submit"
            :disabled="isFormLoading || !isFormValid"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <svg v-if="isFormLoading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white inline" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            {{ isEditing ? 'Aggiorna' : 'Crea' }} Attività
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useTasks } from '../composables/useTasks'
import { useCustomToast } from '../composables/useCustomToast'
import { NotificationType, NOTIFICATION_TYPE_CONFIG, REMINDER_PRESETS, CALENDAR_COLORS, type Task, type TaskFormData } from '../types/task'
import { format } from 'date-fns'

interface Props {
  show: boolean
  task?: Task | null
  initialDate?: Date
}

interface Emits {
  (e: 'close'): void
  (e: 'task-created', task: Task): void
  (e: 'task-updated', task: Task): void
  (e: 'task-deleted', taskId: number): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const {
  isFormLoading,
  formErrors,
  createTask,
  updateTask,
  deleteTask,
  createEmptyTaskForm,
  createTaskFormFromTask,
  convertFormToTaskRequest,
  convertFormToUpdateRequest,
  validateTaskForm
} = useTasks()

const { showSuccess, showError } = useCustomToast()

// Local confirmation dialog implementation
const showConfirmation = (message: string, onConfirm: () => void) => {
  if (window.confirm(message)) {
    onConfirm()
  }
}

// Form state
const formData = ref<TaskFormData>(createEmptyTaskForm())

// Computed
const isEditing = computed(() => !!props.task)

const isFormValid = computed(() => {
  return formData.value.title.trim().length > 0
})

const notificationTypes = computed(() => NOTIFICATION_TYPE_CONFIG)

const calendarColors = computed(() => CALENDAR_COLORS)

const reminderPresets = computed(() => REMINDER_PRESETS)


// Methods
const resetForm = () => {
  formData.value = createEmptyTaskForm()
  
  if (props.initialDate) {
    formData.value.startDate = format(props.initialDate, 'yyyy-MM-dd')
    formData.value.endDate = format(props.initialDate, 'yyyy-MM-dd')
  }
  
  formErrors.value = {}
}

const loadTaskData = () => {
  if (props.task) {
    formData.value = createTaskFormFromTask(props.task)
  }
}

const handleBackdropClick = () => {
  if (!isFormLoading.value) {
    closeModal()
  }
}

const closeModal = () => {
  if (!isFormLoading.value) {
    emit('close')
  }
}

const addReminder = () => {
  const newReminder = {
    offsetMinutes: 15,
    notificationType: NotificationType.PUSH
  }
  formData.value.reminders.push(newReminder)
}

const removeReminder = (index: number) => {
  formData.value.reminders.splice(index, 1)
}

const addReminderPreset = (offsetMinutes: number) => {
  const reminder = {
    offsetMinutes,
    notificationType: NotificationType.PUSH
  }
  formData.value.reminders.push(reminder)
}

const formatReminderLabel = (offsetMinutes: number): string => {
  if (offsetMinutes === 0) return 'Al momento dell\'evento'
  if (offsetMinutes < 60) return `${offsetMinutes} minuti prima`
  if (offsetMinutes < 24 * 60) {
    const hours = Math.floor(offsetMinutes / 60)
    const minutes = offsetMinutes % 60
    if (minutes === 0) return `${hours} ore prima`
    return `${hours}h ${minutes}m prima`
  }
  const days = Math.floor(offsetMinutes / (24 * 60))
  const remainingHours = Math.floor((offsetMinutes % (24 * 60)) / 60)
  if (remainingHours === 0) return `${days} giorni prima`
  return `${days}g ${remainingHours}h prima`
}

const handleSubmit = async () => {
  // Validate form
  const validation = validateTaskForm(formData.value)
  if (!validation.isValid) {
    formErrors.value = validation.errors
    return
  }

  try {
    if (isEditing.value && props.task) {
      // Update existing task
      const updateData = convertFormToUpdateRequest(formData.value)
      const updatedTask = await updateTask(props.task.id, updateData)
      if (updatedTask) {
        emit('task-updated', updatedTask)
        closeModal()
      }
    } else {
      // Create new task
      const createData = convertFormToTaskRequest(formData.value)
      const newTask = await createTask(createData)
      if (newTask) {
        emit('task-created', newTask)
        closeModal()
      }
    }
  } catch (error) {
    console.error('Error submitting form:', error)
  }
}

const handleDelete = () => {
  if (!props.task) return
  
  showConfirmation(
    `Sei sicuro di voler eliminare l'attività "${props.task.title}"?`,
    async () => {
      const success = await deleteTask(props.task!.id)
      if (success) {
        emit('task-deleted', props.task!.id)
        closeModal()
      }
    }
  )
}

// Watchers
watch(() => props.show, (show) => {
  if (show) {
    if (props.task) {
      loadTaskData()
    } else {
      resetForm()
    }
    
    // Focus on title input after modal is shown
    nextTick(() => {
      const titleInput = document.getElementById('title')
      if (titleInput) {
        titleInput.focus()
      }
    })
  }
}, { immediate: true })

watch(() => props.task, () => {
  if (props.show && props.task) {
    loadTaskData()
  }
})
</script>

<style scoped>
.modal-overlay {
  @apply fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4;
}

.modal-content {
  @apply relative bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full overflow-y-auto;
  @apply p-6;
  max-height: 90vh;
}

/* Custom scrollbar for webkit browsers */
.modal-content::-webkit-scrollbar {
  @apply w-2;
}

.modal-content::-webkit-scrollbar-track {
  @apply bg-gray-100 dark:bg-gray-700 rounded-full;
}

.modal-content::-webkit-scrollbar-thumb {
  @apply bg-gray-300 dark:bg-gray-600 rounded-full;
}

.modal-content::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400 dark:bg-gray-500;
}
</style>