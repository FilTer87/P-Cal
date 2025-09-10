import { apiClient } from './api'
import type { 
  LoginCredentials, 
  RegisterCredentials, 
  AuthResponse, 
  RefreshTokenResponse,
  User 
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
    return apiClient.put<void>(`${API_ENDPOINTS.AUTH.PROFILE}/password`, {
      currentPassword,
      newPassword
    })
  }

  /**
   * Request password reset
   */
  async requestPasswordReset(email: string): Promise<void> {
    return apiClient.post<void>('/auth/forgot-password', {
      email
    })
  }

  /**
   * Reset password with token
   */
  async resetPassword(token: string, newPassword: string): Promise<void> {
    return apiClient.post<void>('/auth/reset-password', {
      token,
      newPassword
    })
  }

  /**
   * Verify email with token
   */
  async verifyEmail(token: string): Promise<void> {
    return apiClient.post<void>('/auth/verify-email', {
      token
    })
  }

  /**
   * Resend email verification
   */
  async resendEmailVerification(): Promise<void> {
    return apiClient.post<void>('/auth/resend-verification')
  }

  /**
   * Check if username is available
   */
  async checkUsernameAvailability(username: string): Promise<boolean> {
    try {
      const response = await apiClient.get<{ available: boolean }>(`/auth/check-username/${username}`)
      return response.available
    } catch (error) {
      return false
    }
  }

  /**
   * Check if email is available
   */
  async checkEmailAvailability(email: string): Promise<boolean> {
    try {
      const response = await apiClient.get<{ available: boolean }>(`/auth/check-email/${encodeURIComponent(email)}`)
      return response.available
    } catch (error) {
      return false
    }
  }

  /**
   * Get user sessions (active login sessions)
   */
  async getSessions(): Promise<any[]> {
    return apiClient.get<any[]>('/auth/sessions')
  }

  /**
   * Revoke a specific session
   */
  async revokeSession(sessionId: string): Promise<void> {
    return apiClient.delete<void>(`/auth/sessions/${sessionId}`)
  }

  /**
   * Revoke all sessions except current
   */
  async revokeAllSessions(): Promise<void> {
    return apiClient.delete<void>('/auth/sessions')
  }

  /**
   * Delete user account
   */
  async deleteAccount(password: string): Promise<void> {
    return apiClient.delete<void>('/auth/account', {
      data: { password }
    })
  }

  /**
   * Get account statistics
   */
  async getAccountStats(): Promise<{
    createdAt: string
    lastLoginAt: string
    taskCount: number
    reminderCount: number
  }> {
    return apiClient.get('/auth/stats')
  }

  /**
   * Update user preferences
   */
  async updatePreferences(preferences: {
    theme?: 'light' | 'dark' | 'system'
    language?: string
    timezone?: string
    emailNotifications?: boolean
    reminderNotifications?: boolean
  }): Promise<void> {
    return apiClient.put<void>('/auth/preferences', preferences)
  }

  /**
   * Get user preferences
   */
  async getPreferences(): Promise<{
    theme: 'light' | 'dark' | 'system'
    language: string
    timezone: string
    emailNotifications: boolean
    reminderNotifications: boolean
  }> {
    return apiClient.get('/auth/preferences')
  }

  /**
   * Enable two-factor authentication
   */
  async enableTwoFactor(): Promise<{
    qrCode: string
    secret: string
    backupCodes: string[]
  }> {
    return apiClient.post('/auth/2fa/enable')
  }

  /**
   * Confirm two-factor authentication setup
   */
  async confirmTwoFactor(code: string): Promise<void> {
    return apiClient.post<void>('/auth/2fa/confirm', {
      code
    })
  }

  /**
   * Disable two-factor authentication
   */
  async disableTwoFactor(password: string, code: string): Promise<void> {
    return apiClient.post<void>('/auth/2fa/disable', {
      password,
      code
    })
  }

  /**
   * Generate new backup codes for 2FA
   */
  async generateBackupCodes(password: string): Promise<string[]> {
    return apiClient.post('/auth/2fa/backup-codes', {
      password
    })
  }

  /**
   * Export user data (GDPR compliance)
   */
  async exportData(): Promise<Blob> {
    const response = await apiClient.getRaw('/auth/export', {
      responseType: 'blob'
    })
    return response.data
  }
}

export const authApi = new AuthApi()