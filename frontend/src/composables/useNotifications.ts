import { useToast } from 'vue-toastification'
import type { ToastOptions } from 'vue-toastification/dist/types/types'

export type NotificationType = 'success' | 'error' | 'warning' | 'info'

export interface NotificationOptions extends Partial<ToastOptions> {
  title?: string
  description?: string
  persistent?: boolean
}

export function useNotifications() {
  const toast = useToast()

  const showNotification = (
    message: string, 
    type: NotificationType = 'info', 
    options: NotificationOptions = {}
  ) => {
    const defaultOptions: ToastOptions = {
      timeout: type === 'error' || options.persistent ? 8000 : 5000,
      closeOnClick: true,
      pauseOnFocusLoss: true,
      pauseOnHover: true,
      draggable: true,
      draggablePercent: 0.6,
      showCloseButtonOnHover: false,
      hideProgressBar: false,
      closeButton: 'button',
      icon: true,
      rtl: false,
      position: 'top-right',
      ...options
    }

    // If persistent, set timeout to false
    if (options.persistent) {
      defaultOptions.timeout = false
    }

    switch (type) {
      case 'success':
        toast.success(message, defaultOptions)
        break
      case 'error':
        toast.error(message, defaultOptions)
        break
      case 'warning':
        toast.warning(message, defaultOptions)
        break
      case 'info':
      default:
        toast.info(message, defaultOptions)
        break
    }
  }

  const showSuccess = (message: string, options?: NotificationOptions) => {
    showNotification(message, 'success', options)
  }

  const showError = (message: string, options?: NotificationOptions) => {
    showNotification(message, 'error', options)
  }

  const showWarning = (message: string, options?: NotificationOptions) => {
    showNotification(message, 'warning', options)
  }

  const showInfo = (message: string, options?: NotificationOptions) => {
    showNotification(message, 'info', options)
  }

  const clearAll = () => {
    toast.clear()
  }

  // Utility functions for common notifications
  const showSaveSuccess = () => {
    showSuccess('Salvataggio completato con successo!')
  }

  const showDeleteSuccess = () => {
    showSuccess('Eliminazione completata con successo!')
  }

  const showUpdateSuccess = () => {
    showSuccess('Aggiornamento completato con successo!')
  }

  const showLoadingError = () => {
    showError('Errore durante il caricamento dei dati.')
  }

  const showNetworkError = () => {
    showError('Errore di connessione. Verifica la tua connessione internet.')
  }

  const showValidationError = (message = 'Verifica i dati inseriti e riprova.') => {
    showError(message)
  }

  const showAuthError = () => {
    showError('Sessione scaduta. Effettua nuovamente l\'accesso.')
  }

  const showTaskCompleted = (taskTitle?: string) => {
    const message = taskTitle ? `"${taskTitle}" completata!` : 'Attività completata!'
    showSuccess(message)
  }

  const showTaskCreated = (taskTitle?: string) => {
    const message = taskTitle ? `"${taskTitle}" creata!` : 'Attività creata con successo!'
    showSuccess(message)
  }

  const showReminderSet = (reminderTime?: string) => {
    const message = reminderTime 
      ? `Promemoria impostato per ${reminderTime}` 
      : 'Promemoria impostato con successo!'
    showSuccess(message)
  }

  const showOfflineNotification = () => {
    showWarning('Modalità offline attiva. Le modifiche verranno sincronizzate quando tornerai online.', {
      persistent: true
    })
  }

  const showOnlineNotification = () => {
    showSuccess('Connessione ripristinata. Sincronizzazione in corso...')
  }

  const showMaintenance = () => {
    showWarning('Il sistema è in manutenzione. Alcune funzionalità potrebbero non essere disponibili.', {
      persistent: true
    })
  }

  const showConfirmation = (
    message: string, 
    onConfirm: () => void, 
    onCancel?: () => void,
    options?: NotificationOptions
  ) => {
    // For confirmation dialogs, we'll use a custom toast with action buttons
    const confirmOptions: ToastOptions = {
      timeout: false,
      closeOnClick: false,
      ...options,
      content: {
        component: 'div',
        props: {
          innerHTML: `
            <div class="flex flex-col space-y-3">
              <p class="text-sm">${message}</p>
              <div class="flex space-x-2 justify-end">
                <button class="px-3 py-1 text-xs bg-gray-200 text-gray-800 rounded hover:bg-gray-300 transition-colors" onclick="this.closest('.Vue-Toastification__toast').querySelector('.Vue-Toastification__close-button').click(); ${onCancel ? 'onCancel()' : ''}">
                  Annulla
                </button>
                <button class="px-3 py-1 text-xs bg-red-600 text-white rounded hover:bg-red-700 transition-colors" onclick="this.closest('.Vue-Toastification__toast').querySelector('.Vue-Toastification__close-button').click(); onConfirm()">
                  Conferma
                </button>
              </div>
            </div>
          `
        }
      }
    }

    // Make functions available globally for the inline handlers
    ;(window as any).onConfirm = onConfirm
    ;(window as any).onCancel = onCancel

    toast.warning('', confirmOptions)
  }

  return {
    // Core functions
    showNotification,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    clearAll,

    // Utility functions
    showSaveSuccess,
    showDeleteSuccess,
    showUpdateSuccess,
    showLoadingError,
    showNetworkError,
    showValidationError,
    showAuthError,
    showTaskCompleted,
    showTaskCreated,
    showReminderSet,
    showOfflineNotification,
    showOnlineNotification,
    showMaintenance,
    showConfirmation
  }
}