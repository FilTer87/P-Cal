<template>
  <div class="task-stats">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <h2 :class="['text-lg font-semibold', textClass]">
        Statistiche Attività
      </h2>
      
      <!-- Refresh button -->
      <button
        @click="refreshStats"
        :disabled="isLoading"
        :class="[
          'p-2 rounded-lg border transition-colors',
          'border-gray-300 dark:border-gray-600',
          'hover:bg-gray-50 dark:hover:bg-gray-700',
          'focus:outline-none focus:ring-2 focus:ring-blue-500',
          isLoading ? 'opacity-50 cursor-not-allowed' : ''
        ]"
        title="Aggiorna statistiche"
        aria-label="Aggiorna statistiche"
      >
        <ArrowPathIcon 
          :class="[
            'w-4 h-4 text-gray-600 dark:text-gray-400',
            isLoading ? 'animate-spin' : ''
          ]" 
        />
      </button>
    </div>

    <!-- Loading state -->
    <div v-if="isLoading" class="animate-pulse space-y-4">
      <div class="h-32 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
      <div class="grid grid-cols-2 gap-4">
        <div class="h-24 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
        <div class="h-24 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
      </div>
    </div>

    <!-- Stats content -->
    <div v-else class="space-y-6">
      <!-- Overview Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Total Tasks -->
        <div :class="[
          'p-4 rounded-lg border',
          cardClass,
          'border-gray-200 dark:border-gray-700'
        ]">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
                Totale
              </p>
              <p class="text-2xl font-bold text-blue-600 dark:text-blue-400">
                {{ stats.total }}
              </p>
            </div>
            <div class="p-3 bg-blue-100 dark:bg-blue-900/20 rounded-full">
              <ListBulletIcon class="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
        </div>

        <!-- Completed Tasks -->
        <div :class="[
          'p-4 rounded-lg border',
          cardClass,
          'border-gray-200 dark:border-gray-700'
        ]">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
                Completate
              </p>
              <p class="text-2xl font-bold text-green-600 dark:text-green-400">
                {{ stats.completed }}
              </p>
            </div>
            <div class="p-3 bg-green-100 dark:bg-green-900/20 rounded-full">
              <CheckCircleIcon class="w-5 h-5 text-green-600 dark:text-green-400" />
            </div>
          </div>
        </div>

        <!-- Pending Tasks -->
        <div :class="[
          'p-4 rounded-lg border',
          cardClass,
          'border-gray-200 dark:border-gray-700'
        ]">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
                In attesa
              </p>
              <p class="text-2xl font-bold text-yellow-600 dark:text-yellow-400">
                {{ stats.pending }}
              </p>
            </div>
            <div class="p-3 bg-yellow-100 dark:bg-yellow-900/20 rounded-full">
              <ClockIcon class="w-5 h-5 text-yellow-600 dark:text-yellow-400" />
            </div>
          </div>
        </div>

        <!-- Overdue Tasks -->
        <div :class="[
          'p-4 rounded-lg border',
          cardClass,
          'border-gray-200 dark:border-gray-700'
        ]">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600 dark:text-gray-400">
                In ritardo
              </p>
              <p class="text-2xl font-bold text-red-600 dark:text-red-400">
                {{ stats.overdue }}
              </p>
            </div>
            <div class="p-3 bg-red-100 dark:bg-red-900/20 rounded-full">
              <ExclamationTriangleIcon class="w-5 h-5 text-red-600 dark:text-red-400" />
            </div>
          </div>
        </div>
      </div>

      <!-- Progress Section -->
      <div :class="[
        'p-6 rounded-lg border',
        cardClass,
        'border-gray-200 dark:border-gray-700'
      ]">
        <h3 :class="['text-lg font-medium mb-4', textClass]">
          Progresso Complessivo
        </h3>
        
        <!-- Progress Bar -->
        <div class="mb-4">
          <div class="flex justify-between text-sm mb-2">
            <span :class="textClass">Completamento</span>
            <span :class="textClass">{{ completionPercentage }}%</span>
          </div>
          <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
            <div 
              class="bg-gradient-to-r from-green-400 to-green-600 h-3 rounded-full transition-all duration-500 ease-out"
              :style="{ width: `${completionPercentage}%` }"
            ></div>
          </div>
        </div>

        <!-- Progress Details -->
        <div class="grid grid-cols-3 gap-4 text-center">
          <div>
            <div class="text-lg font-semibold text-green-600 dark:text-green-400">
              {{ stats.completed }}
            </div>
            <div class="text-xs text-gray-500 dark:text-gray-400">
              Completate
            </div>
          </div>
          <div>
            <div class="text-lg font-semibold text-yellow-600 dark:text-yellow-400">
              {{ stats.pending }}
            </div>
            <div class="text-xs text-gray-500 dark:text-gray-400">
              In attesa
            </div>
          </div>
          <div>
            <div class="text-lg font-semibold text-red-600 dark:text-red-400">
              {{ stats.overdue }}
            </div>
            <div class="text-xs text-gray-500 dark:text-gray-400">
              In ritardo
            </div>
          </div>
        </div>
      </div>

      <!-- Priority Distribution -->
      <div :class="[
        'p-6 rounded-lg border',
        cardClass,
        'border-gray-200 dark:border-gray-700'
      ]">
        <h3 :class="['text-lg font-medium mb-4', textClass]">
          Distribuzione per Priorità
        </h3>
        
        <div class="space-y-3">
          <div 
            v-for="priority in priorityDistribution"
            :key="priority.level"
            class="flex items-center justify-between"
          >
            <div class="flex items-center space-x-3">
              <div 
                class="w-3 h-3 rounded-full"
                :class="priority.colorClass"
              ></div>
              <span :class="['text-sm', textClass]">
                {{ priority.label }}
              </span>
            </div>
            <div class="flex items-center space-x-2">
              <span :class="['text-sm font-medium', textClass]">
                {{ priority.count }}
              </span>
              <div class="w-16 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                <div 
                  :class="['h-2 rounded-full transition-all duration-300', priority.colorClass]"
                  :style="{ width: `${priority.percentage}%` }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Timeline Stats -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Today's Tasks -->
        <div :class="[
          'p-6 rounded-lg border',
          cardClass,
          'border-gray-200 dark:border-gray-700'
        ]">
          <h3 :class="['text-lg font-medium mb-4', textClass]">
            Oggi
          </h3>
          
          <div class="space-y-2">
            <div class="flex justify-between text-sm">
              <span class="text-gray-600 dark:text-gray-400">Totali:</span>
              <span :class="['font-medium', textClass]">{{ stats.today }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-600 dark:text-gray-400">Completate:</span>
              <span class="font-medium text-green-600 dark:text-green-400">
                {{ todayCompleted }}
              </span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-600 dark:text-gray-400">Rimanenti:</span>
              <span class="font-medium text-yellow-600 dark:text-yellow-400">
                {{ stats.today - todayCompleted }}
              </span>
            </div>
          </div>
        </div>

        <!-- This Week -->
        <div :class="[
          'p-6 rounded-lg border',
          cardClass,
          'border-gray-200 dark:border-gray-700'
        ]">
          <h3 :class="['text-lg font-medium mb-4', textClass]">
            Questa Settimana
          </h3>
          
          <div class="space-y-2">
            <div class="flex justify-between text-sm">
              <span class="text-gray-600 dark:text-gray-400">Totali:</span>
              <span :class="['font-medium', textClass]">{{ stats.thisWeek }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-600 dark:text-gray-400">Completate:</span>
              <span class="font-medium text-green-600 dark:text-green-400">
                {{ weekCompleted }}
              </span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-600 dark:text-gray-400">Rimanenti:</span>
              <span class="font-medium text-yellow-600 dark:text-yellow-400">
                {{ stats.thisWeek - weekCompleted }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div v-if="showQuickActions" :class="[
        'p-4 rounded-lg border border-dashed border-gray-300 dark:border-gray-600'
      ]">
        <h4 :class="['text-sm font-medium mb-3 text-center', textClass]">
          Azioni Rapide
        </h4>
        
        <div class="flex justify-center space-x-2">
          <button
            @click="$emit('show-all-tasks')"
            :class="[
              'px-3 py-1.5 text-xs rounded-md border transition-colors',
              'text-gray-600 dark:text-gray-300 border-gray-300 dark:border-gray-600',
              'hover:bg-gray-50 dark:hover:bg-gray-700'
            ]"
          >
            Tutte le attività
          </button>
          
          <button
            v-if="stats.overdue > 0"
            @click="$emit('show-overdue-tasks')"
            :class="[
              'px-3 py-1.5 text-xs rounded-md border transition-colors',
              'text-red-600 dark:text-red-400 border-red-300 dark:border-red-600',
              'hover:bg-red-50 dark:hover:bg-red-900/20'
            ]"
          >
            Attività in ritardo
          </button>
          
          <button
            @click="$emit('create-task')"
            :class="[
              'px-3 py-1.5 text-xs rounded-md border transition-colors',
              'text-blue-600 dark:text-blue-400 border-blue-300 dark:border-blue-600',
              'hover:bg-blue-50 dark:hover:bg-blue-900/20'
            ]"
          >
            Nuova attività
          </button>
        </div>
      </div>

      <!-- Empty State -->
      <div 
        v-if="stats.total === 0" 
        class="text-center py-12"
      >
        <ListBulletIcon class="w-12 h-12 mx-auto text-gray-400 dark:text-gray-500 mb-4" />
        <h3 :class="['text-lg font-medium mb-2', textClass]">
          Nessuna attività
        </h3>
        <p class="text-gray-500 dark:text-gray-400 mb-4">
          Crea la tua prima attività per iniziare.
        </p>
        <button
          @click="$emit('create-task')"
          class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
        >
          Crea attività
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTasks } from '../../composables/useTasks'
import { useTheme } from '../../composables/useTheme'
import { TASK_PRIORITY_CONFIG, TaskPriority } from '../../types/task'

