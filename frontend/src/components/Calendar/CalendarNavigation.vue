<template>
  <nav 
    :class="[
      'flex items-center justify-between p-4 border-b',
      cardClass,
      getTransitionClasses()
    ]"
  >
    <!-- Left section: Navigation controls -->
    <div class="flex items-center space-x-4">
      <!-- Previous/Next navigation -->
      <div class="flex items-center space-x-1">
        <button
          @click="navigatePrevious"
          :class="[
            'p-2 rounded-lg transition-colors hover:bg-gray-100 dark:hover:bg-gray-700',
            textClass
          ]"
          :title="getPreviousTitle()"
          aria-label="Periodo precedente"
        >
          <ChevronLeftIcon class="w-5 h-5" />
        </button>

        <button
          @click="navigateNext"
          :class="[
            'p-2 rounded-lg transition-colors hover:bg-gray-100 dark:hover:bg-gray-700',
            textClass
          ]"
          :title="getNextTitle()"
          aria-label="Periodo successivo"
        >
          <ChevronRightIcon class="w-5 h-5" />
        </button>
      </div>

      <!-- Today button -->
      <button
        @click="goToToday"
        :class="[
          'px-3 py-2 text-sm font-medium rounded-lg transition-colors',
          'hover:bg-blue-50 dark:hover:bg-blue-900/20',
          isToday(currentDate) 
            ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300' 
            : 'text-blue-600 dark:text-blue-400'
        ]"
        title="Vai a oggi (Ctrl+T)"
      >
        Oggi
      </button>

      <!-- Current date display -->
      <div class="flex items-center space-x-2">
        <h1 :class="['text-xl font-semibold', textClass]">
          {{ currentDateTitle }}
        </h1>
        
        <!-- Date picker trigger -->
        <button
          @click="showDatePicker = !showDatePicker"
          :class="[
            'p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-700',
            textClass
          ]"
          title="Seleziona data"
          aria-label="Apri selettore data"
        >
          <CalendarIcon class="w-4 h-4" />
        </button>
      </div>

      <!-- Date picker overlay -->
      <div
        v-if="showDatePicker"
        class="absolute top-16 left-4 z-50"
        @click.stop
      >
        <div :class="['p-4 rounded-lg shadow-lg border', cardClass]">
          <input
            v-model="datePickerValue"
            type="date"
            @change="onDatePickerChange"
            :class="[
              'w-full px-3 py-2 border rounded-md text-sm',
              inputClass
            ]"
          />
          <div class="flex justify-end space-x-2 mt-3">
            <button
              @click="showDatePicker = false"
              :class="[
                'px-3 py-1 text-sm rounded',
                buttonSecondaryClass
              ]"
            >
              Annulla
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Right section: View mode selector and user menu -->
    <div class="flex items-center space-x-4">
      <!-- View mode selector -->
      <div class="flex items-center bg-gray-100 dark:bg-gray-700 rounded-lg p-1">
        <button
          v-for="view in viewModes"
          :key="view.value"
          @click="setViewMode(view.value)"
          :class="[
            'px-3 py-1.5 text-sm font-medium rounded-md transition-colors relative',
            viewMode === view.value
              ? 'bg-white dark:bg-gray-600 text-blue-600 dark:text-blue-400 shadow-sm'
              : 'text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100'
          ]"
          :title="`${view.label} (${view.shortcut})`"
          :aria-pressed="viewMode === view.value"
        >
          <component :is="view.icon" class="w-4 h-4 mr-2 inline" />
          <span class="hidden sm:inline">{{ view.label }}</span>
          <span class="sm:hidden">{{ view.label.charAt(0) }}</span>
          
          <!-- Keyboard shortcut indicator -->
          <span 
            v-if="view.shortcut"
            class="absolute -top-1 -right-1 text-xs bg-gray-500 text-white rounded-full w-4 h-4 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
          >
            {{ view.shortcut }}
          </span>
        </button>
      </div>

      <!-- Search button (mobile only) -->
      <button
        v-if="!isDesktop"
        @click="$emit('toggle-search')"
        :class="[
          'p-2 rounded-lg transition-colors hover:bg-gray-100 dark:hover:bg-gray-700',
          textClass
        ]"
        title="Cerca attività"
        aria-label="Apri ricerca"
      >
        <MagnifyingGlassIcon class="w-5 h-5" />
      </button>

      <!-- User menu -->
      <div class="relative">
        <button
          @click="showUserMenu = !showUserMenu"
          :class="[
            'flex items-center space-x-2 p-2 rounded-lg transition-colors',
            'hover:bg-gray-100 dark:hover:bg-gray-700',
            textClass
          ]"
          aria-label="Menu utente"
          :aria-expanded="showUserMenu"
        >
          <div class="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
            <span class="text-white text-sm font-medium">
              {{ userInitials }}
            </span>
          </div>
          <ChevronDownIcon class="w-4 h-4" />
        </button>

        <!-- User menu dropdown -->
        <Transition
          enter-active-class="transition ease-out duration-100"
          enter-from-class="transform opacity-0 scale-95"
          enter-to-class="transform opacity-100 scale-100"
          leave-active-class="transition ease-in duration-75"
          leave-from-class="transform opacity-100 scale-100"
          leave-to-class="transform opacity-0 scale-95"
        >
          <div
            v-if="showUserMenu"
            :class="[
              'absolute right-0 top-12 w-48 rounded-lg shadow-lg border z-50',
              cardClass
            ]"
            @click.stop
          >
            <div class="p-2">
              <div :class="['px-3 py-2 text-sm', textClass]">
                <div class="font-medium">{{ user?.name || 'Utente' }}</div>
                <div :class="['text-gray-500 dark:text-gray-400']">
                  {{ user?.email || 'user@example.com' }}
                </div>
              </div>
              
              <hr class="my-2 border-gray-200 dark:border-gray-600" />
              
              <button
                @click="toggleTheme"
                :class="[
                  'w-full flex items-center px-3 py-2 text-sm rounded-md transition-colors',
                  'hover:bg-gray-100 dark:hover:bg-gray-700',
                  textClass
                ]"
              >
                <component :is="themeIcon" class="w-4 h-4 mr-3" />
                {{ isDarkMode ? 'Tema chiaro' : 'Tema scuro' }}
              </button>
              
              <button
                @click="openCalendarSettings"
                :class="[
                  'w-full flex items-center px-3 py-2 text-sm rounded-md transition-colors',
                  'hover:bg-gray-100 dark:hover:bg-gray-700',
                  textClass
                ]"
              >
                <CogIcon class="w-4 h-4 mr-3" />
                Impostazioni
              </button>
              
              <button
                @click="exportCalendar"
                :class="[
                  'w-full flex items-center px-3 py-2 text-sm rounded-md transition-colors',
                  'hover:bg-gray-100 dark:hover:bg-gray-700',
                  textClass
                ]"
              >
                <ArrowDownTrayIcon class="w-4 h-4 mr-3" />
                Esporta
              </button>
              
              <hr class="my-2 border-gray-200 dark:border-gray-600" />
              
              <button
                @click="logout"
                :class="[
                  'w-full flex items-center px-3 py-2 text-sm rounded-md transition-colors',
                  'hover:bg-red-50 dark:hover:bg-red-900/20 text-red-600 dark:text-red-400'
                ]"
              >
                <ArrowRightOnRectangleIcon class="w-4 h-4 mr-3" />
                Esci
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </nav>

  <!-- Click outside handler -->
  <div
    v-if="showDatePicker || showUserMenu"
    class="fixed inset-0 z-40"
    @click="closeDropdowns"
  ></div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useCalendar } from '../../composables/useCalendar'
