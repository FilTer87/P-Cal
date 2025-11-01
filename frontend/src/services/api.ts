import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse, ApiError } from '../types/api'
import { i18nGlobal } from '../i18n'

// Extended config for API requests with notification control
interface ExtendedAxiosRequestConfig extends AxiosRequestConfig {
  _showErrorNotification?: boolean
}

class ApiClient {
  private client: AxiosInstance
  private refreshPromise: Promise<any> | null = null

  constructor() {
    this.client = axios.create({
      baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    this.setupInterceptors()
  }

  private setupInterceptors() {
    // Request interceptor - Add auth token
    this.client.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('accessToken')
        const tokenExpiresAt = localStorage.getItem('tokenExpiresAt')
        const isExpired = tokenExpiresAt ? Date.now() > parseInt(tokenExpiresAt) : false
        
        console.debug('📤 Request interceptor:', {
          url: config.url,
          hasToken: !!token,
          tokenPreview: token ? token.slice(0, 20) + '...' : 'null',
          isExpired,
          expiresAt: tokenExpiresAt ? new Date(parseInt(tokenExpiresAt)).toLocaleString() : 'null'
        })
        
        if (token && !isExpired) {
          config.headers.Authorization = `Bearer ${token}`
        } else if (isExpired) {
          console.log('⚠️ Token is expired, removing from localStorage')
          localStorage.removeItem('accessToken')
          // Don't add Authorization header, this will cause a 401 which will trigger refresh
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // Response interceptor - Handle token refresh and errors
    this.client.interceptors.response.use(
      (response: AxiosResponse) => {
        // Handle 202 Accepted (2FA required) as success with data
        if (response.status === 202 && response.data?.requiresTwoFactor) {
          return response
        }
        return response
      },
      async (error) => {
        
        const originalRequest = error.config

        // Handle 401 Unauthorized - Token expired/invalid
        if ((error.response?.status === 401) && !originalRequest._retry) {
          console.log(`🔒 ${error.response?.status} Unauthorized detected, attempting token refresh...`)
          originalRequest._retry = true

          try {
            // If a refresh is already in progress, wait for it
            if (this.refreshPromise) {
              console.log('🔄 Refresh already in progress, waiting...')
              await this.refreshPromise
              
              // Check if refresh succeeded by verifying localStorage
              const newAccessToken = localStorage.getItem('accessToken')
              if (newAccessToken) {
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
                return this.client(originalRequest)
              } else {
                this.handleAuthenticationFailure()
                return Promise.reject(error)
              }
            }

            const refreshToken = localStorage.getItem('refreshToken')
            console.log('🔄 API interceptor: refreshToken from localStorage:', refreshToken)
            console.log('🔄 All localStorage tokens:', {
              accessToken: localStorage.getItem('accessToken')?.slice(0, 20) + '...',
              refreshToken: localStorage.getItem('refreshToken')?.slice(0, 20) + '...',
              tokenExpiresAt: localStorage.getItem('tokenExpiresAt')
            })
            
            if (!refreshToken) {
              console.log('❌ No refresh token available')
              this.handleAuthenticationFailure()
              return Promise.reject(error)
            }

            console.log('📞 API interceptor: Making refresh call with token:', refreshToken)
            console.log('📞 API interceptor: Token type and value:', typeof refreshToken, refreshToken)
            
            // Ensure refreshToken is not undefined or "undefined"
            if (refreshToken === 'undefined' || refreshToken === null) {
              console.error('❌ RefreshToken is invalid:', refreshToken)
              this.handleAuthenticationFailure()
              return Promise.reject(error)
            }
            
            // Start refresh process
            this.refreshPromise = axios.post(
              `${import.meta.env.VITE_API_URL || 'http://localhost:8080/api'}/auth/refresh`,
              { refreshToken: refreshToken }
            )

            const response = await this.refreshPromise
            console.log('📨 Refresh API response:', {
              status: response.status,
              data: response.data,
              hasAccessToken: !!response.data.accessToken,
              hasRefreshToken: !!response.data.refreshToken,
              hasExpiresIn: !!response.data.expiresIn
            })
            
            const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data
            
            // Validate essential fields - refreshToken is optional (might be reused)
            if (!accessToken || !expiresIn) {
              console.error('❌ Invalid refresh response - missing accessToken or expiresIn:', response.data)
              this.handleAuthenticationFailure()
              return Promise.reject(new Error('Invalid refresh token response'))
            }
            
            // Use new refreshToken if provided, otherwise keep the existing one
            const finalRefreshToken = newRefreshToken || refreshToken
            console.log('🔄 Using refreshToken:', {
              hasNewRefreshToken: !!newRefreshToken,
              usingExisting: !newRefreshToken,
              finalToken: finalRefreshToken ? finalRefreshToken.slice(0, 20) + '...' : 'undefined'
            })
            
            // Calculate token expiration
            const tokenExpiresAt = Date.now() + (expiresIn * 1000)
            
            // Update localStorage
            localStorage.setItem('accessToken', accessToken)
            localStorage.setItem('refreshToken', finalRefreshToken)
            localStorage.setItem('tokenExpiresAt', tokenExpiresAt.toString())
            
            // Update auth store if available
            this.updateAuthStore(accessToken, finalRefreshToken, tokenExpiresAt)
            
            // Update the original request with new token
            originalRequest.headers.Authorization = `Bearer ${accessToken}`
            
            console.log('✅ Token refreshed successfully')
            
            // Clear refresh promise
            this.refreshPromise = null
            
            // Retry the original request
            return this.client(originalRequest)
            
          } catch (refreshError) {
            console.error('❌ Token refresh failed:', refreshError)
            
            // Clear refresh promise
            this.refreshPromise = null
            
            // Refresh failed - redirect to login
            this.handleAuthenticationFailure()
            return Promise.reject(refreshError)
          }
        }

        // Handle other errors
        const showNotification = error.config?._showErrorNotification !== false
        return Promise.reject(this.handleError(error, showNotification))
      }
    )
  }

  private updateAuthStore(accessToken: string, refreshToken: string, tokenExpiresAt: number) {
    try {
      // Emit custom event to notify auth store
      console.log('📡 Emitting auth-token-refreshed event with:', {
        accessToken: accessToken ? accessToken.slice(0, 20) + '...' : 'undefined',
        refreshToken: refreshToken ? refreshToken.slice(0, 20) + '...' : 'undefined', 
        tokenExpiresAt: new Date(tokenExpiresAt).toLocaleString()
      })
      window.dispatchEvent(new CustomEvent('auth-token-refreshed', {
        detail: { accessToken, refreshToken, tokenExpiresAt }
      }))
    } catch (error) {
      console.warn('Could not emit auth token refresh event:', error)
    }
  }

  private handleAuthenticationFailure() {
    // Clear stored auth data
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    localStorage.removeItem('tokenExpiresAt')
    
    // Emit event to notify auth store
    window.dispatchEvent(new CustomEvent('auth-failure'))
    
    // Redirect to login if not already there
    if (!window.location.pathname.includes('/login')) {
      window.location.href = '/login'
    }
  }

  private handleError(error: any, showNotification = true): ApiError {
    const t = i18nGlobal.t
    const apiError: ApiError = {
      message: t('api.errors.unexpected'),
      timestamp: new Date().toISOString()
    }

    if (error.response) {
      // Server responded with error status
      const { data, status } = error.response

      apiError.status = status
      apiError.message = data?.message || this.getStatusMessage(status)
      apiError.error = data?.error
      apiError.path = data?.path
      apiError.details = data?.details

      // Handle validation errors
      if (data?.errors && Array.isArray(data.errors)) {
        apiError.details = { validationErrors: data.errors }
      }
    } else if (error.request) {
      // Network error
      apiError.message = t('api.errors.networkError')
    } else {
      // Request configuration error
      apiError.message = error.message || t('api.errors.configError')
    }

    // Show notification automatically (unless disabled)
    if (showNotification && apiError.status !== 401 && apiError.status !== 403) {
      // Don't show notifications for auth errors (handled by interceptor)
      window.dispatchEvent(new CustomEvent('api-error', {
        detail: { message: apiError.message, status: apiError.status }
      }))
    }

    return apiError
  }

  private getStatusMessage(status: number): string {
    const t = i18nGlobal.t
    const statusKey = `api.errors.status.${status}`

    // Check if translation exists for this specific status
    if (i18nGlobal.te(statusKey)) {
      return t(statusKey)
    }

    // Return default error message with status code
    return t('api.errors.status.default', { status })
  }

  // HTTP Methods
  async get<T = any>(url: string, config?: ExtendedAxiosRequestConfig): Promise<T> {
    const response = await this.client.get<ApiResponse<T> | T>(url, config)
    
    // Handle both wrapped ApiResponse and direct responses
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return response.data.data
    }
    
    return response.data as T
  }

  async post<T = any>(url: string, data?: any, config?: ExtendedAxiosRequestConfig): Promise<T> {
    const response = await this.client.post<ApiResponse<T> | T>(url, data, config)
    
    // Handle both wrapped ApiResponse and direct responses (for auth endpoints)
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return response.data.data
    }
    
    return response.data as T
  }

  async put<T = any>(url: string, data?: any, config?: ExtendedAxiosRequestConfig): Promise<T> {
    const response = await this.client.put<ApiResponse<T> | T>(url, data, config)
    
    // Handle both wrapped ApiResponse and direct responses
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return response.data.data
    }
    
    return response.data as T
  }

