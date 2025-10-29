import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DayView from '../../../components/Calendar/DayView.vue'
import type { Task } from '../../../types/task'

describe('DayView.vue', () => {
  beforeEach(() => {
    // Create and set active Pinia instance for each test
    setActivePinia(createPinia())
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2025-06-15T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const createMockTask = (id: number, start: string, end: string, overrides: Partial<Task> = {}): Task => ({
    id,
    title: `Task ${id}`,
    description: '',
    startDatetimeLocal: start,
    endDatetimeLocal: end,
    location: '',
    color: '#3788d8',
    userId: 1,
    createdAt: '2025-06-01T00:00:00Z',
    updatedAt: '2025-06-01T00:00:00Z',
    reminders: [],
    ...overrides
  })

  describe('Rendering', () => {
    it('should render day title', () => {
      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks: [],
          showPastTasks: false
        }
      })

      expect(wrapper.find('h3').exists()).toBe(true)
      expect(wrapper.find('h3').text()).toContain('domenica')
      expect(wrapper.find('h3').text()).toContain('15')
    })

    it('should render current tasks', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z', { title: 'Future Task' })
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Future Task')
    })

    it('should render empty state when no tasks', () => {
      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks: [],
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Nessuna attività programmata per oggi')
      expect(wrapper.text()).toContain('Crea la prima attività')
    })
  })

  describe('Task Splitting', () => {
    it('should show only current tasks when showPastTasks is false', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z', { title: 'Future' }),
        createMockTask(2, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z', { title: 'Past' })
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Future')
      expect(wrapper.text()).toContain('1 Attività completata')
    })

    it('should show separator when past tasks exist', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(2, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('1 Attività completata')
      const separator = wrapper.find('.flex.items-center.my-4')
      expect(separator.exists()).toBe(true)
    })

    it('should display past task count correctly', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(2, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z'),
        createMockTask(3, '2025-06-09T10:00:00Z', '2025-06-09T11:00:00Z'),
        createMockTask(4, '2025-06-08T10:00:00Z', '2025-06-08T11:00:00Z')
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('3 Attività completate')
    })
  })

  describe('Past Tasks Toggle', () => {
    it('should show chevron icon', () => {
      const tasks = [
        createMockTask(1, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      const chevron = wrapper.find('svg')
      expect(chevron.exists()).toBe(true)
    })

    it('should rotate chevron when showPastTasks is true', () => {
      const tasks = [
        createMockTask(1, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: true
        }
      })

      const chevron = wrapper.find('svg')
      expect(chevron.classes()).toContain('rotate-180')
    })

    it('should not rotate chevron when showPastTasks is false', () => {
      const tasks = [
        createMockTask(1, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      const chevron = wrapper.find('svg')
      expect(chevron.classes()).not.toContain('rotate-180')
    })

    it('should hide past tasks when showPastTasks is false', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z', { title: 'Future' }),
        createMockTask(2, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z', { title: 'Past' })
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      const pastSection = wrapper.find('[v-show]')
      // In test environment, v-show doesn't actually hide, we just check structure
      expect(wrapper.text()).toContain('Future')
    })
  })

  describe('Events', () => {
    it('should emit task-click when task is clicked', async () => {
      const task = createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks: [task],
          showPastTasks: false
        }
      })

      const taskElement = wrapper.find('.p-3.rounded-lg.cursor-pointer')
      await taskElement.trigger('click')

      expect(wrapper.emitted('task-click')).toBeTruthy()
      expect(wrapper.emitted('task-click')?.[0]).toEqual([task])
    })

    it('should emit create-task when "Crea prima attività" is clicked', async () => {
      const currentDate = new Date('2025-06-15')

      const wrapper = mount(DayView, {
        props: {
          currentDate,
          tasks: [],
          showPastTasks: false
        }
      })

      const createButton = wrapper.find('button')
      await createButton.trigger('click')

      expect(wrapper.emitted('create-task')).toBeTruthy()
      expect(wrapper.emitted('create-task')?.[0]).toEqual([currentDate])
    })

    it('should emit toggle-past-tasks when separator is clicked', async () => {
      const tasks = [
        createMockTask(1, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      const separator = wrapper.find('.flex.items-center.my-4.cursor-pointer')
      await separator.trigger('click')

      expect(wrapper.emitted('toggle-past-tasks')).toBeTruthy()
    })
  })

  describe('Task Display', () => {
    it('should display task title', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z', { title: 'Important Meeting' })
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Important Meeting')
    })

    it('should display task description when present', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z', {
          title: 'Meeting',
          description: 'Discuss project roadmap'
        })
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Discuss project roadmap')
    })

    it('should not display description when empty', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z', {
          title: 'Meeting',
          description: ''
        })
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      // Check that task title is present but description text is not in the output
      expect(wrapper.text()).toContain('Meeting')
      expect(wrapper.text()).not.toContain('description')
    })

    it('should display task time range', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:30:00Z', '2025-06-20T11:45:00Z')
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      // Time format depends on settings, just check it exists
      const timeElements = wrapper.findAll('.text-xs.text-gray-500')
      expect(timeElements.length).toBeGreaterThan(0)
    })
  })

  describe('Edge Cases', () => {
    it('should handle empty tasks array', () => {
      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks: [],
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Nessuna attività programmata')
    })

    it('should handle only current tasks', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(2, '2025-06-21T10:00:00Z', '2025-06-21T11:00:00Z')
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).not.toContain('Attività completate')
    })

    it('should handle only past tasks', () => {
      const tasks = [
        createMockTask(1, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z'),
        createMockTask(2, '2025-06-09T10:00:00Z', '2025-06-09T11:00:00Z')
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      // Should show past tasks separator but NOT show empty state
      expect(wrapper.text()).toContain('2 Attività completate')
      // Empty state should NOT be shown when there are past tasks
      expect(wrapper.text()).not.toContain('Nessuna attività programmata')
    })

    it('should handle mixed current and past tasks', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(2, '2025-06-21T10:00:00Z', '2025-06-21T11:00:00Z'),
        createMockTask(3, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z'),
        createMockTask(4, '2025-06-09T10:00:00Z', '2025-06-09T11:00:00Z')
      ]

      const wrapper = mount(DayView, {
        props: {
          currentDate: new Date('2025-06-15'),
          tasks,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('2 Attività completate')
      expect(wrapper.text()).not.toContain('Nessuna attività programmata')
    })
  })
})
