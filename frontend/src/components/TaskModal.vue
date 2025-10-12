<template>
  <div v-if="show" class="modal-overlay" @click="handleBackdropClick">
    <div class="modal-content" @click.stop>
      <!-- Modal Header -->
      <div class="flex items-center justify-between mb-6">
        <h3 class="text-lg font-medium text-gray-900 dark:text-white">
          {{ isEditing ? t('tasks.editTask') : t('tasks.createNew') }}
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
            {{ t('tasks.taskTitle') }} <span class="text-red-500">*</span>
          </label>
          <input
            id="title"
            v-model="formData.title"
            type="text"
            :disabled="isFormLoading"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm placeholder-gray-400 dark:placeholder-gray-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.title }"
            :placeholder="t('tasks.taskTitlePlaceholder')"
          />
          <p v-if="formErrors.title" class="mt-1 text-sm text-red-500">{{ formErrors.title }}</p>
        </div>

        <!-- Description -->
        <div>
          <label for="description" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            {{ t('tasks.description') }}
          </label>
          <textarea
            id="description"
            v-model="formData.description"
            :disabled="isFormLoading"
            rows="3"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm placeholder-gray-400 dark:placeholder-gray-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.description }"
            :placeholder="t('tasks.descriptionPlaceholder')"
          />
          <p v-if="formErrors.description" class="mt-1 text-sm text-red-500">{{ formErrors.description }}</p>
        </div>

        <!-- Warning banner for editing all occurrences -->
        <div v-if="isEditingAllOccurrences" class="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md">
          <div class="flex items-start gap-2">
            <svg class="h-5 w-5 text-blue-600 dark:text-blue-400 mt-0.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div class="text-sm text-blue-800 dark:text-blue-200">
              <p class="font-medium">{{ t('tasks.editingAllOccurrences') }}</p>
              <p class="mt-1">{{ t('tasks.editingAllOccurrencesDescription') }}</p>
            </div>
          </div>
        </div>

        <!-- Start Date and Time -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label for="startDate" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('tasks.startDate') }} <span class="text-red-500">*</span>
            </label>
            <input
              id="startDate"
              v-model="formData.startDate"
              type="date"
              :disabled="isFormLoading"
              :readonly="isEditingAllOccurrences"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.startDate, 'bg-gray-50 dark:bg-gray-800 cursor-not-allowed': isEditingAllOccurrences }"
            />
            <p v-if="formErrors.startDate" class="mt-1 text-sm text-red-500">{{ formErrors.startDate }}</p>
          </div>
          
          <div>
            <label for="startTime" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('tasks.startTime') }}
              <span class="text-xs text-gray-500 dark:text-gray-400 font-normal">
                ({{ settings.timeFormat === '24h' ? '24h: 15:30' : '12h: 3:30 PM' }})
              </span>
            </label>
            <input
              id="startTime"
              v-model="formData.startTime"
              type="time"
              :disabled="isFormLoading"
              :readonly="isEditingAllOccurrences"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.startTime, 'bg-gray-50 dark:bg-gray-800 cursor-not-allowed': isEditingAllOccurrences }"
            />
            <p v-if="formErrors.startTime" class="mt-1 text-sm text-red-500">{{ formErrors.startTime }}</p>
          </div>
        </div>

        <!-- End Date and Time -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label for="endDate" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('tasks.endDate') }} <span class="text-red-500">*</span>
            </label>
            <input
              id="endDate"
              v-model="formData.endDate"
              type="date"
              :disabled="isFormLoading"
              :readonly="isEditingAllOccurrences"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.endDate, 'bg-gray-50 dark:bg-gray-800 cursor-not-allowed': isEditingAllOccurrences }"
            />
            <p v-if="formErrors.endDate" class="mt-1 text-sm text-red-500">{{ formErrors.endDate }}</p>
          </div>
          
          <div>
            <label for="endTime" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('tasks.endTime') }}
              <span class="text-xs text-gray-500 dark:text-gray-400 font-normal">
                ({{ settings.timeFormat === '24h' ? '24h: 16:00' : '12h: 4:00 PM' }})
              </span>
            </label>
            <input
              id="endTime"
              v-model="formData.endTime"
              type="time"
              :disabled="isFormLoading"
              :readonly="isEditingAllOccurrences"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.endTime, 'bg-gray-50 dark:bg-gray-800 cursor-not-allowed': isEditingAllOccurrences }"
            />
            <p v-if="formErrors.endTime" class="mt-1 text-sm text-red-500">{{ formErrors.endTime }}</p>
          </div>
        </div>

        <!-- Location -->
        <div>
          <label for="location" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            {{ t('tasks.location') }}
          </label>
          <input
            id="location"
            v-model="formData.location"
            type="text"
            :disabled="isFormLoading"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm placeholder-gray-400 dark:placeholder-gray-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            :class="{ 'border-red-500 focus:ring-red-500 focus:border-red-500': formErrors.location }"
            :placeholder="t('tasks.locationPlaceholder')"
          />
          <p v-if="formErrors.location" class="mt-1 text-sm text-red-500">{{ formErrors.location }}</p>
        </div>

        <!-- Color -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            {{ t('tasks.color') }}
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

        <!-- Recurrence Section (hidden when editing single occurrence) -->
        <div v-if="!isEditingSingleOccurrence">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            {{ t('tasks.recurrence') }}
          </label>
          <RecurrenceInput
            v-model="formData"
            :start-date="formData.startDate"
          />
        </div>

        <!-- Reminders Section -->
        <div>
          <div class="flex items-center justify-between mb-3">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('tasks.reminders') }}
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
              {{ t('tasks.addReminder') }}
            </button>
          </div>

          <div v-if="formData.reminders.length > 0" class="space-y-3">
            <div v-for="(reminder, index) in formData.reminders" :key="index"
              class="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-md">
              <div class="grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
                <!-- Notification Type -->
                <div>
                  <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                    {{ t('tasks.reminderType') }}
                  </label>
                  <select
                    v-model="reminder.notificationType"
                    :disabled="isFormLoading"
                    class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:opacity-50"
                  >
                    <option value="PUSH">{{ t('tasks.reminderTypeNotification') }}</option>
                    <option value="EMAIL">{{ t('tasks.reminderTypeEmail') }}</option>
                  </select>
                </div>

                <!-- Offset Value -->
                <div>
                  <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                    {{ t('tasks.reminderValue') }}
                  </label>
                  <input
                    v-model.number="reminder.offsetValue"
                    type="number"
                    min="1"
                    :disabled="isFormLoading"
                    class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:opacity-50"
                  />
                </div>

                <!-- Time Unit -->
                <div>
                  <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                    {{ t('tasks.reminderUnit') }}
                  </label>
                  <select
                    v-model="reminder.offsetUnit"
                    :disabled="isFormLoading"
                    class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:opacity-50"
                  >
                    <option value="minutes">{{ t('tasks.reminderUnitMinutes') }}</option>
                    <option value="hours">{{ t('tasks.reminderUnitHours') }}</option>
                    <option value="days">{{ t('tasks.reminderUnitDays') }}</option>
                  </select>
                </div>

                <!-- Delete Button -->
                <div>
                  <button
                    type="button"
                    @click="removeReminder(index)"
                    :disabled="isFormLoading"
                    class="w-full px-3 py-2 text-sm text-red-600 dark:text-red-400 border border-red-300 dark:border-red-600 rounded hover:bg-red-50 dark:hover:bg-red-900/20 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                    {{ t('tasks.removeReminder') }}
                  </button>
                </div>
              </div>
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
            {{ t('common.cancel') }}
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
            {{ t('tasks.deleteTask') }}
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
            {{ isEditing ? t('tasks.updateTask') : t('tasks.createNew') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTasks } from '../composables/useTasks'
import { useCustomToast } from '../composables/useCustomToast'
import { useSettingsStore } from '../stores/settings'
import { NotificationType, NOTIFICATION_TYPE_CONFIG, CALENDAR_COLORS, RecurrenceFrequency, RecurrenceEndType, type Task, type TaskFormData } from '../types/task'
import { format } from 'date-fns'
import RecurrenceInput from './Tasks/RecurrenceInput.vue'

// Composables
const { t } = useI18n()

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

// Settings store for time format
const settings = useSettingsStore()

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

const isEditingAllOccurrences = computed(() => {
  return props.task && (props.task as any)._editMode === 'all' && props.task.recurrenceRule
})

const isEditingSingleOccurrence = computed(() => {
  return props.task && (props.task as any)._editMode === 'single' && props.task.recurrenceRule
})

const isFormValid = computed(() => {
  return formData.value.title.trim().length > 0
})

const notificationTypes = computed(() => NOTIFICATION_TYPE_CONFIG)

const calendarColors = computed(() => CALENDAR_COLORS)



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

    // If editing single occurrence, force non-recurring
    if (isEditingSingleOccurrence.value) {
      formData.value.isRecurring = false
      formData.value.recurrenceFrequency = 'daily'
      formData.value.recurrenceInterval = 1
      formData.value.recurrenceEndType = 'never'
      formData.value.recurrenceCount = undefined
      formData.value.recurrenceEndDate = undefined
      formData.value.recurrenceByDay = []
    }
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
    offsetValue: 10,
    offsetUnit: 'minutes',
    notificationType: NotificationType.PUSH,
    // Computed property for backward compatibility
    get offsetMinutes() {
      const multipliers = { minutes: 1, hours: 60, days: 24 * 60 }
      return this.offsetValue * multipliers[this.offsetUnit]
    }
  }
  formData.value.reminders.push(newReminder)
}

const removeReminder = (index: number) => {
  formData.value.reminders.splice(index, 1)
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

      // Check if this is a single occurrence edit (stored in _editMode by CalendarView)
      const editMode = (props.task as any)._editMode
      const occurrenceStart = editMode === 'single' ? props.task.startDatetime : undefined

      const updatedTask = await updateTask(props.task.id, updateData, occurrenceStart)
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
    t('tasks.deleteConfirm', { title: props.task.title }),
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