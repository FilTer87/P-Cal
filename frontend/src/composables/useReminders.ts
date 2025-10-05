import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useCustomToast } from './useCustomToast'
import { reminderApi } from '../services/reminderApi'
import type {
  Reminder,
  CreateReminderRequest,
  UpdateReminderRequest,
  ReminderFormData
} from '../types/task'
import { format, addMinutes, subMinutes, parseISO } from 'date-fns'
import { i18n } from '../i18n'

export function useReminders() {
  const { showSuccess, showError } = useCustomToast()

  const showReminderSet = (reminderTime: string) => {
    showSuccess(i18n.global.t('composables.useReminders.reminderSet', { time: reminderTime }))
  }

  // State
  const reminders = ref<Reminder[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Computed properties
  const upcomingReminders = computed(() => {
    const now = new Date()
    const upcoming = (reminders.value || []).filter(reminder => {
      if (reminder.isSent || reminder.sent) return false
      const reminderTime = new Date(reminder.reminderTime)
      const isUpcoming = reminderTime > now
      console.log(`📅 Reminder ${reminder.id}: ${reminder.reminderTime} -> Local: ${reminderTime.toLocaleString()}, isUpcoming: ${isUpcoming}`)
      return isUpcoming
    }).sort((a, b) => 
      new Date(a.reminderTime).getTime() - new Date(b.reminderTime).getTime()
    )
    console.log(`📅 Total upcoming reminders: ${upcoming.length}`)
    return upcoming
  })

  const overdueReminders = computed(() => 
    (reminders.value || []).filter(reminder => 
      !(reminder.isSent || reminder.sent) && 
      new Date(reminder.reminderTime) < new Date()
    )
  )

  const sentReminders = computed(() => 
    (reminders.value || []).filter(reminder => reminder.isSent || reminder.sent)
  )

  const pendingReminders = computed(() => 
    (reminders.value || []).filter(reminder => !(reminder.isSent || reminder.sent))
  )

  const todayReminders = computed(() => {
    const today = format(new Date(), 'yyyy-MM-dd')
    return (reminders.value || []).filter(reminder => 
      format(new Date(reminder.reminderTime), 'yyyy-MM-dd') === today
    )
  })

  // Actions
  const fetchAllReminders = async () => {
    isLoading.value = true
    error.value = null

    try {
      reminders.value = await reminderApi.getAllReminders()
    } catch (err: any) {
      error.value = err.message || i18n.global.t('composables.useReminders.errors.loadReminders')
      showError(i18n.global.t('composables.useReminders.errors.loadReminders'))
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
      error.value = err.message || i18n.global.t('composables.useReminders.errors.loadTaskReminders')
      showError(i18n.global.t('composables.useReminders.errors.loadTaskReminders'))
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
      error.value = err.message || i18n.global.t('composables.useReminders.errors.loadUpcomingReminders')
      showError(i18n.global.t('composables.useReminders.errors.loadUpcomingReminders'))
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
      error.value = err.message || i18n.global.t('composables.useReminders.errors.createReminder')
      showError(i18n.global.t('composables.useReminders.errors.createReminder'))
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

      showSuccess(i18n.global.t('composables.useReminders.reminderUpdated'))
      return updatedReminder
    } catch (err: any) {
      error.value = err.message || i18n.global.t('composables.useReminders.errors.updateReminder')
      showError(i18n.global.t('composables.useReminders.errors.updateReminder'))
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
      showSuccess(i18n.global.t('composables.useReminders.reminderDeleted'))
      return true
    } catch (err: any) {
      error.value = err.message || i18n.global.t('composables.useReminders.errors.deleteReminder')
      showError(i18n.global.t('composables.useReminders.errors.deleteReminder'))
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
      error.value = err.message || i18n.global.t('composables.useReminders.errors.markReminderSent')
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

      showSuccess(i18n.global.t('composables.useReminders.reminderSnoozed', { minutes }))
      return true
    } catch (err: any) {
      error.value = err.message || i18n.global.t('composables.useReminders.errors.snoozeReminder')
      showError(i18n.global.t('composables.useReminders.errors.snoozeReminder'))
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

      showSuccess(i18n.global.t('composables.useReminders.remindersCreated', { count: newReminders.length }))
      return newReminders
    } catch (err: any) {
      error.value = err.message || i18n.global.t('composables.useReminders.errors.createMultiple')
      showError(i18n.global.t('composables.useReminders.errors.createMultiple'))
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
      showSuccess(i18n.global.t('composables.useReminders.remindersDeleted', { count: reminderIds.length }))
      return true
    } catch (err: any) {
      showError(i18n.global.t('composables.useReminders.errors.deleteMultiple'))
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
  const getReminderPresets = () => {
    const t = i18n.global.t
    return [
      { id: '15min', name: t('composables.useReminders.presets.15min'), offsetMinutes: 15 },
      { id: '30min', name: t('composables.useReminders.presets.30min'), offsetMinutes: 30 },
      { id: '1hour', name: t('composables.useReminders.presets.1hour'), offsetMinutes: 60 },
      { id: '2hours', name: t('composables.useReminders.presets.2hours'), offsetMinutes: 120 },
      { id: '1day', name: t('composables.useReminders.presets.1day'), offsetMinutes: 1440 },
      { id: '1week', name: t('composables.useReminders.presets.1week'), offsetMinutes: 10080 }
    ]
  }

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
      const time = format(reminderDate, 'HH:mm')
      return i18n.global.t('composables.useReminders.timeFormat.today', { time })
    }

    return format(reminderDate, 'dd/MM HH:mm')
  }

  const getTimeUntilReminder = (reminder: Reminder): string => {
    const reminderDate = new Date(reminder.reminderDateTime)
    const now = new Date()
    const diffMinutes = Math.floor((reminderDate.getTime() - now.getTime()) / (1000 * 60))
    const t = i18n.global.t

    if (diffMinutes < 0) {
      return t('composables.useReminders.timeFormat.overdue')
    } else if (diffMinutes < 60) {
      return t('composables.useReminders.timeFormat.minutes', { count: diffMinutes })
    } else if (diffMinutes < 1440) {
      const hours = Math.floor(diffMinutes / 60)
      return `${hours} ${hours === 1 ? t('composables.useReminders.timeFormat.hour') : t('composables.useReminders.timeFormat.hours')}`
    } else {
      const days = Math.floor(diffMinutes / 1440)
      return `${days} ${days === 1 ? t('composables.useReminders.timeFormat.day') : t('composables.useReminders.timeFormat.days')}`
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

  // Handle reminder notification events
  const handleReminderNotified = (event: CustomEvent) => {
    const { reminderId } = event.detail
    const reminderIndex = (reminders.value || []).findIndex(r => r.id === reminderId)
    
    if (reminderIndex !== -1 && reminders.value) {
      // Mark reminder as sent in local state
      reminders.value[reminderIndex] = {
        ...reminders.value[reminderIndex],
        isSent: true,
        sent: true
      }
      console.log(`📅 Updated local state for reminder ${reminderId} - marked as sent`)
    }
  }

  // Set up event listeners
  onMounted(() => {
    window.addEventListener('reminder-notified', handleReminderNotified as EventListener)
  })

  onUnmounted(() => {
    window.removeEventListener('reminder-notified', handleReminderNotified as EventListener)
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