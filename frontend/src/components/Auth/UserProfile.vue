<template>
  <div class="user-profile">
    <div class="bg-white dark:bg-gray-900 shadow-lg rounded-lg overflow-hidden">
      <!-- Profile Header -->
      <div class="px-6 py-8 bg-gradient-to-r from-blue-500 to-purple-600 relative">
        <!-- Back to Home Button -->
        <router-link
          to="/"
          class="absolute top-4 right-4 bg-white/20 backdrop-blur-sm hover:bg-white/30 text-white px-4 py-2 rounded-lg flex items-center space-x-2 transition-colors"
          title="Torna alla pagina principale"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          <span class="text-sm">Home</span>
        </router-link>

        <div class="flex flex-col sm:flex-row items-center sm:items-start space-y-4 sm:space-y-0 sm:space-x-6">
          <!-- Avatar Section -->
          <div class="relative group">
            <div class="w-24 h-24 rounded-full bg-white shadow-lg flex items-center justify-center overflow-hidden">
              <img
                v-if="user?.avatar"
                :src="user.avatar"
                :alt="`${user.firstName || user.username}'s avatar`"
                class="w-full h-full object-cover"
              />
              <span
                v-else
                class="text-2xl font-bold text-gray-600"
              >
                {{ userInitials }}
              </span>
            </div>

            <!-- Avatar Upload Overlay - TODO: Implement backend avatar upload -->
            <!-- <div class="absolute inset-0 bg-black bg-opacity-50 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
              <input
                ref="avatarInput"
                type="file"
                accept="image/*"
                @change="handleAvatarChange"
                class="hidden"
              />
              <button
                @click="triggerAvatarUpload"
                class="text-white hover:text-gray-200 transition-colors"
                title="Cambia avatar"
              >
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </button>
            </div> -->
          </div>

          <!-- User Info -->
          <div class="flex-1 text-center sm:text-left">
            <h1 class="text-2xl font-bold text-white mb-2">
              {{ userFullName || user?.username }}
            </h1>
            <p class="text-blue-100 mb-2">{{ user?.email }}</p>
            <p v-if="user?.createdAt" class="text-blue-200 text-sm">
              Membro dal {{ formatMemberSince(user.createdAt) }}
            </p>
          </div>
        </div>
      </div>

      <!-- Navigation Tabs (Desktop only) -->
      <div class="hidden md:block border-b border-gray-200 dark:border-gray-700">
        <nav class="-mb-px flex justify-center">
          <div class="flex space-x-1">
            <button
              v-for="tab in tabs"
              :key="tab.id"
              @click="activeTab = tab.id"
              :class="[
                'group inline-flex items-center py-4 px-6 border-b-2 font-medium text-sm transition-all duration-200 focus:outline-none focus:ring-0 whitespace-nowrap',
                activeTab === tab.id
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 hover:border-gray-300'
              ]"
            >
              <component :is="tab.icon" class="w-5 h-5 mr-2 flex-shrink-0" />
              <span>{{ tab.name }}</span>
            </button>
          </div>
        </nav>
      </div>

      <!-- Accordion Navigation (Mobile only) -->
      <div class="md:hidden">
        <div v-for="tab in tabs" :key="`mobile-${tab.id}`" class="border-b border-gray-200 dark:border-gray-700">
          <button
            @click="toggleMobileSection(tab.id)"
            class="w-full px-4 py-4 flex items-center justify-between hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
            :class="{ 'bg-blue-50/50 dark:bg-blue-900/10': activeTab === tab.id }"
          >
            <div class="flex items-center space-x-3">
              <component :is="tab.icon" class="w-5 h-5 flex-shrink-0" :class="activeTab === tab.id ? 'text-blue-600 dark:text-blue-400' : 'text-gray-500 dark:text-gray-400'" />
              <span class="font-medium text-sm" :class="activeTab === tab.id ? 'text-blue-600 dark:text-blue-400' : 'text-gray-900 dark:text-white'">
                {{ tab.name }}
              </span>
            </div>
            <svg
              class="w-5 h-5 transition-transform duration-200"
              :class="[
                activeTab === tab.id ? 'rotate-180 text-blue-600 dark:text-blue-400' : 'text-gray-400',
              ]"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          <!-- Mobile Tab Content -->
          <div v-show="activeTab === tab.id" class="px-4 py-2">
            <!-- Mobile: Personal Information -->
            <div v-if="tab.id === 'personal'">
              <ProfilePersonalInfo
                ref="personalInfoRef"
                :username="user?.username || ''"
                :email="user?.email || ''"
                :first-name="user?.firstName"
                :last-name="user?.lastName"
                :email-verified="user?.emailVerified || false"
                :is-loading="isLoading"
                @save="handlePersonalInfoSave"
                @cancel="handlePersonalInfoCancel"
              />
            </div>

            <!-- Mobile: Security -->
            <div v-else-if="tab.id === 'security'">
              <ProfileSecurity
                ref="securityRef"
                :two-factor-enabled="user?.twoFactorEnabled || false"
                :is-loading="isLoading"
                @change-password="handlePasswordChange"
                @toggle2-f-a="toggle2FA"
                @cancel="handleSecurityCancel"
              />
            </div>

            <!-- Mobile: Preferences -->
            <div v-else-if="tab.id === 'preferences'">
              <ProfilePreferences
                :is-loading="isLoading"
                :theme="preferencesForm.theme"
                :time-format="preferencesForm.timeFormat"
                :calendar-view="preferencesForm.calendarView"
                :timezone="preferencesForm.timezone"
                :week-start-day="preferencesForm.weekStartDay"
                :email-notifications="preferencesForm.emailNotifications"
                :reminder-notifications="preferencesForm.reminderNotifications"
                @update-theme="updateTheme"
                @update-time-format="updateTimeFormat"
                @update-calendar-view="updateCalendarView"
                @update-timezone="updateTimezone"
                @update-week-start-day="updateWeekStartDay"
                @update-email-notifications="updateEmailNotifications"
                @update-reminder-notifications="updateReminderNotifications"
              />
            </div>

            <!-- Mobile: Danger Zone -->
            <div v-else-if="tab.id === 'danger'">
              <ProfileDangerZone
                ref="dangerZoneRef"
                :is-loading="isLoading"
                @export-data="exportData"
                @delete-account="handleDeleteAccount"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Content (Desktop only) -->
      <div class="hidden md:block p-6">
        <!-- Personal Information Tab -->
        <div v-if="activeTab === 'personal'" class="space-y-6">
          <ProfilePersonalInfo
            ref="personalInfoRef"
            :username="user?.username || ''"
            :email="user?.email || ''"
            :first-name="user?.firstName"
            :last-name="user?.lastName"
            :email-verified="user?.emailVerified || false"
            :is-loading="isLoading"
            @save="handlePersonalInfoSave"
            @cancel="handlePersonalInfoCancel"
          />
        </div>

        <!-- Security Tab -->
        <div v-else-if="activeTab === 'security'" class="space-y-8">
          <ProfileSecurity
            ref="securityRef"
            :two-factor-enabled="user?.twoFactorEnabled || false"
            :is-loading="isLoading"
            @change-password="handlePasswordChange"
            @toggle2-f-a="toggle2FA"
            @cancel="handleSecurityCancel"
          />
        </div>

        <!-- Preferences Tab -->
        <div v-else-if="activeTab === 'preferences'" class="space-y-8">
          <ProfilePreferences
            :is-loading="isLoading"
            :theme="preferencesForm.theme"
            :time-format="preferencesForm.timeFormat"
            :calendar-view="preferencesForm.calendarView"
            :timezone="preferencesForm.timezone"
            :week-start-day="preferencesForm.weekStartDay"
            :email-notifications="preferencesForm.emailNotifications"
            :reminder-notifications="preferencesForm.reminderNotifications"
            @update-theme="updateTheme"
            @update-time-format="updateTimeFormat"
            @update-calendar-view="updateCalendarView"
            @update-timezone="updateTimezone"
            @update-week-start-day="updateWeekStartDay"
            @update-email-notifications="updateEmailNotifications"
            @update-reminder-notifications="updateReminderNotifications"
          />
        </div>

        <!-- Danger Zone Tab -->
        <div v-else-if="activeTab === 'danger'" class="space-y-8">
          <ProfileDangerZone
            ref="dangerZoneRef"
            :is-loading="isLoading"
            @export-data="exportData"
            @delete-account="handleDeleteAccount"
          />
        </div>
      </div>
    </div>

    <!-- Two-Factor Setup Modal -->
    <TwoFactorSetupModal
      v-model="showTwoFactorSetupModal"
      @success="handle2FASetupSuccess"
    />

    <!-- Two-Factor Disable Modal -->
    <TwoFactorDisableModal
      v-model="showTwoFactorDisableModal"
      @success="handle2FADisableSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuth } from '@/composables/useAuth'
