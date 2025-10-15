import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useCustomToast } from './useCustomToast'
import { reminderApi } from '../services/reminderApi'
import type {
  Reminder,
  CreateReminderRequest,
  UpdateReminderRequest,
  ReminderFormData
} from '../types/task'
import { format } from 'date-fns'
import { i18nGlobal } from '../i18n'

export function useReminders() {
  const { showSuccess, showError } = useCustomToast()

  const showReminderSet = (reminderTime: string) => {
    showSuccess(i18nGlobal.t('composables.useReminders.reminderSet', { time: reminderTime }))
  }

  // State
  const reminders = ref<Reminder[]>([])

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

  // Actions
  const fetchAllReminders = async () => {
    try {
      reminders.value = await reminderApi.getAllReminders()
    } catch (err: any) {
      showError(i18nGlobal.t('composables.useReminders.errors.loadReminders'))
    }
  }

  const createReminder = async (taskId: number, reminderData: CreateReminderRequest): Promise<Reminder | null> => {
    try {
      const reminder = await reminderApi.createReminder(taskId, reminderData)
      reminders.value.push(reminder)

      const reminderTime = format(new Date(reminder.reminderTime), 'dd/MM/yyyy HH:mm')
      showReminderSet(reminderTime)

      return reminder
    } catch (err: any) {
      showError(i18nGlobal.t('composables.useReminders.errors.createReminder'))
      return null
    }
  }

  const updateReminder = async (reminderId: number, reminderData: UpdateReminderRequest): Promise<Reminder | null> => {
    try {
      const updatedReminder = await reminderApi.updateReminder(reminderId, reminderData)

      const index = (reminders.value || []).findIndex(r => r.id === reminderId)
      if (index !== -1 && reminders.value) {
        reminders.value[index] = updatedReminder
      }

      showSuccess(i18nGlobal.t('composables.useReminders.reminderUpdated'))
      return updatedReminder
    } catch (err: any) {
      showError(i18nGlobal.t('composables.useReminders.errors.updateReminder'))
      return null
    }
  }

  // Utilities

  const formatReminderTimeShort = (reminder: Reminder): string => {
    const reminderDate = new Date(reminder.reminderTime)
    const now = new Date()
    const today = format(now, 'yyyy-MM-dd')
    const reminderDay = format(reminderDate, 'yyyy-MM-dd')

    if (reminderDay === today) {
      const time = format(reminderDate, 'HH:mm')
      return i18nGlobal.t('composables.useReminders.timeFormat.today', { time })
    }

    return format(reminderDate, 'dd/MM HH:mm')
  }

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
    // Computed
    upcomingReminders,

    // Actions
    fetchAllReminders,
    createReminder,
    updateReminder,

    // Utilities
    formatReminderTimeShort
  }
}