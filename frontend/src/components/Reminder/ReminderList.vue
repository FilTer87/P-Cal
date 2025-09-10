<template>
  <div class="reminder-list">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100">
        Promemoria
      </h3>
      <button
        @click="showReminderForm = true"
        class="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-200"
      >
        <PlusIcon class="h-4 w-4 mr-1" />
        Aggiungi Promemoria
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="flex items-center justify-center py-8">
      <LoadingSpinner size="medium" />
    </div>

    <!-- Empty State -->
    <div v-else-if="taskReminders.length === 0" class="text-center py-8">
      <div class="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
        <BellIcon class="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <p class="text-gray-500 dark:text-gray-400 mb-4">
          Nessun promemoria impostato per questa attività
        </p>
        <button
          @click="showReminderForm = true"
          class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-blue-600 bg-blue-50 hover:bg-blue-100 dark:bg-blue-900 dark:text-blue-300 dark:hover:bg-blue-800 transition-colors duration-200"
        >
          <PlusIcon class="h-4 w-4 mr-2" />
          Crea il primo promemoria
        </button>
      </div>
    </div>

    <!-- Reminder List -->
    <div v-else class="space-y-3">
      <div
        v-for="reminder in sortedReminders"
        :key="reminder.id"
        class="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4 hover:shadow-md transition-shadow duration-200"
      >
        <div class="flex items-center justify-between">
          <!-- Reminder Info -->
          <div class="flex-1">
            <div class="flex items-center space-x-3">
              <!-- Status Icon -->
              <div class="flex-shrink-0">
                <CheckCircleIcon
                  v-if="reminder.sent"
                  class="h-5 w-5 text-green-500"
                  title="Inviato"
                />
                <ClockIcon
                  v-else-if="isReminderUpcoming(reminder)"
                  class="h-5 w-5 text-blue-500"
                  title="In attesa"
                />
                <ExclamationCircleIcon
                  v-else-if="isReminderOverdue(reminder)"
                  class="h-5 w-5 text-red-500"
                  title="Scaduto"
                />
                <BellIcon
                  v-else
                  class="h-5 w-5 text-gray-400"
                  title="Programmato"
                />
              </div>

              <!-- Reminder Details -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center space-x-2">
                  <p class="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {{ formatReminderTimeShort(reminder) }}
                  </p>
                  <span
                    v-if="reminder.sent"
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
                  >
                    Inviato
                  </span>
                  <span
                    v-else-if="isReminderOverdue(reminder)"
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200"
                  >
                    Scaduto
                  </span>
                  <span
                    v-else
                    class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200"
                  >
                    {{ getTimeUntilReminder(reminder) }}
                  </span>
                </div>
                <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  {{ formatReminderTime(reminder) }}
                </p>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex items-center space-x-2">
            <!-- Snooze Button (only for overdue) -->
            <button
              v-if="!reminder.sent && isReminderOverdue(reminder)"
              @click="handleSnoozeReminder(reminder.id)"
              class="p-1 text-gray-400 hover:text-blue-500 transition-colors duration-200"
              title="Posticipa di 10 minuti"
            >
              <ClockIcon class="h-4 w-4" />
            </button>

            <!-- Edit Button -->
            <button
              v-if="!reminder.sent"
              @click="editReminder(reminder)"
              class="p-1 text-gray-400 hover:text-blue-500 transition-colors duration-200"
              title="Modifica"
            >
              <PencilIcon class="h-4 w-4" />
            </button>

            <!-- Delete Button -->
            <button
              @click="confirmDeleteReminder(reminder)"
              class="p-1 text-gray-400 hover:text-red-500 transition-colors duration-200"
              title="Elimina"
            >
              <TrashIcon class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Presets -->
    <div v-if="!showReminderForm" class="mt-6">
      <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100 mb-3">
        Promemoria rapidi
      </h4>
      <ReminderPresets
        :task-due-date="taskDueDate"
        :existing-reminders="taskReminders"
        @preset-selected="handlePresetSelected"
      />
    </div>

    <!-- Reminder Form Modal -->
    <Modal
      v-if="showReminderForm"
      @close="closeReminderForm"
      title="Aggiungi Promemoria"
    >
      <ReminderForm
        :task-id="taskId"
        :task-due-date="taskDueDate"
        :reminder="editingReminder"
        @saved="handleReminderSaved"
        @cancel="closeReminderForm"
      />
    </Modal>

    <!-- Delete Confirmation -->
    <ConfirmDialog
      v-if="reminderToDelete"
      title="Elimina Promemoria"
      message="Sei sicuro di voler eliminare questo promemoria? Questa azione non può essere annullata."
      confirm-text="Elimina"
      cancel-text="Annulla"
      variant="danger"
      @confirm="handleDeleteConfirmed"
      @cancel="reminderToDelete = null"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useReminders } from '../../composables/useReminders'
