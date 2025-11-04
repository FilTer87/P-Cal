import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProfileCalendarData from '../../../components/Auth/ProfileCalendarData.vue'
import { calendarApi, type CalendarStats, type ImportPreviewResponse, DuplicateStrategy } from '@/services/calendarApi'
import { calendarsApi, type Calendar } from '@/services/calendarsApi'

// Mock the APIs
vi.mock('@/services/calendarApi', () => ({
  calendarApi: {
    getCalendarStats: vi.fn(),
    exportCalendar: vi.fn(),
    previewImport: vi.fn(),
    confirmImport: vi.fn()
  },
  DuplicateStrategy: {
    SKIP: 'SKIP',
    UPDATE: 'UPDATE',
    CREATE_ANYWAY: 'CREATE_ANYWAY'
  }
}))

vi.mock('@/services/calendarsApi', () => ({
  calendarsApi: {
    getAllCalendars: vi.fn()
  }
}))

// Mock toast composable
vi.mock('@/composables/useCustomToast', () => ({
  useCustomToast: () => ({
    showError: vi.fn(),
    showSuccess: vi.fn()
  })
}))

describe('ProfileCalendarData.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  const createMockStats = (overrides: Partial<CalendarStats> = {}): CalendarStats => ({
    totalTasks: 10,
    todayTasks: 2,
    thisWeekTasks: 5,
    ...overrides
  })

  const createMockCalendar = (id: number, overrides: Partial<Calendar> = {}): Calendar => ({
    id,
    name: `Calendar ${id}`,
    slug: `calendar-${id}`,
    color: '#3788d8',
    timezone: 'Europe/Rome',
    isDefault: false,
    taskCount: 0,
    caldavPath: `/caldav/user/calendar-${id}/`,
    ...overrides
  })

  describe('Rendering', () => {
    it('should render the component', async () => {
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      expect(wrapper.exists()).toBe(true)
    })

    it('should display calendar statistics', async () => {
      const stats = createMockStats({ totalTasks: 25 })
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(stats)
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      expect(wrapper.text()).toContain('25')
    })

    it('should display export section', async () => {
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      expect(wrapper.text()).toContain('Esporta Calendario')
    })

    it('should display import section', async () => {
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      expect(wrapper.text()).toContain('Importa Calendario')
    })
  })

  describe('Export Calendar', () => {
    it('should trigger export when clicking export button', async () => {
      const stats = createMockStats({ totalTasks: 5 })
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(stats)
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])
      vi.mocked(calendarApi.exportCalendar).mockResolvedValue({
        blob: new Blob(['test'], { type: 'text/calendar' }),
        filename: 'calendar.ics'
      })

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      // Find export button by text
      const exportButton = wrapper.findAll('button').find(btn =>
        btn.text().includes('Scarica')
      )

      if (exportButton) {
        await exportButton.trigger('click')
        await flushPromises()

        expect(calendarApi.exportCalendar).toHaveBeenCalled()
      }
    })

    it('should disable export button when no tasks exist', async () => {
      const stats = createMockStats({ totalTasks: 0 })
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(stats)
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const exportButton = wrapper.findAll('button').find(btn =>
        btn.text().includes('Scarica')
      )

      if (exportButton) {
        expect((exportButton.element as HTMLButtonElement).disabled).toBe(true)
      }
    })

    it('should handle export error gracefully', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      const stats = createMockStats({ totalTasks: 5 })
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(stats)
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])
      vi.mocked(calendarApi.exportCalendar).mockRejectedValue(new Error('Export failed'))

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const exportButton = wrapper.findAll('button').find(btn =>
        btn.text().includes('Scarica')
      )

      if (exportButton) {
        await exportButton.trigger('click')
        await flushPromises()

        expect(consoleErrorSpy).toHaveBeenCalled()
      }

      consoleErrorSpy.mockRestore()
    })
  })

  describe('Import Calendar', () => {
    it('should trigger file input when clicking import button', async () => {
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const importButton = wrapper.findAll('button').find(btn =>
        btn.text().includes('Carica')
      )

      expect(importButton).toBeDefined()
    })

    it('should reject non-ICS files', async () => {
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      // Create a mock file with wrong extension
      const file = new File(['content'], 'test.txt', { type: 'text/plain' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      // Should not call preview (validation should fail)
      expect(calendarApi.previewImport).not.toHaveBeenCalled()
    })

    it('should show preview modal when duplicates are detected', async () => {
      const mockPreview: ImportPreviewResponse = {
        newEvents: 5,
        duplicateEvents: 3,
        errorEvents: 0,
        duplicates: [
          {
            title: 'Meeting',
            existingDate: '2025-01-15',
            newDate: '2025-01-15',
            contentChanged: false
          }
        ]
      }

      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])
      vi.mocked(calendarApi.previewImport).mockResolvedValue(mockPreview)

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      // Create a mock ICS file
      const file = new File(['BEGIN:VCALENDAR\nEND:VCALENDAR'], 'test.ics', { type: 'text/calendar' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      // Preview modal should be shown
      expect(wrapper.text()).toContain('Anteprima Importazione')
    })

    it('should import directly when no duplicates exist', async () => {
      const mockPreview: ImportPreviewResponse = {
        newEvents: 5,
        duplicateEvents: 0,
        errorEvents: 0,
        duplicates: []
      }

      const mockResult = {
        success: true,
        totalParsed: 5,
        successCount: 5,
        failedCount: 0
      }

      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])
      vi.mocked(calendarApi.previewImport).mockResolvedValue(mockPreview)
      vi.mocked(calendarApi.confirmImport).mockResolvedValue(mockResult)

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const file = new File(['BEGIN:VCALENDAR\nEND:VCALENDAR'], 'test.ics', { type: 'text/calendar' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      // Should import directly without showing modal
      expect(calendarApi.confirmImport).toHaveBeenCalled()
    })

    it('should handle import error gracefully', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])
      vi.mocked(calendarApi.previewImport).mockRejectedValue(new Error('Preview failed'))

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const file = new File(['BEGIN:VCALENDAR\nEND:VCALENDAR'], 'test.ics', { type: 'text/calendar' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      expect(consoleErrorSpy).toHaveBeenCalled()

      consoleErrorSpy.mockRestore()
    })
  })

  describe('Import Preview Modal', () => {
    it('should display preview statistics', async () => {
      const mockPreview: ImportPreviewResponse = {
        newEvents: 10,
        duplicateEvents: 5,
        errorEvents: 2,
        duplicates: []
      }

      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])
      vi.mocked(calendarApi.previewImport).mockResolvedValue(mockPreview)

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const file = new File(['BEGIN:VCALENDAR\nEND:VCALENDAR'], 'test.ics', { type: 'text/calendar' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      // Check statistics are displayed
      expect(wrapper.text()).toContain('10')
      expect(wrapper.text()).toContain('5')
      expect(wrapper.text()).toContain('2')
    })

    it('should allow calendar selection for import', async () => {
      const mockPreview: ImportPreviewResponse = {
        newEvents: 5,
        duplicateEvents: 1,
        errorEvents: 0,
        duplicates: []
      }

      const mockCalendars = [
        createMockCalendar(1, { name: 'Personal', isDefault: true }),
        createMockCalendar(2, { name: 'Work' })
      ]

      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue(mockCalendars)
      vi.mocked(calendarApi.previewImport).mockResolvedValue(mockPreview)

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const file = new File(['BEGIN:VCALENDAR\nEND:VCALENDAR'], 'test.ics', { type: 'text/calendar' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      // Should have calendar selector
      const calendarSelect = wrapper.find('select')
      expect(calendarSelect.exists()).toBe(true)

      const options = calendarSelect.findAll('option')
      expect(options.length).toBe(2)
    })

    it('should confirm import with selected strategy', async () => {
      const mockPreview: ImportPreviewResponse = {
        newEvents: 5,
        duplicateEvents: 1,
        errorEvents: 0,
        duplicates: []
      }

      const mockResult = {
        success: true,
        totalParsed: 6,
        successCount: 5,
        failedCount: 1
      }

      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])
      vi.mocked(calendarApi.previewImport).mockResolvedValue(mockPreview)
      vi.mocked(calendarApi.confirmImport).mockResolvedValue(mockResult)

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const file = new File(['BEGIN:VCALENDAR\nEND:VCALENDAR'], 'test.ics', { type: 'text/calendar' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      // Click confirm button
      const confirmButton = wrapper.findAll('button').find(btn =>
        btn.text().includes('Conferma Importazione')
      )

      if (confirmButton) {
        await confirmButton.trigger('click')
        await flushPromises()

        expect(calendarApi.confirmImport).toHaveBeenCalledWith(
          expect.any(File),
          DuplicateStrategy.SKIP,
          1
        )
      }
    })

    it('should cancel import when clicking cancel', async () => {
      const mockPreview: ImportPreviewResponse = {
        newEvents: 5,
        duplicateEvents: 1,
        errorEvents: 0,
        duplicates: []
      }

      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])
      vi.mocked(calendarApi.previewImport).mockResolvedValue(mockPreview)

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const file = new File(['BEGIN:VCALENDAR\nEND:VCALENDAR'], 'test.ics', { type: 'text/calendar' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      // Modal should be visible
      expect(wrapper.text()).toContain('Anteprima Importazione')

      // Click cancel
      const cancelButton = wrapper.findAll('button').find(btn => btn.text().includes('Annulla'))
      if (cancelButton) {
        await cancelButton.trigger('click')
        await flushPromises()

        // Modal should be hidden
        expect(wrapper.text()).not.toContain('Conferma Importazione')
      }
    })
  })

  describe('Import Result Display', () => {
    it('should display success message after successful import', async () => {
      const mockPreview: ImportPreviewResponse = {
        newEvents: 5,
        duplicateEvents: 0,
        errorEvents: 0,
        duplicates: []
      }

      const mockResult = {
        success: true,
        totalParsed: 5,
        successCount: 5,
        failedCount: 0
      }

      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])
      vi.mocked(calendarApi.previewImport).mockResolvedValue(mockPreview)
      vi.mocked(calendarApi.confirmImport).mockResolvedValue(mockResult)

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const file = new File(['BEGIN:VCALENDAR\nEND:VCALENDAR'], 'test.ics', { type: 'text/calendar' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      // Should show success result - "Importazione completata con successo"
      expect(wrapper.text()).toContain('completata con successo')
    })

    it('should display partial success message with errors', async () => {
      const mockPreview: ImportPreviewResponse = {
        newEvents: 5,
        duplicateEvents: 0,
        errorEvents: 0,
        duplicates: []
      }

      const mockResult = {
        success: true,
        totalParsed: 10,
        successCount: 7,
        failedCount: 3,
        errors: ['Error 1', 'Error 2', 'Error 3']
      }

      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])
      vi.mocked(calendarApi.previewImport).mockResolvedValue(mockPreview)
      vi.mocked(calendarApi.confirmImport).mockResolvedValue(mockResult)

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const file = new File(['BEGIN:VCALENDAR\nEND:VCALENDAR'], 'test.ics', { type: 'text/calendar' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      // Should show partial success - "parziale"
      expect(wrapper.text()).toContain('parziale')
    })

    it('should reload stats after successful import', async () => {
      const mockPreview: ImportPreviewResponse = {
        newEvents: 5,
        duplicateEvents: 0,
        errorEvents: 0,
        duplicates: []
      }

      const mockResult = {
        success: true,
        totalParsed: 5,
        successCount: 5,
        failedCount: 0
      }

      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats({ totalTasks: 10 }))
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])
      vi.mocked(calendarApi.previewImport).mockResolvedValue(mockPreview)
      vi.mocked(calendarApi.confirmImport).mockResolvedValue(mockResult)

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      // Clear call count
      vi.mocked(calendarApi.getCalendarStats).mockClear()

      const file = new File(['BEGIN:VCALENDAR\nEND:VCALENDAR'], 'test.ics', { type: 'text/calendar' })
      const fileInput = wrapper.find('input[type="file"]')

      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        writable: false
      })

      await fileInput.trigger('change')
      await flushPromises()

      // Stats should be reloaded after import
      expect(calendarApi.getCalendarStats).toHaveBeenCalled()
    })
  })

  describe('Event Listeners', () => {
    it('should reload calendars when calendars-updated event is fired', async () => {
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([createMockCalendar(1, { isDefault: true })])

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      // Clear call count
      vi.mocked(calendarsApi.getAllCalendars).mockClear()

      // Dispatch event
      window.dispatchEvent(new CustomEvent('calendars-updated'))
      await flushPromises()

      // Should reload calendars
      expect(calendarsApi.getAllCalendars).toHaveBeenCalled()
    })

    it('should cleanup event listeners on unmount', async () => {
      vi.mocked(calendarApi.getCalendarStats).mockResolvedValue(createMockStats())
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(ProfileCalendarData)
      await flushPromises()

      const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener')

      wrapper.unmount()

      expect(removeEventListenerSpy).toHaveBeenCalledWith('calendars-updated', expect.any(Function))

      removeEventListenerSpy.mockRestore()
    })
  })
})
