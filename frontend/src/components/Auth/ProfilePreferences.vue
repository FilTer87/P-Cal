<template>
  <div class="space-y-8">
    <!-- Theme Preference -->
    <div>
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        {{ t('profile.themeLabel') }}
      </h3>
      <div class="flex items-center justify-between">
        <div>
          <h4 class="text-sm font-medium text-gray-900 dark:text-white">
            {{ t('profile.themeMode') }}
          </h4>
          <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
            {{ t('profile.themeModeDescription') }}
          </p>
        </div>
        <div class="flex items-center space-x-2">
          <button
            v-for="themeOption in themeOptions"
            :key="themeOption.value"
            type="button"
            @click="handleThemeChange(themeOption.value)"
            :disabled="isLoading"
            :class="[
              'p-2 rounded transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500',
              preferences.theme === themeOption.value
                ? 'bg-blue-100 text-blue-600 dark:bg-blue-900 dark:text-blue-400'
                : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
            ]"
            :title="themeOption.label"
          >
            <component :is="themeOption.icon" class="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>

    <!-- App Preferences -->
    <div>
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        {{ t('profile.appPreferences') }}
      </h3>
      <div class="space-y-6">
        <!-- Time Format -->
        <div class="flex items-center justify-between">
          <div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-white">
              {{ t('profile.timeFormatLabel') }}
            </h4>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('profile.timeFormatDescription') }}
            </p>
          </div>
          <select
            v-model="preferences.timeFormat"
            @change="handleTimeFormatChange"
            :disabled="isLoading"
            class="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="24h">{{ t('profile.timeFormat24h') }}</option>
            <option value="12h">{{ t('profile.timeFormat12h') }}</option>
          </select>
        </div>

        <!-- Default Calendar View -->
        <div class="flex items-center justify-between">
          <div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-white">
              {{ t('profile.defaultCalendarView') }}
            </h4>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ t('profile.defaultCalendarViewDescription') }}
            </p>
          </div>
          <select
            v-model="preferences.calendarView"
            @change="handleCalendarViewChange"
            :disabled="isLoading"
            class="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="week">{{ t('profile.calendarViewWeek') }}</option>
            <option value="month">{{ t('profile.calendarViewMonth') }}</option>
            <option value="day">{{ t('profile.calendarViewDay') }}</option>
            <option value="agenda">{{ t('profile.calendarViewAgenda') }}</option>
          </select>
        </div>

        <!-- Timezone -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            {{ t('profile.timezoneLabel') }}
          </label>
          <select
            v-model="preferences.timezone"
            @change="handleTimezoneChange"
            :disabled="isLoading"
            class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 text-gray-900 dark:text-white transition-colors"
          >
            <optgroup label="Italia">
              <option value="Europe/Rome">Roma (UTC+1)</option>
            </optgroup>
            <optgroup label="Europa">
              <option value="Europe/London">Londra (UTC+0)</option>
              <option value="Europe/Berlin">Berlino (UTC+1)</option>
              <option value="Europe/Paris">Parigi (UTC+1)</option>
              <option value="Europe/Madrid">Madrid (UTC+1)</option>
              <option value="Europe/Amsterdam">Amsterdam (UTC+1)</option>
            </optgroup>
            <optgroup label="Americhe">
              <option value="America/New_York">New York (UTC-5)</option>
              <option value="America/Los_Angeles">Los Angeles (UTC-8)</option>
              <option value="America/Chicago">Chicago (UTC-6)</option>
            </optgroup>
            <optgroup label="Asia">
              <option value="Asia/Tokyo">Tokyo (UTC+9)</option>
              <option value="Asia/Shanghai">Shanghai (UTC+8)</option>
              <option value="Asia/Dubai">Dubai (UTC+4)</option>
            </optgroup>
          </select>
        </div>

        <!-- Week Start Day -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            {{ t('profile.weekStartLabel') }}
          </label>
          <select
            v-model="preferences.weekStartDay"
            @change="handleWeekStartDayChange"
            :disabled="isLoading"
            class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 text-gray-900 dark:text-white transition-colors"
          >
            <option :value="1">{{ t('profile.weekStartMonday') }}</option>
            <option :value="0">{{ t('profile.weekStartSunday') }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Notification Preferences -->
    <div>
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        {{ t('profile.notificationPreferences') }}
      </h3>
      <div class="space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <label for="email-notifications" class="text-sm font-medium text-gray-700 dark:text-gray-300">
              {{ t('profile.emailNotificationsLabel') }}
            </label>
            <p class="text-sm text-gray-500 dark:text-gray-400">
              {{ t('profile.emailNotificationsDescription') }}
            </p>
          </div>
          <input
            id="email-notifications"
            v-model="preferences.emailNotifications"
            @change="handleEmailNotificationsChange"
            type="checkbox"
            :disabled="isLoading"
            class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
          />
        </div>

        <div class="flex items-center justify-between">
          <div>
            <label for="reminder-notifications" class="text-sm font-medium text-gray-700 dark:text-gray-300">
              {{ t('profile.reminderNotificationsLabel') }}
            </label>
            <p class="text-sm text-gray-500 dark:text-gray-400">
              {{ t('profile.reminderNotificationsDescription') }}
            </p>
          </div>
          <input
            id="reminder-notifications"
            v-model="preferences.reminderNotifications"
            @change="handleReminderNotificationsChange"
            type="checkbox"
            :disabled="isLoading"
            class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
          />
        </div>
      </div>
    </div>

    <!-- Advanced Notifications Section -->
    <div>
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        {{ t('profile.advancedNotifications') }}
      </h3>
      <div class="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
        <NotificationSettings />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { SunIcon, MoonIcon, ComputerDesktopIcon } from '@heroicons/vue/24/outline'
import NotificationSettings from '@/components/Reminder/NotificationSettings.vue'

// Composables
const { t } = useI18n()

// Props
interface Props {
  isLoading?: boolean
  theme: 'light' | 'dark' | 'system'
  timeFormat: '12h' | '24h'
  calendarView: 'month' | 'week' | 'day' | 'agenda'
  timezone: string
  weekStartDay: 0 | 1
  emailNotifications: boolean
  reminderNotifications: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isLoading: false,
  theme: 'system',
  timeFormat: '24h',
  calendarView: 'week',
  timezone: 'Europe/Rome',
  weekStartDay: 1,
  emailNotifications: true,
  reminderNotifications: true
})

