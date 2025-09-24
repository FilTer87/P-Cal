<template>
  <Modal v-model="isOpen" title="Configura Autenticazione a Due Fattori" :persistent="true" size="large">
    <div class="space-y-6">
      <!-- Step 1: Setup Instructions -->
      <div v-if="step === 1" class="space-y-4">
        <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 rounded-lg p-4">
          <div class="flex">
            <svg class="h-5 w-5 text-blue-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
            </svg>
            <div class="ml-3">
              <h3 class="text-sm font-medium text-blue-800 dark:text-blue-200">
                Come funziona l'autenticazione a due fattori
              </h3>
              <div class="mt-2 text-sm text-blue-700 dark:text-blue-300">
                <ul class="list-disc pl-5 space-y-1">
                  <li>Scarica un'app di autenticazione (Google Authenticator, Authy, etc.)</li>
                  <li>Scansiona il QR code con l'app</li>
                  <li>Inserisci il codice a 6 cifre generato dall'app</li>
                  <li>Ad ogni login ti verrà richiesto il codice oltre alla password</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end space-x-4">
          <button
            type="button"
            @click="closeModal"
            :disabled="isLoading"
            class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            Annulla
          </button>
          <button
            type="button"
            @click="generateQRCode"
            :disabled="isLoading"
            class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
            {{ isLoading ? 'Generazione...' : 'Continua' }}
          </button>
        </div>
      </div>

      <!-- Step 2: QR Code Display -->
      <div v-else-if="step === 2" class="space-y-4">
        <div class="text-center">
          <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">
            Scansiona il QR Code
          </h3>

          <!-- QR Code -->
          <div v-if="qrCodeUrl" class="flex justify-center mb-4">
            <img :src="qrCodeUrl" alt="QR Code 2FA" class="w-64 h-64 border-4 border-gray-200 dark:border-gray-700 rounded-lg" />
          </div>

          <!-- Manual Entry Key -->
          <div class="mt-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
            <p class="text-sm text-gray-600 dark:text-gray-400 mb-2">
              Oppure inserisci manualmente questo codice:
            </p>
            <div class="flex items-center justify-center space-x-2">
              <code class="text-lg font-mono font-bold text-gray-900 dark:text-white">
                {{ manualEntryKey }}
              </code>
              <button
                type="button"
                @click="copyToClipboard(manualEntryKey)"
                class="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                title="Copia"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div class="flex justify-between">
          <button
            type="button"
            @click="step = 1"
            :disabled="isLoading"
            class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            Indietro
          </button>
          <button
            type="button"
            @click="step = 3"
            :disabled="isLoading"
            class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            Continua
          </button>
        </div>
      </div>

      <!-- Step 3: Verify Code -->
      <div v-else-if="step === 3" class="space-y-4">
        <div>
          <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">
            Verifica il codice
          </h3>

          <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
            Inserisci il codice a 6 cifre generato dalla tua app di autenticazione
          </p>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Codice di verifica *
            </label>
            <input
              v-model="verificationCode"
              type="text"
              inputmode="numeric"
              pattern="[0-9]*"
              maxlength="6"
              placeholder="000000"
              :disabled="isLoading"
              class="w-full px-4 py-3 text-center text-2xl font-mono tracking-widest border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
              @input="validateCode"
            />
            <p v-if="verificationError" class="mt-2 text-sm text-red-600 dark:text-red-400">
              {{ verificationError }}
            </p>
          </div>
        </div>

        <div class="flex justify-between">
          <button
            type="button"
            @click="step = 2"
            :disabled="isLoading"
            class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            Indietro
          </button>
          <button
            type="button"
            @click="verifyAndEnable"
            :disabled="isLoading || verificationCode.length !== 6"
            class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
            {{ isLoading ? 'Verificando...' : 'Abilita 2FA' }}
          </button>
        </div>
      </div>

      <!-- Step 4: Success -->
      <div v-else-if="step === 4" class="space-y-4">
        <div class="text-center">
          <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100 dark:bg-green-900/30">
            <svg class="h-6 w-6 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h3 class="mt-4 text-lg font-medium text-gray-900 dark:text-white">
            2FA Abilitato con Successo!
          </h3>
          <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
            L'autenticazione a due fattori è ora attiva sul tuo account.
            Al prossimo login ti verrà richiesto il codice dall'app.
          </p>
        </div>

        <div class="flex justify-center">
          <button
            type="button"
            @click="closeModal"
            class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Chiudi
          </button>
        </div>
      </div>
    </div>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { authApi } from '@/services/authApi'
import { useCustomToast } from '@/composables/useCustomToast'
import Modal from '@/components/Common/Modal.vue'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()

const { showError, showSuccess } = useCustomToast()

const isOpen = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const step = ref(1)
const isLoading = ref(false)
const secret = ref('')
const qrCodeUrl = ref('')
const manualEntryKey = ref('')
const verificationCode = ref('')
const verificationError = ref('')

const generateQRCode = async () => {
  try {
    isLoading.value = true
    const response = await authApi.setupTwoFactor()

    if (response.success) {
      secret.value = response.secret
      qrCodeUrl.value = response.qrCodeUrl
      manualEntryKey.value = response.manualEntryKey
      step.value = 2
    } else {
      showError('Errore durante la generazione del codice QR')
    }
  } catch (error: any) {
    console.error('2FA setup error:', error)
    showError(error.response?.data?.message || 'Errore durante la configurazione 2FA')
  } finally {
    isLoading.value = false
  }
}

const validateCode = () => {
  verificationError.value = ''
  verificationCode.value = verificationCode.value.replace(/\D/g, '').slice(0, 6)
}

const verifyAndEnable = async () => {
  try {
    isLoading.value = true
    verificationError.value = ''

    const response = await authApi.enableTwoFactor(secret.value, verificationCode.value)

    if (response.success) {
      step.value = 4
      emit('success')
      showSuccess('2FA abilitato con successo!')
    } else {
      verificationError.value = response.message || 'Codice non valido'
    }
  } catch (error: any) {
    console.error('2FA verification error:', error)
    verificationError.value = error.response?.data?.message || 'Codice di verifica non valido'
  } finally {
    isLoading.value = false
  }
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    showSuccess('Codice copiato negli appunti')
  } catch (error) {
    console.error('Copy failed:', error)
    showError('Impossibile copiare negli appunti')
  }
}

const closeModal = () => {
  step.value = 1
  secret.value = ''
  qrCodeUrl.value = ''
  manualEntryKey.value = ''
  verificationCode.value = ''
  verificationError.value = ''
  isOpen.value = false
}
</script>