import { describe, it, expect, beforeEach } from 'vitest'
import { createI18n } from 'vue-i18n'
import itIT from '../i18n/locales/it-IT.json'
import enUS from '../i18n/locales/en-US.json'

describe('i18n Pluralization', () => {
  describe('Italian (it-IT)', () => {
    let i18n: ReturnType<typeof createI18n>

    beforeEach(() => {
      i18n = createI18n({
        legacy: false,
        locale: 'it-IT',
        fallbackLocale: 'en-US',
        messages: {
          'it-IT': itIT,
          'en-US': enUS
        }
      })
    })

    it('should pluralize remindersCount correctly', () => {
      const { t } = i18n.global

      expect(t('tasks.remindersCount', 0)).toBe('0 Promemoria')
      expect(t('tasks.remindersCount', 1)).toBe('1 Promemoria')
      expect(t('tasks.remindersCount', 2)).toBe('2 Promemoria')
      expect(t('tasks.remindersCount', 10)).toBe('10 Promemoria')
    })

    it('should pluralize completedActivities correctly', () => {
      const { t } = i18n.global

      expect(t('calendar.completedActivities', 0)).toBe('0 Attività completate')
      expect(t('calendar.completedActivities', 1)).toBe('1 Attività completata')
      expect(t('calendar.completedActivities', 2)).toBe('2 Attività completate')
      expect(t('calendar.completedActivities', 5)).toBe('5 Attività completate')
    })

    it('should pluralize moreActivities correctly', () => {
      const { t } = i18n.global

      expect(t('calendar.moreActivities', 1)).toBe('+1 altra')
      expect(t('calendar.moreActivities', 2)).toBe('+2 altre')
      expect(t('calendar.moreActivities', 10)).toBe('+10 altre')
    })

    it('should pluralize showMoreActivities correctly', () => {
      const { t } = i18n.global

      expect(t('calendar.sidebar.showMoreActivities', 1)).toBe('Mostra 1 altra attività')
      expect(t('calendar.sidebar.showMoreActivities', 2)).toBe('Mostra 2 altre attività')
      expect(t('calendar.sidebar.showMoreActivities', 5)).toBe('Mostra 5 altre attività')
    })

    it('should pluralize remindersCreated correctly', () => {
      const { t } = i18n.global

      expect(t('composables.useReminders.remindersCreated', 1)).toBe('1 promemoria creato con successo!')
      expect(t('composables.useReminders.remindersCreated', 2)).toBe('2 promemoria creati con successo!')
      expect(t('composables.useReminders.remindersCreated', 5)).toBe('5 promemoria creati con successo!')
    })

    it('should pluralize remindersDeleted correctly', () => {
      const { t } = i18n.global

      expect(t('composables.useReminders.remindersDeleted', 1)).toBe('1 promemoria eliminato con successo!')
      expect(t('composables.useReminders.remindersDeleted', 2)).toBe('2 promemoria eliminati con successo!')
      expect(t('composables.useReminders.remindersDeleted', 3)).toBe('3 promemoria eliminati con successo!')
    })
  })

  describe('English (en-US)', () => {
    let i18n: ReturnType<typeof createI18n>

    beforeEach(() => {
      i18n = createI18n({
        legacy: false,
        locale: 'en-US',
        fallbackLocale: 'en-US',
        messages: {
          'it-IT': itIT,
          'en-US': enUS
        }
      })
    })

    it('should pluralize remindersCount correctly', () => {
      const { t } = i18n.global

      expect(t('tasks.remindersCount', 0)).toBe('0 Reminders')
      expect(t('tasks.remindersCount', 1)).toBe('1 Reminder')
      expect(t('tasks.remindersCount', 2)).toBe('2 Reminders')
      expect(t('tasks.remindersCount', 10)).toBe('10 Reminders')
    })

    it('should pluralize completedActivities correctly', () => {
      const { t } = i18n.global

      expect(t('calendar.completedActivities', 0)).toBe('0 Completed activities')
      expect(t('calendar.completedActivities', 1)).toBe('1 Completed activity')
      expect(t('calendar.completedActivities', 2)).toBe('2 Completed activities')
      expect(t('calendar.completedActivities', 5)).toBe('5 Completed activities')
    })

    it('should pluralize moreActivities correctly', () => {
      const { t } = i18n.global

      // "more" is invariant in English
      expect(t('calendar.moreActivities', 1)).toBe('+1 more')
      expect(t('calendar.moreActivities', 2)).toBe('+2 more')
      expect(t('calendar.moreActivities', 10)).toBe('+10 more')
    })

    it('should pluralize showMoreActivities correctly', () => {
      const { t } = i18n.global

      expect(t('calendar.sidebar.showMoreActivities', 1)).toBe('Show 1 more activity')
      expect(t('calendar.sidebar.showMoreActivities', 2)).toBe('Show 2 more activities')
      expect(t('calendar.sidebar.showMoreActivities', 5)).toBe('Show 5 more activities')
    })

    it('should pluralize remindersCreated correctly', () => {
      const { t } = i18n.global

      expect(t('composables.useReminders.remindersCreated', 1)).toBe('1 reminder created successfully!')
      expect(t('composables.useReminders.remindersCreated', 2)).toBe('2 reminders created successfully!')
      expect(t('composables.useReminders.remindersCreated', 5)).toBe('5 reminders created successfully!')
    })

    it('should pluralize remindersDeleted correctly', () => {
      const { t } = i18n.global

      expect(t('composables.useReminders.remindersDeleted', 1)).toBe('1 reminder deleted successfully!')
      expect(t('composables.useReminders.remindersDeleted', 2)).toBe('2 reminders deleted successfully!')
      expect(t('composables.useReminders.remindersDeleted', 3)).toBe('3 reminders deleted successfully!')
    })
  })

  describe('Edge Cases', () => {
    let i18n: ReturnType<typeof createI18n>

    beforeEach(() => {
      i18n = createI18n({
        legacy: false,
        locale: 'it-IT',
        fallbackLocale: 'en-US',
        messages: {
          'it-IT': itIT,
          'en-US': enUS
        }
      })
    })

    it('should handle zero count correctly', () => {
      const { t } = i18n.global

      // Zero uses plural form in Italian (and most languages)
      expect(t('tasks.remindersCount', 0)).toBe('0 Promemoria')
      expect(t('calendar.completedActivities', 0)).toBe('0 Attività completate')
    })

    it('should handle large numbers correctly', () => {
      const { t } = i18n.global

      expect(t('tasks.remindersCount', 100)).toBe('100 Promemoria')
      expect(t('calendar.completedActivities', 1000)).toBe('1000 Attività completate')
    })

    it('should handle negative numbers gracefully', () => {
      const { t } = i18n.global

      // Negative numbers should still work (even if not semantically correct)
      expect(t('tasks.remindersCount', -1)).toBe('-1 Promemoria')
    })
  })

  describe('Locale Switching', () => {
    let i18n: ReturnType<typeof createI18n>

    beforeEach(() => {
      i18n = createI18n({
        legacy: false,
        locale: 'it-IT',
        fallbackLocale: 'en-US',
        messages: {
          'it-IT': itIT,
          'en-US': enUS
        }
      })
    })

    it('should use correct pluralization rules when switching locales', () => {
      const { t } = i18n.global

      // Italian
      expect(t('calendar.completedActivities', 1)).toBe('1 Attività completata')
      expect(t('calendar.completedActivities', 2)).toBe('2 Attività completate')

      // Switch to English
      i18n.global.locale.value = 'en-US'

      expect(t('calendar.completedActivities', 1)).toBe('1 Completed activity')
      expect(t('calendar.completedActivities', 2)).toBe('2 Completed activities')
    })
  })
})
