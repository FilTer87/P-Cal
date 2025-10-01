# Changelog
All significant changes to this project will be documented in this file.

The format will follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)  
This project adheres to [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html) starting from the first pre-release (v0.9.1-beta).

## [Unreleased]

### Added
- Email verification flow on user registration

### Changed

### Deprecated

### Removed

### Fixed

### Security


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