/**
 * TypeScript types for i18n integration
 */

import 'vue-i18n'
import type { MessageSchema } from './index'

// Extend vue-i18n types for better autocomplete
declare module 'vue-i18n' {
  export interface DefineLocaleMessage extends MessageSchema {}
}
