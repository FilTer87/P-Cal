<template>
  <div class="space-y-8">
    <!-- Export Data -->
    <div class="border border-gray-200 dark:border-gray-700 rounded-lg p-6">
      <div class="flex items-center justify-between">
        <div>
          <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
            Esporta Dati
          </h3>
          <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
            Scarica una copia di tutti i tuoi dati (GDPR compliance)
          </p>
        </div>
        <button
          @click="handleExportData"
          :disabled="isLoading"
          class="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
        >
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          Esporta Dati
        </button>
      </div>
    </div>

    <!-- Delete Account -->
    <div class="border border-red-200 dark:border-red-800 rounded-lg p-6 bg-red-50 dark:bg-red-900/20">
      <div class="flex items-center justify-between">
        <div>
          <h3 class="text-lg font-semibold text-red-900 dark:text-red-400">
            Elimina Account
          </h3>
          <p class="text-sm text-red-700 dark:text-red-300 mt-1">
            Elimina permanentemente il tuo account e tutti i dati associati. Questa azione non può essere annullata.
          </p>
        </div>
        <button
          @click="openDeleteModal"
          :disabled="isLoading"
          class="inline-flex items-center px-4 py-2 border border-red-300 dark:border-red-600 rounded-md shadow-sm text-sm font-medium text-red-700 dark:text-red-300 bg-white dark:bg-red-900/50 hover:bg-red-50 dark:hover:bg-red-900/70 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50"
        >
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          Elimina Account
        </button>
      </div>
    </div>

    <!-- Delete Account Modal -->
    <Modal
      v-model="showDeleteModal"
      title="Conferma Eliminazione Account"
      :persistent="true"
    >
      <div class="space-y-4">
        <div class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700 rounded-lg p-4">
          <div class="flex">
            <svg class="h-5 w-5 text-red-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
            <div class="ml-3">
              <h3 class="text-sm font-medium text-red-800 dark:text-red-200">
                Attenzione: Questa azione è irreversibile
              </h3>
              <div class="mt-2 text-sm text-red-700 dark:text-red-300">
                <ul class="list-disc pl-5 space-y-1">
                  <li>Tutti i tuoi dati verranno eliminati permanentemente</li>
                  <li>Le tue attività, promemoria e impostazioni andranno perse</li>
                  <li>Non sarà possibile recuperare l'account una volta eliminato</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <p class="text-sm text-gray-600 dark:text-gray-400">
          Per confermare l'eliminazione, inserisci la tua password attuale:
        </p>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Password Attuale
          </label>
          <input
            v-model="deletePassword"
            type="password"
            required
            :disabled="isDeleting"
            class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
            placeholder="Inserisci la tua password"
          />
          <p v-if="deleteError" class="mt-1 text-sm text-red-600 dark:text-red-400">
            {{ deleteError }}
          </p>
        </div>

        <div class="flex justify-end space-x-4 pt-4">
          <button
            @click="cancelDelete"
            :disabled="isDeleting"
            class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            Annulla
          </button>
          <button
            @click="confirmDelete"
            :disabled="isDeleting || !deletePassword.trim()"
            class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50"
          >
            <LoadingSpinner v-if="isDeleting" class="w-4 h-4 mr-2" />
            {{ isDeleting ? 'Eliminando...' : 'Elimina Account' }}
          </button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Modal from '@/components/Common/Modal.vue'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'

// Props
interface Props {
  isLoading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isLoading: false
})

// Emits
const emit = defineEmits<{
  exportData: []
  deleteAccount: [password: string]
}>()

// Local state
const showDeleteModal = ref(false)
const deletePassword = ref('')
const deleteError = ref('')
const isDeleting = ref(false)

// Methods
const handleExportData = () => {
  emit('exportData')
}

const openDeleteModal = () => {
  showDeleteModal.value = true
  deletePassword.value = ''
  deleteError.value = ''
}

const cancelDelete = () => {
  showDeleteModal.value = false
  deletePassword.value = ''
  deleteError.value = ''
}

const confirmDelete = async () => {
  if (!deletePassword.value.trim()) return

  isDeleting.value = true
  deleteError.value = ''

  try {
    emit('deleteAccount', deletePassword.value)
  } catch (error) {
    // Error handling will be done in parent
    isDeleting.value = false
  }
}

// Expose methods for parent to control state
defineExpose({
  setDeleteError: (error: string) => {
    deleteError.value = error
    isDeleting.value = false
  },
  closeModal: () => {
    showDeleteModal.value = false
    deletePassword.value = ''
    deleteError.value = ''
    isDeleting.value = false
  }
})
</script>
