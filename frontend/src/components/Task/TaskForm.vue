<template>
  <form 
    @submit.prevent="handleSubmit"
    :class="[
      'task-form space-y-6',
      containerClass
    ]"
    novalidate
  >
    <!-- Title Field -->
    <div>
      <label 
        for="task-title"
        :class="[
          'block text-sm font-medium mb-2',
          textClass
        ]"
      >
        Titolo <span class="text-red-500">*</span>
      </label>
      <input
        id="task-title"
        v-model="formData.title"
        type="text"
        maxlength="100"
        :placeholder="'Inserisci il titolo dell\'attività'"
        :class="[
          'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
          inputClass,
          errors.title ? 'border-red-500 focus:ring-red-500' : ''
        ]"
        :disabled="isLoading"
        @input="clearError('title')"
        @blur="validateField('title')"
        aria-describedby="title-error"
        required
      />
      <p v-if="errors.title" id="title-error" class="text-sm text-red-600 dark:text-red-400 mt-1">
        {{ errors.title }}
      </p>
      <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
        {{ formData.title.length }}/100 caratteri
      </p>
    </div>

    <!-- Description Field -->
    <div>
      <label 
        for="task-description"
        :class="[
          'block text-sm font-medium mb-2',
          textClass
        ]"
      >
        Descrizione
      </label>
      <textarea
        id="task-description"
        v-model="formData.description"
        rows="3"
        maxlength="500"
        :placeholder="'Aggiungi una descrizione (opzionale)'"
        :class="[
          'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors resize-none',
          inputClass,
          errors.description ? 'border-red-500 focus:ring-red-500' : ''
        ]"
        :disabled="isLoading"
        @input="clearError('description')"
        @blur="validateField('description')"
        aria-describedby="description-error"
      ></textarea>
      <p v-if="errors.description" id="description-error" class="text-sm text-red-600 dark:text-red-400 mt-1">
        {{ errors.description }}
      </p>
      <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
        {{ formData.description.length }}/500 caratteri
      </p>
    </div>

    <!-- Date and Time Row -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <!-- Start Date/Time -->
      <div>
        <DateTimePicker
          v-model:date-value="formData.startDate"
          v-model:time-value="formData.startTime"
          v-model:is-all-day="formData.isAllDay"
          label="Data e ora di inizio"
          :required="true"
          :error="errors.startDate || errors.startTime"
          :allow-all-day="true"
          :show-quick-times="true"
          :disabled="isLoading"
          @datetime-change="handleStartDateTimeChange"
        />
      </div>

      <!-- End Date/Time -->
      <div>
        <DateTimePicker
          v-model:date-value="formData.endDate"
          v-model:time-value="formData.endTime"
          v-model:is-all-day="formData.isAllDay"
          label="Data e ora di fine"
          :required="false"
          :error="errors.endDate || errors.endTime"
          :allow-all-day="true"
          :show-quick-times="true"
          :disabled="isLoading"
          :min-date="formData.startDate"
          @datetime-change="handleEndDateTimeChange"
        />
      </div>
    </div>

    <!-- Location Field -->
    <div>
      <label 
        for="task-location"
        :class="[
          'block text-sm font-medium mb-2',
          textClass
        ]"
      >
        Luogo
      </label>
      <input
        id="task-location"
        v-model="formData.location"
        type="text"
        maxlength="200"
        :placeholder="'Inserisci il luogo (opzionale)'"
        :class="[
          'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
          inputClass,
          errors.location ? 'border-red-500 focus:ring-red-500' : ''
        ]"
        :disabled="isLoading"
        @input="clearError('location')"
        @blur="validateField('location')"
        aria-describedby="location-error"
      />
      <p v-if="errors.location" id="location-error" class="text-sm text-red-600 dark:text-red-400 mt-1">
        {{ errors.location }}
      </p>
      <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
        {{ formData.location.length }}/200 caratteri
      </p>
    </div>

    <!-- Priority and Color Row -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <!-- Priority -->
      <div>
        <label 
          for="task-priority"
          :class="[
            'block text-sm font-medium mb-2',
            textClass
          ]"
        >
          Priorità
        </label>
        <select
          id="task-priority"
          v-model="formData.priority"
          :class="[
            'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent',
            inputClass
          ]"
          :disabled="isLoading"
          @change="clearError('priority')"
        >
          <option 
            v-for="(config, priority) in TASK_PRIORITY_CONFIG"
            :key="priority"
            :value="priority"
          >
            {{ config.label }}
          </option>
        </select>
      </div>

      <!-- Color -->
      <div>
        <ColorPicker
          v-model="formData.color"
          label="Colore"
          :allow-custom="true"
          :show-recent-colors="true"
          :error="errors.color"
          @color-selected="handleColorSelected"
        />
      </div>
    </div>

    <!-- Reminders Section -->
    <div>
      <div class="flex items-center justify-between mb-3">
        <label :class="['text-sm font-medium', textClass]">
          Promemoria
        </label>
        <button
          type="button"
          @click="addReminder"
          :disabled="isLoading || formData.reminders.length >= 5"
          :class="[
            'px-3 py-1 text-xs border rounded-md transition-colors',
            'text-blue-600 dark:text-blue-400 border-blue-300 dark:border-blue-600',
            'hover:bg-blue-50 dark:hover:bg-blue-900/20',
            'disabled:opacity-50 disabled:cursor-not-allowed',
            'focus:outline-none focus:ring-2 focus:ring-blue-500'
          ]"
          title="Aggiungi promemoria"
        >
          <PlusIcon class="w-3 h-3 inline mr-1" />
          Aggiungi
        </button>
      </div>

      <!-- Reminder Items -->
      <div v-if="formData.reminders.length > 0" class="space-y-3">
        <div 
          v-for="(reminder, index) in formData.reminders"
          :key="`reminder-${index}`"
          :class="[
            'flex items-center gap-3 p-3 border rounded-lg',
            cardClass,
            'border-gray-200 dark:border-gray-700'
          ]"
        >
          <div class="flex-1 grid grid-cols-2 gap-2">
            <input
              v-model="reminder.date"
              type="date"
              :class="[
                'px-2 py-1 text-sm border rounded focus:outline-none focus:ring-1 focus:ring-blue-500',
                inputClass
              ]"
              :disabled="isLoading"
              @change="validateReminder(index)"
            />
            <input
              v-if="!formData.isAllDay"
              v-model="reminder.time"
              type="time"
              :class="[
                'px-2 py-1 text-sm border rounded focus:outline-none focus:ring-1 focus:ring-blue-500',
                inputClass
              ]"
              :disabled="isLoading"
              @change="validateReminder(index)"
            />
          </div>
          
          <button
            type="button"
            @click="removeReminder(index)"
            :disabled="isLoading"
            :class="[
              'p-1 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors',
              'focus:outline-none focus:ring-2 focus:ring-red-500'
            ]"
            :title="'Rimuovi promemoria'"
          >
            <TrashIcon class="w-4 h-4" />
          </button>
        </div>
      </div>

      <!-- No reminders state -->
      <p v-else class="text-sm text-gray-500 dark:text-gray-400 italic">
        Nessun promemoria impostato
      </p>

      <!-- Reminder presets -->
      <div v-if="showReminderPresets" class="mt-3">
        <p class="text-xs font-medium text-gray-600 dark:text-gray-400 mb-2">
          Promemoria rapidi:
        </p>
        <div class="flex flex-wrap gap-1">
          <button
            v-for="preset in reminderPresets"
            :key="preset.label"
            type="button"
            @click="addReminderPreset(preset.minutes)"
            :disabled="isLoading || formData.reminders.length >= 5"
            :class="[
              'px-2 py-1 text-xs border rounded transition-colors',
              'text-gray-600 dark:text-gray-300 border-gray-300 dark:border-gray-600',
              'hover:bg-gray-50 dark:hover:bg-gray-700',
              'disabled:opacity-50 disabled:cursor-not-allowed'
            ]"
          >
            {{ preset.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- Conflict Warning -->
    <div v-if="hasConflict" :class="[
      'p-3 rounded-lg border-l-4 border-yellow-400 bg-yellow-50 dark:bg-yellow-900/20'
    ]">
      <div class="flex">
        <ExclamationTriangleIcon class="w-5 h-5 text-yellow-400 mt-0.5 mr-2" />
        <div>
          <h4 class="text-sm font-medium text-yellow-800 dark:text-yellow-300">
            Possibile conflitto
          </h4>
          <p class="text-sm text-yellow-700 dark:text-yellow-400 mt-1">
            {{ conflictMessage }}
          </p>
        </div>
      </div>
    </div>

    <!-- Form Actions -->
    <div class="flex justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
      <button
        type="button"
        @click="handleCancel"
        :disabled="isLoading"
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
        type="submit"
        :disabled="isLoading || !isFormValid"
        :class="[
          'px-4 py-2 text-sm font-medium rounded-lg transition-colors',
          'bg-blue-600 text-white hover:bg-blue-700',
          'focus:outline-none focus:ring-2 focus:ring-blue-500',
          'disabled:opacity-50 disabled:cursor-not-allowed flex items-center'
        ]"
      >
        <svg
          v-if="isLoading"
          class="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        {{ isLoading ? 'Salvando...' : (isEditMode ? 'Aggiorna' : 'Crea attività') }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useTasks } from '../../composables/useTasks'
