<template>
  <div class="date-time-picker">
    <!-- Label -->
    <label 
      v-if="label"
      :class="[
        'block text-sm font-medium mb-2',
        textClass
      ]"
    >
      {{ label }}
      <span v-if="required" class="text-red-500 ml-1">*</span>
    </label>

    <div class="flex flex-col sm:flex-row gap-2">
      <!-- Date Input -->
      <div class="flex-1">
        <div class="relative">
          <input
            :id="`date-${id}`"
            type="date"
            :value="dateValue"
            @input="handleDateChange"
            :min="minDate"
            :max="maxDate"
            :disabled="disabled"
            :class="[
              'w-full px-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent',
              inputClass,
              hasError ? 'border-red-500 focus:ring-red-500' : ''
            ]"
            :aria-label="`${label} - Data`"
            :aria-describedby="hasError ? `error-${id}` : undefined"
          />
          
          <!-- Calendar icon -->
          <CalendarDaysIcon 
            class="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" 
          />
        </div>
      </div>

      <!-- Time Input (if not all-day) -->
      <div v-if="!isAllDay" class="flex-shrink-0">
        <div class="relative">
          <input
            :id="`time-${id}`"
            type="time"
            :value="timeValue"
            @input="handleTimeChange"
            :step="timeStep"
            :disabled="disabled"
            :class="[
              'w-full px-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent',
              inputClass,
              hasError ? 'border-red-500 focus:ring-red-500' : ''
            ]"
            :aria-label="`${label} - Ora`"
          />
          
          <!-- Clock icon -->
          <ClockIcon 
            class="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" 
          />
        </div>
      </div>

      <!-- Quick time buttons (if enabled and not all-day) -->
      <div v-if="showQuickTimes && !isAllDay" class="flex gap-1">
        <button
          v-for="quickTime in quickTimes"
          :key="quickTime.value"
          type="button"
          @click="setQuickTime(quickTime.value)"
          :class="[
            'px-2 py-1 text-xs rounded border transition-colors',
            timeValue === quickTime.value
              ? 'bg-blue-500 text-white border-blue-500'
              : 'text-gray-600 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700'
          ]"
          :title="`Imposta ora a ${quickTime.label}`"
          :disabled="disabled"
        >
          {{ quickTime.label }}
        </button>
      </div>
    </div>

    <!-- Time presets dropdown -->
    <div v-if="showTimePresets && !isAllDay" class="mt-2">
      <Popover class="relative">
        <PopoverButton
          :class="[
            'inline-flex items-center px-3 py-1 text-xs border rounded-md transition-colors',
            'text-gray-600 dark:text-gray-300 border-gray-300 dark:border-gray-600',
            'hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500'
          ]"
          :disabled="disabled"
        >
          <ClockIcon class="w-3 h-3 mr-1" />
          Orari comuni
          <ChevronDownIcon class="w-3 h-3 ml-1" />
        </PopoverButton>

        <transition
          enter-active-class="transition duration-200 ease-out"
          enter-from-class="translate-y-1 opacity-0"
          enter-to-class="translate-y-0 opacity-100"
          leave-active-class="transition duration-150 ease-in"
          leave-from-class="translate-y-0 opacity-100"
          leave-to-class="translate-y-1 opacity-0"
        >
          <PopoverPanel class="absolute z-10 w-48 mt-1 bg-white dark:bg-gray-800 rounded-md shadow-lg border border-gray-200 dark:border-gray-700">
            <div class="p-2 max-h-48 overflow-y-auto">
              <button
                v-for="preset in timePresets"
                :key="preset.value"
                type="button"
                @click="setQuickTime(preset.value)"
                :class="[
                  'w-full px-2 py-1.5 text-left text-sm rounded transition-colors',
                  timeValue === preset.value
                    ? 'bg-blue-100 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300'
                    : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'
                ]"
                :disabled="disabled"
              >
                {{ preset.label }}
              </button>
            </div>
          </PopoverPanel>
        </transition>
      </Popover>
    </div>

    <!-- All-day toggle -->
    <div v-if="allowAllDay" class="flex items-center mt-2">
      <input
        :id="`all-day-${id}`"
        type="checkbox"
        :checked="isAllDay"
        @change="handleAllDayChange"
        :disabled="disabled"
        class="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500 dark:focus:ring-blue-600 dark:ring-offset-gray-800 dark:bg-gray-700 dark:border-gray-600"
      />
      <label 
        :for="`all-day-${id}`"
        :class="['ml-2 text-sm', textClass, disabled ? 'opacity-50' : '']"
      >
        Tutto il giorno
      </label>
    </div>

    <!-- Timezone info (if enabled) -->
    <div v-if="showTimezone && !isAllDay" class="mt-2 text-xs text-gray-500 dark:text-gray-400">
      <GlobeAltIcon class="w-3 h-3 inline mr-1" />
      {{ currentTimezone }}
    </div>

    <!-- Error message -->
    <p v-if="error" :id="`error-${id}`" class="text-sm text-red-600 dark:text-red-400 mt-1">
      {{ error }}
    </p>

    <!-- Helper text -->
    <p v-if="helperText && !error" class="text-sm text-gray-500 dark:text-gray-400 mt-1">
      {{ helperText }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { format } from 'date-fns'
import { it } from 'date-fns/locale'
import { useTheme } from '../../composables/useTheme'
import { VALIDATION_MESSAGES } from '../../types/task'
import { Popover, PopoverButton, PopoverPanel } from '@headlessui/vue'

// Icons
import { 
  CalendarDaysIcon, 
  ClockIcon, 
  GlobeAltIcon,
  ChevronDownIcon 
} from '@heroicons/vue/24/outline'

// Props
interface Props {
  modelValue?: string
  dateValue?: string
  timeValue?: string
  label?: string
  required?: boolean
  disabled?: boolean
  isAllDay?: boolean
  allowAllDay?: boolean
  showQuickTimes?: boolean
  showTimePresets?: boolean
  showTimezone?: boolean
  minDate?: string
  maxDate?: string
  timeStep?: number
  error?: string
  helperText?: string
  id?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  dateValue: '',
  timeValue: '09:00',
  required: false,
  disabled: false,
  isAllDay: false,
  allowAllDay: true,
  showQuickTimes: false,
  showTimePresets: true,
  showTimezone: false,
  timeStep: 900, // 15 minutes in seconds
  id: () => `datetime-picker-${Date.now()}`
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:dateValue': [value: string]
  'update:timeValue': [value: string]
  'update:isAllDay': [value: boolean]
  'date-change': [date: string]
  'time-change': [time: string]
  'datetime-change': [datetime: string]
}>()

// Composables
const { textClass, inputClass } = useTheme()

// State
const internalDateValue = ref(props.dateValue || getTodayString())
const internalTimeValue = ref(props.timeValue)
const internalIsAllDay = ref(props.isAllDay)

// Quick time options
const quickTimes = [
  { label: '9:00', value: '09:00' },
  { label: '12:00', value: '12:00' },
  { label: '15:00', value: '15:00' },
  { label: '18:00', value: '18:00' }
]

// Time presets
const timePresets = [
  { label: '08:00 - Mattina presto', value: '08:00' },
  { label: '09:00 - Inizio giornata', value: '09:00' },
  { label: '10:00 - Metà mattina', value: '10:00' },
  { label: '12:00 - Mezzogiorno', value: '12:00' },
  { label: '13:00 - Pranzo', value: '13:00' },
  { label: '14:00 - Primo pomeriggio', value: '14:00' },
  { label: '15:00 - Metà pomeriggio', value: '15:00' },
  { label: '17:00 - Fine giornata', value: '17:00' },
  { label: '18:00 - Sera', value: '18:00' },
  { label: '20:00 - Cena', value: '20:00' },
  { label: '21:00 - Tarda serata', value: '21:00' }
]

// Computed
const hasError = computed(() => !!props.error)

const dateValue = computed({
  get: () => props.dateValue || internalDateValue.value,
  set: (value: string) => {
    internalDateValue.value = value
    emit('update:dateValue', value)
  }
})

const timeValue = computed({
  get: () => props.timeValue || internalTimeValue.value,
  set: (value: string) => {
    internalTimeValue.value = value
    emit('update:timeValue', value)
  }
})

const isAllDay = computed({
  get: () => internalIsAllDay.value,
  set: (value: boolean) => {
    internalIsAllDay.value = value
    emit('update:isAllDay', value)
  }
})

const currentTimezone = computed(() => {
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
  return timezone.replace(/_/g, ' ')
})

const combinedDateTime = computed(() => {
  if (!dateValue.value) return ''
  
  if (isAllDay.value) {
    return `${dateValue.value}T00:00:00`
  }
  
  const time = timeValue.value || '00:00'
  return `${dateValue.value}T${time}:00`
})

// Watch for external prop changes
watch(
  () => props.dateValue,
  (newValue) => {
    if (newValue && newValue !== internalDateValue.value) {
      internalDateValue.value = newValue
    }
  },
  { immediate: true }
)

watch(
  () => props.timeValue,
  (newValue) => {
    if (newValue && newValue !== internalTimeValue.value) {
      internalTimeValue.value = newValue
    }
  },
  { immediate: true }
)

watch(
  () => props.isAllDay,
  (newValue) => {
    internalIsAllDay.value = newValue
  },
  { immediate: true }
)

// Watch for internal changes and emit
watch(combinedDateTime, (newValue) => {
  emit('update:modelValue', newValue)
  emit('datetime-change', newValue)
}, { immediate: true })

// Methods
const getTodayString = (): string => {
  return format(new Date(), 'yyyy-MM-dd')
}

const handleDateChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = target.value
  
  dateValue.value = value
  emit('date-change', value)
}

const handleTimeChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = target.value
  
  timeValue.value = value
  emit('time-change', value)
}

const handleAllDayChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  isAllDay.value = target.checked
}

const setQuickTime = (time: string) => {
  timeValue.value = time
  emit('time-change', time)
}

const setToNow = () => {
  const now = new Date()
  dateValue.value = format(now, 'yyyy-MM-dd')
  
  if (!isAllDay.value) {
    // Round to nearest 15 minutes
    const minutes = Math.round(now.getMinutes() / 15) * 15
    now.setMinutes(minutes, 0, 0)
    timeValue.value = format(now, 'HH:mm')
  }
}

const setToStartOfDay = () => {
  timeValue.value = '00:00'
}

const setToEndOfDay = () => {
  timeValue.value = '23:59'
}

const clear = () => {
  dateValue.value = ''
  timeValue.value = '09:00'
  isAllDay.value = false
}

const validate = (): boolean => {
  if (props.required && !dateValue.value) {
    return false
  }
  
  if (dateValue.value) {
    const date = new Date(dateValue.value)
    if (isNaN(date.getTime())) {
      return false
    }
  }
  
  if (!isAllDay.value && timeValue.value) {
    const timeRegex = /^([01]?[0-9]|2[0-3]):[0-5][0-9]$/
    if (!timeRegex.test(timeValue.value)) {
      return false
    }
  }
  
  return true
}

