<template>
  <div
    :class="[
      'calendar-grid',
      gridClasses,
      {
        'gap-px': variant === 'month',
        'gap-0': variant === 'week' || variant === 'day'
      }
    ]"
    :style="gridStyles"
    role="grid"
    :aria-label="gridAriaLabel"
  >
    <!-- Grid headers (days of week for month/week view, hours for day view) -->
    <div
      v-if="showHeaders"
      :class="[
        'contents',
        { 'sr-only': hideHeaders }
      ]"
      role="row"
    >
      <div
        v-for="(header, index) in headers"
        :key="header.key || index"
        :class="[
          'grid-header',
          getHeaderClasses(header, index),
          {
            'sticky top-0 z-10': stickyHeaders,
            'border-b': variant === 'month' || variant === 'week'
          }
        ]"
        role="columnheader"
        :aria-label="header.ariaLabel || header.label"
      >
        <div class="p-2 text-center">
          <div 
            v-if="header.label"
            :class="[
              'text-sm font-medium',
              getHeaderTextClasses(header)
            ]"
          >
            {{ header.label }}
          </div>
          <div
            v-if="header.subtitle"
            :class="[
              'text-xs mt-0.5',
              getHeaderSubtitleClasses(header)
            ]"
          >
            {{ header.subtitle }}
          </div>
        </div>
      </div>
    </div>

    <!-- Grid content -->
    <slot 
      name="default"
      :headers="headers"
      :variant="variant"
      :compact="compact"
    />

    <!-- Loading overlay -->
    <div
      v-if="loading"
      :class="[
        'absolute inset-0 bg-white/50 dark:bg-gray-900/50 flex items-center justify-center z-20',
        'backdrop-blur-sm'
      ]"
    >
      <div class="flex items-center space-x-2 text-gray-600 dark:text-gray-400">
        <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
        <span>Caricamento...</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTheme } from '../../composables/useTheme'

// Types
interface GridHeader {
  key?: string | number
  label: string
  subtitle?: string
  ariaLabel?: string
  isToday?: boolean
  isWeekend?: boolean
  isCurrentPeriod?: boolean
  data?: any
}

interface Props {
  variant: 'month' | 'week' | 'day' | 'custom'
  headers?: GridHeader[]
  showHeaders?: boolean
  hideHeaders?: boolean
  stickyHeaders?: boolean
  columns?: number
  rows?: number
  minHeight?: string
  maxHeight?: string
  compact?: boolean
  responsive?: boolean
  loading?: boolean
  borderStyle?: 'solid' | 'dashed' | 'dotted' | 'none'
  gap?: 'none' | 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  showHeaders: true,
  hideHeaders: false,
  stickyHeaders: true,
  columns: 7,
  rows: 6,
  minHeight: '400px',
  maxHeight: 'none',
  compact: false,
  responsive: true,
  loading: false,
  borderStyle: 'solid',
  gap: 'sm'
})

// Composables
const { 
  cardClass, 
  textClass,
  calendarHeaderClass,
  getTransitionClasses
} = useTheme()

// Computed properties
const gridClasses = computed(() => {
  const baseClasses = [
    'relative',
    cardClass.value,
    getTransitionClasses()
  ]

  // Grid layout classes
  switch (props.variant) {
    case 'month':
      baseClasses.push('grid grid-cols-7')
      if (props.responsive) {
        baseClasses.push('sm:grid-cols-7')
      }
      break
    case 'week':
      baseClasses.push('grid grid-cols-8') // 7 days + time column
      if (props.responsive) {
        baseClasses.push('sm:grid-cols-8 grid-cols-1')
      }
      break
    case 'day':
      baseClasses.push('grid grid-cols-2') // Time + content
      break
    case 'custom':
      baseClasses.push(`grid grid-cols-${props.columns}`)
      break
  }

  // Border styles
  if (props.borderStyle !== 'none') {
    baseClasses.push(`border border-${props.borderStyle}`)
  }

  // Gap classes
  switch (props.gap) {
    case 'none':
      baseClasses.push('gap-0')
      break
    case 'sm':
      baseClasses.push('gap-px')
      break
    case 'md':
      baseClasses.push('gap-1')
      break
    case 'lg':
      baseClasses.push('gap-2')
      break
  }

  // Responsive classes
  if (props.responsive) {
    baseClasses.push('overflow-hidden')
    if (props.variant === 'month') {
      baseClasses.push('md:grid-cols-7 grid-cols-1')
    }
  }

  return baseClasses.join(' ')
})

const gridStyles = computed(() => {
  const styles: Record<string, string> = {}

  if (props.minHeight && props.minHeight !== 'none') {
    styles.minHeight = props.minHeight
  }

  if (props.maxHeight && props.maxHeight !== 'none') {
    styles.maxHeight = props.maxHeight
  }

  // Custom grid template for different variants
  if (props.variant === 'week' || props.variant === 'day') {
    styles.gridTemplateRows = props.showHeaders ? 'auto 1fr' : '1fr'
  }

  if (props.variant === 'month') {
    const rowHeight = props.compact ? 'auto' : 'minmax(120px, 1fr)'
    styles.gridTemplateRows = props.showHeaders 
      ? `auto repeat(${props.rows}, ${rowHeight})` 
      : `repeat(${props.rows}, ${rowHeight})`
  }

  return styles
})

