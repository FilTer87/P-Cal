<template>
  <div v-if="show && task" class="modal-overlay" @click="handleBackdropClick">
    <div class="modal-content" @click.stop>
      <!-- Modal Header -->
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center space-x-3">
          <!-- Task Color Indicator -->
          <div
            class="w-4 h-4 rounded-full flex-shrink-0"
            :style="{ backgroundColor: task.color || '#3788d8' }"
          ></div>
          <h3 class="text-lg font-medium text-gray-900 dark:text-white truncate">
            Dettagli Attività
          </h3>
        </div>
        <div class="flex items-center space-x-2">
          <!-- Edit Button -->
          <button @click="handleEdit"
            class="px-3 py-2 text-sm font-medium text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md transition-colors">
            <svg class="h-4 w-4 mr-1.5 inline-block" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            Modifica
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
      <div class="space-y-6">
        <!-- Title -->
        <div>
          <div class="flex items-center space-x-2 mb-1">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a2 2 0 012-2z" />
            </svg>
            <h4 class="text-sm font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">Titolo</h4>
          </div>
          <p class="text-lg font-semibold text-gray-900 dark:text-white">{{ task.title }}</p>
        </div>

        <!-- Description -->
        <div v-if="task.description">
          <div class="flex items-center space-x-2 mb-2">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <h4 class="text-sm font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">Descrizione</h4>
          </div>
          <p class="text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{{ task.description }}</p>
        </div>

        <!-- Date & Time -->
        <div>
          <div class="flex items-center space-x-2 mb-2">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h4 class="text-sm font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">Orario</h4>
          </div>
          <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-3">
            <div class="flex items-center space-x-4 text-sm">
              <div class="flex items-center space-x-2">
                <span class="text-gray-500 dark:text-gray-400">Inizio:</span>
                <span class="font-medium text-gray-900 dark:text-white">
                  {{ formatDateTime(task.startDatetime) }}
                </span>
              </div>
              <div class="flex items-center space-x-2">
                <span class="text-gray-500 dark:text-gray-400">Fine:</span>
                <span class="font-medium text-gray-900 dark:text-white">
                  {{ formatDateTime(task.endDatetime) }}
                </span>
              </div>
            </div>
            <div class="mt-2 text-xs text-gray-500 dark:text-gray-400">
              Durata: {{ getTaskDuration(task) }}
            </div>
          </div>
        </div>

        <!-- Location -->
        <div v-if="task.location">
          <div class="flex items-center space-x-2 mb-2">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <h4 class="text-sm font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">Luogo</h4>
          </div>
          <p class="text-gray-700 dark:text-gray-300">{{ task.location }}</p>
        </div>

        <!-- Color -->
        <div>
          <div class="flex items-center space-x-2 mb-2">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zM21 5a2 2 0 00-2-2h-4a2 2 0 00-2 2v12a4 4 0 004 4 4 4 0 004-4V5z" />
            </svg>
            <h4 class="text-sm font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">Colore</h4>
          </div>
          <div class="flex items-center space-x-3">
            <div
              class="w-8 h-8 rounded-lg border border-gray-200 dark:border-gray-600 flex-shrink-0"
              :style="{ backgroundColor: task.color || '#3788d8' }"
            ></div>
            <span class="text-sm text-gray-700 dark:text-gray-300 font-mono">{{ task.color || '#3788d8' }}</span>
            <span class="text-xs text-gray-500 dark:text-gray-400">({{ getColorName(task.color) }})</span>
          </div>
        </div>

        <!-- Reminders -->
        <div v-if="task.reminders && task.reminders.length > 0">
          <div class="flex items-center space-x-2 mb-3">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-5 5v-5zM11 19H6a2 2 0 01-2-2V7a2 2 0 012-2h6l5 5v11a2 2 0 01-2 2z" />
            </svg>
            <h4 class="text-sm font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
              Promemoria ({{ task.reminders.length }})
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
                  {{ reminder.isSent ? 'Inviato' : 'In attesa' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Task Status -->
        <div>
          <div class="flex items-center space-x-2 mb-2">
            <svg class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h4 class="text-sm font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">Stato</h4>
          </div>
          <div class="flex items-center space-x-4">
            <span
              class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium"
              :class="getTaskStatusClasses()"
            >
              {{ getTaskStatus() }}
            </span>
            <span class="text-sm text-gray-500 dark:text-gray-400">
              {{ getTaskTimeUntil() }}
            </span>
          </div>
        </div>

        <!-- Metadata -->
        <div class="pt-4 border-t border-gray-200 dark:border-gray-600">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-500 dark:text-gray-400">
            <div>
              <span class="font-medium">Creata:</span>
              {{ formatDateTime(task.createdAt) }}
            </div>
            <div>
              <span class="font-medium">Aggiornata:</span>
              {{ formatDateTime(task.updatedAt) }}
            </div>
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex justify-end space-x-3 mt-8 pt-6 border-t border-gray-200 dark:border-gray-600">
        <button @click="closeModal"
          class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors">
          Chiudi
        </button>
        <button @click="handleEdit"
          class="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors">
          <svg class="h-4 w-4 mr-2 inline-block" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
          Modifica Attività
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Task, NotificationType } from '../types/task'
import { NOTIFICATION_TYPE_CONFIG, CALENDAR_COLORS } from '../types/task'
import { formatDate, formatDateTime, formatTime } from '../utils/dateHelpers'

interface Props {
  show: boolean
  task?: Task | null
}

interface Emits {
  (e: 'close'): void
  (e: 'edit', task: Task): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Methods
const closeModal = () => {
  emit('close')
}

const handleBackdropClick = () => {
  closeModal()
}

const handleEdit = () => {
  if (props.task) {
    emit('edit', props.task)
  }
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
    return `${minutes} minuti`
  } else if (remainingMinutes === 0) {
    return `${hours} ${hours === 1 ? 'ora' : 'ore'}`
  } else {
    return `${hours}h ${remainingMinutes}m`
  }
}

const getColorName = (color?: string) => {
  if (!color) return 'Blu (default)'

  const colorConfig = CALENDAR_COLORS.find(c => c.value === color)
  return colorConfig ? colorConfig.name : 'Personalizzato'
}

const getReminderDescription = (offsetMinutes: number) => {
  if (offsetMinutes === 0) return 'Al momento dell\'inizio'
  if (offsetMinutes < 60) return `${offsetMinutes} minuti prima`

  const hours = Math.floor(offsetMinutes / 60)
  const remainingMinutes = offsetMinutes % 60

  if (hours < 24) {
    if (remainingMinutes === 0) {
      return `${hours} ${hours === 1 ? 'ora' : 'ore'} prima`
    } else {
      return `${hours}h ${remainingMinutes}m prima`
    }
  } else {
    const days = Math.floor(hours / 24)
    const remainingHours = hours % 24

    if (remainingHours === 0) {
      return `${days} ${days === 1 ? 'giorno' : 'giorni'} prima`
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

const getTaskStatus = () => {
  if (!props.task) return 'Sconosciuto'

  const now = new Date()
  const start = new Date(props.task.startDatetime)
  const end = new Date(props.task.endDatetime)

  if (now < start) {
    return 'Programmata'
  } else if (now >= start && now <= end) {
    return 'In corso'
  } else {
    return 'Completata'
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
      return `Inizia tra ${minutesUntilStart} minuti`
    } else if (minutesUntilStart < 24 * 60) {
      const hours = Math.floor(minutesUntilStart / 60)
      return `Inizia tra ${hours} ${hours === 1 ? 'ora' : 'ore'}`
    } else {
      const days = Math.floor(minutesUntilStart / (24 * 60))
      return `Inizia tra ${days} ${days === 1 ? 'giorno' : 'giorni'}`
    }
  } else if (now >= start && now <= end) {
    const msUntilEnd = end.getTime() - now.getTime()
    const minutesUntilEnd = Math.floor(msUntilEnd / (1000 * 60))

    if (minutesUntilEnd < 60) {
      return `Termina tra ${minutesUntilEnd} minuti`
    } else {
      const hours = Math.floor(minutesUntilEnd / 60)
      return `Termina tra ${hours} ${hours === 1 ? 'ora' : 'ore'}`
    }
  } else {
    const msAgo = now.getTime() - end.getTime()
    const minutesAgo = Math.floor(msAgo / (1000 * 60))

    if (minutesAgo < 60) {
      return `Terminata ${minutesAgo} minuti fa`
    } else if (minutesAgo < 24 * 60) {
      const hours = Math.floor(minutesAgo / 60)
      return `Terminata ${hours} ${hours === 1 ? 'ora' : 'ore'} fa`
    } else {
      const days = Math.floor(minutesAgo / (24 * 60))
      return `Terminata ${days} ${days === 1 ? 'giorno' : 'giorni'} fa`
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