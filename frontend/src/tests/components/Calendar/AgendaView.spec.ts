import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AgendaView from '../../../components/Calendar/AgendaView.vue'
import type { Task } from '../../../types/task'

describe('AgendaView.vue', () => {
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
    startDatetime: start,
    endDatetime: end,
    location: '',
    color: '#3788d8',
    userId: 1,
    createdAt: '2025-06-01T00:00:00Z',
    updatedAt: '2025-06-01T00:00:00Z',
    reminders: [],
    ...overrides
  })

  describe('Rendering', () => {
    it('should render empty state when no tasks', () => {
      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate: {},
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Nessuna attività in questo periodo')
    })

    it('should render task groups by date', () => {
      const tasksByDate = {
        '2025-06-20': [createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')],
        '2025-06-21': [createMockTask(2, '2025-06-21T10:00:00Z', '2025-06-21T11:00:00Z')]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      const groups = wrapper.findAll('.bg-white.dark\\:bg-gray-800.rounded-lg.p-4')
      expect(groups).toHaveLength(2)
    })

    it('should render date headers', () => {
      const tasksByDate = {
        '2025-06-20': [createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      expect(wrapper.find('h3').exists()).toBe(true)
    })

    it('should render tasks within groups', () => {
      const tasksByDate = {
        '2025-06-20': [
          createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z', { title: 'Task A' }),
          createMockTask(2, '2025-06-20T14:00:00Z', '2025-06-20T15:00:00Z', { title: 'Task B' })
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Task A')
      expect(wrapper.text()).toContain('Task B')
    })
  })

  describe('Date Sorting', () => {
    it('should sort dates chronologically', () => {
      const tasksByDate = {
        '2025-06-25': [createMockTask(1, '2025-06-25T10:00:00Z', '2025-06-25T11:00:00Z')],
        '2025-06-20': [createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')],
        '2025-06-22': [createMockTask(3, '2025-06-22T10:00:00Z', '2025-06-22T11:00:00Z')]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      const groups = wrapper.findAll('.bg-white.dark\\:bg-gray-800.rounded-lg.p-4')
      expect(groups).toHaveLength(3)
      // Order should be: 20, 22, 25
    })
  })

  describe('Today Special Handling', () => {
    it('should show separator for today with past tasks', () => {
      const tasksByDate = {
        '2025-06-15': [
          createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'), // Future
          createMockTask(2, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')  // Past
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Attività completata')
    })

    it('should display past task count for today', () => {
      const tasksByDate = {
        '2025-06-15': [
          createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
          createMockTask(2, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z'),
          createMockTask(3, '2025-06-09T10:00:00Z', '2025-06-09T11:00:00Z')
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('2 Attività completate')
    })

    it('should not show separator for today with only current tasks', () => {
      const tasksByDate = {
        '2025-06-15': [
          createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
          createMockTask(2, '2025-06-21T10:00:00Z', '2025-06-21T11:00:00Z')
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).not.toContain('Attività completate')
    })

    it('should not apply special handling for other dates', () => {
      const tasksByDate = {
        '2025-06-20': [
          createMockTask(1, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z') // Past task but not today
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      // Should just show the task, no separator
      expect(wrapper.findAll('.flex.items-center.my-4').length).toBe(0)
    })
  })

  describe('Past Tasks Toggle', () => {
    it('should show chevron for today with past tasks', () => {
      const tasksByDate = {
        '2025-06-15': [
          createMockTask(1, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      const chevron = wrapper.find('svg')
      expect(chevron.exists()).toBe(true)
    })

    it('should rotate chevron when showPastTasks is true', () => {
      const tasksByDate = {
        '2025-06-15': [
          createMockTask(1, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: true
        }
      })

      const chevron = wrapper.find('svg')
      expect(chevron.classes()).toContain('rotate-180')
    })

    it('should not rotate chevron when showPastTasks is false', () => {
      const tasksByDate = {
        '2025-06-15': [
          createMockTask(1, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      const chevron = wrapper.find('svg')
      expect(chevron.classes()).not.toContain('rotate-180')
    })
  })

  describe('Events', () => {
    it('should emit task-click when task is clicked', async () => {
      const task = createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')
      const tasksByDate = {
        '2025-06-20': [task]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      const taskElement = wrapper.find('.p-3.rounded-lg.cursor-pointer')
      await taskElement.trigger('click')

      expect(wrapper.emitted('task-click')).toBeTruthy()
      expect(wrapper.emitted('task-click')?.[0]).toEqual([task])
    })

    it('should emit toggle-past-tasks when separator is clicked', async () => {
      const tasksByDate = {
        '2025-06-15': [
          createMockTask(1, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
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
      const tasksByDate = {
        '2025-06-20': [
          createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z', { title: 'Important Meeting' })
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Important Meeting')
    })

    it('should display task description when present', () => {
      const tasksByDate = {
        '2025-06-20': [
          createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z', {
            title: 'Meeting',
            description: 'Discuss roadmap'
          })
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Discuss roadmap')
    })

    it('should not display description when empty', () => {
      const tasksByDate = {
        '2025-06-20': [
          createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z', {
            title: 'Meeting',
            description: ''
          })
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      const descriptions = wrapper.findAll('p').filter(p => p.classes().includes('text-xs'))
      // Should only have time, no description
      expect(descriptions.every(p => !p.text().includes('Meeting'))).toBe(true)
    })
  })

  describe('Edge Cases', () => {
    it('should handle empty tasksByDate object', () => {
      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate: {},
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('Nessuna attività in questo periodo')
    })

    it('should handle multiple days with varying task counts', () => {
      const tasksByDate = {
        '2025-06-20': [createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')],
        '2025-06-21': [
          createMockTask(2, '2025-06-21T10:00:00Z', '2025-06-21T11:00:00Z'),
          createMockTask(3, '2025-06-21T14:00:00Z', '2025-06-21T15:00:00Z')
        ],
        '2025-06-22': []
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      // Should render only non-empty days
      const groups = wrapper.findAll('.bg-white.dark\\:bg-gray-800.rounded-lg.p-4')
      expect(groups.length).toBeGreaterThan(0)
    })

    it('should handle today being the only date', () => {
      const tasksByDate = {
        '2025-06-15': [
          createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
          createMockTask(2, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
        ]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      expect(wrapper.text()).toContain('1 Attività completata')
    })

    it('should handle date far in the past', () => {
      const tasksByDate = {
        '2020-01-01': [createMockTask(1, '2020-01-01T10:00:00Z', '2020-01-01T11:00:00Z')]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      const groups = wrapper.findAll('.bg-white.dark\\:bg-gray-800.rounded-lg.p-4')
      expect(groups).toHaveLength(1)
    })

    it('should handle date far in the future', () => {
      const tasksByDate = {
        '2030-12-31': [createMockTask(1, '2030-12-31T10:00:00Z', '2030-12-31T11:00:00Z')]
      }

      const wrapper = mount(AgendaView, {
        props: {
          tasksByDate,
          showPastTasks: false
        }
      })

      const groups = wrapper.findAll('.bg-white.dark\\:bg-gray-800.rounded-lg.p-4')
      expect(groups).toHaveLength(1)
    })
  })
})
