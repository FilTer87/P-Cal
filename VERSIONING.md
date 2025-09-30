# Project Versioning Guide

This project follows **Semantic Versioning 2.0.0** ([semver.org](https://semver.org/)).

## Version Format

```
MAJOR.MINOR.PATCH[-PRERELEASE]
```

Example versions:
- `0.9.0` - Beta/pre-release
- `1.0.0` - First stable release
- `1.2.3` - Stable release
- `2.0.0-beta` - Pre-release version
- `2.0.0-rc1` - Release candidate

---

## Version Components

### MAJOR Version (X.0.0)

Increment when you make **incompatible API changes** or **breaking changes**.

**Examples:**
- Removing or renaming API endpoints
- Changing database schema in non-backward-compatible way
- Removing deprecated features
- Major architecture changes

### MINOR Version (x.Y.0)

Increment when you add **new features** in a **backward-compatible** manner.

**Examples:**
- Adding new API endpoints
- Adding new features to existing functionality
- Adding optional parameters
- Performance improvements

### PATCH Version (x.y.Z)

Increment for **backward-compatible bug fixes**.

**Examples:**
- Bug fixes
- Security patches
- Documentation updates
- Dependency updates (non-breaking)

### Pre-release Versions

Used for versions not yet ready for production:

- `X.Y.Z-alpha` - Alpha release (early testing)
- `X.Y.Z-beta` - Beta release (feature-complete, testing)
- `X.Y.Z-rc1` - Release candidate (potentially final)

---

## Quick Reference

| Change Type | Example | Command |
|-------------|---------|---------|
| Bug fix | 1.0.0 → 1.0.1 | `./scripts/release.sh patch` |
| New feature | 1.0.0 → 1.1.0 | `./scripts/release.sh minor` |
| Breaking change | 1.9.0 → 2.0.0 | `./scripts/release.sh major` |
| Pre-release | - → 2.0.0-beta | `./scripts/release.sh 2.0.0-beta` |

---

## Automated Versioning

This project includes automated scripts for version management:

### Manual Version Update
```bash
# Update version across all files
./scripts/update-version.sh 1.2.3
```

### Full Release Process
```bash
# Create release with Git tag
./scripts/release.sh minor

# Or with auto-push
./scripts/release.sh minor --push
```

See [`scripts/README.md`](./scripts/README.md) for detailed documentation.

---

## Version Files

The project version is stored in multiple files:

| File | Purpose |
|------|---------|
| `VERSION` | Single source of truth |
| `backend/pom.xml` | Maven project version |
| `backend/src/.../OpenApiConfig.java` | API documentation version |
| `frontend/package.json` | NPM package version |
| `frontend/src/utils/constants.ts` | Frontend app version |
| `**/Dockerfile*` | Docker image labels |

**Important:** Always use the provided scripts to update versions. Manual updates across multiple files are error-prone.

---

## Git Tags

Each release should be tagged in Git:

```bash
# Automatic (done by release.sh)
./scripts/release.sh minor

# Manual
git tag -a v1.2.0 -m "Release v1.2.0"
git push origin v1.2.0
```

**Tag naming convention:** `v{MAJOR}.{MINOR}.{PATCH}[-{PRERELEASE}]`

Examples:
- `v1.0.0`
- `v1.2.3`
- `v2.0.0-beta`
- `v2.0.0-rc1`

---

## Release Workflow

### Development → Stable Release

```
0.1.0 (alpha)
  ↓
0.9.0 (beta)
  ↓
1.0.0-rc1 (release candidate)
  ↓
1.0.0 (stable)
```

### After 1.0.0 Release

```
1.0.0 (stable)
  ↓
1.0.1 (bug fixes)
1.1.0 (new features)
1.2.0 (more features)
  ↓
2.0.0-beta (next major, pre-release)
  ↓
2.0.0 (stable with breaking changes)
```

---

## Best Practices

### ✅ DO

- Use semantic versioning consistently
- Tag every release in Git
- Update CHANGELOG.md for each release
- Test thoroughly before incrementing major version
- Use pre-release versions for testing
- Automate version updates with provided scripts

### ❌ DON'T

- Skip versions (1.0.0 → 1.0.3 without 1.0.1, 1.0.2)
- Use inconsistent version formats
- Manually edit version in files (use scripts instead)
- Release breaking changes as minor/patch versions
- Reuse version numbers
- Delete or modify existing release tags

---

## Examples

### Bug Fix Release
```bash
# Current: 1.2.0
# Fix: Security vulnerability patched

./scripts/release.sh patch
# New: 1.2.1
```

### Feature Release
```bash
# Current: 1.2.1
# New: Added email notifications

./scripts/release.sh minor
# New: 1.3.0
```

### Breaking Changes
```bash
# Current: 1.9.5
# Change: New API structure, incompatible with v1

./scripts/release.sh major
# New: 2.0.0
```

### Pre-release
```bash
# Current: 1.5.0
# Working on: Major refactoring (v2.0.0)

./scripts/release.sh 2.0.0-alpha
./scripts/release.sh 2.0.0-beta
./scripts/release.sh 2.0.0-rc1
./scripts/release.sh 2.0.0  # Final release
```

---

## Integration with CI/CD

Version tags can trigger automated builds and deployments:

```yaml
# Example: GitHub Actions
on:
  push:
    tags:
      - 'v*.*.*'
```

This allows:
- Automated testing on release
- Docker image building with version tags
- Automatic deployment to production
- Release notes generation

---

## References

- [Semantic Versioning 2.0.0](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Keep a Changelog](https://keepachangelog.com/)

---

**Last Updated:** 2025-09-30