import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { 
  User, 
  AuthState, 
  LoginCredentials, 
  RegisterCredentials, 
  AuthResponse 
} from '../types/auth'
import { authApi } from '../services/authApi'
import { useNotifications } from '../composables/useNotifications'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const isLoading = ref(false)
  const isInitialized = ref(false)
  const tokenExpiresAt = ref<number | null>(null)

  // Getters
  const isAuthenticated = computed(() => {
    const result = !!user.value && !!accessToken.value && !isTokenExpired.value
    console.log('🔍 isAuthenticated computed:', {
      hasUser: !!user.value,
      hasAccessToken: !!accessToken.value,
      isTokenExpired: isTokenExpired.value,
      result,
      userValue: user.value?.username,
      tokenValue: accessToken.value?.slice(0, 10) + '...'
    })
    return result
  })

  const isTokenExpired = computed(() => {
    if (!tokenExpiresAt.value) return true
    return Date.now() >= tokenExpiresAt.value
  })

  const userFullName = computed(() => {
    if (!user.value) return ''
    const { firstName, lastName } = user.value
    return [firstName, lastName].filter(Boolean).join(' ') || user.value.username
  })

  const userInitials = computed(() => {
    if (!user.value) return ''
    const { firstName, lastName } = user.value
    if (firstName && lastName) {
      return `${firstName[0]}${lastName[0]}`.toUpperCase()
    }
    return user.value.username.slice(0, 2).toUpperCase()
  })

  // Actions
  const { showSuccess, showError } = useNotifications()

  const initializeAuth = async () => {
    if (isInitialized.value) return

    console.log('🔧 Initializing auth...')
    try {
      // Load auth data from localStorage
      const savedToken = localStorage.getItem('accessToken')
      const savedRefreshToken = localStorage.getItem('refreshToken')
      const savedUser = localStorage.getItem('user')
      const savedExpiresAt = localStorage.getItem('tokenExpiresAt')
      
      console.log('📦 Found in localStorage:', {
        hasToken: !!savedToken,
        hasRefreshToken: !!savedRefreshToken,
        hasUser: !!savedUser,
        expiresAt: savedExpiresAt
      })

      if (savedToken && savedRefreshToken && savedUser) {
        console.log('✅ Restoring auth state from localStorage')
        accessToken.value = savedToken
        refreshToken.value = savedRefreshToken
        user.value = JSON.parse(savedUser)
        tokenExpiresAt.value = savedExpiresAt ? parseInt(savedExpiresAt) : null

        console.log('🔐 Auth state restored:', {
          isAuthenticated: isAuthenticated.value,
          isTokenExpired: isTokenExpired.value,
          user: user.value?.username
        })

        // Check if token is expired and try to refresh
        if (isTokenExpired.value && refreshToken.value) {
          console.log('🔄 Token expired, refreshing...')
          await refreshAccessToken()
        } else {
          console.log('✅ Token is still valid, skipping backend verification during initialization')
          // During initialization, we trust the localStorage data
          // Backend verification can happen later when actually making API calls
        }
      } else {
        console.log('❌ No complete auth data found in localStorage')
      }
    } catch (error) {
      console.error('Failed to initialize auth:', error)
      clearAuthData()
    } finally {
      isInitialized.value = true
    }
  }

  const login = async (credentials: LoginCredentials): Promise<boolean> => {
    isLoading.value = true
    try {
      const response = await authApi.login(credentials)
      
      setAuthData(response)
      showSuccess('Accesso effettuato con successo!')
      return true
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Errore durante l\'accesso'
      showError(errorMessage)
      return false
    } finally {
      isLoading.value = false
    }
  }

  const register = async (credentials: RegisterCredentials): Promise<boolean> => {
    isLoading.value = true
    try {
      const response = await authApi.register(credentials)
      
      setAuthData(response)
      showSuccess('Registrazione completata con successo!')
      return true
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Errore durante la registrazione'
      showError(errorMessage)
      return false
    } finally {
      isLoading.value = false
    }
  }

  const logout = async (): Promise<void> => {
    try {
      if (refreshToken.value) {
        await authApi.logout(refreshToken.value)
      }
    } catch (error) {
      console.error('Error during logout:', error)
    } finally {
      clearAuthData()
      showSuccess('Disconnesso con successo!')
    }
  }

  const refreshAccessToken = async (): Promise<boolean> => {
    if (!refreshToken.value) {
      clearAuthData()
      return false
    }

    try {
      const response = await authApi.refreshToken(refreshToken.value)
      
      accessToken.value = response.accessToken
      refreshToken.value = response.refreshToken
      tokenExpiresAt.value = Date.now() + (response.expiresIn * 1000)
      
      // Save to localStorage
      localStorage.setItem('accessToken', response.accessToken)
      localStorage.setItem('refreshToken', response.refreshToken)
      localStorage.setItem('tokenExpiresAt', tokenExpiresAt.value.toString())
      
      return true
    } catch (error) {
      console.error('Failed to refresh token:', error)
      clearAuthData()
      return false
    }
  }

  const verifyToken = async (duringInitialization = false): Promise<boolean> => {
    try {
      console.log('🔍 verifyToken: Making API call to get profile...')
      const userProfile = await authApi.getProfile()
      console.log('🔍 verifyToken: Received user profile:', userProfile)
      
      if (!userProfile) {
        console.warn('⚠️ verifyToken: Received null/undefined user profile')
        return false
      }
      
      console.log('🔍 verifyToken: Setting user.value to:', userProfile.username)
      user.value = userProfile
      
      // Update user data in localStorage
      localStorage.setItem('user', JSON.stringify(userProfile))
      console.log('🔍 verifyToken: Updated localStorage with user data')
      return true
    } catch (error) {
      console.error('Token verification failed:', error)
      
      // During initialization, be more lenient with errors
      // Don't clear auth data immediately - the token might still work for other endpoints
      if (!duringInitialization && (error.status === 401 || error.status === 403)) {
        console.log('🧹 Clearing auth data due to 401/403 error (not during initialization)')
        clearAuthData()
      } else if (duringInitialization) {
        console.log('⚠️ Token verification failed during initialization - keeping stored auth data for now')
      }
      return false
    }
  }

  const updateProfile = async (profileData: Partial<User>): Promise<boolean> => {
    isLoading.value = true
    try {
      const updatedUser = await authApi.updateProfile(profileData)
      
      user.value = updatedUser
      localStorage.setItem('user', JSON.stringify(updatedUser))
      
      showSuccess('Profilo aggiornato con successo!')
      return true
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Errore durante l\'aggiornamento del profilo'
      showError(errorMessage)
      return false
    } finally {
      isLoading.value = false
    }
  }

  const setAuthData = (authResponse: AuthResponse) => {
    user.value = authResponse.user
    accessToken.value = authResponse.accessToken
    refreshToken.value = authResponse.refreshToken
    tokenExpiresAt.value = Date.now() + (authResponse.expiresIn * 1000)

    // Save to localStorage
    localStorage.setItem('user', JSON.stringify(authResponse.user))
    localStorage.setItem('accessToken', authResponse.accessToken)
    localStorage.setItem('refreshToken', authResponse.refreshToken)
    localStorage.setItem('tokenExpiresAt', tokenExpiresAt.value.toString())
  }

  const clearAuthData = () => {
    console.log('🧹 Clearing auth data')
    console.log('🧹 Stack trace:', new Error().stack)
    user.value = null
    accessToken.value = null
    refreshToken.value = null
    tokenExpiresAt.value = null

    // Clear localStorage
    localStorage.removeItem('user')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('tokenExpiresAt')
  }

  const getAuthState = (): AuthState => ({
    user: user.value,
    accessToken: accessToken.value,
    refreshToken: refreshToken.value,
    isAuthenticated: isAuthenticated.value,
    isLoading: isLoading.value,
    isInitialized: isInitialized.value,
    tokenExpiresAt: tokenExpiresAt.value
  })

  return {
    // State
    user,
    accessToken,
    refreshToken,
    isLoading,
    isInitialized,
    tokenExpiresAt,
    
    // Getters
    isAuthenticated,
    isTokenExpired,
    userFullName,
    userInitials,
    
    // Actions
    initializeAuth,
    login,
    register,
    logout,
    refreshAccessToken,
    verifyToken,
    updateProfile,
    setAuthData,
    clearAuthData,
    getAuthState
  }
})