<template>
  <Modal v-model="isOpen" title="Password Dimenticata" size="md">
    <div class="space-y-4">
      <!-- Info Message -->
      <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 rounded-lg p-4">
        <div class="flex">
          <svg class="h-5 w-5 text-blue-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
          </svg>
          <div class="ml-3">
            <h3 class="text-sm font-medium text-blue-800 dark:text-blue-200">
              Recupero Password
            </h3>
            <div class="mt-2 text-sm text-blue-700 dark:text-blue-300">
              <p>
                Inserisci il tuo indirizzo email e ti invieremo le istruzioni per reimpostare la password.
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Success Message -->
      <div
        v-if="isSuccess"
        class="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-700 rounded-lg p-4"
      >
        <div class="flex">
          <CheckCircleIcon class="h-5 w-5 text-green-400" />
          <div class="ml-3">
            <h3 class="text-sm font-medium text-green-800 dark:text-green-200">
              Email inviata!
            </h3>
            <div class="mt-2 text-sm text-green-700 dark:text-green-300">
              <p>{{ successMessage }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Email Input -->
      <div v-if="!isSuccess">
        <label for="reset-email" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          Indirizzo Email *
        </label>
        <input
          id="reset-email"
          v-model="form.email"
          type="email"
          autocomplete="email"
          autofocus
          :disabled="isLoading"
          placeholder="inserisci@tuaemail.com"
          class="input w-full"
          :class="{ 'input-error': errors.email }"
          @input="validateField('email')"
          @keypress.enter="submitForm"
        />
        <p v-if="errors.email" class="mt-2 text-sm text-red-600 dark:text-red-400">
          {{ errors.email }}
        </p>
      </div>

      <!-- General Error -->
      <div v-if="generalError" class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700 rounded-lg p-4">
        <div class="flex">
          <ExclamationTriangleIcon class="h-5 w-5 text-red-400" />
          <div class="ml-3">
            <h3 class="text-sm font-medium text-red-800 dark:text-red-200">
              Errore
            </h3>
            <div class="mt-2 text-sm text-red-700 dark:text-red-300">
              <p>{{ generalError }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex justify-end space-x-4 pt-4">
        <button
          type="button"
          @click="close"
          :disabled="isLoading"
          class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 transition-colors"
        >
          {{ isSuccess ? 'Chiudi' : 'Annulla' }}
        </button>

        <button
          v-if="!isSuccess"
          type="button"
          @click="submitForm"
          :disabled="isLoading || !isFormValid"
          class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 transition-colors"
        >
          <div v-if="isLoading" class="loading-spinner mr-2"></div>
          {{ isLoading ? 'Invio in corso...' : 'Invia Email' }}
        </button>
      </div>
    </div>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { CheckCircleIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'
import Modal from '@/components/Common/Modal.vue'
import { authApi } from '@/services/authApi'
import { validateForgotPasswordForm } from '@/utils/validators'
import type { ForgotPasswordFormData } from '@/types/auth'

interface Props {
  modelValue: boolean
}

interface Emits {
  (event: 'update:modelValue', value: boolean): void
  (event: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Form state
const form = ref<ForgotPasswordFormData>({
  email: ''
})

const errors = ref<Record<string, string>>({})
const generalError = ref<string>('')
const isLoading = ref(false)
const isSuccess = ref(false)
const successMessage = ref<string>('')

// Computed
const isOpen = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isFormValid = computed(() => {
  return form.value.email.trim() !== '' && Object.keys(errors.value).length === 0
})

// Methods
const validateField = (field: keyof ForgotPasswordFormData) => {
  const validation = validateForgotPasswordForm(form.value)

  if (validation.errors[field]) {
    errors.value[field] = validation.errors[field]
  } else {
    delete errors.value[field]
  }
}

const validateForm = () => {
  const validation = validateForgotPasswordForm(form.value)
  errors.value = validation.errors
  return validation.isValid
}

const resetForm = () => {
  form.value = { email: '' }
  errors.value = {}
  generalError.value = ''
  isSuccess.value = false
  successMessage.value = ''
}

const submitForm = async () => {
  if (isLoading.value || !validateForm()) {
    return
  }

  isLoading.value = true
  generalError.value = ''

  try {
    const response = await authApi.forgotPassword({
      email: form.value.email.trim()
    })

    if (response.success) {
      isSuccess.value = true
      successMessage.value = response.message
      emit('success')
    } else {
      generalError.value = response.message || 'Errore durante l\'invio dell\'email'
    }
  } catch (error: any) {
    console.error('Forgot password error:', error)
    generalError.value = error.response?.data?.message || error.message || 'Errore durante l\'invio dell\'email'
  } finally {
    isLoading.value = false
  }
}

const close = () => {
  isOpen.value = false
}

// Watchers
watch(isOpen, (newValue) => {
  if (newValue) {
    // Reset form when modal opens
    resetForm()
  }
})

// Expose methods for external control
defineExpose({
  resetForm,
  setLoading: (loading: boolean) => {
    isLoading.value = loading
  },
  setError: (error: string) => {
    generalError.value = error
  }
})
</script>