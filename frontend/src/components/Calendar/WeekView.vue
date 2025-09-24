<template>
  <div class="h-full flex flex-col">
    <!-- Week Header -->
    <div class="grid grid-cols-8 gap-px bg-gray-200 dark:bg-gray-600 mb-4">
      <div class="bg-white dark:bg-gray-800 p-2 text-sm font-medium text-center">
        Ora
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
              <div v-for="task in getTasksWithSplitsForDate(dayInfo.day)" :key="`${task.id}-${task._splitIndex || 0}`"
                :style="getTaskTimeStyle(task)"
                @click="handleTaskClick(task)"
                class="absolute left-1 right-1 p-1 rounded text-xs font-medium cursor-pointer pointer-events-auto transition-all hover:shadow-md"
                :class="getTaskTimeDisplayClasses(task)">
                <div class="truncate font-semibold">{{ task.title }}</div>
                <div v-if="task.location" class="truncate text-xs opacity-90">{{ task.location }}</div>
                <div class="text-xs opacity-75">{{ formatTaskTime(task) }}</div>
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
            :key="`top-${task.id}`"
            class="absolute w-3 h-3 rounded-full border border-white shadow-sm"
            :style="{ 
              top: '16px',
              right: `${6 + index * 10}px`,
              backgroundColor: task.color || '#3B82F6'
            }">
          </div>

          <!-- Bottom indicators for hidden tasks below -->
          <div v-for="(task, index) in dayInfo.indicators.bottom" 
            :key="`bottom-${task.id}`"
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
import { format } from 'date-fns'
import { isToday } from '../../utils/dateHelpers'
import { useCalendar } from '../../composables/useCalendar'
import { useSettingsStore } from '../../stores/settings'
import type { Task } from '../../types/task'

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

// Use the settings-aware getWeekDays from composable
const getWeekDays = calendar.getWeekDays

const getWeekDayName = (date: Date, short = false) => {
  if (short) {
    return date.toLocaleDateString('it-IT', { weekday: 'short' })
  }
  return date.toLocaleDateString('it-IT', { weekday: 'long' })
}

// Task filtering and splitting logic
const getTasksWithSplitsForDate = (date: Date) => {
  const currentDay = format(date, 'yyyy-MM-dd')
  const allTasksWithSplits: Task[] = []
  
  // Debug logging for date matching
  console.debug(`🗓️ Getting tasks for column ${currentDay} (${format(date, 'E')})`)
  
  const matchingTasks: string[] = []

  props.tasks.forEach(task => {
    if (!task.startDatetime || !task.endDatetime) return

    // Use local timezone for date extraction, not UTC string split
    const taskStartDay = format(new Date(task.startDatetime), 'yyyy-MM-dd')
    const taskEndDay = format(new Date(task.endDatetime), 'yyyy-MM-dd')
    
    // Debug logging for timezone comparison
    const utcStartDay = task.startDatetime.split('T')[0]
    const utcEndDay = task.endDatetime.split('T')[0]
    if (taskStartDay !== utcStartDay || taskEndDay !== utcEndDay) {
      console.debug(`🕐 Timezone fix for Task ${task.id}: UTC(${utcStartDay}-${utcEndDay}) -> Local(${taskStartDay}-${taskEndDay})`)
    }

    if (taskStartDay === taskEndDay) {
      // Single-day task
      if (taskStartDay === currentDay) {
        allTasksWithSplits.push(task)
        matchingTasks.push(`Task ${task.id} (${task.title}) matches single-day`)
      }
    } else {
      // Multi-day task - ONLY if currentDay is within the task range
      if (currentDay >= taskStartDay && currentDay <= taskEndDay) {
        matchingTasks.push(`Task ${task.id} (${task.title}) matches multi-day (${taskStartDay} to ${taskEndDay})`)
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
      }
    }
  })
  
  if (matchingTasks.length > 0) {
    console.debug(`✅ Column ${currentDay}: ${matchingTasks.join('; ')}`)
  }
  
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
  const height = Math.max((endHour - startHour) * 64, 32) // Minimum 32px height
  
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

const formatTaskTime = (task: Task) => {
  if (!task.startDatetime || !task.endDatetime) return ''
  
  // Use visual times if available (for split multi-day tasks)
  const startTimeStr = (task as any)._visualStartTime || task.startDatetime
  const endTimeStr = (task as any)._visualEndTime || task.endDatetime
  
  const start = new Date(startTimeStr)
  const end = new Date(endTimeStr)
  
  if (task.isAllDay) {
    return 'Tutto il giorno'
  }
  
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

// Computed properties
const weekDaysWithIndicators = computed(() => {
  indicatorsUpdateTrigger.value // Force reactivity
  
  return getWeekDays(props.currentDate).map(day => {
    const dayTasks = getTasksWithSplitsForDate(day)
    const indicators = getTasksOverflowIndicators(dayTasks)
    
    return {
      day,
      tasks: dayTasks,
      indicators
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