import { useAuth } from '../../composables/useAuth'
import { useTheme } from '../../composables/useTheme'
import { CalendarView, CALENDAR_VIEW_CONFIG } from '../../types/calendar'
import { formatDateForInput, formatMonthYear, isToday as isDateToday } from '../../utils/dateHelpers'

// Icons
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  CalendarIcon,
  CalendarDaysIcon,
  ClockIcon,
  ListBulletIcon,
  MagnifyingGlassIcon,
  ChevronDownIcon,
  CogIcon,
  ArrowDownTrayIcon,
  ArrowRightOnRectangleIcon,
  SunIcon,
  MoonIcon
} from '@heroicons/vue/24/outline'

// Composables
const {
  currentDate,
  viewMode,
  navigatePrevious,
  navigateNext,
  goToToday,
  goToDate,
  setViewMode,
  formatMonthYear: formatCurrentMonthYear,
  exportCalendarData
} = useCalendar()

const { user, logout: authLogout } = useAuth()
const { 
  isDarkMode, 
  themeIcon, 
  toggleTheme, 
  cardClass, 
  textClass, 
  inputClass,
  buttonSecondaryClass,
  getTransitionClasses
} = useTheme()

// Component state
const showDatePicker = ref(false)
const showUserMenu = ref(false)
const datePickerValue = ref('')
const isDesktop = ref(window.innerWidth >= 1024)

// Emits
const emit = defineEmits<{
  'toggle-search': []
  'open-settings': []
}>()

