<template>
  <div
    :class="[
      'task-card group relative rounded-lg border-l-4 transition-all duration-200 cursor-pointer',
      getCardClasses(),
      getPriorityBorderColor(),
      {
        'opacity-60': task.completed,
        'hover:shadow-md': !disabled,
        'transform hover:-translate-y-0.5': variant === 'month' && !disabled,
        'border-l-0 border-t-4': variant === 'agenda'
      }
    ]"
    :draggable="draggable && !disabled"
    @click="handleClick"
    @dragstart="handleDragStart"
    @dragend="handleDragEnd"
    :title="getTaskTooltip()"
    :aria-label="`Attività: ${task.title}`"
    role="button"
    tabindex="0"
    @keydown.enter="handleClick"
    @keydown.space.prevent="handleClick"
  >
    <!-- Priority indicator (for non-agenda views) -->
    <div
      v-if="variant !== 'agenda' && showPriority"
      :class="[
        'absolute top-1 right-1 w-2 h-2 rounded-full',
        getPriorityIndicatorColor()
      ]"
      :title="`Priorità: ${getPriorityLabel()}`"
    ></div>

    <!-- Custom color bar (if task has color) -->
    <div
      v-if="task.color && variant !== 'agenda'"
      class="absolute inset-y-0 left-0 w-1 rounded-l-lg"
      :style="{ backgroundColor: task.color }"
    ></div>

    <!-- Task content -->
    <div :class="['p-2', { 'pr-6': showPriority && variant !== 'agenda', 'ml-2': task.color }]">
      <!-- Header: Time and completion status -->
      <div 
        v-if="showTime || showCompletion || task.isAllDay"
        class="flex items-center justify-between mb-1"
      >
        <!-- Time display -->
        <div
          v-if="showTime && (task.dueDate || task.startDate)"
          :class="[
            'text-xs font-medium flex items-center space-x-1',
            getTimeColor()
          ]"
        >
          <ClockIcon v-if="!task.isAllDay" class="w-3 h-3" />
          <CalendarDaysIcon v-if="task.isAllDay" class="w-3 h-3" />
          <span>{{ formatTaskTime() }}</span>
        </div>

        <!-- Completion checkbox -->
        <button
          v-if="showCompletion"
          @click.stop="toggleCompletion"
          :class="[
            'flex-shrink-0 w-4 h-4 rounded border-2 transition-colors',
            getCompletionCheckboxClasses()
          ]"
          :aria-label="task.completed ? 'Segna come non completata' : 'Segna come completata'"
        >
          <CheckIcon 
            v-if="task.completed" 
            class="w-3 h-3 text-white m-0.5" 
          />
        </button>
      </div>

      <!-- Task title -->
      <h3 :class="[
        'font-medium leading-tight',
        getTitleClasses(),
        {
          'line-through': task.completed,
          'text-xs': variant === 'month' && isSmallCard,
          'text-sm': variant === 'month' && !isSmallCard || variant === 'week' || variant === 'day',
          'text-base': variant === 'agenda'
        }
      ]">
        {{ task.title }}
      </h3>

      <!-- Task description (agenda view only) -->
      <p 
        v-if="variant === 'agenda' && task.description && showDescription"
        :class="[
          'text-sm mt-1 line-clamp-2',
          getDescriptionColor()
        ]"
      >
        {{ task.description }}
      </p>

      <!-- Location (if available) -->
      <div
        v-if="task.location && (variant === 'agenda' || variant === 'day')"
        :class="[
          'flex items-center mt-1 text-xs',
          getDescriptionColor()
        ]"
      >
        <MapPinIcon class="w-3 h-3 mr-1" />
        <span class="truncate">{{ task.location }}</span>
      </div>

      <!-- Task metadata (agenda view) -->
      <div 
        v-if="variant === 'agenda'"
        class="flex items-center justify-between mt-2"
      >
        <div class="flex items-center space-x-2 text-xs">
          <!-- Priority badge -->
          <span
            v-if="showPriority"
            :class="[
              'px-2 py-1 rounded-full text-xs font-medium',
              getPriorityBadgeClasses()
            ]"
          >
            {{ getPriorityLabel() }}
          </span>

          <!-- Reminder indicator -->
          <span
            v-if="task.reminders && task.reminders.length > 0"
            :class="[
              'flex items-center text-xs',
              textClass
            ]"
            :title="`${task.reminders.length} promemoria`"
          >
            <BellIcon class="w-3 h-3 mr-1" />
            {{ task.reminders.length }}
          </span>
        </div>

        <!-- Action buttons -->
        <div 
          v-if="showActions"
          class="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition-opacity"
        >
          <button
            @click.stop="$emit('edit-task', task)"
            :class="[
              'p-1 rounded transition-colors hover:bg-gray-100 dark:hover:bg-gray-700',
              textClass
            ]"
            title="Modifica attività"
            aria-label="Modifica attività"
          >
            <PencilIcon class="w-3 h-3" />
          </button>

          <button
            @click.stop="$emit('delete-task', task)"
            class="p-1 rounded transition-colors hover:bg-red-100 dark:hover:bg-red-900/20 text-red-600 dark:text-red-400"
            title="Elimina attività"
            aria-label="Elimina attività"
          >
            <TrashIcon class="w-3 h-3" />
          </button>
        </div>
      </div>

      <!-- Overdue indicator -->
      <div
        v-if="isOverdue && !task.completed"
        class="flex items-center mt-1 text-xs text-red-600 dark:text-red-400"
      >
        <ExclamationTriangleIcon class="w-3 h-3 mr-1" />
        <span>In ritardo</span>
      </div>

      <!-- Due soon indicator -->
      <div
        v-if="isDueSoon && !task.completed && !isOverdue"
        class="flex items-center mt-1 text-xs text-yellow-600 dark:text-yellow-400"
      >
        <ClockIcon class="w-3 h-3 mr-1" />
        <span>Scade presto</span>
      </div>
    </div>

    <!-- Drag indicator -->
    <div
      v-if="draggable && !disabled"
      class="absolute inset-y-0 left-0 w-1 bg-blue-500 rounded-l-lg opacity-0 group-hover:opacity-100 transition-opacity"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTasks } from '../../composables/useTasks'
