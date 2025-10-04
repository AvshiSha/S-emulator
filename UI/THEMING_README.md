# S-Emulator Theming System

This document describes the theming system implemented for the S-Emulator JavaFX application.

## Overview

The theming system allows users to switch between different color schemes and font sizes at runtime. The system includes:

- **3 Theme Variants**: Light, Dark, and High Contrast
- **Font Size Control**: 12px, 14px, 16px, 18px options
- **Persistent Preferences**: User choices are saved and restored on application restart
- **Runtime Switching**: Themes can be changed without restarting the application

## Architecture

### Core Components

1. **Theme.java** - Enum defining available themes and their CSS files
2. **ThemeManager.java** - Singleton class managing theme switching and persistence
3. **CSS Files** - Styling definitions for each theme variant

### CSS Structure

```
UI/src/ui/styles/
├── base.css              # Shared styles across all themes
├── theme-light.css       # Light theme styles
├── theme-dark.css        # Dark theme styles
└── theme-contrast.css    # High contrast theme styles
```

### Theme Variants

#### Light Theme

- Background: White (#ffffff)
- Text: Dark gray (#222222)
- Buttons: Light gray with blue borders
- Font: Segoe UI

#### Dark Theme

- Background: Dark gray (#111111)
- Text: Light gray (#f2f2f2)
- Buttons: Dark gray with blue accents
- Font: Inter (fallback to Segoe UI)

#### High Contrast Theme

- Background: Black (#000000)
- Text: White (#ffffff)
- Buttons: White with yellow/green highlights
- Font: Arial (high contrast)

## Usage

### For Users

1. **Theme Selection**: Use the "Select Theme" dropdown in the header
2. **Font Size**: Use the "Font Size" dropdown in the header
3. **Persistence**: Your choices are automatically saved and restored

### For Developers

#### Applying Themes Programmatically

```java
// Get the theme manager instance
ThemeManager themeManager = ThemeManager.getInstance();

// Apply a specific theme
themeManager.applyTheme(scene, Theme.DARK);

// Set and save a theme
themeManager.setTheme(Theme.LIGHT);

// Apply font size
themeManager.setFontSize("16px");
```

#### Adding New Themes

1. Create a new CSS file in `UI/src/ui/styles/`
2. Add the theme to the `Theme` enum
3. Update the CSS file list in the enum constructor

#### CSS Styling Guidelines

- Use CSS classes instead of inline styles
- Target the `.app-root` class for global background changes
- Use semantic class names (e.g., `.load-button`, `.status-label`)
- Ensure good contrast ratios for accessibility

## Implementation Details

### Theme Manager Features

- **Singleton Pattern**: Ensures single instance across the application
- **Preferences API**: Uses Java's Preferences API for persistence
- **Scene Management**: Handles CSS application to JavaFX scenes
- **Font Size Control**: Dynamic font size adjustment

### CSS Loading Strategy

1. Base styles are always loaded first
2. Theme-specific styles override base styles
3. Font size is applied via inline styles on the root node
4. All styles are applied immediately without restart

### Persistence

User preferences are stored using Java's Preferences API:

- **Location**: User's system preferences
- **Keys**: `theme` and `font_size`
- **Format**: Theme name as string, font size as CSS value

## Testing

To test the theming system:

1. Launch the application
2. Use the theme selector to switch between themes
3. Use the font size selector to change text size
4. Restart the application to verify persistence
5. Verify all UI elements respond to theme changes

## Future Enhancements

Potential improvements to the theming system:

1. **Custom Themes**: Allow users to create custom color schemes
2. **Theme Import/Export**: Share themes between users
3. **More Font Options**: Additional font families and sizes
4. **Component-Specific Themes**: Different themes for different UI sections
5. **Accessibility Features**: High contrast mode improvements
6. **Theme Preview**: Preview themes before applying

## Troubleshooting

### Common Issues

1. **Styles Not Applying**: Ensure CSS files are in the correct resource path
2. **Font Size Not Working**: Check that the root node has the style applied
3. **Preferences Not Saving**: Verify write permissions for user preferences
4. **Theme Not Persisting**: Check that the theme manager is properly initialized

### Debug Tips

- Check the browser developer tools (if using WebView) for CSS issues
- Verify CSS file paths are correct in the Theme enum
- Ensure all UI components use CSS classes instead of inline styles
- Test theme switching with different UI states (loaded/unloaded programs)
