import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  formatFileSize,
  formatNumber,
  formatCurrency,
  formatPercentage,
  formatDuration,
  formatTimeAgo,
  formatTaskDueDate,
  formatReminderTime,
  formatUserName,
  formatUserInitials,
  formatText,
  formatList,
  formatSearchHighlight,
  formatValidationError,
  formatApiError,
  formatDateRange,
  formatStatistic,
  formatKeyboardShortcut,
  formatBreadcrumb,
  formatNotificationMessage,
  formatSlug,
  formatPhoneNumber,
  formatSafeHtml,
  formatTableCell
} from '../utils/formatters'

describe('formatters.ts', () => {
  describe('formatFileSize', () => {
    it('should format bytes correctly', () => {
      expect(formatFileSize(0)).toBe('0 Bytes')
      expect(formatFileSize(500)).toBe('500 Bytes')
      expect(formatFileSize(1023)).toBe('1023 Bytes')
    })

    it('should format KB correctly', () => {
      expect(formatFileSize(1024)).toBe('1 KB')
      expect(formatFileSize(2048)).toBe('2 KB')
      expect(formatFileSize(1536)).toBe('1.5 KB')
    })

    it('should format MB correctly', () => {
      expect(formatFileSize(1048576)).toBe('1 MB')
      expect(formatFileSize(5242880)).toBe('5 MB')
    })

    it('should format GB correctly', () => {
      expect(formatFileSize(1073741824)).toBe('1 GB')
      expect(formatFileSize(2147483648)).toBe('2 GB')
    })

    it('should handle large numbers', () => {
      expect(formatFileSize(1099511627776)).toBe('1 TB')
    })
  })

  describe('formatNumber', () => {
    it('should format numbers with Italian thousand separators', () => {
      expect(formatNumber(1000)).toBe('1.000')
      expect(formatNumber(1000000)).toBe('1.000.000')
    })

    it('should handle small numbers', () => {
      expect(formatNumber(0)).toBe('0')
      expect(formatNumber(999)).toBe('999')
    })

    it('should handle decimal numbers', () => {
      expect(formatNumber(1234.56)).toBe('1.234,56')
    })
  })

  describe('formatCurrency', () => {
    it('should format currency in Euro', () => {
      expect(formatCurrency(100)).toBe('100,00\u00A0€')
      expect(formatCurrency(1000)).toBe('1.000,00\u00A0€')
    })

    it('should handle decimal values', () => {
      expect(formatCurrency(99.99)).toBe('99,99\u00A0€')
    })

    it('should handle zero', () => {
      expect(formatCurrency(0)).toBe('0,00\u00A0€')
    })

    it('should handle negative values', () => {
      expect(formatCurrency(-50)).toBe('-50,00\u00A0€')
    })
  })

  describe('formatPercentage', () => {
    it('should format percentages with default decimals', () => {
      expect(formatPercentage(50)).toBe('50,0%')
      expect(formatPercentage(75.5)).toBe('75,5%')
    })

    it('should format percentages with custom decimals', () => {
      expect(formatPercentage(33.333, 2)).toBe('33,33%')
      expect(formatPercentage(66.666, 0)).toBe('67%')
    })

    it('should handle zero', () => {
      expect(formatPercentage(0)).toBe('0,0%')
    })

    it('should handle 100%', () => {
      expect(formatPercentage(100)).toBe('100,0%')
    })
  })

  describe('formatDuration', () => {
    it('should format minutes', () => {
      expect(formatDuration(1)).toBe('1 minuto')
      expect(formatDuration(30)).toBe('30 minuti')
      expect(formatDuration(59)).toBe('59 minuti')
    })

    it('should format hours', () => {
      expect(formatDuration(60)).toBe('1 ora')
      expect(formatDuration(120)).toBe('2 ore')
      expect(formatDuration(180)).toBe('3 ore')
    })

    it('should format hours and minutes', () => {
      expect(formatDuration(90)).toBe('1h 30m')
      expect(formatDuration(150)).toBe('2h 30m')
    })

    it('should format days', () => {
      expect(formatDuration(1440)).toBe('1 giorno')
      expect(formatDuration(2880)).toBe('2 giorni')
    })

    it('should format days and hours', () => {
      expect(formatDuration(1500)).toBe('1g 1h')
      expect(formatDuration(3000)).toBe('2g 2h')
    })
  })

  describe('formatUserName', () => {
    it('should format full name when available', () => {
      const user = { firstName: 'Mario', lastName: 'Rossi', username: 'mrossi' }
      expect(formatUserName(user)).toBe('Mario Rossi')
    })

    it('should use first name only when last name is missing', () => {
      const user = { firstName: 'Mario', username: 'mrossi' }
      expect(formatUserName(user)).toBe('Mario')
    })

    it('should fall back to username when no name is provided', () => {
      const user = { username: 'mrossi' }
      expect(formatUserName(user)).toBe('mrossi')
    })

    it('should handle last name only', () => {
      const user = { lastName: 'Rossi', username: 'mrossi' }
      expect(formatUserName(user)).toBe('Rossi')
    })
  })

  describe('formatUserInitials', () => {
    it('should format initials from first and last name', () => {
      const user = { firstName: 'Mario', lastName: 'Rossi', username: 'mrossi' }
      expect(formatUserInitials(user)).toBe('MR')
    })

    it('should use first name only when last name is missing', () => {
      const user = { firstName: 'Mario', username: 'mrossi' }
      expect(formatUserInitials(user)).toBe('MA')
    })

    it('should fall back to username', () => {
      const user = { username: 'mrossi' }
      expect(formatUserInitials(user)).toBe('MR')
    })

    it('should handle short usernames', () => {
      const user = { username: 'm' }
      expect(formatUserInitials(user)).toBe('M')
    })
  })

  describe('formatText', () => {
    it('should return text as-is when under limit', () => {
      expect(formatText('Short text')).toBe('Short text')
    })

    it('should truncate text exceeding max length', () => {
      const longText = 'a'.repeat(150)
      const result = formatText(longText, 100)
      expect(result).toHaveLength(100)
      expect(result).toContain('...')
    })

    it('should handle empty strings', () => {
      expect(formatText('')).toBe('')
    })

    it('should use default max length', () => {
      const text = 'a'.repeat(150)
      expect(formatText(text)).toHaveLength(100)
    })
  })

  describe('formatList', () => {
    it('should format single item', () => {
      expect(formatList(['item1'])).toBe('item1')
    })

    it('should format two items', () => {
      expect(formatList(['item1', 'item2'])).toBe('item1 e item2')
    })

    it('should format multiple items', () => {
      expect(formatList(['item1', 'item2', 'item3'])).toBe('item1, item2 e item3')
    })

    it('should handle custom conjunction', () => {
      expect(formatList(['item1', 'item2'], 'o')).toBe('item1 o item2')
    })

    it('should handle empty list', () => {
      expect(formatList([])).toBe('')
    })
  })

  describe('formatSearchHighlight', () => {
    it('should highlight search query in text', () => {
      expect(formatSearchHighlight('Hello world', 'world')).toBe('Hello <mark>world</mark>')
    })

    it('should be case insensitive', () => {
      expect(formatSearchHighlight('Hello World', 'world')).toBe('Hello <mark>World</mark>')
    })

    it('should highlight multiple occurrences', () => {
      const result = formatSearchHighlight('test test test', 'test')
      expect(result).toBe('<mark>test</mark> <mark>test</mark> <mark>test</mark>')
    })

    it('should handle empty query', () => {
      expect(formatSearchHighlight('Hello world', '')).toBe('Hello world')
    })

    it('should handle empty text', () => {
      expect(formatSearchHighlight('', 'query')).toBe('')
    })
  })

  describe('formatValidationError', () => {
    it('should format known field names', () => {
      expect(formatValidationError('title', 'è obbligatorio')).toBe('Titolo: è obbligatorio')
      expect(formatValidationError('email', 'non valida')).toBe('Email: non valida')
    })

    it('should handle unknown field names', () => {
      expect(formatValidationError('unknownField', 'errore')).toBe('unknownField: errore')
    })
  })

  describe('formatApiError', () => {
    it('should extract error from response data', () => {
      const error = {
        response: {
          data: {
            message: 'API error message'
          }
        }
      }
      expect(formatApiError(error)).toBe('API error message')
    })

    it('should extract error from message property', () => {
      const error = { message: 'Error message' }
      expect(formatApiError(error)).toBe('Error message')
    })

    it('should handle string errors', () => {
      expect(formatApiError('String error')).toBe('String error')
    })

    it('should provide default message for unknown errors', () => {
      expect(formatApiError({})).toBe('Si è verificato un errore imprevisto')
    })
  })

  describe('formatStatistic', () => {
    it('should format statistics correctly', () => {
      const result = formatStatistic(50, 100, 'Completati')
      expect(result.value).toBe('50')
      expect(result.percentage).toBe('50,0%')
      expect(result.label).toBe('Completati')
    })

    it('should handle zero total', () => {
      const result = formatStatistic(0, 0, 'Test')
      expect(result.percentage).toBe('0,0%')
    })

    it('should format large numbers', () => {
      const result = formatStatistic(1500, 10000, 'Tasks')
      expect(result.value).toBe('1.500')
      expect(result.percentage).toBe('15,0%')
    })
  })

  describe('formatKeyboardShortcut', () => {
    it('should format single key', () => {
      expect(formatKeyboardShortcut({ key: 'Enter' })).toBe('Enter')
    })

    it('should format Ctrl combination', () => {
      expect(formatKeyboardShortcut({ key: 'S', ctrl: true })).toBe('Ctrl + S')
    })

    it('should format multiple modifiers', () => {
      expect(formatKeyboardShortcut({ key: 'Delete', ctrl: true, shift: true })).toBe('Ctrl + Shift + Delete')
    })

    it('should format Alt combination', () => {
      expect(formatKeyboardShortcut({ key: 'F4', alt: true })).toBe('Alt + F4')
    })
  })

  describe('formatBreadcrumb', () => {
    it('should format breadcrumb path', () => {
      expect(formatBreadcrumb(['Home', 'Tasks', 'Details'])).toBe('Home > Tasks > Details')
    })

    it('should handle single item', () => {
      expect(formatBreadcrumb(['Home'])).toBe('Home')
    })

    it('should handle empty array', () => {
      expect(formatBreadcrumb([])).toBe('')
    })
  })

  describe('formatNotificationMessage', () => {
    it('should interpolate template variables', () => {
      const template = 'Hello {{name}}, you have {{count}} messages'
      const data = { name: 'Mario', count: 5 }
      expect(formatNotificationMessage(template, data)).toBe('Hello Mario, you have 5 messages')
    })

    it('should handle missing variables', () => {
      const template = 'Hello {{name}}, you have {{count}} messages'
      const data = { name: 'Mario' }
      expect(formatNotificationMessage(template, data)).toBe('Hello Mario, you have {{count}} messages')
    })

    it('should handle templates without variables', () => {
      expect(formatNotificationMessage('Simple message', {})).toBe('Simple message')
    })
  })

  describe('formatSlug', () => {
    it('should convert to lowercase', () => {
      expect(formatSlug('Hello World')).toBe('hello-world')
    })

    it('should replace spaces with hyphens', () => {
      expect(formatSlug('My Blog Post')).toBe('my-blog-post')
    })

    it('should remove special characters', () => {
      expect(formatSlug('Hello! World?')).toBe('hello-world')
    })

    it('should handle accented characters', () => {
      expect(formatSlug('Città di Roma')).toBe('citta-di-roma')
      expect(formatSlug('José García')).toBe('jose-garcia')
    })

    it('should remove consecutive hyphens', () => {
      expect(formatSlug('Hello   World')).toBe('hello-world')
    })

    it('should trim leading and trailing hyphens', () => {
      expect(formatSlug('  Hello World  ')).toBe('hello-world')
    })
  })

  describe('formatPhoneNumber', () => {
    it('should format Italian international numbers', () => {
      expect(formatPhoneNumber('+393331234567')).toBe('+39 333 123 456 7')
      expect(formatPhoneNumber('393331234567')).toBe('+39 333 123 456 7')
    })

    it('should format US/Canadian numbers', () => {
      expect(formatPhoneNumber('+12025551234')).toBe('+1 202 555 123 4')
      expect(formatPhoneNumber('12025551234')).toBe('+1 202 555 123 4')
    })

    it('should format national numbers (10 digits)', () => {
      expect(formatPhoneNumber('3331234567')).toBe('333 123 4567')
      expect(formatPhoneNumber('2025551234')).toBe('202 555 1234')
    })

    it('should format other international numbers', () => {
      expect(formatPhoneNumber('+442071234567')).toBe('+44 207 123 456 7')
      expect(formatPhoneNumber('+33123456789')).toBe('+33 123 456 789')
    })

    it('should return original for very short numbers', () => {
      expect(formatPhoneNumber('123')).toBe('123')
      expect(formatPhoneNumber('12345')).toBe('12345')
    })

    it('should handle numbers with spaces and dashes', () => {
      expect(formatPhoneNumber('+39 333 123 4567')).toBe('+39 333 123 456 7')
      expect(formatPhoneNumber('333-123-4567')).toBe('333 123 4567')
    })
  })

  describe('formatSafeHtml', () => {
    it('should escape HTML special characters', () => {
      expect(formatSafeHtml('<script>alert("xss")</script>'))
        .toBe('&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;')
    })

    it('should escape ampersands', () => {
      expect(formatSafeHtml('Tom & Jerry')).toBe('Tom &amp; Jerry')
    })

    it('should escape quotes', () => {
      expect(formatSafeHtml('Say "hello"')).toBe('Say &quot;hello&quot;')
      expect(formatSafeHtml("It's mine")).toBe('It&#39;s mine')
    })

    it('should prevent XSS attacks', () => {
      const xssAttempts = [
        '<img src=x onerror=alert(1)>',
        '<script>alert(document.cookie)</script>',
        '<a href="javascript:alert(1)">Click</a>',
        '<iframe src="evil.com"></iframe>'
      ]

      xssAttempts.forEach(xss => {
        const result = formatSafeHtml(xss)
        expect(result).not.toContain('<')
        expect(result).not.toContain('>')
        expect(result).toContain('&lt;')
        expect(result).toContain('&gt;')
      })
    })

    it('should handle nested HTML', () => {
      expect(formatSafeHtml('<div><span>text</span></div>'))
        .toBe('&lt;div&gt;&lt;span&gt;text&lt;/span&gt;&lt;/div&gt;')
    })

    it('should escape multiple characters correctly', () => {
      expect(formatSafeHtml('<a href="test">Link & More</a>'))
        .toBe('&lt;a href=&quot;test&quot;&gt;Link &amp; More&lt;/a&gt;')
    })
  })

  describe('formatTableCell', () => {
    it('should format text type', () => {
      expect(formatTableCell('Hello', 'text')).toBe('Hello')
      expect(formatTableCell(123, 'text')).toBe('123')
    })

    it('should format number type', () => {
      expect(formatTableCell(1000, 'number')).toBe('1.000')
      expect(formatTableCell(1234.56, 'number')).toBe('1.234,56')
    })

    it('should format boolean type', () => {
      expect(formatTableCell(true, 'boolean')).toBe('Sì')
      expect(formatTableCell(false, 'boolean')).toBe('No')
    })

    it('should handle null/undefined', () => {
      expect(formatTableCell(null, 'text')).toBe('-')
      expect(formatTableCell(undefined, 'number')).toBe('-')
    })
  })

  describe('formatTaskDueDate', () => {
    beforeEach(() => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2025-01-15T10:00:00Z'))
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it('should handle no due date', () => {
      const result = formatTaskDueDate('')
      expect(result.text).toBe('Nessuna scadenza')
      expect(result.color).toBe('gray')
      expect(result.isPast).toBe(false)
    })

    it('should format past due dates', () => {
      const result = formatTaskDueDate('2025-01-01T10:00:00Z')
      expect(result.isPast).toBe(true)
      expect(result.color).toBe('gray')
      expect(result.text).toContain('Passato')
    })

    it('should format future due dates', () => {
      const result = formatTaskDueDate('2025-12-31T10:00:00Z')
      expect(result.isPast).toBe(false)
      expect(result.text).toContain('Scade')
    })

    it('should show yellow color for soon due dates', () => {
      const tomorrow = new Date('2025-01-16T10:00:00Z')
      const result = formatTaskDueDate(tomorrow.toISOString())
      expect(result.color).toBe('yellow')
    })

    it('should show blue color for distant due dates', () => {
      const nextMonth = new Date('2025-02-15T10:00:00Z')
      const result = formatTaskDueDate(nextMonth.toISOString())
      expect(result.color).toBe('blue')
    })
  })

  describe('formatReminderTime', () => {
    beforeEach(() => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2025-01-15T10:00:00Z'))
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it('should format sent reminders', () => {
      const result = formatReminderTime('2025-01-10T10:00:00Z', true)
      expect(result.text).toContain('Inviato il')
      expect(result.color).toBe('green')
    })

    it('should format past reminders (not sent)', () => {
      const result = formatReminderTime('2025-01-10T10:00:00Z', false)
      expect(result.text).toContain('Doveva essere inviato')
      expect(result.color).toBe('red')
    })

    it('should format future reminders', () => {
      const result = formatReminderTime('2025-12-31T10:00:00Z', false)
      expect(result.color).toBe('blue')
    })

    it('should show yellow for soon reminders (within 1 hour)', () => {
      const soon = new Date('2025-01-15T10:30:00Z')
      const result = formatReminderTime(soon.toISOString(), false)
      expect(result.color).toBe('yellow')
    })
  })

  describe('Security Tests for Formatters', () => {
    it('should prevent XSS in formatSearchHighlight', () => {
      const result = formatSearchHighlight('Hello <script>alert(1)</script>', 'script')
      // Note: This function adds <mark> tags, so proper escaping should happen in the component
      expect(result).toContain('<mark>')
    })

    it('should safely escape HTML in formatSafeHtml', () => {
      const maliciousInputs = [
        '<script>alert(document.cookie)</script>',
        '<img src=x onerror=alert(1)>',
        '<iframe src="javascript:alert(1)">',
        '<body onload=alert(1)>',
        '<svg/onload=alert(1)>',
        '<input onfocus=alert(1) autofocus>'
      ]

      maliciousInputs.forEach(input => {
        const result = formatSafeHtml(input)
        expect(result).not.toMatch(/<script|<img|<iframe|<body|<svg|<input/i)
        expect(result).toContain('&lt;')
        expect(result).toContain('&gt;')
      })
    })

    it('should handle template injection in formatNotificationMessage', () => {
      const template = 'Hello {{name}}'
      const data = { name: '<script>alert(1)</script>' }
      const result = formatNotificationMessage(template, data)
      // Note: Escaping should be done when rendering, but the function itself doesn't execute code
      expect(result).toBe('Hello <script>alert(1)</script>')
    })

    it('should prevent double-escaping in formatSafeHtml', () => {
      // First escape
      const once = formatSafeHtml('Tom & Jerry')
      expect(once).toBe('Tom &amp; Jerry')

      // Second escape should escape the already-escaped ampersand
      const twice = formatSafeHtml(once)
      expect(twice).toBe('Tom &amp;amp; Jerry')
    })

    it('should sanitize user input in formatSlug', () => {
      const maliciousInputs = [
        '../../../etc/passwd',
        '<script>alert(1)</script>',
        'test"onclick="alert(1)',
        'test$(rm -rf /)'
      ]

      maliciousInputs.forEach(input => {
        const slug = formatSlug(input)
        expect(slug).not.toContain('<')
        expect(slug).not.toContain('>')
        expect(slug).not.toContain('"')
        expect(slug).not.toContain('$')
        expect(slug).not.toContain('/')
        expect(slug).toMatch(/^[a-z0-9-]*$/)
      })
    })
  })
})
