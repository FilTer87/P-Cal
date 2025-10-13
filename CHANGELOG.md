# Changelog
All significant changes to this project will be documented in this file.

The format will follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)  
This project adheres to [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html) starting from the first pre-release (v0.9.1-beta).

## [Unreleased]

### Added

- Spanish language (frontend translation, Notifications WIP)

### Changed

### Deprecated

### Removed

### Fixed

### Security

- updated happy-DOM dependency to fix RCE attacks issue (https://github.com/advisories/GHSA-37j7-fg3j-429f)

---

## [0.13.1-beta] - 2025-10-13
Small fixes and improvements

### Changed

- Close unsubmitted form only by "X" or "Cancel" buttons (Prevent to lost modified fields on accidentally clicks)

### Fixed

- Prevent reminders to be sent on update of past events (see recurring occurrences)
- Prevent shortcuts to change view while typing


---

## [0.13.0-beta] - 2025-10-13

### Added

- **Recurring Events** management following RFC 5545 (CalDAV compliance)
- Reminders send logic follows RRULE next occurrence on recurrent events
- Future occurrences retrieval and correct visualization
- Recurring event infos shown on task detail translated in human language
- Update "all occurrences" or "single occurrence" option

### Fixed

- Correctly display events after update (no need to change view or date-range)

### Security

---

## [0.12.0-beta] - 2025-10-9

### Added
- Management and correct visualization of **overlapping events**

### Changed
- in week view the overlapping tasks are grouped and layered, z-index ordered by start-time and duration (similar to Google calendar visualization)

---

## [0.11.0-beta] - 2025-10-07

### Added
- Comprehensive internationalization (i18n) support:
  - Full frontend internationalization with Italian (it-IT) and English (en-US) locales
  - Multilingual email templates using Thymeleaf template engine
  - Automatic locale detection from browser with manual override in user settings (persisted preference)
  - User's language applied to all email communications and notifications

- documentation for local run/development + application-local.yml example file
- automatic tests

### Changed
- Refactored email templates using Thymeleaf
- Multilanguage notification system, with shared translations between all the providers as NTFY (already implemented), Telegram, Slack, Discord, ... (future implementations)
- Optional Markdown support in notifications (FormatType.MARKDOWN)

### Deprecated

### Removed

### Fixed
- generate and update NTFY personal topic fix
- adapted tests to new implementations

### Security
Content Security Policy fix with unplugin-vue-i18n (prevent 'eval()')

## [0.10.1-beta] - 2025-10-07

### Added
- release.yml - github action test

---

## [0.10.0-beta] - 2025-10-03

### Added
- Email verification flow on user registration (Optional - enable/disable from env config)
- Frontend testing with vitest
- Minor fixes

### Changed
- if email verification enabled, no email update possible because of security reasons (additional specific flow is needed)

### Fixed
- minor fronted format and validation fix

---

## [0.9.3-beta] - 2025-10-01
P-Cal - first public version of the project.

### Added
- README.md (english version)
- CHANGELOG.md (this file)

### Changed
- markdown documentations files

### Notes
- This is a **pre-release**: all core features are already operational and there are no known critical bugs; email verification flow is missing in order to release v1.0.0

---

## [0.9.2-beta] - 2025-10-01

### Added
- User registration, login and profile management
- Security section for password change and 2FA enabling flow
- Full data export and account deletion (GDPR-friendly)
- Complete Calendar Management: multiple views (Month, Week, Day, and Agenda) and Event/task management
- Multi-channel notifications: currently implemented Email and NTFY server
- Multiple reminders per activity
- Flexible scheduling
- User preferences: theme, time zone, time format, first day of week, notifications

### Security
- Password hashing with BCrypt
- JWT auth with automatic refresh and configurable expirations
- complete server-side input validations
- Configurable Allowed CORS origins
- 2FA with TOTP

### Fixed
- Env variables in docker-compose.yml

### Notes
- This is a **pre-release**: all core features are already operational and there are no known critical bugs; email verification flow is missing in order to release v1.0.0