import { useTheme } from '../../composables/useTheme'
import { format, addMinutes, subMinutes, parseISO } from 'date-fns'
import { 
  TASK_PRIORITY_CONFIG, 
  VALIDATION_MESSAGES,
  TaskPriority 
} from '../../types/task'
import type { 
  Task, 
  TaskFormData, 
  CreateTaskRequest, 
  UpdateTaskRequest,
  ReminderFormData 
} from '../../types/task'

// Components
import DateTimePicker from './DateTimePicker.vue'
import ColorPicker from './ColorPicker.vue'

// Icons
import { 
  PlusIcon, 
  TrashIcon, 
  ExclamationTriangleIcon 
} from '@heroicons/vue/24/outline'

// Props
interface Props {
  task?: Task
  initialDate?: string
  initialTime?: string
  containerClass?: string
  showReminderPresets?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  containerClass: '',
  showReminderPresets: true
})

// Emits
const emit = defineEmits<{
  'submit': [data: CreateTaskRequest | UpdateTaskRequest]
  'cancel': []
  'success': [task: Task]
  'error': [error: string]
}>()

// Composables
const { 
  createTask, 
  updateTask, 
  isFormLoading,
  validateTaskForm,
  getTasksForDate
} = useTasks()
const { textClass, inputClass, cardClass } = useTheme()

