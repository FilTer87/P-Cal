<template>
  <Teleport to="body">
    <div class="fixed top-4 right-4 z-50 flex flex-col space-y-3 pointer-events-none max-w-sm">
      <TransitionGroup
        enter-active-class="transition ease-out duration-300"
        enter-from-class="opacity-0 translate-x-full scale-95"
        enter-to-class="opacity-100 translate-x-0 scale-100"
        leave-active-class="transition ease-in duration-200"
        leave-from-class="opacity-100 translate-x-0 scale-100"
        leave-to-class="opacity-0 translate-x-full scale-95"
        move-class="transition-all duration-300 ease-in-out"
      >
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="pointer-events-auto w-full"
        >
          <Toast
            :type="toast.type"
            :title="toast.title"
            :message="toast.message"
            :auto-dismiss="toast.autoDismiss"
            :duration="toast.duration"
            :action-text="toast.actionText"
            :action-callback="toast.actionCallback"
            :on-close="() => removeToast(toast.id)"
          />
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { useCustomToast } from '../../composables/useCustomToast'
import Toast from './Toast.vue'

const { toasts, removeToast } = useCustomToast()
</script>

<style scoped>
/* Responsive adjustments */
@media (max-width: 640px) {
  .fixed.top-4.right-4 {
    @apply top-2 right-2 left-2;
  }
}
</style>