<template>
  <div class="task-list h-full flex flex-col">
    <!-- Header with Search and Filters -->
    <div :class="[
      'flex-shrink-0 p-4 border-b',
      cardClass,
      'border-gray-200 dark:border-gray-700'
    ]">
      <div class="flex flex-col sm:flex-row gap-4">
        <!-- Search Input -->
        <div class="flex-1">
          <div class="relative">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Cerca attività..."
              :class="[
                'w-full pl-10 pr-4 py-2 text-sm border rounded-lg',
                'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent',
                inputClass
              ]"
              @input="handleSearch"
            />
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
            
            <!-- Clear search -->
            <button
              v-if="searchQuery"
              @click="clearSearch"
              class="absolute right-3 top-1/2 transform -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              title="Cancella ricerca"
            >
              <XMarkIcon class="w-3 h-3" />
            </button>
          </div>
        </div>

        <!-- Filters and Sort -->
        <div class="flex gap-2">
          <!-- Filter Menu -->
          <Menu as="div" class="relative">
            <MenuButton
              :class="[
                'inline-flex items-center px-3 py-2 text-sm border rounded-lg transition-colors',
                'text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600',
                'hover:bg-gray-50 dark:hover:bg-gray-700',
                'focus:outline-none focus:ring-2 focus:ring-blue-500',
                activeFiltersCount > 0 ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-500' : ''
              ]"
            >
              <FunnelIcon class="w-4 h-4 mr-1" />
              Filtri
              <span 
                v-if="activeFiltersCount > 0"
                class="ml-1 px-1.5 py-0.5 text-xs bg-blue-500 text-white rounded-full"
              >
                {{ activeFiltersCount }}
              </span>
              <ChevronDownIcon class="w-3 h-3 ml-1" />
            </MenuButton>

            <transition
              enter-active-class="transition duration-200 ease-out"
              enter-from-class="translate-y-1 opacity-0"
              enter-to-class="translate-y-0 opacity-100"
              leave-active-class="transition duration-150 ease-in"
              leave-from-class="translate-y-0 opacity-100"
              leave-to-class="translate-y-1 opacity-0"
            >
              <MenuItems 
                :class="[
                  'absolute right-0 z-10 mt-2 w-64 rounded-lg shadow-lg border',
                  cardClass,
                  'border-gray-200 dark:border-gray-700'
                ]"
              >
                <div class="p-3 space-y-3">
                  <!-- Status Filter -->
                  <div>
                    <label :class="['text-xs font-medium text-gray-600 dark:text-gray-400 block mb-2']">
                      Stato
                    </label>
                    <div class="space-y-1">
                      <label class="flex items-center text-sm">
                        <input
                          v-model="filters.showCompleted"
                          type="checkbox"
                          class="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                          @change="applyFilters"
                        />
                        <span :class="['ml-2', textClass]">Completate</span>
                      </label>
                      <label class="flex items-center text-sm">
                        <input
                          v-model="filters.showPending"
                          type="checkbox"
                          class="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                          @change="applyFilters"
                        />
                        <span :class="['ml-2', textClass]">In attesa</span>
                      </label>
                      <label class="flex items-center text-sm">
                        <input
                          v-model="filters.showOverdue"
                          type="checkbox"
                          class="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                          @change="applyFilters"
                        />
                        <span :class="['ml-2', textClass]">In ritardo</span>
                      </label>
                    </div>
                  </div>

                  <!-- Priority Filter -->
                  <div>
                    <label :class="['text-xs font-medium text-gray-600 dark:text-gray-400 block mb-2']">
                      Priorità
                    </label>
                    <select
                      v-model="filters.priority"
                      :class="[
                        'w-full px-2 py-1 text-sm border rounded focus:outline-none focus:ring-1 focus:ring-blue-500',
                        inputClass
                      ]"
                      @change="applyFilters"
                    >
                      <option value="">Tutte le priorità</option>
                      <option 
                        v-for="(config, priority) in TASK_PRIORITY_CONFIG"
                        :key="priority"
                        :value="priority"
                      >
                        {{ config.label }}
                      </option>
                    </select>
                  </div>

                  <!-- Date Range Filter -->
                  <div>
                    <label :class="['text-xs font-medium text-gray-600 dark:text-gray-400 block mb-2']">
                      Periodo
                    </label>
                    <div class="grid grid-cols-2 gap-2">
                      <input
                        v-model="filters.dateFrom"
                        type="date"
                        :class="[
                          'px-2 py-1 text-xs border rounded focus:outline-none focus:ring-1 focus:ring-blue-500',
                          inputClass
                        ]"
                        @change="applyFilters"
                      />
                      <input
                        v-model="filters.dateTo"
                        type="date"
                        :class="[
                          'px-2 py-1 text-xs border rounded focus:outline-none focus:ring-1 focus:ring-blue-500',
                          inputClass
                        ]"
                        @change="applyFilters"
                      />
                    </div>
                  </div>

                  <!-- Clear Filters -->
                  <button
                    @click="clearFilters"
                    :disabled="activeFiltersCount === 0"
                    :class="[
                      'w-full px-2 py-1.5 text-xs text-gray-600 dark:text-gray-300 border rounded transition-colors',
                      'hover:bg-gray-50 dark:hover:bg-gray-700',
                      'disabled:opacity-50 disabled:cursor-not-allowed'
                    ]"
                  >
                    Cancella filtri
                  </button>
                </div>
              </MenuItems>
            </transition>
          </Menu>

          <!-- Sort Menu -->
          <Menu as="div" class="relative">
            <MenuButton
              :class="[
                'inline-flex items-center px-3 py-2 text-sm border rounded-lg transition-colors',
                'text-gray-700 dark:text-gray-300 border-gray-300 dark:border-gray-600',
                'hover:bg-gray-50 dark:hover:bg-gray-700',
                'focus:outline-none focus:ring-2 focus:ring-blue-500'
              ]"
            >
              <Bars3BottomLeftIcon class="w-4 h-4 mr-1" />
              {{ currentSortLabel }}
              <ChevronDownIcon class="w-3 h-3 ml-1" />
            </MenuButton>

            <transition
              enter-active-class="transition duration-200 ease-out"
              enter-from-class="translate-y-1 opacity-0"
              enter-to-class="translate-y-0 opacity-100"
              leave-active-class="transition duration-150 ease-in"
              leave-from-class="translate-y-0 opacity-100"
              leave-to-class="translate-y-1 opacity-0"
            >
              <MenuItems 
                :class="[
                  'absolute right-0 z-10 mt-2 w-48 rounded-lg shadow-lg border',
                  cardClass,
                  'border-gray-200 dark:border-gray-700'
                ]"
              >
                <div class="p-1">
                  <MenuItem
                    v-for="option in TASK_SORT_OPTIONS"
                    :key="option.key"
                    v-slot="{ active, close }"
                  >
                    <button
                      @click="setSortOption(option.key, close)"
                      :class="[
                        'w-full px-2 py-2 text-left text-sm rounded transition-colors',
                        active ? 'bg-blue-100 dark:bg-blue-900/20' : '',
                        currentSort === option.key ? 'text-blue-600 dark:text-blue-400 font-medium' : textClass
                      ]"
                    >
                      {{ option.label }}
                      <span v-if="currentSort === option.key" class="float-right">
                        {{ sortDirection === 'asc' ? '↑' : '↓' }}
                      </span>
                    </button>
                  </MenuItem>
                </div>
              </MenuItems>
            </transition>
          </Menu>
        </div>
      </div>
    </div>

    <!-- Task Count and Selection -->
    <div 
      v-if="filteredTasks.length > 0 || selectedTasks.size > 0"
      :class="[
        'flex-shrink-0 px-4 py-2 text-sm border-b',
        cardClass,
        'border-gray-200 dark:border-gray-700',
        textClass
      ]"
    >
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-4">
          <span>
            {{ filteredTasks.length }} di {{ allTasks.length }} attività
          </span>
          
          <!-- Bulk Selection -->
          <div v-if="selectedTasks.size > 0" class="flex items-center space-x-2">
            <span class="text-blue-600 dark:text-blue-400">
              {{ selectedTasks.size }} selezionate
            </span>
            
            <!-- Bulk Actions -->
            <div class="flex space-x-1">
              <button
                @click="bulkMarkCompleted"
                :disabled="bulkLoading"
                :class="[
                  'px-2 py-1 text-xs bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-300 rounded',
                  'hover:bg-green-200 dark:hover:bg-green-900/30 transition-colors',
                  'disabled:opacity-50'
                ]"
                title="Segna come completate"
              >
                <CheckIcon class="w-3 h-3" />
              </button>
              
              <button
                @click="bulkDelete"
                :disabled="bulkLoading"
                :class="[
                  'px-2 py-1 text-xs bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-300 rounded',
                  'hover:bg-red-200 dark:hover:bg-red-900/30 transition-colors',
                  'disabled:opacity-50'
                ]"
                title="Elimina selezionate"
              >
                <TrashIcon class="w-3 h-3" />
              </button>
            </div>
          </div>
        </div>

        <!-- Select All -->
        <button
          v-if="filteredTasks.length > 0"
          @click="toggleSelectAll"
          :class="[
            'text-xs text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300'
          ]"
        >
          {{ selectedTasks.size === filteredTasks.length ? 'Deseleziona tutto' : 'Seleziona tutto' }}
        </button>
      </div>
    </div>

    <!-- Task List Content -->
    <div class="flex-1 overflow-hidden">
      <!-- Loading State -->
      <div v-if="isLoading" class="flex items-center justify-center h-32">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
      </div>

      <!-- Empty State -->
      <div v-else-if="filteredTasks.length === 0" class="flex flex-col items-center justify-center h-full py-12">
        <ListBulletIcon class="w-12 h-12 text-gray-400 dark:text-gray-500 mb-4" />
        <h3 :class="['text-lg font-medium mb-2', textClass]">
          {{ searchQuery || hasActiveFilters ? 'Nessun risultato' : 'Nessuna attività' }}
        </h3>
        <p class="text-gray-500 dark:text-gray-400 text-center max-w-sm">
          {{ getEmptyStateMessage() }}
        </p>
        <button
          v-if="searchQuery || hasActiveFilters"
          @click="clearAllFilters"
          class="mt-4 px-4 py-2 text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300"
        >
          Cancella tutti i filtri
        </button>
      </div>

      <!-- Virtual Scrolling Task List -->
      <div v-else class="h-full overflow-auto" ref="scrollContainer">
        <!-- Grouped by date -->
        <div v-if="groupByDate" class="space-y-6 p-4">
          <div
            v-for="(group, date) in groupedTasks"
            :key="date"
            class="space-y-3"
          >
            <!-- Date Header -->
            <div class="sticky top-0 z-10">
              <h3 :class="[
                'text-sm font-semibold px-3 py-2 rounded-lg border-l-4 border-blue-500',
                cardClass,
                'bg-gray-50 dark:bg-gray-800/50',
                textClass
              ]">
                {{ formatDateHeader(date) }}
                <span class="ml-2 text-xs font-normal text-gray-500 dark:text-gray-400">
                  ({{ group.length }} attività)
                </span>
              </h3>
            </div>
            
            <!-- Tasks in this date group -->
            <div class="space-y-2 ml-4">
              <TaskListItem
                v-for="task in group"
                :key="task.id"
                :task="task"
                :selected="selectedTasks.has(task.id)"
                :show-selection="enableSelection"
                :compact="compactMode"
                @click="handleTaskClick(task)"
                @toggle-selection="toggleTaskSelection(task.id)"
                @toggle-completion="handleToggleCompletion(task.id)"
                @edit="handleEditTask(task)"
                @delete="handleDeleteTask(task)"
              />
            </div>
          </div>
        </div>

        <!-- Simple List (no grouping) -->
        <div v-else class="divide-y divide-gray-200 dark:divide-gray-700">
          <TaskListItem
            v-for="task in visibleTasks"
            :key="task.id"
            :task="task"
            :selected="selectedTasks.has(task.id)"
            :show-selection="enableSelection"
            :compact="compactMode"
            @click="handleTaskClick(task)"
            @toggle-selection="toggleTaskSelection(task.id)"
            @toggle-completion="handleToggleCompletion(task.id)"
            @edit="handleEditTask(task)"
            @delete="handleDeleteTask(task)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useTasks } from '../../composables/useTasks'
