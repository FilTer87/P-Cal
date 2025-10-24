<template>
  <!-- Recurring Edit Choice Modal -->
  <RecurringEditChoiceModal
    :show="showRecurringEditChoice"
    @close="showRecurringEditChoice = false"
    @edit-all="handleEditAllOccurrences"
    @edit-single="handleEditSingleOccurrence"
  />

  <div v-if="show && task" class="modal-overlay" @click="handleBackdropClick">
    <div class="modal-content" @click.stop>
      <!-- Modal Header -->
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center space-x-3">
          <!-- Task Color Indicator -->
          <div
            class="w-4 h-4 rounded-full flex-shrink-0"
            :style="{ backgroundColor: task.color || '#3788d8' }"
          ></div>
          <h3 class="text-base font-medium text-gray-900 dark:text-white truncate">
            {{ t('tasks.taskDetails') }}
          </h3>
        </div>
        <div class="flex items-center space-x-2">
          <!-- Edit Button -->
          <button @click="handleEdit"
            class="px-3 py-2 text-sm font-medium text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md transition-colors">
            <svg class="h-4 w-4 mr-1.5 inline-block" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            {{ t('tasks.editButton') }}
          </button>
          <!-- Close Button -->
          <button @click="closeModal"
            class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md transition-colors">
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Task Content -->
      <div class="space-y-4">
        <!-- Title -->
        <div>
          <div class="flex items-center space-x-2 mb-1">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a2 2 0 012-2z" />
            </svg>
            <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">{{ t('tasks.taskTitle') }}</h4>
          </div>
          <p class="text-base font-semibold text-gray-900 dark:text-white">{{ task.title }}</p>
        </div>

        <!-- Description -->
        <div v-if="task.description">
          <div class="flex items-center space-x-2 mb-2">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">{{ t('tasks.description') }}</h4>
          </div>
          <p class="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{{ task.description }}</p>
        </div>

        <!-- Date & Time -->
        <div>
          <div class="flex items-center space-x-2 mb-2">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">{{ t('tasks.schedule') }}</h4>
          </div>
          <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-3">
            <div class="flex items-center space-x-4 text-sm">
              <div class="flex items-center space-x-2">
                <span class="text-gray-500 dark:text-gray-400">{{ t('tasks.start') }}:</span>
                <span class="font-medium text-gray-900 dark:text-white">
                  {{ formatDateTime(task.startDatetime) }}
                </span>
              </div>
              <div class="flex items-center space-x-2">
                <span class="text-gray-500 dark:text-gray-400">{{ t('tasks.end') }}:</span>
                <span class="font-medium text-gray-900 dark:text-white">
                  {{ formatDateTime(task.endDatetime) }}
                </span>
              </div>
            </div>
            <div class="mt-2 text-xs text-gray-500 dark:text-gray-400">
              {{ t('tasks.duration') }}: {{ getTaskDuration(task) }}
            </div>
          </div>
        </div>

        <!-- Recurrence Info -->
        <div v-if="task.recurrenceRule">
          <div class="flex items-center space-x-2 mb-2">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">{{ t('tasks.recurrence') }}</h4>
          </div>
          <div class="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-3 space-y-2">
            <div class="flex items-start space-x-2">
              <svg class="h-4 w-4 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
              <div class="flex-1">
                <p class="text-sm font-medium text-blue-900 dark:text-blue-100">
                  {{ getRecurrenceDescription() }}
                </p>
                <p v-if="task.recurrenceEnd" class="text-xs text-blue-700 dark:text-blue-300 mt-1">
                  {{ t('tasks.recurrenceUntil') }}: {{ formatDate(task.recurrenceEnd) }}
                </p>
                <p v-else class="text-xs text-blue-700 dark:text-blue-300 mt-1">
                  {{ t('tasks.recurrenceNoEnd') }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Location -->
        <div v-if="task.location">
          <div class="flex items-center space-x-3">
            <svg class="h-4 w-4 text-gray-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">{{ t('tasks.locationLabel') }}:</h4>
            <span class="text-sm text-gray-700 dark:text-gray-300">{{ task.location }}</span>
          </div>
        </div>

        <!-- Color -->
        <div>
          <div class="flex items-center space-x-3">
            <svg class="h-4 w-4 text-gray-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zM21 5a2 2 0 00-2-2h-4a2 2 0 00-2 2v12a4 4 0 004 4 4 4 0 004-4V5z" />
            </svg>
            <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">{{ t('tasks.colorLabel') }}:</h4>
            <div class="flex items-center space-x-2">
              <div
                class="w-5 h-5 rounded border border-gray-200 dark:border-gray-600 flex-shrink-0"
                :style="{ backgroundColor: task.color || '#3788d8' }"
              ></div>
              <span class="text-sm text-gray-700 dark:text-gray-300 font-mono">{{ task.color || '#3788d8' }}</span>
              <span class="text-xs text-gray-500 dark:text-gray-400">({{ getColorName(task.color) }})</span>
            </div>
          </div>
        </div>

        <!-- Task Status -->
        <div>
          <div class="flex items-center space-x-3">
            <svg class="h-4 w-4 text-gray-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">{{ t('tasks.statusLabel') }}:</h4>
            <div class="flex items-center space-x-3">
              <span
                class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium"
                :class="getTaskStatusClasses()"
              >
                {{ getTaskStatus() }}
              </span>
              <span class="text-xs text-gray-500 dark:text-gray-400">
                {{ getTaskTimeUntil() }}
              </span>
            </div>
          </div>
        </div>

        <!-- Reminders -->
        <div v-if="task.reminders && task.reminders.length > 0">
          <div class="flex items-center space-x-2 mb-3">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-5 5v-5zM11 19H6a2 2 0 01-2-2V7a2 2 0 012-2h6l5 5v11a2 2 0 01-2 2z" />
            </svg>
            <h4 class="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
              {{ t('tasks.remindersCount', task.reminders.length) }}
            </h4>
          </div>
          <div class="space-y-2">
            <div
              v-for="reminder in task.reminders"
              :key="reminder.id"
              class="bg-gray-50 dark:bg-gray-700 rounded-lg p-3 flex items-center justify-between"
            >
              <div class="flex items-center space-x-3">
                <!-- Reminder Type Icon -->
                <div class="flex-shrink-0">
                  <span class="text-lg">{{ getNotificationIcon(reminder.notificationType) }}</span>
                </div>
                <div>
                  <p class="text-sm font-medium text-gray-900 dark:text-white">
                    {{ getReminderDescription(reminder.reminderOffsetMinutes) }}
                  </p>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    {{ getNotificationTypeLabel(reminder.notificationType) }}
                  </p>
                </div>
              </div>
              <!-- Reminder Status -->
              <div class="flex-shrink-0">
                <span
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                  :class="reminder.isSent
                    ? 'bg-green-100 dark:bg-green-900/20 text-green-800 dark:text-green-200'
                    : 'bg-yellow-100 dark:bg-yellow-900/20 text-yellow-800 dark:text-yellow-200'"
                >
                  {{ reminder.isSent ? t('tasks.reminderSent') : t('tasks.reminderPending') }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Metadata -->
        <div class="pt-2 border-t border-gray-200 dark:border-gray-600">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs text-gray-500 dark:text-gray-400">
            <div>
              <span class="font-medium">{{ t('tasks.created') }}:</span>
              {{ formatDateTime(task.createdAt) }}
            </div>
            <div>
              <span class="font-medium">{{ t('tasks.updated') }}:</span>
              {{ formatDateTime(task.updatedAt) }}
            </div>
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex justify-between items-center mt-6 pt-4 border-t border-gray-200 dark:border-gray-600">
        <!-- Delete Button (Left) -->
        <button @click="handleDelete"
          class="px-3 py-1.5 text-xs font-medium text-red-600 dark:text-red-400 border border-red-300 dark:border-red-600 rounded-md hover:bg-red-50 dark:hover:bg-red-900/20 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 transition-colors">
          <svg class="h-3 w-3 mr-1.5 inline-block" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          {{ t('tasks.deleteTask') }}
        </button>

        <!-- Main Action Buttons (Right) -->
        <div class="flex space-x-3">
          <button @click="closeModal"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors">
            {{ t('tasks.closeButton') }}
          </button>
          <button @click="handleEdit"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors">
            <svg class="h-4 w-4 mr-2 inline-block" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            {{ t('tasks.editTask') }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Delete Confirmation Dialog -->
  <ConfirmDialog
    v-model="showDeleteConfirm"
    :title="t('tasks.deleteConfirmTitle')"
    :message="t('tasks.deleteConfirmMessage', { title: task?.title })"
    :details="t('tasks.deleteConfirmDetails')"
    variant="danger"
    :confirm-text="t('tasks.deleteTask')"
    :cancel-text="t('common.cancel')"
    @confirm="confirmDelete"
    @cancel="cancelDelete"
  />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Task, NotificationType } from '../types/task'
import { NOTIFICATION_TYPE_CONFIG, CALENDAR_COLORS } from '../types/task'
import { formatDate, formatDateTime, formatTime } from '../utils/dateHelpers'
import { getRecurrenceDescription as getRecurrenceText } from '../utils/recurrence'
import ConfirmDialog from './Common/ConfirmDialog.vue'
import RecurringEditChoiceModal from './RecurringEditChoiceModal.vue'
import { useTasks } from '../composables/useTasks'

// Composables
const { t } = useI18n()

interface Props {
  show: boolean
  task?: Task | null
}

interface Emits {
  (e: 'close'): void
  (e: 'edit', task: Task, editMode?: 'single' | 'all'): void
  (e: 'delete', taskId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Composables
const { deleteTask } = useTasks()

// State
const showDeleteConfirm = ref(false)
const showRecurringEditChoice = ref(false)

// Methods
const closeModal = () => {
  emit('close')
}

const handleBackdropClick = () => {
  closeModal()
}

const handleEdit = () => {
  if (props.task) {
    // Check if this is a recurring task
    if (props.task.recurrenceRule) {
      // Show choice modal for recurring tasks
      showRecurringEditChoice.value = true
    } else {
      // Direct edit for non-recurring tasks
      emit('edit', props.task)
    }
  }
}

const handleEditAllOccurrences = () => {
  showRecurringEditChoice.value = false
  if (props.task) {
    // Edit the master task (all occurrences)
    emit('edit', props.task, 'all')
  }
}

const handleEditSingleOccurrence = () => {
  showRecurringEditChoice.value = false
  if (props.task) {
    // Edit only this occurrence
    emit('edit', props.task, 'single')
  }
}

const handleDelete = () => {
  showDeleteConfirm.value = true
}

const confirmDelete = async () => {
  if (props.task) {
    showDeleteConfirm.value = false
    const success = await deleteTask(props.task.id)
    if (success) {
      emit('delete', props.task.id)
    }
  }
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
}

// Task formatting methods
const getTaskDuration = (task: Task) => {
  if (!task.startDatetime || !task.endDatetime) return 'N/A'

  const start = new Date(task.startDatetime)
  const end = new Date(task.endDatetime)
  const durationMs = end.getTime() - start.getTime()

  const minutes = Math.floor(durationMs / (1000 * 60))
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60

  if (hours === 0) {
    return `${minutes} ${t('tasks.time.minutes')}`
  } else if (remainingMinutes === 0) {
    return `${hours} ${hours === 1 ? t('tasks.time.hour') : t('tasks.time.hours')}`
  } else {
    return `${hours}h ${remainingMinutes}m`
  }
}

const getColorName = (color?: string) => {
  if (!color) return t('tasks.colorNames.default')

  const colorConfig = CALENDAR_COLORS.find(c => c.value === color)
  return colorConfig ? colorConfig.name : t('tasks.colorNames.custom')
}

const getReminderDescription = (offsetMinutes: number) => {
  if (offsetMinutes === 0) return t('tasks.time.atStart')
  if (offsetMinutes < 60) return t('tasks.time.minutesBefore', { minutes: offsetMinutes })

  const hours = Math.floor(offsetMinutes / 60)
  const remainingMinutes = offsetMinutes % 60

  if (hours < 24) {
    if (remainingMinutes === 0) {
      return t('tasks.time.hoursBefore', {
        hours,
        unit: hours === 1 ? t('tasks.time.hour') : t('tasks.time.hours')
      })
    } else {
      return `${hours}h ${remainingMinutes}m prima`
    }
  } else {
    const days = Math.floor(hours / 24)
    const remainingHours = hours % 24

    if (remainingHours === 0) {
      return t('tasks.time.daysBefore', {
        days,
        unit: days === 1 ? t('tasks.time.day') : t('tasks.time.days')
      })
    } else {
      return `${days}g ${remainingHours}h prima`
    }
  }
}

const getNotificationIcon = (type: NotificationType) => {
  return NOTIFICATION_TYPE_CONFIG[type]?.icon || '🔔'
}

const getNotificationTypeLabel = (type: NotificationType) => {
  return NOTIFICATION_TYPE_CONFIG[type]?.label || type
}

const getRecurrenceDescription = () => {
  if (!props.task?.recurrenceRule) return ''
  return getRecurrenceText(props.task.recurrenceRule, t)
}

const getTaskStatus = () => {
  if (!props.task) return t('tasks.status.unknown')

  const now = new Date()
  const start = new Date(props.task.startDatetime)
  const end = new Date(props.task.endDatetime)

  if (now < start) {
    return t('tasks.status.scheduled')
  } else if (now >= start && now <= end) {
    return t('tasks.status.inProgress')
  } else {
    return t('tasks.status.completed')
  }
}

const getTaskStatusClasses = () => {
  if (!props.task) return 'bg-gray-100 text-gray-800'

  const now = new Date()
  const start = new Date(props.task.startDatetime)
  const end = new Date(props.task.endDatetime)

  if (now < start) {
    return 'bg-blue-100 dark:bg-blue-900/20 text-blue-800 dark:text-blue-200'
  } else if (now >= start && now <= end) {
    return 'bg-green-100 dark:bg-green-900/20 text-green-800 dark:text-green-200'
  } else {
    return 'bg-gray-100 dark:bg-gray-900/20 text-gray-800 dark:text-gray-200'
  }
}

const getTaskTimeUntil = () => {
  if (!props.task) return ''

  const now = new Date()
  const start = new Date(props.task.startDatetime)
  const end = new Date(props.task.endDatetime)

  if (now < start) {
    const msUntilStart = start.getTime() - now.getTime()
    const minutesUntilStart = Math.floor(msUntilStart / (1000 * 60))

    if (minutesUntilStart < 60) {
      return t('tasks.time.startsIn', { time: `${minutesUntilStart} ${t('tasks.time.minutes')}` })
    } else if (minutesUntilStart < 24 * 60) {
      const hours = Math.floor(minutesUntilStart / 60)
      return t('tasks.time.startsIn', {
        time: `${hours} ${hours === 1 ? t('tasks.time.hour') : t('tasks.time.hours')}`
      })
    } else {
      const days = Math.floor(minutesUntilStart / (24 * 60))
      return t('tasks.time.startsIn', {
        time: `${days} ${days === 1 ? t('tasks.time.day') : t('tasks.time.days')}`
      })
    }
  } else if (now >= start && now <= end) {
    const msUntilEnd = end.getTime() - now.getTime()
    const minutesUntilEnd = Math.floor(msUntilEnd / (1000 * 60))

    if (minutesUntilEnd < 60) {
      return t('tasks.time.endsIn', { time: `${minutesUntilEnd} ${t('tasks.time.minutes')}` })
    } else {
      const hours = Math.floor(minutesUntilEnd / 60)
      return t('tasks.time.endsIn', {
        time: `${hours} ${hours === 1 ? t('tasks.time.hour') : t('tasks.time.hours')}`
      })
    }
  } else {
    const msAgo = now.getTime() - end.getTime()
    const minutesAgo = Math.floor(msAgo / (1000 * 60))

    if (minutesAgo < 60) {
      return t('tasks.time.endedAgo', { time: `${minutesAgo} ${t('tasks.time.minutes')}` })
    } else if (minutesAgo < 24 * 60) {
      const hours = Math.floor(minutesAgo / 60)
      return t('tasks.time.endedAgo', {
        time: `${hours} ${hours === 1 ? t('tasks.time.hour') : t('tasks.time.hours')}`
      })
    } else {
      const days = Math.floor(minutesAgo / (24 * 60))
      return t('tasks.time.endedAgo', {
        time: `${days} ${days === 1 ? t('tasks.time.day') : t('tasks.time.days')}`
      })
    }
  }
}
</script>

<style scoped>
.modal-overlay {
  @apply fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4;
}

.modal-content {
  @apply relative bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full overflow-y-auto m-4;
  @apply p-6;
  max-height: 90vh;
}

@media (max-width: 640px) {
  .modal-content {
    @apply p-4 max-w-full m-2;
  }
}
</style>