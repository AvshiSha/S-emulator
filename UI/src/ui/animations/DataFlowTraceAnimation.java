package ui.animations;

import javafx.animation.Interpolator;
import javafx.animation.PathTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.util.Duration;

/**
 * Animation for tracing data flow between UI components.
 * Shows a small dot traveling along a path to visualize data movement.
 */
public class DataFlowTraceAnimation {
    private static final Duration TRACE_DURATION = Duration.millis(900);
    private static final double TOKEN_RADIUS = 3.0;
    private static final Color TOKEN_COLOR = Color.web("#4FC3F7"); // Cyan
    private static final Color PATH_COLOR = Color.web("#81D4FA"); // Light cyan
    private static final double PATH_WIDTH = 1.25;
    private static final double PATH_OPACITY = 0.8;

    /**
     * Create and play a data flow trace animation between two points.
     * 
     * @param startPoint   the starting point of the trace
     * @param endPoint     the ending point of the trace
     * @param overlayLayer the pane to add the animation elements to
     */
    public static void traceFlow(Point2D startPoint, Point2D endPoint, Pane overlayLayer) {
        if (!Animations.isEnabled() || overlayLayer == null || startPoint == null || endPoint == null) {
            return;
        }

        // Create the path
        Path path = createPath(startPoint, endPoint);
        path.setStroke(PATH_COLOR);
        path.setStrokeWidth(PATH_WIDTH);
        path.setOpacity(PATH_OPACITY);
        path.getStyleClass().add("flow-path");

        // Create the token (small circle)
        Circle token = new Circle(TOKEN_RADIUS, TOKEN_COLOR);
        token.getStyleClass().add("flow-token");

        // Add elements to overlay layer
        overlayLayer.getChildren().addAll(path, token);

        // Create path transition
        PathTransition pathTransition = new PathTransition(TRACE_DURATION, path, token);
        pathTransition.setInterpolator(Interpolator.EASE_BOTH);

        // Clean up when animation finishes
        pathTransition.setOnFinished(e -> {
            overlayLayer.getChildren().removeAll(path, token);
        });

        // Play the animation
        Animations.playIfEnabled(pathTransition);
    }

    /**
     * Create and play a data flow trace animation along a custom path.
     * 
     * @param path         the path to follow
     * @param overlayLayer the pane to add the animation elements to
     */
    public static void traceFlow(Path path, Pane overlayLayer) {
        if (!Animations.isEnabled() || overlayLayer == null || path == null) {
            return;
        }

        // Style the path
        path.setStroke(PATH_COLOR);
        path.setStrokeWidth(PATH_WIDTH);
        path.setOpacity(PATH_OPACITY);
        path.getStyleClass().add("flow-path");

        // Create the token (small circle)
        Circle token = new Circle(TOKEN_RADIUS, TOKEN_COLOR);
        token.getStyleClass().add("flow-token");

        // Add elements to overlay layer
        overlayLayer.getChildren().addAll(path, token);

        // Create path transition
        PathTransition pathTransition = new PathTransition(TRACE_DURATION, path, token);
        pathTransition.setInterpolator(Interpolator.EASE_BOTH);

        // Clean up when animation finishes
        pathTransition.setOnFinished(e -> {
            overlayLayer.getChildren().removeAll(path, token);
        });

        // Play the animation
        Animations.playIfEnabled(pathTransition);
    }

    /**
     * Create and play a data flow trace animation between two nodes.
     * The animation will trace from the center of the start node to the center of
     * the end node.
     * 
     * @param startNode    the starting node
     * @param endNode      the ending node
     * @param overlayLayer the pane to add the animation elements to
     */
    public static void traceFlow(Node startNode, Node endNode, Pane overlayLayer) {
        if (!Animations.isEnabled() || overlayLayer == null || startNode == null || endNode == null) {
            return;
        }

        // Calculate center points of the nodes
        Point2D startPoint = getNodeCenter(startNode, overlayLayer);
        Point2D endPoint = getNodeCenter(endNode, overlayLayer);

        if (startPoint != null && endPoint != null) {
            traceFlow(startPoint, endPoint, overlayLayer);
        }
    }

    /**
     * Create a simple path between two points.
     * 
     * @param startPoint the starting point
     * @param endPoint   the ending point
     * @return a Path object connecting the two points
     */
    private static Path createPath(Point2D startPoint, Point2D endPoint) {
        Path path = new Path();
        path.getElements().add(new MoveTo(startPoint.getX(), startPoint.getY()));
        path.getElements().add(new LineTo(endPoint.getX(), endPoint.getY()));
        return path;
    }

    /**
     * Get the center point of a node relative to the overlay layer.
     * 
     * @param node         the node to get the center of
     * @param overlayLayer the overlay layer for coordinate conversion
     * @return the center point of the node, or null if calculation fails
     */
    private static Point2D getNodeCenter(Node node, Pane overlayLayer) {
        try {
            // Get the node's bounds in the overlay layer's coordinate system
            javafx.geometry.Bounds bounds = node.localToScene(node.getBoundsInLocal());
            javafx.geometry.Bounds overlayBounds = overlayLayer.localToScene(overlayLayer.getBoundsInLocal());

            // Convert to overlay layer coordinates
            double centerX = bounds.getMinX() + bounds.getWidth() / 2 - overlayBounds.getMinX();
            double centerY = bounds.getMinY() + bounds.getHeight() / 2 - overlayBounds.getMinY();

            return new Point2D(centerX, centerY);
        } catch (Exception e) {
            // If coordinate conversion fails, return null
            return null;
        }
    }

    /**
     * Create a curved path between two points for more visually appealing traces.
     * 
     * @param startPoint the starting point
     * @param endPoint   the ending point
     * @return a curved Path object
     */
    public static Path createCurvedPath(Point2D startPoint, Point2D endPoint) {
        Path path = new Path();

        // Calculate control points for a gentle curve
        double midX = (startPoint.getX() + endPoint.getX()) / 2;
        double midY = (startPoint.getY() + endPoint.getY()) / 2;

        // Add some curvature by offsetting the midpoint
        double offset = Math.abs(endPoint.getX() - startPoint.getX()) * 0.3;
        double controlY = midY - offset;

        path.getElements().add(new MoveTo(startPoint.getX(), startPoint.getY()));
        path.getElements().add(new javafx.scene.shape.QuadCurveTo(
                endPoint.getX(), endPoint.getY(), midX, controlY));

        return path;
    }

    /**
     * Create and play a curved data flow trace animation.
     * 
     * @param startPoint   the starting point
     * @param endPoint     the ending point
     * @param overlayLayer the pane to add the animation elements to
     */
    public static void traceFlowCurved(Point2D startPoint, Point2D endPoint, Pane overlayLayer) {
        if (!Animations.isEnabled() || overlayLayer == null || startPoint == null || endPoint == null) {
            return;
        }

        Path curvedPath = createCurvedPath(startPoint, endPoint);
        traceFlow(curvedPath, overlayLayer);
    }
}
