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
        <p class="text-sm text-gray-900 dark:text-white truncate">
          <p class="font-bold">{{ task.title }}</p>
          <p v-if="task.description"><span class="text-gray-600 dark:text-gray-500">{{ task.description }}</span></p>
        </p>

        <!-- Time range -->
        <div class="flex items-center justify-between mt-1">
          <span class="text-xs text-gray-500 dark:text-gray-400">
            {{ formatTime(task.startDatetime) }} - {{ formatTime(task.endDatetime) }}
          </span>

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
</script>