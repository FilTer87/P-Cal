#!/bin/bash

# ============================================================================
# Icon Generation Script
# ============================================================================
# Generates PNG icons in various sizes from SVG source
#
# Requirements: ImageMagick or Inkscape
# Usage: ./generate-icons.sh
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SVG_SOURCE="$SCRIPT_DIR/icon.svg"

# Check if source SVG exists
if [ ! -f "$SVG_SOURCE" ]; then
    echo "❌ Error: icon.svg not found in $SCRIPT_DIR"
    exit 1
fi

echo "🎨 P-Cal Icon Generator"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check for available converter
if command -v inkscape &> /dev/null; then
    CONVERTER="inkscape"
    echo "✓ Using Inkscape for conversion"
elif command -v convert &> /dev/null; then
    CONVERTER="imagemagick"
    echo "✓ Using ImageMagick for conversion"
else
    echo "❌ Error: Neither Inkscape nor ImageMagick found"
    echo ""
    echo "Please install one of:"
    echo "  - Inkscape: sudo apt install inkscape"
    echo "  - ImageMagick: sudo apt install imagemagick"
    exit 1
fi

echo ""

# Icon sizes to generate
declare -a SIZES=("16" "32" "48" "64" "128" "192" "256" "512")

# Generate PNGs
for SIZE in "${SIZES[@]}"; do
    OUTPUT="$SCRIPT_DIR/icon-${SIZE}x${SIZE}.png"

    echo -n "Generating ${SIZE}x${SIZE}... "

    if [ "$CONVERTER" = "inkscape" ]; then
        inkscape "$SVG_SOURCE" \
            --export-type=png \
            --export-filename="$OUTPUT" \
            --export-width=$SIZE \
            --export-height=$SIZE \
            > /dev/null 2>&1
    else
        convert -background none \
            -resize ${SIZE}x${SIZE} \
            "$SVG_SOURCE" "$OUTPUT"
    fi

    if [ -f "$OUTPUT" ]; then
        echo "✓"
    else
        echo "✗ Failed"
    fi
done

# Generate favicon.ico (multi-size)
echo ""
echo -n "Generating favicon.ico... "
if [ "$CONVERTER" = "imagemagick" ]; then
    convert "$SVG_SOURCE" \
        -background none \
        -define icon:auto-resize=16,32,48 \
        "$SCRIPT_DIR/favicon.ico" 2>/dev/null
    echo "✓"
else
    echo "⊘ Skipped (requires ImageMagick)"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ Icon generation complete!"
echo ""
echo "Generated files:"
ls -lh "$SCRIPT_DIR"/*.png "$SCRIPT_DIR"/*.ico 2>/dev/null || true
echo ""