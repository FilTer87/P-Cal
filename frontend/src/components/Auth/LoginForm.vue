<template>
  <div class="login-form">
    <div class="form-header">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">
        Accedi al tuo account
      </h2>
      <p class="text-gray-600 dark:text-gray-400">
        Inserisci le tue credenziali per accedere a PrivateCal
      </p>
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-6">
      <!-- Email/Username Input -->
      <div>
        <label 
          for="username" 
          class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
        >
          Email o Nome Utente
        </label>
        <div class="relative">
          <input
            id="username"
            v-model="form.username"
            type="text"
            autocomplete="username"
            required
            :disabled="isLoading"
            :class="[
              'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.username 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="inserisci email o nome utente"
            @blur="validateField('username')"
            @input="clearFieldError('username')"
          />
          <div 
            v-if="errors.username" 
            class="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none"
          >
            <svg class="h-5 w-5 text-red-500" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
          </div>
        </div>
        <p v-if="errors.username" class="mt-1 text-sm text-red-600 dark:text-red-400">
          {{ errors.username }}
        </p>
      </div>

      <!-- Password Input -->
      <div>
        <label 
          for="password" 
          class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
        >
          Password
        </label>
        <div class="relative">
          <input
            id="password"
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="current-password"
            required
            :disabled="isLoading"
            :class="[
              'w-full px-4 py-3 pr-12 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.password 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="inserisci la tua password"
            @blur="validateField('password')"
            @input="clearFieldError('password')"
          />
          <button
            type="button"
            @click="togglePasswordVisibility"
            class="absolute inset-y-0 right-0 pr-3 flex items-center"
            :disabled="isLoading"
          >
            <svg 
              v-if="showPassword" 
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
        <p v-if="errors.password" class="mt-1 text-sm text-red-600 dark:text-red-400">
          {{ errors.password }}
        </p>
      </div>

      <!-- Remember Me and Forgot Password -->
      <div class="flex items-center justify-between">
        <div class="flex items-center">
          <input
            id="remember"
            v-model="form.remember"
            type="checkbox"
            :disabled="isLoading"
            class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded disabled:opacity-50"
          />
          <label for="remember" class="ml-2 block text-sm text-gray-700 dark:text-gray-300">
            Ricordami
          </label>
        </div>
        <router-link 
          to="/forgot-password" 
          class="text-sm text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
        >
          Password dimenticata?
        </router-link>
      </div>

      <!-- Rate Limiting Warning -->
      <div 
        v-if="rateLimitInfo.isLimited" 
        class="p-4 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded-lg"
      >
        <div class="flex">
          <svg class="h-5 w-5 text-yellow-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
          </svg>
          <div class="ml-3">
            <p class="text-sm text-yellow-800 dark:text-yellow-200">
              Troppi tentativi di accesso. Riprova tra {{ Math.ceil(rateLimitInfo.resetTime / 60) }} minuti.
            </p>
          </div>
        </div>
      </div>

      <!-- General Error Message -->
      <div 
        v-if="generalError" 
        class="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700 rounded-lg"
      >
        <div class="flex">
          <svg class="h-5 w-5 text-red-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
          <div class="ml-3">
            <p class="text-sm text-red-800 dark:text-red-200">
              {{ generalError }}
            </p>
          </div>
        </div>
      </div>

      <!-- Submit Button -->
      <div>
        <button
          type="submit"
          :disabled="isLoading || rateLimitInfo.isLimited || !isFormValid"
          class="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <LoadingSpinner v-if="isLoading" class="w-5 h-5 mr-2" />
          {{ isLoading ? 'Accesso in corso...' : 'Accedi' }}
        </button>
      </div>
    </form>

    <!-- Social Login Section -->
    <div v-if="showSocialLogin" class="mt-6">
      <div class="relative">
        <div class="absolute inset-0 flex items-center">
          <div class="w-full border-t border-gray-300 dark:border-gray-600" />
        </div>
        <div class="relative flex justify-center text-sm">
          <span class="px-2 bg-white dark:bg-gray-900 text-gray-500 dark:text-gray-400">
            Oppure accedi con
          </span>
        </div>
      </div>

      <div class="mt-6 grid grid-cols-2 gap-3">
        <button
          type="button"
          @click="handleSocialLogin('google')"
          :disabled="isLoading"
          class="w-full inline-flex justify-center py-2 px-4 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm bg-white dark:bg-gray-800 text-sm font-medium text-gray-500 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <svg class="h-5 w-5" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          <span class="ml-2">Google</span>
        </button>

        <button
          type="button"
          @click="handleSocialLogin('github')"
          :disabled="isLoading"
          class="w-full inline-flex justify-center py-2 px-4 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm bg-white dark:bg-gray-800 text-sm font-medium text-gray-500 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 0C4.477 0 0 4.484 0 10.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0110 4.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.203 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.942.359.31.678.921.678 1.856 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0020 10.017C20 4.484 15.522 0 10 0z" clip-rule="evenodd" />
          </svg>
          <span class="ml-2">GitHub</span>
        </button>
      </div>
    </div>

    <!-- Register Link -->
    <div class="mt-6 text-center">
      <p class="text-sm text-gray-600 dark:text-gray-400">
        Non hai ancora un account?
        <router-link 
          to="/register" 
          class="font-medium text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300"
        >
          Registrati qui
        </router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useNotifications } from '@/composables/useNotifications'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'
