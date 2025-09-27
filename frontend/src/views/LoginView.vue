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
          <h2 class="mb-6 text-center text-3xl font-extrabold text-gray-900 dark:text-white">
            Accedi al tuo account
          </h2>
          
          <form @submit.prevent="handleSubmit" class="space-y-6">
            <!-- Username Field -->
            <div>
              <label for="username" class="label">
                Nome utente
              </label>
              <div class="mt-1">
                <input
                  id="username"
                  v-model="form.username"
                  type="text"
                  autocomplete="username"
                  required
                  class="input"
                  :class="{ 'input-error': errors.username }"
                  placeholder="Inserisci il tuo nome utente"
                />
              </div>
              <p v-if="errors.username" class="error-message">
                {{ errors.username }}
              </p>
            </div>

            <!-- Password Field -->
            <div>
              <label for="password" class="label">
                Password
              </label>
              <div class="mt-1 relative">
                <input
                  id="password"
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="current-password"
                  required
                  class="input pr-10"
                  :class="{ 'input-error': errors.password }"
                  placeholder="Inserisci la tua password"
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
            </div>

            <!-- Remember Me -->
            <div class="flex items-center justify-between">
              <div class="flex items-center">
                <input
                  id="remember"
                  v-model="form.remember"
                  type="checkbox"
                  class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded dark:bg-gray-700 dark:border-gray-600"
                />
                <label for="remember" class="ml-2 block text-sm text-gray-900 dark:text-gray-300">
                  Ricordami
                </label>
              </div>

              <div class="text-sm">
                <a
                  href="#"
                  class="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300"
                  @click.prevent="showForgotPassword"
                >
                  Password dimenticata?
                </a>
              </div>
            </div>

            <!-- Submit Button -->
            <div>
              <button
                type="submit"
                :disabled="isLoading"
                class="btn btn-primary w-full"
              >
                <div v-if="isLoading" class="loading-spinner"></div>
                {{ isLoading ? 'Accesso in corso...' : 'Accedi' }}
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

          <!-- Divider -->
          <div class="mt-6">
            <div class="relative">
              <div class="absolute inset-0 flex items-center">
                <div class="w-full border-t border-gray-300 dark:border-gray-600" />
              </div>
              <div class="relative flex justify-center text-sm">
                <span class="px-2 bg-white dark:bg-gray-800 text-gray-500 dark:text-gray-400">
                  oppure
                </span>
              </div>
            </div>
          </div>

          <!-- Register Link -->
          <div class="mt-6 text-center">
            <p class="text-sm text-gray-600 dark:text-gray-400">
              Non hai un account?
              <router-link
                to="/register"
                class="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300"
              >
                Registrati
              </router-link>
            </p>
          </div>

          <!-- Theme Toggle -->
          <div class="mt-6 flex justify-center">
            <button
              @click="toggleTheme"
              class="p-2 text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 rounded-md"
              :title="`Cambia tema: ${themeName}`"
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
        <a href="#" class="hover:text-gray-600 dark:hover:text-gray-300">Termini</a>
        <!-- ·
        <a href="#" class="hover:text-gray-600 dark:hover:text-gray-300">Supporto</a> --> <!-- TODO -->
      </p>
    </footer>
    <!-- Two-Factor Verify Modal -->
    <TwoFactorVerifyModal
      v-model="showTwoFactorModal"
      @verify="handleTwoFactorVerify"
      @cancel="handleTwoFactorCancel"
      ref="twoFactorModalRef"
    />

    <!-- Forgot Password Modal -->
    <ForgotPasswordModal
      v-model="showForgotPasswordModal"
      @success="handleForgotPasswordSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  EyeIcon,
  EyeSlashIcon,
  ExclamationTriangleIcon,
  SunIcon,
  MoonIcon
} from '@heroicons/vue/24/outline'
import { useAuth } from '../composables/useAuth'
import { useTheme } from '../composables/useTheme'
import { useCustomToast } from '../composables/useCustomToast'
import { validateLoginForm } from '../utils/validators'
import { authApi } from '../services/authApi'
import TwoFactorVerifyModal from '../components/Auth/TwoFactorVerifyModal.vue'
import ForgotPasswordModal from '../components/Auth/ForgotPasswordModal.vue'
import type { LoginFormData } from '../types/auth'

// Composables
const router = useRouter()
const { login, isLoading, requireGuest } = useAuth()
const { isDarkMode, themeName, toggleTheme } = useTheme()
const { showError, showSuccess } = useCustomToast()

// Form state
const form = ref<LoginFormData>({
  username: '',
  password: '',
  remember: false
})

const errors = ref<Record<string, string>>({})
const generalError = ref<string>('')
const showPassword = ref(false)
const showTwoFactorModal = ref(false)
const showForgotPasswordModal = ref(false)
const twoFactorModalRef = ref()
const pendingCredentials = ref<LoginFormData | null>(null)

// Computed
const isFormValid = computed(() => {
  return form.value.username.trim() !== '' && 
         form.value.password !== '' && 
         Object.keys(errors.value).length === 0
})

// Methods
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const showForgotPassword = () => {
  showForgotPasswordModal.value = true
}

const handleForgotPasswordSuccess = () => {
  showSuccess('Email di reset inviata! Controlla la tua casella di posta.')
}

const validateForm = () => {
  const validation = validateLoginForm({
    username: form.value.username,
    password: form.value.password
  })
  
  errors.value = validation.errors
  return validation.isValid
}

const handleSubmit = async () => {
  generalError.value = ''

  // Validate form
  if (!validateForm()) {
    return
  }

  try {
    // Store credentials for potential 2FA verification
    pendingCredentials.value = {
      username: form.value.username.trim(),
      password: form.value.password,
      remember: form.value.remember
    }

    const success = await login({
      username: form.value.username.trim(),
      password: form.value.password
    })

    if (success) {
      // Check if user has 2FA enabled
      // The login might succeed partially and require 2FA verification
      // This will be handled by checking the response
      return
    }

    generalError.value = 'Nome utente o password non corretti'
  } catch (error: any) {
    console.error('Login error:', error)

    // Check if error is related to 2FA requirement
    if (error.message === '2FA_REQUIRED' ||
        error.response?.status === 202 ||
        error.response?.data?.requiresTwoFactor) {
      showTwoFactorModal.value = true
      return
    }

    generalError.value = error.message || 'Errore durante l\'accesso'
  }
}

const handleTwoFactorVerify = async (code: string) => {
  if (!twoFactorModalRef.value || !pendingCredentials.value) return

  try {
    twoFactorModalRef.value.setLoading(true)
    twoFactorModalRef.value.setError('')

    // Complete login with 2FA code
    const success = await login({
      username: pendingCredentials.value.username,
      password: pendingCredentials.value.password,
      twoFactorCode: code
    })

    if (success) {
      showTwoFactorModal.value = false
      pendingCredentials.value = null
    } else {
      twoFactorModalRef.value.setError('Codice di verifica non valido')
    }
  } catch (error: any) {
    console.error('2FA verification error:', error)
    twoFactorModalRef.value.setError(
      error.response?.data?.message || 'Codice di verifica non valido'
    )
  } finally {
    twoFactorModalRef.value.setLoading(false)
  }
}

const handleTwoFactorCancel = () => {
  showTwoFactorModal.value = false
  pendingCredentials.value = null
  generalError.value = ''
}

// Real-time validation
const validateField = (field: keyof LoginFormData) => {
  if (errors.value[field]) {
    validateForm()
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