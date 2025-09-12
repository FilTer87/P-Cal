import { ref, reactive } from 'vue'
import type { ToastType } from '../components/Common/Toast.vue'

interface Toast {
  id: string
  type: ToastType
  title?: string
  message: string
  autoDismiss?: boolean
  duration?: number
  actionText?: string
  actionCallback?: () => void
}

// Global toast state
const toasts = ref<Toast[]>([])
let toastIdCounter = 0

export function useCustomToast() {
  const addToast = (toast: Omit<Toast, 'id'>) => {
    const id = `toast-${++toastIdCounter}`
    const newToast: Toast = {
      id,
      autoDismiss: true,
      duration: 4000,
      ...toast
    }
    
    toasts.value.push(newToast)
    
    // Auto-remove toast if autoDismiss is enabled
    if (newToast.autoDismiss && newToast.duration) {
      setTimeout(() => {
        removeToast(id)
      }, newToast.duration)
    }
    
    return id
  }

  const removeToast = (id: string) => {
    const index = toasts.value.findIndex(toast => toast.id === id)
    if (index > -1) {
      toasts.value.splice(index, 1)
    }
  }

  const clearAllToasts = () => {
    toasts.value = []
  }

  // Convenience methods
  const showSuccess = (message: string, options?: Partial<Toast>) => {
    return addToast({ ...options, type: 'success', message })
  }

  const showError = (message: string, options?: Partial<Toast>) => {
    return addToast({ ...options, type: 'error', message, duration: 6000 })
  }

  const showWarning = (message: string, options?: Partial<Toast>) => {
    return addToast({ ...options, type: 'warning', message })
  }

  const showInfo = (message: string, options?: Partial<Toast>) => {
    return addToast({ ...options, type: 'info', message })
  }

  const showReminder = (message: string, options?: Partial<Toast>) => {
    return addToast({ ...options, type: 'reminder', message })
  }

  // Specific utility methods
  const showAuthError = () => {
    return showError('Sessione scaduta. Effettua nuovamente l\'accesso.')
  }

  return {
    // State
    toasts: toasts,
    
    // Methods
    addToast,
    removeToast,
    clearAllToasts,
    
    // Convenience methods
    showSuccess,
    showError,
    showWarning,
    showInfo,
    showReminder,
    showAuthError
  }
}