import { useTheme } from '../../composables/useTheme'
import type { Task } from '../../types/task'
import { formatTime, formatDate } from '../../utils/dateHelpers'
import { TASK_PRIORITY_CONFIG } from '../../types/task'

// Icons
import {
  CheckIcon,
  BellIcon,
  PencilIcon,
  TrashIcon,
  ExclamationTriangleIcon,
  ClockIcon,
  CalendarDaysIcon,
  MapPinIcon
} from '@heroicons/vue/24/outline'

// Props
interface Props {
  task: Task
  variant?: 'month' | 'week' | 'day' | 'agenda'
  showTime?: boolean
  showCompletion?: boolean
  showPriority?: boolean
  showDescription?: boolean
  showActions?: boolean
  draggable?: boolean
  disabled?: boolean
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'month',
  showTime: true,
  showCompletion: false,
  showPriority: true,
  showDescription: true,
  showActions: true,
  draggable: false,
  disabled: false,
  compact: false
})

// Emits
const emit = defineEmits<{
  'click': [task: Task]
  'toggle-completion': [taskId: number]
  'edit-task': [task: Task]
  'delete-task': [task: Task]
  'drag-start': [task: Task]
  'drag-end': []
}>()

// Composables
const { toggleTaskCompletion, isOverdue: isTaskOverdue, isDueSoon: isTaskDueSoon } = useTasks()
const { 
  textClass, 
  cardClass, 
  getTaskPriorityColors,
  getThemeValue
} = useTheme()

// Computed properties
const isSmallCard = computed(() => props.compact || props.variant === 'month')

const isOverdue = computed(() => isTaskOverdue(props.task))
const isDueSoon = computed(() => isTaskDueSoon(props.task))

const priorityConfig = computed(() => 
  TASK_PRIORITY_CONFIG[props.task.priority] || TASK_PRIORITY_CONFIG.MEDIUM
)

const taskPriorityColors = computed(() => getTaskPriorityColors(props.task.priority))

// Style methods
const getCardClasses = () => {
  const baseClasses = 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700'
  
  if (props.task.completed) {
    return `${baseClasses} bg-gray-50 dark:bg-gray-800/50`
  }
  
  if (isOverdue.value) {
    return 'bg-red-50 dark:bg-red-900/10 border-red-100 dark:border-red-900/20'
  }
  
  if (isDueSoon.value) {
    return 'bg-yellow-50 dark:bg-yellow-900/10 border-yellow-100 dark:border-yellow-900/20'
  }
  
  return baseClasses
}

