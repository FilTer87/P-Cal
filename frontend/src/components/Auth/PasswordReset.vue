<template>
  <div class="password-reset">
    <div class="form-header">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">
        {{ currentStep === 'request' ? 'Reimposta Password' : 'Nuova Password' }}
      </h2>
      <p class="text-gray-600 dark:text-gray-400">
        {{ 
          currentStep === 'request' 
            ? 'Inserisci la tua email per ricevere il link di reimpostazione'
            : currentStep === 'reset'
              ? 'Inserisci la tua nuova password'
              : 'Controlla la tua email per il link di reimpostazione'
        }}
      </p>
    </div>

    <!-- Step 1: Request Password Reset -->
    <form v-if="currentStep === 'request'" @submit.prevent="handlePasswordResetRequest" class="space-y-6">
      <!-- Email Input -->
      <div>
        <label 
          for="email" 
          class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
        >
          Indirizzo Email *
        </label>
        <div class="relative">
          <input
            id="email"
            v-model="requestForm.email"
            type="email"
            autocomplete="email"
            required
            :disabled="isLoading"
            :class="[
              'w-full px-4 py-3 pr-10 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.email 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="inserisci la tua email"
            @blur="validateEmail"
            @input="clearEmailError"
          />
          <div 
            v-if="errors.email" 
            class="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none"
          >
            <svg class="h-5 w-5 text-red-500" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
          </div>
        </div>
        <p v-if="errors.email" class="mt-1 text-sm text-red-600 dark:text-red-400">
          {{ errors.email }}
        </p>
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
              Hai raggiunto il limite di richieste. Riprova tra {{ Math.ceil(rateLimitInfo.resetTime / 60) }} minuti.
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
          :disabled="isLoading || rateLimitInfo.isLimited || !requestForm.email.trim()"
          class="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <LoadingSpinner v-if="isLoading" class="w-5 h-5 mr-2" />
          {{ isLoading ? 'Invio in corso...' : 'Invia Link di Reimpostazione' }}
        </button>
      </div>
    </form>

    <!-- Step 2: Success Message -->
    <div v-else-if="currentStep === 'sent'" class="text-center space-y-6">
      <!-- Success Icon -->
      <div class="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-green-100 dark:bg-green-900/20">
        <svg class="h-8 w-8 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 4.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
        </svg>
      </div>

      <!-- Success Message -->
      <div class="space-y-3">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
          Email Inviata!
        </h3>
        <p class="text-gray-600 dark:text-gray-400">
          Abbiamo inviato un link per reimpostare la password a:
        </p>
        <p class="font-medium text-blue-600 dark:text-blue-400">
          {{ requestForm.email }}
        </p>
        <p class="text-sm text-gray-500 dark:text-gray-400">
          Il link sarà valido per 1 ora. Controlla anche nella cartella spam se non vedi l'email.
        </p>
      </div>

      <!-- Actions -->
      <div class="space-y-3">
        <button
          @click="openEmailClient"
          class="w-full flex justify-center items-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 4.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
          Apri Client Email
        </button>
        
        <button
          @click="resendEmail"
          :disabled="isLoading || resendCooldown > 0"
          class="w-full flex justify-center items-center py-3 px-4 border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <LoadingSpinner v-if="isLoading" class="w-5 h-5 mr-2" />
          {{ 
            isLoading 
              ? 'Invio in corso...' 
              : resendCooldown > 0 
                ? `Reinvia tra ${resendCooldown}s`
                : 'Reinvia Email'
          }}
        </button>
      </div>
    </div>

    <!-- Step 3: Reset Password Form -->
    <form v-else-if="currentStep === 'reset'" @submit.prevent="handlePasswordReset" class="space-y-6">
      <!-- New Password Input -->
      <div>
        <label 
          for="newPassword" 
          class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
        >
          Nuova Password *
        </label>
        <div class="relative">
          <input
            id="newPassword"
            v-model="resetForm.newPassword"
            :type="showNewPassword ? 'text' : 'password'"
            autocomplete="new-password"
            required
            :disabled="isLoading"
            :class="[
              'w-full px-4 py-3 pr-12 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.newPassword 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="inserisci la nuova password"
            @input="onPasswordInput"
            @blur="validateNewPassword"
          />
          <button
            type="button"
            @click="toggleNewPasswordVisibility"
            class="absolute inset-y-0 right-0 pr-3 flex items-center"
            :disabled="isLoading"
          >
            <svg 
              v-if="showNewPassword" 
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
        <div v-if="resetForm.newPassword" class="mt-2">
          <div class="flex items-center space-x-2">
            <span class="text-sm text-gray-500 dark:text-gray-400">Sicurezza:</span>
            <div class="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
              <div 
                :class="[
                  'h-2 rounded-full transition-all duration-300',
                  passwordStrength.level === 'weak' ? 'bg-red-500 w-1/4' :
                  passwordStrength.level === 'medium' ? 'bg-yellow-500 w-2/4' :
                  passwordStrength.level === 'good' ? 'bg-blue-500 w-3/4' :
                  passwordStrength.level === 'strong' ? 'bg-green-500 w-full' : 'bg-gray-300 w-0'
                ]"
              ></div>
            </div>
            <span 
              :class="[
                'text-sm font-medium',
                passwordStrength.level === 'weak' ? 'text-red-500' :
                passwordStrength.level === 'medium' ? 'text-yellow-500' :
                passwordStrength.level === 'good' ? 'text-blue-500' :
                passwordStrength.level === 'strong' ? 'text-green-500' : 'text-gray-400'
              ]"
            >
              {{ passwordStrength.text }}
            </span>
          </div>
          <ul v-if="passwordStrength.suggestions.length" class="mt-2 space-y-1">
            <li 
              v-for="suggestion in passwordStrength.suggestions" 
              :key="suggestion"
              class="text-xs text-gray-600 dark:text-gray-400 flex items-center"
            >
              <svg class="h-3 w-3 text-gray-400 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
              {{ suggestion }}
            </li>
          </ul>
        </div>
      </div>

      <!-- Confirm Password Input -->
      <div>
        <label 
          for="confirmPassword" 
          class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
        >
          Conferma Nuova Password *
        </label>
        <div class="relative">
          <input
            id="confirmPassword"
            v-model="resetForm.confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            autocomplete="new-password"
            required
            :disabled="isLoading"
            :class="[
              'w-full px-4 py-3 pr-12 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.confirmPassword 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : resetForm.confirmPassword && resetForm.newPassword === resetForm.confirmPassword
                  ? 'border-green-300 bg-green-50 dark:bg-green-900/20 dark:border-green-600'
                  : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="conferma la nuova password"
            @blur="validateConfirmPassword"
            @input="clearConfirmPasswordError"
          />
          <button
            type="button"
            @click="toggleConfirmPasswordVisibility"
            class="absolute inset-y-0 right-0 pr-3 flex items-center"
            :disabled="isLoading"
          >
            <svg 
              v-if="showConfirmPassword" 
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

      <!-- Token Validation Error -->
      <div 
        v-if="tokenError" 
        class="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700 rounded-lg"
      >
        <div class="flex">
          <svg class="h-5 w-5 text-red-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
          <div class="ml-3">
            <h3 class="text-sm font-medium text-red-800 dark:text-red-200">
              Token non valido o scaduto
            </h3>
            <p class="mt-1 text-sm text-red-700 dark:text-red-300">
              {{ tokenError }}
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
          :disabled="isLoading || !isResetFormValid"
          class="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <LoadingSpinner v-if="isLoading" class="w-5 h-5 mr-2" />
          {{ isLoading ? 'Reimpostazione...' : 'Reimposta Password' }}
        </button>
      </div>
    </form>

    <!-- Step 4: Success State -->
    <div v-else-if="currentStep === 'success'" class="text-center space-y-6">
      <!-- Success Icon -->
      <div class="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-green-100 dark:bg-green-900/20">
        <svg class="h-8 w-8 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
      </div>

      <!-- Success Message -->
      <div class="space-y-3">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
          Password Reimpostata!
        </h3>
        <p class="text-gray-600 dark:text-gray-400">
          La tua password è stata reimpostata con successo. Ora puoi accedere con la nuova password.
        </p>
      </div>

      <!-- Login Button -->
      <div>
        <router-link
          to="/login"
          class="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
        >
          Vai al Login
        </router-link>
      </div>
    </div>

    <!-- Back to Login Link (except on success) -->
    <div v-if="currentStep !== 'success'" class="mt-6 text-center">
      <router-link 
        to="/login" 
        class="text-sm text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 font-medium"
      >
        ← Torna al Login
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCustomToast } from '@/composables/useCustomToast'
import { authApi } from '@/services/authApi'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'

