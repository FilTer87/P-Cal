<template>
  <div
    :class="[
      'time-slot relative',
      getSlotClasses(),
      {
        'border-t': showBorder,
        'border-dashed border-gray-300 dark:border-gray-600': isDashed,
        'cursor-pointer hover:bg-blue-50 dark:hover:bg-blue-900/10': clickable && !disabled,
        'bg-blue-50/50 dark:bg-blue-900/5': isCurrentTime && !disabled,
        'opacity-50': disabled
      }
    ]"
    :style="slotStyle"
    @click="handleClick"
    @dragover.prevent="handleDragOver"
    @dragenter.prevent="handleDragEnter"  
    @dragleave="handleDragLeave"
    @drop.prevent="handleDrop"
    :title="slotTitle"
    role="gridcell"
    :aria-label="ariaLabel"
    :tabindex="clickable && !disabled ? 0 : -1"
    @keydown.enter="handleClick"
    @keydown.space.prevent="handleClick"
  >
    <!-- Time label (for day/week view) -->
    <div
      v-if="showTimeLabel && timeLabel"
      :class="[
        'time-label absolute left-0 top-0 -translate-x-full px-2 py-1',
        'text-xs text-gray-500 dark:text-gray-400',
        {
          'font-semibold text-blue-600 dark:text-blue-400': isCurrentTime,
          'text-red-600 dark:text-red-400': isPastTime && highlightPastTimes
        }
      ]"
    >
      {{ timeLabel }}
    </div>

    <!-- Current time indicator -->
    <div
      v-if="isCurrentTime && showCurrentTimeIndicator"
      class="absolute left-0 right-0 top-0 h-0.5 bg-red-500 z-10"
      :style="currentTimeIndicatorStyle"
    >
      <div class="absolute -left-1 -top-1 w-2 h-2 bg-red-500 rounded-full"></div>
    </div>

    <!-- Tasks container -->
    <div
      v-if="tasks.length > 0 || $slots.default"
      :class="[
        'tasks-container relative h-full',
        {
          'p-1': variant === 'week',
          'p-0.5': variant === 'day' && compact,
          'p-2': variant === 'day' && !compact
        }
      ]"
    >
      <!-- Tasks -->
      <div
        v-for="(task, index) in visibleTasks"
        :key="task.id"
        :class="[
          'task-in-slot absolute left-0 right-0 z-10',
          {
            'mb-0.5': index < visibleTasks.length - 1 && variant === 'day',
            'opacity-75': task.completed
          }
        ]"
        :style="getTaskPositionStyle(task, index)"
      >
        <TaskCard
          :task="task"
          :variant="variant"
          :compact="compact"
          :draggable="allowTaskDrag"
          :show-time="false"
          :show-completion="showTaskCompletion"
          @click="$emit('task-click', task)"
          @toggle-completion="$emit('task-toggle-completion', $event)"
          @edit-task="$emit('task-edit', $event)"
          @delete-task="$emit('task-delete', $event)"
          @drag-start="$emit('task-drag-start', $event)"
          @drag-end="$emit('task-drag-end')"
        />
      </div>

      <!-- More tasks indicator -->
      <div
        v-if="hasHiddenTasks"
        :class="[
          'more-tasks absolute bottom-1 right-1 text-xs text-gray-500 dark:text-gray-400',
          'bg-white dark:bg-gray-800 rounded px-1 border border-gray-200 dark:border-gray-600',
          'cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700'
        ]"
        @click.stop="$emit('show-all-tasks', { time, date: slotDate })"
        :title="`+${hiddenTasksCount} altre attività`"
      >
        +{{ hiddenTasksCount }}
      </div>

      <!-- Slot content (custom content via slot) -->
      <slot 
        :time="time"
        :date="slotDate"
        :isCurrentTime="isCurrentTime"
        :isPastTime="isPastTime"
        :tasks="tasks"
        :visibleTasks="visibleTasks"
      />
    </div>

    <!-- Empty slot placeholder -->
    <div
      v-else-if="clickable && !disabled"
      class="empty-slot h-full flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity"
    >
      <PlusIcon class="w-4 h-4 text-gray-400 dark:text-gray-500" />
    </div>

    <!-- Drop zone indicator -->
    <div
      v-if="isDragOver"
      class="absolute inset-0 bg-blue-100 dark:bg-blue-900/20 border-2 border-dashed border-blue-400 dark:border-blue-500 rounded z-20 flex items-center justify-center"
    >
      <span class="text-blue-600 dark:text-blue-400 text-sm font-medium">
        Rilascia qui
      </span>
    </div>

    <!-- Busy time overlay -->
    <div
      v-if="isBusy"
      class="absolute inset-0 bg-gray-100 dark:bg-gray-800/50 opacity-75 z-5"
    >
      <div class="h-full flex items-center justify-center">
        <span class="text-xs text-gray-500 dark:text-gray-400 font-medium">
          Occupato
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { format, isBefore, isAfter, isSameHour, parseISO } from 'date-fns'
import { useTheme } from '../../composables/useTheme'
import type { Task } from '../../types/task'
import TaskCard from './TaskCard.vue'

