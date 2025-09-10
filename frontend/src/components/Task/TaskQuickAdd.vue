<template>
  <div class="task-quick-add">
    <!-- Collapsed State: Add Button -->
    <div v-if="!isExpanded" class="flex justify-center">
      <button
        @click="expand"
        :class="[
          'group flex items-center px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200',
          'border-2 border-dashed border-gray-300 dark:border-gray-600',
          'text-gray-600 dark:text-gray-400',
          'hover:border-blue-400 dark:hover:border-blue-500',
          'hover:text-blue-600 dark:hover:text-blue-400',
          'hover:bg-blue-50 dark:hover:bg-blue-900/10',
          'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent',
          buttonClass
        ]"
        :title="buttonTitle"
        :aria-label="buttonTitle"
      >
        <PlusIcon class="w-4 h-4 mr-2 group-hover:scale-110 transition-transform" />
        {{ buttonText }}
      </button>
    </div>

    <!-- Expanded State: Quick Form -->
    <div
      v-else
      :class="[
        'quick-form rounded-lg border p-4 transition-all duration-300',
        cardClass,
        'border-gray-200 dark:border-gray-700',
        'shadow-sm'
      ]"
    >
      <form @submit.prevent="handleSubmit" class="space-y-3">
        <!-- Title Input -->
        <div>
          <input
            ref="titleInputRef"
            v-model="formData.title"
            type="text"
            :placeholder="titlePlaceholder"
            :class="[
              'w-full px-3 py-2 text-sm border rounded-lg',
              'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent',
              'transition-colors',
              inputClass,
              errors.title ? 'border-red-500 focus:ring-red-500' : ''
            ]"
            :disabled="isLoading"
            maxlength="100"
            required
            @keydown.esc="collapse"
            @input="clearError('title')"
          />
          <p v-if="errors.title" class="text-xs text-red-600 dark:text-red-400 mt-1">
            {{ errors.title }}
          </p>
        </div>

        <!-- Quick Options Row -->
        <div class="flex flex-wrap items-center gap-2 text-sm">
          <!-- Date/Time Selector -->
          <div class="flex items-center space-x-2">
            <CalendarDaysIcon class="w-4 h-4 text-gray-400" />
            <input
              v-model="formData.date"
              type="date"
              :class="[
                'px-2 py-1 text-xs border rounded focus:outline-none focus:ring-1 focus:ring-blue-500',
                inputClass
              ]"
              :disabled="isLoading"
              @change="validateDate"
            />
            <input
              v-if="!formData.isAllDay"
              v-model="formData.time"
              type="time"
              :class="[
                'px-2 py-1 text-xs border rounded focus:outline-none focus:ring-1 focus:ring-blue-500',
                inputClass
              ]"
              :disabled="isLoading"
            />
          </div>

          <!-- All Day Toggle -->
          <label class="flex items-center space-x-1 cursor-pointer">
            <input
              v-model="formData.isAllDay"
              type="checkbox"
              class="w-3 h-3 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              :disabled="isLoading"
            />
            <span :class="['text-xs', textClass]">Tutto il giorno</span>
          </label>

          <!-- Priority Selector -->
          <div class="flex items-center space-x-1">
            <ExclamationTriangleIcon class="w-4 h-4 text-gray-400" />
            <select
              v-model="formData.priority"
              :class="[
                'px-2 py-1 text-xs border rounded focus:outline-none focus:ring-1 focus:ring-blue-500',
                inputClass
              ]"
              :disabled="isLoading"
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

          <!-- Color Picker (Mini) -->
          <div class="flex items-center space-x-1">
            <div
              class="w-4 h-4 rounded-full border border-gray-300 dark:border-gray-600"
              :style="{ backgroundColor: formData.color }"
            ></div>
            <Popover class="relative">
              <PopoverButton
                :class="[
                  'text-xs text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200',
                  'focus:outline-none'
                ]"
                :disabled="isLoading"
              >
                Colore
              </PopoverButton>

              <transition
                enter-active-class="transition duration-200 ease-out"
                enter-from-class="translate-y-1 opacity-0"
                enter-to-class="translate-y-0 opacity-100"
                leave-active-class="transition duration-150 ease-in"
                leave-from-class="translate-y-0 opacity-100"
                leave-to-class="translate-y-1 opacity-0"
              >
                <PopoverPanel 
                  :class="[
                    'absolute z-20 mt-2 p-3 rounded-lg shadow-lg border',
                    cardClass,
                    'border-gray-200 dark:border-gray-700'
                  ]"
                >
                  <div class="grid grid-cols-5 gap-1">
                    <button
                      v-for="color in CALENDAR_COLORS.slice(0, 10)"
                      :key="color.value"
                      type="button"
                      @click="formData.color = color.value"
                      :class="[
                        'w-6 h-6 rounded border-2 transition-transform hover:scale-110',
                        formData.color === color.value
                          ? 'border-gray-800 dark:border-gray-200 ring-1 ring-blue-500'
                          : 'border-gray-300 dark:border-gray-600'
                      ]"
                      :style="{ backgroundColor: color.value }"
                      :title="color.name"
                    ></button>
                  </div>
                </PopoverPanel>
              </transition>
            </Popover>
          </div>
        </div>

        <!-- Smart Suggestions (if enabled) -->
        <div v-if="showSuggestions && suggestions.length > 0" class="border-t pt-2">
          <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Suggerimenti:</p>
          <div class="flex flex-wrap gap-1">
            <button
              v-for="suggestion in suggestions"
              :key="suggestion.text"
              type="button"
              @click="applySuggestion(suggestion)"
              :class="[
                'px-2 py-1 text-xs rounded border transition-colors',
                'text-gray-600 dark:text-gray-300 border-gray-300 dark:border-gray-600',
                'hover:bg-gray-50 dark:hover:bg-gray-700 hover:border-gray-400'
              ]"
              :disabled="isLoading"
            >
              {{ suggestion.text }}
            </button>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex justify-between items-center pt-2 border-t border-gray-200 dark:border-gray-700">
          <div class="flex items-center space-x-2">
            <!-- Expand to Full Form -->
            <button
              type="button"
              @click="expandToFullForm"
              :class="[
                'text-xs text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300',
                'underline focus:outline-none'
              ]"
              :disabled="isLoading"
            >
              Più opzioni
            </button>

            <!-- Keyboard Shortcut Hint -->
            <span class="text-xs text-gray-400">
              Ctrl+Enter per salvare
            </span>
          </div>

          <div class="flex items-center space-x-2">
            <!-- Cancel -->
            <button
              type="button"
              @click="collapse"
              :disabled="isLoading"
              :class="[
                'px-3 py-1.5 text-xs border rounded transition-colors',
                'text-gray-600 dark:text-gray-300 border-gray-300 dark:border-gray-600',
                'hover:bg-gray-50 dark:hover:bg-gray-700',
                'focus:outline-none focus:ring-1 focus:ring-blue-500',
                'disabled:opacity-50 disabled:cursor-not-allowed'
              ]"
            >
              Annulla
            </button>

            <!-- Save -->
            <button
              type="submit"
              :disabled="isLoading || !isFormValid"
              :class="[
                'px-3 py-1.5 text-xs font-medium rounded transition-colors',
                'bg-blue-600 text-white hover:bg-blue-700',
                'focus:outline-none focus:ring-1 focus:ring-blue-500',
                'disabled:opacity-50 disabled:cursor-not-allowed',
                'flex items-center'
              ]"
            >
              <svg
                v-if="isLoading"
                class="animate-spin -ml-1 mr-1 h-3 w-3 text-white"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              {{ isLoading ? 'Salvando...' : 'Crea' }}
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { useTasks } from '../../composables/useTasks'
import { useTheme } from '../../composables/useTheme'
import { format } from 'date-fns'
import { 
  TASK_PRIORITY_CONFIG,
  CALENDAR_COLORS,
  TaskPriority,
  VALIDATION_MESSAGES
} from '../../types/task'
import type { 
  Task,
  CreateTaskRequest
} from '../../types/task'
import { Popover, PopoverButton, PopoverPanel } from '@headlessui/vue'

