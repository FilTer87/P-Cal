<template>
  <div class="space-y-4">
    <!-- Enable/Disable Recurrence Toggle -->
    <div class="flex items-center justify-between">
      <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
        {{ $t('tasks.isRecurring') }}
      </label>
      <button
        type="button"
        :class="[
          'relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
          modelValue.isRecurring
            ? 'bg-blue-600'
            : 'bg-gray-200 dark:bg-gray-700'
        ]"
        @click="toggleRecurrence"
      >
        <span
          :class="[
            'inline-block h-4 w-4 transform rounded-full bg-white transition-transform',
            modelValue.isRecurring ? 'translate-x-6' : 'translate-x-1'
          ]"
        />
      </button>
    </div>

    <!-- Recurrence Options (shown when enabled) -->
    <div v-if="modelValue.isRecurring" class="space-y-4 border-t border-gray-200 dark:border-gray-700 pt-4">
      <!-- Frequency Selection -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ $t('tasks.recurrenceFrequency') }}
        </label>
        <select
          :value="modelValue.recurrenceFrequency"
          @change="updateFrequency"
          class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
        >
          <option :value="RecurrenceFrequency.DAILY">
            {{ $t('tasks.recurrenceOptions.daily') }}
          </option>
          <option :value="RecurrenceFrequency.WEEKLY">
            {{ $t('tasks.recurrenceOptions.weekly') }}
          </option>
          <option :value="RecurrenceFrequency.MONTHLY">
            {{ $t('tasks.recurrenceOptions.monthly') }}
          </option>
          <option :value="RecurrenceFrequency.YEARLY">
            {{ $t('tasks.recurrenceOptions.yearly') }}
          </option>
        </select>
      </div>

      <!-- Interval Input -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ $t('tasks.recurrenceInterval') }}
        </label>
        <input
          type="number"
          min="1"
          max="99"
          :value="modelValue.recurrenceInterval || 1"
          @input="updateInterval"
          class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
        />
      </div>

      <!-- Days of Week (only for WEEKLY frequency) -->
      <div v-if="modelValue.recurrenceFrequency === RecurrenceFrequency.WEEKLY">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ $t('tasks.recurrenceByDay') }}
        </label>
        <div class="flex gap-2 flex-wrap">
          <button
            v-for="day in weekDays"
            :key="day.value"
            type="button"
            :class="[
              'px-3 py-2 rounded-md text-sm font-medium transition-colors',
              isDaySelected(day.value)
                ? 'bg-blue-600 text-white'
                : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
            ]"
            @click="toggleDay(day.value)"
          >
            {{ $t(day.label) }}
          </button>
        </div>
      </div>

      <!-- End Type Selection -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ $t('tasks.recurrenceEndType') }}
        </label>
        <select
          :value="modelValue.recurrenceEndType"
          @change="updateEndType"
          class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
        >
          <option :value="RecurrenceEndType.NEVER">
            {{ $t('tasks.recurrenceEnd.never') }}
          </option>
          <option :value="RecurrenceEndType.COUNT">
            {{ $t('tasks.recurrenceEnd.count') }}
          </option>
          <option :value="RecurrenceEndType.DATE">
            {{ $t('tasks.recurrenceEnd.date') }}
          </option>
        </select>
      </div>

      <!-- Count Input (shown when endType is COUNT) -->
      <div v-if="modelValue.recurrenceEndType === RecurrenceEndType.COUNT">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ $t('tasks.recurrenceCount') }}
        </label>
        <input
          type="number"
          min="1"
          max="999"
          :value="modelValue.recurrenceCount || 1"
          @input="updateCount"
          class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
        />
      </div>

      <!-- End Date Input (shown when endType is DATE) -->
      <div v-if="modelValue.recurrenceEndType === RecurrenceEndType.DATE">
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ $t('tasks.recurrenceEndDate') }}
        </label>
        <input
          type="date"
          :value="modelValue.recurrenceEndDate"
          :min="minEndDate"
          @input="updateEndDate"
          class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RecurrenceFrequency, RecurrenceEndType, type TaskFormData } from '@/types/task'

interface Props {
  modelValue: TaskFormData
  startDate?: string
}

interface Emits {
  (e: 'update:modelValue', value: TaskFormData): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const weekDays = [
  { label: 'dateTime.weekdays.short.mon', value: 'MO' },
  { label: 'dateTime.weekdays.short.tue', value: 'TU' },
  { label: 'dateTime.weekdays.short.wed', value: 'WE' },
  { label: 'dateTime.weekdays.short.thu', value: 'TH' },
  { label: 'dateTime.weekdays.short.fri', value: 'FR' },
  { label: 'dateTime.weekdays.short.sat', value: 'SA' },
  { label: 'dateTime.weekdays.short.sun', value: 'SU' }
]

const minEndDate = computed(() => {
  return props.startDate || new Date().toISOString().split('T')[0]
})

function toggleRecurrence() {
  const updated = {
    ...props.modelValue,
    isRecurring: !props.modelValue.isRecurring,
    recurrenceFrequency: props.modelValue.isRecurring
      ? undefined
      : RecurrenceFrequency.WEEKLY,
    recurrenceInterval: props.modelValue.isRecurring ? undefined : 1,
    recurrenceEndType: props.modelValue.isRecurring
      ? undefined
      : RecurrenceEndType.NEVER,
    recurrenceCount: undefined,
    recurrenceEndDate: undefined,
    recurrenceByDay: undefined
  }
  emit('update:modelValue', updated)
}

function updateFrequency(event: Event) {
  const target = event.target as HTMLSelectElement
  const updated = {
    ...props.modelValue,
    recurrenceFrequency: target.value as RecurrenceFrequency,
    // Reset byDay when changing away from WEEKLY
    recurrenceByDay: target.value === RecurrenceFrequency.WEEKLY
      ? props.modelValue.recurrenceByDay
      : undefined
  }
  emit('update:modelValue', updated)
}

function updateInterval(event: Event) {
  const target = event.target as HTMLInputElement
  const updated = {
    ...props.modelValue,
    recurrenceInterval: parseInt(target.value, 10) || 1
  }
  emit('update:modelValue', updated)
}

function updateEndType(event: Event) {
  const target = event.target as HTMLSelectElement
  const updated = {
    ...props.modelValue,
    recurrenceEndType: target.value as RecurrenceEndType,
    recurrenceCount: undefined,
    recurrenceEndDate: undefined
  }
  emit('update:modelValue', updated)
}

function updateCount(event: Event) {
  const target = event.target as HTMLInputElement
  const updated = {
    ...props.modelValue,
    recurrenceCount: parseInt(target.value, 10) || 1
  }
  emit('update:modelValue', updated)
}

function updateEndDate(event: Event) {
  const target = event.target as HTMLInputElement
  const updated = {
    ...props.modelValue,
    recurrenceEndDate: target.value
  }
  emit('update:modelValue', updated)
}

function isDaySelected(day: string): boolean {
  return props.modelValue.recurrenceByDay?.includes(day) || false
}

function toggleDay(day: string) {
  const currentDays = props.modelValue.recurrenceByDay || []
  const updated = {
    ...props.modelValue,
    recurrenceByDay: currentDays.includes(day)
      ? currentDays.filter(d => d !== day)
      : [...currentDays, day]
  }
  emit('update:modelValue', updated)
}
</script>
