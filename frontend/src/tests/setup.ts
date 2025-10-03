import { beforeAll, afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/vue'
import { config } from '@vue/test-utils'
import { createMockI18n } from './i18n-test-helper'

beforeAll(() => {
  // Setup global i18n plugin for all component tests
  config.global.plugins = [createMockI18n('it-IT')]

  // Mock window.matchMedia
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(query => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  })

  // Mock Intl.NumberFormat for Italian locale formatting
  const OriginalNumberFormat = Intl.NumberFormat

  class MockNumberFormat extends OriginalNumberFormat {
    constructor(locale?: string | string[], options?: Intl.NumberFormatOptions) {
      super(locale, options)
      this._options = options || {}
    }

    format(value: number): string {
      const { style, currency, minimumFractionDigits, maximumFractionDigits } = this._options as any

      // Handle currency formatting
      if (style === 'currency') {
        const decimals = minimumFractionDigits ?? 2
        const formatted = this._formatNumber(value, decimals)
        return `${formatted}\u00A0€`
      }

      // Handle percentage formatting
      if (style === 'percent') {
        const decimals = minimumFractionDigits ?? 0
        const formatted = this._formatNumber(value, decimals)
        return `${formatted}%`
      }

      // Handle regular number formatting
      const decimals = minimumFractionDigits ?? (value % 1 === 0 ? 0 : 2)
      return this._formatNumber(value, decimals)
    }

    _formatNumber(value: number, decimals: number): string {
      const negative = value < 0
      const absValue = Math.abs(value)

      // Split into integer and decimal parts
      const [intPart, decPart] = absValue.toFixed(decimals).split('.')

      // Add thousand separators (Italian uses .)
      const formattedInt = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, '.')

      // Combine with decimal part (Italian uses ,)
      let result = formattedInt
      if (decimals > 0 && decPart) {
        result += ',' + decPart
      }

      return negative ? '-' + result : result
    }
  }

  // @ts-ignore
  Intl.NumberFormat = MockNumberFormat
})

afterEach(() => {
  cleanup()
})