// Icons
import {
  PlusIcon,
  CalendarDaysIcon,
  ExclamationTriangleIcon
} from '@heroicons/vue/24/outline'

// Props
interface Props {
  initialDate?: string
  initialTime?: string
  buttonText?: string
  buttonTitle?: string
  titlePlaceholder?: string
  autoExpand?: boolean
  showSuggestions?: boolean
  contextDate?: string
}

const props = withDefaults(defineProps<Props>(), {
  buttonText: 'Nuova attività',
  buttonTitle: 'Crea una nuova attività',
  titlePlaceholder: 'Cosa devi fare?',
  autoExpand: false,
  showSuggestions: true
})

// Emits
const emit = defineEmits<{
  'task-created': [task: Task]
  'expand-full-form': [initialData: Partial<CreateTaskRequest>]
  'expand': []
  'collapse': []
}>()

// Composables
const { 
  createTask, 
  isFormLoading,
  validateTaskData
} = useTasks()
const { textClass, inputClass, cardClass, buttonClass } = useTheme()

// State
const isExpanded = ref(props.autoExpand)
const titleInputRef = ref<HTMLInputElement | null>(null)

const formData = ref({
  title: '',
  date: props.initialDate || format(new Date(), 'yyyy-MM-dd'),
  time: props.initialTime || '09:00',
  isAllDay: false,
  priority: TaskPriority.MEDIUM,
  color: CALENDAR_COLORS[0].value
})

