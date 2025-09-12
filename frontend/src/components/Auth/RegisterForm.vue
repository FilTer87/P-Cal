<template>
  <div class="register-form">
    <div class="form-header">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">
        Crea il tuo account
      </h2>
      <p class="text-gray-600 dark:text-gray-400">
        Registrati per iniziare ad usare PrivateCal
      </p>
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-6">
      <!-- Username Input -->
      <div>
        <label 
          for="username" 
          class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
        >
          Nome Utente *
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
              'w-full px-4 py-3 pr-10 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.username 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : usernameStatus === 'available'
                  ? 'border-green-300 bg-green-50 dark:bg-green-900/20 dark:border-green-600'
                  : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="scegli un nome utente"
            @blur="validateField('username')"
            @input="onUsernameInput"
          />
          <div class="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
            <LoadingSpinner 
              v-if="isCheckingUsername" 
              class="w-5 h-5 text-gray-400" 
            />
            <svg 
              v-else-if="usernameStatus === 'available'" 
              class="h-5 w-5 text-green-500" 
              fill="currentColor" 
              viewBox="0 0 20 20"
            >
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
            </svg>
            <svg 
              v-else-if="errors.username" 
              class="h-5 w-5 text-red-500" 
              fill="currentColor" 
              viewBox="0 0 20 20"
            >
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
          </div>
        </div>
        <p v-if="errors.username" class="mt-1 text-sm text-red-600 dark:text-red-400">
          {{ errors.username }}
        </p>
        <p v-else-if="usernameStatus === 'available'" class="mt-1 text-sm text-green-600 dark:text-green-400">
          Nome utente disponibile
        </p>
        <p v-else class="mt-1 text-sm text-gray-500 dark:text-gray-400">
          3-50 caratteri, solo lettere, numeri e underscore
        </p>
      </div>

      <!-- Email Input -->
      <div>
        <label 
          for="email" 
          class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
        >
          Email *
        </label>
        <div class="relative">
          <input
            id="email"
            v-model="form.email"
            type="email"
            autocomplete="email"
            required
            :disabled="isLoading"
            :class="[
              'w-full px-4 py-3 pr-10 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.email 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : emailStatus === 'available'
                  ? 'border-green-300 bg-green-50 dark:bg-green-900/20 dark:border-green-600'
                  : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="inserisci la tua email"
            @blur="validateField('email')"
            @input="onEmailInput"
          />
          <div class="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
            <LoadingSpinner 
              v-if="isCheckingEmail" 
              class="w-5 h-5 text-gray-400" 
            />
            <svg 
              v-else-if="emailStatus === 'available'" 
              class="h-5 w-5 text-green-500" 
              fill="currentColor" 
              viewBox="0 0 20 20"
            >
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
            </svg>
            <svg 
              v-else-if="errors.email" 
              class="h-5 w-5 text-red-500" 
              fill="currentColor" 
              viewBox="0 0 20 20"
            >
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
          </div>
        </div>
        <p v-if="errors.email" class="mt-1 text-sm text-red-600 dark:text-red-400">
          {{ errors.email }}
        </p>
        <p v-else-if="emailStatus === 'available'" class="mt-1 text-sm text-green-600 dark:text-green-400">
          Email disponibile
        </p>
      </div>

      <!-- First Name and Last Name -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label 
            for="firstName" 
            class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
          >
            Nome
          </label>
          <input
            id="firstName"
            v-model="form.firstName"
            type="text"
            autocomplete="given-name"
            :disabled="isLoading"
            :class="[
              'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.firstName 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="nome (facoltativo)"
            @blur="validateField('firstName')"
            @input="clearFieldError('firstName')"
          />
          <p v-if="errors.firstName" class="mt-1 text-sm text-red-600 dark:text-red-400">
            {{ errors.firstName }}
          </p>
        </div>

        <div>
          <label 
            for="lastName" 
            class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
          >
            Cognome
          </label>
          <input
            id="lastName"
            v-model="form.lastName"
            type="text"
            autocomplete="family-name"
            :disabled="isLoading"
            :class="[
              'w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.lastName 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="cognome (facoltativo)"
            @blur="validateField('lastName')"
            @input="clearFieldError('lastName')"
          />
          <p v-if="errors.lastName" class="mt-1 text-sm text-red-600 dark:text-red-400">
            {{ errors.lastName }}
          </p>
        </div>
      </div>

      <!-- Timezone Selection -->
      <div>
        <label 
          for="timezone" 
          class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
        >
          Fuso Orario
        </label>
        <select
          id="timezone"
          v-model="form.timezone"
          :disabled="isLoading"
          class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 text-gray-900 dark:text-white transition-colors"
        >
          <option value="">Rileva automaticamente</option>
          <optgroup label="Italia">
            <option value="Europe/Rome">Roma (UTC+1)</option>
          </optgroup>
          <optgroup label="Europa">
            <option value="Europe/London">Londra (UTC+0)</option>
            <option value="Europe/Berlin">Berlino (UTC+1)</option>
            <option value="Europe/Paris">Parigi (UTC+1)</option>
            <option value="Europe/Madrid">Madrid (UTC+1)</option>
            <option value="Europe/Amsterdam">Amsterdam (UTC+1)</option>
          </optgroup>
          <optgroup label="Americhe">
            <option value="America/New_York">New York (UTC-5)</option>
            <option value="America/Los_Angeles">Los Angeles (UTC-8)</option>
            <option value="America/Chicago">Chicago (UTC-6)</option>
          </optgroup>
          <optgroup label="Asia">
            <option value="Asia/Tokyo">Tokyo (UTC+9)</option>
            <option value="Asia/Shanghai">Shanghai (UTC+8)</option>
            <option value="Asia/Dubai">Dubai (UTC+4)</option>
          </optgroup>
        </select>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
          {{ detectedTimezone ? `Rilevato: ${detectedTimezone}` : 'Rileveremo automaticamente il tuo fuso orario' }}
        </p>
      </div>

      <!-- Password Input -->
      <div>
        <label 
          for="password" 
          class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
        >
          Password *
        </label>
        <div class="relative">
          <input
            id="password"
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="new-password"
            required
            :disabled="isLoading"
            :class="[
              'w-full px-4 py-3 pr-12 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.password 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="crea una password sicura"
            @blur="validateField('password')"
            @input="onPasswordInput"
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
        
        <!-- Password Strength Indicator -->
        <div v-if="form.password" class="mt-2">
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
          Conferma Password *
        </label>
        <div class="relative">
          <input
            id="confirmPassword"
            v-model="form.confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            autocomplete="new-password"
            required
            :disabled="isLoading"
            :class="[
              'w-full px-4 py-3 pr-12 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors',
              errors.confirmPassword 
                ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-600' 
                : form.confirmPassword && form.password === form.confirmPassword
                  ? 'border-green-300 bg-green-50 dark:bg-green-900/20 dark:border-green-600'
                  : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800'
            ]"
            placeholder="conferma la password"
            @blur="validateField('confirmPassword')"
            @input="clearFieldError('confirmPassword')"
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

      <!-- Terms and Privacy Policy -->
      <div class="space-y-4">
        <div class="flex items-start">
          <input
            id="acceptTerms"
            v-model="form.acceptTerms"
            type="checkbox"
            required
            :disabled="isLoading"
            class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded disabled:opacity-50 mt-1"
          />
          <label for="acceptTerms" class="ml-3 block text-sm text-gray-700 dark:text-gray-300">
            Accetto i 
            <a href="/terms" target="_blank" class="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 font-medium">
              Termini e Condizioni
            </a>
            e l'
            <a href="/privacy" target="_blank" class="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 font-medium">
              Informativa sulla Privacy
            </a>
            *
          </label>
        </div>
        <p v-if="errors.acceptTerms" class="text-sm text-red-600 dark:text-red-400">
          {{ errors.acceptTerms }}
        </p>

        <div class="flex items-start">
          <input
            id="acceptMarketing"
            v-model="form.acceptMarketing"
            type="checkbox"
            :disabled="isLoading"
            class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded disabled:opacity-50 mt-1"
          />
          <label for="acceptMarketing" class="ml-3 block text-sm text-gray-700 dark:text-gray-300">
            Accetto di ricevere comunicazioni promozionali e aggiornamenti via email
          </label>
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
          :disabled="isLoading || !isFormValid"
          class="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <LoadingSpinner v-if="isLoading" class="w-5 h-5 mr-2" />
          {{ isLoading ? 'Registrazione in corso...' : 'Crea Account' }}
        </button>
      </div>
    </form>

    <!-- Login Link -->
    <div class="mt-6 text-center">
      <p class="text-sm text-gray-600 dark:text-gray-400">
        Hai già un account?
        <router-link 
          to="/login" 
          class="font-medium text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300"
        >
          Accedi qui
        </router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useCustomToast } from '@/composables/useCustomToast'