import { useTheme } from '../../composables/useTheme'
import { useNotifications } from '../../composables/useNotifications'
import { format, isToday, isTomorrow, isYesterday, parseISO } from 'date-fns'
import { it } from 'date-fns/locale'
import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/vue'
import { 
  TASK_PRIORITY_CONFIG, 
  TASK_SORT_OPTIONS,
  TaskPriority 
} from '../../types/task'
import type { Task, TaskSortOption } from '../../types/task'

// Components
import TaskListItem from './TaskListItem.vue'

// Icons
import {
  MagnifyingGlassIcon,
  XMarkIcon,
  FunnelIcon,
  Bars3BottomLeftIcon,
  ChevronDownIcon,
  ListBulletIcon,
  CheckIcon,
  TrashIcon
} from '@heroicons/vue/24/outline'

// Props
interface Props {
  enableSelection?: boolean
  compactMode?: boolean
  groupByDate?: boolean
  showCompleted?: boolean
  maxHeight?: string
}

const props = withDefaults(defineProps<Props>(), {
  enableSelection: true,
  compactMode: false,
  groupByDate: true,
  showCompleted: true
})

// Emits
const emit = defineEmits<{
  'task-click': [task: Task]
  'task-edit': [task: Task]
  'task-delete': [task: Task]
  'selection-change': [selectedIds: number[]]
}>()

