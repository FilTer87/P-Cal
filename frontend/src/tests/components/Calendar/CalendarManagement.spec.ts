import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import CalendarManagement from '../../../components/Calendar/CalendarManagement.vue'
import { calendarsApi, type Calendar, type CalendarRequest } from '../../../services/calendarsApi'

// Mock calendarsApi
vi.mock('../../../services/calendarsApi', () => ({
  calendarsApi: {
    getAllCalendars: vi.fn(),
    createCalendar: vi.fn(),
    updateCalendar: vi.fn(),
    deleteCalendar: vi.fn()
  }
}))

// Mock toast composable
vi.mock('@/composables/useCustomToast', () => ({
  useCustomToast: () => ({
    showError: vi.fn(),
    showSuccess: vi.fn()
  })
}))

describe('CalendarManagement.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllTimers()
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
    it('should render the component with title', async () => {
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      expect(wrapper.text()).toContain('I Miei Calendari')
    })

    it('should display calendars list', async () => {
      const mockCalendars = [
        createMockCalendar(1, { name: 'Personal', isDefault: true }),
        createMockCalendar(2, { name: 'Work' })
      ]

      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue(mockCalendars)

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      expect(wrapper.text()).toContain('Personal')
      expect(wrapper.text()).toContain('Work')
    })

    it('should display default indicator for default calendar', async () => {
      const mockCalendars = [
        createMockCalendar(1, { name: 'Personal', isDefault: true }),
        createMockCalendar(2, { name: 'Work', isDefault: false })
      ]

      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue(mockCalendars)

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      // The word "default" appears in the list
      expect(wrapper.text()).toContain('default')
    })

    it('should display task count for each calendar', async () => {
      const mockCalendars = [
        createMockCalendar(1, { taskCount: 5 }),
        createMockCalendar(2, { taskCount: 12 })
      ]

      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue(mockCalendars)

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      expect(wrapper.text()).toContain('5')
      expect(wrapper.text()).toContain('12')
    })

    it('should show empty state when no calendars exist', async () => {
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      expect(wrapper.text()).toContain('Nessun calendario')
    })

    it('should display color indicators', async () => {
      const mockCalendars = [
        createMockCalendar(1, { color: '#ff0000' }),
        createMockCalendar(2, { color: '#00ff00' })
      ]

      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue(mockCalendars)

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      const colorBadges = wrapper.findAll('[style*="background-color"]')
      expect(colorBadges.length).toBeGreaterThan(0)
    })
  })

  describe('Create Calendar Modal', () => {
    it('should open create modal when clicking new calendar button', async () => {
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      const createButton = wrapper.find('button:first-of-type')
      await createButton.trigger('click')
      await flushPromises()

      expect(wrapper.text()).toContain('Nuovo Calendario')
    })

    it('should have calendar form with required fields', async () => {
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      // Open modal
      const createButton = wrapper.find('button:first-of-type')
      await createButton.trigger('click')
      await flushPromises()

      // Check form fields exist
      expect(wrapper.find('input#calendar-name').exists()).toBe(true)
      expect(wrapper.find('input#calendar-slug').exists()).toBe(true)
    })
  })

  describe('Calendar Display', () => {
    it('should display CalDAV path for each calendar', async () => {
      const mockCalendars = [
        createMockCalendar(1, { caldavPath: '/caldav/user/calendar-1/' })
      ]
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue(mockCalendars)

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      expect(wrapper.text()).toContain('/caldav/user/calendar-1/')
    })

    it('should display slug for each calendar', async () => {
      const mockCalendars = [
        createMockCalendar(1, { slug: 'my-calendar' })
      ]
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue(mockCalendars)

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      expect(wrapper.text()).toContain('my-calendar')
    })
  })

  describe('Timezone', () => {
    it('should display timezone selector in create modal', async () => {
      vi.mocked(calendarsApi.getAllCalendars).mockResolvedValue([])

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      // Open create modal
      const createButton = wrapper.find('button:first-of-type')
      await createButton.trigger('click')
      await flushPromises()

      // Check if timezone selector exists
      const timezoneSelect = wrapper.find('select#calendar-timezone')
      expect(timezoneSelect.exists()).toBe(true)
    })
  })

  describe('Error Handling', () => {
    it('should handle API errors gracefully', async () => {
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      vi.mocked(calendarsApi.getAllCalendars).mockRejectedValue(new Error('Network error'))

      const wrapper = mount(CalendarManagement)
      await flushPromises()

      // Component should still render even if API fails
      expect(wrapper.exists()).toBe(true)

      consoleErrorSpy.mockRestore()
    })
  })
})
