<template>
  <div id="app" :class="{ 'dark': isDarkMode }" class="min-h-screen transition-colors duration-200">
    <div class="min-h-screen bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100">
      <router-view />
    </div>
    
    <!-- Custom Toast Container -->
    <ToastContainer />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useTheme } from './composables/useTheme'
import { useCustomToast } from './composables/useCustomToast'
import ToastContainer from './components/Common/ToastContainer.vue'

const { isDarkMode, initializeTheme } = useTheme()
const { showError: showCustomError } = useCustomToast()

let apiErrorHandler: ((event: any) => void) | null = null

onMounted(() => {
  initializeTheme()
  
  // Listen for API errors from the service
  apiErrorHandler = (event: any) => {
    showCustomError(event.detail.message)
  }
  
  window.addEventListener('api-error', apiErrorHandler)
})

onUnmounted(() => {
  if (apiErrorHandler) {
    window.removeEventListener('api-error', apiErrorHandler)
  }
})
</script>

<style>
/* Custom scrollbar styles */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  @apply bg-gray-100 dark:bg-gray-800;
}

::-webkit-scrollbar-thumb {
  @apply bg-gray-300 dark:bg-gray-600 rounded-full;
}

::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400 dark:bg-gray-500;
}

/* Smooth transitions for theme changes */
* {
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

/* Focus styles */
.focus-visible {
  @apply outline-2 outline-offset-2 outline-blue-500 dark:outline-blue-400;
}
</style>