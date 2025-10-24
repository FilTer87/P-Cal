export interface ApiResponse<T = any> {
  data: T
  message?: string
  success: boolean
  timestamp: string
}

export interface PaginatedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface ApiError {
  message: string
  error?: string
  timestamp: string
  path?: string
  status?: number
  details?: Record<string, any>
}

export interface ValidationError {
  field: string
  message: string
  rejectedValue?: any
}

export interface ValidationErrorResponse extends ApiError {
  errors: ValidationError[]
}

// HTTP request configuration
export interface ApiRequestConfig {
  baseURL?: string
  timeout?: number
  headers?: Record<string, string>
  params?: Record<string, any>
  withCredentials?: boolean
}

// Loading states
export interface LoadingState {
  isLoading: boolean
  error: string | null
}

export interface AsyncState<T> extends LoadingState {
  data: T | null
}

// Pagination parameters
export interface PaginationParams {
  page?: number
  size?: number
  sort?: string
  direction?: 'asc' | 'desc'
}

// Date range parameters
export interface DateRangeParams {
  startDate?: string
  endDate?: string
}

// Search parameters
export interface SearchParams {
  query?: string
  filters?: Record<string, any>
}

// API endpoint paths
export const API_ENDPOINTS = {
  // Auth endpoints
  AUTH: {
    BASE: '/auth',
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    REFRESH: '/auth/refresh',
    LOGOUT: '/auth/logout',
    PROFILE: '/auth/profile',
    PASSWORD: '/auth/change-password',
    FORGOT_PASSWORD: '/auth/forgot-password',
    RESET_PASSWORD: '/auth/reset-password'
  },
  // Task endpoints
  TASKS: {
    BASE: '/tasks',
    BY_ID: (id: string) => `/tasks/${id}`,
    BY_DATE_RANGE: '/tasks/date-range',
    SEARCH: '/tasks/search',
    STATS: '/tasks/stats'
  },
  // Reminder endpoints
  REMINDERS: {
    BASE: '/reminders',
    BY_ID: (id: number) => `/reminders/${id}`,
    BY_TASK: (taskId: string) => `/tasks/${taskId}/reminders`,
    UPCOMING: '/reminders/upcoming'
  }
} as const