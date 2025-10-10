<template>
  <div
    class="p-3 rounded-lg cursor-pointer transition-colors"
    :class="taskDisplayClasses"
    :style="taskDisplayStyle"
    @click="$emit('click', task)"
  >
    <div class="flex items-center justify-between">
      <div class="flex-1 min-w-0">
        <!-- Title and Description in one line -->
        <div class="text-sm text-gray-900 dark:text-white">
          <p class="font-bold truncate">{{ task.title }}</p>
          <p v-if="task.description" class="text-gray-600 dark:text-gray-500 truncate text-xs">{{ task.description }}</p>
        </div>

        <!-- Time range -->
        <div class="flex items-center justify-between mt-1">
          <span class="text-xs text-gray-500 dark:text-gray-400">
            {{ formatTime(task.startDatetime) }} - {{ formatTime(task.endDatetime) }}
          </span>

          <div class="flex items-center gap-1">
            <!-- Recurrence icon -->
            <svg
              v-if="isRecurring"
              class="h-4 w-4 text-gray-400 dark:text-gray-500 flex-shrink-0"
              :title="t('tasks.recurrence')"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>

            <!-- Reminder bell icon -->
            <BellIcon
              v-if="hasReminders"
              class="h-4 w-4 text-gray-400 dark:text-gray-500 flex-shrink-0"
              :title="t('tasks.hasReminders')"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { BellIcon } from '@heroicons/vue/24/outline'
import type { Task } from '../types/task'
import { formatTime } from '../utils/dateHelpers'

// Composables
const { t } = useI18n()

// Props
interface Props {
  task: Task
  taskDisplayClasses?: string
  taskDisplayStyle?: any
}

const props = defineProps<Props>()

// Emits
defineEmits<{
  click: [task: Task]
}>()

// Computed
const hasReminders = computed(() => {
  return props.task.reminders && props.task.reminders.length > 0
})

const isRecurring = computed(() => {
  return !!props.task.recurrenceRule
})
</script>