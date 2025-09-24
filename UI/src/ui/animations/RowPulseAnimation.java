package ui.animations;

import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Animation for pulsing instruction table rows when they are executed.
 * Creates a warm amber glow effect that fades in and out.
 */
public class RowPulseAnimation {
    private static final Duration PULSE_DURATION = Duration.millis(600);
    private static final double FADE_FROM = 0.6;
    private static final double FADE_TO = 1.0;

    /**
     * Apply a pulse animation to a table row node.
     * The animation adds a warm amber background and border that fades in and out.
     * 
     * @param row the table row node to animate
     */
    public static void pulseRow(Node row) {
        if (!Animations.isEnabled() || row == null) {
            return;
        }

        // Find the instruction table
        javafx.scene.Node instructionTable = row.getParent();
        while (instructionTable != null && !(instructionTable instanceof javafx.scene.control.TableView)) {
            instructionTable = instructionTable.getParent();
        }

        if (instructionTable == null) {
            return;
        }

        // Create a scale animation on the entire table
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(400), instructionTable);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.05);
        scaleTransition.setToY(1.05);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);

        // Create an opacity animation on the entire table
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(400), instructionTable);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.3);
        fadeTransition.setAutoReverse(true);
        fadeTransition.setCycleCount(2);

        // Combine both animations
        ParallelTransition parallelTransition = new ParallelTransition(scaleTransition, fadeTransition);

        // Play the animation
        Animations.playIfEnabled(parallelTransition);
    }

    /**
     * Apply a pulse animation with a custom duration.
     * 
     * @param row      the table row node to animate
     * @param duration the duration of the pulse animation
     */
    public static void pulseRow(Node row, Duration duration) {
        if (!Animations.isEnabled() || row == null) {
            return;
        }

        // Add the pulse style class
        row.getStyleClass().add("exec-pulse");

        // Create fade transition for the pulse effect
        FadeTransition fadeTransition = new FadeTransition(duration, row);
        fadeTransition.setFromValue(FADE_FROM);
        fadeTransition.setToValue(FADE_TO);
        fadeTransition.setAutoReverse(true);
        fadeTransition.setCycleCount(2);

        // Clean up the style class when animation finishes
        fadeTransition.setOnFinished(e -> {
            row.getStyleClass().remove("exec-pulse");
        });

        // Play the animation
        Animations.playIfEnabled(fadeTransition);
    }

    /**
     * Apply a pulse animation with custom fade values.
     * 
     * @param row      the table row node to animate
     * @param duration the duration of the pulse animation
     * @param fadeFrom the starting opacity value
     * @param fadeTo   the ending opacity value
     */
    public static void pulseRow(Node row, Duration duration, double fadeFrom, double fadeTo) {
        if (!Animations.isEnabled() || row == null) {
            return;
        }

        // Add the pulse style class
        row.getStyleClass().add("exec-pulse");

        // Create fade transition for the pulse effect
        FadeTransition fadeTransition = new FadeTransition(duration, row);
        fadeTransition.setFromValue(fadeFrom);
        fadeTransition.setToValue(fadeTo);
        fadeTransition.setAutoReverse(true);
        fadeTransition.setCycleCount(2);

        // Clean up the style class when animation finishes
        fadeTransition.setOnFinished(e -> {
            row.getStyleClass().remove("exec-pulse");
        });

        // Play the animation
        Animations.playIfEnabled(fadeTransition);
    }
}
