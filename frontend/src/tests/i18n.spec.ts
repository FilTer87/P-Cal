import { describe, it, expect } from 'vitest'
import itIT from '../i18n/locales/it-IT.json'
import enUS from '../i18n/locales/en-US.json'
import { createMockI18n, validateI18nKeys } from './i18n-test-helper'

describe('i18n Configuration', () => {
  describe('Translation Files Structure', () => {
    it('should have matching keys in it-IT and en-US', () => {
      const itKeys = getAllKeys(itIT)
      const enKeys = getAllKeys(enUS)

      // Check that all Italian keys exist in English
      const missingInEnglish = itKeys.filter(key => !enKeys.includes(key))
      expect(missingInEnglish, `Missing keys in en-US: ${missingInEnglish.join(', ')}`).toHaveLength(0)

      // Check that all English keys exist in Italian
      const missingInItalian = enKeys.filter(key => !itKeys.includes(key))
      expect(missingInItalian, `Missing keys in it-IT: ${missingInItalian.join(', ')}`).toHaveLength(0)

      // Both should have the same number of keys
      expect(itKeys.length).toBe(enKeys.length)
    })

    it('should have non-empty values for all keys', () => {
      const itKeys = getAllKeys(itIT)
      const enKeys = getAllKeys(enUS)

      itKeys.forEach(key => {
        const value = getNestedValue(itIT, key)
        expect(value, `Empty value for it-IT key: ${key}`).toBeTruthy()
        expect(typeof value, `Invalid type for it-IT key: ${key}`).toBe('string')
      })

      enKeys.forEach(key => {
        const value = getNestedValue(enUS, key)
        expect(value, `Empty value for en-US key: ${key}`).toBeTruthy()
        expect(typeof value, `Invalid type for en-US key: ${key}`).toBe('string')
      })
    })

    it('should have consistent interpolation placeholders', () => {
      const itKeys = getAllKeys(itIT)

      itKeys.forEach(key => {
        const itValue = getNestedValue(itIT, key)
        const enValue = getNestedValue(enUS, key)

        if (typeof itValue !== 'string' || typeof enValue !== 'string') return

        // Extract placeholders like {length}, {min}, {max}
        const itPlaceholders = extractPlaceholders(itValue)
        const enPlaceholders = extractPlaceholders(enValue)

        if (itPlaceholders.length > 0 || enPlaceholders.length > 0) {
          expect(
            itPlaceholders.sort(),
            `Mismatched placeholders for key "${key}": IT has ${itPlaceholders.join(', ')}, EN has ${enPlaceholders.join(', ')}`
          ).toEqual(enPlaceholders.sort())
        }
      })
    })
  })

  describe('i18n Instance', () => {
    it('should create i18n instance with Italian locale', () => {
      const i18n = createMockI18n('it-IT')
      expect(i18n.global.locale.value).toBe('it-IT')
    })

    it('should create i18n instance with English locale', () => {
      const i18n = createMockI18n('en-US')
      expect(i18n.global.locale.value).toBe('en-US')
    })

    it('should translate keys correctly in Italian', () => {
      const i18n = createMockI18n('it-IT')
      const { t } = i18n.global

      expect(t('common.save')).toBe('Salva')
      expect(t('common.cancel')).toBe('Annulla')
      expect(t('auth.login')).toBe('Accedi')
      expect(t('validation.required')).toBe('Questo campo è obbligatorio')
    })

    it('should translate keys correctly in English', () => {
      const i18n = createMockI18n('en-US')
      const { t } = i18n.global

      expect(t('common.save')).toBe('Save')
      expect(t('common.cancel')).toBe('Cancel')
      expect(t('auth.login')).toBe('Login')
      expect(t('validation.required')).toBe('This field is required')
    })

    it('should handle interpolation in Italian', () => {
      const i18n = createMockI18n('it-IT')
      const { t } = i18n.global

      expect(t('validation.minLength', { length: 8 })).toBe('Deve contenere almeno 8 caratteri')
      expect(t('validation.maxLength', { length: 100 })).toBe('Non può superare i 100 caratteri')
    })

    it('should handle interpolation in English', () => {
      const i18n = createMockI18n('en-US')
      const { t } = i18n.global

      expect(t('validation.minLength', { length: 8 })).toBe('Must contain at least 8 characters')
      expect(t('validation.maxLength', { length: 100 })).toBe('Cannot exceed 100 characters')
    })

    it('should fallback to Italian for missing keys', () => {
      const i18n = createMockI18n('en-US')
      const { t } = i18n.global

      // Even if key is missing in English, it should fallback to Italian
      expect(i18n.global.fallbackLocale.value).toBe('it-IT')
    })
  })

  describe('Required Translation Keys', () => {
    it('should have all validation keys', () => {
      const requiredKeys = [
        'validation.required',
        'validation.email',
        'validation.minLength',
        'validation.maxLength',
        'validation.auth.usernameRequired',
        'validation.auth.passwordRequired',
        'validation.auth.confirmPasswordMismatch',
        'validation.task.titleRequired',
        'validation.reminder.dateRequired',
        'validation.reminder.timeRequired'
      ]

      const result = validateI18nKeys(requiredKeys)
      expect(result.valid, `Missing keys: ${result.missing.join(', ')}`).toBe(true)
    })

    it('should have all common UI keys', () => {
      const requiredKeys = [
        'common.save',
        'common.cancel',
        'common.delete',
        'common.edit',
        'common.close',
        'common.loading'
      ]

      const result = validateI18nKeys(requiredKeys)
      expect(result.valid, `Missing keys: ${result.missing.join(', ')}`).toBe(true)
    })

    it('should have all auth keys', () => {
      const requiredKeys = [
        'auth.username',
        'auth.password',
        'auth.email',
        'auth.login',
        'auth.register',
        'auth.logout'
      ]

      const result = validateI18nKeys(requiredKeys)
      expect(result.valid, `Missing keys: ${result.missing.join(', ')}`).toBe(true)
    })

    it('should have all error keys', () => {
      const requiredKeys = [
        'errors.generic',
        'errors.network',
        'errors.unauthorized',
        'errors.invalidDate'
      ]

      const result = validateI18nKeys(requiredKeys)
      expect(result.valid, `Missing keys: ${result.missing.join(', ')}`).toBe(true)
    })
  })
})

// Helper functions
function getAllKeys(obj: any, prefix = ''): string[] {
  const keys: string[] = []

  for (const key in obj) {
    const fullKey = prefix ? `${prefix}.${key}` : key

    if (typeof obj[key] === 'object' && obj[key] !== null && !Array.isArray(obj[key])) {
      keys.push(...getAllKeys(obj[key], fullKey))
    } else {
      keys.push(fullKey)
    }
  }

  return keys
}

function getNestedValue(obj: any, path: string): any {
  return path.split('.').reduce((acc, part) => acc?.[part], obj)
}

function extractPlaceholders(text: string): string[] {
  const regex = /\{(\w+)\}/g
  const matches: string[] = []
  let match

  while ((match = regex.exec(text)) !== null) {
    matches.push(match[1])
  }

  return matches
}