const errors = ref<Record<string, string>>({})

// Smart suggestions
const suggestions = ref<Array<{text: string, priority?: TaskPriority, color?: string}>>([])

// Computed
const isLoading = computed(() => isFormLoading.value)

const isFormValid = computed(() => {
  return formData.value.title.trim().length > 0 && Object.keys(errors.value).length === 0
})

// Watch for context changes
watch(
  () => props.contextDate,
  (newDate) => {
    if (newDate && !isExpanded.value) {
      formData.value.date = newDate
    }
  }
)

watch(
  () => props.initialDate,
  (newDate) => {
    if (newDate) {
      formData.value.date = newDate
    }
  }
)

watch(
  () => props.initialTime,
  (newTime) => {
    if (newTime) {
      formData.value.time = newTime
    }
  }
)

// Watch title for smart suggestions
watch(
  () => formData.value.title,
  (newTitle) => {
    if (props.showSuggestions) {
      generateSuggestions(newTitle)
    }
  }
)

// Methods
const expand = async () => {
  isExpanded.value = true
  emit('expand')
  
  await nextTick()
  if (titleInputRef.value) {
    titleInputRef.value.focus()
  }
}

const collapse = () => {
  isExpanded.value = false
  resetForm()
  emit('collapse')
}

const resetForm = () => {
  formData.value = {
    title: '',
    date: props.initialDate || props.contextDate || format(new Date(), 'yyyy-MM-dd'),
    time: props.initialTime || '09:00',
    isAllDay: false,
    priority: TaskPriority.MEDIUM,
    color: CALENDAR_COLORS[0].value
  }
  errors.value = {}
}

const validateDate = () => {
  if (formData.value.date) {
    const date = new Date(formData.value.date)
    if (isNaN(date.getTime())) {
      errors.value.date = VALIDATION_MESSAGES.invalidDate
    } else {
      delete errors.value.date
    }
  }
}

const clearError = (field: string) => {
  delete errors.value[field]
}

const handleSubmit = async () => {
  // Validate form
  if (!isFormValid.value) {
    return
  }

  const taskData: CreateTaskRequest = {
    title: formData.value.title.trim(),
    priority: formData.value.priority,
    color: formData.value.color,
    isAllDay: formData.value.isAllDay
  }

  // Set dates
  if (formData.value.isAllDay) {
    taskData.dueDate = `${formData.value.date}T00:00:00`
    taskData.startDate = `${formData.value.date}T00:00:00`
    taskData.endDate = `${formData.value.date}T23:59:59`
  } else {
    const dateTime = `${formData.value.date}T${formData.value.time}:00`
    taskData.dueDate = dateTime
    taskData.startDate = dateTime
    
    // End date is 1 hour later by default
    const endTime = new Date(`${formData.value.date}T${formData.value.time}:00`)
    endTime.setHours(endTime.getHours() + 1)
    taskData.endDate = endTime.toISOString().slice(0, 19)
  }

  // Validate the task data
  const validation = validateTaskData(taskData)
  if (!validation.isValid) {
    errors.value = validation.errors
    return
  }

  try {
    const task = await createTask(taskData)
    
    if (task) {
      emit('task-created', task)
      collapse()
    }
  } catch (error: any) {
    errors.value.submit = error.message || 'Errore nella creazione dell\'attività'
  }
}

