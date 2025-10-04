package ui.animations;

import java.util.prefs.Preferences;

/**
 * Global animation control system for the S-Emulator.
 * Provides centralized control over all animations with persistence and
 * accessibility support.
 */
public final class Animations {
    private static volatile boolean enabled = true;
    private static final String PREF_KEY = "animations_enabled";
    private static final Preferences prefs = Preferences.userNodeForPackage(Animations.class);

    // Load animation preference on class initialization
    static {
        enabled = prefs.getBoolean(PREF_KEY, true);
    }

    /**
     * Check if animations are currently enabled.
     * 
     * @return true if animations are enabled, false otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable animations globally.
     * This setting is persisted and will be remembered across application restarts.
     * 
     * @param enabled true to enable animations, false to disable
     */
    public static void setEnabled(boolean enabled) {
        Animations.enabled = enabled;
        prefs.putBoolean(PREF_KEY, enabled);
    }

    /**
     * Toggle the current animation state.
     * 
     * @return the new animation state
     */
    public static boolean toggle() {
        setEnabled(!enabled);
        return enabled;
    }

    /**
     * Check if animations should be disabled due to system accessibility
     * preferences.
     * This respects the "reduce motion" system preference when available.
     * 
     * @return true if animations should be disabled for accessibility
     */
    public static boolean shouldDisableForAccessibility() {
        // Check for system accessibility preferences
        // In a real implementation, this would check system settings
        // For now, we'll just return false as we don't have access to system prefs
        return false;
    }

    /**
     * Get the current animation state as a human-readable string.
     * 
     * @return "Enabled" or "Disabled"
     */
    public static String getStatus() {
        return enabled ? "Enabled" : "Disabled";
    }

    /**
     * Utility method to execute an animation only if animations are enabled.
     * 
     * @param animation the animation to potentially execute
     */
    public static void playIfEnabled(javafx.animation.Animation animation) {
        if (isEnabled() && animation != null) {
            animation.play();
        }
    }

    /**
     * Utility method to execute multiple animations in parallel only if animations
     * are enabled.
     * 
     * @param animations the animations to potentially execute
     */
    public static void playIfEnabled(javafx.animation.Animation... animations) {
        if (isEnabled() && animations != null && animations.length > 0) {
            for (javafx.animation.Animation animation : animations) {
                if (animation != null) {
                    animation.play();
                }
            }
        }
    }
}