// View modes configuration
const viewModes = computed(() => [
  {
    value: CalendarView.MONTH,
    label: CALENDAR_VIEW_CONFIG[CalendarView.MONTH].label,
    icon: CalendarIcon,
    shortcut: CALENDAR_VIEW_CONFIG[CalendarView.MONTH].shortcut
  },
  {
    value: CalendarView.WEEK,
    label: CALENDAR_VIEW_CONFIG[CalendarView.WEEK].label,
    icon: CalendarDaysIcon,
    shortcut: CALENDAR_VIEW_CONFIG[CalendarView.WEEK].shortcut
  },
  {
    value: CalendarView.DAY,
    label: CALENDAR_VIEW_CONFIG[CalendarView.DAY].label,
    icon: ClockIcon,
    shortcut: CALENDAR_VIEW_CONFIG[CalendarView.DAY].shortcut
  },
  {
    value: CalendarView.AGENDA,
    label: CALENDAR_VIEW_CONFIG[CalendarView.AGENDA].label,
    icon: ListBulletIcon,
    shortcut: CALENDAR_VIEW_CONFIG[CalendarView.AGENDA].shortcut
  }
])

// Computed properties
const currentDateTitle = computed(() => {
  switch (viewMode.value) {
    case CalendarView.MONTH:
      return formatMonthYear(currentDate.value)
    case CalendarView.WEEK:
      // Show week range
      const weekStart = new Date(currentDate.value)
      const weekEnd = new Date(currentDate.value)
      weekStart.setDate(weekStart.getDate() - weekStart.getDay() + 1)
      weekEnd.setDate(weekEnd.getDate() - weekEnd.getDay() + 7)
      
      if (weekStart.getMonth() === weekEnd.getMonth()) {
        return `${weekStart.getDate()} - ${weekEnd.getDate()} ${formatMonthYear(weekStart)}`
      } else {
        return `${weekStart.getDate()} ${formatMonthYear(weekStart).split(' ')[0]} - ${weekEnd.getDate()} ${formatMonthYear(weekEnd)}`
      }
    case CalendarView.DAY:
      return currentDate.value.toLocaleDateString('it-IT', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric'
      })
    case CalendarView.AGENDA:
      return 'Agenda'
    default:
      return formatMonthYear(currentDate.value)
  }
})

const userInitials = computed(() => {
  const name = user.value?.name || 'User'
  return name
    .split(' ')
    .map(n => n.charAt(0))
    .join('')
    .toUpperCase()
    .slice(0, 2)
})

// Methods
const isToday = (date: Date) => isDateToday(date)

const getPreviousTitle = () => {
  switch (viewMode.value) {
    case CalendarView.MONTH:
      return 'Mese precedente'
    case CalendarView.WEEK:
      return 'Settimana precedente'
    case CalendarView.DAY:
      return 'Giorno precedente'
    case CalendarView.AGENDA:
      return 'Periodo precedente'
    default:
      return 'Precedente'
  }
}

const getNextTitle = () => {
  switch (viewMode.value) {
    case CalendarView.MONTH:
      return 'Mese successivo'
    case CalendarView.WEEK:
      return 'Settimana successiva'
    case CalendarView.DAY:
      return 'Giorno successivo'
    case CalendarView.AGENDA:
      return 'Periodo successivo'
    default:
      return 'Successivo'
  }
}

const onDatePickerChange = () => {
  if (datePickerValue.value) {
    const selectedDate = new Date(datePickerValue.value + 'T00:00:00')
    goToDate(selectedDate)
    showDatePicker.value = false
  }
}

const closeDropdowns = () => {
  showDatePicker.value = false
  showUserMenu.value = false
}

const openCalendarSettings = () => {
  showUserMenu.value = false
  emit('open-settings')
}

const exportCalendar = () => {
  const data = exportCalendarData()
  if (data) {
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `calendario-${data.startDate}-${data.endDate}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }
  showUserMenu.value = false
}

const logout = () => {
  showUserMenu.value = false
  authLogout()
}

const handleResize = () => {
  isDesktop.value = window.innerWidth >= 1024
}

// Watch for current date changes to update date picker
watch(currentDate, (newDate) => {
  datePickerValue.value = formatDateForInput(newDate)
}, { immediate: true })

// Keyboard shortcuts handler
const handleKeyboard = (event: KeyboardEvent) => {
  if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement) {
    return
  }

  if (event.ctrlKey || event.metaKey) {
    switch (event.key.toLowerCase()) {
      case 't':
        event.preventDefault()
        goToToday()
        break
      case 'arrowleft':
        event.preventDefault()
        navigatePrevious()
        break
      case 'arrowright':
        event.preventDefault()
        navigateNext()
        break
    }
  } else {
    switch (event.key.toLowerCase()) {
      case 'm':
        setViewMode(CalendarView.MONTH)
        break
      case 'w':
        setViewMode(CalendarView.WEEK)
        break
      case 'd':
        setViewMode(CalendarView.DAY)
        break
      case 'a':
        setViewMode(CalendarView.AGENDA)
        break
      case 'escape':
        closeDropdowns()
        break
    }
  }
}

// Lifecycle
onMounted(() => {
  window.addEventListener('keydown', handleKeyboard)
  window.addEventListener('resize', handleResize)
  datePickerValue.value = formatDateForInput(currentDate.value)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyboard)
  window.removeEventListener('resize', handleResize)
})
</script>