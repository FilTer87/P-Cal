import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import MonthView from '../../../components/Calendar/MonthView.vue'
import type { Task } from '../../../types/task'

describe('MonthView.vue', () => {
  beforeEach(() => {
    // Create and set active Pinia instance for each test
    setActivePinia(createPinia())
  })

  const createMockTask = (id: number, overrides: Partial<Task> = {}): Task => ({
    id,
    title: `Task ${id}`,
    description: '',
    startDatetime: '2025-06-20T10:00:00Z',
    endDatetime: '2025-06-20T11:00:00Z',
    location: '',
    color: '#3788d8',
    userId: 1,
    createdAt: '2025-06-01T00:00:00Z',
    updatedAt: '2025-06-01T00:00:00Z',
    reminders: [],
    ...overrides
  })

  const createMockCalendarDay = (overrides: any = {}) => ({
    date: new Date('2025-06-15'),
    dayOfMonth: 15,
    isCurrentMonth: true,
    isToday: false,
    isSelected: false,
    tasks: [],
    ...overrides
  })

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render calendar grid', () => {
      const calendarDays = Array.from({ length: 35 }, (_, i) =>
        createMockCalendarDay({ dayOfMonth: i + 1 })
      )

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      expect(wrapper.find('.calendar-grid-mobile').exists()).toBe(true)
      expect(wrapper.findAll('.calendar-day')).toHaveLength(35)
    })

    it('should render weekday headers', () => {
      const wrapper = mount(MonthView, {
        props: { calendarDays: [] }
      })

      const headers = wrapper.findAll('.calendar-day-header')
      expect(headers.length).toBe(7)
    })

    it('should render day numbers', () => {
      const calendarDays = [
        createMockCalendarDay({ dayOfMonth: 1 }),
        createMockCalendarDay({ dayOfMonth: 15 }),
        createMockCalendarDay({ dayOfMonth: 31 })
      ]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      expect(wrapper.text()).toContain('1')
      expect(wrapper.text()).toContain('15')
      expect(wrapper.text()).toContain('31')
    })

    it('should render task count when tasks present', () => {
      const calendarDays = [
        createMockCalendarDay({
          tasks: [
            createMockTask(1),
            createMockTask(2),
            createMockTask(3)
          ]
        })
      ]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      expect(wrapper.text()).toContain('3')
    })
  })

  describe('Task Display', () => {
    it('should render up to maxVisibleTasks tasks', () => {
      const tasks = [
        createMockTask(1),
        createMockTask(2),
        createMockTask(3),
        createMockTask(4)
      ]

      const calendarDays = [createMockCalendarDay({ tasks })]

      const wrapper = mount(MonthView, {
        props: {
          calendarDays,
          maxVisibleTasks: 3
        }
      })

      // Should show only 3 tasks
      const taskElements = wrapper.findAll('.calendar-day .space-y-1 > div:not(button)')
      expect(taskElements.length).toBe(3)
    })

    it('should show "altro/i" button when more tasks than visible', () => {
      const tasks = [
        createMockTask(1),
        createMockTask(2),
        createMockTask(3),
        createMockTask(4),
        createMockTask(5)
      ]

      const calendarDays = [createMockCalendarDay({ tasks })]

      const wrapper = mount(MonthView, {
        props: {
          calendarDays,
          maxVisibleTasks: 3
        }
      })

      expect(wrapper.text()).toContain('+2 altro/i')
    })

    it('should not show "altro/i" button when tasks <= maxVisibleTasks', () => {
      const tasks = [createMockTask(1), createMockTask(2)]

      const calendarDays = [createMockCalendarDay({ tasks })]

      const wrapper = mount(MonthView, {
        props: {
          calendarDays,
          maxVisibleTasks: 3
        }
      })

      expect(wrapper.text()).not.toContain('altro/i')
    })

    it('should render task titles', () => {
      const tasks = [
        createMockTask(1, { title: 'Meeting' }),
        createMockTask(2, { title: 'Lunch' })
      ]

      const calendarDays = [createMockCalendarDay({ tasks })]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      expect(wrapper.text()).toContain('Meeting')
      expect(wrapper.text()).toContain('Lunch')
    })
  })

  describe('Day Styling', () => {
    it('should apply selected day classes', () => {
      const calendarDays = [
        createMockCalendarDay({ isSelected: true })
      ]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      const day = wrapper.find('.calendar-day')
      expect(day.classes()).toContain('bg-blue-50')
      expect(day.classes()).toContain('dark:bg-blue-900/20')
    })

    it('should apply today classes', () => {
      const calendarDays = [
        createMockCalendarDay({ isToday: true })
      ]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      const day = wrapper.find('.calendar-day')
      expect(day.classes()).toContain('bg-yellow-50')
      expect(day.classes()).toContain('dark:bg-yellow-900/20')
    })

    it('should apply out-of-month classes', () => {
      const calendarDays = [
        createMockCalendarDay({ isCurrentMonth: false })
      ]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      const day = wrapper.find('.calendar-day')
      expect(day.classes()).toContain('text-gray-400')
      expect(day.classes()).toContain('dark:text-gray-600')
    })

    it('should apply hover classes for normal days', () => {
      const calendarDays = [
        createMockCalendarDay({
          isSelected: false,
          isToday: false
        })
      ]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      const day = wrapper.find('.calendar-day')
      expect(day.classes()).toContain('hover:bg-gray-100')
      expect(day.classes()).toContain('dark:hover:bg-gray-700')
    })

    it('should prioritize selected over today styling', () => {
      const calendarDays = [
        createMockCalendarDay({
          isSelected: true,
          isToday: true
        })
      ]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      const day = wrapper.find('.calendar-day')
      expect(day.classes()).toContain('bg-blue-50')
      expect(day.classes()).not.toContain('bg-yellow-50')
    })
  })

  describe('Events', () => {
    it('should emit select-date on day click', async () => {
      const date = new Date('2025-06-15')
      const calendarDays = [createMockCalendarDay({ date })]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      await wrapper.find('.calendar-day').trigger('click')

      expect(wrapper.emitted('select-date')).toBeTruthy()
      expect(wrapper.emitted('select-date')?.[0]).toEqual([date])
    })

    it('should emit create-task on day double-click', async () => {
      const date = new Date('2025-06-15')
      const calendarDays = [createMockCalendarDay({ date })]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      await wrapper.find('.calendar-day').trigger('dblclick')

      expect(wrapper.emitted('create-task')).toBeTruthy()
      expect(wrapper.emitted('create-task')?.[0]).toEqual([date])
    })

    it('should emit task-click on task click', async () => {
      const task = createMockTask(1)
      const calendarDays = [createMockCalendarDay({ tasks: [task] })]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      const taskElement = wrapper.find('.calendar-day .space-y-1 > div:first-child')
      await taskElement.trigger('click')

      expect(wrapper.emitted('task-click')).toBeTruthy()
      expect(wrapper.emitted('task-click')?.[0]).toEqual([task])
    })

    it('should emit open-day-view on "altro/i" click', async () => {
      const date = new Date('2025-06-15')
      const tasks = [
        createMockTask(1),
        createMockTask(2),
        createMockTask(3),
        createMockTask(4)
      ]

      const calendarDays = [createMockCalendarDay({ date, tasks })]

      const wrapper = mount(MonthView, {
        props: {
          calendarDays,
          maxVisibleTasks: 2
        }
      })

      const moreButton = wrapper.find('button')
      await moreButton.trigger('click')

      expect(wrapper.emitted('open-day-view')).toBeTruthy()
      expect(wrapper.emitted('open-day-view')?.[0]).toEqual([date])
    })

    it('should stop propagation on task click (not select day)', async () => {
      const task = createMockTask(1)
      const calendarDays = [createMockCalendarDay({ tasks: [task] })]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      const taskElement = wrapper.find('.calendar-day .space-y-1 > div:first-child')
      await taskElement.trigger('click')

      // Task click should not trigger select-date
      expect(wrapper.emitted('task-click')).toBeTruthy()
      expect(wrapper.emitted('select-date')).toBeFalsy()
    })

    it('should stop propagation on "altro/i" click (not select day)', async () => {
      const tasks = Array.from({ length: 5 }, (_, i) => createMockTask(i + 1))
      const calendarDays = [createMockCalendarDay({ tasks })]

      const wrapper = mount(MonthView, {
        props: {
          calendarDays,
          maxVisibleTasks: 2
        }
      })

      const moreButton = wrapper.find('button')
      await moreButton.trigger('click')

      // More button click should not trigger select-date
      expect(wrapper.emitted('open-day-view')).toBeTruthy()
      expect(wrapper.emitted('select-date')).toBeFalsy()
    })
  })

  describe('Props', () => {
    it('should accept custom maxVisibleTasks', () => {
      const tasks = Array.from({ length: 10 }, (_, i) => createMockTask(i + 1))
      const calendarDays = [createMockCalendarDay({ tasks })]

      const wrapper = mount(MonthView, {
        props: {
          calendarDays,
          maxVisibleTasks: 5
        }
      })

      const taskElements = wrapper.findAll('.calendar-day .space-y-1 > div:not(button)')
      expect(taskElements.length).toBe(5)
      expect(wrapper.text()).toContain('+5 altro/i')
    })

    it('should default maxVisibleTasks to 3', () => {
      const tasks = Array.from({ length: 5 }, (_, i) => createMockTask(i + 1))
      const calendarDays = [createMockCalendarDay({ tasks })]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      const taskElements = wrapper.findAll('.calendar-day .space-y-1 > div:not(button)')
      expect(taskElements.length).toBe(3)
      expect(wrapper.text()).toContain('+2 altro/i')
    })

    it('should handle empty calendarDays', () => {
      const wrapper = mount(MonthView, {
        props: { calendarDays: [] }
      })

      expect(wrapper.findAll('.calendar-day')).toHaveLength(0)
    })
  })

  describe('Edge Cases', () => {
    it('should handle days with no tasks', () => {
      const calendarDays = [
        createMockCalendarDay({ tasks: [] })
      ]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      expect(wrapper.find('.calendar-day .space-y-1').exists()).toBe(true)
      expect(wrapper.findAll('.calendar-day .space-y-1 > div')).toHaveLength(0)
    })

    it('should handle days with undefined tasks', () => {
      const calendarDays = [
        createMockCalendarDay({ tasks: undefined })
      ]

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      expect(wrapper.find('.calendar-day .space-y-1').exists()).toBe(true)
      expect(wrapper.findAll('.calendar-day .space-y-1 > div')).toHaveLength(0)
    })

    it('should handle exactly maxVisibleTasks tasks', () => {
      const tasks = [
        createMockTask(1),
        createMockTask(2),
        createMockTask(3)
      ]

      const calendarDays = [createMockCalendarDay({ tasks })]

      const wrapper = mount(MonthView, {
        props: {
          calendarDays,
          maxVisibleTasks: 3
        }
      })

      const taskElements = wrapper.findAll('.calendar-day .space-y-1 > div:not(button)')
      expect(taskElements.length).toBe(3)
      expect(wrapper.text()).not.toContain('altro/i')
    })

    it('should render 42 days for a typical month view (6 weeks)', () => {
      const calendarDays = Array.from({ length: 42 }, (_, i) =>
        createMockCalendarDay({ dayOfMonth: i + 1 })
      )

      const wrapper = mount(MonthView, {
        props: { calendarDays }
      })

      expect(wrapper.findAll('.calendar-day')).toHaveLength(42)
    })
  })
})
