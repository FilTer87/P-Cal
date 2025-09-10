# Task Management Components

This directory contains a comprehensive set of Vue 3 components for task management in P-Cal. All components are built with TypeScript, Tailwind CSS, and support both light and dark themes.

## Components Overview

### Core Components

#### 1. TaskModal.vue
Full-featured modal for creating and editing tasks.

**Features:**
- Full-screen on mobile, centered modal on desktop
- Smooth animations and transitions
- Keyboard navigation support (Esc to close, Ctrl+S to save)
- Delete confirmation dialog
- Form validation with Italian error messages
- Responsive design

**Usage:**
```vue
<TaskModal
  v-model:is-open="showModal"
  :task="taskToEdit"
  :initial-date="'2025-01-15'"
  :initial-time="'09:00'"
  @task-created="handleTaskCreated"
  @task-updated="handleTaskUpdated"
  @task-deleted="handleTaskDeleted"
/>
```

#### 2. TaskForm.vue
Reusable form component with comprehensive validation.

**Features:**
- Real-time form validation
- Date/time conflict detection
- Color picker integration
- Reminder management
- All-day task support
- Location field
- Priority selection
- Italian validation messages

**Usage:**
```vue
<TaskForm
  :task="existingTask"
  :initial-date="selectedDate"
  @submit="handleFormSubmit"
  @success="handleSuccess"
  @error="handleError"
/>
```

#### 3. TaskList.vue
Advanced list component with filtering and search.

**Features:**
- Virtual scrolling for performance
- Grouped display by date
- Advanced filtering (status, priority, date range)
- Search functionality
- Bulk selection and actions
- Sorting options
- Empty state handling

**Usage:**
```vue
<TaskList
  :enable-selection="true"
  :group-by-date="true"
  :show-completed="true"
  @task-click="handleTaskClick"
  @task-edit="handleTaskEdit"
  @selection-change="handleSelectionChange"
/>
```

#### 4. TaskCard.vue
Enhanced task display component for calendar views.

**Features:**
- Multiple display variants (month, week, day, agenda)
- Color-coded styling
- Priority indicators
- Drag and drop support
- Hover actions
- Status indicators (overdue, due soon)
- Location and reminder display
- All-day task support

**Usage:**
```vue
<TaskCard
  :task="task"
  variant="month"
  :show-actions="true"
  :draggable="true"
  @click="handleTaskClick"
  @edit-task="handleEdit"
/>
```

#### 5. TaskQuickAdd.vue
Rapid task creation component.

**Features:**
- Collapsible inline form
- Smart suggestions based on title
- Quick time and priority selection
- Color picker integration
- Keyboard shortcuts (Ctrl+N to open, Ctrl+Enter to save)
- Auto-save functionality
- Context-aware defaults

**Usage:**
```vue
<TaskQuickAdd
  :initial-date="selectedDate"
  :show-suggestions="true"
  @task-created="handleTaskCreated"
  @expand-full-form="openFullModal"
/>
```

#### 6. TaskStats.vue
Statistics and analytics component.

**Features:**
- Visual progress indicators
- Priority distribution charts
- Timeline statistics (today, this week)
- Quick action buttons
- Completion percentage tracking
- Empty state handling

**Usage:**
```vue
<TaskStats
  :show-quick-actions="true"
  @show-all-tasks="switchToListView"
  @create-task="openTaskModal"
/>
```

### Supporting Components

#### 7. ColorPicker.vue
Advanced color selection component.

**Features:**
- Predefined calendar colors
- Custom color picker
- Recent colors memory
- Color validation
- Accessibility support

#### 8. DateTimePicker.vue
Comprehensive date and time selection.

**Features:**
- Calendar popup for date selection
- Time picker with 15-minute intervals
- All-day toggle
- Quick time presets
- Timezone display
- Italian locale support

#### 9. TaskListItem.vue
Individual task item component for lists.

**Features:**
- Compact and detailed layouts
- Selection checkboxes
- Quick actions
- Status indicators
- Drag handle for reordering

## Type Definitions

### Enhanced Task Interface
```typescript
interface Task {
  id: number
  title: string
  description?: string
  completed: boolean
  priority: TaskPriority
  dueDate?: string
  startDate?: string      // New
  endDate?: string        // New
  location?: string       // New
  color?: string          // New
  isAllDay?: boolean      // New
  createdAt: string
  updatedAt: string
  userId: number
  reminders: Reminder[]
}
```

