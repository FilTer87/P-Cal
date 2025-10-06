<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
    <div class="sm:mx-auto sm:w-full sm:max-w-md">
      <div class="text-center">
        <h1 class="text-4xl font-bold text-gray-900 dark:text-white">P-Cal</h1>
        <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
          {{ $t('auth.tagline') }}
        </p>
      </div>
      
      <div class="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div class="bg-white dark:bg-gray-800 py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <h2 class="mb-6 text-center text-3xl font-extrabold text-gray-900 dark:text-white">
            {{ $t('auth.registerTitle') }}
          </h2>
          
          <form @submit.prevent="handleSubmit" class="space-y-6">
            <!-- Username Field -->
            <div>
              <label for="username" class="label">
                {{ $t('auth.username') }} <span class="text-red-500">*</span>
              </label>
              <div class="mt-1">
                <input
                  id="username"
                  v-model="form.username"
                  @blur="validateField('username')"
                  type="text"
                  autocomplete="username"
                  required
                  class="input"
                  :class="{ 'input-error': errors.username }"
                  :placeholder="$t('auth.usernamePlaceholder')"
                />
              </div>
              <p v-if="errors.username" class="error-message">
                {{ errors.username }}
              </p>
            </div>

            <!-- Email Field -->
            <div>
              <label for="email" class="label">
                {{ $t('auth.email') }} <span class="text-red-500">*</span>
              </label>
              <div class="mt-1">
                <input
                  id="email"
                  v-model="form.email"
                  @blur="validateField('email')"
                  type="email"
                  autocomplete="email"
                  required
                  class="input"
                  :class="{ 'input-error': errors.email }"
                  :placeholder="$t('auth.emailPlaceholder')"
                />
              </div>
              <p v-if="errors.email" class="error-message">
                {{ errors.email }}
              </p>
            </div>

            <!-- Name Fields Row -->
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <!-- First Name -->
              <div>
                <label for="firstName" class="label">
                  {{ $t('auth.firstName') }}
                </label>
                <div class="mt-1">
                  <input
                    id="firstName"
                    v-model="form.firstName"
                    @blur="validateField('firstName')"
                    type="text"
                    autocomplete="given-name"
                    class="input"
                    :class="{ 'input-error': errors.firstName }"
                    :placeholder="$t('auth.firstNamePlaceholder')"
                  />
                </div>
                <p v-if="errors.firstName" class="error-message">
                  {{ errors.firstName }}
                </p>
              </div>

              <!-- Last Name -->
              <div>
                <label for="lastName" class="label">
                  {{ $t('auth.lastName') }}
                </label>
                <div class="mt-1">
                  <input
                    id="lastName"
                    v-model="form.lastName"
                    @blur="validateField('lastName')"
                    type="text"
                    autocomplete="family-name"
                    class="input"
                    :class="{ 'input-error': errors.lastName }"
                    :placeholder="$t('auth.lastNamePlaceholder')"
                  />
                </div>
                <p v-if="errors.lastName" class="error-message">
                  {{ errors.lastName }}
                </p>
              </div>
            </div>

            <!-- Password Field -->
            <div>
              <label for="password" class="label">
                {{ $t('auth.password') }} <span class="text-red-500">*</span>
              </label>
              <div class="mt-1 relative">
                <input
                  id="password"
                  v-model="form.password"
                  @blur="validateField('password')"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="new-password"
                  required
                  class="input pr-10"
                  :class="{ 'input-error': errors.password }"
                  :placeholder="$t('auth.passwordPlaceholder')"
                />
                <button
                  type="button"
                  @click="togglePasswordVisibility"
                  class="absolute inset-y-0 right-0 pr-3 flex items-center"
                >
                  <EyeIcon
                    v-if="!showPassword"
                    class="h-5 w-5 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400"
                  />
                  <EyeSlashIcon
                    v-else
                    class="h-5 w-5 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400"
                  />
                </button>
              </div>
              <p v-if="errors.password" class="error-message">
                {{ errors.password }}
              </p>

              <!-- Password Strength Indicator -->
              <PasswordStrengthIndicator :password="form.password" />
            </div>

            <!-- Confirm Password Field -->
            <div>
              <label for="confirmPassword" class="label">
                {{ $t('auth.confirmPassword') }} <span class="text-red-500">*</span>
              </label>
              <div class="mt-1 relative">
                <input
                  id="confirmPassword"
                  v-model="form.confirmPassword"
                  @blur="validateField('confirmPassword')"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  autocomplete="new-password"
                  required
                  class="input pr-10"
                  :class="{ 'input-error': errors.confirmPassword }"
                  :placeholder="$t('auth.confirmPasswordPlaceholder')"
                />
                <button
                  type="button"
                  @click="toggleConfirmPasswordVisibility"
                  class="absolute inset-y-0 right-0 pr-3 flex items-center"
                >
                  <EyeIcon
                    v-if="!showConfirmPassword"
                    class="h-5 w-5 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400"
                  />
                  <EyeSlashIcon
                    v-else
                    class="h-5 w-5 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400"
                  />
                </button>
              </div>
              <p v-if="errors.confirmPassword" class="error-message">
                {{ errors.confirmPassword }}
              </p>
            </div>

            <!-- Terms Acceptance -->
            <!-- <div class="flex items-start">
              <div class="flex items-center h-5">
                <input
                  id="acceptTerms"
                  v-model="form.acceptTerms"
                  type="checkbox"
                  class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded dark:bg-gray-700 dark:border-gray-600"
                  :class="{ 'border-red-300': errors.acceptTerms }"
                />
              </div>
              <div class="ml-3 text-sm">
                <label for="acceptTerms" class="text-gray-600 dark:text-gray-300">
                  <span class="text-red-500">*</span> Accetto i 
                  <a href="#" class="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300">
                    Terms e condizioni
                  </a>
                  e l'
                  <a href="#" class="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300">
                    informativa sulla privacy
                  </a>
                </label>
              </div>
            </div>
            <p v-if="errors.acceptTerms" class="error-message ml-7">
              {{ errors.acceptTerms }}
            </p> -->

            <!-- Submit Button -->
            <div>
              <button
                type="submit"
                :disabled="isLoading || !isFormValid"
                class="btn btn-primary w-full"
                :class="{ 'opacity-50 cursor-not-allowed': !isFormValid }"
              >
                <div v-if="isLoading" class="loading-spinner"></div>
                {{ isLoading ? $t('auth.registering') : $t('auth.register') }}
              </button>
            </div>

            <!-- Email Verification Required Message -->
            <div v-if="requiresVerification" class="rounded-md bg-blue-50 dark:bg-blue-900/20 p-4 border border-blue-200 dark:border-blue-700">
              <div class="flex">
                <InformationCircleIcon class="h-5 w-5 text-blue-400" />
                <div class="ml-3">
                  <p class="text-sm font-medium text-blue-800 dark:text-blue-200">
                    {{ verificationMessage }}
                  </p>
                  <p class="mt-2 text-xs text-blue-700 dark:text-blue-300">
                    Controlla la tua casella di posta e clicca sul link di verifica per completare la registrazione.
                  </p>
                  <p class="mt-3 text-xs text-blue-700 dark:text-blue-300">
                    Non hai ricevuto l'email?
                    <router-link
                      to="/verify-email"
                      class="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300 underline"
                    >
                      Richiedi un nuovo invio
                    </router-link>
                  </p>
                </div>
              </div>
            </div>

            <!-- General Error Message -->
            <div v-else-if="generalError" class="rounded-md bg-red-50 dark:bg-red-900/20 p-4">
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

          <!-- Divider -->
          <div class="mt-6">
            <div class="relative">
              <div class="absolute inset-0 flex items-center">
                <div class="w-full border-t border-gray-300 dark:border-gray-600" />
              </div>
              <div class="relative flex justify-center text-sm">
                <span class="px-2 bg-white dark:bg-gray-800 text-gray-500 dark:text-gray-400">
                  {{ $t('auth.or') }}
                </span>
              </div>
            </div>
          </div>

          <!-- Login Link -->
          <div class="mt-6 text-center">
            <p class="text-sm text-gray-600 dark:text-gray-400">
              {{ $t("auth.hasAccount") }}
              <router-link
                to="/login"
                class="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300"
              >
                {{ $t("auth.login") }}
              </router-link>
            </p>
          </div>

          <!-- Theme Toggle -->
          <div class="mt-6 flex justify-center">
            <button
              @click="toggleTheme"
              class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 rounded-md"
              :title="$t('auth.changeTheme', { theme: themeName })"
            >
              <SunIcon v-if="isDarkMode" class="h-5 w-5" />
              <MoonIcon v-else class="h-5 w-5" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <footer class="mt-8 text-center text-xs text-gray-500 dark:text-gray-400">
      <!-- <p>© 2025 P-Cal. Tutti i diritti riservati.</p> -->
      <p class="mt-1">
        <a href="#" class="hover:text-gray-600 dark:hover:text-gray-300">Privacy</a>
        ·
        <a href="#" class="hover:text-gray-600 dark:hover:text-gray-300">Terms</a>
        <!-- ·
        <a href="#" class="hover:text-gray-600 dark:hover:text-gray-300">Supporto</a> -->
      </p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  EyeIcon,
  EyeSlashIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  SunIcon,
  MoonIcon
} from '@heroicons/vue/24/outline'
import { useAuth } from '../composables/useAuth'
import { useTheme } from '../composables/useTheme'
import { useCustomToast } from '../composables/useCustomToast'
import { validateRegistrationForm } from '../utils/validators'
import PasswordStrengthIndicator from '../components/Common/PasswordStrengthIndicator.vue'
import type { RegisterFormData } from '../types/auth'