import { useCustomToast } from '@/composables/useCustomToast'
import { useTheme } from '@/composables/useTheme'
import { useSettingsStore } from '@/stores/settings'
import { authApi } from '@/services/authApi'
import TwoFactorSetupModal from '@/components/Auth/TwoFactorSetupModal.vue'
import TwoFactorDisableModal from '@/components/Auth/TwoFactorDisableModal.vue'
import ProfileDangerZone from '@/components/Auth/ProfileDangerZone.vue'
import ProfilePreferences from '@/components/Auth/ProfilePreferences.vue'
import ProfilePersonalInfo from '@/components/Auth/ProfilePersonalInfo.vue'
import ProfileSecurity from '@/components/Auth/ProfileSecurity.vue'
import { UserIcon, ShieldCheckIcon, Cog6ToothIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'
import type { User } from '@/types/auth'

// i18n
const { t } = useI18n()

// Composables
const { user, userFullName, userInitials, updateProfile, logout, isLoading } = useAuth()
const { showError, showSuccess } = useCustomToast()
const { themeMode: currentTheme, setThemeMode } = useTheme()
const settingsStore = useSettingsStore()

// Tab icons
const PersonIcon = UserIcon
const SecurityIcon = ShieldCheckIcon
const SettingsIcon = Cog6ToothIcon
const DangerIcon = ExclamationTriangleIcon

// Format member since date
const formatMemberSince = (dateString: string | Date): string => {
  if (!dateString) return ''

  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('it-IT', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    })
  } catch (error) {
    console.error('Error formatting member since date:', error)
    return ''
  }
}