// State
const formData = ref<TaskFormData>({
  title: '',
  description: '',
  priority: TaskPriority.MEDIUM,
  dueDate: props.initialDate || format(new Date(), 'yyyy-MM-dd'),
  dueTime: props.initialTime || '09:00',
  startDate: props.initialDate || format(new Date(), 'yyyy-MM-dd'),
  startTime: props.initialTime || '09:00',
  endDate: props.initialDate || format(new Date(), 'yyyy-MM-dd'),
  endTime: props.initialTime || '10:00',
  location: '',
  color: '#3b82f6',
  isAllDay: false,
  reminders: []
})

const errors = ref<Record<string, string>>({})
const hasConflict = ref(false)
const conflictMessage = ref('')

// Reminder presets (in minutes before task)
const reminderPresets = [
  { label: '5 min', minutes: 5 },
  { label: '15 min', minutes: 15 },
  { label: '30 min', minutes: 30 },
  { label: '1 ora', minutes: 60 },
  { label: '1 giorno', minutes: 1440 }
]

// Computed
const isEditMode = computed(() => !!props.task)
const isLoading = computed(() => isFormLoading.value)

const isFormValid = computed(() => {
  return formData.value.title.trim().length > 0 &&
         Object.keys(errors.value).length === 0 &&
         !isLoading.value
})

// Initialize form with task data if editing
watch(
  () => props.task,
  (task) => {
    if (task) {
      formData.value = {
        title: task.title,
        description: task.description || '',
        priority: task.priority,
        dueDate: task.dueDate ? format(new Date(task.dueDate), 'yyyy-MM-dd') : '',
        dueTime: task.dueDate ? format(new Date(task.dueDate), 'HH:mm') : '09:00',
        startDate: task.startDate ? format(new Date(task.startDate), 'yyyy-MM-dd') : 
                   (task.dueDate ? format(new Date(task.dueDate), 'yyyy-MM-dd') : format(new Date(), 'yyyy-MM-dd')),
        startTime: task.startDate ? format(new Date(task.startDate), 'HH:mm') : '09:00',
        endDate: task.endDate ? format(new Date(task.endDate), 'yyyy-MM-dd') : 
                 (task.dueDate ? format(new Date(task.dueDate), 'yyyy-MM-dd') : format(new Date(), 'yyyy-MM-dd')),
        endTime: task.endDate ? format(new Date(task.endDate), 'HH:mm') : '10:00',
        location: task.location || '',
        color: task.color || '#3b82f6',
        isAllDay: task.isAllDay || false,
        reminders: task.reminders.map(reminder => ({
          id: reminder.id,
          date: format(new Date(reminder.reminderDateTime), 'yyyy-MM-dd'),
          time: format(new Date(reminder.reminderDateTime), 'HH:mm'),
          reminderDateTime: reminder.reminderDateTime
        }))
      }
    }
  },
  { immediate: true }
)