// Composables
const {
  allTasks,
  isLoading,
  searchTasks,
  clearSearch,
  setFilters,
  clearFilters,
  toggleTaskCompletion,
  bulkMarkCompleted: bulkMarkCompletedTasks,
  bulkDelete: bulkDeleteTasks
} = useTasks()
const { textClass, inputClass, cardClass } = useTheme()
const { showSuccess, showError, showConfirm } = useNotifications()

// State
const searchQuery = ref('')
const scrollContainer = ref<HTMLElement | null>(null)
const selectedTasks = ref(new Set<number>())
const bulkLoading = ref(false)

const filters = ref({
  showCompleted: props.showCompleted,
  showPending: true,
  showOverdue: true,
  priority: '' as TaskPriority | '',
  dateFrom: '',
  dateTo: ''
})

const currentSort = ref('dueDate')
const sortDirection = ref<'asc' | 'desc'>('asc')

// Debounced search
const searchDebounceTimer = ref<NodeJS.Timeout | null>(null)

// Computed
const filteredTasks = computed(() => {
  let tasks = [...allTasks.value]
  
  // Filter by completion status
  if (!filters.value.showCompleted) {
    tasks = tasks.filter(task => !task.completed)
  }
  if (!filters.value.showPending) {
    tasks = tasks.filter(task => task.completed)
  }
  if (!filters.value.showOverdue) {
    const now = new Date()
    tasks = tasks.filter(task => 
      task.completed || !task.dueDate || new Date(task.dueDate) >= now
    )
  }
  
  // Filter by priority
  if (filters.value.priority) {
    tasks = tasks.filter(task => task.priority === filters.value.priority)
  }
  
  // Filter by date range
  if (filters.value.dateFrom || filters.value.dateTo) {
    tasks = tasks.filter(task => {
      if (!task.dueDate) return false
      
      const taskDate = format(new Date(task.dueDate), 'yyyy-MM-dd')
      
      if (filters.value.dateFrom && taskDate < filters.value.dateFrom) {
        return false
      }
      if (filters.value.dateTo && taskDate > filters.value.dateTo) {
        return false
      }
      
      return true
    })
  }
  
  // Sort tasks
  tasks.sort((a, b) => {
    const sortOption = TASK_SORT_OPTIONS.find(opt => opt.key === currentSort.value)
    if (!sortOption) return 0
    
    let aValue: any
    let bValue: any
    
    switch (sortOption.value) {
      case 'dueDate':
        aValue = a.dueDate ? new Date(a.dueDate).getTime() : 0
        bValue = b.dueDate ? new Date(b.dueDate).getTime() : 0
        break
      case 'priority':
        const priorityOrder = { LOW: 0, MEDIUM: 1, HIGH: 2, URGENT: 3 }
        aValue = priorityOrder[a.priority] || 0
        bValue = priorityOrder[b.priority] || 0
        break
      case 'title':
        aValue = a.title.toLowerCase()
        bValue = b.title.toLowerCase()
        break
      case 'createdAt':
        aValue = new Date(a.createdAt).getTime()
        bValue = new Date(b.createdAt).getTime()
        break
      case 'completed':
        aValue = a.completed ? 1 : 0
        bValue = b.completed ? 1 : 0
        break
      default:
        return 0
    }
    
    const result = aValue < bValue ? -1 : aValue > bValue ? 1 : 0
    return sortDirection.value === 'desc' ? -result : result
  })
  
  return tasks
})

