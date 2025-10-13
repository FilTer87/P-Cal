<template>
  <TransitionRoot :show="show" as="template">
    <Dialog as="div" class="relative z-50" @close="handleCancel">
      <TransitionChild
        as="template"
        enter="ease-out duration-300"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="ease-in duration-200"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" />
      </TransitionChild>

      <div class="fixed inset-0 z-10 overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <TransitionChild
            as="template"
            enter="ease-out duration-300"
            enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
            enter-to="opacity-100 translate-y-0 sm:scale-100"
            leave="ease-in duration-200"
            leave-from="opacity-100 translate-y-0 sm:scale-100"
            leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
          >
            <DialogPanel class="relative transform overflow-hidden rounded-lg bg-white dark:bg-gray-800 px-4 pb-4 pt-5 text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:p-6">
              <!-- Icon -->
              <div class="sm:flex sm:items-start">
                <div class="mx-auto flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/20 sm:mx-0 sm:h-10 sm:w-10">
                  <svg class="h-6 w-6 text-blue-600 dark:text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                </div>
                <div class="mt-3 text-center sm:ml-4 sm:mt-0 sm:text-left flex-1">
                  <DialogTitle as="h3" class="text-base font-semibold leading-6 text-gray-900 dark:text-white">
                    {{ t('recurringEdit.title') }}
                  </DialogTitle>
                  <div class="mt-2">
                    <p class="text-sm text-gray-500 dark:text-gray-400">
                      {{ t('recurringEdit.description') }}
                    </p>
                  </div>

                  <!-- Options -->
                  <div class="mt-4 space-y-3">
                    <div
                      v-for="option in options"
                      :key="option.value"
                      @click="selectedOption = option.value"
                      class="relative flex items-start p-3 rounded-lg border-2 cursor-pointer transition-colors"
                      :class="[
                        selectedOption === option.value
                          ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/10'
                          : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                      ]"
                    >
                      <div class="flex h-6 items-center">
                        <input
                          :id="option.value"
                          :value="option.value"
                          v-model="selectedOption"
                          type="radio"
                          class="h-4 w-4 border-gray-300 text-blue-600 focus:ring-blue-600"
                        />
                      </div>
                      <div class="ml-3 flex-1">
                        <label :for="option.value" class="block text-sm font-medium text-gray-900 dark:text-white cursor-pointer">
                          {{ option.label }}
                        </label>
                        <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                          {{ option.description }}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Actions -->
              <div class="mt-5 sm:mt-4 sm:flex sm:flex-row-reverse gap-3">
                <button
                  type="button"
                  @click="handleConfirm"
                  :disabled="!selectedOption"
                  class="inline-flex w-full justify-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed sm:w-auto"
                >
                  {{ t('common.confirm') }}
                </button>
                <button
                  type="button"
                  @click="handleCancel"
                  class="mt-3 inline-flex w-full justify-center rounded-md bg-white dark:bg-gray-700 px-3 py-2 text-sm font-semibold text-gray-900 dark:text-white shadow-sm ring-1 ring-inset ring-gray-300 dark:ring-gray-600 hover:bg-gray-50 dark:hover:bg-gray-600 sm:mt-0 sm:w-auto"
                >
                  {{ t('common.cancel') }}
                </button>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Dialog, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue'

// i18n
const { t } = useI18n()

// Props
interface Props {
  show: boolean
}

defineProps<Props>()

// Emits
const emit = defineEmits<{
  'close': []
  'edit-all': []
  'edit-single': []
}>()

// State
const selectedOption = ref<'all' | 'single' | null>(null)

// Options
const options = computed(() => [
  {
    value: 'single' as const,
    label: t('recurringEdit.options.single.label'),
    description: t('recurringEdit.options.single.description')
  },
  {
    value: 'all' as const,
    label: t('recurringEdit.options.all.label'),
    description: t('recurringEdit.options.all.description')
  }
])

// Methods
const handleConfirm = () => {
  if (selectedOption.value === 'all') {
    emit('edit-all')
  } else if (selectedOption.value === 'single') {
    emit('edit-single')
  }
  selectedOption.value = null
}

const handleCancel = () => {
  selectedOption.value = null
  emit('close')
}
</script>