// Component state
const activeTab = ref('personal')

// Mobile accordion toggle
const toggleMobileSection = (tabId: string) => {
  if (activeTab.value === tabId) {
    activeTab.value = null
  } else {
    activeTab.value = tabId
  }
}
const personalInfoRef = ref<InstanceType<typeof ProfilePersonalInfo>>()
const securityRef = ref<InstanceType<typeof ProfileSecurity>>()
const dangerZoneRef = ref<InstanceType<typeof ProfileDangerZone>>()
const showTwoFactorSetupModal = ref(false)
const showTwoFactorDisableModal = ref(false)

// Form states for preferences (still needed for ProfilePreferences component)
const preferencesForm = reactive({
  theme: 'system' as 'light' | 'dark' | 'system',
  timezone: 'Europe/Rome',
  timeFormat: '24h' as '12h' | '24h',
  calendarView: 'week' as 'month' | 'week' | 'day' | 'agenda',
  emailNotifications: true,
  reminderNotifications: true,
  weekStartDay: 1 as 0 | 1
})

// Tabs configuration
const tabs = computed(() => [
  { id: 'personal', name: t('profile.tabs.personal'), icon: PersonIcon },
  { id: 'security', name: t('profile.tabs.security'), icon: SecurityIcon },
  { id: 'preferences', name: t('profile.tabs.preferences'), icon: SettingsIcon },
  { id: 'danger', name: t('profile.tabs.danger'), icon: DangerIcon }
])

