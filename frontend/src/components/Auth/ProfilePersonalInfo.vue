<template>
  <div>
    <!-- Header with Edit Button -->
    <div class="flex items-center justify-between mb-6">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
        {{ t('profile.personalInfo') }}
      </h3>
      <button
        @click="toggleEdit"
        class="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
      >
        <svg v-if="!isEditing" class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
        </svg>
        <svg v-else class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
        {{ isEditing ? t('common.cancel') : t('common.edit') }}
      </button>
    </div>

    <form @submit.prevent="handleSubmit" class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <!-- Username -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ t('profile.usernameLabel') }}
        </label>
        <input
          :value="username"
          type="text"
          disabled
          class="w-full px-4 py-3 border rounded-lg bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed"
          :placeholder="t('profile.usernamePlaceholder')"
        />
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400" v-if="isEditing">
          {{ t('profile.usernameCannotBeModified') }}
        </p>
      </div>

      <!-- Email -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ t('profile.emailLabel') }}
        </label>
        <input
          v-model="formData.email"
          type="email"
          :disabled="emailVerified || !isEditing || isLoading"
          :class="[
            'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
            emailVerified || !isEditing
              ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed'
              : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
          ]"
          :placeholder="t('profile.emailPlaceholder')"
        />
        <p v-if="emailVerified" class="mt-1 text-sm text-green-500 dark:text-green-400">
          ✓ {{ t('profile.emailVerified') }}
        </p>
        <p v-else class="mt-1 text-sm text-yellow-600 dark:text-yellow-400" v-if="isEditing">
          ⚠ {{ t('profile.emailNotVerified') }}
        </p>
        <p v-if="errors.email" class="mt-1 text-sm text-red-600 dark:text-red-400">
          {{ errors.email }}
        </p>
      </div>

      <!-- First Name -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ t('profile.firstNameLabel') }}
        </label>
        <input
          v-model="formData.firstName"
          type="text"
          :disabled="!isEditing || isLoading"
          :class="[
            'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
            !isEditing
              ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed'
              : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
          ]"
          :placeholder="isEditing ? t('profile.firstNamePlaceholder') : t('profile.firstNameEmpty')"
        />
      </div>

      <!-- Last Name -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ t('profile.lastNameLabel') }}
        </label>
        <input
          v-model="formData.lastName"
          type="text"
          :disabled="!isEditing || isLoading"
          :class="[
            'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
            !isEditing
              ? 'bg-gray-50 dark:bg-gray-800 text-gray-500 dark:text-gray-400 cursor-not-allowed'
              : 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-600'
          ]"
          :placeholder="isEditing ? t('profile.lastNamePlaceholder') : t('profile.lastNameEmpty')"
        />
      </div>

      <!-- Save Button -->
      <div v-if="isEditing" class="md:col-span-2">
        <div class="flex justify-end space-x-4">
          <button
            type="button"
            @click="handleCancel"
            :disabled="isLoading"
            class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            {{ t('common.cancel') }}
          </button>
          <button
            type="submit"
            :disabled="isLoading"
            class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
            {{ isLoading ? t('profile.saving') : t('profile.saveChanges') }}
          </button>
        </div>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'

// Composables
const { t } = useI18n()

// Props
interface Props {
  username: string
  email: string
  firstName?: string
  lastName?: string
  emailVerified: boolean
  isLoading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  firstName: '',
  lastName: '',
  isLoading: false
})

// Emits
const emit = defineEmits<{
  save: [data: { email?: string; firstName?: string; lastName?: string }]
  cancel: []
}>()

// Local state
const isEditing = ref(false)
const formData = reactive({
  email: props.email,
  firstName: props.firstName,
  lastName: props.lastName
})
const errors = reactive({
  email: ''
})

// Watch props to update form when user data changes
watch(() => [props.email, props.firstName, props.lastName], () => {
  if (!isEditing.value) {
    formData.email = props.email
    formData.firstName = props.firstName
    formData.lastName = props.lastName
  }
})

// Methods
const toggleEdit = () => {
  if (isEditing.value) {
    handleCancel()
  } else {
    isEditing.value = true
    // Initialize form with current props
    formData.email = props.email
    formData.firstName = props.firstName
    formData.lastName = props.lastName
  }
}

const handleCancel = () => {
  isEditing.value = false
  // Reset form to original values
  formData.email = props.email
  formData.firstName = props.firstName
  formData.lastName = props.lastName
  errors.email = ''
  emit('cancel')
}

const handleSubmit = () => {
  errors.email = ''

  // Build update data
  const updateData: { email?: string; firstName?: string; lastName?: string } = {
    firstName: formData.firstName.trim() || undefined,
    lastName: formData.lastName.trim() || undefined
  }

  // Include email only if it's not verified (allowing correction of typos)
  if (!props.emailVerified) {
    updateData.email = formData.email.trim()
  }

  emit('save', updateData)
}

// Expose methods for parent to control state
defineExpose({
  closeEdit: () => {
    isEditing.value = false
  },
  setEmailError: (error: string) => {
    errors.email = error
  }
})
</script>
