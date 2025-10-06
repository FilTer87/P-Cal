<template>
  <div class="calendar-grid-mobile md:calendar-grid">
    <!-- Week Days Header -->
    <div v-for="day in weekdaysShort" :key="day" class="calendar-day-header">
      {{ day }}
    </div>

    <!-- Calendar Days -->
    <div
      v-for="day in calendarDays"
      :key="day.date.getTime()"
      @click="handleDateClick(day.date)"
      @dblclick="handleDateDoubleClick(day.date)"
      class="calendar-day min-h-20 md:min-h-32 cursor-pointer transition-colors p-1 md:p-2"
      :class="getDayClasses(day)"
    >
      <!-- Day Number -->
      <div class="flex justify-between items-center mb-2">
        <span class="text-sm font-medium" :class="getDayNumberClasses(day)">
          {{ day.dayOfMonth }}
        </span>
        <div v-if="day.tasks && day.tasks.length > 0" class="text-xs text-gray-500 dark:text-gray-400">
          {{ day.tasks.length }}
        </div>
      </div>

      <!-- Tasks -->
      <div class="space-y-1">
        <div
          v-for="task in getVisibleTasks(day.tasks)"
          :key="task.id"
          @click.stop="handleTaskClick(task)"
          class="text-xs p-1 rounded truncate cursor-pointer transition-colors"
          :class="taskDisplayClasses(task)"
          :style="taskDisplayStyle(task)"
        >
          {{ task.title }}
        </div>
        <button
          v-if="hasMoreTasks(day.tasks)"
          @click.stop="handleMoreClick(day.date)"
          class="text-xs text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 p-1 hover:underline transition-colors"
        >
          {{ t('calendar.moreActivities', day.tasks.length - maxVisibleTasks) }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Task } from '@/types/task'
import type { CalendarDate, CalendarTask } from '@/types/calendar'
import { useTaskDisplay } from '@/composables/useTaskDisplay'
import { useSettingsStore } from '@/stores/settings'

// i18n
const { t } = useI18n()

// Props
interface Props {
  calendarDays: CalendarDate[]
  selectedDate?: Date
  maxVisibleTasks?: number
}

const props = withDefaults(defineProps<Props>(), {
  maxVisibleTasks: 3
})

// Emits
const emit = defineEmits<{
  'select-date': [date: Date]
  'task-click': [task: CalendarTask]
  'create-task': [date: Date]
  'open-day-view': [date: Date]
}>()

// Composables
const settings = useSettingsStore()
const { getTaskDisplayClasses, getTaskDisplayStyle } = useTaskDisplay()

// Computed
const weekdaysShort = computed(() => settings.weekdaysShort)

// Methods
const getDayClasses = (day: CalendarDate): string => {
  const classes: string[] = []

  if (day.isSelected) {
    classes.push('bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-400')
  } else if (day.isToday) {
    classes.push('bg-yellow-50 dark:bg-yellow-900/20')
  } else if (!day.isSelected && !day.isToday) {
    classes.push('hover:bg-gray-100 dark:hover:bg-gray-700')
  }

  if (!day.isCurrentMonth) {
    classes.push('text-gray-400 dark:text-gray-600')
  }

  return classes.join(' ')
}

const getDayNumberClasses = (day: CalendarDate): string => {
  if (day.isSelected) {
    return 'text-blue-600 dark:text-blue-400'
  }
  if (day.isToday && !day.isSelected) {
    return 'text-yellow-700 dark:text-yellow-300'
  }
  if (day.isCurrentMonth && !day.isToday && !day.isSelected) {
    return 'text-gray-900 dark:text-white'
  }
  return 'text-gray-400 dark:text-gray-600'
}

const getVisibleTasks = (tasks: CalendarTask[]): CalendarTask[] => {
  if (!tasks || tasks.length === 0) return []
  return tasks.slice(0, props.maxVisibleTasks)
}

const hasMoreTasks = (tasks: CalendarTask[]): boolean => {
  return tasks ? tasks.length > props.maxVisibleTasks : false
}

const taskDisplayClasses = (task: CalendarTask): string => {
  return getTaskDisplayClasses(task as any)
}

const taskDisplayStyle = (task: CalendarTask): Record<string, string> => {
  return getTaskDisplayStyle(task as any)
}

// Event handlers
const handleDateClick = (date: Date): void => {
  emit('select-date', date)
}

const handleDateDoubleClick = (date: Date): void => {
  emit('create-task', date)
}

const handleTaskClick = (task: CalendarTask): void => {
  emit('task-click', task as any)
}

const handleMoreClick = (date: Date): void => {
  emit('open-day-view', date)
}
</script>

<style scoped>
/* Calendar Grid Styles */
.calendar-grid {
  @apply grid grid-cols-7 gap-px bg-gray-200 dark:bg-gray-600 rounded-lg overflow-hidden;
}

.calendar-grid-mobile {
  @apply grid grid-cols-7 gap-px bg-gray-200 dark:bg-gray-600 rounded-lg overflow-hidden;
}

.calendar-day-header {
  @apply bg-gray-50 dark:bg-gray-700 p-2 md:p-3 text-center text-xs md:text-sm font-medium text-gray-700 dark:text-gray-300;
}

.calendar-day {
  @apply bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 flex flex-col;
}

/* Mobile specific adjustments */
@media (max-width: 768px) {
  .calendar-day-header {
    @apply p-2 text-xs;
  }

  .calendar-day {
    @apply text-xs;
  }
}
</style>