// Form submission handlers
const handlePersonalInfoSave = async (updateData: { email?: string; firstName?: string; lastName?: string }) => {
  try {
    const success = await updateProfile(updateData as Partial<User>)
    if (success) {
      personalInfoRef.value?.closeEdit()
      showSuccess(t('profile.messages.personalInfoUpdated'))
    }
  } catch (error: any) {
    console.error('Profile update failed:', error)

    // Set error on child component if it's email-related
    if (error.response?.data?.field === 'email') {
      personalInfoRef.value?.setEmailError(error.response.data.message)
    } else {
      showError(t('profile.messages.profileUpdateError'))
    }
  }
}

const handlePersonalInfoCancel = () => {
  // Just a placeholder for consistency
}

const handlePasswordChange = async (data: { currentPassword: string; newPassword: string }) => {
  try {
    await authApi.changePassword(data.currentPassword, data.newPassword)

    securityRef.value?.closeEdit()
    showSuccess(t('profile.messages.passwordChanged'))
  } catch (error: any) {
    console.error('Password change failed:', error)

    if (error.response?.status === 401) {
      securityRef.value?.setCurrentPasswordError(t('profile.messages.currentPasswordWrong'))
    } else {
      showError(t('profile.messages.passwordChangeError'))
    }
  }
}

const handleSecurityCancel = () => {
  // Just a placeholder for consistency
}

// Auto-save individual preference updates
const savePreference = async (updates: Partial<typeof preferencesForm>) => {
  try {
    const updatedPreferences = await authApi.updatePreferences(updates)

    // Update settings store with new values
    if (updates.theme) {
      setThemeMode(updates.theme as 'light' | 'dark' | 'system')
      settingsStore.updateTheme(updates.theme)
    }
    if (updates.timeFormat) {
      settingsStore.updateTimeFormat(updates.timeFormat)
    }
    if (updates.calendarView) {
      settingsStore.updateCalendarView(updates.calendarView)
    }

    // Update form with server response to ensure consistency
    if (updatedPreferences) {
      Object.assign(preferencesForm, updatedPreferences)
    }

  } catch (error) {
    console.error('Preference save failed:', error)
    showError(t('profile.messages.preferenceSaveError'))
  }
}

// Individual preference update functions
const updateTheme = (theme: 'light' | 'dark' | 'system') => {
  preferencesForm.theme = theme
  savePreference({ theme })
}

const updateTimeFormat = (timeFormat: '12h' | '24h') => {
  preferencesForm.timeFormat = timeFormat
  savePreference({ timeFormat })
}

const updateCalendarView = (calendarView: 'month' | 'week' | 'day' | 'agenda') => {
  preferencesForm.calendarView = calendarView
  savePreference({ calendarView })
}

const updateTimezone = (timezone: string) => {
  preferencesForm.timezone = timezone
  savePreference({ timezone })
}

const updateEmailNotifications = (enabled: boolean) => {
  preferencesForm.emailNotifications = enabled
  savePreference({ emailNotifications: enabled })
}

const updateReminderNotifications = (enabled: boolean) => {
  preferencesForm.reminderNotifications = enabled
  savePreference({ reminderNotifications: enabled })
}

const updateWeekStartDay = (weekStartDay: 0 | 1) => {
  preferencesForm.weekStartDay = weekStartDay
  savePreference({ weekStartDay })
  settingsStore.updateWeekStartDay(weekStartDay)
}

// Two-Factor Authentication
const toggle2FA = () => {
  if (user.value?.twoFactorEnabled) {
    showTwoFactorDisableModal.value = true
  } else {
    showTwoFactorSetupModal.value = true
  }
}

