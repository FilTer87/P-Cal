import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { splitTasksByTime, sortTasksByStartTime, groupTasksByDate, useTaskFilters } from '../../composables/useTaskFilters'
import type { Task } from '../../types/task'

describe('useTaskFilters', () => {
  // Mock current time for consistent testing
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2025-06-15T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // Helper to create mock task
  const createMockTask = (id: number, start: string, end: string): Task => ({
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
    reminders: []
  })

  describe('splitTasksByTime', () => {
    it('should split tasks into current and past', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'), // Future
        createMockTask(2, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z'), // Past
        createMockTask(3, '2025-06-25T10:00:00Z', '2025-06-25T11:00:00Z'), // Future
        createMockTask(4, '2025-06-05T10:00:00Z', '2025-06-05T11:00:00Z')  // Past
      ]

      const { current, past } = splitTasksByTime(tasks)

      expect(current).toHaveLength(2)
      expect(past).toHaveLength(2)
      expect(current.map(t => t.id)).toEqual([1, 3])
      expect(past.map(t => t.id)).toEqual([4, 2])
    })

    it('should sort current tasks by start time', () => {
      const tasks = [
        createMockTask(1, '2025-06-25T10:00:00Z', '2025-06-25T11:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(3, '2025-06-22T10:00:00Z', '2025-06-22T11:00:00Z')
      ]

      const { current } = splitTasksByTime(tasks)

      expect(current.map(t => t.id)).toEqual([2, 3, 1])
    })

    it('should sort past tasks by start time', () => {
      const tasks = [
        createMockTask(1, '2025-06-05T10:00:00Z', '2025-06-05T11:00:00Z'),
        createMockTask(2, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z'),
        createMockTask(3, '2025-06-08T10:00:00Z', '2025-06-08T11:00:00Z')
      ]

      const { past } = splitTasksByTime(tasks)

      expect(past.map(t => t.id)).toEqual([1, 3, 2])
    })

    it('should handle empty array', () => {
      const { current, past } = splitTasksByTime([])

      expect(current).toEqual([])
      expect(past).toEqual([])
    })

    it('should handle task ending exactly at current time', () => {
      const task = createMockTask(1, '2025-06-15T11:00:00Z', '2025-06-15T12:00:00Z')

      const { current, past } = splitTasksByTime([task])

      // Task ending exactly now should be in current (>= comparison)
      expect(current).toHaveLength(1)
      expect(past).toHaveLength(0)
    })

    it('should handle task ending 1ms before current time', () => {
      vi.setSystemTime(new Date('2025-06-15T12:00:00.001Z'))
      const task = createMockTask(1, '2025-06-15T11:00:00Z', '2025-06-15T12:00:00Z')

      const { current, past } = splitTasksByTime([task])

      expect(current).toHaveLength(0)
      expect(past).toHaveLength(1)
    })

    it('should handle task starting in past but ending in future (ongoing)', () => {
      const task = createMockTask(1, '2025-06-14T10:00:00Z', '2025-06-16T10:00:00Z')

      const { current, past } = splitTasksByTime([task])

      expect(current).toHaveLength(1)
      expect(past).toHaveLength(0)
    })

    it('should handle multiple tasks with same start time', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T12:00:00Z'),
        createMockTask(3, '2025-06-20T10:00:00Z', '2025-06-20T13:00:00Z')
      ]

      const { current } = splitTasksByTime(tasks)

      expect(current).toHaveLength(3)
      // All have same start time, so order should be preserved
      expect(current.map(t => t.id)).toEqual([1, 2, 3])
    })
  })

  describe('sortTasksByStartTime', () => {
    it('should sort tasks by start datetime ascending', () => {
      const tasks = [
        createMockTask(1, '2025-06-25T10:00:00Z', '2025-06-25T11:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(3, '2025-06-22T10:00:00Z', '2025-06-22T11:00:00Z')
      ]

      const sorted = sortTasksByStartTime(tasks)

      expect(sorted.map(t => t.id)).toEqual([2, 3, 1])
    })

    it('should not mutate original array', () => {
      const tasks = [
        createMockTask(1, '2025-06-25T10:00:00Z', '2025-06-25T11:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')
      ]

      const original = [...tasks]
      sortTasksByStartTime(tasks)

      expect(tasks).toEqual(original)
    })

    it('should handle empty array', () => {
      expect(sortTasksByStartTime([])).toEqual([])
    })

    it('should handle single task', () => {
      const task = createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')

      expect(sortTasksByStartTime([task])).toEqual([task])
    })

    it('should handle tasks with same start time (stable sort)', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T12:00:00Z'),
        createMockTask(3, '2025-06-20T10:00:00Z', '2025-06-20T13:00:00Z')
      ]

      const sorted = sortTasksByStartTime(tasks)

      // Should maintain original order for equal start times
      expect(sorted.map(t => t.id)).toEqual([1, 2, 3])
    })

    it('should handle tasks spanning different days', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T23:00:00Z', '2025-06-21T01:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(3, '2025-06-21T05:00:00Z', '2025-06-21T06:00:00Z')
      ]

      const sorted = sortTasksByStartTime(tasks)

      expect(sorted.map(t => t.id)).toEqual([2, 1, 3])
    })

    it('should handle tasks at midnight', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T00:00:00Z', '2025-06-20T01:00:00Z'),
        createMockTask(2, '2025-06-19T23:59:59Z', '2025-06-20T00:30:00Z'),
        createMockTask(3, '2025-06-20T00:00:01Z', '2025-06-20T01:00:00Z')
      ]

      const sorted = sortTasksByStartTime(tasks)

      expect(sorted.map(t => t.id)).toEqual([2, 1, 3])
    })
  })

  describe('groupTasksByDate', () => {
    it('should group tasks by date (YYYY-MM-DD)', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(2, '2025-06-20T14:00:00Z', '2025-06-20T15:00:00Z'),
        createMockTask(3, '2025-06-21T10:00:00Z', '2025-06-21T11:00:00Z')
      ]

      const grouped = groupTasksByDate(tasks)

      expect(Object.keys(grouped)).toHaveLength(2)
      expect(grouped['2025-06-20']).toHaveLength(2)
      expect(grouped['2025-06-21']).toHaveLength(1)
    })

    it('should sort tasks within each day by start time', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T14:00:00Z', '2025-06-20T15:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(3, '2025-06-20T12:00:00Z', '2025-06-20T13:00:00Z')
      ]

      const grouped = groupTasksByDate(tasks)

      expect(grouped['2025-06-20'].map(t => t.id)).toEqual([2, 3, 1])
    })

    it('should handle empty array', () => {
      expect(groupTasksByDate([])).toEqual({})
    })

    it('should handle single task', () => {
      const task = createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')
      const grouped = groupTasksByDate([task])

      expect(Object.keys(grouped)).toHaveLength(1)
      expect(grouped['2025-06-20']).toEqual([task])
    })

    it('should handle tasks spanning midnight (grouped by start date)', () => {
      const task = createMockTask(1, '2025-06-20T23:00:00Z', '2025-06-21T01:00:00Z')
      const grouped = groupTasksByDate([task])

      // Grouped by start date
      expect(Object.keys(grouped)).toHaveLength(1)
      expect(grouped['2025-06-20']).toHaveLength(1)
      expect(grouped['2025-06-21']).toBeUndefined()
    })

    it('should handle tasks across multiple days', () => {
      const tasks = [
        createMockTask(1, '2025-06-18T10:00:00Z', '2025-06-18T11:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(3, '2025-06-22T10:00:00Z', '2025-06-22T11:00:00Z')
      ]

      const grouped = groupTasksByDate(tasks)

      expect(Object.keys(grouped).sort()).toEqual(['2025-06-18', '2025-06-20', '2025-06-22'])
    })

    it('should handle multiple tasks on same day at different times', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T08:00:00Z', '2025-06-20T09:00:00Z'),
        createMockTask(2, '2025-06-20T12:00:00Z', '2025-06-20T13:00:00Z'),
        createMockTask(3, '2025-06-20T16:00:00Z', '2025-06-20T17:00:00Z'),
        createMockTask(4, '2025-06-20T20:00:00Z', '2025-06-20T21:00:00Z')
      ]

      const grouped = groupTasksByDate(tasks)

      expect(grouped['2025-06-20']).toHaveLength(4)
      expect(grouped['2025-06-20'].map(t => t.id)).toEqual([1, 2, 3, 4])
    })

    it('should handle tasks at midnight', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T00:00:00Z', '2025-06-20T01:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')
      ]

      const grouped = groupTasksByDate(tasks)

      expect(grouped['2025-06-20']).toHaveLength(2)
      expect(grouped['2025-06-20'].map(t => t.id)).toEqual([1, 2])
    })
  })

  describe('useTaskFilters composable', () => {
    it('should export all utilities', () => {
      const { splitTasksByTime, sortTasksByStartTime, groupTasksByDate } = useTaskFilters()

      expect(typeof splitTasksByTime).toBe('function')
      expect(typeof sortTasksByStartTime).toBe('function')
      expect(typeof groupTasksByDate).toBe('function')
    })

    it('should provide working splitTasksByTime', () => {
      const { splitTasksByTime } = useTaskFilters()
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(2, '2025-06-10T10:00:00Z', '2025-06-10T11:00:00Z')
      ]

      const { current, past } = splitTasksByTime(tasks)

      expect(current).toHaveLength(1)
      expect(past).toHaveLength(1)
    })

    it('should provide working sortTasksByStartTime', () => {
      const { sortTasksByStartTime } = useTaskFilters()
      const tasks = [
        createMockTask(1, '2025-06-25T10:00:00Z', '2025-06-25T11:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')
      ]

      const sorted = sortTasksByStartTime(tasks)

      expect(sorted[0].id).toBe(2)
      expect(sorted[1].id).toBe(1)
    })

    it('should provide working groupTasksByDate', () => {
      const { groupTasksByDate } = useTaskFilters()
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z'),
        createMockTask(2, '2025-06-21T10:00:00Z', '2025-06-21T11:00:00Z')
      ]

      const grouped = groupTasksByDate(tasks)

      expect(Object.keys(grouped)).toHaveLength(2)
    })
  })

  describe('Edge Cases', () => {
    it('should handle ISO datetime strings with milliseconds', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:00.123Z', '2025-06-20T11:00:00.456Z'),
        createMockTask(2, '2025-06-20T10:00:00.789Z', '2025-06-20T11:00:00.999Z')
      ]

      const sorted = sortTasksByStartTime(tasks)

      expect(sorted[0].id).toBe(1)
      expect(sorted[1].id).toBe(2)
    })

    it('should handle tasks with very close start times', () => {
      const tasks = [
        createMockTask(1, '2025-06-20T10:00:01Z', '2025-06-20T11:00:00Z'),
        createMockTask(2, '2025-06-20T10:00:00Z', '2025-06-20T11:00:00Z')
      ]

      const sorted = sortTasksByStartTime(tasks)

      expect(sorted[0].id).toBe(2)
      expect(sorted[1].id).toBe(1)
    })

    it('should handle very large arrays efficiently', () => {
      const tasks = Array.from({ length: 1000 }, (_, i) =>
        createMockTask(i, `2025-${String(Math.floor(i / 30) + 1).padStart(2, '0')}-${String((i % 30) + 1).padStart(2, '0')}T10:00:00Z`, `2025-${String(Math.floor(i / 30) + 1).padStart(2, '0')}-${String((i % 30) + 1).padStart(2, '0')}T11:00:00Z`)
      )

      const start = Date.now()
      const sorted = sortTasksByStartTime(tasks)
      const duration = Date.now() - start

      expect(sorted).toHaveLength(1000)
      expect(duration).toBeLessThan(100) // Should be fast
    })
  })
})
