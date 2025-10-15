import { isValidDate, isValidDateString, isValidTimeString } from './dateHelpers'
import { i18nGlobal } from '../i18n'

export interface ValidationResult {
  isValid: boolean
  message?: string
}

export interface FormValidationResult {
  isValid: boolean
  errors: Record<string, string>
}

/**
 * Basic validation rules with i18n support
 */
export const rules = {
  required: (value: any): ValidationResult => {
    const { t } = i18nGlobal
    return {
      isValid: value !== null && value !== undefined && value !== '',
      message: t('validation.required')
    }
  },

  email: (value: string): ValidationResult => {
    const { t } = i18nGlobal
    if (!value) return { isValid: true }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return {
      isValid: emailRegex.test(value),
      message: t('validation.email')
    }
  },

  minLength: (length: number) => (value: string): ValidationResult => {
    const { t } = i18nGlobal
    return {
      isValid: !value || value.length >= length,
      message: t('validation.minLength', { length })
    }
  },

  maxLength: (length: number) => (value: string): ValidationResult => {
    const { t } = i18nGlobal
    return {
      isValid: !value || value.length <= length,
      message: t('validation.maxLength', { length })
    }
  },

  pattern: (regex: RegExp, message: string) => (value: string): ValidationResult => ({
    isValid: !value || regex.test(value),
    message
  }),

  numeric: (value: string): ValidationResult => {
    const { t } = i18nGlobal
    return {
      isValid: !value || /^\d+$/.test(value),
      message: t('validation.numeric')
    }
  },

  alphanumeric: (value: string): ValidationResult => {
    const { t } = i18nGlobal
    return {
      isValid: !value || /^[a-zA-Z0-9]+$/.test(value),
      message: t('validation.alphanumeric')
    }
  },

  minValue: (min: number) => (value: number): ValidationResult => {
    const { t } = i18nGlobal
    return {
      isValid: value == null || value >= min,
      message: t('validation.minValue', { min })
    }
  },

  maxValue: (max: number) => (value: number): ValidationResult => {
    const { t } = i18nGlobal
    return {
      isValid: value == null || value <= max,
      message: t('validation.maxValue', { max })
    }
  },

  date: (value: string): ValidationResult => ({
    isValid: !value || isValidDateString(value),
    message: 'Inserisci una data valida'
  }),

  time: (value: string): ValidationResult => ({
    isValid: !value || isValidTimeString(value),
    message: 'Inserisci un orario valido (HH:MM)'
  }),

  url: (value: string): ValidationResult => {
    const { t } = i18nGlobal
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
    const { t } = i18nGlobal
    if (!value) return { isValid: true }
    const phoneRegex = /^[\+]?[1-9][\d]{5,15}$/
    return {
      isValid: phoneRegex.test(value.replace(/\s/g, '')),
      message: t('validation.phone')
    }
  },

  match: (matchValue: string, fieldName: string) => (value: string): ValidationResult => ({
    isValid: !value || value === matchValue,
    message: `Deve corrispondere al campo ${fieldName}`
  })
}

/**
 * Authentication validation
 */