const groupedTasks = computed(() => {
  if (!props.groupByDate) return {}
  
  const groups: Record<string, Task[]> = {}
  
  filteredTasks.value.forEach(task => {
    const dateKey = task.dueDate ? format(new Date(task.dueDate), 'yyyy-MM-dd') : 'no-date'
    
    if (!groups[dateKey]) {
      groups[dateKey] = []
    }
    groups[dateKey].push(task)
  })
  
  return groups
})

const visibleTasks = computed(() => {
  // For simple virtual scrolling, we'll show all filtered tasks for now
  // In a production app, you'd implement proper virtual scrolling here
  return filteredTasks.value
})

const activeFiltersCount = computed(() => {
  let count = 0
  
  if (!filters.value.showCompleted || !filters.value.showPending || !filters.value.showOverdue) count++
  if (filters.value.priority) count++
  if (filters.value.dateFrom || filters.value.dateTo) count++
  
  return count
})

const hasActiveFilters = computed(() => {
  return activeFiltersCount.value > 0 || searchQuery.value.length > 0
})

const currentSortLabel = computed(() => {
  const sortOption = TASK_SORT_OPTIONS.find(opt => opt.key === currentSort.value)
  return sortOption ? sortOption.label : 'Ordinamento'
})

// Methods
const handleSearch = () => {
  if (searchDebounceTimer.value) {
    clearTimeout(searchDebounceTimer.value)
  }
  
  searchDebounceTimer.value = setTimeout(async () => {
    await searchTasks(searchQuery.value)
  }, 300)
}

