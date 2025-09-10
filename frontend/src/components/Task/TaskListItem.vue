<template>
  <div
    :class="[
      'task-list-item group relative cursor-pointer transition-all duration-200',
      getItemClasses(),
      {
        'opacity-60': task.completed,
        'ring-2 ring-blue-500 ring-opacity-50': selected,
        'hover:shadow-sm': !compact,
        'p-3': !compact,
        'p-2': compact
      }
    ]"
    @click="handleClick"
    :aria-label="`Attività: ${task.title}`"
    role="button"
    tabindex="0"
    @keydown.enter="handleClick"
    @keydown.space.prevent="handleClick"
  >
    <!-- Selection Checkbox -->
    <div
      v-if="showSelection"
      class="absolute top-3 left-3 z-10"
      @click.stop
    >
      <input
        type="checkbox"
        :checked="selected"
        @change="$emit('toggle-selection')"
        class="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500 dark:focus:ring-blue-600 dark:ring-offset-gray-800 dark:bg-gray-700 dark:border-gray-600"
        :aria-label="`Seleziona attività ${task.title}`"
      />
    </div>

    <!-- Task Color Bar -->
    <div
      v-if="task.color"
      class="absolute inset-y-0 left-0 w-1 rounded-l-lg"
      :style="{ backgroundColor: task.color }"
    ></div>

    <!-- Main Content -->
    <div :class="[
      'flex items-start gap-3',
      showSelection ? 'ml-6' : 'ml-2'
    ]">
      <!-- Completion Checkbox -->
      <button
        @click.stop="$emit('toggle-completion')"
        :class="[
          'flex-shrink-0 mt-1 w-5 h-5 rounded border-2 transition-all duration-200',
          getCompletionCheckboxClasses()
        ]"
        :aria-label="task.completed ? 'Segna come non completata' : 'Segna come completata'"
        :title="task.completed ? 'Segna come non completata' : 'Segna come completata'"
      >
        <CheckIcon 
          v-if="task.completed" 
          class="w-3 h-3 text-white m-auto" 
        />
      </button>

      <!-- Task Content -->
      <div class="flex-1 min-w-0">
        <!-- Header: Title and Priority -->
        <div class="flex items-start justify-between gap-2">
          <h3 :class="[
            'font-medium leading-tight',
            getTitleClasses(),
            {
              'line-through': task.completed,
              'text-sm': compact,
              'text-base': !compact
            }
          ]">
            {{ task.title }}
          </h3>

          <!-- Priority Badge -->
          <span
            v-if="showPriority"
            :class="[
              'flex-shrink-0 px-2 py-1 text-xs font-medium rounded-full',
              getPriorityBadgeClasses()
            ]"
            :title="`Priorità: ${getPriorityLabel()}`"
          >
            {{ getPriorityLabel() }}
          </span>
        </div>

        <!-- Description -->
        <p
          v-if="task.description && !compact"
          :class="[
            'mt-1 text-sm line-clamp-2',
            getDescriptionColor()
          ]"
        >
          {{ task.description }}
        </p>

        <!-- Metadata Row -->
        <div class="flex items-center justify-between mt-2">
          <div class="flex items-center space-x-3 text-xs">
            <!-- Due Date -->
            <div
              v-if="task.dueDate"
              class="flex items-center space-x-1"
              :title="`Scadenza: ${formatDate(task.dueDate, 'dd/MM/yyyy HH:mm')}`"
            >
              <ClockIcon class="w-3 h-3" />
              <span :class="getDateColor()">
                {{ formatTaskDate() }}
              </span>
            </div>

            <!-- Location -->
            <div
              v-if="task.location"
              class="flex items-center space-x-1"
              :title="`Luogo: ${task.location}`"
            >
              <MapPinIcon class="w-3 h-3" />
              <span :class="textClass" class="truncate max-w-24">
                {{ task.location }}
              </span>
            </div>

            <!-- Reminders -->
            <div
              v-if="task.reminders && task.reminders.length > 0"
              class="flex items-center space-x-1"
              :title="`${task.reminders.length} promemoria`"
            >
              <BellIcon class="w-3 h-3" />
              <span :class="textClass">
                {{ task.reminders.length }}
              </span>
            </div>

            <!-- All Day Indicator -->
            <span
              v-if="task.isAllDay"
              :class="[
                'px-1.5 py-0.5 text-xs rounded',
                'bg-blue-100 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300'
              ]"
            >
              Tutto il giorno
            </span>
          </div>

          <!-- Action Buttons -->
          <div class="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
              @click.stop="$emit('edit', task)"
              :class="[
                'p-1.5 rounded transition-colors',
                'text-gray-400 hover:text-blue-600 dark:hover:text-blue-400',
                'hover:bg-blue-50 dark:hover:bg-blue-900/20'
              ]"
              title="Modifica attività"
              aria-label="Modifica attività"
            >
              <PencilIcon class="w-3 h-3" />
            </button>

            <button
              @click.stop="$emit('delete', task)"
              :class="[
                'p-1.5 rounded transition-colors',
                'text-gray-400 hover:text-red-600 dark:hover:text-red-400',
                'hover:bg-red-50 dark:hover:bg-red-900/20'
              ]"
              title="Elimina attività"
              aria-label="Elimina attività"
            >
              <TrashIcon class="w-3 h-3" />
            </button>
          </div>
        </div>

        <!-- Status Indicators -->
        <div class="flex items-center space-x-2 mt-2">
          <!-- Overdue Indicator -->
          <div
            v-if="isOverdue && !task.completed"
            class="flex items-center space-x-1 text-xs text-red-600 dark:text-red-400"
          >
            <ExclamationTriangleIcon class="w-3 h-3" />
            <span>In ritardo</span>
          </div>

          <!-- Due Soon Indicator -->
          <div
            v-else-if="isDueSoon && !task.completed && !isOverdue"
            class="flex items-center space-x-1 text-xs text-yellow-600 dark:text-yellow-400"
          >
            <ClockIcon class="w-3 h-3" />
            <span>Scade presto</span>
          </div>

          <!-- Completed Indicator -->
          <div
            v-if="task.completed"
            class="flex items-center space-x-1 text-xs text-green-600 dark:text-green-400"
          >
            <CheckCircleIcon class="w-3 h-3" />
            <span>Completata</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Drag Handle (if draggable) -->
    <div
      v-if="draggable"
      class="absolute inset-y-0 right-0 flex items-center pr-3 opacity-0 group-hover:opacity-100 transition-opacity cursor-grab"
      title="Trascina per riordinare"
    >
      <svg class="w-4 h-4 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
        <path d="M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z" />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTasks } from '../../composables/useTasks'
