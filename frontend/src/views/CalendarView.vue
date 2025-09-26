<template>
  <div class="h-screen bg-gray-50 dark:bg-gray-900">
    <!-- Header -->
    <header class="bg-white dark:bg-gray-800 shadow-sm border-b-2 border-gray-200 dark:border-gray-700" style="height: 3.9rem;">
      <div class="px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <!-- Mobile Menu Button + Logo and Title -->
          <div class="flex items-center">
            <!-- Mobile Sidebar Toggle -->
            <button @click="showMobileSidebar = !showMobileSidebar"
              class="md:hidden p-2 rounded-md text-gray-400 hover:text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 mr-2"
              title="Toggle menu">
              <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>

            <h1 class="text-xl md:text-2xl font-bold text-gray-900 dark:text-white">
              P-Cal
            </h1>
            <span class="ml-2 text-xs md:text-sm text-gray-500 dark:text-gray-400 hidden sm:inline">
              {{ formatDisplayDate(currentDate) }}
            </span>
          </div>

          <!-- Navigation Controls -->
          <div class="flex items-center space-x-2 md:space-x-4">
            <!-- View Mode Selector -->
            <div class="hidden sm:flex bg-gray-100 dark:bg-gray-700 rounded-md p-1">
              <button v-for="view in CALENDAR_VIEWS" :key="view.value" @click="setViewMode(view.value)"
                class="px-2 md:px-3 py-1 text-xs md:text-sm font-medium rounded transition-colors" :class="{
                  'bg-white dark:bg-gray-600 text-gray-900 dark:text-white shadow-sm': viewMode === view.value,
                  'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white': viewMode !== view.value
                }" :title="`${view.label} (${view.shortcut})`">
                {{ view.label }}
              </button>
            </div>

            <!-- Mobile View Mode Dropdown -->
            <div class="sm:hidden relative">
              <select :value="viewMode" @change="setViewMode($event.target.value)"
                class="bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 px-2 py-1 text-sm rounded-md border-0 focus:ring-2 focus:ring-blue-500">
                <option v-for="view in CALENDAR_VIEWS" :key="view.value" :value="view.value">
                  {{ view.label }}
                </option>
              </select>
            </div>

            <!-- Navigation Buttons -->
            <div class="flex items-center space-x-1">
              <button @click="navigatePrevious"
                class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md"
                title="Periodo precedente (Ctrl + ←)">
                <ChevronLeftIcon class="h-5 w-5" />
              </button>

              <button @click="goToToday"
                class="px-2 md:px-3 py-2 text-xs md:text-sm font-medium text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md"
                title="Vai a oggi (Ctrl + T)">
                <span class="hidden sm:inline">Oggi</span>
                <span class="sm:hidden">•</span>
              </button>

              <button @click="navigateNext"
                class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md"
                title="Periodo successivo (Ctrl + →)">
                <ChevronRightIcon class="h-5 w-5" />
              </button>
            </div>

          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <div class="flex h-[calc(100vh-4rem)] relative">
      <!-- Mobile Sidebar Overlay -->
      <div v-if="showMobileSidebar" @click="showMobileSidebar = false"
        class="fixed inset-0 bg-gray-600 bg-opacity-75 z-40 md:hidden"></div>

      <!-- Sidebar -->
      <CalendarSidebar
        :show-mobile="showMobileSidebar"
        :user="user"
        :task-stats="taskStats"
        :today-tasks="todayTasks"
        :upcoming-reminders="upcomingReminders"
        :current-view-mode="viewMode"
        @task-click="openTaskModal"
        @new-task="openCreateTaskModalWithDate()"
        @switch-to-day-view="handleSwitchToDayView"
        @close-sidebar="closeMobileSidebar"
      />

      <!-- Calendar Area -->
      <main class="flex-1 flex flex-col overflow-hidden md:ml-0">
        <!-- Calendar Header -->
        <div class="p-3 md:p-4 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex-shrink-0">
          <div class="flex items-center justify-between">
            <div v-if="isAgendaView">
              <h2 class="text-base md:text-lg font-medium text-gray-900 dark:text-white">
                Agenda
              </h2>
              <p class="text-xs md:text-sm text-gray-500 dark:text-gray-400 mt-1">
                Visualizza le attività dei prossimi {{ agendaDays }} giorni
              </p>
            </div>
            <h2 v-else class="text-base md:text-lg font-medium text-gray-900 dark:text-white">
              {{ currentMonthName }}
            </h2>
            <!-- Mobile Close Sidebar Button (only visible when sidebar is open) -->
            <button v-if="showMobileSidebar" @click="showMobileSidebar = false"
              class="md:hidden p-2 rounded-md text-gray-400 hover:text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700">
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Calendar Content -->
        <div class="flex-1 p-2 md:p-4 overflow-auto">
          <!-- Month View -->
          <div v-if="isMonthView" class="calendar-grid-mobile md:calendar-grid">
            <!-- Week Days Header -->
            <div v-for="day in settings.weekdaysShort" :key="day" class="calendar-day-header">
              {{ day }}
            </div>

            <!-- Calendar Days -->
            <div v-for="day in calendarDays" :key="day.date.getTime()" @click="selectDate(day.date)"
              @dblclick="openCreateTaskModalWithDate(day.date)"
              class="calendar-day min-h-20 md:min-h-32 cursor-pointer transition-colors p-1 md:p-2" :class="{
                'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-400': day.isSelected,
                'bg-yellow-50 dark:bg-yellow-900/20': day.isToday && !day.isSelected,
                'text-gray-400 dark:text-gray-600': !day.isCurrentMonth,
                'hover:bg-gray-100 dark:hover:bg-gray-700': !day.isSelected && !day.isToday
              }">
              <!-- Day Number -->
              <div class="flex justify-between items-center mb-2">
                <span class="text-sm font-medium" :class="{
                  'text-blue-600 dark:text-blue-400': day.isSelected,
                  'text-yellow-700 dark:text-yellow-300': day.isToday && !day.isSelected,
                  'text-gray-900 dark:text-white': day.isCurrentMonth && !day.isToday && !day.isSelected,
                  'text-gray-400 dark:text-gray-600': !day.isCurrentMonth
                }">
                  {{ day.dayOfMonth }}
                </span>
                <div v-if="day.tasks && day.tasks.length > 0" class="text-xs text-gray-500 dark:text-gray-400">
                  {{ day.tasks.length }}
                </div>
              </div>

              <!-- Tasks -->
              <div class="space-y-1">
                <div v-for="task in (day.tasks || []).slice(0, 3)" :key="task.id"
                  @click.stop="openTaskModal(getTaskById(task.id)!)"
                  class="text-xs p-1 rounded truncate cursor-pointer transition-colors"
                  :class="getTaskDisplayClasses(task)"
                  :style="getTaskDisplayStyle(task)">
                  {{ task.title }}
                </div>
                <button
                  v-if="day.tasks && day.tasks.length > 3"
                  @click.stop="openDayView(day.date)"
                  class="text-xs text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 p-1 hover:underline transition-colors">
                  +{{ day.tasks.length - 3 }} altro/i
                </button>
              </div>
            </div>
          </div>

          <!-- Week View -->
          <WeekView 
            v-else-if="isWeekView"
            :current-date="currentDate"
            :tasks="allWeekTasks"
            :scroll-top="scrollTop"
            :scroll-height="scrollHeight"
            :client-height="clientHeight"
            @task-click="openTaskModal"
            @scroll="handleWeeklyScroll"
            ref="weekViewRef"
          />

          <!-- Day View -->
          <div v-else-if="isDayView" class="h-full">
            <div class="bg-white dark:bg-gray-800 rounded-lg p-4">
              <h3 class="font-medium text-gray-900 dark:text-white mb-3">
                {{ getDayName(currentDate) }}, {{ formatDate(currentDate) }}
              </h3>

              <div class="space-y-2">
                <!-- Current/Future Tasks -->
                <div v-if="currentDayTasks.length > 0" class="space-y-2">
                  <TaskCard
                    v-for="task in currentDayTasks"
                    :key="task.id"
                    :task="task"
                    :task-display-classes="getTaskDisplayClasses(task)"
                    :task-display-style="getTaskDisplayStyle(task)"
                    @click="openTaskModal"
                  />
                </div>

                <!-- Separator and Past Tasks -->
                <div v-if="pastDayTasks.length > 0">
                  <!-- Clickable Separator line -->
                  <div
                    class="flex items-center my-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50 rounded-md py-2 transition-colors"
                    @click="togglePastTasks"
                  >
                    <div class="flex-1 border-t border-gray-200 dark:border-gray-600"></div>
                    <span class="px-3 text-xs text-gray-500 dark:text-gray-400 bg-white dark:bg-gray-800 flex items-center gap-2">
                      Attività completate ({{ pastDayTasks.length }})
                      <ChevronDownIcon
                        class="h-4 w-4 transition-transform duration-200"
                        :class="{ 'rotate-180': showPastTasks }"
                      />
                    </span>
                    <div class="flex-1 border-t border-gray-200 dark:border-gray-600"></div>
                  </div>

                  <!-- Collapsible Past Tasks -->
                  <div v-show="showPastTasks" class="space-y-2">
                    <TaskCard
                      v-for="task in pastDayTasks"
                      :key="task.id"
                      :task="task"
                      :task-display-classes="getTaskDisplayClasses(task)"
                      :task-display-style="getTaskDisplayStyle(task)"
                      @click="openTaskModal"
                    />
                  </div>
                </div>

                <!-- Empty state -->
                <div v-if="currentDayTasks.length === 0 && pastDayTasks.length === 0" class="text-center py-8">
                  <p class="text-gray-500 dark:text-gray-400">
                    Nessuna attività programmata per oggi
                  </p>
                  <button @click="openCreateTaskModalWithDate(currentDate)"
                    class="mt-2 text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300">
                    Crea la prima attività
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Agenda View -->
          <div v-else-if="isAgendaView" class="space-y-4">
            <div v-for="(dayTasks, date) in sortedTasksByDateInRange" :key="date"
              class="bg-white dark:bg-gray-800 rounded-lg p-4">
              <h3 class="font-medium text-gray-900 dark:text-white mb-3">
                {{ getDateDescription(new Date(date)) }}
              </h3>
              <!-- Special handling for today's tasks with separator -->
              <div v-if="isDateToday(date)" class="space-y-2">
                <!-- Current/Future Tasks for today -->
                <div v-if="splitTasksByTime(dayTasks).current.length > 0" class="space-y-2">
                  <TaskCard
                    v-for="task in splitTasksByTime(dayTasks).current"
                    :key="task.id"
                    :task="task"
                    :task-display-classes="getTaskDisplayClasses(task)"
                    :task-display-style="getTaskDisplayStyle(task)"
                    @click="openTaskModal"
                  />
                </div>

                <!-- Separator and Past Tasks for today -->
                <div v-if="splitTasksByTime(dayTasks).past.length > 0">
                  <!-- Clickable Separator line -->
                  <div
                    class="flex items-center my-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50 rounded-md py-2 transition-colors"
                    @click="togglePastTasks"
                  >
                    <div class="flex-1 border-t border-gray-200 dark:border-gray-600"></div>
                    <span class="px-3 text-xs text-gray-500 dark:text-gray-400 bg-white dark:bg-gray-800 flex items-center gap-2">
                      Attività completate ({{ splitTasksByTime(dayTasks).past.length }})
                      <ChevronDownIcon
                        class="h-4 w-4 transition-transform duration-200"
                        :class="{ 'rotate-180': showPastTasks }"
                      />
                    </span>
                    <div class="flex-1 border-t border-gray-200 dark:border-gray-600"></div>
                  </div>

                  <!-- Collapsible Past Tasks for today -->
                  <div v-show="showPastTasks" class="space-y-2">
                    <TaskCard
                      v-for="task in splitTasksByTime(dayTasks).past"
                      :key="task.id"
                      :task="task"
                      :task-display-classes="getTaskDisplayClasses(task)"
                      :task-display-style="getTaskDisplayStyle(task)"
                      @click="openTaskModal"
                    />
                  </div>
                </div>
              </div>

              <!-- Normal display for other dates -->
              <div v-else class="space-y-2">
                <TaskCard
                  v-for="task in dayTasks"
                  :key="task.id"
                  :task="task"
                  :task-display-classes="getTaskDisplayClasses(task)"
                  :task-display-style="getTaskDisplayStyle(task)"
                  @click="openTaskModal"
                />
              </div>
            </div>

            <div v-if="Object.keys(tasksByDateInRange || {}).length === 0" class="text-center py-8">
              <p class="text-gray-500 dark:text-gray-400">
                Nessuna attività in questo periodo
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- Task Detail Modal -->
    <TaskDetailModal
      :show="showTaskModal"
      :task="selectedTask"
      @close="closeTaskModal"
      @edit="handleTaskDetailEdit"
      @delete="handleTaskDeleted"
    />

    <!-- Task Modal -->
    <TaskModal
      :show="showCreateTaskModal"
      :task="selectedTaskForEdit"
      :initial-date="createTaskDate"
      @close="handleTaskModalClose"
      @task-created="handleTaskCreated"
      @task-updated="handleTaskUpdated"
      @task-deleted="handleTaskDeleted"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  ChevronDownIcon,
  PlusIcon,
  SunIcon,
  MoonIcon
} from '@heroicons/vue/24/outline'
import TaskModal from '../components/TaskModal.vue'
import TaskDetailModal from '../components/TaskDetailModal.vue'
import TaskCard from '../components/TaskCard.vue'
import WeekView from '../components/Calendar/WeekView.vue'
import CalendarSidebar from '../components/Calendar/CalendarSidebar.vue'
import type { Task } from '../types/task'

