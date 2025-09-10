<template>
  <Modal
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="Gestione Task"
    size="lg"
  >
    <div class="space-y-6">
      <!-- Task Form Section -->
      <div class="bg-gray-50 dark:bg-gray-900 rounded-lg p-4">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          Dettagli Task
        </h3>
        
        <!-- Basic task form fields -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Titolo *
            </label>
            <input
              v-model="taskForm.title"
              type="text"
              required
              class="block w-full rounded-md border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              placeholder="Inserisci il titolo del task"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Data di Scadenza
            </label>
            <input
              v-model="taskForm.dueDate"
              type="datetime-local"
              class="block w-full rounded-md border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            />
          </div>
        </div>

        <div class="mt-4">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Descrizione
          </label>
          <textarea
            v-model="taskForm.description"
            rows="3"
            class="block w-full rounded-md border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
            placeholder="Descrizione opzionale del task"
          />
        </div>
      </div>

      <!-- Reminders Section -->
      <div class="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          Promemoria
        </h3>
        
        <!-- Show reminders only if task has due date -->
        <div v-if="taskForm.dueDate">
          <ReminderList
            v-if="taskId"
            :task-id="taskId"
            :task-due-date="taskForm.dueDate"
          />
          
          <!-- Preview for new tasks -->
          <div v-else class="text-center py-6 text-gray-500 dark:text-gray-400">
            <BellIcon class="h-8 w-8 mx-auto mb-2 opacity-50" />
            <p class="text-sm">
              Salva il task per aggiungere promemoria
            </p>
          </div>
        </div>
        
        <!-- No due date message -->
        <div v-else class="text-center py-6 text-gray-500 dark:text-gray-400">
          <ExclamationCircleIcon class="h-8 w-8 mx-auto mb-2 opacity-50" />
          <p class="text-sm">
            Imposta una data di scadenza per aggiungere promemoria
          </p>
        </div>
      </div>

      <!-- Notification Settings Link -->
      <div class="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4">
        <div class="flex items-start space-x-3">
          <CogIcon class="h-5 w-5 text-yellow-600 dark:text-yellow-400 mt-0.5 flex-shrink-0" />
          <div class="flex-1">
            <h4 class="text-sm font-medium text-yellow-800 dark:text-yellow-200">
              Configura le Notifiche
            </h4>
            <p class="text-sm text-yellow-700 dark:text-yellow-300 mt-1">
              Per ricevere i promemoria, configura le impostazioni delle notifiche.
            </p>
            <button
              @click="showNotificationSettings = true"
              class="mt-2 text-sm font-medium text-yellow-600 dark:text-yellow-400 hover:text-yellow-500 dark:hover:text-yellow-300 underline"
            >
              Apri Impostazioni Notifiche
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer Actions -->
    <template #footer>
      <button
        @click="$emit('update:modelValue', false)"
        class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-200"
      >
        Annulla
      </button>
      
      <button
        @click="saveTask"
        :disabled="!taskForm.title.trim() || isSaving"
        class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
      >
        <LoadingSpinner v-if="isSaving" size="small" class="mr-2" />
        {{ taskId ? 'Aggiorna Task' : 'Crea Task' }}
      </button>
    </template>

    <!-- Notification Settings Modal -->
    <Modal
      v-if="showNotificationSettings"
      :model-value="showNotificationSettings"
      @update:model-value="showNotificationSettings = false"
      title="Impostazioni Notifiche"
      size="xl"
    >
      <NotificationSettings />
    </Modal>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useTasks } from '../../composables/useTasks'
import { useNotifications } from '../../composables/useNotifications'
import type { Task } from '../../types/task'

// Components
import { Modal, LoadingSpinner } from '../Common'
import { ReminderList, NotificationSettings } from '../Reminder'
import {
  BellIcon,
  ExclamationCircleIcon,
  CogIcon
} from '@heroicons/vue/24/outline'

interface Props {
  modelValue: boolean
  task?: Task | null
}

interface Emits {
  (event: 'update:modelValue', value: boolean): void
  (event: 'saved', task: Task): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const { createTask, updateTask } = useTasks()
const { showSuccess, showError } = useNotifications()

// Local state
const taskForm = ref({
  title: '',
  description: '',
  dueDate: '',
  priority: 'MEDIUM' as any,
  location: '',
  color: '#3b82f6'
})

const isSaving = ref(false)
const showNotificationSettings = ref(false)

// Computed properties
const taskId = computed(() => props.task?.id)
const isEditing = computed(() => !!props.task)

// Methods
const initializeForm = () => {
  if (props.task) {
    taskForm.value = {
      title: props.task.title,
      description: props.task.description || '',
      dueDate: props.task.dueDate || '',
      priority: props.task.priority,
      location: props.task.location || '',
      color: props.task.color || '#3b82f6'
    }
  } else {
    // Reset form for new task
    taskForm.value = {
      title: '',
      description: '',
      dueDate: '',
      priority: 'MEDIUM' as any,
      location: '',
      color: '#3b82f6'
    }
  }
}

const saveTask = async () => {
  if (!taskForm.value.title.trim()) {
    showError('Il titolo del task è obbligatorio')
    return
  }

  isSaving.value = true

  try {
    const taskData = {
      title: taskForm.value.title.trim(),
      description: taskForm.value.description.trim() || undefined,
      dueDate: taskForm.value.dueDate || undefined,
      priority: taskForm.value.priority,
      location: taskForm.value.location.trim() || undefined,
      color: taskForm.value.color
    }

    let savedTask: Task
    
    if (isEditing.value && taskId.value) {
      savedTask = await updateTask(taskId.value, taskData)
      showSuccess('Task aggiornato con successo!')
    } else {
      savedTask = await createTask(taskData)
      showSuccess('Task creato con successo!')
    }

    emit('saved', savedTask)
    emit('update:modelValue', false)
  } catch (error) {
    console.error('Error saving task:', error)
    showError('Errore nel salvataggio del task')
  } finally {
    isSaving.value = false
  }
}

// Watchers
watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    initializeForm()
  }
})

watch(() => props.task, () => {
  initializeForm()
})

// Lifecycle
onMounted(() => {
  initializeForm()
})
</script>

<style scoped>
/* Custom styles for task modal */
.modal-content {
  max-height: 90vh;
  overflow-y: auto;
}

/* Responsive grid */
@media (max-width: 768px) {
  .grid-cols-1.md\:grid-cols-2 {
    grid-template-columns: 1fr;
  }
}

/* Focus states */
input:focus,
textarea:focus,
select:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* Button hover effects */
button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.dark button:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
</style>