export const authValidators = {
  username: (username: string): ValidationResult => {
    const { t } = i18nGlobal
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

    if (!/^[a-zA-Z0-9_.-]+$/.test(trimmed)) {
      return { isValid: false, message: t('validation.auth.usernameInvalidChars') }
    }

    return { isValid: true }
  },

  password: (password: string): ValidationResult => {
    const { t } = i18nGlobal

    if (!password) {
      return { isValid: false, message: t('validation.auth.passwordRequired') }
    }

    if (password.length < 8) {
      return { isValid: false, message: t('validation.auth.passwordMinLength', { min: 8 }) }
    }

    if (password.length > 128) {
      return { isValid: false, message: t('validation.auth.passwordMaxLength', { max: 128 }) }
    }

    if (!/(?=.*[a-z])/.test(password)) {
      return { isValid: false, message: t('validation.auth.passwordLowercase') }
    }

    if (!/(?=.*[A-Z])/.test(password)) {
      return { isValid: false, message: t('validation.auth.passwordUppercase') }
    }

    if (!/(?=.*\d)/.test(password)) {
      return { isValid: false, message: t('validation.auth.passwordNumber') }
    }

    return { isValid: true }
  },

  confirmPassword: (password: string, confirmPassword: string): ValidationResult => {
    const { t } = i18nGlobal

    if (!confirmPassword) {
      return { isValid: false, message: t('validation.auth.confirmPasswordRequired') }
    }

    if (password !== confirmPassword) {
      return { isValid: false, message: t('validation.auth.confirmPasswordMismatch') }
    }

    return { isValid: true }
  },

  email: (email: string): ValidationResult => {
    const { t } = i18nGlobal
    const trimmed = email?.trim()

    if (!trimmed) {
      return { isValid: false, message: t('validation.auth.emailRequired') }
    }

    if (trimmed.length > 255) {
      return { isValid: false, message: t('validation.auth.emailMaxLength', { max: 255 }) }
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(trimmed)) {
      return { isValid: false, message: t('validation.auth.emailInvalid') }
    }

    return { isValid: true }
  },

  name: (name: string): ValidationResult => {
    const { t } = i18nGlobal
    if (!name) return { isValid: true } // Name is optional

    const trimmed = name.trim()

    if (trimmed.length > 100) {
      return { isValid: false, message: t('validation.auth.nameMaxLength', { max: 100 }) }
    }

    if (!/^[a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñçčšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð\s'-]+$/.test(trimmed)) {
      return { isValid: false, message: t('validation.auth.nameInvalidChars') }
    }

    return { isValid: true }
  }
}

/**
 * Task validation
 */
export const taskValidators = {
  title: (title: string): ValidationResult => {
    const { t } = i18nGlobal
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
    const { t } = i18nGlobal
    if (!description) return { isValid: true } // Description is optional

    if (description.length > 1000) {
      return { isValid: false, message: t('validation.task.descriptionMaxLength', { max: 1000 }) }
    }

    return { isValid: true }
  },

  priority: (priority: string): ValidationResult => {
    const { t } = i18nGlobal
    const validPriorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

    if (!priority) {
      return { isValid: false, message: t('validation.task.priorityRequired') }
    }

    if (!validPriorities.includes(priority)) {
      return { isValid: false, message: t('validation.task.priorityInvalid') }
    }

    return { isValid: true }
  },

  dueDate: (dueDate: string): ValidationResult => {
    const { t } = i18nGlobal
    if (!dueDate) return { isValid: true } // Due date is optional

    if (!isValidDateString(dueDate)) {
      return { isValid: false, message: t('validation.task.dueDateInvalid') }
    }

    return { isValid: true }
  },

  dueTime: (dueTime: string): ValidationResult => {
    const { t } = i18nGlobal
    if (!dueTime) return { isValid: true } // Due time is optional

    if (!isValidTimeString(dueTime)) {
      return { isValid: false, message: t('validation.task.dueTimeInvalid') }
    }

    return { isValid: true }
  }
}

/**
 * Form validation utilities
 */
export const validateForm = (
  data: Record<string, any>,
  validators: Record<string, (value: any) => ValidationResult>
): FormValidationResult => {
  const errors: Record<string, string> = {}
  
  for (const [field, validator] of Object.entries(validators)) {
    const result = validator(data[field])
    if (!result.isValid && result.message) {
      errors[field] = result.message
    }
  }
  
  return {
    isValid: Object.keys(errors).length === 0,
    errors
  }
}

/**
 * Login form validation
 */
export const validateLoginForm = (data: {
  username: string
  password: string
}): FormValidationResult => {
  const { t } = i18nGlobal
  return validateForm(data, {
    username: (value: string) => {
      if (!value?.trim()) {
        return { isValid: false, message: t('validation.auth.usernameRequired') }
      }
      return { isValid: true }
    },
    password: (value: string) => {
      if (!value) {
        return { isValid: false, message: t('validation.auth.passwordRequired') }
      }
      return { isValid: true }
    }
  })
}

/**
 * Registration form validation
 */
export const validateRegistrationForm = (data: {
  username: string
  email: string
  password: string
  confirmPassword: string
  firstName?: string
  lastName?: string
  // acceptTerms: boolean
}): FormValidationResult => {
  const errors: Record<string, string> = {}
  
  // Username validation
  const usernameResult = authValidators.username(data.username)
  if (!usernameResult.isValid) {
    errors.username = usernameResult.message!
  }
  
  // Email validation
  const emailResult = authValidators.email(data.email)
  if (!emailResult.isValid) {
    errors.email = emailResult.message!
  }
  
  // Password validation
  const passwordResult = authValidators.password(data.password)
  if (!passwordResult.isValid) {
    errors.password = passwordResult.message!
  }
  
  // Confirm password validation
  const confirmPasswordResult = authValidators.confirmPassword(data.password, data.confirmPassword)
  if (!confirmPasswordResult.isValid) {
    errors.confirmPassword = confirmPasswordResult.message!
  }
  
  // First name validation (optional)
  if (data.firstName) {
    const firstNameResult = authValidators.name(data.firstName)
    if (!firstNameResult.isValid) {
      errors.firstName = firstNameResult.message!
    }
  }
  
  // Last name validation (optional)
  if (data.lastName) {
    const lastNameResult = authValidators.name(data.lastName)
    if (!lastNameResult.isValid) {
      errors.lastName = lastNameResult.message!
    }
  }
  
  // Terms acceptance validation
  // if (!data.acceptTerms) {
  //   errors.acceptTerms = 'Devi accettare i termini e condizioni'
  // }
  
  return {
    isValid: Object.keys(errors).length === 0,
    errors
  }
}

/**
 * Task form validation
 */
export const validateTaskForm = (data: {
  title: string
  description?: string
  priority: string
  dueDate?: string
  dueTime?: string
}): FormValidationResult => {
  return validateForm(data, {
    title: taskValidators.title,
    description: taskValidators.description,
    priority: taskValidators.priority,
    dueDate: taskValidators.dueDate,
    dueTime: taskValidators.dueTime
  })
}

/**
 * Real-time validation utilities
 */
export const createValidator = (validators: Array<(value: any) => ValidationResult>) => {
  return (value: any): ValidationResult => {
    for (const validator of validators) {
      const result = validator(value)
      if (!result.isValid) {
        return result
      }
    }
    return { isValid: true }
  }
}

/**
 * Debounced validation for real-time feedback
 */
export const createDebouncedValidator = (
  validator: (value: any) => ValidationResult,
  delay = 300
) => {
  let timeout: number | undefined
  
  return (value: any, callback: (result: ValidationResult) => void) => {
    if (timeout) {
      clearTimeout(timeout)
    }
    
    timeout = setTimeout(() => {
      const result = validator(value)
      callback(result)
    }, delay) as unknown as number
  }
}

/**
 * Field validation state
 */
export interface FieldValidationState {
  isValid: boolean
  isDirty: boolean
  isTouched: boolean
  error?: string
}

export const createFieldValidationState = (): FieldValidationState => ({
  isValid: true,
  isDirty: false,
  isTouched: false
})

/**
 * Common validation patterns
 */
export const patterns = {
  email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  phone: /^[\+]?[1-9][\d]{0,15}$/,
  url: /^https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)$/,
  alphanumeric: /^[a-zA-Z0-9]+$/,
  numeric: /^\d+$/,
  decimal: /^\d+(\.\d+)?$/,
  time: /^([01]?[0-9]|2[0-3]):[0-5][0-9]$/,
  date: /^\d{4}-\d{2}-\d{2}$/,
  strongPassword: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/,
  username: /^[a-zA-Z0-9_.-]+$/,
  italianName: /^[a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñçčšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð\s'-]+$/,
  passwordReset: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).*$/
}