import { useNotifications } from '../../composables/useNotifications'
import type { Reminder } from '../../types/task'
import ReminderForm from './ReminderForm.vue'
import ReminderPresets from './ReminderPresets.vue'
import Modal from '../Common/Modal.vue'
import LoadingSpinner from '../Common/LoadingSpinner.vue'
import ConfirmDialog from '../Common/ConfirmDialog.vue'

// Icons
import {
  PlusIcon,
  BellIcon,
  ClockIcon,
  CheckCircleIcon,
  ExclamationCircleIcon,
  PencilIcon,
  TrashIcon
} from '@heroicons/vue/24/outline'

interface Props {
  taskId: number
  taskDueDate?: string
}

const props = defineProps<Props>()

const {
  isLoading,
  fetchTaskReminders,
  deleteReminder,
  snoozeReminder,
  isReminderOverdue,
  isReminderUpcoming,
  formatReminderTime,
  formatReminderTimeShort,
  getTimeUntilReminder,
  getRemindersByTask,
  createReminder
} = useReminders()

const { showSuccess } = useNotifications()

// Local state
const showReminderForm = ref(false)
const editingReminder = ref<Reminder | null>(null)
const reminderToDelete = ref<Reminder | null>(null)

// Computed properties
const taskReminders = computed(() => getRemindersByTask(props.taskId))

const sortedReminders = computed(() => {
  return [...taskReminders.value].sort((a, b) => {
    // Sent reminders go to bottom
    if (a.sent !== b.sent) {
      return a.sent ? 1 : -1
    }
    // Then sort by reminder date
    return new Date(a.reminderDateTime).getTime() - new Date(b.reminderDateTime).getTime()
  })
})

// Methods
const loadReminders = async () => {
  await fetchTaskReminders(props.taskId)
}

const editReminder = (reminder: Reminder) => {
  editingReminder.value = reminder
  showReminderForm.value = true
}

const closeReminderForm = () => {
  showReminderForm.value = false
  editingReminder.value = null
}

const handleReminderSaved = () => {
  closeReminderForm()
  loadReminders()
}

const confirmDeleteReminder = (reminder: Reminder) => {
  reminderToDelete.value = reminder
}

const handleDeleteConfirmed = async () => {
  if (reminderToDelete.value) {
    const success = await deleteReminder(reminderToDelete.value.id)
    if (success) {
      reminderToDelete.value = null
    }
  }
}

const handleSnoozeReminder = async (reminderId: number) => {
  const success = await snoozeReminder(reminderId, 10) // Snooze for 10 minutes
  if (success) {
    loadReminders()
  }
}

const handlePresetSelected = async (presetData: { offsetMinutes: number }) => {
  if (!props.taskDueDate) return

  const dueDate = new Date(props.taskDueDate)
  const reminderDateTime = new Date(dueDate.getTime() - presetData.offsetMinutes * 60000)

  const reminder = await createReminder(props.taskId, {
    reminderDateTime: reminderDateTime.toISOString()
  })

  if (reminder) {
    loadReminders()
  }
}

// Lifecycle
onMounted(() => {
  loadReminders()
})

// Watch for task changes
watch(() => props.taskId, () => {
  if (props.taskId) {
    loadReminders()
  }
}, { immediate: true })
</script>

<style scoped>
.reminder-list {
  @apply w-full;
}

/* Custom scrollbar for reminder list */
.reminder-list::-webkit-scrollbar {
  width: 6px;
}

.reminder-list::-webkit-scrollbar-track {
  @apply bg-gray-100 dark:bg-gray-800 rounded-full;
}

.reminder-list::-webkit-scrollbar-thumb {
  @apply bg-gray-300 dark:bg-gray-600 rounded-full;
}

.reminder-list::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400 dark:bg-gray-500;
}
</style>