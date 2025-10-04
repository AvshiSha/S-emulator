package ui.animations;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Demo application to showcase the S-Emulator animation system.
 * This demonstrates how the three animations work and how to control them.
 */
public class AnimationDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("S-Emulator Animation Demo");

        // Create demo controls
        VBox root = new VBox(20);

        // Animation toggle
        CheckBox animationToggle = new CheckBox("Enable Animations");
        animationToggle.setSelected(Animations.isEnabled());
        animationToggle.setOnAction(e -> {
            Animations.setEnabled(animationToggle.isSelected());
            System.out.println("Animations " + (Animations.isEnabled() ? "enabled" : "disabled"));
        });

        // Demo buttons
        Button rowPulseDemo = new Button("Demo Row Pulse");
        Button variableBlipDemo = new Button("Demo Variable Blip");
        Button dataFlowDemo = new Button("Demo Data Flow Trace");

        // Demo labels (simulating UI elements)
        Label instructionRow = new Label("Instruction Row (Click to pulse)");
        instructionRow.setStyle(
                "-fx-background-color: #f0f0f0; -fx-padding: 10; -fx-border-color: #ccc; -fx-border-width: 1;");

        Label variableCell = new Label("Variable Cell (Click to blip)");
        variableCell.setStyle(
                "-fx-background-color: #e0e0e0; -fx-padding: 10; -fx-border-color: #ccc; -fx-border-width: 1;");

        // Event handlers
        rowPulseDemo.setOnAction(e -> {
            System.out.println("Triggering row pulse animation...");
            RowPulseAnimation.pulseRow(instructionRow);
        });

        variableBlipDemo.setOnAction(e -> {
            System.out.println("Triggering variable blip animation...");
            VariableBlipAnimation.blipCell(variableCell);
        });

        dataFlowDemo.setOnAction(e -> {
            System.out.println("Triggering data flow trace animation...");
            // For demo purposes, we'll create a simple path between two points
            javafx.geometry.Point2D start = new javafx.geometry.Point2D(50, 50);
            javafx.geometry.Point2D end = new javafx.geometry.Point2D(200, 100);
            javafx.scene.layout.Pane overlay = new javafx.scene.layout.Pane();
            overlay.setPrefSize(300, 200);
            overlay.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ccc; -fx-border-width: 1;");

            DataFlowTraceAnimation.traceFlow(start, end, overlay);

            // Add overlay to scene temporarily
            root.getChildren().add(overlay);

            // Remove overlay after animation completes
            javafx.concurrent.Task<Void> removeTask = new javafx.concurrent.Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Thread.sleep(2000); // Wait for animation to complete
                    javafx.application.Platform.runLater(() -> {
                        root.getChildren().remove(overlay);
                    });
                    return null;
                }
            };
            new Thread(removeTask).start();
        });

        // Click handlers for direct interaction
        instructionRow.setOnMouseClicked(e -> RowPulseAnimation.pulseRow(instructionRow));
        variableCell.setOnMouseClicked(e -> VariableBlipAnimation.blipCell(variableCell));

        // Layout
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(rowPulseDemo, variableBlipDemo, dataFlowDemo);

        root.getChildren().addAll(
                new Label("S-Emulator Animation System Demo"),
                animationToggle,
                buttonBox,
                new Label("Click the elements below to see animations:"),
                instructionRow,
                variableCell);

        Scene scene = new Scene(root, 400, 300);

        // Add animation CSS
        scene.getStylesheets().add(getClass().getResource("animations.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