// Icons
import { PlusIcon } from '@heroicons/vue/24/outline'

// Props
interface Props {
  time: string // HH:mm format
  date?: Date
  tasks?: Task[]
  variant?: 'week' | 'day'
  height?: number
  duration?: number // minutes
  showTimeLabel?: boolean
  showBorder?: boolean
  showCurrentTimeIndicator?: boolean
  showTaskCompletion?: boolean
  clickable?: boolean
  allowTaskDrag?: boolean
  disabled?: boolean
  compact?: boolean
  maxVisibleTasks?: number
  isDashed?: boolean
  highlightPastTimes?: boolean
  isBusy?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  date: () => new Date(),
  tasks: () => [],
  variant: 'day',
  height: 60,
  duration: 60,
  showTimeLabel: true,
  showBorder: true,
  showCurrentTimeIndicator: true,
  showTaskCompletion: false,
  clickable: true,
  allowTaskDrag: true,
  disabled: false,
  compact: false,
  maxVisibleTasks: 3,
  isDashed: false,
  highlightPastTimes: false,
  isBusy: false
})

// Emits
const emit = defineEmits<{
  'click': [{ time: string; date: Date }]
  'task-click': [task: Task]
  'task-toggle-completion': [taskId: number]
  'task-edit': [task: Task]
  'task-delete': [task: Task]
  'task-drag-start': [task: Task]
  'task-drag-end': []
  'task-drop': [{ task: Task; time: string; date: Date }]
  'show-all-tasks': [{ time: string; date: Date }]
}>()

// Composables
const { cardClass, textClass } = useTheme()

// State
const isDragOver = ref(false)

// Computed properties
const slotDate = computed(() => props.date || new Date())

const timeLabel = computed(() => {
  if (!props.showTimeLabel) return ''
  
  // Format time for display
  const [hours, minutes] = props.time.split(':')
  const hour = parseInt(hours)
  const minute = parseInt(minutes)
  
  if (minute === 0) {
    return `${hour.toString().padStart(2, '0')}:00`
  }
  
  return props.time
})

const slotDateTime = computed(() => {
  const [hours, minutes] = props.time.split(':')
  const dateTime = new Date(slotDate.value)
  dateTime.setHours(parseInt(hours), parseInt(minutes), 0, 0)
  return dateTime
})

const isCurrentTime = computed(() => {
  const now = new Date()
  const slotTime = slotDateTime.value
  
  // Check if this time slot contains the current time
  const slotEnd = new Date(slotTime)
  slotEnd.setMinutes(slotEnd.getMinutes() + props.duration)
  
  return now >= slotTime && now < slotEnd && 
         format(now, 'yyyy-MM-dd') === format(slotDate.value, 'yyyy-MM-dd')
})

const isPastTime = computed(() => {
  const now = new Date()
  return slotDateTime.value < now
})

const visibleTasks = computed(() => {
  if (!props.maxVisibleTasks) return props.tasks || []
  return (props.tasks || []).slice(0, props.maxVisibleTasks)
})

const hasHiddenTasks = computed(() => {
  return (props.tasks || []).length > (props.maxVisibleTasks || 0)
})

const hiddenTasksCount = computed(() => {
  const totalTasks = (props.tasks || []).length
  const visible = props.maxVisibleTasks || 0
  return Math.max(0, totalTasks - visible)
})

const slotStyle = computed(() => {
  const style: Record<string, string> = {}
  
  if (props.height) {
    style.height = `${props.height}px`
    style.minHeight = `${props.height}px`
  }
  
  return style
})

const currentTimeIndicatorStyle = computed(() => {
  if (!isCurrentTime.value) return {}
  
  const now = new Date()
  const slotTime = slotDateTime.value
  const minutesPassed = (now.getTime() - slotTime.getTime()) / (1000 * 60)
  const percentageThrough = (minutesPassed / props.duration) * 100
  
  return {
    top: `${Math.min(100, Math.max(0, percentageThrough))}%`
  }
})

