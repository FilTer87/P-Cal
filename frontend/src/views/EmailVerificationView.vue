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
              Email Verificata!
            </h2>
            <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
              La tua email è stata verificata con successo. Ora puoi effettuare il login.
            </p>
            <div class="mt-6">
              <router-link to="/login" class="btn btn-primary w-full">
                Vai al Login
              </router-link>
            </div>
          </div>

          <!-- Invalid Token Error -->
          <div v-else-if="invalidToken" class="text-center">
            <div
              class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100 dark:bg-red-900/20">
              <ExclamationTriangleIcon class="h-6 w-6 text-red-600 dark:text-red-400" />
            </div>
            <h2 class="mt-4 text-2xl font-bold text-gray-900 dark:text-white">
              Token Non Valido
            </h2>
            <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
              Il link di verifica non è valido o è scaduto. Puoi richiedere un nuovo link.
            </p>
            <div class="mt-6 space-y-3">
              <button @click="showResendForm = true" class="btn btn-primary w-full">
                Richiedi Nuovo Link
              </button>
              <router-link to="/login" class="btn btn-secondary w-full">
                Torna al Login
              </router-link>
            </div>
          </div>

          <!-- Loading State -->
          <div v-else-if="isLoading" class="text-center">
            <div class="flex justify-center">
              <div class="loading-spinner-lg"></div>
            </div>
            <h2 class="mt-4 text-xl font-semibold text-gray-900 dark:text-white">
              Verifica in corso...
            </h2>
            <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
              Attendere mentre verifichiamo il tuo indirizzo email.
            </p>
          </div>

          <!-- Resend Form -->
          <div v-else-if="showResendForm">
            <h2 class="mb-6 text-center text-2xl font-bold text-gray-900 dark:text-white">
              Richiedi Nuovo Link
            </h2>

            <form @submit.prevent="handleResend" class="space-y-4">
              <div>
                <label for="email" class="label">
                  Indirizzo Email
                </label>
                <input
                  id="email"
                  v-model="resendEmail"
                  type="email"
                  required
                  autofocus
                  class="input mt-1"
                  placeholder="tuo@email.com"
                />
              </div>

              <button type="submit" :disabled="isResending || !resendEmail" class="btn btn-primary w-full">
                <div v-if="isResending" class="loading-spinner"></div>
                {{ isResending ? 'Invio in corso...' : 'Invia Link' }}
              </button>

              <button type="button" @click="showResendForm = false" class="btn btn-secondary w-full">
                Annulla
              </button>
            </form>

            <!-- Resend Error Message -->
            <div v-if="resendError" class="mt-4 rounded-md bg-red-50 dark:bg-red-900/20 p-4">
              <div class="flex">
                <ExclamationTriangleIcon class="h-5 w-5 text-red-400" />
                <div class="ml-3">
                  <p class="text-sm font-medium text-red-800 dark:text-red-200">
                    {{ resendError }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Resend Success Message -->
            <div v-if="resendSuccess" class="mt-4 rounded-md bg-green-50 dark:bg-green-900/20 p-4">
              <div class="flex">
                <CheckCircleIcon class="h-5 w-5 text-green-400" />
                <div class="ml-3">
                  <p class="text-sm font-medium text-green-800 dark:text-green-200">
                    {{ resendSuccess }}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <!-- General Error State -->
          <div v-else-if="generalError" class="text-center">
            <div
              class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100 dark:bg-red-900/20">
              <ExclamationTriangleIcon class="h-6 w-6 text-red-600 dark:text-red-400" />
            </div>
            <h2 class="mt-4 text-2xl font-bold text-gray-900 dark:text-white">
              Errore
            </h2>
            <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
              {{ generalError }}
            </p>
            <div class="mt-6">
              <router-link to="/login" class="btn btn-primary w-full">
                Torna al Login
              </router-link>
            </div>
          </div>
        </div>

        <!-- Back to Login Link -->
        <div v-if="!isSuccess && !invalidToken && !isLoading && !generalError" class="mt-6 text-center">
          <router-link to="/login"
            class="text-sm font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300">
            ← Torna al Login
          </router-link>
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
      <a href="#" class="hover:text-gray-600 dark:hover:text-gray-300">Terms</a>
    </p>
  </footer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ExclamationTriangleIcon,
  CheckCircleIcon,
  SunIcon,
  MoonIcon
} from '@heroicons/vue/24/outline'
import { useTheme } from '@/composables/useTheme'
import { useCustomToast } from '@/composables/useCustomToast'
import { authApi } from '@/services/authApi'

// Composables
const route = useRoute()
const router = useRouter()
const { isDarkMode, themeName, toggleTheme } = useTheme()
const { showSuccess, showError } = useCustomToast()

// State
const isLoading = ref(false)
const isSuccess = ref(false)
const invalidToken = ref(false)
const generalError = ref<string>('')
const showResendForm = ref(false)
const resendEmail = ref<string>('')
const isResending = ref(false)
const resendError = ref<string>('')
const resendSuccess = ref<string>('')

// Get token from URL
const token = computed(() => route.query.token as string)

// Methods
const verifyEmail = async () => {
  if (!token.value) {
    console.error('No verification token provided')
    invalidToken.value = true
    return
  }

  isLoading.value = true
  generalError.value = ''

  try {
    const response = await authApi.verifyEmail(token.value)

    if (response.success) {
      isSuccess.value = true
      showSuccess('Email verificata con successo!')
    } else {
      // Check if token is invalid or expired
      if (response.message?.includes('token') ||
          response.message?.includes('scaduto') ||
          response.message?.includes('non valido')) {
        invalidToken.value = true
      } else {
        generalError.value = response.message || 'Errore durante la verifica dell\'email'
      }
    }
  } catch (error: any) {
    console.error('Email verification error:', error)

    // Check if token is invalid or expired
    if (error.response?.status === 400 ||
        error.response?.data?.message?.includes('token') ||
        error.response?.data?.message?.includes('scaduto')) {
      invalidToken.value = true
    } else {
      generalError.value = error.response?.data?.message || error.message || 'Errore durante la verifica dell\'email'
    }
  } finally {
    isLoading.value = false
  }
}

const handleResend = async () => {
  if (!resendEmail.value || isResending.value) {
    return
  }

  isResending.value = true
  resendError.value = ''
  resendSuccess.value = ''

  try {
    const response = await authApi.resendVerification(resendEmail.value)

    if (response.success) {
      resendSuccess.value = response.message || 'Link di verifica inviato con successo! Controlla la tua email.'
      showSuccess('Link di verifica inviato!')

      // Reset form after 3 seconds
      setTimeout(() => {
        showResendForm.value = false
        resendEmail.value = ''
        resendSuccess.value = ''
      }, 3000)
    } else {
      resendError.value = response.message || 'Errore durante l\'invio del link di verifica'
    }
  } catch (error: any) {
    console.error('Resend verification error:', error)
    resendError.value = error.response?.data?.message || error.message || 'Errore durante l\'invio del link di verifica'
  } finally {
    isResending.value = false
  }
}

// Lifecycle
onMounted(() => {
  // Automatically start verification if token is present
  if (token.value) {
    verifyEmail()
  } else {
    // No token, show resend form
    showResendForm.value = true
  }

  // Set page title
  document.title = 'Verifica Email - P-Cal'
})
</script>

<style scoped>
.loading-spinner-lg {
  border: 4px solid rgba(59, 130, 246, 0.1);
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  width: 48px;
  height: 48px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
