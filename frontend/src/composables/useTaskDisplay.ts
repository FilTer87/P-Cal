import type { Task } from '../types/task'

// Centralized color mapping for task colors
export const TASK_COLOR_MAP: Record<string, string> = {
  '#3b82f6': 'blue',
  '#3788d8': 'blue', // Default blue
  '#10b981': 'emerald',
  '#ef4444': 'red',
  '#f59e0b': 'amber',
  '#8b5cf6': 'violet',
  '#ec4899': 'pink',
  '#6366f1': 'indigo',
  '#14b8a6': 'teal',
  '#f97316': 'orange',
  '#6b7280': 'gray',
  '#22c55e': 'green',
  '#a855f7': 'purple',
  '#06b6d4': 'cyan',
  '#84cc16': 'lime',
  '#eab308': 'yellow',
  '#f43f5e': 'rose'
}

/**
 * Get Tailwind CSS classes for task display
 * @param task - Task object
 * @param detailed - Whether to use detailed border (4px vs 2px)
 * @returns CSS class string
 */
export function getTaskDisplayClasses(task: Task, detailed = false): string {
  const baseClasses = detailed ? 'border-l-4' : 'border-l-2'

  const isPast = new Date(task.endDatetimeLocal) < new Date()
  const color = task.color || '#3788d8'
  const colorName = TASK_COLOR_MAP[color]

  if (colorName) {
    // For Tailwind colors, use background classes
    const classes = `${baseClasses} bg-${colorName}-50 dark:bg-${colorName}-900/20 hover:bg-${colorName}-100 dark:hover:bg-${colorName}-900/30 task-custom-color`
    return isPast ? `${classes} opacity-25 hover:opacity-75` : classes
  } else {
    // For custom hex colors, use neutral background
    const classes = `${baseClasses} task-custom-color bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600`
    return isPast ? `${classes} opacity-25 hover:opacity-75` : classes
  }
}

/**
 * Get inline styles for task display
 * @param task - Task object
 * @returns Style object with CSS custom properties
 */
export function getTaskDisplayStyle(task: Task): Record<string, string> {
  const color = task.color || '#3788d8'
  const colorName = TASK_COLOR_MAP[color]

  const hexToRgba = (hex: string, alpha: number): string => {
    const r = parseInt(hex.slice(1, 3), 16)
    const g = parseInt(hex.slice(3, 5), 16)
    const b = parseInt(hex.slice(5, 7), 16)
    return `rgba(${r}, ${g}, ${b}, ${alpha})`
  }

  // For ALL colors, use CSS custom properties and inline styles
  const style: Record<string, string> = {
    '--task-color': color,
    borderLeftColor: color,
    borderLeftWidth: '2px',
    borderLeftStyle: 'solid'
  }

  // For custom colors (not in Tailwind map), also set background
  if (!colorName) {
    style['--task-bg-color'] = hexToRgba(color, 0.1)
    style.backgroundColor = hexToRgba(color, 0.1)
  }

  return style
}

/**
 * Composable for task display utilities
 */
export function useTaskDisplay() {
  return {
    TASK_COLOR_MAP,
    getTaskDisplayClasses,
    getTaskDisplayStyle
  }
}
