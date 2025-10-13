<template>
  <div class="h-full flex flex-col">
    <!-- Week Header -->
    <div class="grid grid-cols-8 gap-px bg-gray-200 dark:bg-gray-600 mb-4">
      <div class="bg-white dark:bg-gray-800 p-2 text-sm font-medium text-center">
        {{ t('calendar.weekView.hourLabel') }}
      </div>
      <div v-for="day in getWeekDays(currentDate)" :key="day.getTime()"
        class="bg-white dark:bg-gray-800 p-2 text-sm font-medium text-center" :class="{
          'bg-blue-50 dark:bg-blue-900/20': isToday(day),
          'text-blue-600 dark:text-blue-400': isToday(day)
        }">
        <div>{{ getWeekDayName(day, true) }}</div>
        <div class="text-lg font-bold">{{ day.getDate() }}</div>
      </div>
    </div>

    <!-- Week Grid -->
    <div class="flex-1 relative overflow-hidden">
      <!-- Scrollable Content -->
      <div ref="weeklyScrollContainer" @scroll="handleWeeklyScroll" class="absolute inset-0 overflow-auto" style="margin-right: -6px;">
        <div class="grid grid-cols-8 gap-px bg-gray-200 dark:bg-gray-600" style="min-height: 1536px;">
          <!-- Time Column -->
          <div class="bg-white dark:bg-gray-800">
            <div v-for="hour in 24" :key="hour-1" 
              class="h-16 border-b border-gray-200 dark:border-gray-600 flex items-center justify-center text-xs font-medium text-gray-500 dark:text-gray-400"
              :class="{ 'border-b-2 border-gray-300 dark:border-gray-500': (hour-1) % 6 === 0 }">
              {{ settings.formatHourLabel(hour-1) }}
            </div>
          </div>

          <!-- Day Columns -->
          <div v-for="(dayInfo, dayIndex) in weekDaysWithIndicators" :key="dayInfo.day.getTime()" 
            class="bg-white dark:bg-gray-800 relative">
            
            <!-- Time Grid Lines -->
            <div v-for="hour in 24" :key="hour-1" 
              class="h-16 border-b border-gray-100 dark:border-gray-700"
              :class="{ 
                'border-b-2 border-gray-200 dark:border-gray-600': (hour-1) % 6 === 0,
                'bg-blue-50 dark:bg-blue-900/10': isToday(dayInfo.day)
              }">
            </div>

            <!-- Tasks for this day (including split multi-day tasks) -->
            <div class="absolute inset-0 pointer-events-none">
              <div v-for="task in dayInfo.tasks" :key="`${getTaskKey(task)}-${task._splitIndex || 0}`"
                :style="getTaskTimeStyleIntelligent(task, dayInfo.tasks, dayInfo.layouts)"
                @click="handleTaskClick(task)"
                class="absolute rounded text-xs font-medium cursor-pointer pointer-events-auto transition-all hover:shadow-md overflow-hidden group"
                :class="getTaskTimeDisplayClasses(task)"
                :title="getTaskTooltipContent(task)">

                <!-- Content for larger tasks -->
                <template v-if="shouldShowTitle(task, dayInfo.tasks)">
                  <div class="truncate font-semibold leading-tight text-xs">{{ task.title }}</div>
                  <div v-if="task.location && shouldShowLocation(task, dayInfo.tasks)" class="truncate text-xs opacity-90 leading-tight">{{ task.location }}</div>
                  <div v-if="shouldShowTime(task, dayInfo.tasks)" class="text-xs opacity-75 leading-tight">{{ formatTaskTime(task) }}</div>
                </template>

                <!-- Minimal indicator for very small tasks -->
                <template v-else>
                  <div class="w-full h-full flex items-center justify-center">
                    <div class="w-1 h-1 bg-white rounded-full opacity-80"></div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Fixed Overflow Indicators (Positioned relative to scroll container) -->
      <div class="absolute inset-0 pointer-events-none z-50">
        <div v-for="(dayInfo, dayIndex) in weekDaysWithIndicators" :key="`indicators-${dayInfo.day.getTime()}`"
          class="absolute pointer-events-none"
          :style="{ 
            left: `${12.5 + (dayIndex * 12.5)}%`,
            width: '12.5%',
            top: '0px',
            height: '100%'
          }">
          
          
          <!-- Top indicators for hidden tasks above -->
          <div v-for="(task, index) in dayInfo.indicators.top"
            :key="`top-${getTaskKey(task)}`"
            class="absolute w-3 h-3 rounded-full border border-white shadow-sm"
            :style="{ 
              top: '16px',
              right: `${6 + index * 10}px`,
              backgroundColor: task.color || '#3B82F6'
            }">
          </div>

          <!-- Bottom indicators for hidden tasks below -->
          <div v-for="(task, index) in dayInfo.indicators.bottom"
            :key="`bottom-${getTaskKey(task)}`"
            class="absolute w-3 h-3 rounded-full border border-white shadow-lg"
            :style="{ 
              bottom: '16px',
              right: `${6 + index * 10}px`,
              backgroundColor: task.color || '#EF4444'
            }"
            :title="`Hidden below: ${task.title}`">
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { format } from 'date-fns'
import { isToday } from '../../utils/dateHelpers'
import { useCalendar } from '../../composables/useCalendar'
import { useSettingsStore } from '../../stores/settings'
import { useOverlapLayout } from '../../composables/useOverlapLayout'
import { i18n } from '../../i18n'
import type { Task } from '../../types/task'
import { getTaskKey } from '../../utils/recurrence'