import type { LoginFormData } from '@/types/auth'

// Props
interface Props {
  showSocialLogin?: boolean
  redirectTo?: string
}

const props = withDefaults(defineProps<Props>(), {
  showSocialLogin: true,
  redirectTo: '/'
})

// Composables
const { login, isLoading } = useAuth()
const { showError, showSuccess } = useNotifications()

// Form state
const form = reactive<LoginFormData>({
  username: '',
  password: '',
  remember: false
})

// Validation state
const errors = reactive({
  username: '',
  password: ''
})

// Component state
const showPassword = ref(false)
const generalError = ref('')
const loginAttempts = ref(0)
const rateLimitInfo = reactive({
  isLimited: false,
  resetTime: 0
})

// Rate limiting
const MAX_ATTEMPTS = 5
const LOCKOUT_DURATION = 15 * 60 // 15 minutes in seconds
let rateLimitTimer: NodeJS.Timeout | null = null

// Computed properties
const isFormValid = computed(() => {
  return form.username.trim().length > 0 && 
         form.password.trim().length > 0 && 
         !errors.username && 
         !errors.password
})

// Methods
const validateField = (field: keyof typeof errors) => {
  switch (field) {
    case 'username':
      if (!form.username.trim()) {
        errors.username = 'Il campo email/username è obbligatorio'
      } else if (form.username.trim().length < 3) {
        errors.username = 'Il campo deve contenere almeno 3 caratteri'
      }
      break
    
    case 'password':
      if (!form.password.trim()) {
        errors.password = 'Il campo password è obbligatorio'
      } else if (form.password.length < 6) {
        errors.password = 'La password deve contenere almeno 6 caratteri'
      }
      break
  }
}

const clearFieldError = (field: keyof typeof errors) => {
  errors[field] = ''
  if (generalError.value) {
    generalError.value = ''
  }
}

const validateForm = (): boolean => {
  validateField('username')
  validateField('password')
  return isFormValid.value
}

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const updateRateLimitInfo = () => {
  const storedAttempts = localStorage.getItem('loginAttempts')
  const storedTimestamp = localStorage.getItem('loginLockoutTime')
  
  if (storedAttempts) {
    loginAttempts.value = parseInt(storedAttempts, 10)
  }
  
  if (storedTimestamp) {
    const lockoutTime = parseInt(storedTimestamp, 10)
    const now = Date.now()
    const timeRemaining = lockoutTime - now
    
    if (timeRemaining > 0) {
      rateLimitInfo.isLimited = true
      rateLimitInfo.resetTime = Math.ceil(timeRemaining / 1000)
      
      rateLimitTimer = setInterval(() => {
        rateLimitInfo.resetTime -= 1
        if (rateLimitInfo.resetTime <= 0) {
          resetRateLimit()
        }
      }, 1000)
    } else {
      resetRateLimit()
    }
  }
}

const resetRateLimit = () => {
  loginAttempts.value = 0
  rateLimitInfo.isLimited = false
  rateLimitInfo.resetTime = 0
  localStorage.removeItem('loginAttempts')
  localStorage.removeItem('loginLockoutTime')
  
  if (rateLimitTimer) {
    clearInterval(rateLimitTimer)
    rateLimitTimer = null
  }
}

