import { createI18n } from 'vue-i18n'
import type { I18n, I18nOptions } from 'vue-i18n'
import itIT from './locales/it-IT.json'
import enUS from './locales/en-US.json'

// Type for locale messages
export type MessageSchema = typeof itIT

// Available locales
export const AVAILABLE_LOCALES = ['it-IT', 'en-US'] as const
export type Locale = typeof AVAILABLE_LOCALES[number]

// Default locale
export const DEFAULT_LOCALE: Locale = 'it-IT'

// Detect browser locale
export function getBrowserLocale(): Locale {
  const browserLang = navigator.language || (navigator as any).userLanguage

  // Check exact match
  if (AVAILABLE_LOCALES.includes(browserLang as Locale)) {
    return browserLang as Locale
  }

  // Check language code (e.g., 'it' -> 'it-IT')
  const langCode = browserLang.split('-')[0]
  const matchingLocale = AVAILABLE_LOCALES.find(locale => locale.startsWith(langCode))

  return matchingLocale || DEFAULT_LOCALE
}

// Create i18n instance
export const i18n: I18n = createI18n({
  legacy: false, // Use Composition API (no eval, CSP-safe)
  locale: DEFAULT_LOCALE,
  fallbackLocale: DEFAULT_LOCALE,
  globalInjection: true,
  messages: {
    'it-IT': itIT,
    'en-US': enUS
  },
  missingWarn: import.meta.env.DEV,
  fallbackWarn: import.meta.env.DEV,
  // Ensure no message compilation at runtime (CSP-safe)
  warnHtmlMessage: false
})

// Helper to change locale
export function setLocale(locale: Locale) {
  if (!AVAILABLE_LOCALES.includes(locale)) {
    console.warn(`Locale ${locale} not available, using ${DEFAULT_LOCALE}`)
    locale = DEFAULT_LOCALE
  }

  i18n.global.locale.value = locale

  // Persist locale preference
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('locale', locale)
  }
}

// Helper to get locale from storage or browser
export function getInitialLocale(): Locale {
  // Check localStorage first
  if (typeof localStorage !== 'undefined') {
    const stored = localStorage.getItem('locale')
    if (stored && AVAILABLE_LOCALES.includes(stored as Locale)) {
      return stored as Locale
    }
  }

  // Fallback to browser locale
  return getBrowserLocale()
}

// Initialize locale on startup
export function initializeLocale() {
  const locale = getInitialLocale()
  setLocale(locale)
}

// Setup locale change detection
export function setupLocaleWatcher() {
  // Listen for language change events (when user changes browser language)
  window.addEventListener('languagechange', () => {
    // Only auto-switch if user hasn't manually set a preference
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem('locale')
      // If no manual preference exists, update to browser locale
      if (!stored) {
        const newLocale = getBrowserLocale()
        setLocale(newLocale)
      }
    }
  })

  // Also check localStorage changes from other tabs
  window.addEventListener('storage', (e) => {
    if (e.key === 'locale' && e.newValue) {
      const newLocale = e.newValue as Locale
      if (AVAILABLE_LOCALES.includes(newLocale) && i18n.global.locale.value !== newLocale) {
        i18n.global.locale.value = newLocale
      }
    }
  })
}
