<template>
  <div class="border border-gray-200 dark:border-gray-700 rounded-lg p-6">
    <div class="flex items-start justify-between mb-6">
      <div class="flex-1">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center">
          <svg class="w-5 h-5 mr-2 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          {{ t('calendars.title') }}
        </h3>
        <p class="text-sm text-gray-600 dark:text-gray-400 mt-2">
          {{ t('calendars.description') }}
        </p>
      </div>
      <button
        @click="openCreateModal"
        class="ml-4 inline-flex items-center px-4 py-2 bg-purple-600 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 transition-colors"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        {{ t('calendars.newButton') }}
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="flex justify-center items-center py-12">
      <LoadingSpinner class="w-8 h-8 text-purple-600" />
    </div>

    <!-- Calendars List -->
    <div v-else-if="calendars.length > 0" class="space-y-3">
      <div
        v-for="calendar in calendars"
        :key="calendar.id"
        class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg border border-gray-200 dark:border-gray-600 hover:border-gray-300 dark:hover:border-gray-500 transition-colors"
      >
        <div class="flex items-center flex-1 min-w-0">
          <div
            class="w-4 h-4 rounded flex-shrink-0 mr-3"
            :style="{ backgroundColor: calendar.color }"
          ></div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center">
              <h4 class="text-sm font-medium text-gray-900 dark:text-white truncate">
                {{ calendar.name }}
              </h4>
              <span
                v-if="calendar.isDefault"
                class="ml-2 px-2 py-0.5 text-xs font-medium bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 rounded"
              >
                default
              </span>
            </div>
            <div class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              <span class="font-mono">{{ calendar.slug }}</span>
              <span class="mx-2">•</span>
              <span>{{ calendar.taskCount }} {{ calendar.taskCount === 1 ? t('calendars.task') : t('calendars.tasks') }}</span>
            </div>
            <div class="text-xs text-gray-400 dark:text-gray-500 mt-0.5 font-mono">
              CalDAV: {{ calendar.caldavPath }}
            </div>
          </div>
        </div>
        <div class="flex items-center ml-4 space-x-2">
          <button
            @click="openEditModal(calendar)"
            class="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 rounded transition-colors"
            :title="t('calendars.editButton')"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
          </button>
          <button
            @click="confirmDelete(calendar)"
            :disabled="calendars.length === 1"
            class="p-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            :title="calendars.length === 1 ? t('calendars.deleteLastCalendarError') : t('calendars.deleteButton')"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-white">{{ t('calendars.noCalendars') }}</h3>
      <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">{{ t('calendars.noCalendarsDescription') }}</p>
    </div>

    <!-- Create/Edit Modal -->
    <div
      v-if="showModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="closeModal"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full">
        <!-- Modal Header -->
        <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
            <svg v-if="calendarForm.isDefault" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 0 1-1.043 3.296 3.745 3.745 0 0 1-3.296 1.043A3.745 3.745 0 0 1 12 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 0 1-3.296-1.043 3.745 3.745 0 0 1-1.043-3.296A3.745 3.745 0 0 1 3 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 0 1 1.043-3.296 3.746 3.746 0 0 1 3.296-1.043A3.746 3.746 0 0 1 12 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 0 1 3.296 1.043 3.746 3.746 0 0 1 1.043 3.296A3.745 3.745 0 0 1 21 12Z" />
            </svg>
            {{ isEditing ? t('calendars.editTitle') : t('calendars.createTitle') }}
          </h3>
        </div>

        <!-- Modal Body -->
        <form @submit.prevent="saveCalendar" class="px-6 py-4 space-y-4">
          <!-- Name -->
          <div>
            <label for="calendar-name" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('calendars.form.name') }} <span class="text-red-500">*</span>
            </label>
            <input
              id="calendar-name"
              v-model="calendarForm.name"
              type="text"
              required
              maxlength="100"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              :placeholder="t('calendars.form.namePlaceholder')"
            />
          </div>

          <!-- Slug -->
          <div>
            <label for="calendar-slug" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('calendars.form.slug') }} <span class="text-red-500">*</span>
            </label>
            <input
              id="calendar-slug"
              v-model="calendarForm.slug"
              type="text"
              required
              pattern="[a-z0-9-]+"
              maxlength="50"
              :readonly="isEditing"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white font-mono text-sm"
              :class="{ 'bg-gray-100 dark:bg-gray-800 cursor-not-allowed': isEditing }"
              :placeholder="t('calendars.form.slugPlaceholder')"
            />
            <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
              {{ isEditing ? t('calendars.form.slugImmutable') : t('calendars.form.slugHelp') }}
            </p>
          </div>

          <!-- Color -->
          <div>
            <label for="calendar-color" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('calendars.form.color') }}
            </label>
            <div class="flex items-center space-x-2">
              <input
                id="calendar-color"
                v-model="calendarForm.color"
                type="color"
                class="h-10 w-20 rounded cursor-pointer border border-gray-300 dark:border-gray-600"
              />
              <input
                v-model="calendarForm.color"
                type="text"
                pattern="^#[0-9A-Fa-f]{6}$"
                class="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white font-mono text-sm"
                placeholder="#3788d8"
              />
            </div>
          </div>

          <!-- Timezone -->
          <div>
            <label for="calendar-timezone" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {{ t('calendars.form.timezone') }}
            </label>
            <select
              id="calendar-timezone"
              v-model="calendarForm.timezone"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            >
              <option value="Europe/Rome">Europe/Rome</option>
              <option value="UTC">UTC</option>
              <option value="America/New_York">America/New_York</option>
              <option value="America/Los_Angeles">America/Los_Angeles</option>
              <option value="Asia/Tokyo">Asia/Tokyo</option>
              <option value="Australia/Sydney">Australia/Sydney</option>
            </select>
          </div>

          <!-- Is Default -->
          <div class="flex items-start">
            <div class="flex items-center h-5">
              <input
                id="calendar-default"
                v-model="calendarForm.isDefault"
                type="checkbox"
                :disabled="isEditingDefaultCalendar"
                class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 dark:border-gray-600 rounded"
                :class="{ 'opacity-50 cursor-not-allowed': isEditingDefaultCalendar }"
              />
            </div>
            <div class="ml-3 text-sm">
              <label for="calendar-default" class="font-medium text-gray-700 dark:text-gray-300">
                {{ t('calendars.form.setAsDefault') }}
              </label>
              <p v-if="isEditingDefaultCalendar" class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                {{ t('calendars.form.defaultImmutable') }}
              </p>
            </div>
          </div>

          <!-- Error Message -->
          <div v-if="errorMessage" class="p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
            <p class="text-sm text-red-800 dark:text-red-200">{{ errorMessage }}</p>
          </div>
        </form>

        <!-- Modal Footer -->
        <div class="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end space-x-3">
          <button
            @click="closeModal"
            type="button"
            class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
          >
            {{ t('common.cancel') }}
          </button>
          <button
            @click="saveCalendar"
            :disabled="isSaving"
            type="submit"
            class="px-4 py-2 bg-purple-600 border border-transparent rounded-md text-sm font-medium text-white hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors inline-flex items-center"
          >
            <LoadingSpinner v-if="isSaving" class="w-4 h-4 mr-2" />
            {{ isSaving ? t('common.saving') : t('common.save') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div
      v-if="showDeleteModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="cancelDelete"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full">
        <div class="px-6 py-4">
          <div class="flex items-start">
            <div class="flex-shrink-0">
              <svg class="h-6 w-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <div class="ml-3 flex-1">
              <h3 class="text-lg font-medium text-gray-900 dark:text-white">
                {{ t('calendars.deleteConfirmTitle') }}
              </h3>
              <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
                {{ t('calendars.deleteConfirmMessage', {
                  name: calendarToDelete?.name,
                  count: calendarToDelete?.taskCount || 0
                }) }}
              </p>
            </div>
          </div>
        </div>
        <div class="px-6 py-4 bg-gray-50 dark:bg-gray-700/50 flex justify-end space-x-3 rounded-b-lg">
          <button
            @click="cancelDelete"
            class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
          >
            {{ t('common.cancel') }}
          </button>
          <button
            @click="deleteCalendar"
            :disabled="isDeleting"
            class="px-4 py-2 bg-red-600 border border-transparent rounded-md text-sm font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors inline-flex items-center"
          >
            <LoadingSpinner v-if="isDeleting" class="w-4 h-4 mr-2" />
            {{ isDeleting ? t('common.deleting') : t('common.delete') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { calendarsApi, type Calendar, type CalendarRequest } from '@/services/calendarsApi'
import { useCustomToast } from '@/composables/useCustomToast'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'

// Composables
const { t } = useI18n()
const { showError, showSuccess } = useCustomToast()

// State
const calendars = ref<Calendar[]>([])
const isLoading = ref(false)
const showModal = ref(false)
const isEditing = ref(false)
const isSaving = ref(false)
const errorMessage = ref('')
const originalCalendar = ref<Calendar | null>(null) // Track original calendar for edit

// Delete state
const showDeleteModal = ref(false)
const calendarToDelete = ref<Calendar | null>(null)
const isDeleting = ref(false)

// Form state
const calendarForm = ref<CalendarRequest>({
  name: '',
  slug: '',
  color: '#3788d8',
  timezone: 'Europe/Rome',
  isDefault: false
})

// Computed
const isEditingDefaultCalendar = computed(() => {
  return isEditing.value && originalCalendar.value?.isDefault === true
})

// Load calendars
const loadCalendars = async () => {
  isLoading.value = true
  try {
    calendars.value = await calendarsApi.getAllCalendars()
  } catch (error: any) {
    console.error('Failed to load calendars:', error)
    showError(error.message || t('calendars.loadError'))
  } finally {
    isLoading.value = false
  }
}

// Auto-generate slug from name
watch(() => calendarForm.value.name, (newName) => {
  if (!isEditing.value && newName) {
    calendarForm.value.slug = newName
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
  }
})

// Open create modal
const openCreateModal = () => {
  isEditing.value = false
  calendarForm.value = {
    name: '',
    slug: '',
    color: '#3788d8',
    timezone: 'Europe/Rome',
    isDefault: calendars.value.length === 0 // First calendar is default
  }
  errorMessage.value = ''
  showModal.value = true
}

// Open edit modal
const openEditModal = (calendar: Calendar) => {
  isEditing.value = true
  originalCalendar.value = calendar // Save original calendar
  calendarForm.value = {
    name: calendar.name,
    slug: calendar.slug,
    color: calendar.color || '#3788d8',
    timezone: calendar.timezone || 'Europe/Rome',
    isDefault: calendar.isDefault
  }
  errorMessage.value = ''
  showModal.value = true
}

// Close modal
const closeModal = () => {
  showModal.value = false
  errorMessage.value = ''
}

// Save calendar
const saveCalendar = async () => {
  isSaving.value = true
  errorMessage.value = ''

  try {
    if (isEditing.value) {
      // Update existing calendar
      const calendarId = calendars.value.find(c => c.slug === calendarForm.value.slug)?.id
      if (!calendarId) {
        throw new Error('Calendar not found')
      }
      await calendarsApi.updateCalendar(calendarId, calendarForm.value)
      showSuccess(t('calendars.updateSuccess'))
    } else {
      // Create new calendar
      await calendarsApi.createCalendar(calendarForm.value)
      showSuccess(t('calendars.createSuccess'))
    }

    await loadCalendars()
    closeModal()

    // Emit event to refresh calendar data
    window.dispatchEvent(new CustomEvent('calendars-updated'))
  } catch (error: any) {
    console.error('Failed to save calendar:', error)
    errorMessage.value = error.message || t('calendars.saveError')
  } finally {
    isSaving.value = false
  }
}

// Confirm delete
const confirmDelete = (calendar: Calendar) => {
  calendarToDelete.value = calendar
  showDeleteModal.value = true
}

// Cancel delete
const cancelDelete = () => {
  calendarToDelete.value = null
  showDeleteModal.value = false
}

// Delete calendar
const deleteCalendar = async () => {
  if (!calendarToDelete.value) return

  isDeleting.value = true
  try {
    await calendarsApi.deleteCalendar(calendarToDelete.value.id)
    showSuccess(t('calendars.deleteSuccess'))
    await loadCalendars()
    cancelDelete()

    // Emit event to refresh calendar data
    window.dispatchEvent(new CustomEvent('calendars-updated'))
  } catch (error: any) {
    console.error('Failed to delete calendar:', error)
    showError(error.message || t('calendars.deleteError'))
  } finally {
    isDeleting.value = false
  }
}

// Lifecycle
onMounted(() => {
  loadCalendars()
})
</script>
