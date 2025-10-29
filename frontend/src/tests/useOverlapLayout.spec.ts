import { describe, it, expect } from 'vitest'
import { useOverlapLayout } from '@/composables/useOverlapLayout'
import type { Task } from '@/types/task'

describe('useOverlapLayout', () => {
  const { calculateLayout, getMaxLayers } = useOverlapLayout()

  // Helper to create mock tasks
  function createTask(id: number, startHour: number, endHour: number): Task {
    const date = '2025-10-07'
    const startMinutes = Math.floor((startHour % 1) * 60)
    const endMinutes = Math.floor((endHour % 1) * 60)
    const startHourInt = Math.floor(startHour)
    const endHourInt = Math.floor(endHour)

    // Use local timezone (no Z suffix) to match getHourFloat behavior
    return {
      id,
      title: `Task ${id}`,
      startDatetimeLocal: `${date}T${String(startHourInt).padStart(2, '0')}:${String(startMinutes).padStart(2, '0')}:00`,
      endDatetimeLocal: `${date}T${String(endHourInt).padStart(2, '0')}:${String(endMinutes).padStart(2, '0')}:00`,
      color: '#3B82F6',
      description: '',
      location: '',
      reminders: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    } as Task
  }

  describe('Single task', () => {
    it('should handle single task with no offset', () => {
      const tasks = [createTask(1, 10, 11)] // 10:00-11:00

      const layout = calculateLayout(tasks)

      expect(layout.size).toBe(1)
      const result = layout.get(1)!
      expect(result.leftOffset).toBe('0px')
      expect(result.width).toBe('calc(100% - 0px)')
      expect(result.zIndex).toBe(10) // 10 + layer(0)
      expect(result.layer).toBe(0)
      expect(result.totalLayers).toBe(1)
    })
  })

  describe('Two overlapping tasks', () => {
    it('should stack two overlapping tasks', () => {
      const tasks = [
        createTask(1, 10, 11),    // 10:00-11:00 (più lungo, layer 0)
        createTask(2, 10.5, 11)   // 10:30-11:00 (più corto, layer 1)
      ]

      const layout = calculateLayout(tasks)

      expect(layout.size).toBe(2)

      // Task 1 (più lungo) dovrebbe essere layer 0 (sotto, più largo)
      const task1 = layout.get(1)!
      expect(task1.layer).toBe(0)
      expect(task1.leftOffset).toBe('0px')
      expect(task1.width).toBe('calc(100% - 0px)')
      expect(task1.zIndex).toBe(10) // 10 + layer(0) = 10

      // Task 2 (più corto) dovrebbe essere layer 1 (sopra, con offset)
      const task2 = layout.get(2)!
      expect(task2.layer).toBe(1)
      expect(task2.leftOffset).toBe('26px')
      expect(task2.width).toBe('calc(100% - 26px)')
      expect(task2.zIndex).toBe(11) // 10 + layer(1) = 11
    })
  })

  describe('Three overlapping tasks', () => {
    it('should stack three overlapping tasks by duration', () => {
      const tasks = [
        createTask(1, 10, 12),      // 10:00-12:00 (più lungo)
        createTask(2, 10.5, 11.5),  // 10:30-11:30 (medio)
        createTask(3, 11, 11.5)     // 11:00-11:30 (più corto)
      ]

      const layout = calculateLayout(tasks)

      expect(layout.size).toBe(3)

      // Task 1 (più lungo) → layer 0
      expect(layout.get(1)!.layer).toBe(0)
      expect(layout.get(1)!.leftOffset).toBe('0px')

      // Task 2 (medio) → layer 1
      expect(layout.get(2)!.layer).toBe(1)
      expect(layout.get(2)!.leftOffset).toBe('26px')

      // Task 3 (più corto) → layer 2
      expect(layout.get(3)!.layer).toBe(2)
      expect(layout.get(3)!.leftOffset).toBe('52px')
    })
  })

  describe('Non-overlapping tasks', () => {
    it('should not offset non-overlapping tasks', () => {
      const tasks = [
        createTask(1, 10, 11),    // 10:00-11:00
        createTask(2, 11, 12)     // 11:00-12:00 (non overlap)
      ]

      const layout = calculateLayout(tasks)

      expect(layout.size).toBe(2)

      // Entrambi dovrebbero essere layer 0 (no overlap)
      expect(layout.get(1)!.layer).toBe(0)
      expect(layout.get(1)!.leftOffset).toBe('0px')

      expect(layout.get(2)!.layer).toBe(0)
      expect(layout.get(2)!.leftOffset).toBe('0px')

      // Ma in gruppi separati
      expect(layout.get(1)!.totalLayers).toBe(1)
      expect(layout.get(2)!.totalLayers).toBe(1)
    })
  })

  describe('Complex scenario (Google Calendar style)', () => {
    it('should handle screenshot example 1', () => {
      // Scenario screenshot 1:
      // Event 1: 16:00-18:30 (più lungo, layer 0)
      // Event 2: 16:45-18:00 (medio, layer 1)
      // Event 3: 17:00-17:45 (più corto, layer 2)

      const tasks = [
        createTask(1, 16, 18.5),    // 16:00-18:30
        createTask(2, 16.75, 18),   // 16:45-18:00
        createTask(3, 17, 17.75)    // 17:00-17:45
      ]

      const layout = calculateLayout(tasks)

      expect(layout.size).toBe(3)

      // Verifica ordine layer (più lungo → layer più basso)
      const task1 = layout.get(1)!
      const task2 = layout.get(2)!
      const task3 = layout.get(3)!

      expect(task1.layer).toBeLessThan(task2.layer)
      expect(task2.layer).toBeLessThan(task3.layer)

      // Verifica offset progressivo
      expect(task1.leftOffset).toBe('0px')
      expect(task2.leftOffset).toBe('26px')
      expect(task3.leftOffset).toBe('52px')

      // Verifica zIndex crescente
      expect(task1.zIndex).toBeLessThan(task2.zIndex)
      expect(task2.zIndex).toBeLessThan(task3.zIndex)
    })

    it('should share layer for non-overlapping tasks within same group', () => {
      // Four tasks in same group:
      // Task 1: 15:30-22:30 (longest, overlaps all)
      // Task 2: 16:30-21:00 (long, overlaps 1,3,4)
      // Task 3: 18:00-19:00 (short, overlaps 1,2 only)
      // Task 4: 20:00-21:30 (short, overlaps 1,2 only, NOT 3)

      const tasks = [
        createTask(1, 15.5, 22.5),  // 15:30-22:30
        createTask(2, 16.5, 21),    // 16:30-21:00
        createTask(3, 18, 19),      // 18:00-19:00
        createTask(4, 20, 21.5)     // 20:00-21:30 (does NOT overlap with task 3)
      ]

      const layout = calculateLayout(tasks)

      expect(layout.size).toBe(4)

      const task1 = layout.get(1)!
      const task2 = layout.get(2)!
      const task3 = layout.get(3)!
      const task4 = layout.get(4)!

      // Task 1 and 2 should have different layers (they overlap)
      expect(task1.layer).toBe(0)
      expect(task2.layer).toBe(1)

      // Task 3 and 4 do NOT overlap each other → should share same layer
      expect(task3.layer).toBe(task4.layer)
      expect(task3.leftOffset).toBe(task4.leftOffset)

      // Both should be layer 2 (on top of task 1 and 2)
      expect(task3.layer).toBe(2)
      expect(task3.leftOffset).toBe('52px') // 26px * 2
    })

    it('should handle screenshot example 2', () => {
      // Scenario screenshot 2:
      // (Senza titolo) 1: 13:30-17:00
      // (Senza titolo) 2: 14:15-15:15
      // Event 1: 16:00-18:30
      // Event 2: 16:45-18:00
      // Event 3: 17:00-19:00

      const tasks = [
        createTask(1, 13.5, 17),    // 13:30-17:00 (più lungo)
        createTask(2, 14.25, 15.25), // 14:15-15:15
        createTask(3, 16, 18.5),    // 16:00-18:30
        createTask(4, 16.75, 18),   // 16:45-18:00
        createTask(5, 17, 19)       // 17:00-19:00
      ]

      const layout = calculateLayout(tasks)

      expect(layout.size).toBe(5)

      // Verifica che task più lunghi abbiano layer più bassi
      const durations = tasks.map(t => {
        const start = parseFloat(t.startDatetimeLocal.split('T')[1].split(':')[0]) +
                      parseFloat(t.startDatetimeLocal.split('T')[1].split(':')[1]) / 60
        const end = parseFloat(t.endDatetimeLocal.split('T')[1].split(':')[0]) +
                    parseFloat(t.endDatetimeLocal.split('T')[1].split(':')[1]) / 60
        return { id: t.id, duration: end - start }
      }).sort((a, b) => b.duration - a.duration)

      // Task con durata maggiore dovrebbe avere layer minore
      const longestTask = layout.get(durations[0].id)!
      expect(longestTask.layer).toBe(0)
    })
  })

  describe('Layer reuse', () => {
    it('should reuse layers for non-overlapping tasks in same group', () => {
      const tasks = [
        createTask(1, 10, 11),      // 10:00-11:00
        createTask(2, 10.5, 11.5),  // 10:30-11:30 (overlap con 1)
        createTask(3, 11.5, 12)     // 11:30-12:00 (overlap con 2, ma non con 1)
      ]

      const layout = calculateLayout(tasks)

      // Task 1 e Task 3 non si sovrappongono → possono condividere layer 0
      const task1Layer = layout.get(1)!.layer
      const task3Layer = layout.get(3)!.layer

      expect(task1Layer).toBe(0)
      expect(task3Layer).toBe(0) // Riusa layer 0
    })
  })

  describe('getMaxLayers', () => {
    it('should return max layers for the day', () => {
      const tasks = [
        createTask(1, 10, 11),
        createTask(2, 10.5, 11.5),
        createTask(3, 11, 11.5)
      ]

      const maxLayers = getMaxLayers(tasks)

      expect(maxLayers).toBeGreaterThanOrEqual(2) // Almeno 2 layer sovrapposti
    })

    it('should return 1 for single task', () => {
      const tasks = [createTask(1, 10, 11)]
      expect(getMaxLayers(tasks)).toBe(1)
    })

    it('should return 1 for non-overlapping tasks', () => {
      const tasks = [
        createTask(1, 10, 11),
        createTask(2, 11, 12),
        createTask(3, 12, 13)
      ]
      expect(getMaxLayers(tasks)).toBe(1)
    })
  })

  describe('Edge cases', () => {
    it('should handle empty task list', () => {
      const layout = calculateLayout([])
      expect(layout.size).toBe(0)
    })

    it('should handle tasks without datetime', () => {
      const tasks = [
        { id: 1, title: 'No datetime', startDatetimeLocal: '', endDatetimeLocal: '' } as Task
      ]

      const layout = calculateLayout(tasks)
      expect(layout.size).toBe(0)
    })

    it('should handle same start time (sort by duration)', () => {
      const tasks = [
        createTask(1, 10, 11),    // Duration: 1h
        createTask(2, 10, 11.5),  // Duration: 1.5h (più lungo)
        createTask(3, 10, 10.5)   // Duration: 0.5h (più corto)
      ]

      const layout = calculateLayout(tasks)

      // Task 2 (più lungo) dovrebbe essere layer 0
      expect(layout.get(2)!.layer).toBe(0)

      // Task 3 (più corto) dovrebbe essere layer più alto
      const task3Layer = layout.get(3)!.layer
      const task2Layer = layout.get(2)!.layer
      expect(task3Layer).toBeGreaterThan(task2Layer)
    })

    it('should handle tasks spanning multiple days (same day hours)', () => {
      const tasks = [
        createTask(1, 8, 20)  // 8:00-20:00 (12 ore)
      ]

      const layout = calculateLayout(tasks)

      expect(layout.size).toBe(1)
      expect(layout.get(1)!.layer).toBe(0)
      expect(layout.get(1)!.leftOffset).toBe('0px')
    })
  })
})
