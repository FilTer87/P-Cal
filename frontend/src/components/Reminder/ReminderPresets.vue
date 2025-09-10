<template>
  <div class="reminder-presets">
    <!-- Preset Buttons -->
    <div class="grid grid-cols-3 gap-2 mb-4">
      <button
        v-for="preset in availablePresets"
        :key="preset.id"
        @click="selectPreset(preset)"
        :disabled="isPresetDisabled(preset) || isLoading"
        :class="[
          'px-3 py-2 text-sm font-medium rounded-lg border transition-all duration-200',
          selectedPresets.has(preset.id)
            ? 'bg-blue-500 text-white border-blue-500 shadow-md'
            : isPresetDisabled(preset)
            ? 'bg-gray-100 dark:bg-gray-800 text-gray-400 border-gray-300 dark:border-gray-600 cursor-not-allowed opacity-50'
            : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-600 hover:border-gray-400 dark:hover:border-gray-500'
        ]"
        :title="getPresetTooltip(preset)"
      >
        <div class="flex items-center justify-center space-x-1">
          <span>{{ preset.name }}</span>
          <CheckIcon
            v-if="selectedPresets.has(preset.id)"
            class="h-3 w-3"
          />
          <ExclamationTriangleIcon
            v-else-if="isPresetDisabled(preset)"
            class="h-3 w-3"
          />
        </div>
      </button>
    </div>

    <!-- Custom Time Input -->
    <div class="mb-4">
      <div class="flex items-center space-x-2">
        <input
          v-model="customTime.value"
          type="number"
          min="1"
          max="999"
          placeholder="15"
          class="w-20 px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        />
        <select
          v-model="customTime.unit"
          class="px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          <option value="minutes">min</option>
          <option value="hours">ore</option>
          <option value="days">giorni</option>
        </select>
        <button
          @click="addCustomReminder"
          :disabled="!isCustomTimeValid || isLoading"
          class="px-3 py-1 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
        >
          <PlusIcon class="h-4 w-4" />
        </button>
      </div>
      <p v-if="customPreviewTime" class="text-xs text-gray-500 dark:text-gray-400 mt-1">
        Promemoria: {{ customPreviewTime }}
      </p>
    </div>

    <!-- Selected Reminders Summary -->
    <div v-if="selectedPresets.size > 0" class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-3 mb-4">
      <div class="flex items-start space-x-2">
        <BellIcon class="h-4 w-4 text-blue-500 mt-0.5 flex-shrink-0" />
        <div class="flex-1">
          <p class="text-sm font-medium text-blue-800 dark:text-blue-200">
            {{ selectedPresets.size }} promemoria selezionati
          </p>
          <div class="mt-1 space-x-1">
            <span
              v-for="presetId in selectedPresets"
              :key="presetId"
              class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 dark:bg-blue-800 text-blue-800 dark:text-blue-200"
            >
              {{ getPresetById(presetId)?.name }}
              <button
                @click="unselectPreset(presetId)"
                class="ml-1 inline-flex items-center justify-center w-3 h-3 rounded-full hover:bg-blue-200 dark:hover:bg-blue-700"
              >
                <XMarkIcon class="h-2 w-2" />
              </button>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Action Buttons -->
    <div class="flex justify-between">
      <button
        v-if="selectedPresets.size > 0"
        @click="clearSelection"
        class="px-3 py-1 text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors duration-200"
      >
        Cancella tutto
      </button>
      <div class="flex space-x-2">
        <button
          v-if="selectedPresets.size > 0"
          @click="createSelectedReminders"
          :disabled="isLoading"
          class="px-4 py-2 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
        >
          <LoadingSpinner v-if="isLoading" size="small" class="mr-2" />
          Crea {{ selectedPresets.size }} promemoria
        </button>
      </div>
    </div>

    <!-- Existing Reminders Warning -->
    <div v-if="conflictingPresets.length > 0" class="mt-4 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-3">
      <div class="flex items-start space-x-2">
        <ExclamationTriangleIcon class="h-4 w-4 text-yellow-500 mt-0.5 flex-shrink-0" />
        <div class="flex-1">
          <p class="text-sm font-medium text-yellow-800 dark:text-yellow-200">
            Alcuni promemoria esistono già
          </p>
          <p class="text-xs text-yellow-700 dark:text-yellow-300 mt-1">
            I pulsanti disabilitati corrispondono a promemoria già configurati per questo task.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { format, subMinutes, parseISO, isBefore } from 'date-fns'
import { useReminders } from '../../composables/useReminders'
import type { Reminder } from '../../types/task'
import LoadingSpinner from '../Common/LoadingSpinner.vue'
import {
  PlusIcon,
  BellIcon,
  CheckIcon,
  XMarkIcon,
  ExclamationTriangleIcon
} from '@heroicons/vue/24/outline'

interface ReminderPreset {
  id: string
  name: string
  offsetMinutes: number
  description: string
}

interface Props {
  taskDueDate?: string
  existingReminders?: Reminder[]
}

