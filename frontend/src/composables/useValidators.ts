import { useI18n } from 'vue-i18n'
import type { ValidationResult } from '../utils/validators'
import { isValidDateString, isValidTimeString } from '../utils/dateHelpers'

/**
 * Composable for i18n-aware validators
 * Use this in Vue components for locale-aware validation messages
 */
export function useValidators() {
  const { t } = useI18n()

  // Basic rules with i18n
  const rules = {
    required: (value: any): ValidationResult => ({
      isValid: value !== null && value !== undefined && value !== '',
      message: t('validation.required')
    }),

    email: (value: string): ValidationResult => {
      if (!value) return { isValid: true }
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      return {
        isValid: emailRegex.test(value),
        message: t('validation.email')
      }
    },

    minLength: (length: number) => (value: string): ValidationResult => ({
      isValid: !value || value.length >= length,
      message: t('validation.minLength', { length })
    }),

    maxLength: (length: number) => (value: string): ValidationResult => ({
      isValid: !value || value.length <= length,
      message: t('validation.maxLength', { length })
    }),

    numeric: (value: string): ValidationResult => ({
      isValid: !value || /^\d+$/.test(value),
      message: t('validation.numeric')
    }),

    alphanumeric: (value: string): ValidationResult => ({
      isValid: !value || /^[a-zA-Z0-9]+$/.test(value),
      message: t('validation.alphanumeric')
    }),

    minValue: (min: number) => (value: number): ValidationResult => ({
      isValid: value == null || value >= min,
      message: t('validation.minValue', { min })
    }),

    maxValue: (max: number) => (value: number): ValidationResult => ({
      isValid: value == null || value <= max,
      message: t('validation.maxValue', { max })
    }),

    url: (value: string): ValidationResult => {
      if (!value) return { isValid: true }
      try {
        new URL(value)
        return { isValid: true }
      } catch {
        return {
          isValid: false,
          message: t('validation.url')
        }
      }
    },

    phone: (value: string): ValidationResult => {
      if (!value) return { isValid: true }
      const phoneRegex = /^[\+]?[1-9][\d]{5,15}$/
      return {
        isValid: phoneRegex.test(value.replace(/\s/g, '')),
        message: t('validation.phone')
      }
    }
  }

  // Auth validators with i18n
  const authValidators = {
    username: (username: string): ValidationResult => {
      const trimmed = username?.trim()

      if (!trimmed) {
        return { isValid: false, message: t('validation.auth.usernameRequired') }
      }

      if (trimmed.length < 3) {
        return { isValid: false, message: t('validation.auth.usernameMinLength', { min: 3 }) }
      }

      if (trimmed.length > 50) {
        return { isValid: false, message: t('validation.auth.usernameMaxLength', { max: 50 }) }
      }

      if (!/^[a-zA-Z0-9._-]+$/.test(trimmed)) {
        return { isValid: false, message: t('validation.auth.usernameInvalidChars') }
      }

      return { isValid: true }
    },

    password: (password: string): ValidationResult => {
      if (!password) {
        return { isValid: false, message: t('validation.auth.passwordRequired') }
      }

      if (password.length < 8) {
        return { isValid: false, message: t('validation.auth.passwordMinLength', { min: 8 }) }
      }

      if (password.length > 128) {
        return { isValid: false, message: t('validation.auth.passwordMaxLength', { max: 128 }) }
      }

      if (!/[a-z]/.test(password)) {
        return { isValid: false, message: t('validation.auth.passwordLowercase') }
      }

      if (!/[A-Z]/.test(password)) {
        return { isValid: false, message: t('validation.auth.passwordUppercase') }
      }

      if (!/[0-9]/.test(password)) {
        return { isValid: false, message: t('validation.auth.passwordNumber') }
      }

      return { isValid: true }
    },

    confirmPassword: (password: string, confirmPassword: string): ValidationResult => {
      if (!confirmPassword) {
        return { isValid: false, message: t('validation.auth.confirmPasswordRequired') }
      }

      if (password !== confirmPassword) {
        return { isValid: false, message: t('validation.auth.confirmPasswordMismatch') }
      }

      return { isValid: true }
    },

    email: (email: string): ValidationResult => {
      const trimmed = email?.trim()

      if (!trimmed) {
        return { isValid: false, message: t('validation.auth.emailRequired') }
      }

      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(trimmed)) {
        return { isValid: false, message: t('validation.auth.emailInvalid') }
      }

      if (trimmed.length > 255) {
        return { isValid: false, message: t('validation.auth.emailMaxLength', { max: 255 }) }
      }

      return { isValid: true }
    },

    name: (name: string): ValidationResult => {
      if (!name) return { isValid: true } // Optional field

      const trimmed = name.trim()

      if (trimmed.length > 100) {
        return { isValid: false, message: t('validation.auth.nameMaxLength', { max: 100 }) }
      }

      if (!/^[a-zA-ZÀ-ÿ\s'-]+$/.test(trimmed)) {
        return { isValid: false, message: t('validation.auth.nameInvalidChars') }
      }

      return { isValid: true }
    }
  }

  // Task validators with i18n
  const taskValidators = {
    title: (title: string): ValidationResult => {
      const trimmed = title?.trim()

      if (!trimmed) {
        return { isValid: false, message: t('validation.task.titleRequired') }
      }

      if (trimmed.length > 255) {
        return { isValid: false, message: t('validation.task.titleMaxLength', { max: 255 }) }
      }

      return { isValid: true }
    },

    description: (description: string): ValidationResult => {
      if (!description) return { isValid: true }

      if (description.length > 1000) {
        return { isValid: false, message: t('validation.task.descriptionMaxLength', { max: 1000 }) }
      }

      return { isValid: true }
    },

    priority: (priority: string): ValidationResult => {
      if (!priority) {
        return { isValid: false, message: t('validation.task.priorityRequired') }
      }

      const validPriorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
      if (!validPriorities.includes(priority)) {
        return { isValid: false, message: t('validation.task.priorityInvalid') }
      }

      return { isValid: true }
    }
  }

  return {
    rules,
    authValidators,
    taskValidators
  }
}