// Composables
import { useAuth } from '../composables/useAuth'
import { useRouter } from 'vue-router'
import { useCalendar } from '../composables/useCalendar'
import { useTasks } from '../composables/useTasks'
import { useReminders } from '../composables/useReminders'
import { useTheme } from '../composables/useTheme'

// Utilities
import {
  formatDate,
  formatDateTime,
  formatTime as formatTimeUtil,
  getDayName,
  getDateDescription,
  isToday as isDateToday
} from '../utils/dateHelpers'
import { it } from 'date-fns/locale'
// import { formatTaskPriority } from '../utils/formatters' // Removed as priority is not handled
import { CALENDAR_VIEWS } from '../utils/constants'

// Reminder notification service
import { startReminderNotifications, stopReminderNotifications } from '../services/reminderNotificationService'

// Composable instances
const auth = useAuth()
const $router = useRouter()
const calendar = useCalendar()
const tasks = useTasks()
const reminders = useReminders()
const theme = useTheme()
// Settings store
import { useSettingsStore } from '../stores/settings'
const settings = useSettingsStore()

// Reactive state
const showMobileSidebar = ref(false)
const showPastTasks = ref(false)
const selectedTaskForEdit = ref<Task | null>(null)
const createTaskDate = ref<Date | undefined>(undefined)