  async patch<T = any>(url: string, data?: any, config?: ExtendedAxiosRequestConfig): Promise<T> {
    const response = await this.client.patch<ApiResponse<T> | T>(url, data, config)
    
    // Handle both wrapped ApiResponse and direct responses
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return response.data.data
    }
    
    return response.data as T
  }

  async delete<T = any>(url: string, config?: ExtendedAxiosRequestConfig): Promise<T> {
    const response = await this.client.delete<ApiResponse<T> | T>(url, config)

    // Handle both wrapped ApiResponse and direct responses
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return response.data.data
    }

    return response.data as T
  }

  // Raw response methods (for cases where you need the full response)
  async getRaw<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.get<T>(url, config)
  }

  async postRaw<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.post<T>(url, data, config)
  }

  async putRaw<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.put<T>(url, data, config)
  }

  async patchRaw<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.patch<T>(url, data, config)
  }

  async deleteRaw<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.client.delete<T>(url, config)
  }

  // Upload file with progress
  async uploadFile<T = any>(
    url: string, 
    file: File, 
    onProgress?: (progressEvent: any) => void,
    additionalData?: Record<string, any>
  ): Promise<T> {
    const formData = new FormData()
    formData.append('file', file)
    
    if (additionalData) {
      Object.entries(additionalData).forEach(([key, value]) => {
        formData.append(key, String(value))
      })
    }

    const config: AxiosRequestConfig = {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: onProgress
    }

    const response = await this.client.post<ApiResponse<T>>(url, formData, config)
    return response.data.data
  }

  // Get the raw axios instance for advanced usage
  getClient(): AxiosInstance {
    return this.client
  }

  // Update base URL
  setBaseURL(baseURL: string) {
    this.client.defaults.baseURL = baseURL
  }

  // Update timeout
  setTimeout(timeout: number) {
    this.client.defaults.timeout = timeout
  }

  // Update default headers
  setHeader(key: string, value: string) {
    this.client.defaults.headers.common[key] = value
  }

  // Remove header
  removeHeader(key: string) {
    delete this.client.defaults.headers.common[key]
  }

  // Utility method to call APIs without showing error notifications
  withoutErrorNotification() {
    return {
      get: <T = any>(url: string, config?: AxiosRequestConfig) => 
        this.get<T>(url, { ...config, _showErrorNotification: false }),
      
      post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => 
        this.post<T>(url, data, { ...config, _showErrorNotification: false }),
      
      put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => 
        this.put<T>(url, data, { ...config, _showErrorNotification: false }),
      
      patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => 
        this.patch<T>(url, data, { ...config, _showErrorNotification: false }),
      
      delete: <T = any>(url: string, config?: AxiosRequestConfig) =>
        this.delete<T>(url, { ...config, _showErrorNotification: false })
    }
  }
}

// Create and export a singleton instance
export const apiClient = new ApiClient()
export default apiClient