import { authApi } from '@/services/authApi'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'
import type { RegisterFormData } from '@/types/auth'

// Props
interface Props {
  redirectTo?: string
}

const props = withDefaults(defineProps<Props>(), {
  redirectTo: '/'
})

// Composables
const { register, isLoading } = useAuth()
const { showError, showSuccess } = useCustomToast()

// Form state
const form = reactive<RegisterFormData & { 
  timezone: string
  acceptMarketing: boolean
}>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  firstName: '',
  lastName: '',
  acceptTerms: false,
  acceptMarketing: false,
  timezone: ''
})

// Validation state
const errors = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  firstName: '',
  lastName: '',
  acceptTerms: ''
})

// Component state
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const generalError = ref('')
const usernameStatus = ref<'checking' | 'available' | 'unavailable' | 'error'>('')
const emailStatus = ref<'checking' | 'available' | 'unavailable' | 'error'>('')
const isCheckingUsername = ref(false)
const isCheckingEmail = ref(false)
const detectedTimezone = ref('')

// Debounce timers
let usernameCheckTimer: NodeJS.Timeout | null = null
let emailCheckTimer: NodeJS.Timeout | null = null

// Password strength calculation
const passwordStrength = computed(() => {
  const password = form.password
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

  const levels = ['', 'weak', 'medium', 'good', 'strong']
  const texts = ['', 'Debole', 'Media', 'Buona', 'Forte']
  
  const level = score <= 1 ? 'weak' : score <= 2 ? 'medium' : score <= 3 ? 'good' : 'strong'
  const text = texts[Math.min(score, 4)]

  return { level, text, suggestions }
})

