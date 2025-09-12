<template>
  <div class="user-menu relative" ref="menuContainer">
    <!-- User Avatar/Trigger -->
    <button
      @click="toggleMenu"
      @keydown.escape="closeMenu"
      @keydown.enter="toggleMenu"
      @keydown.space.prevent="toggleMenu"
      :class="[
        'flex items-center space-x-3 px-3 py-2 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900',
        isOpen 
          ? 'bg-gray-100 dark:bg-gray-800' 
          : 'hover:bg-gray-50 dark:hover:bg-gray-800'
      ]"
      :aria-expanded="isOpen"
      aria-haspopup="true"
      :aria-label="`Menu utente per ${userDisplayName}`"
    >
      <!-- Avatar -->
      <div class="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center overflow-hidden shadow-sm">
        <img
          v-if="user?.avatar"
          :src="user.avatar"
          :alt="`Avatar di ${userDisplayName}`"
          class="w-full h-full object-cover"
          @error="handleAvatarError"
        />
        <span
          v-else
          class="text-sm font-semibold text-white"
          :title="userDisplayName"
        >
          {{ userInitials }}
        </span>
      </div>

      <!-- User Info (Desktop) -->
      <div v-if="showUserInfo" class="hidden sm:block text-left min-w-0 flex-1">
        <p class="text-sm font-medium text-gray-900 dark:text-white truncate">
          {{ userDisplayName }}
        </p>
        <p class="text-xs text-gray-500 dark:text-gray-400 truncate">
          {{ user?.email }}
        </p>
      </div>

      <!-- Chevron Icon -->
      <svg
        :class="[
          'w-4 h-4 text-gray-400 transition-transform duration-200',
          isOpen ? 'rotate-180' : 'rotate-0'
        ]"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <!-- Dropdown Menu -->
    <Transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="transform opacity-0 scale-95"
      enter-to-class="transform opacity-100 scale-100"
      leave-active-class="transition ease-in duration-150"
      leave-from-class="transform opacity-100 scale-100"
      leave-to-class="transform opacity-0 scale-95"
    >
      <div
        v-if="isOpen"
        ref="dropdown"
        :class="[
          'absolute z-50 mt-2 w-56 rounded-lg shadow-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 divide-y divide-gray-100 dark:divide-gray-700',
          dropdownPosition === 'left' ? 'right-0' : 'left-0'
        ]"
        role="menu"
        aria-orientation="vertical"
        :aria-labelledby="triggerId"
      >
        <!-- User Info Header (Mobile) -->
        <div v-if="!showUserInfo" class="px-4 py-3 sm:hidden">
          <p class="text-sm font-medium text-gray-900 dark:text-white">
            {{ userDisplayName }}
          </p>
          <p class="text-xs text-gray-500 dark:text-gray-400 truncate">
            {{ user?.email }}
          </p>
        </div>

        <!-- Quick Actions -->
        <div class="py-1">
          <!-- Profile -->
          <router-link
            to="/profile"
            @click="closeMenu"
            class="group flex items-center px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 focus:outline-none focus:bg-gray-100 dark:focus:bg-gray-700 transition-colors"
            role="menuitem"
          >
            <svg class="mr-3 h-4 w-4 text-gray-400 group-hover:text-gray-500 dark:group-hover:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            Il mio profilo
          </router-link>

          <!-- Settings -->
          <router-link
            to="/settings"
            @click="closeMenu"
            class="group flex items-center px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 focus:outline-none focus:bg-gray-100 dark:focus:bg-gray-700 transition-colors"
            role="menuitem"
          >
            <svg class="mr-3 h-4 w-4 text-gray-400 group-hover:text-gray-500 dark:group-hover:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            Impostazioni
          </router-link>

          <!-- Account Statistics (if available) -->
          <button
            v-if="accountStats"
            @click="showAccountStats"
            class="group flex items-center w-full px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 focus:outline-none focus:bg-gray-100 dark:focus:bg-gray-700 transition-colors"
            role="menuitem"
          >
            <svg class="mr-3 h-4 w-4 text-gray-400 group-hover:text-gray-500 dark:group-hover:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
            Statistiche account
          </button>

          <!-- Help & Support -->
          <button
            @click="openHelpCenter"
            class="group flex items-center w-full px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 focus:outline-none focus:bg-gray-100 dark:focus:bg-gray-700 transition-colors"
            role="menuitem"
          >
            <svg class="mr-3 h-4 w-4 text-gray-400 group-hover:text-gray-500 dark:group-hover:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Aiuto e supporto
          </button>
        </div>

        <!-- Theme Toggle -->
        <div class="py-1">
          <div class="px-4 py-2">
            <div class="flex items-center justify-between">
              <span class="text-sm text-gray-700 dark:text-gray-300">Tema</span>
              <div class="flex items-center space-x-2">
                <button
                  v-for="themeOption in themeOptions"
                  :key="themeOption.value"
                  @click="changeTheme(themeOption.value)"
                  :class="[
                    'p-1 rounded transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800',
                    currentTheme === themeOption.value
                      ? 'bg-blue-100 text-blue-600 dark:bg-blue-900 dark:text-blue-400'
                      : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300'
                  ]"
                  :title="themeOption.label"
                  :aria-label="themeOption.label"
                >
                  <component :is="themeOption.icon" class="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Admin Panel (if admin) -->
        <div v-if="isAdmin" class="py-1">
          <router-link
            to="/admin"
            @click="closeMenu"
            class="group flex items-center px-4 py-2 text-sm text-purple-700 dark:text-purple-300 hover:bg-purple-50 dark:hover:bg-purple-900/20 focus:outline-none focus:bg-purple-50 dark:focus:bg-purple-900/20 transition-colors"
            role="menuitem"
          >
            <svg class="mr-3 h-4 w-4 text-purple-500 group-hover:text-purple-600 dark:group-hover:text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            Pannello Admin
          </router-link>
        </div>

        <!-- Logout -->
        <div class="py-1">
          <button
            @click="handleLogout"
            :disabled="isLoggingOut"
            class="group flex items-center w-full px-4 py-2 text-sm text-red-700 dark:text-red-300 hover:bg-red-50 dark:hover:bg-red-900/20 focus:outline-none focus:bg-red-50 dark:focus:bg-red-900/20 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            role="menuitem"
          >
            <LoadingSpinner v-if="isLoggingOut" class="mr-3 h-4 w-4 text-red-500" />
            <svg v-else class="mr-3 h-4 w-4 text-red-500 group-hover:text-red-600 dark:group-hover:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            {{ isLoggingOut ? 'Disconnessione...' : 'Disconnetti' }}
          </button>
        </div>
      </div>
    </Transition>

    <!-- Account Stats Modal -->
    <Modal 
      v-model="showStatsModal" 
      title="Statistiche Account"
      size="md"
    >
      <div v-if="accountStats" class="space-y-6">
        <div class="grid grid-cols-2 gap-4">
          <div class="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold text-blue-600 dark:text-blue-400">
              {{ accountStats.taskCount }}
            </div>
            <div class="text-sm text-blue-700 dark:text-blue-300">
              Attività create
            </div>
          </div>
          
          <div class="bg-green-50 dark:bg-green-900/20 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold text-green-600 dark:text-green-400">
              {{ accountStats.reminderCount }}
            </div>
            <div class="text-sm text-green-700 dark:text-green-300">
              Promemoria attivi
            </div>
          </div>
        </div>

        <div class="space-y-3">
          <div class="flex justify-between items-center">
            <span class="text-sm text-gray-600 dark:text-gray-400">Membro dal:</span>
            <span class="text-sm font-medium text-gray-900 dark:text-white">
              {{ formatDate(accountStats.createdAt) }}
            </span>
          </div>
          
          <div class="flex justify-between items-center">
            <span class="text-sm text-gray-600 dark:text-gray-400">Ultimo accesso:</span>
            <span class="text-sm font-medium text-gray-900 dark:text-white">
              {{ formatDate(accountStats.lastLoginAt) }}
            </span>
          </div>
        </div>
      </div>
    </Modal>

    <!-- Help Modal -->
    <Modal 
      v-model="showHelpModal" 
      title="Aiuto e Supporto"
      size="lg"
    >
      <div class="space-y-6">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
            <h4 class="font-medium text-gray-900 dark:text-white mb-2">
              Guide Rapide
            </h4>
            <ul class="space-y-2 text-sm text-gray-600 dark:text-gray-400">
              <li>• Come creare la tua prima attività</li>
              <li>• Gestire i promemoria</li>
              <li>• Personalizzare il calendario</li>
              <li>• Condividere attività</li>
            </ul>
          </div>
          
          <div class="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
            <h4 class="font-medium text-gray-900 dark:text-white mb-2">
              Scorciatoie da Tastiera
            </h4>
            <ul class="space-y-2 text-sm text-gray-600 dark:text-gray-400">
              <li><kbd class="px-1 py-0.5 text-xs bg-gray-200 dark:bg-gray-700 rounded">C</kbd> Crea attività</li>
              <li><kbd class="px-1 py-0.5 text-xs bg-gray-200 dark:bg-gray-700 rounded">T</kbd> Vista oggi</li>
              <li><kbd class="px-1 py-0.5 text-xs bg-gray-200 dark:bg-gray-700 rounded">W</kbd> Vista settimana</li>
              <li><kbd class="px-1 py-0.5 text-xs bg-gray-200 dark:bg-gray-700 rounded">M</kbd> Vista mese</li>
            </ul>
          </div>
        </div>

        <div class="text-center">
          <a 
            href="mailto:support@privatecal.com" 
            class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 4.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            Contatta il Supporto
          </a>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useTheme } from '@/composables/useTheme'
