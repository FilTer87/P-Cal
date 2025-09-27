<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
    <div class="sm:mx-auto sm:w-full sm:max-w-md">
      <div class="text-center">
        <h1 class="text-4xl font-bold text-gray-900 dark:text-white">P-Cal</h1>
        <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
          Il tuo calendario personale
        </p>
      </div>

      <div class="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div class="bg-white dark:bg-gray-800 py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <!-- Success State -->
          <div v-if="isSuccess" class="text-center">
            <div
              class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100 dark:bg-green-900/20">
              <CheckCircleIcon class="h-6 w-6 text-green-600 dark:text-green-400" />
            </div>
            <h2 class="mt-4 text-2xl font-bold text-gray-900 dark:text-white">
              Password Reimpostata!
            </h2>
            <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
              La tua password è stata cambiata con successo. Ora puoi effettuare il login con la nuova password.
            </p>
            <div class="mt-6">
              <router-link to="/login" class="btn btn-primary w-full">
                Vai al Login
              </router-link>
            </div>
          </div>

          <!-- Reset Form -->
          <div v-else>
            <h2 class="mb-6 text-center text-3xl font-extrabold text-gray-900 dark:text-white">
              Reimposta Password
            </h2>

            <!-- Invalid Token Error -->
            <div v-if="invalidToken"
              class="mb-6 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700 rounded-lg p-4">
              <div class="flex">
                <ExclamationTriangleIcon class="h-5 w-5 text-red-400" />
                <div class="ml-3">
                  <h3 class="text-sm font-medium text-red-800 dark:text-red-200">
                    Token Non Valido
                  </h3>
                  <div class="mt-2 text-sm text-red-700 dark:text-red-300">
                    <p>Il link per il reset della password non è valido o è scaduto. Richiedi un nuovo link.</p>
                  </div>
                  <div class="mt-4">
                    <router-link to="/login"
                      class="text-sm font-medium text-red-800 dark:text-red-200 hover:text-red-700 dark:hover:text-red-100 underline">
                      Torna al Login
                    </router-link>
                  </div>
                </div>
              </div>
            </div>

            <form v-else @submit.prevent="handleSubmit" class="space-y-6">
              <!-- Password Field -->
              <div>
                <label for="new-password" class="label">
                  Nuova Password
                </label>
                <div class="mt-1 relative">
                  <input id="new-password" v-model="form.newPassword" :type="showPassword ? 'text' : 'password'"
                    autocomplete="new-password" required autofocus class="input pr-10"
                    :class="{ 'input-error': errors.newPassword }" placeholder="Inserisci la nuova password"
                    @input="validateField('newPassword')" />
                  <button type="button" @click="togglePasswordVisibility"
                    class="absolute inset-y-0 right-0 pr-3 flex items-center">
                    <EyeIcon v-if="!showPassword"
                      class="h-5 w-5 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400" />
                    <EyeSlashIcon v-else
                      class="h-5 w-5 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400" />
                  </button>
                </div>
                <p v-if="errors.newPassword" class="error-message">
                  {{ errors.newPassword }}
                </p>

                <!-- Password Strength Indicator -->
                <div v-if="form.newPassword" class="mt-2">
                  <div class="text-xs text-gray-600 dark:text-gray-400 mb-1">
                    Sicurezza password:
                  </div>
                  <div class="flex space-x-1">
                    <div v-for="i in 5" :key="i" class="h-1 flex-1 rounded" :class="[
                        i <= passwordStrength.score
                          ? getStrengthColor(passwordStrength.score)
                          : 'bg-gray-200 dark:bg-gray-700'
                      ]" />
                  </div>
                  <div v-if="passwordStrength.feedback.length > 0"
                    class="mt-1 text-xs text-gray-600 dark:text-gray-400">
                    <ul class="list-disc list-inside space-y-0.5">
                      <li v-for="tip in passwordStrength.feedback" :key="tip">{{ tip }}</li>
                    </ul>
                  </div>
                </div>
              </div>

              <!-- Confirm Password Field -->
              <div>
                <label for="confirm-password" class="label">
                  Conferma Nuova Password
                </label>
                <div class="mt-1 relative">
                  <input id="confirm-password" v-model="form.confirmPassword"
                    :type="showConfirmPassword ? 'text' : 'password'" autocomplete="new-password" required
                    class="input pr-10" :class="{ 'input-error': errors.confirmPassword }"
                    placeholder="Conferma la nuova password" @input="validateField('confirmPassword')" />
                  <button type="button" @click="toggleConfirmPasswordVisibility"
                    class="absolute inset-y-0 right-0 pr-3 flex items-center">
                    <EyeIcon v-if="!showConfirmPassword"
                      class="h-5 w-5 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400" />
                    <EyeSlashIcon v-else
                      class="h-5 w-5 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400" />
                  </button>
                </div>
                <p v-if="errors.confirmPassword" class="error-message">
                  {{ errors.confirmPassword }}
                </p>
              </div>

              <!-- Submit Button -->
              <div>
                <button type="submit" :disabled="isLoading || !isFormValid" class="btn btn-primary w-full">
                  <div v-if="isLoading" class="loading-spinner"></div>
                  {{ isLoading ? 'Reimpostazione in corso...' : 'Reimposta Password' }}
                </button>
              </div>

              <!-- General Error Message -->
              <div v-if="generalError" class="rounded-md bg-red-50 dark:bg-red-900/20 p-4">
                <div class="flex">
                  <ExclamationTriangleIcon class="h-5 w-5 text-red-400" />
                  <div class="ml-3">
                    <p class="text-sm font-medium text-red-800 dark:text-red-200">
                      {{ generalError }}
                    </p>
                  </div>
                </div>
              </div>
            </form>
          </div>

          <!-- Back to Login Link -->
          <div v-if="!isSuccess">
            <div class="mt-6 text-center">
            <router-link to="/login"
              class="text-sm font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300">
              ← Torna al Login
            </router-link>
          </div>
        </div>

        <!-- Theme Toggle -->
        <div class="mt-6 flex justify-center">
          <button @click="toggleTheme"
            class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 rounded-md"
            :title="`Cambia tema: ${themeName}`">
            <SunIcon v-if="isDarkMode" class="h-5 w-5" />
            <MoonIcon v-else class="h-5 w-5" />
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Footer -->
  <footer class="mt-8 text-center text-xs text-gray-500 dark:text-gray-400">
    <p class="mt-1">
      <a href="#" class="hover:text-gray-600 dark:hover:text-gray-300">Privacy</a>
      ·
      <a href="#" class="hover:text-gray-600 dark:hover:text-gray-300">Termini</a>
    </p>
  </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  EyeIcon,
  EyeSlashIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  SunIcon,
  MoonIcon
} from '@heroicons/vue/24/outline'
import { useTheme } from '@/composables/useTheme'
import { useCustomToast } from '@/composables/useCustomToast'
import { validateResetPasswordForm, checkPasswordStrength } from '@/utils/validators'
import { authApi } from '@/services/authApi'
import type { ResetPasswordFormData } from '@/types/auth'