// Router
const route = useRoute()
const router = useRouter()

// Composables
const { showError, showSuccess } = useCustomToast()

// Component state
const currentStep = ref<'request' | 'sent' | 'reset' | 'success'>('request')
const isLoading = ref(false)
const resendCooldown = ref(0)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

// Forms
const requestForm = reactive({
  email: ''
})

const resetForm = reactive({
  token: '',
  newPassword: '',
  confirmPassword: ''
})

// Error states
const errors = reactive({
  email: '',
  newPassword: '',
  confirmPassword: ''
})

const generalError = ref('')
const tokenError = ref('')

// Rate limiting
const rateLimitInfo = reactive({
  isLimited: false,
  resetTime: 0
})

let rateLimitTimer: NodeJS.Timeout | null = null
let resendTimer: NodeJS.Timeout | null = null

// Password strength calculation
const passwordStrength = computed(() => {
  const password = resetForm.newPassword
  if (!password) return { level: '', text: '', suggestions: [] }

  let score = 0
  const suggestions = []

  // Length check
  if (password.length >= 8) {
    score += 1
  } else {
    suggestions.push('Almeno 8 caratteri')
  }

  // Complexity checks
  if (/[a-z]/.test(password)) score += 1
  else suggestions.push('Lettere minuscole')

  if (/[A-Z]/.test(password)) score += 1
  else suggestions.push('Lettere maiuscole')

  if (/[0-9]/.test(password)) score += 1
  else suggestions.push('Numeri')

  if (/[^a-zA-Z0-9]/.test(password)) score += 1
  else suggestions.push('Caratteri speciali (!@#$%^&*)')

  // Special patterns
  if (password.length >= 12) score += 1

  const level = score <= 1 ? 'weak' : score <= 2 ? 'medium' : score <= 3 ? 'good' : 'strong'
  const text = score <= 1 ? 'Debole' : score <= 2 ? 'Media' : score <= 3 ? 'Buona' : 'Forte'

  return { level, text, suggestions }
})

