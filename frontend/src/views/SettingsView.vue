<template>
  <div class="settings-view min-h-screen bg-gray-50 dark:bg-gray-900">
    <div class="max-w-4xl mx-auto py-8 px-4 sm:px-6 lg:px-8">

      <!-- Back to Calendar -->
        <div class="absolute fixed right-4">
          <router-link 
            to="/"
            class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 transition-colors"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Torna al Calendario
          </router-link>
        </div>

      <!-- Page Header -->
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
          Impostazioni
        </h1>
        <p class="text-gray-600 dark:text-gray-400 mt-2">
          Gestisci le tue preferenze e configurazioni dell'applicazione
        </p>
      </div>

      <!-- Settings Sections -->
      <div class="space-y-6">


        <!-- Calendar Settings -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Calendario
          </h2>
          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <div>
                <h3 class="text-sm font-medium text-gray-900 dark:text-white">
                  Inizio settimana
                </h3>
                <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  Primo giorno della settimana nel calendario (solo per questa sessione)
                </p>
              </div>
              <select
                :value="settings.weekStartDay"
                @change="changeWeekStartDay"
                class="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="0">Domenica</option>
                <option value="1">Lunedì</option>
              </select>
            </div>
            <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-3">
              <p class="text-sm text-blue-800 dark:text-blue-200">
                <strong>Nota:</strong> Per salvare permanentemente le tue preferenze di formato orario e vista predefinita,
                vai alla sezione <router-link to="/profile" class="underline hover:text-blue-600">Preferenze del Profilo</router-link>.
              </p>
            </div>
          </div>
        </div>

        <!-- Privacy Settings - NO DATA ACQUIRED -->
        <!-- <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Privacy
          </h2>
          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <div>
                <h3 class="text-sm font-medium text-gray-900 dark:text-white">
                  Dati di utilizzo
                </h3>
                <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  Condividi dati anonimi per migliorare l'applicazione
                </p>
              </div>
              <label class="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" class="sr-only peer" checked>
                <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600"></div>
              </label>
            </div>
          </div>
        </div> -->

        <!-- Back to Calendar -->
        <div class="pt-4">
          <router-link 
            to="/"
            class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 transition-colors"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Torna al Calendario
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useCustomToast } from '@/composables/useCustomToast'
import { useSettingsStore } from '@/stores/settings'

// Composables
const { showSuccess } = useCustomToast()
const settings = useSettingsStore()

// Methods
const changeWeekStartDay = (event: Event) => {
  const value = parseInt((event.target as HTMLSelectElement).value)
  settings.updateWeekStartDay(value as 0 | 1)
  showSuccess(`Inizio settimana cambiato in ${value === 0 ? 'Domenica' : 'Lunedì'} (solo per questa sessione)`)
}

// Initialize settings on mount
onMounted(() => {
  settings.loadSettings()
})
</script>

<style scoped>
/* Toggle switch styles */
.peer:checked + div {
  @apply bg-blue-600;
}

.peer:checked + div:after {
  @apply translate-x-full;
}

/* Focus ring for accessibility */
.peer:focus + div {
  @apply ring-2 ring-blue-500 ring-offset-2 dark:ring-offset-gray-800;
}

/* Custom select styling */
select:focus {
  @apply ring-2 ring-blue-500 ring-offset-2 dark:ring-offset-gray-800;
}

/* Smooth transitions */
* {
  @apply transition-colors duration-200;
}
</style>