const getPriorityBorderColor = () => {
  return taskPriorityColors.value.border
}

const getPriorityIndicatorColor = () => {
  const colors = {
    LOW: 'bg-green-500',
    MEDIUM: 'bg-yellow-500',
    HIGH: 'bg-orange-500',
    URGENT: 'bg-red-500'
  }
  return colors[props.task.priority] || colors.MEDIUM
}

const getPriorityBadgeClasses = () => {
  return `${taskPriorityColors.value.bg} ${taskPriorityColors.value.text}`
}

const getTitleClasses = () => {
  if (props.task.completed) {
    return 'text-gray-500 dark:text-gray-400'
  }
  
  if (isOverdue.value) {
    return 'text-red-700 dark:text-red-300'
  }
  
  return textClass.value
}

const getDescriptionColor = () => {
  if (props.task.completed) {
    return 'text-gray-400 dark:text-gray-500'
  }
  
  return 'text-gray-600 dark:text-gray-300'
}

const getTimeColor = () => {
  if (props.task.completed) {
    return 'text-gray-400 dark:text-gray-500'
  }
  
  if (isOverdue.value) {
    return 'text-red-600 dark:text-red-400'
  }
  
  if (isDueSoon.value) {
    return 'text-yellow-600 dark:text-yellow-400'
  }
  
  return 'text-blue-600 dark:text-blue-400'
}

const getCompletionCheckboxClasses = () => {
  if (props.task.completed) {
    return 'bg-green-500 border-green-500'
  }
  
  return 'border-gray-300 dark:border-gray-600 hover:border-green-400'
}

// Methods
const getPriorityLabel = () => {
  return priorityConfig.value.label
}

const formatTaskTime = () => {
  const task = props.task
  
  // Handle all-day tasks
  if (task.isAllDay) {
    return 'Tutto il giorno'
  }
  
  // Use startDate if available, otherwise dueDate
  const dateToFormat = task.startDate || task.dueDate
  if (!dateToFormat) return ''
  
  if (props.variant === 'agenda') {
    return formatDate(dateToFormat, 'dd/MM/yyyy HH:mm')
  }
  
  return formatTime(dateToFormat)
}

const getTaskTooltip = () => {
  let tooltip = props.task.title
  
  if (props.task.description) {
    tooltip += `\n${props.task.description}`
  }
  
  if (props.task.location) {
    tooltip += `\nLuogo: ${props.task.location}`
  }
  
  if (props.task.isAllDay) {
    tooltip += '\nTutto il giorno'
  } else if (props.task.startDate && props.task.endDate) {
    tooltip += `\nInizio: ${formatDate(props.task.startDate, 'dd/MM/yyyy HH:mm')}`
    tooltip += `\nFine: ${formatDate(props.task.endDate, 'dd/MM/yyyy HH:mm')}`
  } else if (props.task.dueDate) {
    tooltip += `\nScadenza: ${formatDate(props.task.dueDate, 'dd/MM/yyyy HH:mm')}`
  }
  
  tooltip += `\nPriorità: ${getPriorityLabel()}`
  
  if (props.task.reminders && props.task.reminders.length > 0) {
    tooltip += `\nPromemoria: ${props.task.reminders.length}`
  }
  
  return tooltip
}

const handleClick = () => {
  if (!props.disabled) {
    emit('click', props.task)
  }
}

const toggleCompletion = async () => {
  if (!props.disabled) {
    emit('toggle-completion', props.task.id)
    await toggleTaskCompletion(props.task.id)
  }
}

const handleDragStart = (event: DragEvent) => {
  if (props.draggable && !props.disabled) {
    emit('drag-start', props.task)
    
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/plain', JSON.stringify({
        taskId: props.task.id,
        title: props.task.title
      }))
    }
  }
}

const handleDragEnd = () => {
  if (props.draggable && !props.disabled) {
    emit('drag-end')
  }
}
</script>

<style scoped>
.task-card {
  /* Add any custom styles here */
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Drag styles */
.task-card[draggable="true"]:hover {
  cursor: grab;
}

.task-card[draggable="true"]:active {
  cursor: grabbing;
}

/* Focus styles for accessibility */
.task-card:focus {
  @apply outline-none ring-2 ring-blue-500 ring-opacity-50;
}

/* Animation for task completion */
.task-card.completed {
  transition: all 0.3s ease;
}
</style>