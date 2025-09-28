import { apiClient } from './api'
import type {
  LoginCredentials,
  RegisterCredentials,
  AuthResponse,
  RefreshTokenResponse,
  User,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  PasswordResetResponse
} from '../types/auth'
import { API_ENDPOINTS } from '../types/api'

export class AuthApi {
  /**
   * Login with username and password
   */
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    return apiClient.post<AuthResponse>(API_ENDPOINTS.AUTH.LOGIN, credentials)
  }

  /**
   * Register a new user
   */
  async register(credentials: RegisterCredentials): Promise<AuthResponse> {
    return apiClient.post<AuthResponse>(API_ENDPOINTS.AUTH.REGISTER, credentials)
  }

  /**
   * Refresh access token using refresh token
   */
  async refreshToken(refreshToken: string): Promise<RefreshTokenResponse> {
    return apiClient.post<RefreshTokenResponse>(API_ENDPOINTS.AUTH.REFRESH, {
      refreshToken
    })
  }

  /**
   * Logout - invalidate refresh token
   */
  async logout(refreshToken: string): Promise<void> {
    return apiClient.post<void>(API_ENDPOINTS.AUTH.LOGOUT, {
      refreshToken
    })
  }

  /**
   * Get current user profile
   */
  async getProfile(): Promise<User> {
    return apiClient.get<User>(API_ENDPOINTS.AUTH.PROFILE)
  }

  /**
   * Update user profile
   */
  async updateProfile(profileData: Partial<User>): Promise<User> {
    return apiClient.put<User>(API_ENDPOINTS.AUTH.PROFILE, profileData)
  }

  /**
   * Change password
   */
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    return apiClient.post<void>(`${API_ENDPOINTS.AUTH.PASSWORD}`, {
      currentPassword,
      newPassword
    })
  }

  /**
   * Request password reset - sends reset email to user
   */
  async forgotPassword(request: ForgotPasswordRequest): Promise<PasswordResetResponse> {
    return apiClient.post<PasswordResetResponse>(API_ENDPOINTS.AUTH.FORGOT_PASSWORD, request)
  }

  /**
   * Reset password using token from email
   */
  async resetPassword(request: ResetPasswordRequest): Promise<PasswordResetResponse> {
    return apiClient.post<PasswordResetResponse>(API_ENDPOINTS.AUTH.RESET_PASSWORD, request)
  }


  /**
   * Verify email with token
   * // TODO - Unused
   */
  async verifyEmail(token: string): Promise<void> {
    return apiClient.post<void>('/auth/verify-email', {
      token
    })
  }

  /**
   * Resend email verification
   * // TODO - Unused
   */
  async resendEmailVerification(): Promise<void> {
    return apiClient.post<void>('/auth/resend-verification')
  }


  /**
   * Get user sessions (active login sessions)
   * // TODO - Unused
   */
  async getSessions(): Promise<any[]> {
    return apiClient.get<any[]>('/auth/sessions')
  }

  /**
   * Revoke a specific session
   * // TODO - Unused
   */
  async revokeSession(sessionId: string): Promise<void> {
    return apiClient.delete<void>(`/auth/sessions/${sessionId}`)
  }

  /**
   * Revoke all sessions except current
   * // TODO - Unused
   */
  async revokeAllSessions(): Promise<void> {
    return apiClient.delete<void>('/auth/sessions')
  }

  /**
   * Delete user account
   */
  async deleteAccount(password: string): Promise<void> {
    return apiClient.delete<void>('/auth/me', {
      data: { password }
    })
  }


  /**
   * Update user preferences
   */
  async updatePreferences(preferences: {
    theme?: 'light' | 'dark' | 'system'
    language?: string
    timezone?: string
    timeFormat?: '12h' | '24h'
    calendarView?: 'month' | 'week' | 'day' | 'agenda'
    emailNotifications?: boolean
    reminderNotifications?: boolean
    weekStartDay?: 0 | 1
  }): Promise<{
    theme: 'light' | 'dark' | 'system'
    language: string
    timezone: string
    timeFormat: '12h' | '24h'
    calendarView: 'month' | 'week' | 'day' | 'agenda'
    emailNotifications: boolean
    reminderNotifications: boolean
    weekStartDay: 0 | 1
  }> {
    return apiClient.put('/auth/preferences', preferences)
  }

  /**
   * Get user preferences
   */
  async getPreferences(): Promise<{
    theme: 'light' | 'dark' | 'system'
    language: string
    timezone: string
    timeFormat: '12h' | '24h'
    calendarView: 'month' | 'week' | 'day' | 'agenda'
    emailNotifications: boolean
    reminderNotifications: boolean
    weekStartDay: 0 | 1
  }> {
    return apiClient.get('/auth/preferences')
  }

  /**
   * Setup two-factor authentication (get QR code)
   */
  async setupTwoFactor(): Promise<{
    success: boolean
    secret: string
    qrCodeUrl: string
    manualEntryKey: string
  }> {
    return apiClient.post('/auth/2fa/setup')
  }

  /**
   * Enable two-factor authentication with verification code
   */
  async enableTwoFactor(secret: string, code: string): Promise<{
    success: boolean
    message: string
  }> {
    return apiClient.post('/auth/2fa/enable', {
      secret,
      code
    })
  }


  /**
   * Disable two-factor authentication
   */
  async disableTwoFactor(password: string): Promise<{
    success: boolean
    message: string
  }> {
    return apiClient.post('/auth/2fa/disable', {
      password
    })
  }

  /**
   * Export user data (GDPR compliance)
   */
  async exportData(): Promise<{ blob: Blob; filename: string }> {
    const response = await apiClient.getRaw('/auth/export', {
      responseType: 'blob'
    })

    const contentDisposition = response.headers['content-disposition']
    let filename = `privatecal-data-${new Date().toISOString().split('T')[0]}.json`

    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename="?(.+)"?/i)
      if (filenameMatch && filenameMatch[1]) {
        filename = filenameMatch[1].replace(/"/g, '')
      }
    }

    return { blob: response.data, filename }
  }
}

export const authApi = new AuthApi()