// Watch for date/time changes to check conflicts
watch(
  [() => formData.value.startDate, () => formData.value.startTime, 
   () => formData.value.endDate, () => formData.value.endTime],
  () => {
    nextTick(() => {
      checkConflicts()
    })
  }
)

// Methods
const validateField = (fieldName: keyof TaskFormData) => {
  const validation = validateTaskForm(formData.value)
  
  if (validation.errors[fieldName]) {
    errors.value[fieldName] = validation.errors[fieldName]
  } else {
    delete errors.value[fieldName]
  }
  
  return !validation.errors[fieldName]
}

const clearError = (fieldName: string) => {
  delete errors.value[fieldName]
}

const validateForm = (): boolean => {
  const validation = validateTaskForm(formData.value)
  errors.value = validation.errors
  
  // Additional validation
  if (formData.value.endDate && formData.value.startDate) {
    const startDateTime = new Date(`${formData.value.startDate}T${formData.value.startTime || '00:00'}`)
    const endDateTime = new Date(`${formData.value.endDate}T${formData.value.endTime || '23:59'}`)
    
    if (endDateTime <= startDateTime) {
      errors.value.endDate = VALIDATION_MESSAGES.endBeforeStart
    }
  }
  
  return validation.isValid && Object.keys(errors.value).length === 0
}

const handleStartDateTimeChange = (datetime: string) => {
  // Auto-adjust end time if needed
  if (formData.value.endDate === formData.value.startDate) {
    const startTime = formData.value.startTime
    if (startTime) {
      const startMinutes = parseInt(startTime.split(':')[0]) * 60 + parseInt(startTime.split(':')[1])
      const endMinutes = startMinutes + 60 // Default 1 hour duration
      const endHours = Math.floor(endMinutes / 60) % 24
      const endMins = endMinutes % 60
      formData.value.endTime = `${endHours.toString().padStart(2, '0')}:${endMins.toString().padStart(2, '0')}`
    }
  }
}

const handleEndDateTimeChange = (datetime: string) => {
  // Validation will be handled by watchers
}

const handleColorSelected = (color: string, colorName: string) => {
  formData.value.color = color
  clearError('color')
}

const addReminder = () => {
  if (formData.value.reminders.length >= 5) return
  
  const reminderDate = formData.value.startDate || formData.value.dueDate
  const reminderTime = formData.value.isAllDay ? '09:00' : (formData.value.startTime || '09:00')
  
  formData.value.reminders.push({
    date: reminderDate,
    time: reminderTime
  })
}

const removeReminder = (index: number) => {
  formData.value.reminders.splice(index, 1)
}

const addReminderPreset = (minutesBefore: number) => {
  if (formData.value.reminders.length >= 5) return
  
  const startDateTime = formData.value.startDate && formData.value.startTime
    ? parseISO(`${formData.value.startDate}T${formData.value.startTime}:00`)
    : parseISO(`${formData.value.dueDate}T${formData.value.dueTime}:00`)
  
  const reminderDateTime = subMinutes(startDateTime, minutesBefore)
  
  formData.value.reminders.push({
    date: format(reminderDateTime, 'yyyy-MM-dd'),
    time: format(reminderDateTime, 'HH:mm'),
    reminderDateTime: reminderDateTime.toISOString()
  })
}

const validateReminder = (index: number) => {
  const reminder = formData.value.reminders[index]
  if (reminder.date && reminder.time && !formData.value.isAllDay) {
    reminder.reminderDateTime = `${reminder.date}T${reminder.time}:00`
  }
}