// Form validation
const isResetFormValid = computed(() => {
  return resetForm.newPassword.trim().length >= 8 &&
         resetForm.confirmPassword === resetForm.newPassword &&
         passwordStrength.value.level !== 'weak' &&
         !errors.newPassword &&
         !errors.confirmPassword
})

// Methods
const validateEmail = () => {
  if (!requestForm.email.trim()) {
    errors.email = 'Il campo email è obbligatorio'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(requestForm.email)) {
    errors.email = 'Inserisci un indirizzo email valido'
  } else {
    errors.email = ''
  }
}

const clearEmailError = () => {
  errors.email = ''
  if (generalError.value) {
    generalError.value = ''
  }
}

const validateNewPassword = () => {
  if (!resetForm.newPassword.trim()) {
    errors.newPassword = 'La nuova password è richiesta'
  } else if (resetForm.newPassword.length < 8) {
    errors.newPassword = 'La password deve contenere almeno 8 caratteri'
  } else if (passwordStrength.value.level === 'weak') {
    errors.newPassword = 'La password è troppo debole'
  } else {
    errors.newPassword = ''
  }
}

const validateConfirmPassword = () => {
  if (!resetForm.confirmPassword.trim()) {
    errors.confirmPassword = 'Conferma la password'
  } else if (resetForm.newPassword !== resetForm.confirmPassword) {
    errors.confirmPassword = 'Le password non coincidono'
  } else {
    errors.confirmPassword = ''
  }
}

