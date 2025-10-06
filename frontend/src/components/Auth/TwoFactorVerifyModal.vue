<template>
  <Modal v-model="isOpen" :title="$t('twoFactor.verifyTitle')" :persistent="true">
    <div class="space-y-4">
      <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 rounded-lg p-4">
        <div class="flex">
          <svg class="h-5 w-5 text-blue-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
          </svg>
          <div class="ml-3">
            <h3 class="text-sm font-medium text-blue-800 dark:text-blue-200">
              {{ $t('twoFactor.verifyRequired') }}
            </h3>
            <div class="mt-2 text-sm text-blue-700 dark:text-blue-300">
              <p>
                {{ $t('twoFactor.verifyDescription') }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ $t('twoFactor.verificationCodeLabel') }} *
        </label>
        <input
          v-model="code"
          type="text"
          inputmode="numeric"
          pattern="[0-9]*"
          maxlength="6"
          autofocus
          :placeholder="$t('twoFactor.verificationCodePlaceholder')"
          :disabled="isLoading"
          class="w-full px-4 py-3 text-center text-2xl font-mono tracking-widest border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
          @input="validateCode"
          @keypress.enter="verifyCode"
        />
        <p v-if="error" class="mt-2 text-sm text-red-600 dark:text-red-400">
          {{ error }}
        </p>
      </div>

      <div class="flex justify-end space-x-4 pt-4">
        <button
          type="button"
          @click="cancel"
          :disabled="isLoading"
          class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
        >
          {{ $t('common.cancel') }}
        </button>
        <button
          type="button"
          @click="verifyCode"
          :disabled="isLoading || code.length !== 6"
          class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
        >
          <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
          {{ isLoading ? $t('twoFactor.verifying') : $t('twoFactor.verify') }}
        </button>
      </div>
    </div>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Modal from '@/components/Common/Modal.vue'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'verify': [code: string]
  'cancel': []
}>()

const isOpen = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const code = ref('')
const error = ref('')
const isLoading = ref(false)

const validateCode = () => {
  error.value = ''
  code.value = code.value.replace(/\D/g, '').slice(0, 6)
}

const verifyCode = () => {
  if (code.value.length === 6) {
    emit('verify', code.value)
  }
}

const cancel = () => {
  emit('cancel')
  closeModal()
}

const closeModal = () => {
  code.value = ''
  error.value = ''
  isOpen.value = false
}

const setError = (message: string) => {
  error.value = message
}

const setLoading = (loading: boolean) => {
  isLoading.value = loading
}

defineExpose({
  setError,
  setLoading
})
</script>