// Icons
import {
  ListBulletIcon,
  CheckCircleIcon,
  ClockIcon,
  ExclamationTriangleIcon,
  ArrowPathIcon
} from '@heroicons/vue/24/outline'

// Props
interface Props {
  showQuickActions?: boolean
  refreshInterval?: number
}

const props = withDefaults(defineProps<Props>(), {
  showQuickActions: true,
  refreshInterval: 0
})

// Emits
const emit = defineEmits<{
  'show-all-tasks': []
  'show-overdue-tasks': []
  'create-task': []
  'refresh': []
}>()

// Composables
const { 
  taskStats: stats, 
  isLoading, 
  refreshTasks,
  allTasks,
  completedTasks 
} = useTasks()
const { textClass, cardClass } = useTheme()

// Computed properties
const completionPercentage = computed(() => {
  if (stats.value.total === 0) return 0
  return Math.round((stats.value.completed / stats.value.total) * 100)
})

const todayCompleted = computed(() => {
  // This would need to be implemented in the tasks store
  // For now, we'll estimate based on overall completion rate
  if (stats.value.today === 0) return 0
  const rate = stats.value.completed / stats.value.total
  return Math.round(stats.value.today * rate)
})

const weekCompleted = computed(() => {
  // This would need to be implemented in the tasks store
  // For now, we'll estimate based on overall completion rate
  if (stats.value.thisWeek === 0) return 0
  const rate = stats.value.completed / stats.value.total
  return Math.round(stats.value.thisWeek * rate)
})

