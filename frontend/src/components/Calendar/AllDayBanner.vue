<template>
  <div class="all-day-banner-container">
    <!-- CASE 1: No all-day tasks (completely empty, matching grid background) -->
    <div v-if="tasks.length === 0" class="h-8 bg-white dark:bg-gray-800"></div>

    <!-- CASE 2: Single task (expanded) -->
    <div v-else-if="tasks.length === 1"
         class="h-8 px-1 py-1 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors rounded"
         @click="handleTaskClick(tasks[0])">
      <div class="flex items-center gap-1 h-full">
        <!-- Colored side bar -->
        <div class="w-1 h-full rounded-full flex-shrink-0"
             :style="{ backgroundColor: tasks[0].color }"></div>

        <!-- Task title -->
        <div class="truncate text-xs font-medium flex-1"
             :style="{ color: tasks[0].color }"
             :title="tasks[0].title">
          {{ tasks[0].title }}
        </div>

        <!-- Calendar icon -->
        <svg class="w-3 h-3 flex-shrink-0 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      </div>
    </div>

    <!-- CASE 3: Multiple tasks (compact expandable badge) -->
    <div v-else class="relative" ref="bannerRef">
      <!-- Compact badge (collapsed) -->
      <div v-if="!isExpanded"
           class="h-8 px-2 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors rounded"
           @click="toggleExpanded">
        <div class="flex items-center justify-between h-full">
          <!-- Color indicator dots (max 4 visible) -->
          <div class="flex gap-0.5">
            <div v-for="(task, index) in tasks.slice(0, 4)"
                 :key="task.id"
                 class="w-2 h-2 rounded-full"
                 :style="{ backgroundColor: task.color }"
                 :title="task.title"></div>
            <div v-if="tasks.length > 4"
                 class="w-2 h-2 rounded-full bg-gray-300 dark:bg-gray-600"
                 title="More events..."></div>
          </div>

          <!-- Counter + icons -->
          <div class="flex items-center gap-1">
            <span class="text-xs font-semibold text-gray-600 dark:text-gray-300">
              {{ tasks.length }}
            </span>
            <svg class="w-3 h-3 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <svg class="w-3 h-3 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Expanded list -->
      <div v-else
           class="absolute top-0 left-0 right-0 bg-white dark:bg-gray-800 shadow-lg border border-gray-200 dark:border-gray-600 rounded-b z-50 max-h-48 overflow-y-auto all-day-expanded-list">
        <!-- Expandable header -->
        <div class="sticky top-0 bg-gray-50 dark:bg-gray-700 px-2 py-1 flex items-center justify-between border-b border-gray-200 dark:border-gray-600 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-600"
             @click="toggleExpanded">
          <span class="text-xs font-semibold text-gray-700 dark:text-gray-200">{{ tasks.length }} all-day events</span>
          <svg class="w-3 h-3 text-gray-500 dark:text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7" />
          </svg>
        </div>

        <!-- Task list -->
        <div v-for="task in tasks"
             :key="task.id"
             class="px-2 py-1.5 hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer border-b border-gray-100 dark:border-gray-700 last:border-b-0 transition-colors"
             @click="handleTaskClickExpanded(task)">
          <div class="flex items-center gap-2">
            <!-- Colored bar -->
            <div class="w-1 h-6 rounded-full flex-shrink-0"
                 :style="{ backgroundColor: task.color }"></div>

            <!-- Task info -->
            <div class="flex-1 min-w-0">
              <div class="truncate text-xs font-medium"
                   :style="{ color: task.color }"
                   :title="task.title">
                {{ task.title }}
              </div>
              <div v-if="task.location"
                   class="truncate text-xs text-gray-500 dark:text-gray-400 mt-0.5"
                   :title="task.location">
                📍 {{ task.location }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { Task } from '../../types/task'

interface Props {
  tasks: Task[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'task-click': [task: Task]
}>()

const isExpanded = ref(false)
const bannerRef = ref<HTMLElement | null>(null)

const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

const handleTaskClick = (task: Task) => {
  emit('task-click', task)
}

const handleTaskClickExpanded = (task: Task) => {
  emit('task-click', task)
  isExpanded.value = false // Chiudi lista dopo click
}

// Click outside per chiudere espansione
const handleClickOutside = (event: MouseEvent) => {
  if (isExpanded.value && bannerRef.value && !bannerRef.value.contains(event.target as Node)) {
    isExpanded.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.all-day-banner-container {
  /* Seamless integration con griglia */
  background: inherit;
}

/* Animazione espansione smooth */
.all-day-expanded-list {
  animation: slideDown 150ms ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Scrollbar styling per lista espansa */
.all-day-expanded-list::-webkit-scrollbar {
  width: 6px;
}

.all-day-expanded-list::-webkit-scrollbar-track {
  background: transparent;
}

.all-day-expanded-list::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.5);
  border-radius: 3px;
}

.all-day-expanded-list::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.7);
}
</style>
