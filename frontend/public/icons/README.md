# P-Cal Icons

Application icons for P-Cal (Private Calendar).

## 🎨 Design

The icon features:
- **Modern calendar grid** with gradient colors (indigo to purple)
- **Privacy shield** overlay with checkmark
- **Minimalist design** that works at all sizes
- **Brand colors**: `#6366f1` (indigo) to `#8b5cf6` (purple)

## 📁 Files

### Source
- `icon.svg` - Vector source file (scalable)

### Generated PNGs
- `icon-16x16.png` - Browser favicon
- `icon-32x32.png` - Browser favicon
- `icon-48x48.png` - Browser tab
- `icon-64x64.png` - Desktop shortcuts
- `icon-128x128.png` - App launcher
- `icon-192x192.png` - PWA icon, Apple touch icon
- `icon-256x256.png` - High-res displays
- `icon-512x512.png` - PWA splash screen

### Browser Icons
- `favicon.ico` - Multi-size ICO file (16, 32, 48px)

## 🔄 Regenerating Icons

To regenerate PNG icons from SVG source:

```bash
./generate-icons.sh
```

**Requirements:**
- ImageMagick: `sudo apt install imagemagick`
- OR Inkscape: `sudo apt install inkscape`

## 🚀 Usage

Icons are automatically loaded via `index.html`:

```html
<!-- Browser favicons -->
<link rel="icon" type="image/x-icon" href="/icons/favicon.ico" />
<link rel="icon" type="image/svg+xml" href="/icons/icon.svg" />

<!-- PWA icons -->
<link rel="manifest" href="/manifest.json" />
<link rel="apple-touch-icon" href="/icons/icon-192x192.png" />
```

The `manifest.json` file references all icon sizes for PWA support.

## ✏️ Editing

To modify the icon design:

1. Edit `icon.svg` with any SVG editor (Inkscape, Figma, Adobe Illustrator, etc.)
2. Run `./generate-icons.sh` to regenerate PNG files
3. Test the new icons by clearing browser cache

## 🎯 Design Guidelines

- **Keep it simple**: Icons should be recognizable at 16x16px
- **Use brand colors**: Maintain the indigo/purple gradient
- **Test at all sizes**: Verify readability from 16px to 512px
- **Maintain contrast**: Ensure visibility in both light and dark themes