// Composables
const route = useRoute()
const router = useRouter()
const { isDarkMode, themeName, toggleTheme } = useTheme()
const { showSuccess } = useCustomToast()

// Form state
const form = ref<ResetPasswordFormData>({
  newPassword: '',
  confirmPassword: ''
})

const errors = ref<Record<string, string>>({})
const generalError = ref<string>('')
const isLoading = ref(false)
const isSuccess = ref(false)
const invalidToken = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)

// Get token from URL
const token = computed(() => route.query.token as string)

// Password strength
const passwordStrength = computed(() => {
  return checkPasswordStrength(form.value.newPassword)
})

// Computed
const isFormValid = computed(() => {
  return form.value.newPassword !== '' &&
         form.value.confirmPassword !== '' &&
         Object.keys(errors.value).length === 0 &&
         passwordStrength.value.isStrong
})

// Methods
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const toggleConfirmPasswordVisibility = () => {
  showConfirmPassword.value = !showConfirmPassword.value
}

const getStrengthColor = (score: number) => {
  if (score >= 4) return 'bg-green-500'
  if (score >= 3) return 'bg-yellow-500'
  if (score >= 2) return 'bg-orange-500'
  return 'bg-red-500'
}

const validateField = (field: keyof ResetPasswordFormData) => {
  const validation = validateResetPasswordForm(form.value)

  if (validation.errors[field]) {
    errors.value[field] = validation.errors[field]
  } else {
    delete errors.value[field]
  }
}

const validateForm = () => {
  const validation = validateResetPasswordForm(form.value)
  errors.value = validation.errors
  return validation.isValid
}

const handleSubmit = async () => {
  if (isLoading.value || !validateForm()) {
    return
  }

  if (!passwordStrength.value.isStrong) {
    generalError.value = 'La password non rispetta i criteri di sicurezza richiesti'
    return
  }

  isLoading.value = true
  generalError.value = ''

  try {
    const response = await authApi.resetPassword({
      token: token.value,
      newPassword: form.value.newPassword
    })

    if (response.success) {
      isSuccess.value = true
      showSuccess('Password reimpostata con successo!')
    } else {
      generalError.value = response.message || 'Errore durante la reimpostazione della password'
    }
  } catch (error: any) {
    console.error('Reset password error:', error)

    // Check if token is invalid or expired
    if (error.response?.status === 400 ||
        error.response?.data?.message?.includes('token') ||
        error.response?.data?.message?.includes('scaduto')) {
      invalidToken.value = true
    } else {
      generalError.value = error.response?.data?.message || error.message || 'Errore durante la reimpostazione della password'
    }
  } finally {
    isLoading.value = false
  }
}

// Lifecycle
onMounted(() => {
  // Check if token is present
  if (!token.value) {
    console.error('No reset token provided')
    invalidToken.value = true
    return
  }

  // Focus password field
  const passwordInput = document.getElementById('new-password')
  if (passwordInput) {
    passwordInput.focus()
  }

  // Set page title
  document.title = 'Reimposta Password - P-Cal'
})
</script>