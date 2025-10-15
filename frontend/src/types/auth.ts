export interface User {
  id: number
  username: string
  email: string
  emailVerified?: boolean
  firstName?: string
  lastName?: string
  fullName?: string
  displayName?: string
  timezone?: string
  twoFactorEnabled?: boolean
  avatar?: string
  createdAt: string
  updatedAt: string
}

export interface LoginCredentials {
  username: string
  password: string
  twoFactorCode?: string
}

export interface RegisterCredentials {
  username: string
  email: string
  password: string
  firstName?: string
  lastName?: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: User
  tokenType: string
  expiresIn: number
  requiresTwoFactor?: boolean
  requiresEmailVerification?: boolean
  message?: string
  success?: boolean
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface RefreshTokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  isLoading: boolean
  isInitialized: boolean
  tokenExpiresAt: number | null
}

export interface LoginFormData {
  username: string
  password: string
  remember: boolean
}

export interface RegisterFormData {
  username: string
  email: string
  password: string
  confirmPassword: string
  firstName: string
  lastName: string
  acceptTerms: boolean
}

// Password Reset interfaces
export interface ForgotPasswordRequest {
  email: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

export interface PasswordResetResponse {
  message: string
  success: boolean
}

// Password Reset form data interfaces
export interface ForgotPasswordFormData {
  email: string
}

export interface ResetPasswordFormData {
  newPassword: string
  confirmPassword: string
}

// Password validation rules (matching backend constraints)
export interface PasswordValidationRules {
  minLength: number
  maxLength: number
  requireLowercase: boolean
  requireUppercase: boolean
  requireDigit: boolean
  pattern: RegExp
}

export const PASSWORD_VALIDATION: PasswordValidationRules = {
  minLength: 8,
  maxLength: 128,
  requireLowercase: true,
  requireUppercase: true,
  requireDigit: true,
  pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).*$/
}