const expandToFullForm = () => {
  const initialData: Partial<CreateTaskRequest> = {
    title: formData.value.title.trim(),
    priority: formData.value.priority,
    color: formData.value.color,
    isAllDay: formData.value.isAllDay
  }

  if (formData.value.isAllDay) {
    initialData.dueDate = `${formData.value.date}T00:00:00`
  } else {
    initialData.dueDate = `${formData.value.date}T${formData.value.time}:00`
  }

  emit('expand-full-form', initialData)
  collapse()
}

const applySuggestion = (suggestion: any) => {
  if (suggestion.priority) {
    formData.value.priority = suggestion.priority
  }
  if (suggestion.color) {
    formData.value.color = suggestion.color
  }
}

const generateSuggestions = (title: string) => {
  const titleLower = title.toLowerCase()
  const newSuggestions = []

  // Smart priority suggestions
  if (titleLower.includes('urgente') || titleLower.includes('importante') || titleLower.includes('asap')) {
    newSuggestions.push({
      text: 'Alta priorità',
      priority: TaskPriority.URGENT,
      color: CALENDAR_COLORS.find(c => c.name === 'Rosso')?.value
    })
  }

  if (titleLower.includes('riunione') || titleLower.includes('meeting')) {
    newSuggestions.push({
      text: 'Riunione',
      color: CALENDAR_COLORS.find(c => c.name === 'Blu')?.value
    })
  }

  if (titleLower.includes('compleanno') || titleLower.includes('festa')) {
    newSuggestions.push({
      text: 'Tutto il giorno',
      color: CALENDAR_COLORS.find(c => c.name === 'Rosa')?.value
    })
  }

  if (titleLower.includes('medico') || titleLower.includes('dottore') || titleLower.includes('visita')) {
    newSuggestions.push({
      text: 'Appuntamento medico',
      priority: TaskPriority.HIGH,
      color: CALENDAR_COLORS.find(c => c.name === 'Verde')?.value
    })
  }

  suggestions.value = newSuggestions.slice(0, 3) // Max 3 suggestions
}

// Keyboard shortcuts
const handleKeydown = (event: KeyboardEvent) => {
  if (!isExpanded.value) {
    // Quick add shortcut
    if ((event.ctrlKey || event.metaKey) && event.key === 'n') {
      event.preventDefault()
      expand()
    }
    return
  }

  switch (event.key) {
    case 'Escape':
      event.preventDefault()
      collapse()
      break
    case 'Enter':
      if (event.ctrlKey || event.metaKey) {
        event.preventDefault()
        if (isFormValid.value) {
          handleSubmit()
        }
      }
      break
  }
}

// Lifecycle
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  
  if (props.autoExpand) {
    expand()
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

// Expose methods for parent components
defineExpose({
  expand,
  collapse,
  resetForm,
  isExpanded: computed(() => isExpanded.value),
  isFormValid: computed(() => isFormValid.value)
})
</script>

<style scoped>
.task-quick-add {
  /* Custom styles if needed */
}

/* Smooth expand/collapse animation */
.quick-form {
  animation: expandForm 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes expandForm {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Loading spinner */
@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* Button hover effects */
.task-quick-add button:hover {
  transform: translateY(-1px);
}

.task-quick-add button:active {
  transform: translateY(0);
}

/* Focus styles for accessibility */
.task-quick-add input:focus,
.task-quick-add select:focus,
.task-quick-add button:focus {
  outline: none;
}

/* Color picker animation */
.task-quick-add .grid button {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.task-quick-add .grid button:hover {
  transform: scale(1.1);
}

/* Mobile optimizations */
@media (max-width: 640px) {
  .task-quick-add .flex.flex-wrap {
    flex-direction: column;
    align-items: stretch;
  }
  
  .task-quick-add .flex.flex-wrap > * {
    margin-bottom: 0.5rem;
  }
  
  .task-quick-add .justify-between {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .task-quick-add button {
    border-width: 2px;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .task-quick-add *,
  .quick-form {
    animation: none;
    transition: none;
  }
}
</style>