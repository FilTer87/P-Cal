<template>
  <aside :class="[
    'bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 overflow-y-auto transition-transform duration-300 ease-in-out z-50',
    'lg:relative lg:translate-x-0 lg:w-80',
    'fixed inset-y-0 left-0 w-80 transform',
    showMobile ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
  ]">
    <div class="p-4">
      <!-- Mobile Close Button -->
      <div class="lg:hidden flex justify-end mb-4">
        <button @click="handleCloseSidebar" class="p-2 rounded-md text-gray-400 hover:text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700" :aria-label="t('calendar.sidebar.closeMenu')">
          <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- User Profile & Settings -->
      <div class="mb-4 rounded-lg transition-colors" :class="{ 'bg-blue-50/50 dark:bg-blue-900/10': activeSection === 'user' }">
        <!-- User Profile Header (Always Visible, Clickable) -->
        <div class="flex items-center justify-between mb-2 cursor-pointer p-2 rounded-lg" @click="toggleAccordion('user')">
          <div class="flex items-center space-x-3">
            <div
              class="w-10 h-10 bg-blue-600 text-white rounded-full flex items-center justify-center text-sm font-medium">
              {{ userInitials }}
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-gray-900 dark:text-white truncate">
                {{ userFullName }}
              </p>
              <p class="text-xs text-gray-500 dark:text-gray-400">
                {{ user?.username }}
              </p>
            </div>
          </div>

          <div class="flex items-center space-x-2">
            <!-- Dropdown Arrow -->
            <ChevronDownIcon class="h-4 w-4 text-gray-400 transition-transform duration-200"
              :class="{ 'rotate-180': activeSection === 'user' }" />
          </div>
        </div>

        <!-- User Menu Actions (Collapsible) -->
        <div v-show="activeSection === 'user'" class="space-y-1 transition-all duration-200 mb-2 px-2 pb-2">
          <button @click="handleShowProfile"
            class="w-full flex items-center px-3 py-2 text-sm text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
            <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            {{ t('calendar.sidebar.accountManagement') }}
          </button>

          <button @click="handleLogout"
            class="w-full flex items-center px-3 py-2 text-sm text-red-600 dark:text-red-400 rounded-md hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors">
            <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            {{ t('calendar.sidebar.logout') }}
          </button>
        </div>
      </div>

      <!-- Statistics Section (Collapsible) -->
      <!-- <div class="mb-6">
        <div class="flex items-center justify-between mb-3 cursor-pointer" @click="toggleStatistics">
          <h3 class="text-sm font-medium text-gray-900 dark:text-white">
            {{ t('calendar.sidebar.statistics') }}
          </h3>
          <ChevronDownIcon class="h-4 w-4 text-gray-400 transition-transform duration-200" 
            :class="{ 'rotate-180': showStatistics }" />
        </div>
        
        <div v-show="showStatistics" class="transition-all duration-200">
          <div class="grid grid-cols-2 gap-2 sm:gap-3">
            <div class="bg-blue-50 dark:bg-blue-900/20 p-2 sm:p-3 rounded-md">
              <div class="text-lg sm:text-2xl font-bold text-blue-600 dark:text-blue-400">
                {{ taskStats?.pending ?? 0 }}
              </div>
              <div class="text-xs text-blue-600 dark:text-blue-400">
                {{ t('calendar.sidebar.statsPending') }}
              </div>
            </div>
            <div class="bg-green-50 dark:bg-green-900/20 p-2 sm:p-3 rounded-md">
              <div class="text-lg sm:text-2xl font-bold text-green-600 dark:text-green-400">
                {{ taskStats?.completed ?? 0 }}
              </div>
              <div class="text-xs text-green-600 dark:text-green-400">
                {{ t('calendar.sidebar.statsCompleted') }}
              </div>
            </div>
            <div class="bg-yellow-50 dark:bg-yellow-900/20 p-2 sm:p-3 rounded-md">
              <div class="text-lg sm:text-2xl font-bold text-yellow-600 dark:text-yellow-400">
                {{ taskStats?.today ?? 0 }}
              </div>
              <div class="text-xs text-yellow-600 dark:text-yellow-400">
                {{ t('calendar.sidebar.statsToday') }}
              </div>
            </div>
            <div class="bg-red-50 dark:bg-red-900/20 p-2 sm:p-3 rounded-md">
              <div class="text-lg sm:text-2xl font-bold text-red-600 dark:text-red-400">
                {{ taskStats?.overdue ?? 0 }}
              </div>
              <div class="text-xs text-red-600 dark:text-red-400">
                {{ t('calendar.sidebar.statsOverdue') }}
              </div>
            </div>
          </div>
        </div>
      </div> -->

      <!-- Today's Tasks -->
      <div class="mb-4 rounded-lg transition-colors" :class="{ 'bg-blue-50/50 dark:bg-blue-900/10': activeSection === 'tasks' }" v-if="todayTasks && todayTasks.length > 0 && !isDayView">
        <div class="flex items-center justify-between mb-2 cursor-pointer p-2 rounded-lg" @click="toggleAccordion('tasks')">
          <h3 class="text-sm font-medium text-gray-900 dark:text-white">
            {{ t('calendar.sidebar.todayTasks') }}
          </h3>
          <ChevronDownIcon class="h-4 w-4 text-gray-400 transition-transform duration-200"
            :class="{ 'rotate-180': activeSection === 'tasks' }" />
        </div>
        <div v-show="activeSection === 'tasks'" class="space-y-2 transition-all duration-200 px-2 pb-2">
          <div v-for="task in todayTasks.slice(0, 5)" :key="getTaskKey(task)" @click="handleTaskClick(task)"
            class="p-3 rounded-md cursor-pointer transition-colors bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600">
            <div class="flex items-center space-x-2">
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium truncate text-gray-900 dark:text-white"
                  :class="{ 'line-through opacity-60': isPastTask(task) }">
                  {{ task.title }}
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-400 truncate">
                  {{ task.endDatetime ? formatTime(task.endDatetime) : t('calendar.sidebar.noTime') }}
                </p>
              </div>
              <div class="w-3 h-3 rounded-full" :style="{ backgroundColor: task.color || '#3788d8' }"></div>
            </div>
          </div>
          <div v-if="todayTasks && todayTasks.length > 5" class="mt-2 text-center">
            <button @click="showTodayView" class="text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300">
              {{ t('calendar.sidebar.showMoreActivities', { count: todayTasks.length - 5 }) }}
            </button>
          </div>
        </div>
      </div>

      <!-- Upcoming Reminders -->
      <div class="mb-4 rounded-lg transition-colors" :class="{ 'bg-blue-50/50 dark:bg-blue-900/10': activeSection === 'reminders' }" v-if="upcomingReminders && upcomingReminders.length > 0">
        <div class="flex items-center justify-between mb-2 cursor-pointer p-2 rounded-lg" @click="toggleAccordion('reminders')">
          <h3 class="text-sm font-medium text-gray-900 dark:text-white">
            {{ t('calendar.sidebar.upcomingReminders') }}
          </h3>
          <ChevronDownIcon class="h-4 w-4 text-gray-400 transition-transform duration-200"
            :class="{ 'rotate-180': activeSection === 'reminders' }" />
        </div>
        <div v-show="activeSection === 'reminders'" class="space-y-2 transition-all duration-200 px-2 pb-2">
          <div v-for="reminder in upcomingReminders.slice(0, 3)" :key="reminder.id"
            class="p-3 bg-yellow-50 dark:bg-yellow-900/20 rounded-md">
            <p class="text-sm font-medium text-yellow-800 dark:text-yellow-200">
              {{ getTaskTitle(reminder.taskId) }}
            </p>
            <p class="text-xs text-yellow-600 dark:text-yellow-400">
              {{ formatReminderTimeShort(reminder) }}
            </p>
          </div>
        </div>
      </div>

      <!-- New Task Button -->
      <button @click="handleNewTask" class="w-full btn btn-primary mb-6" :title="t('calendar.sidebar.newActivityShortcut')">
        <PlusIcon class="h-4 w-4 mr-2" />
        {{ t('calendar.sidebar.newActivity') }}
      </button>
      
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import {
  PlusIcon,
  ChevronDownIcon
} from '@heroicons/vue/24/outline'
import { formatTime } from '../../utils/dateHelpers'
import type { Task, Reminder } from '../../types/task'
import { getTaskKey } from '@/utils/recurrence'

