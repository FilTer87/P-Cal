import type { Task } from '../types/task'

/**
 * Split tasks into current/future and past based on their end datetime
 * @param tasks - Array of tasks to split
 * @returns Object with 'current' and 'past' task arrays, both sorted by start time
 */
export function splitTasksByTime(tasks: Task[]): { current: Task[]; past: Task[] } {
  const now = new Date()

  const current = tasks
    .filter(task => new Date(task.endDatetime) >= now)
    .sort((a, b) => new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime())

  const past = tasks
    .filter(task => new Date(task.endDatetime) < now)
    .sort((a, b) => new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime())

  return { current, past }
}

/**
 * Sort tasks by start datetime (ascending)
 * @param tasks - Array of tasks to sort
 * @returns Sorted array of tasks
 */
export function sortTasksByStartTime(tasks: Task[]): Task[] {
  return [...tasks].sort((a, b) =>
    new Date(a.startDatetime).getTime() - new Date(b.startDatetime).getTime()
  )
}

/**
 * Group tasks by date (YYYY-MM-DD)
 * @param tasks - Array of tasks to group
 * @returns Object with date keys and task arrays as values
 */
export function groupTasksByDate(tasks: Task[]): Record<string, Task[]> {
  const groups: Record<string, Task[]> = {}

  tasks.forEach(task => {
    const date = new Date(task.startDatetime).toISOString().split('T')[0]
    if (!groups[date]) {
      groups[date] = []
    }
    groups[date].push(task)
  })

  // Sort tasks within each day
  Object.keys(groups).forEach(date => {
    groups[date] = sortTasksByStartTime(groups[date])
  })

  return groups
}

/**
 * Composable for task filtering and sorting utilities
 */
export function useTaskFilters() {
  return {
    splitTasksByTime,
    sortTasksByStartTime,
    groupTasksByDate
  }
}