import { useCustomToast } from '@/composables/useCustomToast'
import { authApi } from '@/services/authApi'
import LoadingSpinner from '@/components/Common/LoadingSpinner.vue'
import Modal from '@/components/Common/Modal.vue'

// Props
interface Props {
  showUserInfo?: boolean
  position?: 'left' | 'right'
}

const props = withDefaults(defineProps<Props>(), {
  showUserInfo: true,
  position: 'right'
})

// Composables
const { user, userFullName, userInitials, logout, hasRole } = useAuth()
const { theme: currentTheme, setTheme } = useTheme()
const { showSuccess, showError } = useCustomToast()

// Refs
const menuContainer = ref<HTMLElement>()
const dropdown = ref<HTMLElement>()
const triggerId = ref(`user-menu-${Math.random().toString(36).substr(2, 9)}`)

// State
const isOpen = ref(false)
const isLoggingOut = ref(false)
const showStatsModal = ref(false)
const showHelpModal = ref(false)
const accountStats = ref<{
  taskCount: number
  reminderCount: number
  createdAt: string
  lastLoginAt: string
} | null>(null)

// Computed
const userDisplayName = computed(() => {
  return userFullName.value || user.value?.username || 'Utente'
})

const isAdmin = computed(() => {
  return hasRole('admin') || hasRole('ADMIN')
})