const slotTitle = computed(() => {
  let title = `${timeLabel.value}`
  if (slotDate.value) {
    title += ` - ${format(slotDate.value, 'dd/MM/yyyy')}`
  }
  if (props.tasks && props.tasks.length > 0) {
    title += ` - ${props.tasks.length} attività`
  }
  if (props.isBusy) {
    title += ' (Occupato)'
  }
  return title
})

const ariaLabel = computed(() => {
  let label = `Fascia oraria ${timeLabel.value}`
  if (slotDate.value) {
    label += ` del ${format(slotDate.value, 'dd/MM/yyyy')}`
  }
  if (props.tasks && props.tasks.length > 0) {
    label += `, ${props.tasks.length} attività`
  }
  if (isCurrentTime.value) {
    label += ', ora corrente'
  }
  if (props.isBusy) {
    label += ', periodo occupato'
  }
  return label
})

// Style methods
const getSlotClasses = () => {
  const classes = [
    'border-gray-200 dark:border-gray-700',
    'transition-colors duration-150'
  ]
  
  if (props.variant === 'week') {
    classes.push('min-h-[40px]')
  }
  
  if (props.compact) {
    classes.push('text-sm')
  }
  
  return classes
}

const getTaskPositionStyle = (task: Task, index: number) => {
  const style: Record<string, string> = {}
  
  if (props.variant === 'day' && !props.compact) {
    // Stack tasks vertically with small offset
    style.top = `${index * 4}px`
    style.zIndex = `${10 + index}`
  } else if (props.variant === 'week') {
    // Stack tasks more compactly
    style.top = `${index * 2}px`
    style.zIndex = `${10 + index}`
  }
  
  return style
}

// Event handlers
const handleClick = () => {
  if (props.clickable && !props.disabled) {
    emit('click', { 
      time: props.time, 
      date: slotDate.value 
    })
  }
}

const handleDragOver = (event: DragEvent) => {
  if (props.allowTaskDrag && !props.disabled) {
    event.preventDefault()
    isDragOver.value = true
  }
}

const handleDragEnter = (event: DragEvent) => {
  if (props.allowTaskDrag && !props.disabled) {
    event.preventDefault()
    isDragOver.value = true
  }
}

const handleDragLeave = (event: DragEvent) => {
  // Only set to false if we're leaving the entire slot area
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const isLeavingSlot = 
    event.clientX < rect.left || 
    event.clientX > rect.right || 
    event.clientY < rect.top || 
    event.clientY > rect.bottom
    
  if (isLeavingSlot) {
    isDragOver.value = false
  }
}

const handleDrop = (event: DragEvent) => {
  if (props.allowTaskDrag && !props.disabled) {
    isDragOver.value = false
    
    try {
      const data = event.dataTransfer?.getData('text/plain')
      if (data) {
        const { taskId, title } = JSON.parse(data)
        // Find the task (this would typically come from a parent component)
        const task = props.tasks?.find(t => t.id === taskId)
        if (task) {
          emit('task-drop', { 
            task, 
            time: props.time, 
            date: slotDate.value 
          })
        }
      }
    } catch (error) {
      console.warn('Failed to handle task drop:', error)
    }
  }
}
</script>

<style scoped>
.time-slot {
  position: relative;
}

/* Task stacking animations */
.task-in-slot {
  transition: all 0.2s ease;
}

.task-in-slot:hover {
  transform: translateY(-1px);
  z-index: 20 !important;
}

/* Current time indicator animation */
.time-slot:has(.current-time-indicator) {
  animation: pulse-subtle 2s ease-in-out infinite;
}

@keyframes pulse-subtle {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.95;
  }
}

/* Drag over animation */
.time-slot.drag-over {
  transform: scale(1.02);
  transition: transform 0.2s ease;
}

/* Empty slot hover effects */
.empty-slot {
  transition: all 0.2s ease;
}

/* Focus styles for accessibility */
.time-slot:focus {
  outline: none;
  box-shadow: inset 0 0 0 2px rgb(59 130 246 / 0.5);
  border-radius: 4px;
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .time-slot {
    border: 1px solid black;
  }
  
  .current-time-indicator {
    background-color: red !important;
    height: 2px !important;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .time-slot,
  .task-in-slot,
  .empty-slot {
    transition: none !important;
    animation: none !important;
  }
}

/* Print styles */
@media print {
  .time-slot {
    break-inside: avoid;
    page-break-inside: avoid;
  }
  
  .empty-slot,
  .more-tasks {
    display: none;
  }
}
</style>