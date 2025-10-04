<template>
  <div class="reminder-form">
    <form @submit.prevent="handleSubmit" class="space-y-6">
      <!-- Time Selection Mode -->
      <div>
        <label class="text-base font-medium text-gray-900 dark:text-gray-100">
          {{ t('reminders.mode') }}
        </label>
        <div class="mt-2 space-y-2">
          <label class="flex items-center">
            <input
              v-model="form.mode"
              type="radio"
              value="offset"
              class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
            />
            <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">
              {{ t('reminders.beforeDue') }}
            </span>
          </label>
          <label class="flex items-center">
            <input
              v-model="form.mode"
              type="radio"
              value="absolute"
              class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300"
            />
            <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">
              {{ t('reminders.specificDateTime') }}
            </span>
          </label>
        </div>
      </div>

      <!-- Offset Mode -->
      <div v-if="form.mode === 'offset'" class="space-y-4">
        <div>
          <label for="offset-value" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            {{ t('reminders.timeBefore') }}
          </label>
          <div class="flex space-x-2">
            <input
              id="offset-value"
              v-model.number="form.offsetValue"
              type="number"
              min="1"
              max="9999"
              required
              class="flex-1 block w-full rounded-md border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              placeholder="15"
            />
            <select
              v-model="form.offsetUnit"
              required
              class="block rounded-md border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            >
              <option value="minutes">{{ t('reminders.units.minutes') }}</option>
              <option value="hours">{{ t('reminders.units.hours') }}</option>
              <option value="days">{{ t('reminders.units.days') }}</option>
              <option value="weeks">{{ t('reminders.units.weeks') }}</option>
            </select>
          </div>
          <p v-if="offsetReminderTime" class="mt-1 text-xs text-gray-500 dark:text-gray-400">
            {{ t('reminders.reminderLabel') }}: {{ offsetReminderTime }}
          </p>
        </div>

        <!-- Common Presets -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            {{ t('reminders.commonPresets') }}
          </label>
          <div class="grid grid-cols-3 gap-2">
            <button
              v-for="preset in commonPresets"
              :key="preset.id"
              type="button"
              @click="applyPreset(preset)"
              class="px-3 py-2 text-xs font-medium rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-200"
            >
              {{ preset.name }}
            </button>
          </div>
        </div>
      </div>

      <!-- Absolute Mode -->
      <div v-else class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label for="reminder-date" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('reminders.reminderDate') }}
            </label>
            <input
              id="reminder-date"
              v-model="form.date"
              type="date"
              required
              :min="minDate"
              :max="maxDate"
              class="block w-full rounded-md border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
          <div>
            <label for="reminder-time" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('reminders.reminderTime') }}
            </label>
            <input
              id="reminder-time"
              v-model="form.time"
              type="time"
              required
              class="block w-full rounded-md border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
        </div>

        <p v-if="absoluteReminderTime" class="text-xs text-gray-500 dark:text-gray-400">
          Promemoria: {{ absoluteReminderTime }}
        </p>
      </div>

      <!-- Validation Errors -->
      <div v-if="validationErrors.length > 0" class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md p-4">
        <div class="flex">
          <ExclamationCircleIcon class="h-5 w-5 text-red-400" />
          <div class="ml-3">
            <h3 class="text-sm font-medium text-red-800 dark:text-red-200">
              Errori di validazione
            </h3>
            <div class="mt-2 text-sm text-red-700 dark:text-red-300">
              <ul class="list-disc pl-5 space-y-1">
                <li v-for="error in validationErrors" :key="error">
                  {{ error }}
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <!-- Preview -->
      <div v-if="reminderPreview" class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md p-4">
        <div class="flex">
          <BellIcon class="h-5 w-5 text-blue-400" />
          <div class="ml-3">
            <h3 class="text-sm font-medium text-blue-800 dark:text-blue-200">
              Anteprima promemoria
            </h3>
            <div class="mt-2 text-sm text-blue-700 dark:text-blue-300">
              <p>{{ reminderPreview.message }}</p>
              <p class="text-xs mt-1">{{ reminderPreview.time }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex justify-end space-x-3 pt-6 border-t border-gray-200 dark:border-gray-700">
        <button
          type="button"
          @click="$emit('cancel')"
          class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-200"
        >
          {{ t('common.cancel') }}
        </button>
        <button
          type="submit"
          :disabled="!isFormValid || isSubmitting"
          class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
        >
          <LoadingSpinner v-if="isSubmitting" size="small" class="mr-2" />
          {{ isEditing ? t('common.edit') : t('common.save') }} {{ t('reminders.reminderLabel') }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { format, parseISO, subMinutes, addMinutes, isBefore } from 'date-fns'
import { useReminders } from '../../composables/useReminders'
import type { Reminder } from '../../types/task'
import LoadingSpinner from '../Common/LoadingSpinner.vue'
import { ExclamationCircleIcon, BellIcon } from '@heroicons/vue/24/outline'

// i18n
const { t } = useI18n()

interface Props {
  taskId: number
  taskDueDate?: string
  reminder?: Reminder | null
}

interface Emits {
  (event: 'saved'): void
  (event: 'cancel'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const {
  createReminder,
  updateReminder,
  createReminderFromForm,
  createFormFromReminder
} = useReminders()

// Form state
const form = ref({
  mode: 'offset' as 'offset' | 'absolute',
  offsetValue: 15,
  offsetUnit: 'minutes' as 'minutes' | 'hours' | 'days' | 'weeks',
  date: '',
  time: '09:00'
})

const isSubmitting = ref(false)
const validationErrors = ref<string[]>([])

// Computed properties
const isEditing = computed(() => !!props.reminder)

const minDate = computed(() => {
  return format(new Date(), 'yyyy-MM-dd')
})

const maxDate = computed(() => {
  if (!props.taskDueDate) return ''
  return format(parseISO(props.taskDueDate), 'yyyy-MM-dd')
})

const offsetMultiplier = computed(() => {
  switch (form.value.offsetUnit) {
    case 'minutes': return 1
    case 'hours': return 60
    case 'days': return 1440
    case 'weeks': return 10080
    default: return 1
  }
})

const offsetReminderTime = computed(() => {
  if (!props.taskDueDate || form.value.mode !== 'offset') return null
  
  const dueDate = parseISO(props.taskDueDate)
  const offsetMinutes = form.value.offsetValue * offsetMultiplier.value
  const reminderTime = subMinutes(dueDate, offsetMinutes)
  
  return format(reminderTime, "dd/MM/yyyy 'alle' HH:mm")
})

const absoluteReminderTime = computed(() => {
  if (form.value.mode !== 'absolute' || !form.value.date || !form.value.time) return null
  
  const reminderDateTime = new Date(`${form.value.date}T${form.value.time}`)
  return format(reminderDateTime, "dd/MM/yyyy 'alle' HH:mm")
})

const reminderPreview = computed(() => {
  if (form.value.mode === 'offset' && offsetReminderTime.value) {
    return {
      message: `Ti ricorderemo questo task ${form.value.offsetValue} ${getUnitLabel()} prima della scadenza`,
      time: offsetReminderTime.value
    }
  } else if (form.value.mode === 'absolute' && absoluteReminderTime.value) {
    return {
      message: `Ti ricorderemo questo task il ${absoluteReminderTime.value}`,
      time: absoluteReminderTime.value
    }
  }
  return null
})

const isFormValid = computed(() => {
  validateForm()
  return validationErrors.value.length === 0
})

// Preset configurations
const commonPresets = [
  { id: '5min', name: '5 min', value: 5, unit: 'minutes' },
  { id: '15min', name: '15 min', value: 15, unit: 'minutes' },
  { id: '30min', name: '30 min', value: 30, unit: 'minutes' },
  { id: '1hour', name: '1 ora', value: 1, unit: 'hours' },
  { id: '2hours', name: '2 ore', value: 2, unit: 'hours' },
  { id: '4hours', name: '4 ore', value: 4, unit: 'hours' },
  { id: '1day', name: '1 giorno', value: 1, unit: 'days' },
  { id: '3days', name: '3 giorni', value: 3, unit: 'days' },
  { id: '1week', name: '1 settimana', value: 1, unit: 'weeks' }
]

// Methods
const getUnitLabel = () => {
  const value = form.value.offsetValue
  switch (form.value.offsetUnit) {
    case 'minutes': return value === 1 ? 'minuto' : 'minuti'
    case 'hours': return value === 1 ? 'ora' : 'ore'
    case 'days': return value === 1 ? 'giorno' : 'giorni'
    case 'weeks': return value === 1 ? 'settimana' : 'settimane'
    default: return form.value.offsetUnit
  }
}

const applyPreset = (preset: typeof commonPresets[0]) => {
  form.value.offsetValue = preset.value
  form.value.offsetUnit = preset.unit as any
}

const validateForm = () => {
  validationErrors.value = []
  
  if (form.value.mode === 'offset') {
    if (!form.value.offsetValue || form.value.offsetValue < 1) {
      validationErrors.value.push('Il valore del tempo deve essere almeno 1')
    }
    
    if (!props.taskDueDate) {
      validationErrors.value.push('Data di scadenza del task mancante')
      return
    }
    
    const dueDate = parseISO(props.taskDueDate)
    const offsetMinutes = form.value.offsetValue * offsetMultiplier.value
    const reminderTime = subMinutes(dueDate, offsetMinutes)
    
    if (isBefore(reminderTime, new Date())) {
      validationErrors.value.push('Il promemoria sarebbe nel passato')
    }
  } else {
    if (!form.value.date) {
      validationErrors.value.push('Data del promemoria obbligatoria')
    }
    
    if (!form.value.time) {
      validationErrors.value.push('Ora del promemoria obbligatoria')
    }
    
    if (form.value.date && form.value.time) {
      const reminderDateTime = new Date(`${form.value.date}T${form.value.time}`)
      
      if (isBefore(reminderDateTime, new Date())) {
        validationErrors.value.push('Il promemoria non può essere nel passato')
      }
      
      if (props.taskDueDate && !isBefore(reminderDateTime, parseISO(props.taskDueDate))) {
        validationErrors.value.push('Il promemoria deve essere prima della scadenza del task')
      }
    }
  }
}

const handleSubmit = async () => {
  if (!isFormValid.value) return
  
  isSubmitting.value = true
  
  try {
    let reminderDateTime: string
    
    if (form.value.mode === 'offset') {
      const dueDate = parseISO(props.taskDueDate!)
      const offsetMinutes = form.value.offsetValue * offsetMultiplier.value
      const reminderTime = subMinutes(dueDate, offsetMinutes)
      reminderDateTime = reminderTime.toISOString()
    } else {
      const reminderTime = new Date(`${form.value.date}T${form.value.time}:00`)
      reminderDateTime = reminderTime.toISOString()
    }
    
    if (isEditing.value) {
      await updateReminder(props.reminder!.id, { reminderDateTime })
    } else {
      await createReminder(props.taskId, { reminderDateTime })
    }
    
    emit('saved')
  } catch (error) {
    console.error('Error saving reminder:', error)
  } finally {
    isSubmitting.value = false
  }
}

const initializeForm = () => {
  if (props.reminder) {
    const reminderData = createFormFromReminder(props.reminder)
    form.value.mode = 'absolute'
    form.value.date = reminderData.date
    form.value.time = reminderData.time
  } else {
    // Set default date/time for absolute mode
    const tomorrow = addMinutes(new Date(), 1440) // 24 hours from now
    form.value.date = format(tomorrow, 'yyyy-MM-dd')
    form.value.time = format(tomorrow, 'HH:mm')
  }
}

// Watchers
watch(() => [form.value.mode, form.value.offsetValue, form.value.offsetUnit, form.value.date, form.value.time], () => {
  validateForm()
})

// Lifecycle
onMounted(() => {
  initializeForm()
})
</script>

<style scoped>
.reminder-form {
  @apply w-full max-w-lg mx-auto;
}

input[type="number"]::-webkit-outer-spin-button,
input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type="number"] {
  -moz-appearance: textfield;
}
</style>