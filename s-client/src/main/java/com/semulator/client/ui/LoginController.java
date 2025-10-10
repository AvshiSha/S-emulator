package com.semulator.client.ui;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.service.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the login screen
 * Phase 1: Simple username entry (Exercise-2 compatible)
 * Phase 2: HTTP authentication (Exercise-3 client-server)
 */
public class LoginController implements Initializable {

    @FXML
    private VBox loginContainer;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField usernameField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
    }

    private void setupUI() {
        // Set up the login form
        titleLabel.setText("S-Emulator Login");
        titleLabel.getStyleClass().add("login-title");

        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().add("login-field");

        loginButton.setText("Login");
        loginButton.getStyleClass().add("login-button");

        errorLabel.setVisible(false);
        errorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");

        loginContainer.getStyleClass().add("login-container");
    }

    private void setupEventHandlers() {
        loginButton.setOnAction(event -> handleLogin());

        usernameField.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            showError("Please enter a username");
            return;
        }

        // Clear any previous errors
        hideError();

        // Basic validation
        if (username.length() < 2) {
            showError("Username must be at least 2 characters long");
            return;
        }

        // Disable login button during authentication
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");

        // Call HTTP authentication
        ApiClient apiClient = AppContext.getInstance().getApiClient();
        ApiModels.LoginRequest loginRequest = new ApiModels.LoginRequest(username);

        apiClient.post("/auth/login", loginRequest, ApiModels.LoginResponse.class)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        // Store user info and auth token
                        AppContext.getInstance().setCurrentUser(response.username());
                        AppContext.getInstance().setAuthToken(response.token());

                        // Navigate to dashboard
                        navigateToDashboard();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        String errorMessage = throwable.getMessage();

                        // Handle specific error cases
                        if (errorMessage != null) {
                            if (errorMessage.contains("already logged in")) {
                                showError(
                                        "This username is already logged in. Please use a different username or wait for the other session to logout.");
                            } else if (errorMessage.contains("CONFLICT")) {
                                showError("Username conflict. Please try a different username.");
                            } else if (errorMessage.contains("Username is required")) {
                                showError("Please enter a username.");
                            } else if (errorMessage.contains("HTTP error: 500")) {
                                showError("Server error. Please try again later.");
                            } else if (errorMessage.contains("HTTP error: 404")) {
                                showError("Server not available. Please check if the server is running.");
                            } else {
                                // Show the clean error message from server
                                showError(errorMessage);
                            }
                        } else {
                            showError("Login failed. Please try again.");
                        }

                        loginButton.setDisable(false);
                        loginButton.setText("Login");
                    });
                    return null;
                });
    }

    private void navigateToDashboard() {
        try {
            // Load the main dashboard (Exercise-2 style UI)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            AppContext.getInstance().getMainStage()
                    .setTitle("S-Emulator - " + AppContext.getInstance().getCurrentUser());
            AppContext.getInstance().getMainStage().setScene(scene);
            AppContext.getInstance().getMainStage().setResizable(true);

        } catch (IOException e) {
            showError("Failed to load dashboard: " + e.getMessage());
            loginButton.setDisable(false);
            loginButton.setText("Login");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
        errorLabel.setVisible(true);
    }
}