// i18n
const { t } = useI18n()

// Props
interface Props {
  showMobile?: boolean
  user?: any
  todayTasks?: Task[]
  upcomingReminders?: Reminder[]
  currentViewMode?: string
}

const props = withDefaults(defineProps<Props>(), {
  showMobile: false,
  currentViewMode: 'month'
})

// Composables
const router = useRouter()
const { logout } = useAuth()

// Emits
const emit = defineEmits<{
  taskClick: [task: Task]
  newTask: []
  switchToDayView: []
  closeSidebar: []
}>()

// State
const activeSection = ref<'user' | 'tasks' | 'reminders' | null>('tasks')

// Computed
const userInitials = computed(() => {
  if (!props.user?.fullName) return 'U'
  return props.user.fullName
    .split(' ')
    .map((name: string) => name.charAt(0).toUpperCase())
    .slice(0, 2)
    .join('')
})

const userFullName = computed(() => {
  return props.user?.fullName || 'Utente'
})

const isDayView = computed(() => {
  return props.currentViewMode === 'day'
})

// Methods
const toggleAccordion = (section: 'user' | 'tasks' | 'reminders') => {
  if (activeSection.value === section) {
    activeSection.value = null
  } else {
    activeSection.value = section
  }
}

const handleShowProfile = async () => {
  await router.push('/profile')
}

const handleLogout = async () => {
  try {
    await logout()
  } catch (error) {
    console.error('Errore durante il logout:', error)
  }
}

const handleTaskClick = (task: Task) => {
  emit('taskClick', task)
}


const handleNewTask = () => {
  emit('newTask')
}

const getTaskTitle = (taskId: number) => {
  // Try to find the task in the reminder data first (it includes taskTitle)
  const reminder = props.upcomingReminders?.find(r => r.taskId === taskId)
  if (reminder?.taskTitle) return reminder.taskTitle

  // Fallback to finding in todayTasks
  const task = props.todayTasks?.find(t => t.id === taskId)
  return task?.title || t('calendar.sidebar.deletedActivity')
}

const formatReminderTimeShort = (reminder: Reminder) => {
  if (!reminder.reminderTime) return 'Data non valida'
  return formatTime(reminder.reminderTime)
}

const isPastTask = (task: Task): boolean => {
  if (!task.endDatetime) return false
  return new Date(task.endDatetime) < new Date()
}

const showTodayView = () => {
  emit('switchToDayView')
  // Chiudi la sidebar su mobile
  if (props.showMobile) {
    emit('closeSidebar')
  }
}

const handleCloseSidebar = () => {
  emit('closeSidebar')
}
</script>

<style scoped>
.btn {
  @apply inline-flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors;
}

.btn-primary {
  @apply text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-500 dark:bg-blue-500 dark:hover:bg-blue-600;
}
</style>