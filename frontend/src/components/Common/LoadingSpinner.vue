<template>
  <div 
    :class="[
      'inline-flex items-center justify-center',
      containerClasses[size]
    ]"
    role="status" 
    :aria-label="ariaLabel"
  >
    <!-- Spinner SVG -->
    <svg
      :class="[
        'animate-spin',
        spinnerClasses[size],
        color ? colorClasses[color] : 'text-blue-600 dark:text-blue-400'
      ]"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle
        class="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        stroke-width="4"
      />
      <path
        class="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>

    <!-- Loading Text -->
    <span 
      v-if="label || $slots.default"
      :class="[
        'ml-2 font-medium',
        textClasses[size],
        color ? colorClasses[color] : 'text-gray-700 dark:text-gray-300'
      ]"
    >
      <slot>{{ label }}</slot>
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export type SpinnerSize = 'xs' | 'small' | 'medium' | 'large' | 'xl'
export type SpinnerColor = 'blue' | 'green' | 'red' | 'yellow' | 'purple' | 'gray' | 'white'

interface Props {
  size?: SpinnerSize
  color?: SpinnerColor
  label?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium'
})

// Size configuration
const containerClasses: Record<SpinnerSize, string> = {
  xs: '',
  small: '',
  medium: '',
  large: '',
  xl: ''
}

const spinnerClasses: Record<SpinnerSize, string> = {
  xs: 'h-3 w-3',
  small: 'h-4 w-4',
  medium: 'h-6 w-6',
  large: 'h-8 w-8',
  xl: 'h-12 w-12'
}

const textClasses: Record<SpinnerSize, string> = {
  xs: 'text-xs',
  small: 'text-sm',
  medium: 'text-base',
  large: 'text-lg',
  xl: 'text-xl'
}

const colorClasses: Record<SpinnerColor, string> = {
  blue: 'text-blue-600 dark:text-blue-400',
  green: 'text-green-600 dark:text-green-400',
  red: 'text-red-600 dark:text-red-400',
  yellow: 'text-yellow-600 dark:text-yellow-400',
  purple: 'text-purple-600 dark:text-purple-400',
  gray: 'text-gray-600 dark:text-gray-400',
  white: 'text-white'
}

// Computed properties
const ariaLabel = computed(() => {
  return props.label || 'Caricamento in corso'
})
</script>

<style scoped>
/* Ensure smooth rotation */
.animate-spin {
  animation: spin 1s linear infinite;
}

/* Custom spin animation for better performance */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .animate-spin {
    animation: none;
  }
  
  /* Show a pulsing effect instead */
  .animate-spin {
    animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  svg circle {
    stroke-width: 6;
  }
  
  svg path {
    opacity: 1;
  }
}

/* Focus indicators for accessibility */
.inline-flex:focus {
  outline: 2px solid currentColor;
  outline-offset: 2px;
}
</style>