import { useTheme } from '../../composables/useTheme'
import { formatDistanceToNow, format } from 'date-fns'
import { it } from 'date-fns/locale'
import { TASK_PRIORITY_CONFIG } from '../../types/task'
import type { Task } from '../../types/task'

// Icons
import {
  CheckIcon,
  ClockIcon,
  MapPinIcon,
  BellIcon,
  PencilIcon,
  TrashIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon
} from '@heroicons/vue/24/outline'

// Props
interface Props {
  task: Task
  selected?: boolean
  showSelection?: boolean
  showPriority?: boolean
  compact?: boolean
  draggable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selected: false,
  showSelection: false,
  showPriority: true,
  compact: false,
  draggable: false
})

// Emits
const emit = defineEmits<{
  'click': []
  'toggle-selection': []
  'toggle-completion': []
  'edit': [task: Task]
  'delete': [task: Task]
}>()

// Composables
const { isOverdue: isTaskOverdue, isDueSoon: isTaskDueSoon } = useTasks()
const { textClass, cardClass } = useTheme()

// Computed properties
const isOverdue = computed(() => isTaskOverdue(props.task))
const isDueSoon = computed(() => isTaskDueSoon(props.task))

const priorityConfig = computed(() => 
  TASK_PRIORITY_CONFIG[props.task.priority] || TASK_PRIORITY_CONFIG.MEDIUM
)

// Style methods
const getItemClasses = () => {
  const baseClasses = `rounded-lg border ${cardClass.value} border-gray-200 dark:border-gray-700`
  
  if (props.task.completed) {
    return `${baseClasses} bg-gray-50 dark:bg-gray-800/50`
  }
  
  if (isOverdue.value) {
    return `${baseClasses} bg-red-50 dark:bg-red-900/10 border-red-200 dark:border-red-800/50`
  }
  
  if (isDueSoon.value) {
    return `${baseClasses} bg-yellow-50 dark:bg-yellow-900/10 border-yellow-200 dark:border-yellow-800/50`
  }
  
  return baseClasses
}

const getCompletionCheckboxClasses = () => {
  if (props.task.completed) {
    return 'bg-green-500 border-green-500 hover:bg-green-600'
  }
  
  return 'border-gray-300 dark:border-gray-600 hover:border-green-400 dark:hover:border-green-500 bg-white dark:bg-gray-700'
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

const getDateColor = () => {
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

const getPriorityBadgeClasses = () => {
  const config = priorityConfig.value
  return `${config.bgColor} ${config.color}`
}

// Methods
const getPriorityLabel = () => {
  return priorityConfig.value.label
}

const formatTaskDate = () => {
  if (!props.task.dueDate) return ''
  
  const date = new Date(props.task.dueDate)
  const now = new Date()
  
  // If it's today or within 24 hours, show relative time
  if (Math.abs(date.getTime() - now.getTime()) < 24 * 60 * 60 * 1000) {
    return formatDistanceToNow(date, { 
      addSuffix: true,
      locale: it 
    })
  }
  
  // Otherwise show formatted date
  return format(date, 'dd/MM/yyyy', { locale: it })
}

const formatDate = (dateStr: string, formatStr: string) => {
  return format(new Date(dateStr), formatStr, { locale: it })
}

const handleClick = () => {
  emit('click')
}
</script>

<style scoped>
.task-list-item {
  /* Custom styles if needed */
}

/* Line clamp for descriptions */
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Hover effects */
.task-list-item:hover {
  transform: translateY(-1px);
}

.task-list-item:active {
  transform: translateY(0);
}

/* Focus styles for accessibility */
.task-list-item:focus {
  outline: none;
  ring: 2px;
  ring-color: rgb(59 130 246);
  ring-opacity: 0.5;
}

/* Smooth transitions */
.task-list-item * {
  transition-property: color, background-color, border-color, transform, opacity;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
}

/* Button hover effects */
.task-list-item button:hover {
  transform: scale(1.05);
}

.task-list-item button:active {
  transform: scale(0.95);
}

/* Draggable cursor */
.cursor-grab:active {
  cursor: grabbing;
}

/* Mobile optimizations */
@media (max-width: 640px) {
  .task-list-item .flex.items-center.space-x-3 {
    flex-wrap: wrap;
    row-gap: 0.5rem;
  }
  
  .task-list-item .max-w-24 {
    max-width: 6rem;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .task-list-item {
    border-width: 2px;
  }
  
  .task-list-item.selected {
    border-color: #000;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .task-list-item,
  .task-list-item * {
    transition: none;
  }
}
</style>