### Form Data Interface
```typescript
interface TaskFormData {
  title: string
  description: string
  priority: TaskPriority
  dueDate: string
  dueTime: string
  startDate: string       // New
  startTime: string       // New
  endDate: string         // New
  endTime: string         // New
  location: string        // New
  color: string           // New
  isAllDay: boolean       // New
  reminders: ReminderFormData[]
}
```

## Color System

### Predefined Calendar Colors
The components use a consistent color system with 10 predefined colors:
- Blu (#3b82f6)
- Verde (#10b981)
- Rosso (#ef4444)
- Giallo (#f59e0b)
- Viola (#8b5cf6)
- Rosa (#ec4899)
- Indaco (#6366f1)
- Teal (#14b8a6)
- Arancione (#f97316)
- Grigio (#6b7280)

Each color includes light/dark theme variants and accessibility-compliant contrast ratios.

## Validation System

### Italian Error Messages
All components include comprehensive validation with Italian error messages:

```typescript
const VALIDATION_MESSAGES = {
  required: 'Questo campo è obbligatorio',
  minLength: (min: number) => `Minimo ${min} caratteri richiesti`,
  maxLength: (max: number) => `Massimo ${max} caratteri consentiti`,
  invalidDate: 'Data non valida',
  invalidTime: 'Ora non valida (formato HH:MM)',
  invalidColor: 'Colore non valido',
  endBeforeStart: 'La data di fine deve essere successiva alla data di inizio',
  pastDate: 'La data non può essere nel passato',
  conflict: 'Conflitto con un altro evento'
}
```

### Validation Rules
- **Title**: Required, 1-100 characters
- **Description**: Optional, max 500 characters
- **Start Date**: Required for new tasks, cannot be in past
- **End Date**: Must be after start date
- **Time**: Valid HH:MM format
- **Color**: Valid hex color
- **Location**: Optional, max 200 characters

## Integration Example

See `TaskManagement.example.vue` for a complete integration example showing how all components work together.

## Keyboard Shortcuts

### Global Shortcuts
- `Ctrl+N`: Open quick add form
- `Ctrl+T`: Go to today (in calendar context)

### Modal Shortcuts
- `Escape`: Close modal
- `Ctrl+S`: Save form
- `Ctrl+Enter`: Save form (in quick add)

### List Shortcuts
- `Ctrl+A`: Select all visible tasks
- `Delete`: Delete selected tasks (with confirmation)

## Accessibility Features

All components include comprehensive accessibility support:
- ARIA labels and descriptions
- Keyboard navigation
- Focus management
- Screen reader compatibility
- High contrast mode support
- Reduced motion support

## Responsive Design

Components are designed mobile-first with breakpoint-specific optimizations:
- **Mobile**: Full-screen modals, stacked layouts, touch-optimized targets
- **Tablet**: Intermediate layouts, hover states
- **Desktop**: Multi-column layouts, advanced interactions

## Theme Support

Full dark/light theme support using Tailwind CSS:
- Automatic system preference detection
- Manual theme switching
- Consistent color variables
- Theme-aware animations

## Performance Optimizations

- Virtual scrolling for large task lists
- Debounced search input
- Optimistic updates
- Component lazy loading
- Memory-efficient state management

## Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## Dependencies

- Vue 3.4+
- TypeScript 5.0+
- Tailwind CSS 3.4+
- @headlessui/vue 1.7+
- @heroicons/vue 2.0+
- date-fns 3.0+
- Pinia 2.1+

## Usage with Composables

All components integrate seamlessly with the existing composable system:

```typescript
import { useTasks } from '@/composables/useTasks'
import { useCalendar } from '@/composables/useCalendar'
import { useTheme } from '@/composables/useTheme'

// In your component
const { createTask, updateTask, deleteTask } = useTasks()
const { selectedDate, currentView } = useCalendar()
const { textClass, cardClass } = useTheme()
```

## Testing

Components include comprehensive test coverage:
- Unit tests for all public methods
- Integration tests for form workflows
- E2E tests for user interactions
- Accessibility tests
- Visual regression tests

## Contributing

When adding new features or modifying components:
1. Maintain TypeScript strict mode compliance
2. Follow existing naming conventions
3. Add appropriate ARIA attributes
4. Include Italian translations
5. Test with both light and dark themes
6. Ensure mobile responsiveness
7. Add comprehensive JSDoc comments