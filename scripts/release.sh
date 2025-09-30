#!/bin/bash

# ============================================================================
# Project Release Script
# ============================================================================
#
# This script automates the release process:
# 1. Updates version across all project files
# 2. Creates a versioning commit
# 3. Creates an annotated Git tag
# 4. Optionally pushes to remote
#
# Usage: ./scripts/release.sh <release_type> [OPTIONS]
#
# Release types (Semantic Versioning):
#   major - Increment X.0.0 (breaking changes)
#   minor - Increment x.Y.0 (new features)
#   patch - Increment x.y.Z (bug fixes)
#   [custom_version] - Specify custom version (e.g., 2.0.0-beta)
#
# Options:
#   --push              Push commit and tag to remote
#   -m, --message       Custom commit message
#   -f, --message-file  Read commit message from file
#
# Examples:
#   ./scripts/release.sh patch                    # 0.9.0 → 0.9.1
#   ./scripts/release.sh minor --push             # Increment and push to origin
#   ./scripts/release.sh 1.0.0-rc1                # → 1.0.0-rc1
#   ./scripts/release.sh patch -m "Custom msg"    # With custom message
#   ./scripts/release.sh patch -f commit_msg.txt  # Message from file
#
# ============================================================================

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Determine script and project directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSION_FILE="$PROJECT_ROOT/VERSION"

# Function to read current version from VERSION file
get_current_version() {
    if [ -f "$VERSION_FILE" ]; then
        cat "$VERSION_FILE"
    else
        echo "0.0.0"
    fi
}

# Function to increment version based on semantic versioning
increment_version() {
    local version=$1
    local type=$2

    # Remove any suffix (-beta, -rc1, etc)
    local base_version=$(echo "$version" | sed 's/-.*$//')

    # Split into major.minor.patch
    IFS='.' read -r major minor patch <<< "$base_version"

    case $type in
        major)
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        minor)
            minor=$((minor + 1))
            patch=0
            ;;
        patch)
            patch=$((patch + 1))
            ;;
        *)
            echo -e "${RED}Invalid release type: $type${NC}"
            return 1
            ;;
    esac

    echo "$major.$minor.$patch"
}

# Function to check Git repository status
check_git_status() {
    if [ ! -d "$PROJECT_ROOT/.git" ]; then
        echo -e "${RED}❌ Error: Not a Git repository${NC}"
        exit 1
    fi

    # Check for uncommitted changes
    if ! git diff-index --quiet HEAD --; then
        echo -e "${YELLOW}⚠️  Warning: You have uncommitted changes${NC}"
        echo ""
        git status --short
        echo ""
        read -p "$(echo -e ${YELLOW}Do you want to continue? [y/N]:${NC} )" -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# Banner
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   RELEASE MANAGER                                      ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check parameters
if [ $# -eq 0 ]; then
    echo -e "${RED}❌ Error: Please specify release type${NC}"
    echo ""
    echo -e "${YELLOW}Usage: $0 <release_type> [OPTIONS]${NC}"
    echo ""
    echo -e "${YELLOW}Release types:${NC}"
    echo -e "  ${GREEN}major${NC}  - Increment X.0.0 (breaking changes)"
    echo -e "  ${GREEN}minor${NC}  - Increment x.Y.0 (new features)"
    echo -e "  ${GREEN}patch${NC}  - Increment x.y.Z (bug fixes)"
    echo -e "  ${GREEN}X.Y.Z${NC}  - Custom version"
    echo ""
    echo -e "${YELLOW}Options:${NC}"
    echo -e "  ${GREEN}--push${NC}              Push commit and tag to remote"
    echo -e "  ${GREEN}-m, --message${NC}       Custom commit message"
    echo -e "  ${GREEN}-f, --message-file${NC}  Read commit message from file"
    echo ""
    exit 1
fi

RELEASE_TYPE=$1
SHOULD_PUSH=false
CUSTOM_MESSAGE=""
MESSAGE_FILE=""

# Parse arguments
shift
while [[ $# -gt 0 ]]; do
    case $1 in
        --push)
            SHOULD_PUSH=true
            shift
            ;;
        -m|--message)
            CUSTOM_MESSAGE="$2"
            shift 2
            ;;
        -f|--message-file)
            MESSAGE_FILE="$2"
            shift 2
            ;;
        *)
            echo -e "${RED}❌ Error: Unknown option $1${NC}"
            exit 1
            ;;
    esac
