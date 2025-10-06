import { describe, it, expect, beforeEach, vi } from 'vitest'
import { getTaskDisplayClasses, getTaskDisplayStyle, TASK_COLOR_MAP, useTaskDisplay } from '../../composables/useTaskDisplay'
import type { Task } from '../../types/task'

describe('useTaskDisplay', () => {
  // Mock current time for consistent past/future task testing
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2025-06-15T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // Helper to create mock task
  const createMockTask = (overrides: Partial<Task> = {}): Task => ({
    id: 1,
    title: 'Test Task',
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

  describe('TASK_COLOR_MAP', () => {
    it('should contain all standard Tailwind colors', () => {
      expect(TASK_COLOR_MAP).toHaveProperty('#3b82f6', 'blue')
      expect(TASK_COLOR_MAP).toHaveProperty('#10b981', 'emerald')
      expect(TASK_COLOR_MAP).toHaveProperty('#ef4444', 'red')
      expect(TASK_COLOR_MAP).toHaveProperty('#f59e0b', 'amber')
      expect(TASK_COLOR_MAP).toHaveProperty('#8b5cf6', 'violet')
      expect(TASK_COLOR_MAP).toHaveProperty('#ec4899', 'pink')
    })

    it('should map default color to blue', () => {
      expect(TASK_COLOR_MAP['#3788d8']).toBe('blue')
    })

    it('should have 17 color mappings', () => {
      expect(Object.keys(TASK_COLOR_MAP)).toHaveLength(17)
    })
  })

  describe('getTaskDisplayClasses', () => {
    it('should return base classes for future task with Tailwind color', () => {
      const task = createMockTask({ color: '#3b82f6' })
      const classes = getTaskDisplayClasses(task)

      expect(classes).toContain('border-l-2')
      expect(classes).toContain('bg-blue-50')
      expect(classes).toContain('dark:bg-blue-900/20')
      expect(classes).toContain('hover:bg-blue-100')
      expect(classes).toContain('task-custom-color')
      expect(classes).not.toContain('opacity-25')
    })

    it('should add opacity classes for past tasks', () => {
      const task = createMockTask({
        startDatetime: '2025-06-10T10:00:00Z',
        endDatetime: '2025-06-10T11:00:00Z', // Past
        color: '#3b82f6'
      })
      const classes = getTaskDisplayClasses(task)

      expect(classes).toContain('opacity-25')
      expect(classes).toContain('hover:opacity-75')
    })

    it('should use border-l-4 when detailed=true', () => {
      const task = createMockTask()
      const classes = getTaskDisplayClasses(task, true)

      expect(classes).toContain('border-l-4')
      expect(classes).not.toContain('border-l-2')
    })

    it('should handle all Tailwind colors correctly', () => {
      const colors = [
        { hex: '#ef4444', name: 'red' },
        { hex: '#10b981', name: 'emerald' },
        { hex: '#f59e0b', name: 'amber' },
        { hex: '#8b5cf6', name: 'violet' }
      ]

      colors.forEach(({ hex, name }) => {
        const task = createMockTask({ color: hex })
        const classes = getTaskDisplayClasses(task)

        expect(classes).toContain(`bg-${name}-50`)
        expect(classes).toContain(`dark:bg-${name}-900/20`)
      })
    })

    it('should use neutral background for custom hex colors', () => {
      const task = createMockTask({ color: '#FF5733' }) // Custom color not in map
      const classes = getTaskDisplayClasses(task)

      expect(classes).toContain('bg-gray-50')
      expect(classes).toContain('dark:bg-gray-700')
      expect(classes).toContain('hover:bg-gray-100')
      expect(classes).toContain('dark:hover:bg-gray-600')
    })

    it('should default to blue color when task.color is undefined', () => {
      const task = createMockTask({ color: undefined })
      const classes = getTaskDisplayClasses(task)

      expect(classes).toContain('bg-blue-50')
      expect(classes).toContain('dark:bg-blue-900/20')
    })

    it('should include task-custom-color for all tasks', () => {
      const taskWithColor = createMockTask({ color: '#3b82f6' })
      const taskWithCustomColor = createMockTask({ color: '#FF5733' })

      expect(getTaskDisplayClasses(taskWithColor)).toContain('task-custom-color')
      expect(getTaskDisplayClasses(taskWithCustomColor)).toContain('task-custom-color')
    })
  })

  describe('getTaskDisplayStyle', () => {
    it('should return inline styles with CSS custom properties', () => {
      const task = createMockTask({ color: '#3b82f6' })
      const style = getTaskDisplayStyle(task)

      expect(style).toHaveProperty('--task-color', '#3b82f6')
      expect(style).toHaveProperty('borderLeftColor', '#3b82f6')
      expect(style).toHaveProperty('borderLeftWidth', '2px')
      expect(style).toHaveProperty('borderLeftStyle', 'solid')
    })

    it('should add background styles for custom colors', () => {
      const task = createMockTask({ color: '#FF5733' })
      const style = getTaskDisplayStyle(task)

      expect(style).toHaveProperty('--task-bg-color')
      expect(style).toHaveProperty('backgroundColor')
      expect(style['--task-bg-color']).toMatch(/^rgba\(255, 87, 51, 0\.1\)$/)
      expect(style.backgroundColor).toMatch(/^rgba\(255, 87, 51, 0\.1\)$/)
    })

    it('should NOT add background styles for Tailwind colors', () => {
      const task = createMockTask({ color: '#3b82f6' })
      const style = getTaskDisplayStyle(task)

      expect(style).not.toHaveProperty('--task-bg-color')
      expect(style).not.toHaveProperty('backgroundColor')
    })

    it('should handle all Tailwind colors without background override', () => {
      Object.keys(TASK_COLOR_MAP).forEach(color => {
        const task = createMockTask({ color })
        const style = getTaskDisplayStyle(task)

        expect(style).toHaveProperty('--task-color', color)
        expect(style).toHaveProperty('borderLeftColor', color)
        expect(style).not.toHaveProperty('backgroundColor')
      })
    })

    it('should default to blue when color is undefined', () => {
      const task = createMockTask({ color: undefined })
      const style = getTaskDisplayStyle(task)

      expect(style['--task-color']).toBe('#3788d8')
      expect(style.borderLeftColor).toBe('#3788d8')
    })

    it('should correctly convert hex to rgba with alpha 0.1', () => {
      const task = createMockTask({ color: '#FFFFFF' })
      const style = getTaskDisplayStyle(task)

      expect(style.backgroundColor).toBe('rgba(255, 255, 255, 0.1)')
    })

    it('should handle 3-digit hex colors (if any)', () => {
      // Note: current implementation expects 6-digit hex
      // This test documents the current behavior
      const task = createMockTask({ color: '#FFF' })
      const style = getTaskDisplayStyle(task)

      // Will return NaN, NaN, NaN - documenting this edge case
      expect(style).toHaveProperty('--task-color', '#FFF')
    })
  })

  describe('useTaskDisplay composable', () => {
    it('should export all utilities', () => {
      const { TASK_COLOR_MAP, getTaskDisplayClasses, getTaskDisplayStyle } = useTaskDisplay()

      expect(TASK_COLOR_MAP).toBeDefined()
      expect(typeof getTaskDisplayClasses).toBe('function')
      expect(typeof getTaskDisplayStyle).toBe('function')
    })

    it('should provide functional color map', () => {
      const { TASK_COLOR_MAP } = useTaskDisplay()

      expect(TASK_COLOR_MAP['#3b82f6']).toBe('blue')
      expect(Object.keys(TASK_COLOR_MAP).length).toBeGreaterThan(0)
    })

    it('should provide working display class function', () => {
      const { getTaskDisplayClasses } = useTaskDisplay()
      const task = createMockTask()

      const classes = getTaskDisplayClasses(task)
      expect(classes).toContain('border-l-2')
      expect(classes).toContain('task-custom-color')
    })

    it('should provide working display style function', () => {
      const { getTaskDisplayStyle } = useTaskDisplay()
      const task = createMockTask()

      const style = getTaskDisplayStyle(task)
      expect(style).toHaveProperty('--task-color')
      expect(style).toHaveProperty('borderLeftColor')
    })
  })

  describe('Edge Cases', () => {
    it('should handle task at exact current time', () => {
      const task = createMockTask({
        startDatetime: '2025-06-15T11:00:00Z',
        endDatetime: '2025-06-15T12:00:00Z' // Ends exactly now
      })

      // Task ending exactly now should NOT be considered past (< not <=)
      const classes = getTaskDisplayClasses(task)
      expect(classes).not.toContain('opacity-25')
    })

    it('should handle task with very old end date', () => {
      const task = createMockTask({
        startDatetime: '2020-01-01T10:00:00Z',
        endDatetime: '2020-01-01T11:00:00Z'
      })

      const classes = getTaskDisplayClasses(task)
      expect(classes).toContain('opacity-25')
    })

    it('should handle task with far future date', () => {
      const task = createMockTask({
        startDatetime: '2030-01-01T10:00:00Z',
        endDatetime: '2030-01-01T11:00:00Z'
      })

      const classes = getTaskDisplayClasses(task)
      expect(classes).not.toContain('opacity-25')
    })

    it('should handle empty/null color gracefully', () => {
      const task = createMockTask({ color: '' as any })

      // Empty string defaults to '#3788d8' (|| operator)
      const style = getTaskDisplayStyle(task)
      expect(style['--task-color']).toBe('#3788d8')
    })

    it('should handle malformed hex color', () => {
      const task = createMockTask({ color: 'notahexcolor' as any })

      const style = getTaskDisplayStyle(task)
      // Should still set the color (even if invalid)
      expect(style['--task-color']).toBe('notahexcolor')
    })
  })
})
