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
            <optgroup
              v-for="group in timezoneGroups"
              :key="group.continent"
              :label="group.label"
            >
              <option
                v-for="zone in group.zones"
                :key="zone.value"
                :value="zone.value"
              >
                {{ zone.label }} ({{ zone.offset }})
              </option>
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
import * as ct from 'countries-and-timezones'

// Composables
const { t, locale } = useI18n()

// Get timezone offset with DST information using countries-and-timezones library
const getTimezoneOffset = (timezone: string): string => {
  try {
    const tzData = ct.getTimezone(timezone)

    if (!tzData) {
      return 'UTC'
    }

    const standardOffset = tzData.utcOffsetStr
    const dstOffset = tzData.dstOffsetStr

    // Check if timezone has DST (different standard and DST offsets)
    if (standardOffset !== dstOffset) {
      return `UTC${standardOffset}, DST: ${dstOffset}`
    } else {
      return `UTC${standardOffset}`
    }
  } catch (error) {
    console.error('Error getting timezone offset:', error)
    return 'UTC'
  }
}

const formatTimezoneName = (timezone: string): string => {
  // Extract city name from IANA timezone (e.g., "America/New_York" -> "New York")
  const parts = timezone.split('/')
  const city = parts[parts.length - 1].replace(/_/g, ' ')
  return city
}

// Group timezones by continent with translated labels
const timezoneGroups = computed(() => {
  // Get all IANA timezones supported by the browser
  const allTimezones = Intl.supportedValuesOf('timeZone')

  // Translation mapping for continent names
  const continentTranslations: Record<string, string> = {
    'Africa': t('profile.timezones.continents.africa'),
    'America': t('profile.timezones.continents.america'),
    'Antarctica': t('profile.timezones.continents.antarctica'),
    'Arctic': t('profile.timezones.continents.arctic'),
    'Asia': t('profile.timezones.continents.asia'),
    'Atlantic': t('profile.timezones.continents.atlantic'),
    'Australia': t('profile.timezones.continents.australia'),
    'Europe': t('profile.timezones.continents.europe'),
    'Indian': t('profile.timezones.continents.indian'),
    'Pacific': t('profile.timezones.continents.pacific')
  }

  // Group timezones by continent
  const grouped: Record<string, Array<{ value: string, label: string, offset: string }>> = {}

  allTimezones.forEach(tz => {
    // Skip deprecated and special timezones
    if (tz.startsWith('Etc/') || tz === 'Factory') return

    const parts = tz.split('/')
    if (parts.length < 2) return

    const continent = parts[0]
    if (!grouped[continent]) {
      grouped[continent] = []
    }

    grouped[continent].push({
      value: tz,
      label: formatTimezoneName(tz),
      offset: getTimezoneOffset(tz)
    })
  })

  // Sort each group by label
  Object.keys(grouped).forEach(continent => {
    grouped[continent].sort((a, b) => a.label.localeCompare(b.label))
  })

  // Convert to array format for template
  return Object.entries(grouped)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([continent, zones]) => ({
      continent,
      label: continentTranslations[continent] || continent,
      zones
    }))
})

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