// i18n
const { t } = useI18n()

// Props
interface Props {
  currentDate: Date
  tasks: Task[]
  scrollTop?: number
  scrollHeight?: number  
  clientHeight?: number
}

const props = withDefaults(defineProps<Props>(), {
  scrollTop: 0,
  scrollHeight: 0,
  clientHeight: 0
})

// Emits
const emit = defineEmits<{
  taskClick: [task: Task]
  scroll: [event: Event]
}>()

// Refs
const weeklyScrollContainer = ref<HTMLElement | null>(null)
const indicatorsUpdateTrigger = ref(0)

// Get calendar composable with settings-aware functions
const calendar = useCalendar()
const settings = useSettingsStore()
const overlaps = useOverlapLayout()

// Use the settings-aware getWeekDays from composable
const getWeekDays = calendar.getWeekDays

const getWeekDayName = (date: Date, short = false) => {
  const locale = i18n.global.locale.value
  if (short) {
    return date.toLocaleDateString(locale, { weekday: 'short' })
  }
  return date.toLocaleDateString(locale, { weekday: 'long' })
}

// Task filtering and splitting logic
const getTasksWithSplitsForDate = (date: Date) => {
  const currentDay = format(date, 'yyyy-MM-dd')
  const allTasksWithSplits: Task[] = []
  const seenKeys = new Set<string | number>()

  props.tasks.forEach(task => {
    if (!task.startDatetime || !task.endDatetime) return

    // Use local timezone for date extraction, not UTC string split
    const taskStartDay = format(new Date(task.startDatetime), 'yyyy-MM-dd')
    const taskEndDay = format(new Date(task.endDatetime), 'yyyy-MM-dd')
    const taskKey = getTaskKey(task)

    // Skip if we've already seen this task key
    if (seenKeys.has(taskKey)) {
      return
    }

    if (taskStartDay === taskEndDay) {
      // Single-day task
      if (taskStartDay === currentDay) {
        allTasksWithSplits.push(task)
        seenKeys.add(taskKey)
      }
    } else {
      // Multi-day task - ONLY if currentDay is within the task range
      if (currentDay >= taskStartDay && currentDay <= taskEndDay) {
        let visualStartTime: string
        let visualEndTime: string

        if (currentDay === taskStartDay) {
          // First day: from original start time to end of day
          visualStartTime = task.startDatetime
          visualEndTime = `${currentDay}T23:59:59`
        } else if (currentDay === taskEndDay) {
          // Last day: from start of day to original end time
          visualStartTime = `${currentDay}T00:00:00`
          visualEndTime = task.endDatetime
        } else {
          // Middle day: full day (using local timezone format for consistent positioning)
          visualStartTime = `${currentDay}T00:00:00`
          visualEndTime = `${currentDay}T23:59:59`
        }

        // Create task with ORIGINAL data but visual positioning times
        const splitTask = {
          ...task, // Keep ALL original data
          // Add visual positioning properties for rendering only
          _visualStartTime: visualStartTime,
          _visualEndTime: visualEndTime,
          _splitIndex: currentDay // For unique key
        }

        allTasksWithSplits.push(splitTask)
        seenKeys.add(taskKey)
      }
    }
  })

  return allTasksWithSplits
}

