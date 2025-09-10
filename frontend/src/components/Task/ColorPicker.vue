<template>
  <div class="color-picker">
    <!-- Label -->
    <label 
      v-if="label"
      :for="`color-picker-${id}`"
      :class="[
        'block text-sm font-medium mb-2',
        textClass
      ]"
    >
      {{ label }}
      <span v-if="required" class="text-red-500 ml-1">*</span>
    </label>

    <!-- Selected color preview -->
    <div class="flex items-center space-x-3 mb-3">
      <div
        class="w-8 h-8 rounded-lg border-2 border-gray-300 dark:border-gray-600 shadow-sm"
        :style="{ backgroundColor: selectedColor }"
        :title="`Colore selezionato: ${getColorName(selectedColor)}`"
      ></div>
      <span :class="['text-sm', textClass]">
        {{ getColorName(selectedColor) }}
      </span>
    </div>

    <!-- Predefined colors grid -->
    <div class="grid grid-cols-5 gap-2 mb-4">
      <button
        v-for="color in CALENDAR_COLORS"
        :key="color.value"
        type="button"
        @click="selectColor(color.value)"
        :class="[
          'relative w-10 h-10 rounded-lg border-2 transition-all hover:scale-105 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
          selectedColor === color.value
            ? 'border-gray-800 dark:border-gray-200 ring-2 ring-blue-500'
            : 'border-gray-300 dark:border-gray-600 hover:border-gray-400'
        ]"
        :style="{ backgroundColor: color.value }"
        :title="color.name"
        :aria-label="`Seleziona colore ${color.name}`"
      >
        <!-- Check mark for selected color -->
        <CheckIcon
          v-if="selectedColor === color.value"
          class="absolute inset-0 m-auto w-5 h-5 text-white drop-shadow-md"
        />
      </button>
    </div>

    <!-- Custom color section -->
    <div v-if="allowCustom" class="border-t pt-4 dark:border-gray-700">
      <h4 :class="['text-sm font-medium mb-2', textClass]">
        Colore personalizzato
      </h4>
      
      <div class="flex items-center space-x-2">
        <input
          :id="`color-picker-${id}`"
          type="color"
          :value="selectedColor"
          @input="handleCustomColorChange"
          class="w-10 h-10 border-2 border-gray-300 dark:border-gray-600 rounded-lg cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500"
          :aria-label="'Selettore colore personalizzato'"
        />
        
        <input
          type="text"
          :value="selectedColor"
          @input="handleHexInput"
          @blur="validateHexInput"
          placeholder="#000000"
          maxlength="7"
          :class="[
            'flex-1 px-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent',
            inputClass,
            hasError ? 'border-red-500 focus:ring-red-500' : ''
          ]"
          :aria-label="'Valore esadecimale del colore'"
        />
      </div>

      <!-- Custom color validation error -->
      <p v-if="customColorError" class="text-sm text-red-600 dark:text-red-400 mt-1">
        {{ customColorError }}
      </p>
    </div>

    <!-- Recent colors -->
    <div v-if="showRecentColors && recentColors.length > 0" class="border-t pt-4 mt-4 dark:border-gray-700">
      <h4 :class="['text-sm font-medium mb-2', textClass]">
        Colori recenti
      </h4>
      
      <div class="flex flex-wrap gap-2">
        <button
          v-for="color in recentColors"
          :key="`recent-${color}`"
          type="button"
          @click="selectColor(color)"
          :class="[
            'relative w-8 h-8 rounded-lg border-2 transition-all hover:scale-105 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1',
            selectedColor === color
              ? 'border-gray-800 dark:border-gray-200 ring-1 ring-blue-500'
              : 'border-gray-300 dark:border-gray-600 hover:border-gray-400'
          ]"
          :style="{ backgroundColor: color }"
          :title="`Colore recente: ${color}`"
          :aria-label="`Seleziona colore recente ${color}`"
        >
          <CheckIcon
            v-if="selectedColor === color"
            class="absolute inset-0 m-auto w-4 h-4 text-white drop-shadow-md"
          />
        </button>
      </div>
    </div>

    <!-- Error message -->
    <p v-if="error" class="text-sm text-red-600 dark:text-red-400 mt-2">
      {{ error }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useTheme } from '../../composables/useTheme'
import { CALENDAR_COLORS, VALIDATION_MESSAGES } from '../../types/task'
import type { CalendarColor } from '../../types/task'

// Icons
import { CheckIcon } from '@heroicons/vue/24/outline'

