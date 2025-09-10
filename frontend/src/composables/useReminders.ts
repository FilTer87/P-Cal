import { ref, computed } from 'vue'
import { useNotifications } from './useNotifications'
import { reminderApi } from '../services/reminderApi'
import type { 
  Reminder, 
  CreateReminderRequest, 
  UpdateReminderRequest,
  ReminderFormData
} from '../types/task'
import { format, addMinutes, subMinutes, parseISO } from 'date-fns'

export function useReminders() {
  const { showSuccess, showError, showReminderSet } = useNotifications()

  // State
  const reminders = ref<Reminder[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Computed properties
  const upcomingReminders = computed(() => 
    (reminders.value || []).filter(reminder => 
      !reminder.sent && 
      new Date(reminder.reminderDateTime) > new Date()
    ).sort((a, b) => 
      new Date(a.reminderDateTime).getTime() - new Date(b.reminderDateTime).getTime()
    )
  )

  const overdueReminders = computed(() => 
    (reminders.value || []).filter(reminder => 
      !reminder.sent && 
      new Date(reminder.reminderDateTime) < new Date()
    )
  )

  const sentReminders = computed(() => 
    (reminders.value || []).filter(reminder => reminder.sent)
  )

  const pendingReminders = computed(() => 
    (reminders.value || []).filter(reminder => !reminder.sent)
  )

  const todayReminders = computed(() => {
    const today = format(new Date(), 'yyyy-MM-dd')
    return (reminders.value || []).filter(reminder => 
      format(new Date(reminder.reminderDateTime), 'yyyy-MM-dd') === today
    )
  })

  // Actions
  const fetchAllReminders = async () => {
    isLoading.value = true
    error.value = null

    try {
      reminders.value = await reminderApi.getAllReminders()
    } catch (err: any) {
      error.value = err.message || 'Errore nel caricamento dei promemoria'
      showError('Errore nel caricamento dei promemoria')
    } finally {
      isLoading.value = false
    }
  }

  const fetchTaskReminders = async (taskId: number) => {
    isLoading.value = true
    error.value = null

    try {
      const taskReminders = await reminderApi.getTaskReminders(taskId)
      // Update reminders array with task reminders
      reminders.value = [
        ...(reminders.value || []).filter(r => r.taskId !== taskId),
        ...taskReminders
      ]
      return taskReminders
    } catch (err: any) {
      error.value = err.message || 'Errore nel caricamento dei promemoria'
      showError('Errore nel caricamento dei promemoria per l\'attività')
      return []
    } finally {
      isLoading.value = false
    }
  }

  const fetchUpcomingReminders = async (hours = 24) => {
    isLoading.value = true
    error.value = null

    try {
      const upcoming = await reminderApi.getUpcomingReminders(hours)
      return upcoming
    } catch (err: any) {
      error.value = err.message || 'Errore nel caricamento dei promemoria imminenti'
      showError('Errore nel caricamento dei promemoria imminenti')
      return []
    } finally {
      isLoading.value = false
    }
  }

  const createReminder = async (taskId: number, reminderData: CreateReminderRequest): Promise<Reminder | null> => {
    isLoading.value = true
    error.value = null

    try {
      const reminder = await reminderApi.createReminder(taskId, reminderData)
      reminders.value.push(reminder)
      
      const reminderTime = format(new Date(reminder.reminderDateTime), 'dd/MM/yyyy HH:mm')
      showReminderSet(reminderTime)
      
      return reminder
    } catch (err: any) {
      error.value = err.message || 'Errore nella creazione del promemoria'
      showError('Errore nella creazione del promemoria')
      return null
    } finally {
      isLoading.value = false
    }
  }

  const updateReminder = async (reminderId: number, reminderData: UpdateReminderRequest): Promise<Reminder | null> => {
    isLoading.value = true
    error.value = null

    try {
      const updatedReminder = await reminderApi.updateReminder(reminderId, reminderData)
      
      const index = (reminders.value || []).findIndex(r => r.id === reminderId)
      if (index !== -1 && reminders.value) {
        reminders.value[index] = updatedReminder
      }
      
      showSuccess('Promemoria aggiornato con successo!')
      return updatedReminder
    } catch (err: any) {
      error.value = err.message || 'Errore nell\'aggiornamento del promemoria'
      showError('Errore nell\'aggiornamento del promemoria')
      return null
    } finally {
      isLoading.value = false
    }
  }

  const deleteReminder = async (reminderId: number): Promise<boolean> => {
    isLoading.value = true
    error.value = null

    try {
      await reminderApi.deleteReminder(reminderId)
      
      reminders.value = (reminders.value || []).filter(r => r.id !== reminderId)
      showSuccess('Promemoria eliminato con successo!')
      return true
    } catch (err: any) {
      error.value = err.message || 'Errore nell\'eliminazione del promemoria'
      showError('Errore nell\'eliminazione del promemoria')
      return false
    } finally {
      isLoading.value = false
    }
  }

  const markReminderSent = async (reminderId: number): Promise<boolean> => {
    try {
      const updatedReminder = await reminderApi.markReminderSent(reminderId)
      
      const index = (reminders.value || []).findIndex(r => r.id === reminderId)
      if (index !== -1 && reminders.value) {
        reminders.value[index] = updatedReminder
      }
      
      return true
    } catch (err: any) {
      error.value = err.message || 'Errore nel segnare il promemoria come inviato'
      return false
    }
  }

  const snoozeReminder = async (reminderId: number, minutes: number): Promise<boolean> => {
    try {
      const updatedReminder = await reminderApi.snoozeReminder(reminderId, minutes)
      
      const index = (reminders.value || []).findIndex(r => r.id === reminderId)
      if (index !== -1 && reminders.value) {
        reminders.value[index] = updatedReminder
      }
      
      showSuccess(`Promemoria posticipato di ${minutes} minuti`)
      return true
    } catch (err: any) {
      error.value = err.message || 'Errore nel posticipare il promemoria'
      showError('Errore nel posticipare il promemoria')
      return false
    }
  }

  // Bulk operations
  const createMultipleReminders = async (taskId: number, reminderDataArray: CreateReminderRequest[]): Promise<Reminder[]> => {
    isLoading.value = true
    error.value = null

    try {
      const newReminders = await reminderApi.bulkCreateReminders(taskId, reminderDataArray)
      reminders.value.push(...newReminders)
      
      showSuccess(`${newReminders.length} promemoria creati con successo!`)
      return newReminders
    } catch (err: any) {
      error.value = err.message || 'Errore nella creazione dei promemoria'
      showError('Errore nella creazione dei promemoria')
      return []
    } finally {
      isLoading.value = false
    }
  }

  const deleteMultipleReminders = async (reminderIds: number[]): Promise<boolean> => {
    isLoading.value = true

    try {
      await reminderApi.bulkDeleteReminders(reminderIds)
      
      reminders.value = (reminders.value || []).filter(r => !reminderIds.includes(r.id))
      showSuccess(`${reminderIds.length} promemoria eliminati con successo!`)
      return true
    } catch (err: any) {
      showError('Errore nell\'eliminazione dei promemoria')
      return false
    } finally {
      isLoading.value = false
    }
  }

  // Form helpers
  const createReminderFromForm = (formData: ReminderFormData): CreateReminderRequest => {
    const reminderDateTime = formData.reminderDateTime || `${formData.date}T${formData.time}:00`
    
    return {
      reminderDateTime
    }
  }

  const createFormFromReminder = (reminder: Reminder): ReminderFormData => {
    const reminderDate = new Date(reminder.reminderDateTime)
    
    return {
      id: reminder.id,
      date: format(reminderDate, 'yyyy-MM-dd'),
      time: format(reminderDate, 'HH:mm'),
      reminderDateTime: reminder.reminderDateTime
    }
  }

  // Reminder presets
  const getReminderPresets = () => [
    { id: '15min', name: '15 minuti prima', offsetMinutes: 15 },
    { id: '30min', name: '30 minuti prima', offsetMinutes: 30 },
    { id: '1hour', name: '1 ora prima', offsetMinutes: 60 },
    { id: '2hours', name: '2 ore prima', offsetMinutes: 120 },
    { id: '1day', name: '1 giorno prima', offsetMinutes: 1440 },
    { id: '1week', name: '1 settimana prima', offsetMinutes: 10080 }
  ]

  const createReminderFromPreset = (dueDate: string, presetId: string): CreateReminderRequest | null => {
    const preset = getReminderPresets().find(p => p.id === presetId)
    if (!preset) return null

    const dueDateObj = parseISO(dueDate)
    const reminderDateTime = subMinutes(dueDateObj, preset.offsetMinutes)

    return {
      reminderDateTime: reminderDateTime.toISOString()
    }
  }

  const addPresetReminders = (dueDate: string, presetIds: string[]): CreateReminderRequest[] => {
    return presetIds
      .map(presetId => createReminderFromPreset(dueDate, presetId))
      .filter(Boolean) as CreateReminderRequest[]
  }

  // Utilities
  const isReminderOverdue = (reminder: Reminder): boolean => {
    return !reminder.sent && new Date(reminder.reminderDateTime) < new Date()
  }

  const isReminderUpcoming = (reminder: Reminder, hours = 24): boolean => {
    if (reminder.sent) return false
    
    const reminderDate = new Date(reminder.reminderDateTime)
    const now = new Date()
    const diffHours = (reminderDate.getTime() - now.getTime()) / (1000 * 60 * 60)
    
    return diffHours > 0 && diffHours <= hours
  }

  const formatReminderTime = (reminder: Reminder): string => {
    return format(new Date(reminder.reminderDateTime), 'dd/MM/yyyy HH:mm')
  }

  const formatReminderTimeShort = (reminder: Reminder): string => {
    const reminderDate = new Date(reminder.reminderDateTime)
    const now = new Date()
    const today = format(now, 'yyyy-MM-dd')
    const reminderDay = format(reminderDate, 'yyyy-MM-dd')
    
    if (reminderDay === today) {
      return `Oggi alle ${format(reminderDate, 'HH:mm')}`
    }
    
    return format(reminderDate, 'dd/MM HH:mm')
  }

  const getTimeUntilReminder = (reminder: Reminder): string => {
    const reminderDate = new Date(reminder.reminderDateTime)
    const now = new Date()
    const diffMinutes = Math.floor((reminderDate.getTime() - now.getTime()) / (1000 * 60))
    
    if (diffMinutes < 0) {
      return 'Scaduto'
    } else if (diffMinutes < 60) {
      return `${diffMinutes} minuti`
    } else if (diffMinutes < 1440) {
      const hours = Math.floor(diffMinutes / 60)
      return `${hours} ${hours === 1 ? 'ora' : 'ore'}`
    } else {
      const days = Math.floor(diffMinutes / 1440)
      return `${days} ${days === 1 ? 'giorno' : 'giorni'}`
    }
  }

  const getRemindersByTask = (taskId: number): Reminder[] => {
    return (reminders.value || []).filter(reminder => reminder.taskId === taskId)
  }

  const hasUpcomingReminders = (taskId: number): boolean => {
    return getRemindersByTask(taskId).some(reminder => 
      !reminder.sent && new Date(reminder.reminderDateTime) > new Date()
    )
  }

  const getNextReminder = (taskId: number): Reminder | null => {
    const taskReminders = getRemindersByTask(taskId)
      .filter(reminder => !reminder.sent && new Date(reminder.reminderDateTime) > new Date())
      .sort((a, b) => new Date(a.reminderDateTime).getTime() - new Date(b.reminderDateTime).getTime())
    
    return taskReminders[0] || null
  }

  // Statistics
  const getReminderStats = () => ({
    total: (reminders.value || []).length,
    pending: pendingReminders.value.length,
    sent: sentReminders.value.length,
    overdue: overdueReminders.value.length,
    upcoming: upcomingReminders.value.length,
    today: todayReminders.value.length
  })

  return {
    // State
    reminders,
    isLoading,
    error,

    // Computed
    upcomingReminders,
    overdueReminders,
    sentReminders,
    pendingReminders,
    todayReminders,

    // Actions
    fetchAllReminders,
    fetchTaskReminders,
    fetchUpcomingReminders,
    createReminder,
    updateReminder,
    deleteReminder,
    markReminderSent,
    snoozeReminder,

    // Bulk operations
    createMultipleReminders,
    deleteMultipleReminders,

    // Form helpers
    createReminderFromForm,
    createFormFromReminder,

    // Presets
    getReminderPresets,
    createReminderFromPreset,
    addPresetReminders,

    // Utilities
    isReminderOverdue,
    isReminderUpcoming,
    formatReminderTime,
    formatReminderTimeShort,
    getTimeUntilReminder,
    getRemindersByTask,
    hasUpcomingReminders,
    getNextReminder,
    getReminderStats
  }
}