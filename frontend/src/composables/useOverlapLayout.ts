import type { Task } from '@/types/task'
import { getTaskKey } from '@/utils/recurrence'

export interface LayoutResult {
  taskKey: string | number  // Unique key (uses occurrenceId for recurring tasks)
  leftOffset: string   // Left offset: "0px", "40px", "80px"
  width: string        // Width: "calc(100% - 0px)", "calc(100% - 40px)"
  zIndex: number       // z-index for stacking (higher layer = higher zIndex)
  layer: number        // Layer in group (0-based, 0 = bottom/widest)
  totalLayers: number  // Total layers in group
}

interface TimeTask {
  key: string | number  // Unique key (uses occurrenceId for recurring tasks)
  start: number // Hour float (10:30 → 10.5)
  end: number
  task: Task
}

/**
 * Composable for calculating layered layout (Google Calendar style)
 * Overlapping events with progressive left offset
 */
export function useOverlapLayout() {

  // Offset configuration
  const LAYER_OFFSET_PX = 26 // Pixels offset per layer
  const MAX_VISIBLE_LAYERS = 5 // Max visible layers (beyond this, becomes too narrow)

  /**
   * Calculate layered layout for overlapping tasks
   * @param tasks Task list with startDatetime/endDatetime
   * @returns Map<taskKey, LayoutResult>
   */
  function calculateLayout(tasks: Task[]): Map<string | number, LayoutResult> {
    if (!tasks || tasks.length === 0) {
      return new Map()
    }

    // Convert tasks to time-based objects
    const timeTasks: TimeTask[] = tasks
      .filter(task => task.startDatetime && task.endDatetime)
      .map(task => ({
        key: getTaskKey(task),
        start: getHourFloat(task.startDatetime),
        end: getHourFloat(task.endDatetime),
        task: task
      }))
      .sort((a, b) => {
        // Sort by start time, then by duration (longer first)
        if (a.start !== b.start) {
          return a.start - b.start
        }
        return (b.end - b.start) - (a.end - a.start) // Longer tasks first
      })

    // Find overlapping groups
    const groups = findOverlappingGroups(timeTasks)

    // Calculate layers for each group
    const layoutMap = new Map<string | number, LayoutResult>()

    for (const group of groups) {
      const layers = assignLayers(group)
      const totalLayers = Math.max(...Array.from(layers.values())) + 1

      for (const [taskKey, layer] of layers.entries()) {
        const offsetPx = layer * LAYER_OFFSET_PX

        layoutMap.set(taskKey, {
          taskKey,
          leftOffset: `${offsetPx}px`,
          width: `calc(100% - ${offsetPx}px)`,
          // Higher layer = higher zIndex (appears on top)
          zIndex: 10 + layer,
          layer,
          totalLayers
        })
      }
    }

    return layoutMap
  }

  /**
   * Convert datetime string to hour float
   */
  function getHourFloat(datetime: string): number {
    const date = new Date(datetime)
    return date.getHours() + date.getMinutes() / 60 + date.getSeconds() / 3600
  }

  /**
   * Group tasks that overlap temporally
   */
  function findOverlappingGroups(tasks: TimeTask[]): TimeTask[][] {
    if (tasks.length === 0) return []

    const groups: TimeTask[][] = []
    let currentGroup: TimeTask[] = [tasks[0]]

    for (let i = 1; i < tasks.length; i++) {
      const task = tasks[i]

      // Check if task overlaps with ANY task in current group
      const groupEnd = Math.max(...currentGroup.map(t => t.end))

      if (task.start < groupEnd) {
        // Overlap detected: add to current group
        currentGroup.push(task)
      } else {
        // No overlap: start new group
        groups.push(currentGroup)
        currentGroup = [task]
      }
    }

    if (currentGroup.length > 0) {
      groups.push(currentGroup)
    }

    return groups
  }

  /**
   * Assign layers for each task in a group
   *
   * Algorithm:
   * 1. Process tasks by start time (earlier first, longer first if tied)
   * 2. For each task, find the lowest layer where it doesn't overlap
   * 3. Tasks that don't overlap can share the same layer
   *
   * This creates a "cascading" effect like Google Calendar
   */
  function assignLayers(group: TimeTask[]): Map<string | number, number> {
    const layers = new Map<string | number, number>()
    const layerTasks: TimeTask[][] = [] // Tasks in each layer

    // Sort by start time, then by duration (longer first)
    const sortedTasks = [...group].sort((a, b) => {
      if (a.start !== b.start) {
        return a.start - b.start // Earlier first
      }
      return (b.end - b.start) - (a.end - a.start) // Longer first if same start
    })

    for (const task of sortedTasks) {
      // Find the lowest layer where task doesn't overlap with existing tasks
      let assignedLayer = -1

      for (let layer = 0; layer < layerTasks.length; layer++) {
        const hasOverlap = layerTasks[layer].some(otherTask =>
          // Overlap check: task.start < otherTask.end AND task.end > otherTask.start
          task.start < otherTask.end && task.end > otherTask.start
        )

        if (!hasOverlap) {
          // Layer is free (no overlap with existing tasks)
          assignedLayer = layer
          break
        }
      }

      // If no layer available, create new layer
      if (assignedLayer === -1) {
        assignedLayer = layerTasks.length
        layerTasks.push([])
      }

      // Assign task to layer
      layers.set(task.key, assignedLayer)
      layerTasks[assignedLayer].push(task)
    }

    return layers
  }

  /**
   * Calculate maximum number of layers for the day
   */
  function getMaxLayers(tasks: Task[]): number {
    const layout = calculateLayout(tasks)

    if (layout.size === 0) return 1

    let maxLayers = 1
    for (const result of layout.values()) {
      maxLayers = Math.max(maxLayers, result.totalLayers)
    }

    return maxLayers
  }

  /**
   * Check if task has too many layers (becomes too narrow)
   */
  function hasTooManyLayers(layoutResult: LayoutResult): boolean {
    return layoutResult.totalLayers > MAX_VISIBLE_LAYERS
  }

  /**
   * Calculate effective width percentage
   */
  function getEffectiveWidthPercent(layoutResult: LayoutResult): number {
    const offsetPx = layoutResult.layer * LAYER_OFFSET_PX
    // Assuming column ~200px, approximation
    return Math.max(20, 100 - (offsetPx / 2))
  }

  return {
    calculateLayout,
    getMaxLayers,
    hasTooManyLayers,
    getEffectiveWidthPercent,
    LAYER_OFFSET_PX
  }
}