const handleFailedAttempt = () => {
  loginAttempts.value += 1
  localStorage.setItem('loginAttempts', loginAttempts.value.toString())
  
  if (loginAttempts.value >= MAX_ATTEMPTS) {
    const lockoutTime = Date.now() + (LOCKOUT_DURATION * 1000)
    localStorage.setItem('loginLockoutTime', lockoutTime.toString())
    updateRateLimitInfo()
  }
}

const handleSubmit = async () => {
  if (!validateForm() || rateLimitInfo.isLimited) {
    return
  }

  try {
    generalError.value = ''
    
    const success = await login({
      username: form.username.trim(),
      password: form.password
    })
    
    if (success) {
      // Reset rate limiting on successful login
      resetRateLimit()
      
      // Store remember me preference
      if (form.remember) {
        localStorage.setItem('rememberLogin', 'true')
      }
      
      showSuccess('Accesso effettuato con successo!')
    } else {
      handleFailedAttempt()
      generalError.value = 'Credenziali non valide. Verifica email/username e password.'
    }
  } catch (error: any) {
    console.error('Login error:', error)
    handleFailedAttempt()
    
    if (error.response?.status === 429) {
      generalError.value = 'Troppi tentativi di accesso. Riprova più tardi.'
    } else if (error.response?.status === 401) {
      generalError.value = 'Credenziali non valide. Verifica email/username e password.'
    } else if (error.response?.status === 403) {
      generalError.value = 'Account temporaneamente bloccato. Contatta il supporto.'
    } else if (error.response?.data?.message) {
      generalError.value = error.response.data.message
    } else {
      generalError.value = 'Errore durante l\'accesso. Riprova più tardi.'
    }
  }
}

const handleSocialLogin = async (provider: 'google' | 'github') => {
  try {
    // Implement social login logic here
    showError(`Login con ${provider} non ancora implementato`)
  } catch (error) {
    console.error('Social login error:', error)
    showError(`Errore durante il login con ${provider}`)
  }
}

// Auto-focus first field
const focusFirstField = () => {
  const firstInput = document.getElementById('username')
  if (firstInput) {
    firstInput.focus()
  }
}

// Load saved username if remember me was checked
const loadSavedCredentials = () => {
  const rememberLogin = localStorage.getItem('rememberLogin')
  const savedUsername = localStorage.getItem('savedUsername')
  
  if (rememberLogin === 'true' && savedUsername) {
    form.username = savedUsername
    form.remember = true
  }
}

// Save username if remember me is checked
const saveCredentials = () => {
  if (form.remember && form.username.trim()) {
    localStorage.setItem('savedUsername', form.username.trim())
  } else {
    localStorage.removeItem('savedUsername')
    localStorage.removeItem('rememberLogin')
  }
}

// Lifecycle
onMounted(() => {
  loadSavedCredentials()
  updateRateLimitInfo()
  focusFirstField()
})

onUnmounted(() => {
  if (rateLimitTimer) {
    clearInterval(rateLimitTimer)
  }
  saveCredentials()
})

// Keyboard shortcuts
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSubmit()
  }
}

// Add event listener for keyboard shortcuts
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.login-form {
  @apply max-w-md mx-auto;
}

.form-header {
  @apply text-center mb-8;
}

/* Custom focus styles for better accessibility */
input:focus {
  @apply outline-none ring-2 ring-blue-500 ring-offset-2 dark:ring-offset-gray-900;
}

button:focus {
  @apply outline-none ring-2 ring-blue-500 ring-offset-2 dark:ring-offset-gray-900;
}

/* Animation for error messages */
.error-message {
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Loading spinner animation */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* Dark mode improvements */
@media (prefers-color-scheme: dark) {
  input:-webkit-autofill {
    -webkit-box-shadow: 0 0 0 1000px #374151 inset;
    -webkit-text-fill-color: #f3f4f6;
  }
}

/* Mobile responsiveness */
@media (max-width: 640px) {
  .login-form {
    @apply mx-4;
  }
  
  .form-header h2 {
    @apply text-xl;
  }
  
  input, button {
    @apply py-3;
  }
}
</style>