// Task positioning and styling
const getTaskTimePosition = (task: any) => {
  // Use visual times for positioning if available (for split multi-day tasks)
  const startTimeStr = task._visualStartTime || task.startDatetime
  const endTimeStr = task._visualEndTime || task.endDatetime
  
  if (!startTimeStr) return { top: '0px', height: '32px' }
  
  const start = new Date(startTimeStr)
  const end = endTimeStr ? new Date(endTimeStr) : new Date(start.getTime() + 60 * 60 * 1000) // Default 1 hour
  
  // Calculate position based on hours (each hour = 64px height)
  // All times now use local timezone for consistent positioning
  const startHour = start.getHours() + start.getMinutes() / 60
  let endHour = end.getHours() + end.getMinutes() / 60
  
  // For VISUAL positioning: limit to current day (max 24:00)
  if (endHour < startHour) {
    // Task spans midnight - show only until end of day for this column
    endHour = 24
  }
  
  const topPosition = startHour * 64 // 64px per hour (h-16 = 4rem = 64px)
  const actualDuration = (endHour - startHour) * 64
  // Use actual duration with minimum 3px for visibility
  const height = Math.max(actualDuration, 3)
  
  return {
    top: `${topPosition}px`,
    height: `${height}px`
  }
}

const getTaskTimeDisplayClasses = (task: Task) => {
  const baseClasses = [
    'border-l-4',
    'text-white',
    'shadow-sm'
  ]

  // Color based on task color or default
  const color = task.color || '#3788d8'
  const bgColor = `${color}CC` // Add transparency
  const isPast = new Date(task.endDatetime) < new Date()

  return [
    ...baseClasses,
    {
      'opacity-50': isPast
    }
  ]
}

const getTaskTimeStyle = (task: Task) => {
  const position = getTaskTimePosition(task)
  const color = task.color || '#3788d8'

  return {
    ...position,
    backgroundColor: `${color}CC`,
    borderLeftColor: color,
    zIndex: 10
  }
}

const getTaskTimeStyleIntelligent = (task: Task, dayTasks: Task[], calculatedLayouts: Map<string | number, any>) => {
  const position = getTaskTimePositionIntelligent(task, dayTasks)
  const color = task.color || '#3788d8'

  const taskKey = getTaskKey(task)
  const layout = calculatedLayouts.get(taskKey)

  return {
    ...position,
    backgroundColor: `${color}CC`,
    borderLeftColor: color,
    zIndex: layout?.zIndex || 10,
    width: layout?.width || 'calc(100% - 8px)',
    left: layout ? `calc(0px + ${layout.leftOffset})` : '0px'
  }
}

const formatTaskTime = (task: Task) => {
  if (!task.startDatetime || !task.endDatetime) return ''
  
  // Use visual times if available (for split multi-day tasks)
  const startTimeStr = (task as any)._visualStartTime || task.startDatetime
  const endTimeStr = (task as any)._visualEndTime || task.endDatetime
  
  const start = new Date(startTimeStr)
  const end = new Date(endTimeStr)

  return `${settings.formatTime(start)} - ${settings.formatTime(end)}`
}

// Overflow indicators logic
const getTasksOverflowIndicators = (dayTasks: Task[]) => {
  const containerHeight = props.clientHeight
  const scrollTop = props.scrollTop
  const scrollBottom = scrollTop + containerHeight

  const indicators = {
    top: [] as Task[],
    bottom: [] as Task[]
  }

  dayTasks.forEach(task => {
    const position = getTaskTimePosition(task)
    const taskTop = parseInt(position.top)
    const taskBottom = taskTop + parseInt(position.height)

    // Task is above visible area
    if (taskBottom < scrollTop) {
      indicators.top.push(task)
    }
    // Task is below visible area  
    else if (taskTop > scrollBottom) {
      indicators.bottom.push(task)
    }
  })

  return indicators
}

// Helper functions for content display based on task height
const getTaskHeight = (task: any) => {
  const position = getTaskTimePosition(task)
  return parseInt(position.height)
}

const getTaskHeightIntelligent = (task: any, dayTasks: Task[]) => {
  const position = getTaskTimePositionIntelligent(task, dayTasks)
  return parseInt(position.height)
}

const shouldShowTitle = (task: any, dayTasks?: Task[]) => {
  // Show title only if task is at least 16px tall
  if (dayTasks) {
    return getTaskHeightIntelligent(task, dayTasks) >= 16
  }
  return getTaskHeight(task) >= 16
}

