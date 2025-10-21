<template>
  <div class="space-y-6">
    <!-- Calendar Statistics -->
    <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
      <div class="flex items-center">
        <svg class="h-5 w-5 text-blue-400 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div class="text-sm text-blue-800 dark:text-blue-200">
          {{ t('profile.calendarDataInfo', { count: stats?.totalTasks || 0 }) }}
        </div>
      </div>
    </div>

    <!-- Export Calendar Section -->
    <div class="border border-gray-200 dark:border-gray-700 rounded-lg p-6">
      <div class="flex items-start justify-between">
        <div class="flex-1">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center">
            <svg class="w-5 h-5 mr-2 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            {{ t('profile.exportCalendar') }}
          </h3>
          <p class="text-sm text-gray-600 dark:text-gray-400 mt-2">
            {{ t('profile.exportCalendarDescription') }}
          </p>
          <ul class="mt-3 text-xs text-gray-500 dark:text-gray-400 space-y-1">
            <li class="flex items-center">
              <svg class="w-4 h-4 mr-2 text-green-500" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
              {{ t('profile.exportCalendarFeature1') }}
            </li>
            <li class="flex items-center">
              <svg class="w-4 h-4 mr-2 text-green-500" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
              {{ t('profile.exportCalendarFeature2') }}
            </li>
            <li class="flex items-center">
              <svg class="w-4 h-4 mr-2 text-green-500" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
              {{ t('profile.exportCalendarFeature3') }}
            </li>
          </ul>
        </div>
        <button
          @click="handleExportCalendar"
          :disabled="isExporting || (stats?.totalTasks === 0)"
          class="ml-4 inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          :title="stats?.totalTasks === 0 ? t('profile.exportCalendarNoTasks') : ''"
        >
          <LoadingSpinner v-if="isExporting" class="w-4 h-4 mr-2" />
          <svg v-else class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
          {{ isExporting ? t('profile.exporting') : t('profile.exportCalendarButton') }}
        </button>
      </div>
    </div>

    <!-- Import Calendar Section -->
    <div class="border border-gray-200 dark:border-gray-700 rounded-lg p-6">
      <div class="flex items-start justify-between">
        <div class="flex-1">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center">
            <svg class="w-5 h-5 mr-2 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
            </svg>
            {{ t('profile.importCalendar') }}
          </h3>
          <p class="text-sm text-gray-600 dark:text-gray-400 mt-2">
            {{ t('profile.importCalendarDescription') }}
          </p>
          <ul class="mt-3 text-xs text-gray-500 dark:text-gray-400 space-y-1">
            <li class="flex items-center">
              <svg class="w-4 h-4 mr-2 text-green-500" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
              {{ t('profile.importCalendarFeature1') }}
            </li>
            <li class="flex items-center">
              <svg class="w-4 h-4 mr-2 text-green-500" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
              {{ t('profile.importCalendarFeature2') }}
            </li>
            <li class="flex items-center">
              <svg class="w-4 h-4 mr-2 text-green-500" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
              {{ t('profile.importCalendarFeature3') }}
            </li>
          </ul>
        </div>
        <div class="ml-4">
          <input
            ref="fileInput"
            type="file"
            accept=".ics,.ical"
            @change="handleFileSelect"
            class="hidden"
          />
          <button
            @click="triggerFileInput"
            :disabled="isImporting"
            class="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <LoadingSpinner v-if="isImporting" class="w-4 h-4 mr-2" />
            <svg v-else class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3v-6" />
            </svg>
            {{ isImporting ? t('profile.importing') : t('profile.importCalendarButton') }}
          </button>
        </div>
      </div>

      <!-- Import Results -->
      <div v-if="importResult" class="mt-4 p-4 rounded-lg" :class="getImportResultClasses()">
        <div class="flex items-start">
          <!-- Success icon (only if all succeeded) -->
          <svg v-if="isFullSuccess()" class="h-5 w-5 text-green-400 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
          </svg>
          <!-- Warning icon (partial success) -->
          <svg v-else-if="isPartialSuccess()" class="h-5 w-5 text-yellow-500 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
          </svg>
          <!-- Error icon (total failure) -->
          <svg v-else class="h-5 w-5 text-red-400 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
          <div class="ml-3 flex-1">
            <h4 class="text-sm font-medium" :class="getImportResultTextClass()">
              {{ getImportResultTitle() }}
            </h4>
            <div class="mt-2 text-sm" :class="getImportResultContentClass()">
              <p v-if="importResult.success">
                {{ t('profile.importCalendarStats', {
                  total: importResult.totalParsed,
                  success: importResult.successCount,
                  failed: importResult.failedCount
                }) }}
              </p>
              <p v-else>{{ importResult.error || t('profile.importCalendarUnknownError') }}</p>

              <!-- Show errors if any -->
              <div v-if="importResult.errors && importResult.errors.length > 0" class="mt-3">
                <p class="font-medium">{{ t('profile.importCalendarErrors') }}:</p>
                <ul class="mt-1 list-disc list-inside space-y-1">
                  <li v-for="(error, index) in importResult.errors.slice(0, 5)" :key="index" class="text-xs">
                    {{ error }}
                  </li>
                  <li v-if="importResult.errors.length > 5" class="text-xs italic">
                    {{ t('profile.importCalendarMoreErrors', { count: importResult.errors.length - 5 }) }}
                  </li>
                </ul>
              </div>
            </div>
            <button
              @click="importResult = null"
              class="mt-3 text-xs underline hover:no-underline"
              :class="getImportResultContentClass()"
            >
              {{ t('common.close') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { calendarApi, type CalendarImportResult, type CalendarStats } from '@/services/calendarApi'
import { useCustomToast } from '@/composables/useCustomToast'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'

// Composables
const { t } = useI18n()
const { showError, showSuccess } = useCustomToast()

// Local state
const fileInput = ref<HTMLInputElement>()
const isExporting = ref(false)
const isImporting = ref(false)
const importResult = ref<CalendarImportResult | null>(null)
const stats = ref<CalendarStats | null>(null)

// Load calendar stats
const loadStats = async () => {
  try {
    stats.value = await calendarApi.getCalendarStats()
  } catch (error) {
    console.error('Failed to load calendar stats:', error)
  }
}

// Export calendar
const handleExportCalendar = async () => {
  isExporting.value = true
  try {
    const { blob, filename } = await calendarApi.exportCalendar()

    // Create download link
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    showSuccess(t('profile.exportCalendarSuccessMessage'))
  } catch (error: any) {
    console.error('Export failed:', error)
    showError(error.message || t('profile.exportCalendarErrorMessage'))
  } finally {
    isExporting.value = false
  }
}

// Trigger file input
const triggerFileInput = () => {
  fileInput.value?.click()
}

// Handle file selection
const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  // Validate file type
  if (!file.name.endsWith('.ics') && !file.name.endsWith('.ical')) {
    showError(t('profile.importCalendarInvalidFile'))
    target.value = ''
    return
  }

  // Import file
  isImporting.value = true
  importResult.value = null

  try {
    const result = await calendarApi.importCalendar(file)
    importResult.value = result

    if (result.success && result.successCount > 0) {
      showSuccess(t('profile.importCalendarSuccessMessage', { count: result.successCount }))

      // Reload stats
      await loadStats()

      // Emit event to refresh calendar data
      window.dispatchEvent(new CustomEvent('calendar-imported'))
    }
  } catch (error: any) {
    console.error('Import failed:', error)
    importResult.value = {
      success: false,
      totalParsed: 0,
      successCount: 0,
      failedCount: 0,
      error: error.message || t('profile.importCalendarErrorMessage')
    }
  } finally {
    isImporting.value = false
    target.value = '' // Reset input so same file can be selected again
  }
}

// Helper functions for import result display
const isFullSuccess = () => {
  return importResult.value?.success && importResult.value.failedCount === 0
}

const isPartialSuccess = () => {
  return importResult.value?.success && importResult.value.failedCount > 0
}

const getImportResultClasses = () => {
  if (isFullSuccess()) {
    return 'bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800'
  } else if (isPartialSuccess()) {
    return 'bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800'
  } else {
    return 'bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800'
  }
}

const getImportResultTextClass = () => {
  if (isFullSuccess()) {
    return 'text-green-800 dark:text-green-200'
  } else if (isPartialSuccess()) {
    return 'text-yellow-800 dark:text-yellow-200'
  } else {
    return 'text-red-800 dark:text-red-200'
  }
}

const getImportResultContentClass = () => {
  if (isFullSuccess()) {
    return 'text-green-700 dark:text-green-300'
  } else if (isPartialSuccess()) {
    return 'text-yellow-700 dark:text-yellow-300'
  } else {
    return 'text-red-700 dark:text-red-300'
  }
}

const getImportResultTitle = () => {
  if (isFullSuccess()) {
    return t('profile.importCalendarSuccess')
  } else if (isPartialSuccess()) {
    return t('profile.importCalendarPartialSuccess')
  } else {
    return t('profile.importCalendarError')
  }
}

// Lifecycle
onMounted(() => {
  loadStats()
})
</script>