/**
 * Password Reset form validation
 */
export const validateForgotPasswordForm = (data: {
  email: string
}): FormValidationResult => {
  return validateForm(data, {
    email: authValidators.email
  })
}

/**
 * Reset Password form validation
 */
export const validateResetPasswordForm = (data: {
  newPassword: string
  confirmPassword: string
}): FormValidationResult => {
  const errors: Record<string, string> = {}

  // New password validation
  const passwordResult = authValidators.password(data.newPassword)
  if (!passwordResult.isValid) {
    errors.newPassword = passwordResult.message!
  }

  // Confirm password validation
  const confirmPasswordResult = authValidators.confirmPassword(data.newPassword, data.confirmPassword)
  if (!confirmPasswordResult.isValid) {
    errors.confirmPassword = confirmPasswordResult.message!
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors
  }
}

/**
 * Real-time password strength checker
 */
export const checkPasswordStrength = (password: string): {
  score: number
  feedback: string[]
  isStrong: boolean
} => {
  const { t } = i18nGlobal
  const feedback: string[] = []
  let score = 0

  if (!password) {
    return { score: 0, feedback: [t('password.strength.required')], isStrong: false }
  }

  // Length check
  if (password.length >= 8) {
    score += 1
  } else {
    feedback.push(t('password.strength.tooShort', { min: 8 }))
  }

  if (password.length >= 12) {
    score += 1
  }

  // Character variety checks
  if (/[a-z]/.test(password)) {
    score += 1
  } else {
    feedback.push(t('password.strength.missingLowercase'))
  }

  if (/[A-Z]/.test(password)) {
    score += 1
  } else {
    feedback.push(t('password.strength.missingUppercase'))
  }

  if (/\d/.test(password)) {
    score += 1
  } else {
    feedback.push(t('password.strength.missingNumber'))
  }

  if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
    score += 1
  } else {
    feedback.push(t('password.strength.missingSpecial'))
  }

  // Common patterns to avoid
  if (/(.)\1{2,}/.test(password)) {
    score -= 1
    feedback.push(t('password.strength.repeatedChars'))
  }

  if (/123|abc|qwe|asd/i.test(password)) {
    score -= 1
    feedback.push(t('password.strength.commonSequences'))
  }

  const isStrong = score >= 5 && password.length >= 8

  return {
    score: Math.max(0, Math.min(5, score)),
    feedback,
    isStrong
  }
}