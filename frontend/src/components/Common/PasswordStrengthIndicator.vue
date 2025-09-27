<template>
  <div v-if="password" class="mt-2">
    <div class="text-xs text-gray-600 dark:text-gray-400 mb-1">
      Sicurezza password:
    </div>
    <div class="flex space-x-1">
      <div
        v-for="i in 5"
        :key="i"
        class="h-1 flex-1 rounded"
        :class="[
          i <= passwordStrength.score
            ? getStrengthColor(passwordStrength.score)
            : 'bg-gray-200 dark:bg-gray-700'
        ]"
      />
    </div>
    <div v-if="passwordStrength.feedback.length > 0" class="mt-1 text-xs text-gray-600 dark:text-gray-400">
      <ul class="list-disc list-inside space-y-0.5">
        <li v-for="tip in passwordStrength.feedback" :key="tip">{{ tip }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { checkPasswordStrength } from '@/utils/validators'

interface Props {
  password: string
}

const props = defineProps<Props>()

// Password strength computation
const passwordStrength = computed(() => {
  return checkPasswordStrength(props.password)
})

// Color mapping for strength levels
const getStrengthColor = (score: number) => {
  if (score >= 4) return 'bg-green-500'
  if (score >= 3) return 'bg-yellow-500'
  if (score >= 2) return 'bg-orange-500'
  return 'bg-red-500'
}
</script>