// Weekly view scroll tracking  
const weeklyScrollContainer = ref<HTMLElement | null>(null)
const weekViewRef = ref<InstanceType<typeof WeekView> | null>(null)
const scrollTop = ref(0)
const scrollHeight = ref(0)
const clientHeight = ref(0)

// Force reactivity with ref
const indicatorsUpdateTrigger = ref(0)

// Watch scroll changes to force indicators update
watch([scrollTop, clientHeight, scrollHeight], () => {
  indicatorsUpdateTrigger.value++
})

// Computed property for overflow indicators to ensure reactivity
const weekDaysWithIndicators = computed(() => {
  // Force reactivity on scroll changes
  indicatorsUpdateTrigger.value
  
  const weekDays = calendar.getWeekDays(currentDate.value)
  return weekDays.map(day => ({
    day,
    indicators: getTasksOverflowIndicators(calendar.getTasksForDate(day))
  }))
})

// Computed properties from composables
const {
  user,
  userFullName,
  userInitials,
  logout
} = auth

const {
  currentDate,
  selectedDate,
  viewMode,
  agendaDays,
  currentMonthName,
  calendarDays,
  todayTasks,
  isMonthView,
  isWeekView,
  isDayView,
  isAgendaView,
  showTaskModal,
  selectedTask,
  showCreateTaskModal,
  setViewMode,
  navigatePrevious,
  navigateNext,
  goToToday,
  selectDate,
  openTaskModal,
  closeTaskModal,
  openCreateTaskModal,
  closeCreateTaskModal,
  formatDisplayDate,
  getTasksForDate,
  getWeekDays
} = calendar

