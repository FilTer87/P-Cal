import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  rules,
  authValidators,
  taskValidators,
  validateForm,
  validateLoginForm,
  validateRegistrationForm,
  validateTaskForm,
  validateForgotPasswordForm,
  validateResetPasswordForm,
  checkPasswordStrength,
  createValidator,
  patterns
} from '../utils/validators'

describe('validators.ts', () => {
  describe('Basic Rules', () => {
    describe('rules.required', () => {
      it('should validate non-empty values', () => {
        expect(rules.required('test').isValid).toBe(true)
        expect(rules.required(0).isValid).toBe(true)
        expect(rules.required(false).isValid).toBe(true)
      })

      it('should reject null, undefined, and empty string', () => {
        expect(rules.required(null).isValid).toBe(false)
        expect(rules.required(undefined).isValid).toBe(false)
        expect(rules.required('').isValid).toBe(false)
      })

      it('should return appropriate error message', () => {
        const result = rules.required('')
        expect(result.message).toBe('Questo campo è obbligatorio')
      })
    })

    describe('rules.email', () => {
      it('should validate correct email formats', () => {
        expect(rules.email('user@example.com').isValid).toBe(true)
        expect(rules.email('test.user@example.co.uk').isValid).toBe(true)
        expect(rules.email('user+tag@example.com').isValid).toBe(true)
      })

      it('should reject invalid email formats', () => {
        expect(rules.email('invalid').isValid).toBe(false)
        expect(rules.email('invalid@').isValid).toBe(false)
        expect(rules.email('@example.com').isValid).toBe(false)
        expect(rules.email('user@').isValid).toBe(false)
        expect(rules.email('user@domain').isValid).toBe(false)
      })

      it('should allow empty values', () => {
        expect(rules.email('').isValid).toBe(true)
      })
    })

    describe('rules.minLength', () => {
      it('should validate strings meeting minimum length', () => {
        expect(rules.minLength(3)('test').isValid).toBe(true)
        expect(rules.minLength(3)('abc').isValid).toBe(true)
      })

      it('should reject strings below minimum length', () => {
        expect(rules.minLength(5)('test').isValid).toBe(false)
        expect(rules.minLength(3)('ab').isValid).toBe(false)
      })

      it('should allow empty values', () => {
        expect(rules.minLength(5)('').isValid).toBe(true)
      })
    })

    describe('rules.maxLength', () => {
      it('should validate strings within maximum length', () => {
        expect(rules.maxLength(5)('test').isValid).toBe(true)
        expect(rules.maxLength(5)('12345').isValid).toBe(true)
      })

      it('should reject strings exceeding maximum length', () => {
        expect(rules.maxLength(3)('test').isValid).toBe(false)
        expect(rules.maxLength(5)('123456').isValid).toBe(false)
      })

      it('should allow empty values', () => {
        expect(rules.maxLength(5)('').isValid).toBe(true)
      })
    })

    describe('rules.numeric', () => {
      it('should validate numeric strings', () => {
        expect(rules.numeric('123').isValid).toBe(true)
        expect(rules.numeric('0').isValid).toBe(true)
      })

      it('should reject non-numeric strings', () => {
        expect(rules.numeric('abc').isValid).toBe(false)
        expect(rules.numeric('12.3').isValid).toBe(false)
        expect(rules.numeric('12a').isValid).toBe(false)
      })

      it('should allow empty values', () => {
        expect(rules.numeric('').isValid).toBe(true)
      })
    })

    describe('rules.alphanumeric', () => {
      it('should validate alphanumeric strings', () => {
        expect(rules.alphanumeric('abc123').isValid).toBe(true)
        expect(rules.alphanumeric('ABC').isValid).toBe(true)
        expect(rules.alphanumeric('123').isValid).toBe(true)
      })

      it('should reject strings with special characters', () => {
        expect(rules.alphanumeric('abc-123').isValid).toBe(false)
        expect(rules.alphanumeric('abc_123').isValid).toBe(false)
        expect(rules.alphanumeric('abc 123').isValid).toBe(false)
      })
    })

    describe('rules.minValue', () => {
      it('should validate numbers meeting minimum value', () => {
        expect(rules.minValue(5)(10).isValid).toBe(true)
        expect(rules.minValue(5)(5).isValid).toBe(true)
      })

      it('should reject numbers below minimum value', () => {
        expect(rules.minValue(5)(3).isValid).toBe(false)
        expect(rules.minValue(0)(-1).isValid).toBe(false)
      })

      it('should allow null values', () => {
        expect(rules.minValue(5)(null as any).isValid).toBe(true)
      })
    })

    describe('rules.maxValue', () => {
      it('should validate numbers within maximum value', () => {
        expect(rules.maxValue(10)(5).isValid).toBe(true)
        expect(rules.maxValue(10)(10).isValid).toBe(true)
      })

      it('should reject numbers exceeding maximum value', () => {
        expect(rules.maxValue(10)(15).isValid).toBe(false)
        expect(rules.maxValue(0)(1).isValid).toBe(false)
      })

      it('should allow null values', () => {
        expect(rules.maxValue(10)(null as any).isValid).toBe(true)
      })
    })

    describe('rules.url', () => {
      it('should validate correct URLs', () => {
        expect(rules.url('https://example.com').isValid).toBe(true)
        expect(rules.url('http://example.com/path').isValid).toBe(true)
        expect(rules.url('https://sub.example.com:8080/path?query=1').isValid).toBe(true)
      })

      it('should reject invalid URLs', () => {
        expect(rules.url('not-a-url').isValid).toBe(false)
        expect(rules.url('example.com').isValid).toBe(false)
        expect(rules.url('//example.com').isValid).toBe(false)
      })

      it('should allow empty values', () => {
        expect(rules.url('').isValid).toBe(true)
      })
    })

    describe('rules.phone', () => {
      it('should validate correct phone numbers', () => {
        expect(rules.phone('+393331234567').isValid).toBe(true)
        expect(rules.phone('1234567890').isValid).toBe(true)
        expect(rules.phone('+1 555 123 4567').isValid).toBe(true)
      })

      it('should reject invalid phone numbers', () => {
        expect(rules.phone('abc').isValid).toBe(false)
        expect(rules.phone('123').isValid).toBe(false)
        expect(rules.phone('+').isValid).toBe(false)
      })

      it('should allow empty values', () => {
        expect(rules.phone('').isValid).toBe(true)
      })
    })

    describe('rules.match', () => {
      it('should validate matching values', () => {
        expect(rules.match('password123', 'password')('password123').isValid).toBe(true)
      })

      it('should reject non-matching values', () => {
        expect(rules.match('password123', 'password')('different').isValid).toBe(false)
      })

      it('should allow empty values', () => {
        expect(rules.match('password123', 'password')('').isValid).toBe(true)
      })
    })
  })

  describe('Authentication Validators', () => {
    describe('authValidators.username', () => {
      it('should validate correct usernames', () => {
        expect(authValidators.username('user123').isValid).toBe(true)
        expect(authValidators.username('test_user').isValid).toBe(true)
        expect(authValidators.username('user.name').isValid).toBe(true)
        expect(authValidators.username('user-name').isValid).toBe(true)
      })

      it('should reject usernames that are too short', () => {
        const result = authValidators.username('ab')
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('almeno 3 caratteri')
      })

      it('should reject usernames that are too long', () => {
        const result = authValidators.username('a'.repeat(51))
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('non può superare i 50 caratteri')
      })

      it('should reject usernames with invalid characters', () => {
        expect(authValidators.username('user name').isValid).toBe(false)
        expect(authValidators.username('user@name').isValid).toBe(false)
        expect(authValidators.username('user#name').isValid).toBe(false)
      })

      it('should reject empty or whitespace usernames', () => {
        expect(authValidators.username('').isValid).toBe(false)
        expect(authValidators.username('   ').isValid).toBe(false)
      })

      it('should trim whitespace', () => {
        expect(authValidators.username('  user123  ').isValid).toBe(true)
      })
    })

    describe('authValidators.password', () => {
      it('should validate strong passwords', () => {
        expect(authValidators.password('Password123').isValid).toBe(true)
        expect(authValidators.password('Secur3Pass').isValid).toBe(true)
      })

      it('should reject passwords that are too short', () => {
        const result = authValidators.password('Pass1')
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('almeno 8 caratteri')
      })

      it('should reject passwords that are too long', () => {
        const result = authValidators.password('A1' + 'a'.repeat(127))
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('non può superare i 128 caratteri')
      })

      it('should reject passwords without lowercase letters', () => {
        const result = authValidators.password('PASSWORD123')
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('lettera minuscola')
      })

      it('should reject passwords without uppercase letters', () => {
        const result = authValidators.password('password123')
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('lettera maiuscola')
      })

      it('should reject passwords without numbers', () => {
        const result = authValidators.password('PasswordABC')
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('numero')
      })

      it('should reject empty passwords', () => {
        const result = authValidators.password('')
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('obbligatoria')
      })
    })

    describe('authValidators.confirmPassword', () => {
      it('should validate matching passwords', () => {
        expect(authValidators.confirmPassword('Password123', 'Password123').isValid).toBe(true)
      })

      it('should reject non-matching passwords', () => {
        const result = authValidators.confirmPassword('Password123', 'Different123')
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('non corrispondono')
      })

      it('should reject empty confirmation', () => {
        const result = authValidators.confirmPassword('Password123', '')
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('obbligatoria')
      })
    })

    describe('authValidators.email', () => {
      it('should validate correct email addresses', () => {
        expect(authValidators.email('user@example.com').isValid).toBe(true)
        expect(authValidators.email('test.user@example.co.uk').isValid).toBe(true)
      })

      it('should reject invalid email formats', () => {
        expect(authValidators.email('invalid').isValid).toBe(false)
        expect(authValidators.email('@example.com').isValid).toBe(false)
        expect(authValidators.email('user@').isValid).toBe(false)
      })

      it('should reject emails that are too long', () => {
        const longEmail = 'a'.repeat(250) + '@example.com'
        expect(authValidators.email(longEmail).isValid).toBe(false)
      })

      it('should reject empty emails', () => {
        expect(authValidators.email('').isValid).toBe(false)
        expect(authValidators.email('   ').isValid).toBe(false)
      })

      it('should trim whitespace', () => {
        expect(authValidators.email('  user@example.com  ').isValid).toBe(true)
      })
    })

    describe('authValidators.name', () => {
      it('should validate correct names', () => {
        expect(authValidators.name('Mario').isValid).toBe(true)
        expect(authValidators.name('Mario Rossi').isValid).toBe(true)
        expect(authValidators.name("O'Brien").isValid).toBe(true)
        expect(authValidators.name('Jean-Claude').isValid).toBe(true)
      })

      it('should allow empty names (optional field)', () => {
        expect(authValidators.name('').isValid).toBe(true)
      })

      it('should reject names that are too long', () => {
        const result = authValidators.name('a'.repeat(101))
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('non può superare i 100 caratteri')
      })

      it('should reject names with numbers', () => {
        expect(authValidators.name('Mario123').isValid).toBe(false)
      })

      it('should reject names with special characters', () => {
        expect(authValidators.name('Mario@Rossi').isValid).toBe(false)
      })

      it('should accept international characters', () => {
        expect(authValidators.name('José').isValid).toBe(true)
        expect(authValidators.name('François').isValid).toBe(true)
        expect(authValidators.name('Müller').isValid).toBe(true)
      })
    })
  })

  describe('Task Validators', () => {
    describe('taskValidators.title', () => {
      it('should validate non-empty titles', () => {
        expect(taskValidators.title('Valid Task').isValid).toBe(true)
      })

      it('should reject empty titles', () => {
        expect(taskValidators.title('').isValid).toBe(false)
        expect(taskValidators.title('   ').isValid).toBe(false)
      })

      it('should reject titles that are too long', () => {
        const result = taskValidators.title('a'.repeat(256))
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('255 caratteri')
      })

      it('should trim whitespace', () => {
        expect(taskValidators.title('  Task Title  ').isValid).toBe(true)
      })
    })

    describe('taskValidators.description', () => {
      it('should validate descriptions within limit', () => {
        expect(taskValidators.description('Valid description').isValid).toBe(true)
      })

      it('should allow empty descriptions', () => {
        expect(taskValidators.description('').isValid).toBe(true)
      })

      it('should reject descriptions that are too long', () => {
        const result = taskValidators.description('a'.repeat(1001))
        expect(result.isValid).toBe(false)
        expect(result.message).toContain('1000 caratteri')
      })
    })

    describe('taskValidators.priority', () => {
      it('should validate correct priority values', () => {
        expect(taskValidators.priority('LOW').isValid).toBe(true)
        expect(taskValidators.priority('MEDIUM').isValid).toBe(true)
        expect(taskValidators.priority('HIGH').isValid).toBe(true)
        expect(taskValidators.priority('URGENT').isValid).toBe(true)
      })

      it('should reject invalid priority values', () => {
        expect(taskValidators.priority('INVALID').isValid).toBe(false)
        expect(taskValidators.priority('low').isValid).toBe(false)
      })

      it('should reject empty priority', () => {
        expect(taskValidators.priority('').isValid).toBe(false)
      })
    })
  })

  describe('Form Validation Functions', () => {
    describe('validateLoginForm', () => {
      it('should validate correct login credentials', () => {
        const result = validateLoginForm({
          username: 'testuser',
          password: 'password123'
        })
        expect(result.isValid).toBe(true)
        expect(result.errors).toEqual({})
      })

      it('should reject empty username', () => {
        const result = validateLoginForm({
          username: '',
          password: 'password123'
        })
        expect(result.isValid).toBe(false)
        expect(result.errors.username).toBeDefined()
      })

      it('should reject empty password', () => {
        const result = validateLoginForm({
          username: 'testuser',
          password: ''
        })
        expect(result.isValid).toBe(false)
        expect(result.errors.password).toBeDefined()
      })
    })

    describe('validateRegistrationForm', () => {
      it('should validate correct registration data', () => {
        const result = validateRegistrationForm({
          username: 'testuser',
          email: 'test@example.com',
          password: 'Password123',
          confirmPassword: 'Password123',
          firstName: 'Mario',
          lastName: 'Rossi'
        })
        expect(result.isValid).toBe(true)
        expect(result.errors).toEqual({})
      })

      it('should validate without optional fields', () => {
        const result = validateRegistrationForm({
          username: 'testuser',
          email: 'test@example.com',
          password: 'Password123',
          confirmPassword: 'Password123'
        })
        expect(result.isValid).toBe(true)
      })

      it('should collect multiple validation errors', () => {
        const result = validateRegistrationForm({
          username: 'ab',
          email: 'invalid',
          password: 'weak',
          confirmPassword: 'different'
        })
        expect(result.isValid).toBe(false)
        expect(Object.keys(result.errors).length).toBeGreaterThan(1)
      })

      it('should validate optional firstName and lastName', () => {
        const result = validateRegistrationForm({
          username: 'testuser',
          email: 'test@example.com',
          password: 'Password123',
          confirmPassword: 'Password123',
          firstName: 'Mario123', // Invalid
          lastName: 'Rossi'
        })
        expect(result.isValid).toBe(false)
        expect(result.errors.firstName).toBeDefined()
      })
    })

    describe('validateTaskForm', () => {
      it('should validate correct task data', () => {
        const result = validateTaskForm({
          title: 'Task title',
          description: 'Task description',
          priority: 'HIGH',
          dueDate: '2025-12-31',
          dueTime: '14:30'
        })
        expect(result.isValid).toBe(true)
      })

      it('should validate without optional fields', () => {
        const result = validateTaskForm({
          title: 'Task title',
          priority: 'MEDIUM'
        })
        expect(result.isValid).toBe(true)
      })
    })

    describe('validateForgotPasswordForm', () => {
      it('should validate correct email', () => {
        const result = validateForgotPasswordForm({
          email: 'user@example.com'
        })
        expect(result.isValid).toBe(true)
      })

      it('should reject invalid email', () => {
        const result = validateForgotPasswordForm({
          email: 'invalid'
        })
        expect(result.isValid).toBe(false)
      })
    })

    describe('validateResetPasswordForm', () => {
      it('should validate matching strong passwords', () => {
        const result = validateResetPasswordForm({
          newPassword: 'NewPassword123',
          confirmPassword: 'NewPassword123'
        })
        expect(result.isValid).toBe(true)
      })

      it('should reject weak passwords', () => {
        const result = validateResetPasswordForm({
          newPassword: 'weak',
          confirmPassword: 'weak'
        })
        expect(result.isValid).toBe(false)
      })

      it('should reject non-matching passwords', () => {
        const result = validateResetPasswordForm({
          newPassword: 'Password123',
          confirmPassword: 'Different123'
        })
        expect(result.isValid).toBe(false)
      })
    })

    // describe('validateReminderForm', () => {
    //   beforeEach(() => {
    //     vi.useFakeTimers()
    //     vi.setSystemTime(new Date('2025-01-01T10:00:00Z'))
    //   })

    //   it('should validate future reminder date/time', () => {
    //     const result = validateReminderForm({
    //       date: '2025-12-31',
    //       time: '14:30'
    //     })
    //     expect(result.isValid).toBe(true)
    //   })

    //   it('should reject empty date', () => {
    //     const result = validateReminderForm({
    //       date: '',
    //       time: '14:30'
    //     })
    //     expect(result.isValid).toBe(false)
    //     expect(result.errors.date).toBeDefined()
    //   })

    //   it('should reject empty time', () => {
    //     const result = validateReminderForm({
    //       date: '2025-12-31',
    //       time: ''
    //     })
    //     expect(result.isValid).toBe(false)
    //     expect(result.errors.time).toBeDefined()
    //   })

    //   it('should reject past reminder date/time', () => {
    //     const result = validateReminderForm({
    //       date: '2024-01-01',
    //       time: '10:00'
    //     })
    //     expect(result.isValid).toBe(false)
    //     expect(result.errors.time).toContain('futuro')
    //   })

    //   vi.useRealTimers()
    // })
  })

  describe('Password Strength Checker', () => {
    describe('checkPasswordStrength', () => {
      it('should rate strong passwords highly', () => {
        const result = checkPasswordStrength('StrongP@ssw0rd!')
        expect(result.score).toBeGreaterThanOrEqual(4)
        expect(result.isStrong).toBe(true)
      })

      it('should rate weak passwords low', () => {
        const result = checkPasswordStrength('weak')
        expect(result.score).toBeLessThan(4)
        expect(result.isStrong).toBe(false)
      })

      it('should provide helpful feedback for weak passwords', () => {
        const result = checkPasswordStrength('password')
        expect(result.feedback.length).toBeGreaterThan(0)
        expect(result.feedback.some(f => f.includes('maiuscola'))).toBe(true)
        expect(result.feedback.some(f => f.includes('numero'))).toBe(true)
      })

      it('should penalize repeated characters', () => {
        const noRepeat = checkPasswordStrength('Password7531')
        const withRepeat = checkPasswordStrength('Passsssword111')
        expect(noRepeat.score).toBeGreaterThan(withRepeat.score)
        expect(withRepeat.feedback.some(f => f.includes('ripetuti'))).toBe(true)
      })

      it('should penalize common sequences', () => {
        const result = checkPasswordStrength('Password123')
        expect(result.feedback.some(f => f.includes('sequenze comuni'))).toBe(true)
      })

      it('should reward longer passwords', () => {
        const short = checkPasswordStrength('Pass123!')
        const long = checkPasswordStrength('LongPassword123!')
        expect(long.score).toBeGreaterThanOrEqual(short.score)
      })

      it('should handle empty password', () => {
        const result = checkPasswordStrength('')
        expect(result.score).toBe(0)
        expect(result.isStrong).toBe(false)
        expect(result.feedback).toContain('Password è obbligatoria')
      })
    })
  })

  describe('Utility Functions', () => {
    describe('createValidator', () => {
      it('should combine multiple validators', () => {
        const validator = createValidator([
          rules.required,
          rules.minLength(3),
          rules.maxLength(10)
        ])

        expect(validator('test').isValid).toBe(true)
        expect(validator('').isValid).toBe(false)
        expect(validator('ab').isValid).toBe(false)
        expect(validator('12345678901').isValid).toBe(false)
      })

      it('should return first validation error', () => {
        const validator = createValidator([
          rules.required,
          rules.minLength(5)
        ])

        const result = validator('')
        expect(result.message).toBe('Questo campo è obbligatorio')
      })
    })

    describe('validateForm', () => {
      it('should validate all fields', () => {
        const result = validateForm(
          { name: 'Test', age: 25 },
          {
            name: rules.required,
            age: rules.minValue(18)
          }
        )
        expect(result.isValid).toBe(true)
      })

      it('should collect errors from all fields', () => {
        const result = validateForm(
          { name: '', age: 10 },
          {
            name: rules.required,
            age: rules.minValue(18)
          }
        )
        expect(result.isValid).toBe(false)
        expect(result.errors.name).toBeDefined()
        expect(result.errors.age).toBeDefined()
      })
    })
  })

  describe('Regex Patterns', () => {
    describe('patterns.email', () => {
      it('should match valid emails', () => {
        expect(patterns.email.test('user@example.com')).toBe(true)
        expect(patterns.email.test('test.user@example.co.uk')).toBe(true)
      })

      it('should not match invalid emails', () => {
        expect(patterns.email.test('invalid')).toBe(false)
        expect(patterns.email.test('@example.com')).toBe(false)
      })
    })

    describe('patterns.username', () => {
      it('should match valid usernames', () => {
        expect(patterns.username.test('user123')).toBe(true)
        expect(patterns.username.test('user_name')).toBe(true)
        expect(patterns.username.test('user.name')).toBe(true)
        expect(patterns.username.test('user-name')).toBe(true)
      })

      it('should not match invalid usernames', () => {
        expect(patterns.username.test('user name')).toBe(false)
        expect(patterns.username.test('user@name')).toBe(false)
      })
    })

    describe('patterns.time', () => {
      it('should match valid time formats', () => {
        expect(patterns.time.test('14:30')).toBe(true)
        expect(patterns.time.test('00:00')).toBe(true)
        expect(patterns.time.test('23:59')).toBe(true)
        expect(patterns.time.test('1:30')).toBe(true)
        expect(patterns.time.test('9:05')).toBe(true)
      })

      it('should not match invalid time formats', () => {
        expect(patterns.time.test('24:00')).toBe(false)
        expect(patterns.time.test('14:60')).toBe(false)
        expect(patterns.time.test('25:30')).toBe(false)
        expect(patterns.time.test('12:65')).toBe(false)
      })
    })

    describe('patterns.date', () => {
      it('should match valid date formats (ISO)', () => {
        expect(patterns.date.test('2025-01-01')).toBe(true)
        expect(patterns.date.test('2025-12-31')).toBe(true)
      })

      it('should not match invalid date formats', () => {
        expect(patterns.date.test('01-01-2025')).toBe(false)
        expect(patterns.date.test('2025/01/01')).toBe(false)
      })
    })
  })

  describe('Security Tests', () => {
    it('should prevent XSS in username validation', () => {
      const xssAttempts = [
        '<script>alert("xss")</script>',
        'user<script>alert(1)</script>',
        'user"onload="alert(1)',
        "user'onclick='alert(1)",
        'user<img src=x onerror=alert(1)>'
      ]

      xssAttempts.forEach(xss => {
        expect(authValidators.username(xss).isValid).toBe(false)
      })
    })

    it('should prevent SQL injection patterns in username', () => {
      const sqlInjectionAttempts = [
        "admin' OR '1'='1",
        'admin"--',
        "admin' DROP TABLE users--",
        '1 OR 1=1',
        "'; DELETE FROM users WHERE '1'='1"
      ]

      sqlInjectionAttempts.forEach(sql => {
        expect(authValidators.username(sql).isValid).toBe(false)
      })
    })

    it('should prevent path traversal in task title', () => {
      const pathTraversal = [
        '../../../etc/passwd',
        '..\\..\\..\\windows\\system32',
        'task/../../../secret'
      ]

      pathTraversal.forEach(path => {
        // Should be valid (no special validation for paths in title)
        // but sanitization should happen elsewhere
        const result = taskValidators.title(path)
        expect(result.isValid).toBe(true) // Title validation doesn't block this
      })
    })

    it('should enforce password complexity to prevent weak passwords', () => {
      const weakPasswords = [
        'password',
        '12345678',
        'abcdefgh',
        'Password',
        'password1',
        'PASSWORD1'
      ]

      weakPasswords.forEach(pwd => {
        expect(authValidators.password(pwd).isValid).toBe(false)
      })
    })

    it('should prevent overly long inputs (DoS prevention)', () => {
      const veryLongString = 'a'.repeat(10000)

      expect(authValidators.username(veryLongString).isValid).toBe(false)
      expect(authValidators.email(veryLongString + '@example.com').isValid).toBe(false)
      expect(taskValidators.title(veryLongString).isValid).toBe(false)
      expect(taskValidators.description(veryLongString).isValid).toBe(false)
    })

    it('should handle null bytes correctly', () => {
      const nullByteAttempts = [
        'user\0name',
        'admin\u0000',
        'test\x00user'
      ]

      nullByteAttempts.forEach(attempt => {
        expect(authValidators.username(attempt).isValid).toBe(false)
      })
    })

    it('should trim whitespace to prevent whitespace-only bypasses', () => {
      expect(authValidators.username('   ').isValid).toBe(false)
      expect(authValidators.email('   ').isValid).toBe(false)
      expect(taskValidators.title('   ').isValid).toBe(false)
    })

    it('should validate email length to prevent header injection', () => {
      const longEmail = 'a'.repeat(250) + '@example.com'
      expect(authValidators.email(longEmail).isValid).toBe(false)
    })
  })
})