const checkConflicts = async () => {
  if (!formData.value.startDate) return
  
  const tasksOnDate = getTasksForDate(new Date(formData.value.startDate))
  const currentTaskId = props.task?.id
  
  const conflictingTasks = tasksOnDate.filter(task => {
    if (currentTaskId && task.id === currentTaskId) return false
    
    if (task.isAllDay || formData.value.isAllDay) {
      return true // All-day tasks always conflict
    }
    
    if (!task.startDate || !task.endDate) return false
    
    const taskStart = new Date(`${task.startDate}T${task.startTime || '00:00'}`)
    const taskEnd = new Date(`${task.endDate}T${task.endTime || '23:59'}`)
    const formStart = new Date(`${formData.value.startDate}T${formData.value.startTime}`)
    const formEnd = new Date(`${formData.value.endDate}T${formData.value.endTime}`)
    
    // Check for overlap
    return (formStart < taskEnd && formEnd > taskStart)
  })
  
  if (conflictingTasks.length > 0) {
    hasConflict.value = true
    const taskTitles = conflictingTasks.map(t => t.title).join(', ')
    conflictMessage.value = `Questo orario si sovrappone con: ${taskTitles}`
  } else {
    hasConflict.value = false
    conflictMessage.value = ''
  }
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }
  
  const taskData = isEditMode.value ? 
    convertToUpdateRequest(formData.value) : 
    convertToCreateRequest(formData.value)
  
  emit('submit', taskData)
  
  try {
    let result
    if (isEditMode.value && props.task) {
      result = await updateTask(props.task.id, taskData as UpdateTaskRequest)
    } else {
      result = await createTask(taskData as CreateTaskRequest)
    }
    
    if (result) {
      emit('success', result)
    } else {
      emit('error', 'Errore nel salvataggio dell\'attività')
    }
  } catch (error: any) {
    emit('error', error.message || 'Errore nel salvataggio dell\'attività')
  }
}

const handleCancel = () => {
  emit('cancel')
}

const convertToCreateRequest = (data: TaskFormData): CreateTaskRequest => {
  return {
    title: data.title.trim(),
    description: data.description.trim() || undefined,
    priority: data.priority,
    dueDate: data.dueDate && data.dueTime ? `${data.dueDate}T${data.dueTime}:00` : undefined,
    startDate: data.startDate && data.startTime ? `${data.startDate}T${data.startTime}:00` : undefined,
    endDate: data.endDate && data.endTime ? `${data.endDate}T${data.endTime}:00` : undefined,
    location: data.location.trim() || undefined,
    color: data.color,
    isAllDay: data.isAllDay,
    reminders: data.reminders.map(reminder => ({
      reminderDateTime: reminder.reminderDateTime || `${reminder.date}T${reminder.time}:00`
    }))
  }
}

const convertToUpdateRequest = (data: TaskFormData): UpdateTaskRequest => {
  return {
    title: data.title.trim(),
    description: data.description.trim() || undefined,
    priority: data.priority,
    dueDate: data.dueDate && data.dueTime ? `${data.dueDate}T${data.dueTime}:00` : undefined,
    startDate: data.startDate && data.startTime ? `${data.startDate}T${data.startTime}:00` : undefined,
    endDate: data.endDate && data.endTime ? `${data.endDate}T${data.endTime}:00` : undefined,
    location: data.location.trim() || undefined,
    color: data.color,
    isAllDay: data.isAllDay
  }
}

const resetForm = () => {
  formData.value = {
    title: '',
    description: '',
    priority: TaskPriority.MEDIUM,
    dueDate: format(new Date(), 'yyyy-MM-dd'),
    dueTime: '09:00',
    startDate: format(new Date(), 'yyyy-MM-dd'),
    startTime: '09:00',
    endDate: format(new Date(), 'yyyy-MM-dd'),
    endTime: '10:00',
    location: '',
    color: '#3b82f6',
    isAllDay: false,
    reminders: []
  }
  errors.value = {}
}

// Expose methods for parent components
defineExpose({
  validateForm,
  resetForm,
  formData: computed(() => formData.value),
  isFormValid: computed(() => isFormValid.value),
  isLoading: computed(() => isLoading.value)
})

// Focus title field on mount
onMounted(() => {
  nextTick(() => {
    const titleInput = document.getElementById('task-title') as HTMLInputElement
    if (titleInput) {
      titleInput.focus()
    }
  })
})
</script>

<style scoped>
.task-form {
  /* Custom styles if needed */
}

/* Loading spinner animation */
@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* Focus styles for better accessibility */
.task-form input:focus,
.task-form textarea:focus,
.task-form select:focus {
  outline: none;
}

/* Improved spacing for mobile */
@media (max-width: 768px) {
  .task-form .grid {
    grid-template-columns: 1fr;
  }
}

/* Animation for form validation errors */
.task-form .text-red-600,
.task-form .text-red-400 {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>