const clearConfirmPasswordError = () => {
  errors.confirmPassword = ''
}

const onPasswordInput = () => {
  validateNewPassword()
  // Also validate confirm password if it has been entered
  if (resetForm.confirmPassword) {
    validateConfirmPassword()
  }
}

const toggleNewPasswordVisibility = () => {
  showNewPassword.value = !showNewPassword.value
}

const toggleConfirmPasswordVisibility = () => {
  showConfirmPassword.value = !showConfirmPassword.value
}

// Rate limiting management
const updateRateLimitInfo = () => {
  const storedLimit = localStorage.getItem('resetRateLimit')
  if (storedLimit) {
    const limitTime = parseInt(storedLimit, 10)
    const now = Date.now()
    const timeRemaining = limitTime - now
    
    if (timeRemaining > 0) {
      rateLimitInfo.isLimited = true
      rateLimitInfo.resetTime = Math.ceil(timeRemaining / 1000)
      
      rateLimitTimer = setInterval(() => {
        rateLimitInfo.resetTime -= 1
        if (rateLimitInfo.resetTime <= 0) {
          resetRateLimit()
        }
      }, 1000)
    }
  }
}

const resetRateLimit = () => {
  rateLimitInfo.isLimited = false
  rateLimitInfo.resetTime = 0
  localStorage.removeItem('resetRateLimit')
  
  if (rateLimitTimer) {
    clearInterval(rateLimitTimer)
    rateLimitTimer = null
  }
}

const setRateLimit = () => {
  const limitTime = Date.now() + (15 * 60 * 1000) // 15 minutes
  localStorage.setItem('resetRateLimit', limitTime.toString())
  updateRateLimitInfo()
}

// Password reset request
const handlePasswordResetRequest = async () => {
  validateEmail()
  if (errors.email || rateLimitInfo.isLimited) {
    return
  }

  try {
    isLoading.value = true
    generalError.value = ''
    
    await authApi.requestPasswordReset(requestForm.email.trim())
    
    currentStep.value = 'sent'
    startResendCooldown()
  } catch (error: any) {
    console.error('Password reset request failed:', error)
    
    if (error.response?.status === 429) {
      setRateLimit()
      generalError.value = 'Troppi tentativi. Riprova più tardi.'
    } else if (error.response?.status === 404) {
      // Security: Don't reveal if email exists
      currentStep.value = 'sent'
      startResendCooldown()
    } else {
      generalError.value = 'Errore durante l\'invio dell\'email. Riprova più tardi.'
    }
  } finally {
    isLoading.value = false
  }
}

// Resend email
const resendEmail = async () => {
  if (resendCooldown.value > 0) return
  
  await handlePasswordResetRequest()
}

const startResendCooldown = () => {
  resendCooldown.value = 60 // 1 minute cooldown
  
  resendTimer = setInterval(() => {
    resendCooldown.value -= 1
    if (resendCooldown.value <= 0) {
      clearInterval(resendTimer!)
      resendTimer = null
    }
  }, 1000)
}

// Password reset
const handlePasswordReset = async () => {
  validateNewPassword()
  validateConfirmPassword()
  
  if (!isResetFormValid.value) {
    return
  }

  try {
    isLoading.value = true
    generalError.value = ''
    tokenError.value = ''
    
    await authApi.resetPassword(resetForm.token, resetForm.newPassword)
    
    currentStep.value = 'success'
    showSuccess('Password reimpostata con successo!')
  } catch (error: any) {
    console.error('Password reset failed:', error)
    
    if (error.response?.status === 400) {
      tokenError.value = error.response.data?.message || 'Token non valido o scaduto'
    } else if (error.response?.status === 422) {
      const validationErrors = error.response.data?.errors || {}
      if (validationErrors.password) {
        errors.newPassword = validationErrors.password[0]
      }
    } else {
      generalError.value = 'Errore durante la reimpostazione. Riprova più tardi.'
    }
  } finally {
    isLoading.value = false
  }
}