// Composables
const { t } = useI18n()
const router = useRouter()
const { register, isLoading, requireGuest } = useAuth()
const { isDarkMode, themeName, toggleTheme } = useTheme()
const { showError } = useCustomToast()

// Form state
const form = ref<RegisterFormData>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  firstName: '',
  lastName: '',
  acceptTerms: false
})

const errors = ref<Record<string, string>>({})
const generalError = ref<string>('')
const requiresVerification = ref(false)
const verificationMessage = ref<string>('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)

// Computed
const isFormValid = computed(() => {
  return form.value.username.trim() !== '' &&
         form.value.email.trim() !== '' &&
         form.value.password !== '' &&
         form.value.confirmPassword !== '' &&
        //  form.value.acceptTerms &&
         Object.keys(errors.value).length === 0
})

// Methods
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const toggleConfirmPasswordVisibility = () => {
  showConfirmPassword.value = !showConfirmPassword.value
}

const validateForm = () => {
  const validation = validateRegistrationForm(form.value)
  errors.value = validation.errors
  return validation.isValid
}

const validateField = (field: keyof RegisterFormData) => {
  // Only validate if the field has been touched and has errors
  if (errors.value[field]) {
    validateForm()
  }
}

const handleSubmit = async () => {
  generalError.value = ''
  requiresVerification.value = false
  verificationMessage.value = ''

  // Validate form
  if (!validateForm()) {
    return
  }

  try {
    const success = await register({
      username: form.value.username.trim(),
      email: form.value.email.trim(),
      password: form.value.password,
      firstName: form.value.firstName.trim() || undefined,
      lastName: form.value.lastName.trim() || undefined
    })

    if (success) {
      // Redirect is handled by the auth composable
      return
    }

    generalError.value = t('auth.registrationError')
  } catch (error: any) {
    console.error('Registration error:', error)

    // Check if this is an email verification required response
    if (error.response?.data?.requiresEmailVerification) {
      requiresVerification.value = true
      verificationMessage.value = error.response.data.message || t('auth.registrationSuccess')
    } else if (error.response?.data?.message) {
      generalError.value = error.response.data.message
    } else if (error.response?.data?.errors) {
      // Handle field-specific errors from server
      const serverErrors = error.response.data.errors
      const fieldErrors: Record<string, string> = {}

      serverErrors.forEach((err: { field: string; message: string }) => {
        fieldErrors[err.field] = err.message
      })

      errors.value = { ...errors.value, ...fieldErrors }
    } else {
      generalError.value = error.message || t('auth.registrationError')
    }
  }
}

// Lifecycle
onMounted(async () => {
  // Ensure user is not already authenticated
  await requireGuest()
  
  // Focus username field
  const usernameInput = document.getElementById('username')
  if (usernameInput) {
    usernameInput.focus()
  }
})
</script>