// Form validation
const isFormValid = computed(() => {
  return form.username.trim().length > 0 && 
         form.email.trim().length > 0 && 
         form.password.trim().length > 0 && 
         form.confirmPassword.trim().length > 0 &&
         form.acceptTerms &&
         !errors.username && 
         !errors.email &&
         !errors.password && 
         !errors.confirmPassword &&
         !errors.firstName &&
         !errors.lastName &&
         !errors.acceptTerms &&
         usernameStatus.value === 'available' &&
         emailStatus.value === 'available'
})

// Methods
const validateField = (field: keyof typeof errors) => {
  switch (field) {
    case 'username':
      if (!form.username.trim()) {
        errors.username = 'Il campo nome utente è obbligatorio'
      } else if (form.username.trim().length < 3) {
        errors.username = 'Il nome utente deve contenere almeno 3 caratteri'
      } else if (form.username.trim().length > 50) {
        errors.username = 'Il nome utente non può superare 50 caratteri'
      } else if (!/^[a-zA-Z0-9_]+$/.test(form.username)) {
        errors.username = 'Il nome utente può contenere solo lettere, numeri e underscore'
      } else {
        errors.username = ''
      }
      break
    
    case 'email':
      if (!form.email.trim()) {
        errors.email = 'Il campo email è obbligatorio'
      } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
        errors.email = 'Inserisci un indirizzo email valido'
      } else {
        errors.email = ''
      }
      break
    
    case 'password':
      if (!form.password.trim()) {
        errors.password = 'Il campo password è obbligatorio'
      } else if (form.password.length < 8) {
        errors.password = 'La password deve contenere almeno 8 caratteri'
      } else if (passwordStrength.value.level === 'weak') {
        errors.password = 'La password è troppo debole'
      } else {
        errors.password = ''
      }
      break
    
    case 'confirmPassword':
      if (!form.confirmPassword.trim()) {
        errors.confirmPassword = 'Conferma la password'
      } else if (form.password !== form.confirmPassword) {
        errors.confirmPassword = 'Le password non coincidono'
      } else {
        errors.confirmPassword = ''
      }
      break
    
    case 'firstName':
      if (form.firstName && form.firstName.length > 50) {
        errors.firstName = 'Il nome non può superare 50 caratteri'
      } else {
        errors.firstName = ''
      }
      break
    
    case 'lastName':
      if (form.lastName && form.lastName.length > 50) {
        errors.lastName = 'Il cognome non può superare 50 caratteri'
      } else {
        errors.lastName = ''
      }
      break
    
    case 'acceptTerms':
      if (!form.acceptTerms) {
        errors.acceptTerms = 'Devi accettare i termini e condizioni'
      } else {
        errors.acceptTerms = ''
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
  Object.keys(errors).forEach(key => {
    validateField(key as keyof typeof errors)
  })
  return isFormValid.value
}

// Username availability check
const checkUsernameAvailability = async (username: string) => {
  if (username.length < 3) return
  
  isCheckingUsername.value = true
  usernameStatus.value = 'checking'
  
  try {
    const available = await authApi.checkUsernameAvailability(username)
    if (available) {
      usernameStatus.value = 'available'
    } else {
      usernameStatus.value = 'unavailable'
      errors.username = 'Nome utente già in uso'
    }
  } catch (error) {
    console.error('Username check failed:', error)
    usernameStatus.value = 'error'
  } finally {
    isCheckingUsername.value = false
  }
}

// Email availability check
const checkEmailAvailability = async (email: string) => {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return
  
  isCheckingEmail.value = true
  emailStatus.value = 'checking'
  
  try {
    const available = await authApi.checkEmailAvailability(email)
    if (available) {
      emailStatus.value = 'available'
    } else {
      emailStatus.value = 'unavailable'
      errors.email = 'Email già registrata'
    }
  } catch (error) {
    console.error('Email check failed:', error)
    emailStatus.value = 'error'
  } finally {
    isCheckingEmail.value = false
  }
}

// Input handlers with debouncing
const onUsernameInput = () => {
  clearFieldError('username')
  usernameStatus.value = ''
  
  if (usernameCheckTimer) {
    clearTimeout(usernameCheckTimer)
  }
  
  if (form.username.trim().length >= 3) {
    usernameCheckTimer = setTimeout(() => {
      checkUsernameAvailability(form.username.trim())
    }, 500)
  }
}

const onEmailInput = () => {
  clearFieldError('email')
  emailStatus.value = ''
  
  if (emailCheckTimer) {
    clearTimeout(emailCheckTimer)
  }
  
  if (form.email.trim() && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    emailCheckTimer = setTimeout(() => {
      checkEmailAvailability(form.email.trim())
    }, 500)
  }
}

const onPasswordInput = () => {
  clearFieldError('password')
  // Also validate confirm password if it has been entered
  if (form.confirmPassword) {
    validateField('confirmPassword')
  }
}

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const toggleConfirmPasswordVisibility = () => {
  showConfirmPassword.value = !showConfirmPassword.value
}

// Timezone detection
const detectTimezone = () => {
  try {
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
    detectedTimezone.value = timezone
    if (!form.timezone) {
      form.timezone = timezone
    }
  } catch (error) {
    console.error('Failed to detect timezone:', error)
    detectedTimezone.value = 'Europe/Rome' // Default fallback
    if (!form.timezone) {
      form.timezone = 'Europe/Rome'
    }
  }
}

// Form submission
const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  try {
    generalError.value = ''
    
    const registrationData = {
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      firstName: form.firstName.trim() || undefined,
      lastName: form.lastName.trim() || undefined,
      timezone: form.timezone || detectedTimezone.value,
      acceptMarketing: form.acceptMarketing
    }
    
    const success = await register(registrationData)
    
    if (success) {
      showSuccess('Account creato con successo! Benvenuto in PrivateCal!')
    }
  } catch (error: any) {
    console.error('Registration error:', error)
    
    if (error.response?.status === 409) {
      // Handle conflict errors (username/email already exists)
      if (error.response.data?.field === 'username') {
        errors.username = 'Nome utente già in uso'
        usernameStatus.value = 'unavailable'
      } else if (error.response.data?.field === 'email') {
        errors.email = 'Email già registrata'
        emailStatus.value = 'unavailable'
      } else {
        generalError.value = 'Nome utente o email già in uso'
      }
    } else if (error.response?.status === 422) {
      // Validation errors
      const validationErrors = error.response.data?.errors || {}
      Object.keys(validationErrors).forEach(field => {
        if (field in errors) {
          errors[field as keyof typeof errors] = validationErrors[field][0] || `Errore nel campo ${field}`
        }
      })
      generalError.value = 'Controlla i dati inseriti e riprova'
    } else if (error.response?.data?.message) {
      generalError.value = error.response.data.message
    } else {
      generalError.value = 'Errore durante la registrazione. Riprova più tardi.'
    }
  }
}