const {
  taskStats,
  getTaskById
} = tasks

const {
  upcomingReminders,
  formatReminderTimeShort
} = reminders

const {
  isDarkMode,
  themeName,
  toggleTheme
} = theme

// Reset past tasks visibility when view mode or date changes
watch([viewMode, currentDate], () => {
  showPastTasks.value = false
})

// Additional computed properties
const tasksByDateInRange = computed(() => {
  const range = calendar.viewDateRange.value
  if (!range) return {}

  const result: Record<string, any[]> = {}
  const allTasks = Array.isArray(tasks.allTasks?.value) ? tasks.allTasks.value : 
                     Array.isArray(tasks.allTasks) ? tasks.allTasks : []


  allTasks.forEach(task => {
    if (task && task.startDatetime) {
      const taskDate = new Date(task.startDatetime)
      if (taskDate >= range.start && taskDate <= range.end) {
        const dateKey = formatDate(taskDate, 'yyyy-MM-dd')
        if (!result[dateKey]) result[dateKey] = []
        result[dateKey].push(task)
      }
    }
  })

  return result
})

// Sort tasks by date for agenda view
const sortedTasksByDateInRange = computed(() => {
  const tasks = tasksByDateInRange.value
  
  // Convert object to array of [date, tasks] pairs and sort by date
  const sortedEntries = Object.entries(tasks)
    .sort(([dateA], [dateB]) => {
      return new Date(dateA).getTime() - new Date(dateB).getTime()
    })
    .map(([date, dayTasks]) => [
      date, 
      // Also sort tasks within each day by start time
      (dayTasks || []).sort((a, b) => {
        return new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime()
      })
    ])

  // Convert back to object
  const sortedResult: Record<string, any[]> = {}
  sortedEntries.forEach(([date, dayTasks]) => {
    sortedResult[date] = dayTasks
  })
  
  return sortedResult
})

