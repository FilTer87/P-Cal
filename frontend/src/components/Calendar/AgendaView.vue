<template>
  <div class="space-y-4">
    <div
      v-for="([date, dayTasks], index) in sortedTasksEntries"
      :key="date"
      class="bg-white dark:bg-gray-800 rounded-lg p-4"
    >
      <h3 class="font-medium text-gray-900 dark:text-white mb-3">
        {{ formatDateDescription(date) }}
      </h3>

      <!-- Special handling for today's tasks with separator -->
      <div v-if="isToday(date)" class="space-y-2">
        <!-- Current/Future Tasks for today -->
        <div v-if="getTodayCurrentTasks(dayTasks).length > 0" class="space-y-2">
          <slot
            name="task-card"
            v-for="task in getTodayCurrentTasks(dayTasks)"
            :key="getTaskKey(task)"
            :task="task"
          >
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

        <!-- Separator and Past Tasks for today -->
        <div v-if="getTodayPastTasks(dayTasks).length > 0">
          <!-- Clickable Separator line -->
          <div
            class="flex items-center my-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50 rounded-md py-2 transition-colors"
            @click="togglePast"
          >
            <div class="flex-1 border-t border-gray-200 dark:border-gray-600"></div>
            <span class="px-3 text-xs text-gray-500 dark:text-gray-400 bg-white dark:bg-gray-800 flex items-center gap-2">
              {{ t('calendar.completedActivities', getTodayPastTasks(dayTasks).length) }}
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

          <!-- Collapsible Past Tasks for today -->
          <div v-show="showPastTasks" class="space-y-2">
            <slot
              name="task-card"
              v-for="task in getTodayPastTasks(dayTasks)"
              :key="getTaskKey(task)"
              :task="task"
            >
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
      </div>

      <!-- Normal display for other dates -->
      <div v-else class="space-y-2">
        <slot name="task-card" v-for="task in dayTasks" :key="getTaskKey(task)" :task="task">
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

    <div v-if="sortedTasksEntries.length === 0" class="text-center py-8">
      <p class="text-gray-500 dark:text-gray-400">
        {{ t('calendar.noActivities') }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Task } from '@/types/task'
import { useTaskDisplay } from '@/composables/useTaskDisplay'
import { splitTasksByTime } from '@/composables/useTaskFilters'
import { formatTime, getDateDescription, formatDate } from '@/utils/dateHelpers'
import { getTaskKey } from '@/utils/recurrence'

// i18n
const { t } = useI18n()

// Props
interface Props {
  tasksByDate: Record<string, Task[]>
  showPastTasks: boolean
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'task-click': [task: Task]
  'toggle-past-tasks': []
}>()

// Composables
const { getTaskDisplayClasses, getTaskDisplayStyle } = useTaskDisplay()

// Computed
const sortedTasksEntries = computed(() => {
  return Object.entries(props.tasksByDate).sort(([dateA], [dateB]) => {
    return new Date(dateA).getTime() - new Date(dateB).getTime()
  })
})

// Methods
const isToday = (dateStr: string): boolean => {
  const today = formatDate(new Date(), 'yyyy-MM-dd')
  return dateStr === today
}

const getTodayCurrentTasks = (tasks: Task[]): Task[] => {
  return splitTasksByTime(tasks).current
}

const getTodayPastTasks = (tasks: Task[]): Task[] => {
  return splitTasksByTime(tasks).past
}

const formatDateDescription = (dateStr: string): string => {
  return getDateDescription(new Date(dateStr))
}

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

const togglePast = (): void => {
  emit('toggle-past-tasks')
}
</script>