const shouldShowLocation = (task: any, dayTasks?: Task[]) => {
  // Show location only if task is at least 36px tall (enough for title + location)
  if (dayTasks) {
    return getTaskHeightIntelligent(task, dayTasks) >= 36
  }
  return getTaskHeight(task) >= 36
}

const shouldShowTime = (task: any, dayTasks?: Task[]) => {
  // Show time only if task is at least 56px tall (enough for title + location + time)
  if (dayTasks) {
    return getTaskHeightIntelligent(task, dayTasks) >= 56
  }
  return getTaskHeight(task) >= 56
}

const getTaskTooltipContent = (task: any) => {
  const parts = []

  // Always show title
  parts.push(task.title)

  // Add time info
  const timeStr = formatTaskTime(task)
  if (timeStr) {
    parts.push(`${t('calendar.weekView.time')}: ${timeStr}`)
  }

  // Add location if present
  if (task.location) {
    parts.push(`${t('calendar.weekView.location')}: ${task.location}`)
  }

  // Add description if present and not too long
  if (task.description && task.description.length <= 100) {
    parts.push(`${task.description}`)
  } else if (task.description) {
    parts.push(`${task.description.substring(0, 97)}...`)
  }

  return parts.join(' • ')
}

// Check if task overlaps with next task (for intelligent height calculation)
const hasOverlapWithNext = (currentTask: any, dayTasks: Task[]) => {
  const currentEndStr = currentTask._visualEndTime || currentTask.endDatetime
  if (!currentEndStr) return false

  const currentEnd = new Date(currentEndStr)

  // Find tasks that start within 2 minutes of this task's end
  const overlapping = dayTasks.some(otherTask => {
    if (getTaskKey(otherTask) === getTaskKey(currentTask)) return false

    const otherStartStr = otherTask._visualStartTime || otherTask.startDatetime
    if (!otherStartStr) return false

    const otherStart = new Date(otherStartStr)
    const timeDifference = Math.abs(otherStart.getTime() - currentEnd.getTime())

    // Consider overlap if next task starts within 2 minutes (120000ms)
    return timeDifference <= 120000 && otherStart >= currentEnd
  })

  return overlapping
}

// Intelligent task positioning with overlap detection
const getTaskTimePositionIntelligent = (task: any, dayTasks: Task[]) => {
  // Use visual times for positioning if available (for split multi-day tasks)
  const startTimeStr = task._visualStartTime || task.startDatetime
  const endTimeStr = task._visualEndTime || task.endDatetime

  if (!startTimeStr) return { top: '0px', height: '18px' }

  const start = new Date(startTimeStr)
  const end = endTimeStr ? new Date(endTimeStr) : new Date(start.getTime() + 60 * 60 * 1000) // Default 1 hour

  // Calculate position based on hours (each hour = 64px height)
  const startHour = start.getHours() + start.getMinutes() / 60
  let endHour = end.getHours() + end.getMinutes() / 60

  // For VISUAL positioning: limit to current day (max 24:00)
  if (endHour < startHour) {
    endHour = 24
  }

  const topPosition = startHour * 64 // 64px per hour (h-16 = 4rem = 64px)
  const actualDuration = (endHour - startHour) * 64

  // Intelligent height calculation
  let height
  if (actualDuration >= 18) {
    // Task is naturally large enough, use actual duration
    height = actualDuration
  } else {
    // Task is very short, check for overlaps
    const hasOverlap = hasOverlapWithNext(task, dayTasks)
    if (hasOverlap) {
      // Use minimal height to avoid overlaps
      height = Math.max(actualDuration, 3)
    } else {
      // No overlap, use comfortable minimum for readability
      height = 18
    }
  }

  return {
    top: `${topPosition}px`,
    height: `${height}px`
  }
}

// Computed properties
const weekDaysWithIndicators = computed(() => {
  indicatorsUpdateTrigger.value // Force reactivity

  return getWeekDays(props.currentDate).map(day => {
    const dayTasks = getTasksWithSplitsForDate(day)
    const indicators = getTasksOverflowIndicators(dayTasks)
    const layouts = overlaps.calculateLayout(dayTasks)

    return {
      day,
      tasks: dayTasks,
      indicators,
      layouts
    }
  })
})

// Event handlers
const handleTaskClick = (task: Task) => {
  emit('taskClick', task)
}

const handleWeeklyScroll = (event: Event) => {
  emit('scroll', event)
}

// Expose refs for parent component
defineExpose({
  weeklyScrollContainer
})
</script>