<template>
  <div class="auth-guard">
    <!-- Loading State -->
    <div v-if="isLoading" class="flex items-center justify-center min-h-screen">
      <div class="text-center space-y-4">
        <LoadingSpinner class="w-8 h-8 mx-auto text-blue-600" />
        <p class="text-gray-600 dark:text-gray-400">
          Verificando autenticazione...
        </p>
      </div>
    </div>

    <!-- Authentication Error -->
    <div v-else-if="authError" class="flex items-center justify-center min-h-screen">
      <div class="max-w-md mx-auto text-center space-y-6 p-6">
        <!-- Error Icon -->
        <div class="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-red-100 dark:bg-red-900/20">
          <svg class="h-8 w-8 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-2.694-.833-3.464 0L3.35 16.5c-.77.833.192 2.5 1.732 2.5z" />
          </svg>
        </div>

        <!-- Error Message -->
        <div class="space-y-2">
          <h2 class="text-xl font-semibold text-gray-900 dark:text-white">
            Accesso Richiesto
          </h2>
          <p class="text-gray-600 dark:text-gray-400">
            {{ authError }}
          </p>
        </div>

        <!-- Action Buttons -->
        <div class="space-y-3">
          <router-link
            to="/login"
            class="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
          >
            Accedi ora
          </router-link>
          
          <button
            @click="retry"
            :disabled="retrying"
            class="w-full flex justify-center py-3 px-4 border border-gray-300 dark:border-gray-600 rounded-lg shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 transition-colors"
          >
            <LoadingSpinner v-if="retrying" class="w-4 h-4 mr-2" />
            {{ retrying ? 'Riprovando...' : 'Riprova' }}
          </button>
        </div>

        <!-- Register Link -->
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

    <!-- Session Expired Warning -->
    <div v-else-if="sessionExpired" class="flex items-center justify-center min-h-screen">
      <div class="max-w-md mx-auto text-center space-y-6 p-6">
        <!-- Warning Icon -->
        <div class="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-yellow-100 dark:bg-yellow-900/20">
          <svg class="h-8 w-8 text-yellow-600 dark:text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>

        <!-- Warning Message -->
        <div class="space-y-2">
          <h2 class="text-xl font-semibold text-gray-900 dark:text-white">
            Sessione Scaduta
          </h2>
          <p class="text-gray-600 dark:text-gray-400">
            La tua sessione è scaduta per motivi di sicurezza. Effettua nuovamente l'accesso per continuare.
          </p>
        </div>

        <!-- Action Buttons -->
        <div class="space-y-3">
          <button
            @click="handleRelogin"
            :disabled="isLoading"
            class="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 transition-colors"
          >
            <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
            {{ isLoading ? 'Reindirizzamento...' : 'Accedi di nuovo' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Protected Content -->
    <div v-else-if="isAuthenticated" class="auth-guard-content">
      <slot />
    </div>

    <!-- Fallback -->
    <div v-else class="flex items-center justify-center min-h-screen">
      <div class="text-center space-y-4">
        <LoadingSpinner class="w-8 h-8 mx-auto text-blue-600" />
        <p class="text-gray-600 dark:text-gray-400">
          Caricamento...
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useRoute, useRouter } from 'vue-router'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'

// Props
interface Props {
  requireAdmin?: boolean
  fallback?: string
  redirectTo?: string
}

const props = withDefaults(defineProps<Props>(), {
  requireAdmin: false,
  fallback: '/login',
  redirectTo: '/'
})

// Emits
const emit = defineEmits<{
  authenticated: [user: any]
  unauthenticated: []
  sessionExpired: []
}>()

// Composables
const { 
  user, 
  isAuthenticated, 
  isLoading: authLoading, 
  isInitialized,
  isTokenExpired,
  requireAuth,
  hasRole,
  refreshToken,
  redirectToLogin
} = useAuth()

const route = useRoute()
const router = useRouter()

// Component state
const isLoading = ref(true)
const authError = ref('')
const sessionExpired = ref(false)
const retrying = ref(false)
const retryCount = ref(0)
const maxRetries = 3

// Computed
const shouldCheckAuth = computed(() => {
  return !isAuthenticated.value || props.requireAdmin
})

// Methods
const checkAuthentication = async (): Promise<boolean> => {
  try {
    isLoading.value = true
    authError.value = ''
    sessionExpired.value = false

    // Wait for auth to initialize
    if (!isInitialized.value) {
      await new Promise(resolve => {
        const unwatch = watch(isInitialized, (initialized) => {
          if (initialized) {
            unwatch()
            resolve(true)
          }
        })
      })
    }

    // Check if user is authenticated
    const isAuthValid = await requireAuth()
    
    if (!isAuthValid) {
      if (isTokenExpired.value) {
        sessionExpired.value = true
        emit('sessionExpired')
      } else {
        authError.value = 'Devi effettuare l\'accesso per accedere a questa pagina.'
        emit('unauthenticated')
      }
      return false
    }

    // Check admin requirement
    if (props.requireAdmin && !hasRole('admin')) {
      authError.value = 'Non hai i permessi necessari per accedere a questa pagina.'
      emit('unauthenticated')
      return false
    }

    // Authentication successful
    emit('authenticated', user.value)
    return true
    
  } catch (error) {
    console.error('Authentication check failed:', error)
    
    if (retryCount.value < maxRetries) {
      // Try to refresh token
      const refreshSuccess = await refreshToken()
      if (refreshSuccess) {
        return checkAuthentication()
      }
    }
    
    authError.value = 'Errore durante la verifica dell\'autenticazione. Riprova più tardi.'
    emit('unauthenticated')
    return false
  } finally {
    isLoading.value = false
  }
}

const retry = async () => {
  if (retrying.value || retryCount.value >= maxRetries) return

  try {
    retrying.value = true
    retryCount.value += 1
    
    const success = await checkAuthentication()
    if (success) {
      retryCount.value = 0 // Reset on success
    }
  } finally {
    retrying.value = false
  }
}

const handleRelogin = async () => {
  try {
    isLoading.value = true
    await redirectToLogin(route.fullPath)
  } catch (error) {
    console.error('Relogin failed:', error)
    await router.push(props.fallback)
  } finally {
    isLoading.value = false
  }
}

// Auto-retry on auth state changes
watch(isAuthenticated, async (authenticated) => {
  if (!authenticated && isInitialized.value && !sessionExpired.value) {
    await checkAuthentication()
  }
})

// Check token expiration periodically
let tokenCheckInterval: NodeJS.Timeout | null = null

const startTokenCheck = () => {
  tokenCheckInterval = setInterval(async () => {
    if (isAuthenticated.value && isTokenExpired.value) {
      sessionExpired.value = true
      emit('sessionExpired')
    }
  }, 60000) // Check every minute
}

const stopTokenCheck = () => {
  if (tokenCheckInterval) {
    clearInterval(tokenCheckInterval)
    tokenCheckInterval = null
  }
}

// Lifecycle
onMounted(async () => {
  await checkAuthentication()
  startTokenCheck()
})

onUnmounted(() => {
  stopTokenCheck()
})

// Watch for route changes to re-check auth
watch(() => route.path, async () => {
  if (shouldCheckAuth.value) {
    await checkAuthentication()
  }
})
</script>

<style scoped>
.auth-guard {
  @apply min-h-screen;
}

.auth-guard-content {
  @apply min-h-screen;
}

/* Loading animation */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* Error state animations */
.error-container {
  animation: slideUp 0.4s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Button hover effects */
button:hover,
a:hover {
  transform: translateY(-1px);
  transition: transform 0.2s ease;
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

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* Mobile responsiveness */
@media (max-width: 640px) {
  .max-w-md {
    @apply mx-4;
  }
}

/* Focus styles */
button:focus,
a:focus {
  @apply outline-none ring-2 ring-blue-500 ring-offset-2 dark:ring-offset-gray-900;
}

/* Dark mode improvements */
@media (prefers-color-scheme: dark) {
  .auth-guard {
    @apply bg-gray-900;
  }
}

/* Print styles */
@media print {
  .auth-guard {
    @apply shadow-none min-h-0;
  }
}
</style>