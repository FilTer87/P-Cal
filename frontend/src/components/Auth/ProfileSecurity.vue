<template>
  <div class="space-y-8">
    <!-- Change Password Section -->
    <div>
      <div class="flex items-center justify-between mb-6">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
          Cambia Password
        </h3>
        <button
          @click="toggleEdit"
          class="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
        >
          <svg v-if="!isEditing" class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
          {{ isEditing ? 'Annulla' : 'Modifica Password' }}
        </button>
      </div>

      <form v-if="isEditing" @submit.prevent="handleSubmit" class="space-y-6 max-w-md">
        <!-- Current Password -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Password Attuale *
          </label>
          <div class="relative">
            <input
              v-model="formData.currentPassword"
              :type="showPasswords.current ? 'text' : 'password'"
              required
              :disabled="isLoading"
              class="w-full px-4 py-3 pr-12 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
              placeholder="Inserisci password attuale"
            />
            <button
              type="button"
              @click="showPasswords.current = !showPasswords.current"
              class="absolute inset-y-0 right-0 pr-3 flex items-center"
              :disabled="isLoading"
            >
              <svg
                v-if="showPasswords.current"
                class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
              </svg>
              <svg
                v-else
                class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </button>
          </div>
          <p v-if="errors.currentPassword" class="mt-1 text-sm text-red-600 dark:text-red-400">
            {{ errors.currentPassword }}
          </p>
        </div>

        <!-- New Password -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Nuova Password *
          </label>
          <div class="relative">
            <input
              v-model="formData.newPassword"
              :type="showPasswords.new ? 'text' : 'password'"
              required
              :disabled="isLoading"
              class="w-full px-4 py-3 pr-12 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
              placeholder="Inserisci nuova password"
            />
            <button
              type="button"
              @click="showPasswords.new = !showPasswords.new"
              class="absolute inset-y-0 right-0 pr-3 flex items-center"
              :disabled="isLoading"
            >
              <svg
                v-if="showPasswords.new"
                class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
              </svg>
              <svg
                v-else
                class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </button>
          </div>
          <p v-if="errors.newPassword" class="mt-1 text-sm text-red-600 dark:text-red-400">
            {{ errors.newPassword }}
          </p>

          <!-- Password Strength Indicator -->
          <PasswordStrengthIndicator :password="formData.newPassword" />
        </div>

        <!-- Confirm New Password -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Conferma Nuova Password *
          </label>
          <div class="relative">
            <input
              v-model="formData.confirmPassword"
              :type="showPasswords.confirm ? 'text' : 'password'"
              required
              :disabled="isLoading"
              class="w-full px-4 py-3 pr-12 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
              placeholder="Conferma nuova password"
            />
            <button
              type="button"
              @click="showPasswords.confirm = !showPasswords.confirm"
              class="absolute inset-y-0 right-0 pr-3 flex items-center"
              :disabled="isLoading"
            >
              <svg
                v-if="showPasswords.confirm"
                class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
              </svg>
              <svg
                v-else
                class="h-5 w-5 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </button>
          </div>
          <p v-if="errors.confirmPassword" class="mt-1 text-sm text-red-600 dark:text-red-400">
            {{ errors.confirmPassword }}
          </p>
        </div>

        <!-- Password Change Actions -->
        <div class="flex justify-end space-x-4">
          <button
            type="button"
            @click="handleCancel"
            :disabled="isLoading"
            class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            Annulla
          </button>
          <button
            type="submit"
            :disabled="isLoading || !isFormValid"
            class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
            {{ isLoading ? 'Cambiando...' : 'Cambia Password' }}
          </button>
        </div>
      </form>
    </div>

    <!-- Two-Factor Authentication Section -->
    <div class="border-t border-gray-200 dark:border-gray-700 pt-8">
      <div class="flex items-center justify-between">
        <div>
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
            Autenticazione a Due Fattori
          </h3>
          <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
            Aumenta la sicurezza del tuo account abilitando l'autenticazione a due fattori
          </p>
        </div>
        <button
          @click="handle2FAToggle"
          :disabled="isLoading"
          class="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
        >
          {{ twoFactorEnabled ? 'Disabilita 2FA' : 'Abilita 2FA' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { checkPasswordStrength } from '@/utils/validators'
import PasswordStrengthIndicator from '@/components/Common/PasswordStrengthIndicator.vue'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'

// Props
interface Props {
  twoFactorEnabled: boolean
  isLoading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isLoading: false
})

// Emits
const emit = defineEmits<{
  changePassword: [data: { currentPassword: string; newPassword: string }]
  toggle2FA: []
  cancel: []
}>()

// Local state
const isEditing = ref(false)
const formData = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const showPasswords = reactive({
  current: false,
  new: false,
  confirm: false
})
const errors = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// Computed
const passwordStrength = computed(() => checkPasswordStrength(formData.newPassword))

const isFormValid = computed(() => {
  return formData.currentPassword.trim().length > 0 &&
         formData.newPassword.trim().length >= 8 &&
         formData.confirmPassword === formData.newPassword &&
         passwordStrength.value.isStrong &&
         !errors.currentPassword &&
         !errors.newPassword &&
         !errors.confirmPassword
})

// Methods
const toggleEdit = () => {
  if (isEditing.value) {
    handleCancel()
  } else {
    isEditing.value = true
    resetForm()
  }
}

const resetForm = () => {
  formData.currentPassword = ''
  formData.newPassword = ''
  formData.confirmPassword = ''
  errors.currentPassword = ''
  errors.newPassword = ''
  errors.confirmPassword = ''
  showPasswords.current = false
  showPasswords.new = false
  showPasswords.confirm = false
}

const handleCancel = () => {
  isEditing.value = false
  resetForm()
  emit('cancel')
}

const handleSubmit = () => {
  errors.currentPassword = ''
  errors.newPassword = ''
  errors.confirmPassword = ''

  // Validate
  if (!formData.currentPassword.trim()) {
    errors.currentPassword = 'La password attuale è richiesta'
    return
  }

  if (!formData.newPassword.trim()) {
    errors.newPassword = 'La nuova password è richiesta'
    return
  }

  if (formData.newPassword.length < 8) {
    errors.newPassword = 'La password deve contenere almeno 8 caratteri'
    return
  }

  if (!passwordStrength.value.isStrong) {
    errors.newPassword = 'La password non rispetta i criteri di sicurezza richiesti'
    return
  }

  if (formData.newPassword === formData.currentPassword) {
    errors.newPassword = 'La nuova password deve essere diversa da quella attuale'
    return
  }

  if (!formData.confirmPassword.trim()) {
    errors.confirmPassword = 'Conferma la password'
    return
  }

  if (formData.newPassword !== formData.confirmPassword) {
    errors.confirmPassword = 'Le password non coincidono'
    return
  }

  // Emit change password event
  emit('changePassword', {
    currentPassword: formData.currentPassword,
    newPassword: formData.newPassword
  })
}

const handle2FAToggle = () => {
  emit('toggle2FA')
}

// Expose methods for parent to control state
defineExpose({
  closeEdit: () => {
    isEditing.value = false
    resetForm()
  },
  setCurrentPasswordError: (error: string) => {
    errors.currentPassword = error
  }
})
</script>
