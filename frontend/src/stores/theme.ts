import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

export const useThemeStore = defineStore('theme', () => {
  // State
  const themeMode = ref<ThemeMode>('system')
  const systemPrefersDark = ref(false)

  // Getters
  const isDarkMode = computed(() => {
    if (themeMode.value === 'system') {
      return systemPrefersDark.value
    }
    return themeMode.value === 'dark'
  })

  const themeIcon = computed(() => {
    switch (themeMode.value) {
      case 'light':
        return 'SunIcon'
      case 'dark':
        return 'MoonIcon'
      case 'system':
        return 'ComputerDesktopIcon'
      default:
        return 'ComputerDesktopIcon'
    }
  })

  const themeName = computed(() => {
    switch (themeMode.value) {
      case 'light':
        return 'Chiaro'
      case 'dark':
        return 'Scuro'
      case 'system':
        return 'Sistema'
      default:
        return 'Sistema'
    }
  })

  // Actions
  const initializeTheme = () => {
    console.log('🎨 Initializing theme...')
    // Load saved theme preference
    const savedTheme = localStorage.getItem('theme') as ThemeMode
    console.log('🎨 Saved theme from localStorage:', savedTheme)
    
    if (savedTheme && ['light', 'dark', 'system'].includes(savedTheme)) {
      themeMode.value = savedTheme
    }

    // Set up system preference detection
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    systemPrefersDark.value = mediaQuery.matches
    
    console.log('🎨 System prefers dark:', systemPrefersDark.value)
    console.log('🎨 Current theme mode:', themeMode.value)
    console.log('🎨 Will be dark mode:', isDarkMode.value)

    // Listen for system preference changes
    mediaQuery.addEventListener('change', (e) => {
      systemPrefersDark.value = e.matches
      applyTheme()
    })

    // Apply initial theme
    applyTheme()
  }

  const setThemeMode = (mode: ThemeMode) => {
    themeMode.value = mode
    localStorage.setItem('theme', mode)
    applyTheme()
  }

  const toggleTheme = () => {
    switch (themeMode.value) {
      case 'light':
        setThemeMode('dark')
        break
      case 'dark':
        setThemeMode('system')
        break
      case 'system':
        setThemeMode('light')
        break
    }
  }

  const applyTheme = () => {
    const root = window.document.documentElement
    
    console.log('🎨 Applying theme - isDarkMode:', isDarkMode.value)
    
    if (isDarkMode.value) {
      root.classList.add('dark')
      console.log('🎨 Added "dark" class to root element')
    } else {
      root.classList.remove('dark')
      console.log('🎨 Removed "dark" class from root element')
    }
    
    console.log('🎨 Root element classes:', root.className)
    
    // Update meta theme-color for mobile browsers
    updateMetaThemeColor()
  }

  const updateMetaThemeColor = () => {
    let metaThemeColor = document.querySelector('meta[name="theme-color"]')
    
    if (!metaThemeColor) {
      metaThemeColor = document.createElement('meta')
      metaThemeColor.setAttribute('name', 'theme-color')
      document.head.appendChild(metaThemeColor)
    }
    
    const color = isDarkMode.value ? '#111827' : '#ffffff'
    metaThemeColor.setAttribute('content', color)
  }

  const getThemeClass = (lightClass: string, darkClass: string) => {
    return isDarkMode.value ? darkClass : lightClass
  }

  const getThemeValue = <T>(lightValue: T, darkValue: T): T => {
    return isDarkMode.value ? darkValue : lightValue
  }

  return {
    // State
    themeMode,
    systemPrefersDark,
    
    // Getters
    isDarkMode,
    themeIcon,
    themeName,
    
    // Actions
    initializeTheme,
    setThemeMode,
    toggleTheme,
    applyTheme,
    updateMetaThemeColor,
    getThemeClass,
    getThemeValue
  }
})