const formatForDisplay = (): string => {
  if (!dateValue.value) return ''
  
  const date = new Date(dateValue.value)
  let display = format(date, 'EEEE d MMMM yyyy', { locale: it })
  
  if (!isAllDay.value && timeValue.value) {
    display += ` alle ${timeValue.value}`
  } else if (isAllDay.value) {
    display += ' (tutto il giorno)'
  }
  
  return display
}

// Initialize with current date if no value provided
onMounted(() => {
  if (!dateValue.value) {
    dateValue.value = getTodayString()
  }
})

// Expose methods for parent components
defineExpose({
  setToNow,
  setToStartOfDay,
  setToEndOfDay,
  clear,
  validate,
  formatForDisplay,
  dateValue: computed(() => dateValue.value),
  timeValue: computed(() => timeValue.value),
  isAllDay: computed(() => isAllDay.value),
  combinedDateTime: computed(() => combinedDateTime.value)
})
</script>

<style scoped>
.date-time-picker {
  /* Custom styles if needed */
}

/* Hide browser-specific date/time picker styling when needed */
input[type="date"]::-webkit-calendar-picker-indicator,
input[type="time"]::-webkit-calendar-picker-indicator {
  opacity: 0;
  position: absolute;
  right: 0;
  top: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

/* Firefox date input styling */
input[type="date"]::-moz-placeholder,
input[type="time"]::-moz-placeholder {
  color: transparent;
}

/* Ensure consistent sizing across browsers */
input[type="date"],
input[type="time"] {
  -webkit-appearance: none;
  -moz-appearance: textfield;
}

input[type="time"]::-webkit-inner-spin-button,
input[type="time"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

/* Focus styles for better accessibility */
input[type="date"]:focus,
input[type="time"]:focus {
  outline: none;
}

/* Animation for popover */
.date-time-picker .transition {
  transition-property: opacity, transform;
}
</style>