done

CURRENT_VERSION=$(get_current_version)
echo -e "${BLUE}Current version: ${GREEN}$CURRENT_VERSION${NC}"

# Calculate new version
case $RELEASE_TYPE in
    major|minor|patch)
        NEW_VERSION=$(increment_version "$CURRENT_VERSION" "$RELEASE_TYPE")
        ;;
    *)
        # Custom version
        NEW_VERSION=$RELEASE_TYPE
        ;;
esac

echo -e "${BLUE}New version:     ${GREEN}$NEW_VERSION${NC}"
echo ""

# Verify Git status
check_git_status

# Confirmation prompt
read -p "$(echo -e ${YELLOW}Proceed with release? [y/N]:${NC} )" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Operation cancelled.${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}🚀 Creating release...${NC}"
echo ""

# Step 1: Update version
echo -e "${YELLOW}[1/4] Updating version in files...${NC}"
"$SCRIPT_DIR/update-version.sh" "$NEW_VERSION" <<< "y"

# Step 2: Commit
echo ""
echo -e "${YELLOW}[2/4] Creating commit...${NC}"
git add -A

# Determine commit message
if [ -n "$MESSAGE_FILE" ]; then
    # Read from file
    if [ ! -f "$MESSAGE_FILE" ]; then
        echo -e "${RED}❌ Error: Message file not found: $MESSAGE_FILE${NC}"
        exit 1
    fi
    git commit -F "$MESSAGE_FILE"
    echo -e "${GREEN}✓ Commit created (from file)${NC}"
elif [ -n "$CUSTOM_MESSAGE" ]; then
    # Use custom message
    git commit -m "$CUSTOM_MESSAGE"
    echo -e "${GREEN}✓ Commit created (custom message)${NC}"
else
    # Default message
    git commit -m "chore: bump version to $NEW_VERSION" -m "Release v$NEW_VERSION"
    echo -e "${GREEN}✓ Commit created${NC}"
fi

# Step 3: Tag
echo ""
echo -e "${YELLOW}[3/4] Creating Git tag...${NC}"
git tag -a "v$NEW_VERSION" -m "Release v$NEW_VERSION

## Changes
- Version bump to $NEW_VERSION

See CHANGELOG.md for detailed changes."
echo -e "${GREEN}✓ Tag v$NEW_VERSION created${NC}"

# Step 4: Push (optional)
if [ "$SHOULD_PUSH" = true ]; then
    echo ""
    echo -e "${YELLOW}[4/4] Pushing to origin...${NC}"

    # Get current branch
    CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

    echo -e "${BLUE}Pushing commit to branch $CURRENT_BRANCH...${NC}"
    git push origin "$CURRENT_BRANCH"

    echo -e "${BLUE}Pushing tag v$NEW_VERSION...${NC}"
    git push origin "v$NEW_VERSION"

    echo -e "${GREEN}✓ Push completed${NC}"
else
    echo ""
    echo -e "${YELLOW}[4/4] Push skipped (use --push to push to remote)${NC}"
fi

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   RELEASE CREATED SUCCESSFULLY!                        ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}📦 Release: v$NEW_VERSION${NC}"
echo -e "${BLUE}🏷️  Git Tag: v$NEW_VERSION${NC}"
echo ""

if [ "$SHOULD_PUSH" = false ]; then
    echo -e "${YELLOW}📋 To push the release to origin:${NC}"
    echo -e "   ${BLUE}git push origin $(git rev-parse --abbrev-ref HEAD)${NC}"
    echo -e "   ${BLUE}git push origin v$NEW_VERSION${NC}"
    echo ""
fi

echo -e "${YELLOW}📋 Suggested next steps:${NC}"
echo -e "1. Verify the release on GitHub/GitLab"
echo -e "2. Update CHANGELOG.md with detailed changes"
echo -e "3. Rebuild and deploy containers:"
echo -e "   ${BLUE}docker-compose build${NC}  # or podman-compose"
echo -e "   ${BLUE}docker-compose up -d${NC}  # or podman-compose"
echo ""