// Props
interface Props {
  modelValue: string
  label?: string
  required?: boolean
  allowCustom?: boolean
  showRecentColors?: boolean
  error?: string
  id?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '#3b82f6',
  required: false,
  allowCustom: true,
  showRecentColors: true,
  id: () => `color-picker-${Date.now()}`
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: string]
  'color-selected': [color: string, colorName: string]
}>()

// Composables
const { textClass, inputClass } = useTheme()

// State
const selectedColor = ref(props.modelValue)
const customColorError = ref('')
const recentColors = ref<string[]>([])

// Computed
const hasError = computed(() => !!props.error || !!customColorError.value)

// Watch for external changes
watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue !== selectedColor.value) {
      selectedColor.value = newValue
      validateColor(newValue)
    }
  }
)

// Watch for internal changes
watch(selectedColor, (newValue) => {
  emit('update:modelValue', newValue)
})

// Methods
const selectColor = (color: string) => {
  selectedColor.value = color
  customColorError.value = ''
  
  // Add to recent colors
  addToRecentColors(color)
  
  // Emit selection event
  const colorName = getColorName(color)
  emit('color-selected', color, colorName)
}

const getColorName = (colorValue: string): string => {
  const predefinedColor = CALENDAR_COLORS.find(c => c.value.toLowerCase() === colorValue.toLowerCase())
  return predefinedColor ? predefinedColor.name : colorValue.toUpperCase()
}

const handleCustomColorChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const color = target.value
  selectColor(color)
}

const handleHexInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  let value = target.value.trim()
  
  // Auto-add # if not present
  if (value && !value.startsWith('#')) {
    value = '#' + value
    target.value = value
  }
  
  selectedColor.value = value
}

const validateHexInput = () => {
  const color = selectedColor.value
  if (!validateColor(color)) {
    customColorError.value = VALIDATION_MESSAGES.invalidColor
    return false
  }
  
  customColorError.value = ''
  addToRecentColors(color)
  return true
}

const validateColor = (color: string): boolean => {
  // Check if it's a valid hex color
  const hexColorRegex = /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/
  return hexColorRegex.test(color)
}

const addToRecentColors = (color: string) => {
  if (!color || !validateColor(color)) return
  
  // Remove if already exists
  const index = recentColors.value.indexOf(color)
  if (index > -1) {
    recentColors.value.splice(index, 1)
  }
  
  // Add to beginning
  recentColors.value.unshift(color)
  
  // Keep only last 10 colors
  if (recentColors.value.length > 10) {
    recentColors.value = recentColors.value.slice(0, 10)
  }
  
  // Save to localStorage
  if (props.showRecentColors) {
    try {
      localStorage.setItem('privatecal-recent-colors', JSON.stringify(recentColors.value))
    } catch (e) {
      console.warn('Could not save recent colors to localStorage:', e)
    }
  }
}

const loadRecentColors = () => {
  if (!props.showRecentColors) return
  
  try {
    const saved = localStorage.getItem('privatecal-recent-colors')
    if (saved) {
      const colors = JSON.parse(saved)
      if (Array.isArray(colors)) {
        recentColors.value = colors.filter(color => 
          typeof color === 'string' && validateColor(color)
        ).slice(0, 10)
      }
    }
  } catch (e) {
    console.warn('Could not load recent colors from localStorage:', e)
    recentColors.value = []
  }
}

const resetToDefault = () => {
  selectColor(CALENDAR_COLORS[0].value)
}

// Lifecycle
onMounted(() => {
  loadRecentColors()
  validateColor(selectedColor.value)
})

// Expose methods for parent components
defineExpose({
  resetToDefault,
  validateColor,
  selectedColor: computed(() => selectedColor.value)
})
</script>

<style scoped>
.color-picker {
  /* Custom styles if needed */
}

/* Custom color input styling for WebKit browsers */
.color-picker input[type="color"]::-webkit-color-swatch-wrapper {
  padding: 0;
  border-radius: 0.5rem;
}

.color-picker input[type="color"]::-webkit-color-swatch {
  border: none;
  border-radius: 0.375rem;
}

/* Firefox color input styling */
.color-picker input[type="color"]::-moz-color-swatch {
  border: none;
  border-radius: 0.375rem;
}

/* Hover effects */
.color-picker button:hover {
  transform: scale(1.05);
}

.color-picker button:active {
  transform: scale(0.95);
}

/* Focus ring for accessibility */
.color-picker button:focus {
  outline: none;
}

/* Animation for color selection */
.color-picker button {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>