// Emits
const emit = defineEmits<{
  updateTheme: [theme: 'light' | 'dark' | 'system']
  updateTimeFormat: [timeFormat: '12h' | '24h']
  updateCalendarView: [calendarView: 'month' | 'week' | 'day' | 'agenda']
  updateTimezone: [timezone: string]
  updateWeekStartDay: [weekStartDay: 0 | 1]
  updateEmailNotifications: [enabled: boolean]
  updateReminderNotifications: [enabled: boolean]
}>()

// Local preferences (reactive copy of props for v-model binding)
const preferences = reactive({
  theme: props.theme,
  timeFormat: props.timeFormat,
  calendarView: props.calendarView,
  timezone: props.timezone,
  weekStartDay: props.weekStartDay,
  emailNotifications: props.emailNotifications,
  reminderNotifications: props.reminderNotifications
})

// Watch props to update local preferences when they change
watch(() => [props.theme, props.timeFormat, props.calendarView, props.timezone, props.weekStartDay, props.emailNotifications, props.reminderNotifications], () => {
  preferences.theme = props.theme
  preferences.timeFormat = props.timeFormat
  preferences.calendarView = props.calendarView
  preferences.timezone = props.timezone
  preferences.weekStartDay = props.weekStartDay
  preferences.emailNotifications = props.emailNotifications
  preferences.reminderNotifications = props.reminderNotifications
})

// Theme options
const themeOptions = computed(() => [
  { value: 'light', label: t('profile.lightMode'), icon: SunIcon },
  { value: 'dark', label: t('profile.darkMode'), icon: MoonIcon },
  { value: 'system', label: t('profile.systemMode'), icon: ComputerDesktopIcon }
])

// Event handlers
const handleThemeChange = (theme: string) => {
  preferences.theme = theme as 'light' | 'dark' | 'system'
  emit('updateTheme', preferences.theme)
}

const handleTimeFormatChange = () => {
  emit('updateTimeFormat', preferences.timeFormat)
}

const handleCalendarViewChange = () => {
  emit('updateCalendarView', preferences.calendarView)
}

const handleTimezoneChange = () => {
  emit('updateTimezone', preferences.timezone)
}

const handleWeekStartDayChange = () => {
  emit('updateWeekStartDay', preferences.weekStartDay)
}

const handleEmailNotificationsChange = () => {
  emit('updateEmailNotifications', preferences.emailNotifications)
}

const handleReminderNotificationsChange = () => {
  emit('updateReminderNotifications', preferences.reminderNotifications)
}
</script>
