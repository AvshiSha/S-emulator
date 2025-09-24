package ui.animations;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Animation for variable/register cells when they are updated.
 * Creates a scale pop effect combined with a brief color wash.
 */
public class VariableBlipAnimation {
    private static final Duration SCALE_DURATION = Duration.millis(160);
    private static final Duration FADE_DURATION = Duration.millis(220);
    private static final double SCALE_FROM = 1.0;
    private static final double SCALE_TO = 1.06;
    private static final double FADE_FROM = 0.85;
    private static final double FADE_TO = 1.0;

    /**
     * Apply a blip animation to a variable cell node.
     * The animation combines a scale pop with a fade effect.
     * 
     * @param cell the cell node to animate
     */
    public static void blipCell(Node cell) {
        if (!Animations.isEnabled() || cell == null) {
            return;
        }

        // Create scale transition for the pop effect
        ScaleTransition scaleTransition = new ScaleTransition(SCALE_DURATION, cell);
        scaleTransition.setFromX(SCALE_FROM);
        scaleTransition.setFromY(SCALE_FROM);
        scaleTransition.setToX(SCALE_TO);
        scaleTransition.setToY(SCALE_TO);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);

        // Create fade transition for the color wash effect
        FadeTransition fadeTransition = new FadeTransition(FADE_DURATION, cell);
        fadeTransition.setFromValue(FADE_FROM);
        fadeTransition.setToValue(FADE_TO);

        // Combine both animations
        ParallelTransition parallelTransition = new ParallelTransition(scaleTransition, fadeTransition);

        // Play the animation
        Animations.playIfEnabled(parallelTransition);
    }

    /**
     * Apply a blip animation with custom scale values.
     * 
     * @param cell    the cell node to animate
     * @param scaleTo the scale factor to animate to
     */
    public static void blipCell(Node cell, double scaleTo) {
        if (!Animations.isEnabled() || cell == null) {
            return;
        }

        // Create scale transition for the pop effect
        ScaleTransition scaleTransition = new ScaleTransition(SCALE_DURATION, cell);
        scaleTransition.setFromX(SCALE_FROM);
        scaleTransition.setFromY(SCALE_FROM);
        scaleTransition.setToX(scaleTo);
        scaleTransition.setToY(scaleTo);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);

        // Create fade transition for the color wash effect
        FadeTransition fadeTransition = new FadeTransition(FADE_DURATION, cell);
        fadeTransition.setFromValue(FADE_FROM);
        fadeTransition.setToValue(FADE_TO);

        // Combine both animations
        ParallelTransition parallelTransition = new ParallelTransition(scaleTransition, fadeTransition);

        // Play the animation
        Animations.playIfEnabled(parallelTransition);
    }

    /**
     * Apply a blip animation with custom durations.
     * 
     * @param cell          the cell node to animate
     * @param scaleDuration the duration of the scale animation
     * @param fadeDuration  the duration of the fade animation
     */
    public static void blipCell(Node cell, Duration scaleDuration, Duration fadeDuration) {
        if (!Animations.isEnabled() || cell == null) {
            return;
        }

        // Create scale transition for the pop effect
        ScaleTransition scaleTransition = new ScaleTransition(scaleDuration, cell);
        scaleTransition.setFromX(SCALE_FROM);
        scaleTransition.setFromY(SCALE_FROM);
        scaleTransition.setToX(SCALE_TO);
        scaleTransition.setToY(SCALE_TO);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);

        // Create fade transition for the color wash effect
        FadeTransition fadeTransition = new FadeTransition(fadeDuration, cell);
        fadeTransition.setFromValue(FADE_FROM);
        fadeTransition.setToValue(FADE_TO);

        // Combine both animations
        ParallelTransition parallelTransition = new ParallelTransition(scaleTransition, fadeTransition);

        // Play the animation
        Animations.playIfEnabled(parallelTransition);
    }

    /**
     * Apply a blip animation with custom scale and fade values.
     * 
     * @param cell     the cell node to animate
     * @param scaleTo  the scale factor to animate to
     * @param fadeFrom the starting opacity value
     * @param fadeTo   the ending opacity value
     */
    public static void blipCell(Node cell, double scaleTo, double fadeFrom, double fadeTo) {
        if (!Animations.isEnabled() || cell == null) {
            return;
        }

        // Create scale transition for the pop effect
        ScaleTransition scaleTransition = new ScaleTransition(SCALE_DURATION, cell);
        scaleTransition.setFromX(SCALE_FROM);
        scaleTransition.setFromY(SCALE_FROM);
        scaleTransition.setToX(scaleTo);
        scaleTransition.setToY(scaleTo);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);

        // Create fade transition for the color wash effect
        FadeTransition fadeTransition = new FadeTransition(FADE_DURATION, cell);
        fadeTransition.setFromValue(fadeFrom);
        fadeTransition.setToValue(fadeTo);

        // Combine both animations
        ParallelTransition parallelTransition = new ParallelTransition(scaleTransition, fadeTransition);

        // Play the animation
        Animations.playIfEnabled(parallelTransition);
    }

    /**
     * Apply a subtle blip animation for less prominent updates.
     * Uses smaller scale and shorter duration.
     * 
     * @param cell the cell node to animate
     */
    public static void subtleBlipCell(Node cell) {
        if (!Animations.isEnabled() || cell == null) {
            return;
        }

        // Use smaller scale and shorter duration for subtle effect
        blipCell(cell, 1.03, 0.9, 1.0);
    }

    /**
     * Apply a strong blip animation for important updates.
     * Uses larger scale and longer duration.
     * 
     * @param cell the cell node to animate
     */
    public static void strongBlipCell(Node cell) {
        if (!Animations.isEnabled() || cell == null) {
            return;
        }

        // Use larger scale and longer duration for strong effect
        Duration longScaleDuration = Duration.millis(200);
        Duration longFadeDuration = Duration.millis(300);
        blipCell(cell, longScaleDuration, longFadeDuration);
    }
}
