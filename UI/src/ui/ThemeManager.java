package ui;

import javafx.scene.Scene;
import javafx.scene.Parent;
import java.util.prefs.Preferences;

/**
 * Manages theme switching and persistence for the S-Emulator application.
 * Handles applying themes to scenes and saving/loading user preferences.
 */
public class ThemeManager {
    private static final String PREF_KEY_THEME = "theme";
    private static final String PREF_KEY_FONT_SIZE = "font_size";

    private static ThemeManager instance;
    private Theme currentTheme;
    private String currentFontSize;
    private Preferences preferences;

    /**
     * Private constructor for singleton pattern.
     */
    private ThemeManager() {
        this.preferences = Preferences.userNodeForPackage(ThemeManager.class);
        loadPreferences();
    }

    /**
     * Get the singleton instance of ThemeManager.
     * 
     * @return The ThemeManager instance
     */
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * Apply a theme to a scene.
     * 
     * @param scene The scene to apply the theme to
     * @param theme The theme to apply
     */
    public void applyTheme(Scene scene, Theme theme) {
        if (scene == null || theme == null) {
            return;
        }

        // Clear existing stylesheets
        scene.getStylesheets().clear();

        // Apply the theme's CSS files
        for (String cssFile : theme.getCssFiles()) {
            String cssUrl = getClass().getResource(cssFile).toExternalForm();
            scene.getStylesheets().add(cssUrl);
        }

        // Apply font size if set
        if (currentFontSize != null && !currentFontSize.isEmpty()) {
            // Force font size by setting it on the root and all child nodes
            applyFontSizeToNode(scene.getRoot(), currentFontSize);
        }

        this.currentTheme = theme;
    }

    /**
     * Apply the current theme to a scene.
     * 
     * @param scene The scene to apply the theme to
     */
    public void applyCurrentTheme(Scene scene) {
        applyTheme(scene, currentTheme);
    }

    /**
     * Set the current theme and save it to preferences.
     * 
     * @param theme The theme to set
     */
    public void setTheme(Theme theme) {
        if (theme != null) {
            this.currentTheme = theme;
            savePreferences();
        }
    }

    /**
     * Get the current theme.
     * 
     * @return The current theme
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Set the font size and save it to preferences.
     * 
     * @param fontSize The font size (e.g., "12px", "14px", "16px")
     */
    public void setFontSize(String fontSize) {
        this.currentFontSize = fontSize;
        savePreferences();
    }

    /**
     * Get the current font size.
     * 
     * @return The current font size
     */
    public String getCurrentFontSize() {
        return currentFontSize;
    }

    /**
     * Apply font size to a scene's root node.
     * 
     * @param scene The scene to apply font size to
     */
    public void applyFontSize(Scene scene) {
        if (scene != null && currentFontSize != null && !currentFontSize.isEmpty()) {
            // Force font size by reapplying the entire theme
            applyCurrentTheme(scene);
        }
    }

    /**
     * Recursively apply font size to a node and all its children.
     * 
     * @param node     The node to apply font size to
     * @param fontSize The font size to apply
     */
    private void applyFontSizeToNode(javafx.scene.Node node, String fontSize) {
        if (node == null)
            return;

        // Apply font size to the current node
        String currentStyle = node.getStyle();
        // Remove any existing font-size declarations
        currentStyle = currentStyle.replaceAll("-fx-font-size:[^;]*;?", "");
        // Add the new font size
        node.setStyle(currentStyle + "-fx-font-size: " + fontSize + " !important;");

        // Apply to all children if this is a parent node
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                applyFontSizeToNode(child, fontSize);
            }
        }
    }

    /**
     * Load preferences from the system preferences store.
     */
    private void loadPreferences() {
        try {
            String themeName = preferences.get(PREF_KEY_THEME, Theme.getDefault().name());
            this.currentTheme = Theme.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            // If the saved theme is invalid, use the default
            this.currentTheme = Theme.getDefault();
        }

        this.currentFontSize = preferences.get(PREF_KEY_FONT_SIZE, "12px");
    }

    /**
     * Save current preferences to the system preferences store.
     */
    private void savePreferences() {
        if (currentTheme != null) {
            preferences.put(PREF_KEY_THEME, currentTheme.name());
        }
        if (currentFontSize != null) {
            preferences.put(PREF_KEY_FONT_SIZE, currentFontSize);
        }
    }

    /**
     * Reset all preferences to default values.
     */
    public void resetToDefaults() {
        this.currentTheme = Theme.getDefault();
        this.currentFontSize = "12px";
        savePreferences();
    }
}