const clearSearchQuery = () => {
  searchQuery.value = ''
  clearSearch()
}

const applyFilters = () => {
  // The filtering is handled in the computed property
}

const clearAllFilters = () => {
  searchQuery.value = ''
  filters.value = {
    showCompleted: props.showCompleted,
    showPending: true,
    showOverdue: true,
    priority: '',
    dateFrom: '',
    dateTo: ''
  }
  clearSearch()
  clearFilters()
}

const setSortOption = (sortKey: string, closeMenu: () => void) => {
  if (currentSort.value === sortKey) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    currentSort.value = sortKey
    sortDirection.value = 'asc'
  }
  closeMenu()
}

const formatDateHeader = (dateStr: string): string => {
  if (dateStr === 'no-date') {
    return 'Senza data'
  }
  
  const date = parseISO(dateStr)
  
  if (isToday(date)) {
    return 'Oggi'
  }
  if (isTomorrow(date)) {
    return 'Domani'
  }
  if (isYesterday(date)) {
    return 'Ieri'
  }
  
  return format(date, 'EEEE d MMMM yyyy', { locale: it })
}

const getEmptyStateMessage = (): string => {
  if (searchQuery.value) {
    return `Nessuna attività trovata per "${searchQuery.value}"`
  }
  if (hasActiveFilters.value) {
    return 'Nessuna attività corrisponde ai filtri selezionati'
  }
  return 'Non hai ancora creato nessuna attività. Inizia creando la tua prima attività!'
}

