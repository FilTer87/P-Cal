#!/bin/bash

# ============================================================================
# Version Update Script
# ============================================================================
#
# This script updates the project version across all project files:
# - VERSION file (single source of truth)
# - Backend: pom.xml, OpenApiConfig.java
# - Frontend: package.json, constants.ts
# - Dockerfiles
#
# Usage: ./scripts/update-version.sh [new_version]
#
# Examples:
#   ./scripts/update-version.sh 1.0.0
#   ./scripts/update-version.sh 1.0.0-beta
#
# If no version is specified, it displays the current version
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

# Function to validate version format (semantic versioning)
validate_version() {
    local version=$1
    # Format: X.Y.Z or X.Y.Z-suffix (e.g., 1.0.0-beta, 2.1.0-rc1)
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$ ]]; then
        echo -e "${RED}❌ Invalid version format: $version${NC}"
        echo -e "${YELLOW}Use semantic versioning: X.Y.Z or X.Y.Z-suffix${NC}"
        echo -e "${YELLOW}Examples: 1.0.0, 2.1.3, 1.0.0-beta, 2.0.0-rc1${NC}"
        return 1
    fi
    return 0
}

# Banner
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   PROJECT VERSION UPDATE                               ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

CURRENT_VERSION=$(get_current_version)
echo -e "${BLUE}Current version: ${GREEN}$CURRENT_VERSION${NC}"
echo ""

# If no version is provided, show only current version
if [ $# -eq 0 ]; then
    echo -e "${YELLOW}To update the version, run:${NC}"
    echo -e "${YELLOW}  ./scripts/update-version.sh <new_version>${NC}"
    echo ""
    echo -e "${YELLOW}Example:${NC}"
    echo -e "${YELLOW}  ./scripts/update-version.sh 1.0.0${NC}"
    exit 0
fi

NEW_VERSION=$1

# Validate new version format
if ! validate_version "$NEW_VERSION"; then
    exit 1
fi

echo -e "${YELLOW}New version: ${GREEN}$NEW_VERSION${NC}"
echo ""

# Confirmation prompt
read -p "$(echo -e ${GREEN}Do you want to update the version? [y/N]:${NC} )" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Operation cancelled.${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}Updating version...${NC}"
echo ""

# Update VERSION file (single source of truth)
echo "$NEW_VERSION" > "$VERSION_FILE"
echo -e "${GREEN}✓ Updated VERSION${NC}"

# ============================================================================
# Backend - pom.xml
# ============================================================================
if [ -f "$PROJECT_ROOT/backend/pom.xml" ]; then
    sed -i "s|<version>$CURRENT_VERSION</version>|<version>$NEW_VERSION</version>|g" "$PROJECT_ROOT/backend/pom.xml"
    echo -e "${GREEN}✓ Updated backend/pom.xml${NC}"
fi

# ============================================================================
# Backend - OpenApiConfig.java (Swagger/OpenAPI version)
# ============================================================================
if [ -f "$PROJECT_ROOT/backend/src/main/java/com/privatecal/config/OpenApiConfig.java" ]; then
    # Handles both v0.9.0 and 0.9.0 formats
    sed -i "s|\.version(\"v*$CURRENT_VERSION\")|.version(\"v$NEW_VERSION\")|g" "$PROJECT_ROOT/backend/src/main/java/com/privatecal/config/OpenApiConfig.java"
    echo -e "${GREEN}✓ Updated OpenApiConfig.java${NC}"
fi

# ============================================================================
# Frontend - package.json
# ============================================================================
if [ -f "$PROJECT_ROOT/frontend/package.json" ]; then
    sed -i "s|\"version\": \"$CURRENT_VERSION\"|\"version\": \"$NEW_VERSION\"|g" "$PROJECT_ROOT/frontend/package.json"
    echo -e "${GREEN}✓ Updated frontend/package.json${NC}"
fi

# ============================================================================
# Frontend - constants.ts (application version constant)
# ============================================================================
if [ -f "$PROJECT_ROOT/frontend/src/utils/constants.ts" ]; then
    sed -i "s|version: '$CURRENT_VERSION'|version: '$NEW_VERSION'|g" "$PROJECT_ROOT/frontend/src/utils/constants.ts"
    echo -e "${GREEN}✓ Updated frontend/src/utils/constants.ts${NC}"
fi

# ============================================================================
# Dockerfiles (all Dockerfile variants)
# ============================================================================
for dockerfile in "$PROJECT_ROOT/backend/Dockerfile"* "$PROJECT_ROOT/frontend/Dockerfile"*; do
    if [ -f "$dockerfile" ]; then
        sed -i "s|version=\"$CURRENT_VERSION\"|version=\"$NEW_VERSION\"|g" "$dockerfile"
        echo -e "${GREEN}✓ Updated $(basename $dockerfile)${NC}"
    fi
done

# ============================================================================
# Database demo data script (if exists)
# ============================================================================
if [ -f "$PROJECT_ROOT/database/populate_demo_data.sh" ]; then
    sed -i "s|-- Generation date:.*|-- Generation date: $(date)|g" "$PROJECT_ROOT/database/populate_demo_data.sh"
    echo -e "${GREEN}✓ Updated database/populate_demo_data.sh${NC}"
fi

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   VERSION UPDATED SUCCESSFULLY!                        ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Version: $CURRENT_VERSION → $NEW_VERSION${NC}"
echo ""
echo -e "${YELLOW}📋 Suggested next steps:${NC}"
echo -e "${YELLOW}1. Review the changes:${NC}"
echo -e "   ${BLUE}git diff${NC}"
echo ""
echo -e "${YELLOW}2. Commit the changes:${NC}"
echo -e "   ${BLUE}git add -A${NC}"
echo -e "   ${BLUE}git commit -m \"chore: bump version to $NEW_VERSION\"${NC}"
echo ""
echo -e "${YELLOW}3. Create a Git tag:${NC}"
echo -e "   ${BLUE}git tag -a v$NEW_VERSION -m \"Release v$NEW_VERSION\"${NC}"
echo -e "   ${BLUE}git push origin v$NEW_VERSION${NC}"
echo ""
echo -e "${YELLOW}4. (Optional) Rebuild containers:${NC}"
echo -e "   ${BLUE}docker-compose build${NC}  # or podman-compose"
echo ""