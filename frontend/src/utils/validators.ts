import { isValidDate, isValidDateString, isValidTimeString } from './dateHelpers'

export interface ValidationResult {
  isValid: boolean
  message?: string
}

export interface FormValidationResult {
  isValid: boolean
  errors: Record<string, string>
}

/**
 * Basic validation rules
 */
export const rules = {
  required: (value: any): ValidationResult => ({
    isValid: value !== null && value !== undefined && value !== '',
    message: 'Questo campo è obbligatorio'
  }),

  email: (value: string): ValidationResult => {
    if (!value) return { isValid: true }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return {
      isValid: emailRegex.test(value),
      message: 'Inserisci un indirizzo email valido'
    }
  },

  minLength: (length: number) => (value: string): ValidationResult => ({
    isValid: !value || value.length >= length,
    message: `Deve contenere almeno ${length} caratteri`
  }),

  maxLength: (length: number) => (value: string): ValidationResult => ({
    isValid: !value || value.length <= length,
    message: `Non può superare i ${length} caratteri`
  }),

  pattern: (regex: RegExp, message: string) => (value: string): ValidationResult => ({
    isValid: !value || regex.test(value),
    message
  }),

  numeric: (value: string): ValidationResult => ({
    isValid: !value || /^\d+$/.test(value),
    message: 'Deve contenere solo numeri'
  }),

  alphanumeric: (value: string): ValidationResult => ({
    isValid: !value || /^[a-zA-Z0-9]+$/.test(value),
    message: 'Deve contenere solo lettere e numeri'
  }),

  minValue: (min: number) => (value: number): ValidationResult => ({
    isValid: value == null || value >= min,
    message: `Il valore deve essere almeno ${min}`
  }),

  maxValue: (max: number) => (value: number): ValidationResult => ({
    isValid: value == null || value <= max,
    message: `Il valore non può superare ${max}`
  }),

  date: (value: string): ValidationResult => ({
    isValid: !value || isValidDateString(value),
    message: 'Inserisci una data valida'
  }),

  time: (value: string): ValidationResult => ({
    isValid: !value || isValidTimeString(value),
    message: 'Inserisci un orario valido (HH:MM)'
  }),

  url: (value: string): ValidationResult => {
    if (!value) return { isValid: true }
    try {
      new URL(value)
      return { isValid: true }
    } catch {
      return {
        isValid: false,
        message: 'Inserisci un URL valido'
      }
    }
  },

  phone: (value: string): ValidationResult => {
    if (!value) return { isValid: true }
    const phoneRegex = /^[\+]?[1-9][\d]{5,15}$/
    return {
      isValid: phoneRegex.test(value.replace(/\s/g, '')),
      message: 'Inserisci un numero di telefono valido'
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
    const trimmed = username?.trim()
    
    if (!trimmed) {
      return { isValid: false, message: 'Nome utente è obbligatorio' }
    }
    
    if (trimmed.length < 3) {
      return { isValid: false, message: 'Nome utente deve avere almeno 3 caratteri' }
    }
    
    if (trimmed.length > 50) {
      return { isValid: false, message: 'Nome utente non può superare i 50 caratteri' }
    }
    
    if (!/^[a-zA-Z0-9_.-]+$/.test(trimmed)) {
      return { isValid: false, message: 'Nome utente può contenere solo lettere, numeri, underscore, punti e trattini' }
    }
    
    return { isValid: true }
  },

  password: (password: string): ValidationResult => {
    if (!password) {
      return { isValid: false, message: 'Password è obbligatoria' }
    }

    if (password.length < 8) {
      return { isValid: false, message: 'Password deve avere almeno 8 caratteri' }
    }

    if (password.length > 128) {
      return { isValid: false, message: 'Password non può superare i 128 caratteri' }
    }

    if (!/(?=.*[a-z])/.test(password)) {
      return { isValid: false, message: 'Password deve contenere almeno una lettera minuscola' }
    }

    if (!/(?=.*[A-Z])/.test(password)) {
      return { isValid: false, message: 'Password deve contenere almeno una lettera maiuscola' }
    }

    if (!/(?=.*\d)/.test(password)) {
      return { isValid: false, message: 'Password deve contenere almeno un numero' }
    }

    return { isValid: true }
  },

  confirmPassword: (password: string, confirmPassword: string): ValidationResult => {
    if (!confirmPassword) {
      return { isValid: false, message: 'Conferma password è obbligatoria' }
    }
    
    if (password !== confirmPassword) {
      return { isValid: false, message: 'Le password non corrispondono' }
    }
    
    return { isValid: true }
  },

  email: (email: string): ValidationResult => {
    const trimmed = email?.trim()
    
    if (!trimmed) {
      return { isValid: false, message: 'Email è obbligatoria' }
    }
    
    if (trimmed.length > 255) {
      return { isValid: false, message: 'Email non può superare i 255 caratteri' }
    }
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(trimmed)) {
      return { isValid: false, message: 'Inserisci un indirizzo email valido' }
    }
    
    return { isValid: true }
  },

  name: (name: string): ValidationResult => {
    if (!name) return { isValid: true } // Name is optional
    
    const trimmed = name.trim()
    
    if (trimmed.length > 100) {
      return { isValid: false, message: 'Nome non può superare i 100 caratteri' }
    }
    
    if (!/^[a-zA-ZàáâäãåąčćęèéêëėįìíîïłńòóôöõøùúûüųūÿýżźñçčšžÀÁÂÄÃÅĄĆČĖĘÈÉÊËÌÍÎÏĮŁŃÒÓÔÖÕØÙÚÛÜŲŪŸÝŻŹÑßÇŒÆČŠŽ∂ð\s'-]+$/.test(trimmed)) {
      return { isValid: false, message: 'Nome può contenere solo lettere, spazi, apostrofi e trattini' }
    }
    
    return { isValid: true }
  }
}

/**
 * Task validation
 */
export const taskValidators = {
  title: (title: string): ValidationResult => {
    const trimmed = title?.trim()
    
    if (!trimmed) {
      return { isValid: false, message: 'Titolo è obbligatorio' }
    }
    
    if (trimmed.length > 255) {
      return { isValid: false, message: 'Titolo non può superare i 255 caratteri' }
    }
    
    return { isValid: true }
  },

  description: (description: string): ValidationResult => {
    if (!description) return { isValid: true } // Description is optional
    
    if (description.length > 1000) {
      return { isValid: false, message: 'Descrizione non può superare i 1000 caratteri' }
    }
    
    return { isValid: true }
  },

  priority: (priority: string): ValidationResult => {
    const validPriorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
    
    if (!priority) {
      return { isValid: false, message: 'Priorità è obbligatoria' }
    }
    
    if (!validPriorities.includes(priority)) {
      return { isValid: false, message: 'Priorità non valida' }
    }
    
    return { isValid: true }
  },

  dueDate: (dueDate: string): ValidationResult => {
    if (!dueDate) return { isValid: true } // Due date is optional
    
    if (!isValidDateString(dueDate)) {
      return { isValid: false, message: 'Data di scadenza non valida' }
    }
    
    return { isValid: true }
  },

  dueTime: (dueTime: string): ValidationResult => {
    if (!dueTime) return { isValid: true } // Due time is optional
    
    if (!isValidTimeString(dueTime)) {
      return { isValid: false, message: 'Orario non valido (formato HH:MM)' }
    }
    
    return { isValid: true }
  },

  reminderDateTime: (reminderDateTime: string): ValidationResult => {
    if (!reminderDateTime) {
      return { isValid: false, message: 'Data e ora del promemoria sono obbligatorie' }
    }
    
    if (!isValidDate(reminderDateTime)) {
      return { isValid: false, message: 'Data e ora del promemoria non valide' }
    }
    
    const reminderDate = new Date(reminderDateTime)
    const now = new Date()
    
    if (reminderDate <= now) {
      return { isValid: false, message: 'Il promemoria deve essere impostato per il futuro' }
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
  return validateForm(data, {
    username: (value: string) => {
      if (!value?.trim()) {
        return { isValid: false, message: 'Nome utente è obbligatorio' }
      }
      return { isValid: true }
    },
    password: (value: string) => {
      if (!value) {
        return { isValid: false, message: 'Password è obbligatoria' }
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
 * Reminder form validation
 */
export const validateReminderForm = (data: {
  date: string
  time: string
}): FormValidationResult => {
  const errors: Record<string, string> = {}
  
  if (!data.date) {
    errors.date = 'Data del promemoria è obbligatoria'
  } else if (!isValidDateString(data.date)) {
    errors.date = 'Data del promemoria non valida'
  }
  
  if (!data.time) {
    errors.time = 'Orario del promemoria è obbligatorio'
  } else if (!isValidTimeString(data.time)) {
    errors.time = 'Orario del promemoria non valido'
  }
  
  // Check if reminder is in the future
  if (data.date && data.time && isValidDateString(data.date) && isValidTimeString(data.time)) {
    const reminderDateTime = new Date(`${data.date}T${data.time}:00`)
    const now = new Date()
    
    if (reminderDateTime <= now) {
      errors.time = 'Il promemoria deve essere impostato per il futuro'
    }
  }
  
  return {
    isValid: Object.keys(errors).length === 0,
    errors
  }
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
  const feedback: string[] = []
  let score = 0

  if (!password) {
    return { score: 0, feedback: ['Password è obbligatoria'], isStrong: false }
  }

  // Length check
  if (password.length >= 8) {
    score += 1
  } else {
    feedback.push('Almeno 8 caratteri')
  }

  if (password.length >= 12) {
    score += 1
  }

  // Character variety checks
  if (/[a-z]/.test(password)) {
    score += 1
  } else {
    feedback.push('Almeno una lettera minuscola')
  }

  if (/[A-Z]/.test(password)) {
    score += 1
  } else {
    feedback.push('Almeno una lettera maiuscola')
  }

  if (/\d/.test(password)) {
    score += 1
  } else {
    feedback.push('Almeno un numero')
  }

  if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
    score += 1
  } else {
    feedback.push('Caratteri speciali per maggiore sicurezza')
  }

  // Common patterns to avoid
  if (/(.)\1{2,}/.test(password)) {
    score -= 1
    feedback.push('Evita sequenze di caratteri ripetuti')
  }

  if (/123|abc|qwe|asd/i.test(password)) {
    score -= 1
    feedback.push('Evita sequenze comuni di caratteri')
  }

  const isStrong = score >= 5 && password.length >= 8

  return {
    score: Math.max(0, Math.min(5, score)),
    feedback,
    isStrong
  }
}