// Tasks for day view - split into current and past
const currentDayTasks = computed(() => {
  const now = new Date()
  return getTasksForDate(currentDate.value)
    .filter(task => new Date(task.endDatetime) >= now)
    .sort((a, b) => new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime())
})

const pastDayTasks = computed(() => {
  const now = new Date()
  return getTasksForDate(currentDate.value)
    .filter(task => new Date(task.endDatetime) < now)
    .sort((a, b) => new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime())
})

// Helper function to split tasks for a specific date (used in agenda view)
const splitTasksByTime = (tasks: any[]) => {
  const now = new Date()
  const current = tasks
    .filter(task => new Date(task.endDatetime) >= now)
    .sort((a, b) => new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime())
  const past = tasks
    .filter(task => new Date(task.endDatetime) < now)
    .sort((a, b) => new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime())

  return { current, past }
}

// Check if a date is today
const isDateToday = (date: string) => {
  const today = formatDate(new Date(), 'yyyy-MM-dd')
  return date === today
}

// Methods
const isToday = (date: Date) => isDateToday(date)

// getPriorityColor removed as priority no longer exists in Task model

// Centralized color mapping
const TASK_COLOR_MAP: Record<string, string> = {
  '#3b82f6': 'blue',
  '#3788d8': 'blue', // Default blue
  '#10b981': 'emerald',
  '#ef4444': 'red',
  '#f59e0b': 'amber',
  '#8b5cf6': 'violet',
  '#ec4899': 'pink',
  '#6366f1': 'indigo',
  '#14b8a6': 'teal',
  '#f97316': 'orange',
  '#6b7280': 'gray',
  '#22c55e': 'green',
  '#a855f7': 'purple',
  '#06b6d4': 'cyan',
  '#84cc16': 'lime',
  '#eab308': 'yellow',
  '#f43f5e': 'rose'
}

const getTaskDisplayClasses = (task: any, detailed = false) => {
  const baseClasses = detailed
    ? 'border-l-4'
    : 'border-l-2' // Add colored border for monthly view as well

  const isPast = new Date(task.endDatetime) < new Date()
  const color = task.color || '#3788d8'
  const colorName = TASK_COLOR_MAP[color]

  if (colorName) {
    // For Tailwind colors, use background classes but no border color classes (we'll use inline styles)
    const classes = `${baseClasses} bg-${colorName}-50 dark:bg-${colorName}-900/20 hover:bg-${colorName}-100 dark:hover:bg-${colorName}-900/30 task-custom-color`
    return isPast ? `${classes} opacity-25 hover:opacity-75` : classes
  } else {
    // For custom hex colors, use neutral background and inline styles
    const classes = `${baseClasses} task-custom-color bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600`
    return isPast ? `${classes} opacity-25 hover:opacity-75` : classes
  }
}

const getTaskDisplayStyle = (task: any) => {
  const color = task.color || '#3788d8'
  const colorName = TASK_COLOR_MAP[color]


  const hexToRgba = (hex: string, alpha: number) => {
    const r = parseInt(hex.slice(1, 3), 16)
    const g = parseInt(hex.slice(3, 5), 16)
    const b = parseInt(hex.slice(5, 7), 16)
    return `rgba(${r}, ${g}, ${b}, ${alpha})`
  }

  // For ALL colors, use CSS custom properties and inline styles
  const style: any = {
    '--task-color': color,
    borderLeftColor: color,
    borderLeftWidth: '2px',
    borderLeftStyle: 'solid'
  }

  // For custom colors (not in Tailwind map), also set background
  if (!colorName) {
    style['--task-bg-color'] = hexToRgba(color, 0.1)
    style.backgroundColor = hexToRgba(color, 0.1)
  }


  return style
}

const handleKeyboardShortcuts = (event: KeyboardEvent) => {
  calendar.handleKeyboardNavigation(event)

  // Additional shortcuts
  if (event.ctrlKey && event.key === 'n') {
    event.preventDefault()
    openCreateTaskModalWithDate()
  }
}

const handleClickOutside = (event: Event) => {
  // Handle click outside for mobile sidebar if needed
}

const closeMobileSidebar = () => {
  showMobileSidebar.value = false
}

const togglePastTasks = () => {
  showPastTasks.value = !showPastTasks.value
}

// Task Modal Methods
const handleTaskModalClose = () => {
  selectedTaskForEdit.value = null
  createTaskDate.value = undefined
  closeCreateTaskModal()
}

const handleTaskCreated = async (task: Task) => {
  // Refresh tasks data
  await tasks.fetchTasks()
  await tasks.refreshStatistics()
  // Refresh reminders to update sidebar
  await reminders.fetchAllReminders()
}

