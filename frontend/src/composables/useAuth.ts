import { computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useNotifications } from './useNotifications'
import type { LoginCredentials, RegisterCredentials, User } from '../types/auth'

export function useAuth() {
  const authStore = useAuthStore()
  const router = useRouter()
  const { showError, showSuccess, showAuthError } = useNotifications()

  // Computed properties
  const user = computed(() => authStore.user)
  const isAuthenticated = computed(() => authStore.isAuthenticated)
  const isLoading = computed(() => authStore.isLoading)
  const isInitialized = computed(() => authStore.isInitialized)
  const userFullName = computed(() => authStore.userFullName)
  const userInitials = computed(() => authStore.userInitials)
  const accessToken = computed(() => authStore.accessToken)
  const isTokenExpired = computed(() => authStore.isTokenExpired)

  // Watch for authentication state changes
  watch(isAuthenticated, (authenticated, oldAuthenticated) => {
    console.log('👀 isAuthenticated changed:', authenticated, 'isInitialized:', authStore.isInitialized, 'oldAuthenticated:', oldAuthenticated)
    
    // Only react to authentication changes AFTER initialization is complete
    // AND when there was an actual change from authenticated to not authenticated
    if (!authenticated && authStore.isInitialized && oldAuthenticated === true) {
      console.log('🚪 Redirecting to login due to authentication lost')
      // User was logged out, redirect to login
      redirectToLogin()
    }
  })

  // Actions
  const initializeAuth = async () => {
    try {
      await authStore.initializeAuth()
    } catch (error) {
      console.error('Failed to initialize auth:', error)
    }
  }

  const login = async (credentials: LoginCredentials): Promise<boolean> => {
    try {
      const success = await authStore.login(credentials)
      if (success) {
        await redirectAfterLogin()
      }
      return success
    } catch (error) {
      console.error('Login failed:', error)
      return false
    }
  }

  const register = async (credentials: RegisterCredentials): Promise<boolean> => {
    try {
      const success = await authStore.register(credentials)
      if (success) {
        await redirectAfterLogin()
      }
      return success
    } catch (error) {
      console.error('Registration failed:', error)
      return false
    }
  }

  const logout = async (): Promise<void> => {
    try {
      await authStore.logout()
      await router.push('/login')
    } catch (error) {
      console.error('Logout failed:', error)
      // Even if logout fails on server, clear local state
      authStore.clearAuthData()
      await router.push('/login')
    }
  }

  const updateProfile = async (profileData: Partial<User>): Promise<boolean> => {
    try {
      return await authStore.updateProfile(profileData)
    } catch (error) {
      console.error('Profile update failed:', error)
      return false
    }
  }

  const refreshToken = async (): Promise<boolean> => {
    console.log('⚠️ Manual refresh token called - now handled by API interceptor')
    // Just return true as refresh is handled automatically by API interceptor
    return true
  }

  const verifyToken = async (): Promise<boolean> => {
    try {
      return await authStore.verifyToken()
    } catch (error) {
      console.error('Token verification failed:', error)
      return false
    }
  }

  // Navigation helpers
  const redirectToLogin = async (returnUrl?: string) => {
    const query: Record<string, string> = {}
    if (returnUrl) {
      query.redirect = returnUrl
    } else if (router.currentRoute.value.path !== '/login') {
      query.redirect = router.currentRoute.value.fullPath
    }

    await router.push({
      name: 'Login',
      query
    })
  }

  const redirectAfterLogin = async () => {
    const redirectPath = router.currentRoute.value.query.redirect as string
    if (redirectPath && redirectPath !== '/login') {
      await router.push(redirectPath)
    } else {
      await router.push('/')
    }
  }

  // Auth guards
  const requireAuth = async (): Promise<boolean> => {
    if (!isInitialized.value) {
      await initializeAuth()
    }

    if (!isAuthenticated.value) {
      showAuthError()
      await redirectToLogin()
      return false
    }

    // Check if token is expired and try to refresh
    if (isTokenExpired.value) {
      const refreshSuccess = await refreshToken()
      if (!refreshSuccess) {
        showAuthError()
        await redirectToLogin()
        return false
      }
    }

    return true
  }

  const requireGuest = async (): Promise<boolean> => {
    if (!isInitialized.value) {
      await initializeAuth()
    }

    if (isAuthenticated.value) {
      await router.push('/')
      return false
    }

    return true
  }

  // Utility functions
  const hasPermission = (permission: string): boolean => {
    // Implement permission checking logic here
    // For now, return true for authenticated users
    return isAuthenticated.value
  }

  const hasRole = (role: string): boolean => {
    // Implement role checking logic here
    // For now, return true for authenticated users
    return isAuthenticated.value
  }

  const isCurrentUser = (userId: number): boolean => {
    return user.value?.id === userId
  }

  const getAuthHeader = (): string | null => {
    if (accessToken.value) {
      return `Bearer ${accessToken.value}`
    }
    return null
  }

  const isSessionValid = (): boolean => {
    return isAuthenticated.value && !isTokenExpired.value
  }

  // Token management
  const scheduleTokenRefresh = () => {
    if (!authStore.tokenExpiresAt) return

    const expiresAt = authStore.tokenExpiresAt
    const now = Date.now()
    const timeUntilExpiry = expiresAt - now
    const refreshTime = Math.max(timeUntilExpiry - 5 * 60 * 1000, 60 * 1000) // 5 minutes before expiry, minimum 1 minute

    if (refreshTime > 0) {
      setTimeout(async () => {
        if (isAuthenticated.value && !isTokenExpired.value) {
          const success = await refreshToken()
          if (success) {
            scheduleTokenRefresh() // Schedule next refresh
          }
        }
      }, refreshTime)
    }
  }

  // Auto-refresh token setup
  if (isAuthenticated.value) {
    scheduleTokenRefresh()
  }

  // Form validation helpers
  const validateLoginForm = (credentials: LoginCredentials): string[] => {
    const errors: string[] = []

    if (!credentials.username?.trim()) {
      errors.push('Nome utente è richiesto')
    }

    if (!credentials.password?.trim()) {
      errors.push('Password è richiesta')
    }

    return errors
  }

  const validateRegisterForm = (credentials: RegisterCredentials): string[] => {
    const errors: string[] = []

    if (!credentials.username?.trim()) {
      errors.push('Nome utente è richiesto')
    } else if (credentials.username.length < 3) {
      errors.push('Nome utente deve essere di almeno 3 caratteri')
    }

    if (!credentials.email?.trim()) {
      errors.push('Email è richiesta')
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(credentials.email)) {
      errors.push('Email non valida')
    }

    if (!credentials.password?.trim()) {
      errors.push('Password è richiesta')
    } else if (credentials.password.length < 8) {
      errors.push('Password deve essere di almeno 8 caratteri')
    }

    return errors
  }

  // Session management
  const extendSession = async (): Promise<void> => {
    if (isAuthenticated.value) {
      await verifyToken()
    }
  }

  const clearSession = (): void => {
    authStore.clearAuthData()
  }

  return {
    // State
    user,
    isAuthenticated,
    isLoading,
    isInitialized,
    userFullName,
    userInitials,
    accessToken,
    isTokenExpired,

    // Actions
    initializeAuth,
    login,
    register,
    logout,
    updateProfile,
    refreshToken,
    verifyToken,

    // Navigation
    redirectToLogin,
    redirectAfterLogin,

    // Guards
    requireAuth,
    requireGuest,

    // Utilities
    hasPermission,
    hasRole,
    isCurrentUser,
    getAuthHeader,
    isSessionValid,
    scheduleTokenRefresh,

    // Validation
    validateLoginForm,
    validateRegisterForm,

    // Session
    extendSession,
    clearSession
  }
}