interface Emits {
  (event: 'preset-selected', data: { offsetMinutes: number }): void
  (event: 'multiple-presets-selected', data: { presets: ReminderPreset[] }): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const { createMultipleReminders } = useReminders()

// Preset configurations
const defaultPresets: ReminderPreset[] = [
  { id: '5min', name: '5min', offsetMinutes: 5, description: '5 minuti prima' },
  { id: '15min', name: '15min', offsetMinutes: 15, description: '15 minuti prima' },
  { id: '30min', name: '30min', offsetMinutes: 30, description: '30 minuti prima' },
  { id: '1h', name: '1h', offsetMinutes: 60, description: '1 ora prima' },
  { id: '2h', name: '2h', offsetMinutes: 120, description: '2 ore prima' },
  { id: '4h', name: '4h', offsetMinutes: 240, description: '4 ore prima' },
  { id: '1d', name: '1 giorno', offsetMinutes: 1440, description: '1 giorno prima' },
  { id: '3d', name: '3 giorni', offsetMinutes: 4320, description: '3 giorni prima' },
  { id: '1w', name: '1 settimana', offsetMinutes: 10080, description: '1 settimana prima' }
]

// Local state
const selectedPresets = ref(new Set<string>())
const isLoading = ref(false)
const customTime = ref({
  value: 15,
  unit: 'minutes' as 'minutes' | 'hours' | 'days'
})

// Computed properties
const availablePresets = computed(() => {
  if (!props.taskDueDate) return defaultPresets
  
  const dueDate = parseISO(props.taskDueDate)
  const now = new Date()
  
  return defaultPresets.filter(preset => {
    const reminderTime = subMinutes(dueDate, preset.offsetMinutes)
    return !isBefore(reminderTime, now)
  })
})

const conflictingPresets = computed(() => {
  if (!props.existingReminders || props.existingReminders.length === 0) return []
  
  const existingTimes = props.existingReminders.map(r => new Date(r.reminderDateTime).getTime())
  
  return availablePresets.value.filter(preset => {
    if (!props.taskDueDate) return false
    
    const dueDate = parseISO(props.taskDueDate)
    const reminderTime = subMinutes(dueDate, preset.offsetMinutes)
    const reminderTimestamp = reminderTime.getTime()
    
    // Check if any existing reminder is within 1 minute of this preset
    return existingTimes.some(existingTime => 
      Math.abs(existingTime - reminderTimestamp) < 60000
    )
  })
})

const customTimeMultiplier = computed(() => {
  switch (customTime.value.unit) {
    case 'minutes': return 1
    case 'hours': return 60
    case 'days': return 1440
    default: return 1
  }
})

const isCustomTimeValid = computed(() => {
  if (!customTime.value.value || customTime.value.value < 1) return false
  if (!props.taskDueDate) return false
  
  const dueDate = parseISO(props.taskDueDate)
  const offsetMinutes = customTime.value.value * customTimeMultiplier.value
  const reminderTime = subMinutes(dueDate, offsetMinutes)
  
  return !isBefore(reminderTime, new Date())
})

const customPreviewTime = computed(() => {
  if (!isCustomTimeValid.value || !props.taskDueDate) return null
  
  const dueDate = parseISO(props.taskDueDate)
  const offsetMinutes = customTime.value.value * customTimeMultiplier.value
  const reminderTime = subMinutes(dueDate, offsetMinutes)
  
  return format(reminderTime, "dd/MM/yyyy 'alle' HH:mm")
})

// Methods
const isPresetDisabled = (preset: ReminderPreset): boolean => {
  return conflictingPresets.value.some(cp => cp.id === preset.id)
}

const getPresetTooltip = (preset: ReminderPreset): string => {
  if (isPresetDisabled(preset)) {
    return 'Promemoria già esistente per questo orario'
  }
  return preset.description
}

const getPresetById = (id: string): ReminderPreset | undefined => {
  return availablePresets.value.find(p => p.id === id)
}

const selectPreset = (preset: ReminderPreset) => {
  if (isPresetDisabled(preset)) return
  
  if (selectedPresets.value.has(preset.id)) {
    selectedPresets.value.delete(preset.id)
  } else {
    selectedPresets.value.add(preset.id)
  }
}

const unselectPreset = (presetId: string) => {
  selectedPresets.value.delete(presetId)
}

const clearSelection = () => {
  selectedPresets.value.clear()
}

const addCustomReminder = () => {
  if (!isCustomTimeValid.value) return
  
  const offsetMinutes = customTime.value.value * customTimeMultiplier.value
  emit('preset-selected', { offsetMinutes })
  
  // Reset custom input
  customTime.value.value = 15
  customTime.value.unit = 'minutes'
}

const createSelectedReminders = () => {
  if (selectedPresets.value.size === 0) return
  
  const selectedPresetObjects = Array.from(selectedPresets.value)
    .map(id => getPresetById(id))
    .filter(Boolean) as ReminderPreset[]
  
  emit('multiple-presets-selected', { presets: selectedPresetObjects })
  
  // Clear selection after emitting
  clearSelection()
}
</script>

<style scoped>
.reminder-presets {
  @apply w-full;
}

/* Custom button focus styles */
button:focus {
  outline: none;
}

/* Loading animation for buttons */
.transition-all {
  transition: all 0.2s ease-in-out;
}

/* Grid responsiveness */
@media (max-width: 640px) {
  .grid-cols-3 {
    @apply grid-cols-2;
  }
}
</style>