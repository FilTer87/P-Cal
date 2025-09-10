<template>
  <div class="task-management-example min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- Header -->
    <div class="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center py-4">
          <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
            Gestione Attività
          </h1>
          
          <div class="flex space-x-3">
            <!-- View Toggle -->
            <div class="flex bg-gray-100 dark:bg-gray-700 rounded-lg p-1">
              <button
                @click="currentView = 'stats'"
                :class="[
                  'px-3 py-1 text-sm font-medium rounded-md transition-colors',
                  currentView === 'stats'
                    ? 'bg-white dark:bg-gray-600 text-gray-900 dark:text-white shadow-sm'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                ]"
              >
                Statistiche
              </button>
              <button
                @click="currentView = 'list'"
                :class="[
                  'px-3 py-1 text-sm font-medium rounded-md transition-colors',
                  currentView === 'list'
                    ? 'bg-white dark:bg-gray-600 text-gray-900 dark:text-white shadow-sm'
                    : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                ]"
              >
                Lista
              </button>
            </div>
            
            <!-- Quick Add Toggle -->
            <button
              @click="showQuickAdd = !showQuickAdd"
              class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
            >
              Nuova Attività
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <!-- Quick Add Section -->
      <div v-if="showQuickAdd" class="mb-6">
        <TaskQuickAdd
          :auto-expand="true"
          @task-created="handleTaskCreated"
          @expand-full-form="handleExpandFullForm"
          @collapse="showQuickAdd = false"
        />
      </div>

      <!-- Stats View -->
      <div v-if="currentView === 'stats'" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Statistics Panel -->
        <div class="lg:col-span-2">
          <TaskStats
            :show-quick-actions="true"
            @show-all-tasks="currentView = 'list'"
            @show-overdue-tasks="showOverdueOnly"
            @create-task="showTaskModal = true"
          />
        </div>
        
        <!-- Recent Tasks -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
          <div class="p-4 border-b border-gray-200 dark:border-gray-700">
            <h3 class="text-lg font-medium text-gray-900 dark:text-white">
              Attività Recenti
            </h3>
          </div>
          <div class="p-4">
            <TaskList
              :enable-selection="false"
              :compact-mode="true"
              :group-by-date="false"
              :max-height="'400px'"
              @task-click="handleTaskClick"
              @task-edit="handleTaskEdit"
              @task-delete="handleTaskDelete"
            />
          </div>
        </div>
      </div>

      <!-- List View -->
      <div v-if="currentView === 'list'" class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
        <TaskList
          :enable-selection="true"
          :compact-mode="false"
          :group-by-date="true"
          :show-completed="showCompleted"
          @task-click="handleTaskClick"
          @task-edit="handleTaskEdit"
          @task-delete="handleTaskDelete"
          @selection-change="handleSelectionChange"
        />
      </div>
    </div>

    <!-- Task Modal -->
    <TaskModal
      v-model:is-open="showTaskModal"
      :task="selectedTask"
      :initial-date="initialTaskDate"
      :initial-time="initialTaskTime"
      :show-delete-button="!!selectedTask"
      @task-created="handleTaskCreated"
      @task-updated="handleTaskUpdated"
      @task-deleted="handleTaskDeleted"
      @close="handleModalClose"
    />

    <!-- Floating Action Button (Mobile) -->
    <div class="fixed bottom-6 right-6 sm:hidden">
      <button
        @click="showTaskModal = true"
        class="w-14 h-14 bg-blue-600 text-white rounded-full shadow-lg hover:bg-blue-700 transition-colors flex items-center justify-center"
        aria-label="Crea nuova attività"
      >
        <PlusIcon class="w-6 h-6" />
      </button>
    </div>

    <!-- Toast Notifications -->
    <div id="toast-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useTasks } from '../../composables/useTasks'
import { useNotifications } from '../../composables/useNotifications'
import { format } from 'date-fns'
import type { Task, CreateTaskRequest } from '../../types/task'

// Components
import TaskModal from './TaskModal.vue'
import TaskList from './TaskList.vue'
import TaskQuickAdd from './TaskQuickAdd.vue'
import TaskStats from './TaskStats.vue'

// Icons
import { PlusIcon } from '@heroicons/vue/24/outline'

// State
const currentView = ref<'stats' | 'list'>('stats')
const showTaskModal = ref(false)
const showQuickAdd = ref(false)
const selectedTask = ref<Task | null>(null)
const selectedTaskIds = ref<number[]>([])
const showCompleted = ref(true)
const initialTaskDate = ref('')
const initialTaskTime = ref('')

// Composables
const { fetchTasks, refreshTasks } = useTasks()
const { showSuccess, showError } = useNotifications()

// Methods
const handleTaskCreated = (task: Task) => {
  console.log('Task created:', task)
  showSuccess(`Attività "${task.title}" creata con successo!`)
  refreshTasks()
  showQuickAdd.value = false
}

const handleTaskUpdated = (task: Task) => {
  console.log('Task updated:', task)
  showSuccess(`Attività "${task.title}" aggiornata con successo!`)
  refreshTasks()
  selectedTask.value = null
}

const handleTaskDeleted = (taskId: number) => {
  console.log('Task deleted:', taskId)
  showSuccess('Attività eliminata con successo!')
  refreshTasks()
  selectedTask.value = null
}

const handleTaskClick = (task: Task) => {
  selectedTask.value = task
  showTaskModal.value = true
}

const handleTaskEdit = (task: Task) => {
  selectedTask.value = task
  showTaskModal.value = true
}

const handleTaskDelete = async (task: Task) => {
  // This would typically show a confirmation dialog
  console.log('Delete task:', task)
}

const handleExpandFullForm = (initialData: Partial<CreateTaskRequest>) => {
  selectedTask.value = null
  if (initialData.dueDate) {
    const date = new Date(initialData.dueDate)
    initialTaskDate.value = format(date, 'yyyy-MM-dd')
    initialTaskTime.value = format(date, 'HH:mm')
  }
  showTaskModal.value = true
}

const handleModalClose = () => {
  selectedTask.value = null
  initialTaskDate.value = ''
  initialTaskTime.value = ''
}

const handleSelectionChange = (selectedIds: number[]) => {
  selectedTaskIds.value = selectedIds
  console.log('Selection changed:', selectedIds)
}

const showOverdueOnly = () => {
  currentView.value = 'list'
  // This would apply a filter to show only overdue tasks
  // Implementation would depend on the TaskList component's filtering system
}

// Lifecycle
onMounted(async () => {
  await fetchTasks()
})
</script>

<style scoped>
/* Component-specific styles */
.task-management-example {
  /* Any custom styles */
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .task-management-example .grid-cols-1.lg\\:grid-cols-3 {
    grid-template-columns: 1fr;
  }
}

/* Dark mode adjustments */
.dark .task-management-example {
  /* Dark mode specific styles if needed */
}
</style>