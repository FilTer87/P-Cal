# Project Scripts

Utility scripts for managing project versioning and releases.

## 📄 Available Scripts

### `update-version.sh`

Updates the project version across all project files.

**Files updated:**
- `VERSION` (single source of truth)
- `backend/pom.xml`
- `backend/src/main/java/com/privatecal/config/OpenApiConfig.java`
- `frontend/package.json`
- `frontend/src/utils/constants.ts`
- All `Dockerfile*` files

**Usage:**
```bash
# Show current version
./scripts/update-version.sh

# Update to specific version
./scripts/update-version.sh 1.0.0
./scripts/update-version.sh 2.1.0-beta
```

**Example:**
```bash
$ ./scripts/update-version.sh 1.0.0
╔════════════════════════════════════════════════════════╗
║   PROJECT VERSION UPDATE                               ║
╚════════════════════════════════════════════════════════╝

Current version: 0.9.0
New version: 1.0.0

Do you want to update the version? [y/N]: y

Updating version...

✓ Updated VERSION
✓ Updated backend/pom.xml
✓ Updated OpenApiConfig.java
✓ Updated frontend/package.json
✓ Updated frontend/src/utils/constants.ts
✓ Updated Dockerfile
✓ Updated Dockerfile.optimized
✓ Updated Dockerfile.buildkit

╔════════════════════════════════════════════════════════╗
║   VERSION UPDATED SUCCESSFULLY!                        ║
╚════════════════════════════════════════════════════════╝

Version: 0.9.0 → 1.0.0
```

---

### `release.sh`

Automated release management with Git integration.

**What it does:**
1. Updates version in all files
2. Creates a Git commit
3. Creates an annotated Git tag
4. Optionally pushes to remote

**Usage:**
```bash
# Increment patch version (0.9.0 → 0.9.1)
./scripts/release.sh patch

# Increment minor version (0.9.0 → 0.10.0)
./scripts/release.sh minor

# Increment major version (0.9.0 → 1.0.0)
./scripts/release.sh major

# Custom version
./scripts/release.sh 2.0.0-rc1

# Auto-push to remote
./scripts/release.sh minor --push
```

**Example:**
```bash
$ ./scripts/release.sh patch
╔════════════════════════════════════════════════════════╗
║   RELEASE MANAGER                                      ║
╚════════════════════════════════════════════════════════╝

Current version: 0.9.0
New version:     0.9.1

Proceed with release? [y/N]: y

🚀 Creating release...

[1/4] Updating version in files...
✓ Updated VERSION
✓ Updated backend/pom.xml
...

[2/4] Creating commit...
✓ Commit created

[3/4] Creating Git tag...
✓ Tag v0.9.1 created

[4/4] Push skipped (use --push to push to remote)

╔════════════════════════════════════════════════════════╗
║   RELEASE CREATED SUCCESSFULLY!                        ║
╚════════════════════════════════════════════════════════╝

📦 Release: v0.9.1
🏷️  Git Tag: v0.9.1
```

---

## 🎯 Best Practices

### Semantic Versioning

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** (X.0.0): Breaking changes, incompatible API changes
- **MINOR** (x.Y.0): New features, backward-compatible
- **PATCH** (x.y.Z): Bug fixes, backward-compatible

**Examples:**
- `1.0.0` - First stable release
- `1.1.0` - Added new feature
- `1.1.1` - Fixed bug
- `2.0.0` - Breaking changes
- `2.0.0-beta` - Pre-release version
- `2.0.0-rc1` - Release candidate

### Versioning Workflow

#### Standard Release Flow:
```bash
# 1. Make your changes and commit them
git add .
git commit -m "feat: add new feature"

# 2. Create release (automatically commits version changes and creates tag)
./scripts/release.sh minor

# 3. Push to remote (if not using --push flag)
git push origin main
git push origin v1.1.0
```

#### Quick Release with Auto-Push:
```bash
# Make changes
git add .
git commit -m "feat: add new feature"

# Release and push in one go
./scripts/release.sh minor --push
```

#### Custom Version Release:
```bash
# For pre-releases, release candidates, etc.
./scripts/release.sh 2.0.0-beta
./scripts/release.sh 2.0.0-rc1
./scripts/release.sh 1.5.0-alpha.1
```

### Git Tags

Tags created by `release.sh` are annotated and include:
- Release version
- Creation date
- Automated message

**View tags:**
```bash
git tag -l -n9  # List tags with messages
git show v1.0.0 # Show tag details
```

**Delete tag (if needed):**
```bash
git tag -d v1.0.0           # Delete locally
git push origin :v1.0.0     # Delete from remote
```

---

## 🔧 Customization

### Adding New Files to Version Update

To add more files to the version update process, edit `update-version.sh`:

```bash
# ============================================================================
# Your Custom File
# ============================================================================
if [ -f "$PROJECT_ROOT/path/to/your/file" ]; then
    sed -i "s|old_pattern_$CURRENT_VERSION|new_pattern_$NEW_VERSION|g" \
        "$PROJECT_ROOT/path/to/your/file"
    echo -e "${GREEN}✓ Updated your/file${NC}"
fi
```

### Integration with CI/CD

Example GitHub Actions workflow:

```yaml
name: Release

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to release (e.g., 1.0.0, minor, patch)'
        required: true

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Create Release
        run: |
          ./scripts/release.sh ${{ github.event.inputs.version }} --push

      - name: Build Docker Images
        run: docker-compose build
```

---

## 📚 Additional Resources

- [Semantic Versioning](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Git Tagging](https://git-scm.com/book/en/v2/Git-Basics-Tagging)

---

## 🐛 Troubleshooting

### Script not executable
```bash
chmod +x scripts/*.sh
```

### sed command errors on macOS
The scripts use GNU sed syntax. On macOS, install GNU sed:
```bash
brew install gnu-sed
# Then use gsed instead, or add to PATH
```

### Git tag already exists
```bash
# Delete existing tag
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0

# Create new tag
./scripts/release.sh 1.0.0
```

### Uncommitted changes warning
Either commit your changes first or use `git stash`:
```bash
git stash
./scripts/release.sh minor
git stash pop
```