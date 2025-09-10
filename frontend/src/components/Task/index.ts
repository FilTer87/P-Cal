// Task Management Components
export { default as TaskModal } from './TaskModal.vue'
export { default as TaskForm } from './TaskForm.vue'
export { default as TaskCard } from '../Calendar/TaskCard.vue' // Re-export the enhanced TaskCard
export { default as TaskList } from './TaskList.vue'
export { default as TaskListItem } from './TaskListItem.vue'
export { default as TaskQuickAdd } from './TaskQuickAdd.vue'
export { default as TaskStats } from './TaskStats.vue'

// Supporting Components
export { default as ColorPicker } from './ColorPicker.vue'
export { default as DateTimePicker } from './DateTimePicker.vue'

// Types and Constants
export {
  Task,
  TaskFormData,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskPriority,
  TaskFilters,
  TaskStats as TaskStatsType,
  CalendarColor,
  TaskSortOption,
  TaskViewMode,
  TASK_PRIORITY_CONFIG,
  CALENDAR_COLORS,
  TASK_SORT_OPTIONS,
  VALIDATION_MESSAGES
} from '../../types/task'