const handle2FASetupSuccess = async () => {
  try {
    const updatedUser = await authApi.getProfile()
    if (user.value) {
      user.value.twoFactorEnabled = updatedUser.twoFactorEnabled
    }
  } catch (error) {
    console.error('Error refreshing user profile:', error)
  }
}

const handle2FADisableSuccess = async () => {
  try {
    const updatedUser = await authApi.getProfile()
    if (user.value) {
      user.value.twoFactorEnabled = updatedUser.twoFactorEnabled
    }
  } catch (error) {
    console.error('Error refreshing user profile:', error)
  }
}

// Data export
const exportData = async () => {
  try {
    const { blob, filename } = await authApi.exportData()

    // Create download link
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    showSuccess(t('profile.messages.dataExported'))
  } catch (error) {
    console.error('Data export failed:', error)
    showError(t('profile.messages.dataExportError'))
  }
}

// Account deletion
const handleDeleteAccount = async (password: string) => {
  try {
    await authApi.deleteAccount(password)

    showSuccess(t('profile.messages.accountDeleted'))

    // Close modal and logout
    dangerZoneRef.value?.closeModal()
    await logout()
  } catch (error: any) {
    console.error('Account deletion failed:', error)

    if (error.response?.status === 401) {
      dangerZoneRef.value?.setDeleteError(t('profile.messages.passwordWrong'))
    } else {
      dangerZoneRef.value?.setDeleteError(t('profile.messages.accountDeleteError'))
    }
  }
}

// Load preferences on mount
const loadPreferences = async () => {
  try {
    const preferences = await authApi.getPreferences()
    preferencesForm.theme = preferences.theme || 'system'
    preferencesForm.timezone = preferences.timezone || 'Europe/Rome'
    preferencesForm.timeFormat = preferences.timeFormat || '24h'
    preferencesForm.calendarView = preferences.calendarView || 'week'
    preferencesForm.emailNotifications = preferences.emailNotifications ?? true
    preferencesForm.reminderNotifications = preferences.reminderNotifications ?? true
    preferencesForm.weekStartDay = preferences.weekStartDay ?? 1

    // Sync with settings store
    settingsStore.updateTheme(preferencesForm.theme)
    settingsStore.updateTimeFormat(preferencesForm.timeFormat)
    settingsStore.updateCalendarView(preferencesForm.calendarView)
    settingsStore.updateWeekStartDay(preferencesForm.weekStartDay)
  } catch (error) {
    console.error('Failed to load preferences:', error)
    // Load defaults from settings store
    preferencesForm.theme = settingsStore.settings.theme
    preferencesForm.timeFormat = settingsStore.settings.timeFormat
    preferencesForm.calendarView = settingsStore.settings.calendarView
  }
}

// Lifecycle
onMounted(() => {
  loadPreferences()
})
</script>

<style scoped>
.user-profile {
  @apply max-w-4xl mx-auto;
}

/* Custom focus styles */
input:focus,
select:focus {
  @apply outline-none ring-2 ring-blue-500 ring-offset-2 dark:ring-offset-gray-900;
}

/* Tab button focus styles - remove all focus styles */
nav button {
  @apply focus:outline-none focus:ring-0 focus:shadow-none;
}

nav button:focus {
  outline: none !important;
  box-shadow: none !important;
  border: none !important;
}

/* Tab transitions */
.tab-content {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Avatar hover effect */
.group:hover .group-hover\:opacity-100 {
  opacity: 1;
}

/* Mobile responsiveness */
@media (max-width: 768px) {
  .user-profile {
    @apply mx-4;
  }
  
  
  .space-x-8 {
    @apply space-x-4;
  }
}

/* High contrast mode */
@media (prefers-contrast: high) {
  .border-gray-200 {
    @apply border-black;
  }
  
  .text-gray-600 {
    @apply text-black;
  }
}

/* Print styles */
@media print {
  .user-profile {
    @apply shadow-none;
  }
  
  button {
    @apply hidden;
  }
}
</style>