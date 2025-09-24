package ui;

import java.util.Arrays;
import java.util.List;

/**
 * Enum representing available themes for the S-Emulator application.
 * Each theme defines a set of CSS files to be applied.
 */
public enum Theme {
    LIGHT("Light", "/ui/styles/base.css", "/ui/styles/theme-light.css"),
    DARK("Dark", "/ui/styles/base.css", "/ui/styles/theme-dark.css"),
    CONTRAST("High Contrast", "/ui/styles/base.css", "/ui/styles/theme-contrast.css");

    private final String displayName;
    private final List<String> cssFiles;

    /**
     * Constructor for Theme enum.
     * 
     * @param displayName The human-readable name for the theme
     * @param cssFiles    The CSS files to load for this theme (in order)
     */
    Theme(String displayName, String... cssFiles) {
        this.displayName = displayName;
        this.cssFiles = Arrays.asList(cssFiles);
    }

    /**
     * Get the display name of the theme.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the list of CSS files for this theme.
     * 
     * @return List of CSS file paths
     */
    public List<String> getCssFiles() {
        return cssFiles;
    }

    /**
     * Get the default theme.
     * 
     * @return The default theme (LIGHT)
     */
    public static Theme getDefault() {
        return LIGHT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
