<template>
  <div class="h-full">
    <div class="bg-white dark:bg-gray-800 rounded-lg p-4">
      <h3 class="font-medium text-gray-900 dark:text-white mb-3">
        {{ dayTitle }}
      </h3>

      <div class="space-y-2">
        <!-- Current/Future Tasks -->
        <div v-if="currentTasks.length > 0" class="space-y-2">
          <slot name="task-card" v-for="task in currentTasks" :key="task.id" :task="task">
            <div
              :class="taskDisplayClasses(task)"
              :style="taskDisplayStyle(task)"
              @click="handleTaskClick(task)"
              class="p-3 rounded-lg cursor-pointer transition-colors"
            >
              <p class="font-bold text-sm text-gray-900 dark:text-white">{{ task.title }}</p>
              <p v-if="task.description" class="text-xs text-gray-600 dark:text-gray-500 mt-1">
                {{ task.description }}
              </p>
              <div class="flex items-center justify-between mt-1">
                <span class="text-xs text-gray-500 dark:text-gray-400">
                  {{ formatTaskTime(task) }}
                </span>
              </div>
            </div>
          </slot>
        </div>

        <!-- Separator and Past Tasks -->
        <div v-if="pastTasks.length > 0">
          <!-- Clickable Separator line -->
          <div
            class="flex items-center my-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50 rounded-md py-2 transition-colors"
            @click="togglePast"
          >
            <div class="flex-1 border-t border-gray-200 dark:border-gray-600"></div>
            <span class="px-3 text-xs text-gray-500 dark:text-gray-400 bg-white dark:bg-gray-800 flex items-center gap-2">
              {{ t('calendar.completedActivities', pastTasks.length) }}
              <svg
                class="h-4 w-4 transition-transform duration-200"
                :class="{ 'rotate-180': showPastTasks }"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </span>
            <div class="flex-1 border-t border-gray-200 dark:border-gray-600"></div>
          </div>

          <!-- Collapsible Past Tasks -->
          <div v-show="showPastTasks" class="space-y-2">
            <slot name="task-card" v-for="task in pastTasks" :key="task.id" :task="task">
              <div
                :class="taskDisplayClasses(task)"
                :style="taskDisplayStyle(task)"
                @click="handleTaskClick(task)"
                class="p-3 rounded-lg cursor-pointer transition-colors"
              >
                <p class="font-bold text-sm text-gray-900 dark:text-white">{{ task.title }}</p>
                <p v-if="task.description" class="text-xs text-gray-600 dark:text-gray-500 mt-1">
                  {{ task.description }}
                </p>
                <div class="flex items-center justify-between mt-1">
                  <span class="text-xs text-gray-500 dark:text-gray-400">
                    {{ formatTaskTime(task) }}
                  </span>
                </div>
              </div>
            </slot>
          </div>
        </div>

        <!-- Empty state -->
        <div v-if="currentTasks.length === 0 && pastTasks.length === 0" class="text-center py-8">
          <p class="text-gray-500 dark:text-gray-400">
            {{ t('calendar.noActivitiesToday') }}
          </p>
          <button
            @click="handleCreateTask"
            class="mt-2 text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300"
          >
            {{ t('calendar.createFirstActivity') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Task } from '@/types/task'
import { useTaskDisplay } from '@/composables/useTaskDisplay'
import { splitTasksByTime } from '@/composables/useTaskFilters'
import { formatTime, getDayName, formatDate } from '@/utils/dateHelpers'

// i18n
const { t } = useI18n()

// Props
interface Props {
  currentDate: Date
  tasks: Task[]
  showPastTasks: boolean
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'task-click': [task: Task]
  'create-task': [date?: Date]
  'toggle-past-tasks': []
}>()

// Composables
const { getTaskDisplayClasses, getTaskDisplayStyle } = useTaskDisplay()

// Computed
const dayTitle = computed(() => {
  return `${getDayName(props.currentDate)}, ${formatDate(props.currentDate)}`
})

const currentTasks = computed(() => {
  return splitTasksByTime(props.tasks).current
})

const pastTasks = computed(() => {
  return splitTasksByTime(props.tasks).past
})

// Methods
const taskDisplayClasses = (task: Task): string => {
  return getTaskDisplayClasses(task)
}

const taskDisplayStyle = (task: Task): Record<string, string> => {
  return getTaskDisplayStyle(task)
}

const formatTaskTime = (task: Task): string => {
  if (!task.startDatetime || !task.endDatetime) return ''
  return `${formatTime(new Date(task.startDatetime))} - ${formatTime(new Date(task.endDatetime))}`
}

// Event handlers
const handleTaskClick = (task: Task): void => {
  emit('task-click', task)
}

const handleCreateTask = (): void => {
  emit('create-task', props.currentDate)
}

const togglePast = (): void => {
  emit('toggle-past-tasks')
}
</script>
