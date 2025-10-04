package ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Simple test application to verify the theming system works correctly.
 * This can be run independently to test theme switching functionality.
 */
public class ThemeTest extends Application {

    private ThemeManager themeManager;

    @Override
    public void start(Stage primaryStage) {
        themeManager = ThemeManager.getInstance();

        // Create test UI components
        VBox root = new VBox(10);
        root.getStyleClass().add("app-root");

        Label titleLabel = new Label("Theme Test Application");
        titleLabel.getStyleClass().add("title-label");

        Button testButton = new Button("Test Button");
        testButton.getStyleClass().add("load-button");

        TextField testField = new TextField("Test text field");

        ComboBox<String> testCombo = new ComboBox<>();
        testCombo.getItems().addAll("Option 1", "Option 2", "Option 3");
        testCombo.setValue("Option 1");

        Label statusLabel = new Label("Status: Ready");
        statusLabel.getStyleClass().add("status-label");

        // Theme selector
        ComboBox<Theme> themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll(Theme.values());
        themeSelector.setValue(themeManager.getCurrentTheme());
        themeSelector.setOnAction(e -> {
            Theme selectedTheme = themeSelector.getValue();
            if (selectedTheme != null) {
                themeManager.setTheme(selectedTheme);
                themeManager.applyCurrentTheme(primaryStage.getScene());
                statusLabel.setText("Status: Theme changed to " + selectedTheme.getDisplayName());
            }
        });

        // Font size selector
        ComboBox<String> fontSizeSelector = new ComboBox<>();
        fontSizeSelector.getItems().addAll("12px", "14px", "16px", "18px");
        fontSizeSelector.setValue(themeManager.getCurrentFontSize());
        fontSizeSelector.setOnAction(e -> {
            String selectedSize = fontSizeSelector.getValue();
            if (selectedSize != null) {
                themeManager.setFontSize(selectedSize);
                themeManager.applyFontSize(primaryStage.getScene());
                statusLabel.setText("Status: Font size changed to " + selectedSize);
            }
        });

        // Add all components to root
        root.getChildren().addAll(
                titleLabel,
                testButton,
                testField,
                testCombo,
                statusLabel,
                new Label("Theme:"),
                themeSelector,
                new Label("Font Size:"),
                fontSizeSelector);

        Scene scene = new Scene(root, 400, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Theme Test");

        // Apply the current theme
        themeManager.applyCurrentTheme(scene);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
