import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse, ApiError } from '../types/api'

class ApiClient {
  private client: AxiosInstance

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
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
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
        return response
      },
      async (error) => {
        const originalRequest = error.config

        // Handle 401 Unauthorized - Token expired
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true

          try {
            const refreshToken = localStorage.getItem('refreshToken')
            if (refreshToken) {
              const response = await axios.post(
                `${import.meta.env.VITE_API_URL || 'http://localhost:8080/api'}/auth/refresh`,
                { refreshToken }
              )

              const { accessToken, refreshToken: newRefreshToken } = response.data
              
              // Update stored tokens
              localStorage.setItem('accessToken', accessToken)
              localStorage.setItem('refreshToken', newRefreshToken)
              
              // Update the original request with new token
              originalRequest.headers.Authorization = `Bearer ${accessToken}`
              
              // Retry the original request
              return this.client(originalRequest)
            }
          } catch (refreshError) {
            // Refresh failed - redirect to login
            this.handleAuthenticationFailure()
            return Promise.reject(refreshError)
          }
        }

        // Handle other errors
        return Promise.reject(this.handleError(error))
      }
    )
  }

  private handleAuthenticationFailure() {
    // Clear stored auth data
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    localStorage.removeItem('tokenExpiresAt')
    
    // Redirect to login if not already there
    if (!window.location.pathname.includes('/login')) {
      window.location.href = '/login'
    }
  }

  private handleError(error: any): ApiError {
    const apiError: ApiError = {
      message: 'Si è verificato un errore imprevisto',
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
      apiError.message = 'Errore di connessione. Verifica la tua connessione internet.'
    } else {
      // Request configuration error
      apiError.message = error.message || 'Errore nella configurazione della richiesta'
    }

    return apiError
  }

  private getStatusMessage(status: number): string {
    const messages: Record<number, string> = {
      400: 'Richiesta non valida',
      401: 'Non autorizzato. Effettua l\'accesso.',
      403: 'Accesso negato',
      404: 'Risorsa non trovata',
      409: 'Conflitto - La risorsa esiste già',
      422: 'Dati non validi',
      429: 'Troppe richieste. Riprova più tardi.',
      500: 'Errore interno del server',
      502: 'Gateway non disponibile',
      503: 'Servizio non disponibile',
      504: 'Timeout del gateway'
    }

    return messages[status] || `Errore ${status}`
  }

  // HTTP Methods
  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.get<ApiResponse<T> | T>(url, config)
    
    // Handle both wrapped ApiResponse and direct responses
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return response.data.data
    }
    
    return response.data as T
  }

  async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.post<ApiResponse<T> | T>(url, data, config)
    
    // Handle both wrapped ApiResponse and direct responses (for auth endpoints)
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return response.data.data
    }
    
    return response.data as T
  }

  async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.put<ApiResponse<T> | T>(url, data, config)
    
    // Handle both wrapped ApiResponse and direct responses
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return response.data.data
    }
    
    return response.data as T
  }

  async patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.patch<ApiResponse<T> | T>(url, data, config)
    
    // Handle both wrapped ApiResponse and direct responses
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return response.data.data
    }
    
    return response.data as T
  }

  async delete<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.delete<ApiResponse<T> | T>(url, { ...config, data })
    
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
}

// Create and export a singleton instance
export const apiClient = new ApiClient()
export default apiClient