// Utility methods
const openEmailClient = () => {
  const emailDomain = requestForm.email.split('@')[1]
  let emailUrl = 'mailto:'
  
  // Common webmail providers
  if (emailDomain?.includes('gmail.com')) {
    emailUrl = 'https://mail.google.com'
  } else if (emailDomain?.includes('outlook.com') || emailDomain?.includes('hotmail.com')) {
    emailUrl = 'https://outlook.live.com'
  } else if (emailDomain?.includes('yahoo.com')) {
    emailUrl = 'https://mail.yahoo.com'
  }
  
  window.open(emailUrl, '_blank')
}

// Initialize component based on route
const initializeComponent = () => {
  const token = route.query.token as string
  
  if (token) {
    // Token provided, show reset form
    resetForm.token = token
    currentStep.value = 'reset'
  } else {
    // No token, show request form
    currentStep.value = 'request'
  }
  
  // Auto-focus first field
  setTimeout(() => {
    const firstInput = document.querySelector('input') as HTMLInputElement
    if (firstInput) {
      firstInput.focus()
    }
  }, 100)
}

// Lifecycle
onMounted(() => {
  initializeComponent()
  updateRateLimitInfo()
})

onUnmounted(() => {
  if (rateLimitTimer) {
    clearInterval(rateLimitTimer)
  }
  if (resendTimer) {
    clearInterval(resendTimer)
  }
})
</script>

<style scoped>
.password-reset {
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

/* Animation for step transitions */
.step-transition {
  animation: stepSlideIn 0.3s ease-out;
}

@keyframes stepSlideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* Password strength indicator animations */
.password-strength-bar {
  transition: width 0.3s ease-in-out;
}

/* Success icon animation */
@keyframes checkmarkDraw {
  0% {
    stroke-dasharray: 0 24;
  }
  100% {
    stroke-dasharray: 24 24;
  }
}

.success-checkmark {
  stroke-dasharray: 24 24;
  animation: checkmarkDraw 0.6s ease-in-out;
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

/* Email client button hover effect */
button:hover svg {
  transform: scale(1.1);
  transition: transform 0.2s ease;
}

/* Rate limit warning pulse */
.rate-limit-warning {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
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
  .password-reset {
    @apply mx-4;
  }
  
  .form-header h2 {
    @apply text-xl;
  }
  
  input, button {
    @apply py-3;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .border-gray-300 {
    @apply border-black;
  }
  
  .text-gray-600 {
    @apply text-black;
  }
  
  .bg-gray-50 {
    @apply bg-white;
  }
}

/* Print styles */
@media print {
  .password-reset {
    @apply shadow-none;
  }
  
  button {
    @apply hidden;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* Focus trap for accessibility */
.password-reset:focus-within {
  @apply ring-2 ring-blue-500 ring-opacity-25 rounded-lg;
}

/* Email link styling */
a[href^="mailto:"] {
  @apply text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300;
}

/* Success state styling */
.success-icon-container {
  animation: bounceIn 0.6s ease;
}

@keyframes bounceIn {
  0%, 20%, 40%, 60%, 80%, 100% {
    animation-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1);
  }
  0% {
    opacity: 0;
    transform: scale3d(0.3, 0.3, 0.3);
  }
  20% {
    transform: scale3d(1.1, 1.1, 1.1);
  }
  40% {
    transform: scale3d(0.9, 0.9, 0.9);
  }
  60% {
    opacity: 1;
    transform: scale3d(1.03, 1.03, 1.03);
  }
  80% {
    transform: scale3d(0.97, 0.97, 0.97);
  }
  100% {
    opacity: 1;
    transform: scale3d(1, 1, 1);
  }
}
</style>