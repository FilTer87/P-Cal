<template>
  <Modal v-model="isOpen" :title="$t('twoFactor.disableTitle')" :persistent="true">
    <div class="space-y-4">
      <div class="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded-lg p-4">
        <div class="flex">
          <svg class="h-5 w-5 text-yellow-400" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
          </svg>
          <div class="ml-3">
            <h3 class="text-sm font-medium text-yellow-800 dark:text-yellow-200">
              {{ $t('twoFactor.disableWarningTitle') }}
            </h3>
            <div class="mt-2 text-sm text-yellow-700 dark:text-yellow-300">
              <p>
                {{ $t('twoFactor.disableWarningMessage') }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <p class="text-sm text-gray-600 dark:text-gray-400">
        {{ $t('twoFactor.disableConfirmMessage') }}
      </p>

      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {{ $t('twoFactor.currentPasswordLabel') }} *
        </label>
        <input
          v-model="password"
          type="password"
          required
          :disabled="isLoading"
          class="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-800 transition-colors"
          :placeholder="$t('twoFactor.currentPasswordPlaceholder')"
        />
        <p v-if="error" class="mt-2 text-sm text-red-600 dark:text-red-400">
          {{ error }}
        </p>
      </div>

      <div class="flex justify-end space-x-4 pt-4">
        <button
          type="button"
          @click="closeModal"
          :disabled="isLoading"
          class="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
        >
          {{ $t('common.cancel') }}
        </button>
        <button
          type="button"
          @click="disableTwoFactor"
          :disabled="isLoading || !password.trim()"
          class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50"
        >
          <LoadingSpinner v-if="isLoading" class="w-4 h-4 mr-2" />
          {{ isLoading ? $t('twoFactor.disabling') : $t('twoFactor.disable2FA') }}
        </button>
      </div>
    </div>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
const { showError, showSuccess } = useCustomToast()

const isOpen = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isLoading = ref(false)
const password = ref('')
const error = ref('')

const disableTwoFactor = async () => {
  try {
    isLoading.value = true
    error.value = ''

    const response = await authApi.disableTwoFactor(password.value)

    if (response.success) {
      emit('success')
      showSuccess(t('twoFactor.disableSuccess'))
      closeModal()
    } else {
      error.value = response.message || t('twoFactor.disableError')
    }
  } catch (err: any) {
    console.error('2FA disable error:', err)
    error.value = err.response?.data?.message || t('twoFactor.passwordIncorrect')
  } finally {
    isLoading.value = false
  }
}

const closeModal = () => {
  password.value = ''
  error.value = ''
  isOpen.value = false
}
</script>