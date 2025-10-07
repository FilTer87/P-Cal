import type { Task } from '@/types/task'

export interface LayoutResult {
  taskId: number
  leftOffset: string   // Offset a sinistra: "0px", "40px", "80px"
  width: string        // Larghezza: "calc(100% - 0px)", "calc(100% - 40px)"
  zIndex: number       // z-index per stacking (layer superiore = zIndex maggiore)
  layer: number        // Layer nel gruppo (0-based, 0 = più in basso)
  totalLayers: number  // Numero totale layer nel gruppo
}

interface TimeTask {
  id: number
  start: number // Hour float (10:30 → 10.5)
  end: number
  task: Task
}

/**
 * Composable per calcolare layout layered (Google Calendar style)
 * Eventi sovrapposti con offset progressivo a sinistra
 */
export function useOverlapLayout() {

  // Configurazione offset
  const LAYER_OFFSET_PX = 40 // Pixel di offset per ogni layer
  const MAX_VISIBLE_LAYERS = 5 // Max layer visibili (dopo, diventa troppo stretto)

  /**
   * Calcola layout layered per task sovrapposti
   * @param tasks Lista task con startDatetime/endDatetime
   * @returns Map<taskId, LayoutResult>
   */
  function calculateLayout(tasks: Task[]): Map<number, LayoutResult> {
    if (!tasks || tasks.length === 0) {
      return new Map()
    }

    // Convert tasks to time-based objects
    const timeTasks: TimeTask[] = tasks
      .filter(task => task.startDatetime && task.endDatetime)
      .map(task => ({
        id: task.id,
        start: getHourFloat(task.startDatetime),
        end: getHourFloat(task.endDatetime),
        task: task
      }))
      .sort((a, b) => {
        // Sort by start time, poi per durata (più lunghi prima)
        if (a.start !== b.start) {
          return a.start - b.start
        }
        return (b.end - b.start) - (a.end - a.start) // Longer tasks first
      })

    // Find overlapping groups
    const groups = findOverlappingGroups(timeTasks)

    // Calculate layers for each group
    const layoutMap = new Map<number, LayoutResult>()

    for (const group of groups) {
      const layers = assignLayers(group)
      const totalLayers = Math.max(...Array.from(layers.values())) + 1

      for (const [taskId, layer] of layers.entries()) {
        const offsetPx = layer * LAYER_OFFSET_PX

        layoutMap.set(taskId, {
          taskId,
          leftOffset: `${offsetPx}px`,
          width: `calc(100% - ${offsetPx}px)`,
          zIndex: layer, // Layer più alti hanno zIndex maggiore
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
   * Assegna layer per ogni task nel gruppo
   *
   * Algoritmo:
   * 1. Task più lunghi vanno nei layer più bassi (layer 0 = più in basso, più largo)
   * 2. Task più corti vanno sopra (layer maggiore, offset maggiore)
   * 3. Task che non si sovrappongono possono condividere lo stesso layer
   *
   * Questo crea effetto "cascata" come Google Calendar
   */
  function assignLayers(group: TimeTask[]): Map<number, number> {
    const layers = new Map<number, number>()
    const layerEndTimes: number[] = []

    // Sort by duration (longer first) per assegnare layer
    const sortedByDuration = [...group].sort((a, b) => {
      const durationA = a.end - a.start
      const durationB = b.end - b.start
      if (durationA !== durationB) {
        return durationB - durationA // Longer first
      }
      return a.start - b.start // Earlier first if same duration
    })

    for (const task of sortedByDuration) {
      // Trova il layer più basso disponibile
      let assignedLayer = 0

      for (let layer = 0; layer < layerEndTimes.length; layer++) {
        if (layerEndTimes[layer] <= task.start) {
          // Layer è libero
          assignedLayer = layer
          break
        }
        assignedLayer = layer + 1
      }

      // Se serve nuovo layer, crealo
      if (assignedLayer >= layerEndTimes.length) {
        layerEndTimes.push(0)
      }

      // Assegna task al layer
      layers.set(task.id, assignedLayer)
      layerEndTimes[assignedLayer] = task.end
    }

    return layers
  }

  /**
   * Calcola numero massimo di layer per il giorno
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
   * Check se il task ha troppi layer (diventa troppo stretto)
   */
  function hasTooManyLayers(layoutResult: LayoutResult): boolean {
    return layoutResult.totalLayers > MAX_VISIBLE_LAYERS
  }

  /**
   * Calcola larghezza effettiva in percentuale
   */
  function getEffectiveWidthPercent(layoutResult: LayoutResult): number {
    const offsetPx = layoutResult.layer * LAYER_OFFSET_PX
    // Assumendo colonna ~200px, approssimazione
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