const handleTaskUpdated = async (task: Task) => {
  // Refresh tasks data
  await tasks.fetchTasks()
  await tasks.refreshStatistics()
  // Refresh reminders to update sidebar
  await reminders.fetchAllReminders()
}

const handleTaskDeleted = async (taskId: number) => {
  // Close the detail modal
  closeTaskModal()
  // Refresh tasks data
  await tasks.fetchTasks()
  await tasks.refreshStatistics()
  // Refresh reminders to update sidebar
  await reminders.fetchAllReminders()
}

// Override calendar methods to use our enhanced modal
const openCreateTaskModalWithDate = (date?: Date) => {
  selectedTaskForEdit.value = null
  createTaskDate.value = date
  calendar.openCreateTaskModal(date)
}

// Handle editing from task detail modal
const handleTaskDetailEdit = (task: Task) => {
  // Close the detail modal first
  calendar.closeTaskModal()

  // Then open the edit modal
  selectedTaskForEdit.value = task
  createTaskDate.value = undefined
  calendar.openCreateTaskModal()
}

// Weekly view scroll management
const handleWeeklyScroll = (event: Event) => {
  const target = event.target as HTMLElement
  scrollTop.value = target.scrollTop
  scrollHeight.value = target.scrollHeight
  clientHeight.value = target.clientHeight
}

// Overflow indicators logic
const getTasksOverflowIndicators = (dayTasks: Task[]) => {
  if (!dayTasks.length) return { top: [], bottom: [] }
  
  const visibleTop = scrollTop.value
  const visibleBottom = scrollTop.value + clientHeight.value
  
  const topHiddenTasks: Task[] = []
  const bottomHiddenTasks: Task[] = []
  
  console.log('📊 Indicator calculation:', {
    visibleTop,
    visibleBottom,
    scrollTop: scrollTop.value,
    clientHeight: clientHeight.value,
    tasksCount: dayTasks.length
  })
  
  dayTasks.forEach(task => {
    if (!task.startDatetime) return
    
    const start = new Date(task.startDatetime)
    const end = task.endDatetime ? new Date(task.endDatetime) : new Date(start.getTime() + 60 * 60 * 1000)
    
    const startHour = start.getHours() + start.getMinutes() / 60
    let endHour = end.getHours() + end.getMinutes() / 60
    
    // Fix for tasks that span midnight or have end time before start time
    if (endHour < startHour) {
      endHour += 24 // Add 24 hours if end is next day
    }
    
    const taskTop = startHour * 64
    const taskBottom = endHour * 64
    
    console.log(`📋 Task "${task.title}":`, {
      startHour,
      endHour,
      taskTop,
      taskBottom,
      visibleTop,
      visibleBottom,
      isHiddenAbove: taskBottom <= visibleTop,
      isHiddenBelow: taskTop >= visibleBottom,
      isVisible: !(taskBottom <= visibleTop) && !(taskTop >= visibleBottom)
    })
    
    // Task completamente sopra l'area visibile
    if (taskBottom <= visibleTop) {
      topHiddenTasks.push(task)
      console.log(`⬆️ Adding "${task.title}" to TOP indicators`)
    }
    // Task completamente sotto l'area visibile  
    else if (taskTop >= visibleBottom) {
      bottomHiddenTasks.push(task)
      console.log(`⬇️ Adding "${task.title}" to BOTTOM indicators`)
    } else {
      console.log(`👁️ "${task.title}" is VISIBLE`)
    }
  })
  
  console.log('🎯 Final result:', {
    topCount: topHiddenTasks.length,
    bottomCount: bottomHiddenTasks.length,
    topTasks: topHiddenTasks.map(t => t.title),
    bottomTasks: bottomHiddenTasks.map(t => t.title)
  })
  
  return {
    top: topHiddenTasks,
    bottom: bottomHiddenTasks
  }
}

// Weekly view helper methods
const getTaskTimePosition = (task: any) => {
  // Use visual times for positioning if available (for split multi-day tasks)
  const startTimeStr = task._visualStartTime || task.startDatetime
  const endTimeStr = task._visualEndTime || task.endDatetime
  
  if (!startTimeStr) return { top: '0px', height: '32px' }
  
  const start = new Date(startTimeStr)
  const end = endTimeStr ? new Date(endTimeStr) : new Date(start.getTime() + 60 * 60 * 1000) // Default 1 hour
  
  // Calculate position based on hours (each hour = 64px height)
  // All times now use local timezone for consistent positioning
  const startHour = start.getHours() + start.getMinutes() / 60
  let endHour = end.getHours() + end.getMinutes() / 60
  
  // For VISUAL positioning: limit to current day (max 24:00)
  if (endHour < startHour) {
    // Task spans midnight - show only until end of day for this column
    endHour = 24
  }
  
  const topPosition = startHour * 64 // 64px per hour (h-16 = 4rem = 64px)
  const height = Math.max((endHour - startHour) * 64, 32) // Minimum 32px height
  
  return {
    top: `${topPosition}px`,
    height: `${height}px`
  }
}