// Auto-focus first field
const focusFirstField = () => {
  const firstInput = document.getElementById('username')
  if (firstInput) {
    firstInput.focus()
  }
}

// Lifecycle
onMounted(() => {
  detectTimezone()
  focusFirstField()
})

// Watchers for real-time validation
watch(() => form.confirmPassword, () => {
  if (form.confirmPassword && form.password) {
    validateField('confirmPassword')
  }
})

watch(() => form.acceptTerms, () => {
  if (form.acceptTerms) {
    clearFieldError('acceptTerms')
  }
})
</script>

<style scoped>
.register-form {
  @apply max-w-2xl mx-auto;
}

.form-header {
  @apply text-center mb-8;
}

/* Custom focus styles for better accessibility */
input:focus,
select:focus {
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

/* Password strength indicator animations */
.password-strength-bar {
  transition: width 0.3s ease-in-out;
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
  input:-webkit-autofill,
  input:-webkit-autofill:hover,
  input:-webkit-autofill:focus {
    -webkit-box-shadow: 0 0 0 1000px #374151 inset;
    -webkit-text-fill-color: #f3f4f6;
  }
}

/* Mobile responsiveness */
@media (max-width: 768px) {
  .register-form {
    @apply mx-4;
  }
  
  .form-header h2 {
    @apply text-xl;
  }
  
  input, button, select {
    @apply py-3;
  }
  
  .grid-cols-1.md\:grid-cols-2 {
    @apply grid-cols-1;
  }
}

/* Focus trap for accessibility */
.register-form:focus-within {
  @apply ring-2 ring-blue-500 ring-opacity-25 rounded-lg;
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
</style>