const priorityDistribution = computed(() => {
  const distribution = Object.values(TaskPriority).map(priority => {
    const tasksWithPriority = allTasks.value.filter(task => task.priority === priority)
    const count = tasksWithPriority.length
    const percentage = stats.value.total > 0 ? Math.round((count / stats.value.total) * 100) : 0
    
    const config = TASK_PRIORITY_CONFIG[priority]
    let colorClass = 'bg-gray-400'
    
    switch (priority) {
      case TaskPriority.LOW:
        colorClass = 'bg-green-500'
        break
      case TaskPriority.MEDIUM:
        colorClass = 'bg-yellow-500'
        break
      case TaskPriority.HIGH:
        colorClass = 'bg-orange-500'
        break
      case TaskPriority.URGENT:
        colorClass = 'bg-red-500'
        break
    }
    
    return {
      level: priority,
      label: config.label,
      count,
      percentage,
      colorClass
    }
  })
  
  // Sort by count descending
  return distribution.sort((a, b) => b.count - a.count)
})

// Methods
const refreshStats = async () => {
  await refreshTasks()
  emit('refresh')
}
</script>

<style scoped>
.task-stats {
  /* Custom styles if needed */
}

/* Animation for progress bars */
.task-stats .transition-all {
  transition-property: width, background-color;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
}

/* Gradient for progress bar */
.bg-gradient-to-r {
  background-image: linear-gradient(to right, var(--tw-gradient-stops));
}

/* Loading animation */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* Pulse animation for loading skeleton */
@keyframes pulse {
  50% {
    opacity: .5;
  }
}

.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

/* Hover effects */
.task-stats button:hover {
  transform: translateY(-1px);
}

.task-stats button:active {
  transform: translateY(0);
}
</style>