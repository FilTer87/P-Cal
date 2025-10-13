import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import RecurrenceInput from '../../components/Tasks/RecurrenceInput.vue'
import { RecurrenceFrequency, RecurrenceEndType, type TaskFormData } from '../../types/task'
import enUS from '../../i18n/locales/en-US.json'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: enUS
  }
})

describe('RecurrenceInput.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  const createMockFormData = (overrides: Partial<TaskFormData> = {}): TaskFormData => ({
    title: 'Test Task',
    description: '',
    startDate: '2025-10-07',
    startTime: '10:00',
    endDate: '2025-10-07',
    endTime: '11:00',
    location: '',
    color: '#3788d8',
    isRecurring: false,
    reminders: [],
    ...overrides
  })

  describe('Toggle recurrence', () => {
    it('should render recurrence toggle', () => {
      const formData = createMockFormData()
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      expect(wrapper.text()).toContain('Repeat event')
    })

    it('should emit update when toggling recurrence on', async () => {
      const formData = createMockFormData()
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      const toggle = wrapper.find('button')
      await toggle.trigger('click')

      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY,
        recurrenceInterval: 1,
        recurrenceEndType: RecurrenceEndType.NEVER
      })
    })

    it('should emit update when toggling recurrence off', async () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      const toggle = wrapper.find('button')
      await toggle.trigger('click')

      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        isRecurring: false,
        recurrenceFrequency: undefined
      })
    })
  })

  describe('Recurrence options visibility', () => {
    it('should hide options when not recurring', () => {
      const formData = createMockFormData({ isRecurring: false })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      expect(wrapper.text()).not.toContain('Frequency')
      expect(wrapper.find('select').exists()).toBe(false)
    })

    it('should show options when recurring', () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      expect(wrapper.text()).toContain('Frequency')
      expect(wrapper.findAll('select').length).toBeGreaterThan(0)
    })
  })

  describe('Frequency selection', () => {
    it('should display all frequency options', () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      const select = wrapper.findAll('select')[0]
      expect(select.html()).toContain('Daily')
      expect(select.html()).toContain('Weekly')
      expect(select.html()).toContain('Monthly')
      expect(select.html()).toContain('Yearly')
    })

    it('should emit update when changing frequency', async () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      const select = wrapper.findAll('select')[0]
      await select.setValue(RecurrenceFrequency.MONTHLY)

      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        recurrenceFrequency: RecurrenceFrequency.MONTHLY
      })
    })
  })

  describe('Interval input', () => {
    it('should display interval input', () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY,
        recurrenceInterval: 2
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      const intervalInput = wrapper.find('input[type="number"]')
      expect(intervalInput.exists()).toBe(true)
      expect(intervalInput.element.value).toBe('2')
    })

    it('should emit update when changing interval', async () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      const intervalInput = wrapper.find('input[type="number"]')
      await intervalInput.setValue('3')

      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        recurrenceInterval: 3
      })
    })
  })

  describe('Weekly recurrence - day selection', () => {
    it('should show day buttons for weekly frequency', () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      expect(wrapper.text()).toContain('Days of week')
      const dayButtons = wrapper.findAll('button').filter(btn =>
        btn.text().match(/^(Mon|Tue|Wed|Thu|Fri|Sat|Sun)$/)
      )
      expect(dayButtons.length).toBe(7)
    })

    it('should not show day buttons for non-weekly frequency', () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      expect(wrapper.text()).not.toContain('Days of week')
    })

    it('should toggle day selection', async () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY,
        recurrenceByDay: []
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      const dayButtons = wrapper.findAll('button').filter(btn =>
        btn.text() === 'Mon'
      )
      await dayButtons[0].trigger('click')

      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        recurrenceByDay: ['MO']
      })
    })

    it('should deselect day when clicking again', async () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY,
        recurrenceByDay: ['MO', 'WE']
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      const dayButtons = wrapper.findAll('button').filter(btn =>
        btn.text() === 'Mon'
      )
      await dayButtons[0].trigger('click')

      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        recurrenceByDay: ['WE']
      })
    })
  })

  describe('End type selection', () => {
    it('should display end type select', () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      const selects = wrapper.findAll('select')
      const endTypeSelect = selects.find(s => s.html().includes('Never'))
      expect(endTypeSelect).toBeTruthy()
      expect(endTypeSelect!.html()).toContain('After a certain number')
      expect(endTypeSelect!.html()).toContain('On a date')
    })

    it('should show count input when end type is COUNT', () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY,
        recurrenceEndType: RecurrenceEndType.COUNT,
        recurrenceCount: 10
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      expect(wrapper.text()).toContain('Number of occurrences')
      const numberInputs = wrapper.findAll('input[type="number"]')
      const countInput = numberInputs.find(input =>
        input.element.value === '10'
      )
      expect(countInput).toBeTruthy()
    })

    it('should show date input when end type is DATE', () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY,
        recurrenceEndType: RecurrenceEndType.DATE,
        recurrenceEndDate: '2025-12-31'
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      expect(wrapper.text()).toContain('End date')
      const dateInput = wrapper.find('input[type="date"]')
      expect(dateInput.exists()).toBe(true)
      expect(dateInput.element.value).toBe('2025-12-31')
    })

    it('should emit update when changing end type', async () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY,
        recurrenceEndType: RecurrenceEndType.NEVER
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      const selects = wrapper.findAll('select')
      const endTypeSelect = selects.find(s => s.html().includes('Never'))
      await endTypeSelect!.setValue(RecurrenceEndType.COUNT)

      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        recurrenceEndType: RecurrenceEndType.COUNT,
        recurrenceCount: undefined,
        recurrenceEndDate: undefined
      })
    })
  })

  describe('End date validation', () => {
    it('should set min date to start date', () => {
      const formData = createMockFormData({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.DAILY,
        recurrenceEndType: RecurrenceEndType.DATE,
        startDate: '2025-10-15'
      })
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData,
          startDate: '2025-10-15'
        },
        global: {
          plugins: [i18n]
        }
      })

      const dateInput = wrapper.find('input[type="date"]')
      expect(dateInput.attributes('min')).toBe('2025-10-15')
    })
  })

  describe('Complete workflow', () => {
    it('should handle complete weekly recurrence setup', async () => {
      const formData = createMockFormData()
      const wrapper = mount(RecurrenceInput, {
        props: {
          modelValue: formData
        },
        global: {
          plugins: [i18n]
        }
      })

      // Enable recurrence
      const toggle = wrapper.find('button')
      await toggle.trigger('click')

      // Select weekly
      let emitted = wrapper.emitted('update:modelValue')!
      const afterToggle = emitted[emitted.length - 1][0]

      await wrapper.setProps({ modelValue: afterToggle })

      // Select days
      const dayButtons = wrapper.findAll('button').filter(btn =>
        btn.text() === 'Mon' || btn.text() === 'Wed' || btn.text() === 'Fri'
      )

      for (const btn of dayButtons) {
        await btn.trigger('click')
        emitted = wrapper.emitted('update:modelValue')!
        await wrapper.setProps({ modelValue: emitted[emitted.length - 1][0] })
      }

      // Set count
      await wrapper.setProps({
        modelValue: {
          ...wrapper.props('modelValue'),
          recurrenceEndType: RecurrenceEndType.COUNT
        }
      })

      const numberInputs = wrapper.findAll('input[type="number"]')
      const countInput = numberInputs[numberInputs.length - 1]
      await countInput.setValue('8')

      emitted = wrapper.emitted('update:modelValue')!
      const final = emitted[emitted.length - 1][0]

      expect(final).toMatchObject({
        isRecurring: true,
        recurrenceFrequency: RecurrenceFrequency.WEEKLY,
        recurrenceEndType: RecurrenceEndType.COUNT,
        recurrenceCount: 8
      })
    })
  })
})