const toggleTaskSelection = (taskId: number) => {
  if (selectedTasks.value.has(taskId)) {
    selectedTasks.value.delete(taskId)
  } else {
    selectedTasks.value.add(taskId)
  }
  
  emit('selection-change', Array.from(selectedTasks.value))
}

const toggleSelectAll = () => {
  if (selectedTasks.value.size === filteredTasks.value.length) {
    selectedTasks.value.clear()
  } else {
    selectedTasks.value = new Set(filteredTasks.value.map(task => task.id))
  }
  
  emit('selection-change', Array.from(selectedTasks.value))
}

const handleTaskClick = (task: Task) => {
  emit('task-click', task)
}

const handleEditTask = (task: Task) => {
  emit('task-edit', task)
}

const handleDeleteTask = (task: Task) => {
  emit('task-delete', task)
}

const handleToggleCompletion = async (taskId: number) => {
  await toggleTaskCompletion(taskId)
}

const bulkMarkCompleted = async () => {
  if (selectedTasks.value.size === 0) return
  
  const confirmed = await showConfirm(
    'Conferma operazione',
    `Vuoi segnare come completate ${selectedTasks.value.size} attività?`
  )
  
  if (!confirmed) return
  
  bulkLoading.value = true
  
  try {
    const success = await bulkMarkCompletedTasks(Array.from(selectedTasks.value))
    
    if (success) {
      selectedTasks.value.clear()
      emit('selection-change', [])
    }
  } finally {
    bulkLoading.value = false
  }
}

const bulkDelete = async () => {
  if (selectedTasks.value.size === 0) return
  
  const confirmed = await showConfirm(
    'Elimina attività',
    `Sei sicuro di voler eliminare ${selectedTasks.value.size} attività? Questa azione non può essere annullata.`
  )
  
  if (!confirmed) return
  
  bulkLoading.value = true
  
  try {
    const success = await bulkDeleteTasks(Array.from(selectedTasks.value))
    
    if (success) {
      selectedTasks.value.clear()
      emit('selection-change', [])
    }
  } finally {
    bulkLoading.value = false
  }
}

// Expose methods for parent components
defineExpose({
  clearAllFilters,
  selectedTasks: computed(() => Array.from(selectedTasks.value)),
  filteredTasksCount: computed(() => filteredTasks.value.length)
})
</script>

<style scoped>
/* Custom scrollbar */
.overflow-auto {
  scrollbar-width: thin;
  scrollbar-color: rgb(156 163 175) transparent;
}

.overflow-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-auto::-webkit-scrollbar-track {
  background: transparent;
}

.overflow-auto::-webkit-scrollbar-thumb {
  background-color: rgb(156 163 175);
  border-radius: 3px;
}

.overflow-auto::-webkit-scrollbar-thumb:hover {
  background-color: rgb(107 114 128);
}

.dark .overflow-auto {
  scrollbar-color: rgb(75 85 99) transparent;
}

.dark .overflow-auto::-webkit-scrollbar-thumb {
  background-color: rgb(75 85 99);
}

/* Loading spinner */
@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* Smooth transitions */
.task-list * {
  transition-property: background-color, border-color, color;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
}

/* Focus styles */
.task-list button:focus,
.task-list input:focus,
.task-list select:focus {
  outline: 2px solid rgb(59 130 246);
  outline-offset: 2px;
}

/* Sticky headers */
.sticky {
  position: sticky;
  backdrop-filter: blur(8px);
}

/* Improved mobile responsiveness */
@media (max-width: 640px) {
  .task-list .grid-cols-2 {
    grid-template-columns: 1fr;
  }
  
  .task-list .flex-col.sm\:flex-row {
    flex-direction: column;
  }
}
</style>