const dropdownPosition = computed(() => {
  return props.position
})

// Theme options
const SunIcon = () => '☀️'
const MoonIcon = () => '🌙'
const ComputerIcon = () => '💻'

const themeOptions = [
  { value: 'light', label: 'Modalità chiara', icon: SunIcon },
  { value: 'dark', label: 'Modalità scura', icon: MoonIcon },
  { value: 'system', label: 'Segui sistema', icon: ComputerIcon }
]

// Methods
const toggleMenu = () => {
  isOpen.value = !isOpen.value
  
  if (isOpen.value) {
    nextTick(() => {
      // Focus first menu item for accessibility
      const firstMenuItem = dropdown.value?.querySelector('[role="menuitem"]') as HTMLElement
      firstMenuItem?.focus()
    })
  }
}

const closeMenu = () => {
  isOpen.value = false
}

const handleClickOutside = (event: Event) => {
  if (menuContainer.value && !menuContainer.value.contains(event.target as Node)) {
    closeMenu()
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  if (!isOpen.value) return

  switch (event.key) {
    case 'Escape':
      closeMenu()
      break
    case 'ArrowDown':
      event.preventDefault()
      focusNextMenuItem()
      break
    case 'ArrowUp':
      event.preventDefault()
      focusPreviousMenuItem()
      break
    case 'Home':
      event.preventDefault()
      focusFirstMenuItem()
      break
    case 'End':
      event.preventDefault()
      focusLastMenuItem()
      break
  }
}

const focusNextMenuItem = () => {
  const menuItems = getMenuItems()
  const currentIndex = menuItems.findIndex(item => item === document.activeElement)
  const nextIndex = currentIndex < menuItems.length - 1 ? currentIndex + 1 : 0
  menuItems[nextIndex]?.focus()
}

const focusPreviousMenuItem = () => {
  const menuItems = getMenuItems()
  const currentIndex = menuItems.findIndex(item => item === document.activeElement)
  const prevIndex = currentIndex > 0 ? currentIndex - 1 : menuItems.length - 1
  menuItems[prevIndex]?.focus()
}

const focusFirstMenuItem = () => {
  const menuItems = getMenuItems()
  menuItems[0]?.focus()
}

const focusLastMenuItem = () => {
  const menuItems = getMenuItems()
  menuItems[menuItems.length - 1]?.focus()
}

const getMenuItems = () => {
  return Array.from(dropdown.value?.querySelectorAll('[role="menuitem"]') || []) as HTMLElement[]
}

const handleAvatarError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}

