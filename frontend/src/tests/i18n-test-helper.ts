import { createI18n } from 'vue-i18n'
import type { I18n } from 'vue-i18n'
import itIT from '../i18n/locales/it-IT.json'
import enUS from '../i18n/locales/en-US.json'
import esES from '../i18n/locales/es-ES.json'

// Use actual translation files for testing
export const mockMessages = {
  'it-IT': itIT,
  'en-US': enUS,
  'es-ES': esES
}

/**
 * Creates a mock i18n instance for testing
 */
export function createMockI18n(locale: string = 'it-IT'): I18n {
  return createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'it-IT',
    messages: mockMessages,
    missingWarn: false,
    fallbackWarn: false
  })
}

/**
 * Helper to get global i18n config for component tests
 */
export function getI18nGlobalConfig(locale: string = 'it-IT') {
  return {
    global: {
      plugins: [createMockI18n(locale)]
    }
  }
}

/**
 * Validates that all translation keys exist in both locales
 */
export function validateI18nKeys(keys: string[]): { valid: boolean; missing: string[] } {
  const missing: string[] = []

  keys.forEach(key => {
    const itValue = getNestedValue(mockMessages['it-IT'], key)
    const enValue = getNestedValue(mockMessages['en-US'], key)

    if (!itValue || !enValue) {
      missing.push(key)
    }
  })

  return {
    valid: missing.length === 0,
    missing
  }
}

/**
 * Helper to get nested object value by dot notation key
 */
function getNestedValue(obj: any, path: string): any {
  return path.split('.').reduce((acc, part) => acc?.[part], obj)
}

/**
 * Mock translate function for utilities that don't have access to i18n
 */
export function mockT(key: string, params?: Record<string, any>): string {
  const value = getNestedValue(mockMessages['it-IT'], key)

  if (!value) {
    console.warn(`Missing translation key: ${key}`)
    return key
  }

  // Handle interpolation
  if (params) {
    return Object.entries(params).reduce((str, [key, val]) => {
      return str.replace(new RegExp(`\\{${key}\\}`, 'g'), String(val))
    }, value)
  }

  return value
}