const headers = computed(() => {
  if (!props.headers) {
    return generateDefaultHeaders()
  }
  return props.headers
})

const gridAriaLabel = computed(() => {
  switch (props.variant) {
    case 'month':
      return 'Calendario mensile'
    case 'week':
      return 'Calendario settimanale'
    case 'day':
      return 'Calendario giornaliero'
    default:
      return 'Griglia del calendario'
  }
})

// Methods
const generateDefaultHeaders = (): GridHeader[] => {
  switch (props.variant) {
    case 'month':
    case 'week':
      return [
        { label: 'Lun', key: 1, isWeekend: false },
        { label: 'Mar', key: 2, isWeekend: false },
        { label: 'Mer', key: 3, isWeekend: false },
        { label: 'Gio', key: 4, isWeekend: false },
        { label: 'Ven', key: 5, isWeekend: false },
        { label: 'Sab', key: 6, isWeekend: true },
        { label: 'Dom', key: 0, isWeekend: true }
      ]
    case 'day':
      return [
        { label: 'Ora', key: 'time' },
        { label: 'Attività', key: 'tasks' }
      ]
    default:
      return []
  }
}

const getHeaderClasses = (header: GridHeader, index: number) => {
  const classes = [
    calendarHeaderClass.value,
    'border-gray-200 dark:border-gray-700'
  ]

  if (header.isToday) {
    classes.push('bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300')
  }

  if (header.isWeekend && props.variant !== 'day') {
    classes.push('bg-gray-50 dark:bg-gray-800/50')
  }

  if (header.isCurrentPeriod) {
    classes.push('ring-2 ring-blue-500 ring-opacity-50')
  }

  // First column (time column for week/day view)
  if (index === 0 && (props.variant === 'week' || props.variant === 'day')) {
    classes.push('bg-gray-50 dark:bg-gray-800/50')
  }

  return classes.join(' ')
}

const getHeaderTextClasses = (header: GridHeader) => {
  const classes = [textClass.value]

  if (header.isToday) {
    classes.push('font-semibold text-blue-700 dark:text-blue-300')
  }

  if (header.isWeekend && !header.isToday) {
    classes.push('text-gray-500 dark:text-gray-400')
  }

  return classes.join(' ')
}

const getHeaderSubtitleClasses = (header: GridHeader) => {
  const classes = ['text-gray-500 dark:text-gray-400']

  if (header.isToday) {
    classes.push('text-blue-600 dark:text-blue-400')
  }

  return classes.join(' ')
}

// Provide methods for child components
defineExpose({
  getHeaderClasses,
  getHeaderTextClasses,
  getHeaderSubtitleClasses
})
</script>

<style scoped>
.calendar-grid {
  /* Custom CSS properties for theming */
  --grid-border-color: theme('colors.gray.200');
  --grid-header-bg: theme('colors.gray.50');
}

@media (prefers-color-scheme: dark) {
  .calendar-grid {
    --grid-border-color: theme('colors.gray.700');
    --grid-header-bg: theme('colors.gray.800');
  }
}

/* Responsive grid adjustments */
@media (max-width: 768px) {
  .calendar-grid.grid-cols-7 {
    grid-template-columns: repeat(7, minmax(0, 1fr));
    font-size: 0.875rem;
  }
}

@media (max-width: 640px) {
  .calendar-grid.grid-cols-7 {
    grid-template-columns: repeat(7, minmax(40px, 1fr));
    font-size: 0.75rem;
  }
}

/* Grid cell hover effects */
.calendar-grid :deep(.grid-cell:hover) {
  background-color: var(--tw-color-gray-50);
  transition: background-color 0.15s ease;
}

.dark .calendar-grid :deep(.grid-cell:hover) {
  background-color: var(--tw-color-gray-800);
}

/* Smooth transitions for loading states */
.calendar-grid {
  transition: opacity 0.2s ease;
}

.calendar-grid.loading {
  opacity: 0.7;
}

.overflow-fix {
  float: left;
  width: 100%;
  overflow-y: scroll;
  height: 0;
}

/* Print styles */
@media print {
  .calendar-grid {
    break-inside: avoid;
    page-break-inside: avoid;
  }
  
  .calendar-grid .grid-header {
    background-color: #f9fafb !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .calendar-grid {
    border: 2px solid black;
  }
  
  .calendar-grid .grid-header {
    border: 1px solid black;
    background-color: #f0f0f0;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .calendar-grid,
  .calendar-grid * {
    transition: none !important;
    animation: none !important;
  }
}
</style>