const getTaskTimeDisplayClasses = (task: Task) => {
  const baseClasses = ['border-l-4']

  const isPast = new Date(task.endDatetime) < new Date()

  // Color based on task color or default
  if (task.color) {
    const style = document.createElement('div')
    style.style.backgroundColor = task.color
    const rgb = window.getComputedStyle(style).backgroundColor
    baseClasses.push('text-white')
    // We'll use inline styles for custom colors
  } else {
    // Default blue theme
    baseClasses.push('bg-blue-500 text-white border-blue-700')
  }

  if (isPast) {
    baseClasses.push('opacity-25', 'hover:opacity-75')
  }

  return baseClasses.join(' ')
}

const getTaskTimeStyle = (task: Task) => {
  const positionStyle = getTaskTimePosition(task)
  
  if (task.color) {
    return {
      ...positionStyle,
      backgroundColor: task.color,
      borderLeftColor: task.color
    }
  }
  
  return positionStyle
}

const formatTaskTime = (task: Task) => {
  if (!task.startDatetime) return ''
  
  const start = new Date(task.startDatetime)
  const startTime = formatDate(start, 'HH:mm')
  
  if (task.endDatetime) {
    const end = new Date(task.endDatetime)
    const endTime = formatDate(end, 'HH:mm')
    return `${startTime} - ${endTime}`
  }
  
  return startTime
}

const getWeekDayName = (date: Date, short = false) => {
  return formatDate(date, short ? 'EEE' : 'EEEE', { locale: it })
}

// Use settings-aware time formatting
const formatTime = (date: Date | string): string => {
  return settings.formatTime(date)
}

// Handle switch to day view from sidebar
const handleSwitchToDayView = () => {
  currentDate.value = new Date()
  setViewMode('day')
}

// Open day view for a specific date (from month view)
const openDayView = (date: Date) => {
  currentDate.value = date
  setViewMode('day')
}

// Lifecycle
onMounted(async () => {
  // Initialize auth and require authentication
  await auth.requireAuth()

  // Initialize settings 
  settings.loadSettings()
  
  // Initialize calendar view mode
  calendar.initializeViewMode()

  // Fetch initial data
  await tasks.fetchTasks()
  await reminders.fetchAllReminders()

  // Fetch accurate statistics from dedicated endpoints
  await tasks.refreshStatistics()

  // Set up event listeners
  document.addEventListener('keydown', handleKeyboardShortcuts)
  document.addEventListener('click', handleClickOutside)

  // Initialize scroll values for weekly view
  if (weeklyScrollContainer.value) {
    scrollTop.value = weeklyScrollContainer.value.scrollTop
    scrollHeight.value = weeklyScrollContainer.value.scrollHeight
    clientHeight.value = weeklyScrollContainer.value.clientHeight
  }

  // Start reminder notification service
  console.log('🔔 Starting reminder notification service from CalendarView')
  await startReminderNotifications()
})

// Multi-day task splitting logic - collect all tasks from the week first
const allWeekTasks = computed(() => {
  const weekDays = calendar.getWeekDays(currentDate.value)
  const weekStart = formatDate(weekDays[0], 'yyyy-MM-dd')
  const weekEnd = formatDate(weekDays[weekDays.length - 1], 'yyyy-MM-dd')
  const allTasks: any[] = []

  // Get ALL tasks from the composable
  const allStoreTasks = tasks.allTasks.value || []

  // Filter tasks that overlap with the current week
  allStoreTasks.forEach(task => {
    if (!task.startDatetime || !task.endDatetime) return

    const taskStart = formatDate(new Date(task.startDatetime), 'yyyy-MM-dd')
    const taskEnd = formatDate(new Date(task.endDatetime), 'yyyy-MM-dd')

    // Include task if it overlaps with the week:
    // - Task starts before/during week AND ends during/after week
    // - This covers: tasks starting before week, tasks within week, tasks ending after week
    if (taskStart <= weekEnd && taskEnd >= weekStart) {
      allTasks.push(task)
    }
  })

  return allTasks
})

