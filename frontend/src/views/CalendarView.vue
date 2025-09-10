<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- Header -->
    <header class="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
      <div class="px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <!-- Logo and Title -->
          <div class="flex items-center">
            <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
              PrivateCal
            </h1>
            <span class="ml-2 text-sm text-gray-500 dark:text-gray-400">
              {{ formatDisplayDate(currentDate) }}
            </span>
          </div>

          <!-- Navigation Controls -->
          <div class="flex items-center space-x-4">
            <!-- View Mode Selector -->
            <div class="flex bg-gray-100 dark:bg-gray-700 rounded-md p-1">
              <button
                v-for="view in CALENDAR_VIEWS"
                :key="view.value"
                @click="setViewMode(view.value)"
                class="px-3 py-1 text-sm font-medium rounded transition-colors"
                :class="{
                  'bg-white dark:bg-gray-600 text-gray-900 dark:text-white shadow-sm': viewMode === view.value,
                  'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white': viewMode !== view.value
                }"
                :title="`${view.label} (${view.shortcut})`"
              >
                {{ view.label }}
              </button>
            </div>

            <!-- Navigation Buttons -->
            <div class="flex items-center space-x-1">
              <button
                @click="navigatePrevious"
                class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md"
                title="Periodo precedente (Ctrl + ←)"
              >
                <ChevronLeftIcon class="h-5 w-5" />
              </button>
              
              <button
                @click="goToToday"
                class="px-3 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md"
                title="Vai a oggi (Ctrl + T)"
              >
                Oggi
              </button>
              
              <button
                @click="navigateNext"
                class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md"
                title="Periodo successivo (Ctrl + →)"
              >
                <ChevronRightIcon class="h-5 w-5" />
              </button>
            </div>

            <!-- User Menu -->
            <div class="flex items-center space-x-4">
              <!-- Theme Toggle -->
              <button
                @click="toggleTheme"
                class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md"
                :title="`Cambia tema: ${themeName}`"
              >
                <SunIcon v-if="isDarkMode" class="h-5 w-5" />
                <MoonIcon v-else class="h-5 w-5" />
              </button>

              <!-- User Profile -->
              <div class="relative" ref="userMenuRef">
                <button
                  @click="showUserMenu = !showUserMenu"
                  class="flex items-center space-x-2 p-2 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700"
                >
                  <div class="w-8 h-8 bg-blue-600 text-white rounded-full flex items-center justify-center text-sm font-medium">
                    {{ userInitials }}
                  </div>
                  <ChevronDownIcon class="h-4 w-4 text-gray-400" />
                </button>

                <!-- User Dropdown -->
                <div
                  v-if="showUserMenu"
                  class="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 rounded-md shadow-lg ring-1 ring-black ring-opacity-5 z-50"
                >
                  <div class="py-1">
                    <div class="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-600">
                      {{ userFullName }}
                    </div>
                    <a
                      href="#"
                      class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                      @click.prevent="showProfile"
                    >
                      Profilo
                    </a>
                    <a
                      href="#"
                      class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                      @click.prevent="showSettings"
                    >
                      Impostazioni
                    </a>
                    <div class="border-t border-gray-200 dark:border-gray-600"></div>
                    <a
                      href="#"
                      class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                      @click.prevent="handleLogout"
                    >
                      Disconnetti
                    </a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <div class="flex h-[calc(100vh-4rem)]">
      <!-- Sidebar -->
      <aside class="w-80 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 overflow-y-auto">
        <div class="p-4">
          <!-- New Task Button -->
          <button
            @click="openCreateTaskModal()"
            class="w-full btn btn-primary mb-6"
            title="Nuova attività (Ctrl + N)"
          >
            <PlusIcon class="h-4 w-4 mr-2" />
            Nuova Attività
          </button>

          <!-- Quick Stats -->
          <div class="mb-6">
            <h3 class="text-sm font-medium text-gray-900 dark:text-white mb-3">
              Statistiche
            </h3>
            <div class="grid grid-cols-2 gap-3">
              <div class="bg-blue-50 dark:bg-blue-900/20 p-3 rounded-md">
                <div class="text-2xl font-bold text-blue-600 dark:text-blue-400">
                  {{ taskStats?.pending ?? 0 }}
                </div>
                <div class="text-xs text-blue-600 dark:text-blue-400">
                  In corso
                </div>
              </div>
              <div class="bg-green-50 dark:bg-green-900/20 p-3 rounded-md">
                <div class="text-2xl font-bold text-green-600 dark:text-green-400">
                  {{ taskStats?.completed ?? 0 }}
                </div>
                <div class="text-xs text-green-600 dark:text-green-400">
                  Completate
                </div>
              </div>
              <div class="bg-yellow-50 dark:bg-yellow-900/20 p-3 rounded-md">
                <div class="text-2xl font-bold text-yellow-600 dark:text-yellow-400">
                  {{ taskStats?.today ?? 0 }}
                </div>
                <div class="text-xs text-yellow-600 dark:text-yellow-400">
                  Oggi
                </div>
              </div>
              <div class="bg-red-50 dark:bg-red-900/20 p-3 rounded-md">
                <div class="text-2xl font-bold text-red-600 dark:text-red-400">
                  {{ taskStats?.overdue ?? 0 }}
                </div>
                <div class="text-xs text-red-600 dark:text-red-400">
                  In ritardo
                </div>
              </div>
            </div>
          </div>

          <!-- Today's Tasks -->
          <div class="mb-6" v-if="todayTasks && todayTasks.length > 0">
            <h3 class="text-sm font-medium text-gray-900 dark:text-white mb-3">
              Attività di oggi
            </h3>
            <div class="space-y-2">
              <div
                v-for="task in todayTasks.slice(0, 5)"
                :key="task.id"
                @click="openTaskModal(task)"
                class="p-3 rounded-md cursor-pointer transition-colors"
                :class="[
                  task.completed 
                    ? 'bg-green-50 dark:bg-green-900/20 hover:bg-green-100 dark:hover:bg-green-900/30' 
                    : 'bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600'
                ]"
              >
                <div class="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    :checked="task.completed"
                    @click.stop="toggleTaskCompletion(task.id)"
                    class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <div class="flex-1 min-w-0">
                    <p
                      class="text-sm font-medium truncate"
                      :class="{
                        'text-gray-900 dark:text-white': !task.completed,
                        'text-gray-500 dark:text-gray-400 line-through': task.completed
                      }"
                    >
                      {{ task.title }}
                    </p>
                    <p class="text-xs text-gray-500 dark:text-gray-400 truncate">
                      {{ task.dueDate ? formatTime(task.dueDate) : 'Nessuna ora' }}
                    </p>
                  </div>
                  <div
                    class="w-2 h-2 rounded-full"
                    :class="getPriorityColor(task.priority)"
                  ></div>
                </div>
              </div>
            </div>
            <div v-if="todayTasks && todayTasks.length > 5" class="mt-2 text-center">
              <button class="text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300">
                Mostra altre {{ todayTasks.length - 5 }} attività
              </button>
            </div>
          </div>

          <!-- Upcoming Reminders -->
          <div v-if="upcomingReminders && upcomingReminders.length > 0">
            <h3 class="text-sm font-medium text-gray-900 dark:text-white mb-3">
              Promemoria imminenti
            </h3>
            <div class="space-y-2">
              <div
                v-for="reminder in upcomingReminders.slice(0, 3)"
                :key="reminder.id"
                class="p-3 bg-yellow-50 dark:bg-yellow-900/20 rounded-md"
              >
                <p class="text-sm font-medium text-yellow-800 dark:text-yellow-200">
                  {{ getTaskById(reminder.taskId)?.title || 'Attività eliminata' }}
                </p>
                <p class="text-xs text-yellow-600 dark:text-yellow-400">
                  {{ formatReminderTimeShort(reminder) }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- Calendar Area -->
      <main class="flex-1 overflow-hidden">
        <!-- Calendar Header -->
        <div class="p-4 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-lg font-medium text-gray-900 dark:text-white">
            {{ currentMonthName }}
          </h2>
        </div>

        <!-- Calendar Content -->
        <div class="p-4 h-full overflow-y-auto">
          <!-- Month View -->
          <div v-if="isMonthView" class="calendar-grid">
            <!-- Week Days Header -->
            <div
              v-for="day in LOCALE_STRINGS.weekdaysShort"
              :key="day"
              class="calendar-day-header"
            >
              {{ day }}
            </div>

            <!-- Calendar Days -->
            <div
              v-for="day in calendarDays"
              :key="day.date.getTime()"
              @click="selectDate(day.date)"
              @dblclick="openCreateTaskModal(day.date)"
              class="calendar-day min-h-32 cursor-pointer transition-colors"
              :class="{
                'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-400': day.isSelected,
                'bg-yellow-50 dark:bg-yellow-900/20': day.isToday && !day.isSelected,
                'text-gray-400 dark:text-gray-600': !day.isCurrentMonth,
                'hover:bg-gray-100 dark:hover:bg-gray-700': !day.isSelected && !day.isToday
              }"
            >
              <!-- Day Number -->
              <div class="flex justify-between items-center mb-2">
                <span
                  class="text-sm font-medium"
                  :class="{
                    'text-blue-600 dark:text-blue-400': day.isSelected,
                    'text-yellow-700 dark:text-yellow-300': day.isToday && !day.isSelected,
                    'text-gray-900 dark:text-white': day.isCurrentMonth && !day.isToday && !day.isSelected,
                    'text-gray-400 dark:text-gray-600': !day.isCurrentMonth
                  }"
                >
                  {{ day.dayOfMonth }}
                </span>
                <div
                  v-if="day.tasks && day.tasks.length > 0"
                  class="text-xs text-gray-500 dark:text-gray-400"
                >
                  {{ day.tasks.length }}
                </div>
              </div>

              <!-- Tasks -->
              <div class="space-y-1">
                <div
                  v-for="task in (day.tasks || []).slice(0, 3)"
                  :key="task.id"
                  @click.stop="openTaskModal(getTaskById(task.id)!)"
                  class="text-xs p-1 rounded truncate cursor-pointer transition-colors"
                  :class="getTaskDisplayClasses(task)"
                >
                  {{ task.completed ? '✓' : '' }} {{ task.title }}
                </div>
                <div
                  v-if="day.tasks && day.tasks.length > 3"
                  class="text-xs text-gray-500 dark:text-gray-400 p-1"
                >
                  +{{ day.tasks.length - 3 }} altro/i
                </div>
              </div>
            </div>
          </div>

          <!-- Week View -->
          <div v-else-if="isWeekView" class="h-full flex flex-col">
            <!-- Week Header -->
            <div class="grid grid-cols-8 gap-px bg-gray-200 dark:bg-gray-600 mb-4">
              <div class="bg-white dark:bg-gray-800 p-2 text-sm font-medium text-center">
                Ora
              </div>
              <div
                v-for="day in getWeekDays(currentDate)"
                :key="day.getTime()"
                class="bg-white dark:bg-gray-800 p-2 text-sm font-medium text-center"
                :class="{
                  'bg-blue-50 dark:bg-blue-900/20': isToday(day),
                  'text-blue-600 dark:text-blue-400': isToday(day)
                }"
              >
                <div>{{ getDayName(day, true) }}</div>
                <div class="text-lg font-bold">{{ day.getDate() }}</div>
              </div>
            </div>

            <!-- Week Grid (simplified) -->
            <div class="flex-1 bg-gray-100 dark:bg-gray-700 rounded-lg p-4">
              <p class="text-center text-gray-500 dark:text-gray-400">
                Vista settimana - In sviluppo
              </p>
            </div>
          </div>

          <!-- Day View -->
          <div v-else-if="isDayView" class="h-full">
            <div class="bg-white dark:bg-gray-800 rounded-lg p-6">
              <h3 class="text-xl font-medium text-gray-900 dark:text-white mb-4">
                {{ formatDate(currentDate) }}
              </h3>
              
              <div class="space-y-3">
                <div
                  v-for="task in getTasksForDate(currentDate)"
                  :key="task.id"
                  @click="openTaskModal(task)"
                  class="p-4 rounded-lg cursor-pointer transition-colors"
                  :class="getTaskDisplayClasses(task, true)"
                >
                  <div class="flex items-center space-x-3">
                    <input
                      type="checkbox"
                      :checked="task.completed"
                      @click.stop="toggleTaskCompletion(task.id)"
                      class="h-5 w-5 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    />
                    <div class="flex-1">
                      <h4 class="font-medium text-gray-900 dark:text-white">
                        {{ task.title }}
                      </h4>
                      <p v-if="task.description" class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                        {{ task.description }}
                      </p>
                      <div class="flex items-center space-x-4 mt-2 text-sm text-gray-500 dark:text-gray-400">
                        <span>{{ formatTaskPriority(task.priority) }}</span>
                        <span v-if="task.dueDate">{{ formatTime(task.dueDate) }}</span>
                        <span v-if="task.reminders && task.reminders.length > 0">
                          {{ task.reminders.length }} promemoria
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="getTasksForDate(currentDate).length === 0" class="text-center py-8">
                  <p class="text-gray-500 dark:text-gray-400">
                    Nessuna attività programmata per oggi
                  </p>
                  <button
                    @click="openCreateTaskModal(currentDate)"
                    class="mt-2 text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300"
                  >
                    Crea la prima attività
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Agenda View -->
          <div v-else-if="isAgendaView" class="space-y-4">
            <div
              v-for="(dayTasks, date) in tasksByDateInRange"
              :key="date"
              class="bg-white dark:bg-gray-800 rounded-lg p-4"
            >
              <h3 class="font-medium text-gray-900 dark:text-white mb-3">
                {{ getDateDescription(new Date(date)) }}
              </h3>
              <div class="space-y-2">
                <div
                  v-for="task in dayTasks"
                  :key="task.id"
                  @click="openTaskModal(task)"
                  class="p-3 rounded-lg cursor-pointer transition-colors"
                  :class="getTaskDisplayClasses(task)"
                >
                  <div class="flex items-center space-x-3">
                    <input
                      type="checkbox"
                      :checked="task.completed"
                      @click.stop="toggleTaskCompletion(task.id)"
                      class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    />
                    <div class="flex-1">
                      <p class="font-medium text-gray-900 dark:text-white">
                        {{ task.title }}
                      </p>
                      <div class="flex items-center space-x-2 text-xs text-gray-500 dark:text-gray-400">
                        <span>{{ formatTaskPriority(task.priority) }}</span>
                        <span v-if="task.dueDate">{{ formatTime(task.dueDate) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="Object.keys(tasksByDateInRange).length === 0" class="text-center py-8">
              <p class="text-gray-500 dark:text-gray-400">
                Nessuna attività in questo periodo
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- Task Modal (placeholder) -->
    <div v-if="showTaskModal" class="modal-overlay" @click="closeTaskModal">
      <div class="modal-content" @click.stop>
        <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">
          Dettagli Attività
        </h3>
        <div v-if="selectedTask">
          <p><strong>Titolo:</strong> {{ selectedTask.title }}</p>
          <p v-if="selectedTask.description"><strong>Descrizione:</strong> {{ selectedTask.description }}</p>
          <p><strong>Priorità:</strong> {{ formatTaskPriority(selectedTask.priority) }}</p>
          <p v-if="selectedTask.dueDate"><strong>Scadenza:</strong> {{ formatDateTime(selectedTask.dueDate) }}</p>
          <p><strong>Stato:</strong> {{ selectedTask.completed ? 'Completata' : 'In corso' }}</p>
        </div>
        <div class="mt-6 flex justify-end space-x-2">
          <button @click="closeTaskModal" class="btn btn-secondary">
            Chiudi
          </button>
        </div>
      </div>
    </div>

    <!-- Create Task Modal (placeholder) -->
    <div v-if="showCreateTaskModal" class="modal-overlay" @click="closeCreateTaskModal">
      <div class="modal-content" @click.stop>
        <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">
          Nuova Attività
        </h3>
        <p class="text-gray-600 dark:text-gray-400">
          Form di creazione attività - Da implementare
        </p>
        <div class="mt-6 flex justify-end space-x-2">
          <button @click="closeCreateTaskModal" class="btn btn-secondary">
            Annulla
          </button>
          <button class="btn btn-primary">
            Crea
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  ChevronDownIcon,
  PlusIcon,
  SunIcon,
  MoonIcon
} from '@heroicons/vue/24/outline'

// Composables
import { useAuth } from '../composables/useAuth'
import { useCalendar } from '../composables/useCalendar'
import { useTasks } from '../composables/useTasks'
import { useReminders } from '../composables/useReminders'
import { useTheme } from '../composables/useTheme'
import { useNotifications } from '../composables/useNotifications'

// Utilities
import { 
  formatDate, 
  formatDateTime, 
  formatTime, 
  getDayName,
  getDateDescription,
  isToday as isDateToday
} from '../utils/dateHelpers'
import { formatTaskPriority } from '../utils/formatters'
import { CALENDAR_VIEWS, LOCALE_STRINGS } from '../utils/constants'

// Composable instances
const auth = useAuth()
const calendar = useCalendar()
const tasks = useTasks()
const reminders = useReminders()
const theme = useTheme()
const { showError, showConfirmation } = useNotifications()

// Reactive state
const showUserMenu = ref(false)
const userMenuRef = ref<HTMLElement>()

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
  getTaskById,
  toggleTaskCompletion
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

// Additional computed properties
const tasksByDateInRange = computed(() => {
  const range = calendar.viewDateRange.value
  if (!range) return {}

  const result: Record<string, any[]> = {}
  const allTasks = tasks.allTasks.value || []

  allTasks.forEach(task => {
    if (task.dueDate) {
      const taskDate = new Date(task.dueDate)
      if (taskDate >= range.start && taskDate <= range.end) {
        const dateKey = formatDate(taskDate, 'yyyy-MM-dd')
        if (!result[dateKey]) result[dateKey] = []
        result[dateKey].push(task)
      }
    }
  })

  return result
})

// Methods
const isToday = (date: Date) => isDateToday(date)

const getPriorityColor = (priority: string) => {
  const colors = {
    LOW: 'bg-green-500',
    MEDIUM: 'bg-yellow-500',
    HIGH: 'bg-orange-500',
    URGENT: 'bg-red-500'
  }
  return colors[priority as keyof typeof colors] || 'bg-gray-500'
}

const getTaskDisplayClasses = (task: any, detailed = false) => {
  const baseClasses = detailed 
    ? 'border-l-4' 
    : ''
  
  if (task.completed) {
    return `${baseClasses} bg-green-50 dark:bg-green-900/20 border-green-500 hover:bg-green-100 dark:hover:bg-green-900/30`
  }
  
  if (task.isOverdue) {
    return `${baseClasses} bg-red-50 dark:bg-red-900/20 border-red-500 hover:bg-red-100 dark:hover:bg-red-900/30`
  }
  
  const priorityClasses = {
    URGENT: `${baseClasses} bg-red-50 dark:bg-red-900/20 border-red-500 hover:bg-red-100 dark:hover:bg-red-900/30`,
    HIGH: `${baseClasses} bg-orange-50 dark:bg-orange-900/20 border-orange-500 hover:bg-orange-100 dark:hover:bg-orange-900/30`,
    MEDIUM: `${baseClasses} bg-yellow-50 dark:bg-yellow-900/20 border-yellow-500 hover:bg-yellow-100 dark:hover:bg-yellow-900/30`,
    LOW: `${baseClasses} bg-green-50 dark:bg-green-900/20 border-green-500 hover:bg-green-100 dark:hover:bg-green-900/30`
  }
  
  return priorityClasses[task.priority as keyof typeof priorityClasses] || 
         `${baseClasses} bg-blue-50 dark:bg-blue-900/20 border-blue-500 hover:bg-blue-100 dark:hover:bg-blue-900/30`
}

const showProfile = () => {
  showError('Funzionalità profilo non ancora implementata')
  showUserMenu.value = false
}

const showSettings = () => {
  showError('Funzionalità impostazioni non ancora implementata')
  showUserMenu.value = false
}

const handleLogout = () => {
  showUserMenu.value = false
  showConfirmation(
    'Sei sicuro di voler uscire?',
    async () => {
      await logout()
    }
  )
}

const handleKeyboardShortcuts = (event: KeyboardEvent) => {
  calendar.handleKeyboardNavigation(event)
  
  // Additional shortcuts
  if (event.ctrlKey && event.key === 'n') {
    event.preventDefault()
    openCreateTaskModal()
  }
}

const handleClickOutside = (event: Event) => {
  if (userMenuRef.value && !userMenuRef.value.contains(event.target as Node)) {
    showUserMenu.value = false
  }
}

// Lifecycle
onMounted(async () => {
  // Initialize auth and require authentication
  await auth.requireAuth()
  
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
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyboardShortcuts)
  document.removeEventListener('click', handleClickOutside)
})
</script>