const changeTheme = (newTheme: 'light' | 'dark' | 'system') => {
  setTheme(newTheme)
  showSuccess(`Tema cambiato in ${newTheme === 'light' ? 'chiaro' : newTheme === 'dark' ? 'scuro' : 'sistema'}`)
}

const handleLogout = async () => {
  if (isLoggingOut.value) return

  try {
    isLoggingOut.value = true
    closeMenu()
    
    await logout()
    showSuccess('Disconnessione effettuata con successo')
  } catch (error) {
    console.error('Logout failed:', error)
    showError('Errore durante la disconnessione')
  } finally {
    isLoggingOut.value = false
  }
}

const loadAccountStats = async () => {
  try {
    const stats = await authApi.getAccountStats()
    accountStats.value = stats
  } catch (error) {
    console.error('Failed to load account stats:', error)
    // Stats are optional, don't show error to user
  }
}

const showAccountStats = () => {
  closeMenu()
  showStatsModal.value = true
}

const openHelpCenter = () => {
  closeMenu()
  showHelpModal.value = true
}

const formatDate = (dateString: string): string => {
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('it-IT', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  } catch {
    return 'Data non disponibile'
  }
}

// Event listeners
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleKeydown)
  loadAccountStats()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* Custom dropdown positioning */
.user-menu {
  @apply inline-block;
}

/* Focus trap styles */
.user-menu:focus-within {
  @apply ring-2 ring-blue-500 ring-opacity-25 rounded-lg;
}

/* Menu item focus styles */
[role="menuitem"]:focus {
  @apply outline-none;
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .border-gray-200 {
    @apply border-black;
  }
  
  .text-gray-600 {
    @apply text-black;
  }
  
  .bg-gray-50 {
    @apply bg-white;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .transition-transform,
  .transition-colors {
    transition: none;
  }
  
  .rotate-180 {
    transform: none;
  }
}

/* Keyboard navigation indicators */
.user-menu:focus-within [role="menuitem"] {
  @apply ring-2 ring-blue-500 ring-offset-2 dark:ring-offset-gray-800 ring-opacity-50;
}

/* Mobile optimizations */
@media (max-width: 640px) {
  .w-56 {
    @apply w-64;
  }
  
  /* Ensure dropdown doesn't go off screen */
  .absolute.right-0 {
    @apply right-0;
  }
  
  .absolute.left-0 {
    @apply left-0;
  }
}

/* Print styles */
@media print {
  .user-menu {
    @apply hidden;
  }
}

/* Loading state */
.user-menu button[disabled] {
  @apply cursor-not-allowed opacity-50;
}

/* Animation improvements */
.transition.ease-out.duration-200 {
  transition-timing-function: cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.transition.ease-in.duration-150 {
  transition-timing-function: cubic-bezier(0.55, 0.055, 0.675, 0.19);
}

/* Avatar improvements */
.w-8.h-8.rounded-full {
  @apply ring-2 ring-white dark:ring-gray-800;
}

.w-8.h-8.rounded-full img {
  @apply transition-opacity duration-200;
}

.w-8.h-8.rounded-full img:hover {
  @apply opacity-90;
}

/* Theme button active states */
.bg-blue-100 {
  @apply shadow-sm;
}

.dark\:bg-blue-900 {
  @apply shadow-sm;
}

/* Modal backdrop blur effect */
.modal-backdrop {
  backdrop-filter: blur(4px);
}

/* Tooltip styles for theme buttons */
button[title]:hover::after {
  content: attr(title);
  @apply absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-2 py-1 text-xs text-white bg-gray-900 rounded shadow-lg z-50;
}

/* Kbd element styling */
kbd {
  @apply inline-block px-1 py-0.5 text-xs font-mono bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded shadow-sm;
}

/* Menu dividers */
.divide-y > * + * {
  @apply border-t border-gray-100 dark:border-gray-700;
}

/* Hover effects for better UX */
[role="menuitem"]:hover {
  @apply transform scale-[1.02] transition-transform duration-150;
}

/* Admin menu item special styling */
.text-purple-700 {
  @apply font-medium;
}

/* Logout button special styling */
.text-red-700 {
  @apply font-medium;
}

/* Stats grid improvements */
.grid.grid-cols-2.gap-4 > div {
  @apply transition-transform duration-200 hover:scale-105;
}
</style>