const getTasksWithSplitsForDate = (date: Date) => {
  const currentDay = formatDate(date, 'yyyy-MM-dd')
  const allTasksWithSplits: any[] = []
  
  console.log(`🌞 Processing tasks for ${currentDay}`)
  
  // Check ALL week tasks, not just ones assigned to this day
  allWeekTasks.value.forEach(task => {
    if (!task.startDatetime) {
      // Task without start time - only show on its "assigned" day
      const dayTasks = calendar.getTasksForDate(date)
      if (dayTasks.find(t => t.id === task.id)) {
        allTasksWithSplits.push(task)
      }
      return
    }
    
    const startDate = new Date(task.startDatetime)
    const endDate = task.endDatetime ? new Date(task.endDatetime) : startDate
    const taskStartDay = formatDate(startDate, 'yyyy-MM-dd')
    const taskEndDay = formatDate(endDate, 'yyyy-MM-dd')
    
    console.log(`📋 Task "${task.title}":`, {
      taskStartDay,
      taskEndDay,
      currentDay,
      spansMultipleDays: taskStartDay !== taskEndDay
    })
    
    // Check if this task should appear on the current day
    if (currentDay >= taskStartDay && currentDay <= taskEndDay) {
      
      if (taskStartDay === taskEndDay) {
        // Single day task - show as normal but only on the correct day
        if (currentDay === taskStartDay) {
          allTasksWithSplits.push(task)
        }
      } else {
        // Multi-day task - create split version for visual positioning only
        let visualStartTime: string
        let visualEndTime: string
        
        if (currentDay === taskStartDay) {
          // First day: from original start time to end of day
          visualStartTime = task.startDatetime
          visualEndTime = `${currentDay}T23:59:59`
          console.log(`🚀 First day split for "${task.title}": visual ${visualStartTime} → ${visualEndTime}`)
        } else if (currentDay === taskEndDay) {
          // Last day: from start of day to original end time  
          visualStartTime = `${currentDay}T00:00:00`
          visualEndTime = task.endDatetime
          console.log(`🏁 Last day split for "${task.title}": visual ${visualStartTime} → ${visualEndTime}`)
        } else {
          // Middle day: full day (using local timezone format for consistent positioning)
          visualStartTime = `${currentDay}T00:00:00`
          visualEndTime = `${currentDay}T23:59:59`
          console.log(`🔄 Middle day split for "${task.title}": visual full day`)
        }
        
        // Create task with ORIGINAL data but visual positioning times
        const splitTask = {
          ...task, // Keep ALL original data
          // Add visual positioning properties for rendering only
          _visualStartTime: visualStartTime,
          _visualEndTime: visualEndTime,
          _splitIndex: currentDay // For unique key
        }
        
        allTasksWithSplits.push(splitTask)
      }
    }
  })
  
  console.log(`✅ Final tasks for ${currentDay}:`, allTasksWithSplits.map(t => ({
    title: t.title,
    originalStart: t.startDatetime,
    originalEnd: t.endDatetime,
    visualStart: t._visualStartTime || t.startDatetime,
    visualEnd: t._visualEndTime || t.endDatetime
  })))
  
  return allTasksWithSplits
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyboardShortcuts)
  document.removeEventListener('click', handleClickOutside)
  
  // Stop reminder notification service
  console.log('🔔 Stopping reminder notification service from CalendarView')
  stopReminderNotifications()
})
</script>

<style scoped>
/* Calendar Grid Styles */
.calendar-grid {
  @apply grid grid-cols-7 gap-px bg-gray-200 dark:bg-gray-600 rounded-lg overflow-hidden;
}

.calendar-grid-mobile {
  @apply grid grid-cols-7 gap-px bg-gray-200 dark:bg-gray-600 rounded-lg overflow-hidden;
}

.calendar-day-header {
  @apply bg-gray-50 dark:bg-gray-700 p-2 md:p-3 text-center text-xs md:text-sm font-medium text-gray-700 dark:text-gray-300;
}

.calendar-day {
  @apply bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 flex flex-col;
}

/* Mobile specific adjustments */
@media (max-width: 768px) {
  .calendar-day-header {
    @apply p-2 text-xs;
  }

  .calendar-day {
    @apply text-xs;
  }
}

/* Responsive modal positioning */
.modal-overlay {
  @apply fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4;
}

.modal-content {
  @apply relative bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full overflow-y-auto m-4;
  @apply p-4 md:p-6;
  max-height: 90vh;
}

/* Responsive utility classes */
@media (max-width: 640px) {
  .mobile-hidden {
    @apply hidden;
  }

  .mobile-full {
    @apply w-full;
  }
}

/* Custom task color styles for hover effects */
.task-custom-color {
  @apply transition-all duration-200;
}

.task-custom-color:hover {
  @apply shadow-sm;
  filter: brightness(0.95);
}

/* Force border colors to use inline styles over Tailwind classes */
.task-custom-color {
  border-left-color: var(--task-color) !important;
}

/* For custom colors, also apply background */
.task-custom-color[style*="--task-bg-color"] {
  background-color: var(--task-bg-color) !important;
}
</style>