package com.semulator.client.ui;

import com.semulator.client.AppContext;
import com.semulator.client.service.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Main dashboard controller - replaces the original mainView
 * Handles HTTP communication instead of direct engine calls
 */
public class DashboardController implements Initializable {

    @FXML
    private VBox headerComponent;
    @FXML
    private VBox instructionTableComponent;

    // Store references to component controllers
    private com.semulator.client.ui.components.Header.Header headerController;
    private com.semulator.client.ui.components.InstructionTable.InstructionTable instructionTableController;
    private com.semulator.client.ui.components.HistoryChain.HistoryChain historyChainController;
    private com.semulator.client.ui.components.DebuggerExecution.DebuggerExecution debuggerExecutionController;
    private com.semulator.client.ui.components.HistoryStats.HistoryStats historyStatsController;
    @FXML
    private VBox historyChainComponent;
    @FXML
    private VBox debuggerExecutionComponent;
    @FXML
    private VBox historyStatsComponent;

    private ApiClient apiClient;
    private String currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.apiClient = AppContext.getInstance().getApiClient();
        this.currentUser = AppContext.getInstance().getCurrentUser();

        // Dashboard controller initialized

        setupComponents();
        setupComponentRelationships();
        loadInitialData();
    }

    private void setupComponents() {
        setupHeaderComponent();
        setupInstructionTableComponent();
        setupHistoryChainComponent();
        setupDebuggerExecutionComponent();
        setupHistoryStatsComponent();
    }

    private void setupComponentRelationships() {
        // Set up relationships between Header and other components
        if (headerController != null) {
            if (instructionTableController != null) {
                headerController.setInstructionTable(instructionTableController);
            }
            if (historyStatsController != null) {
                headerController.setHistoryStats(historyStatsController);
            }
            if (debuggerExecutionController != null) {
                headerController.setDebuggerExecution(debuggerExecutionController);
            }
        }
    }

    private void setupHeaderComponent() {
        try {
            // Load the real Exercise-2 Header component
            javafx.fxml.FXMLLoader headerLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/components/Header.fxml"));
            this.headerController = new com.semulator.client.ui.components.Header.Header();
            headerLoader.setController(this.headerController);
            javafx.scene.layout.VBox headerRoot = headerLoader.load();

            // Initialize the header component
            this.headerController.initializeWithHttp();

            headerComponent.getChildren().clear();
            headerComponent.getChildren().add(headerRoot);
        } catch (Exception e) {
            System.err.println("Failed to load header component: " + e.getMessage());
            e.printStackTrace();

            // Add a fallback
            Label fallbackLabel = new Label("Header Component - User: " + currentUser);
            fallbackLabel.setStyle(
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10px; -fx-background-color: #e0e0e0;");
            headerComponent.getChildren().clear();
            headerComponent.getChildren().add(fallbackLabel);
        }
    }

    private void setupInstructionTableComponent() {
        try {
            // Load the real Exercise-2 Instruction Table component
            javafx.fxml.FXMLLoader tableLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/components/InstructionTable.fxml"));
            this.instructionTableController = new com.semulator.client.ui.components.InstructionTable.InstructionTable();
            tableLoader.setController(this.instructionTableController);
            javafx.scene.layout.VBox tableRoot = tableLoader.load();

            // Initialize the instruction table component
            this.instructionTableController.initializeWithHttp();

            instructionTableComponent.getChildren().clear();
            instructionTableComponent.getChildren().add(tableRoot);
        } catch (Exception e) {
            System.err.println("Failed to load instruction table component: " + e.getMessage());
            e.printStackTrace();

            // Add a fallback
            Label fallbackLabel = new Label("Instruction Table Component\n(Table will show here)");
            fallbackLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-padding: 20px;");
            instructionTableComponent.getChildren().clear();
            instructionTableComponent.getChildren().add(fallbackLabel);
        }
    }

    private void setupHistoryChainComponent() {

        try {
            // Load the real Exercise-2 History Chain component
            javafx.fxml.FXMLLoader historyChainLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/components/HistoryChain.fxml"));
            this.historyChainController = new com.semulator.client.ui.components.HistoryChain.HistoryChain();
            historyChainLoader.setController(this.historyChainController);
            javafx.scene.layout.VBox historyChainRoot = historyChainLoader.load();

            // Initialize the history chain component
            this.historyChainController.initializeWithHttp();

            historyChainComponent.getChildren().clear();
            historyChainComponent.getChildren().add(historyChainRoot);

        } catch (Exception e) {
            System.err.println("Failed to load history chain component: " + e.getMessage());
            e.printStackTrace();

            // Add a fallback
            Label fallbackLabel = new Label("History Chain Component\n(History will show here)");
            fallbackLabel.setStyle(
                    "-fx-font-size: 14px; -fx-text-fill: #666; -fx-padding: 20px; -fx-background-color: #f0f0f0;");
            historyChainComponent.getChildren().clear();
            historyChainComponent.getChildren().add(fallbackLabel);
        }
    }

    private void setupDebuggerExecutionComponent() {

        try {
            // Load the real Exercise-2 Debugger Execution component
            javafx.fxml.FXMLLoader debuggerLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/components/DebuggerExecution.fxml"));
            this.debuggerExecutionController = new com.semulator.client.ui.components.DebuggerExecution.DebuggerExecution();
            debuggerLoader.setController(this.debuggerExecutionController);
            javafx.scene.layout.VBox debuggerRoot = debuggerLoader.load();

            // Initialize the debugger execution component
            this.debuggerExecutionController.initializeWithHttp();

            debuggerExecutionComponent.getChildren().clear();
            debuggerExecutionComponent.getChildren().add(debuggerRoot);

        } catch (Exception e) {
            System.err.println("Failed to load debugger execution component: " + e.getMessage());
            e.printStackTrace();

            // Add a fallback
            Label fallbackLabel = new Label("Debugger Execution Component\n(Controls will show here)");
            fallbackLabel.setStyle(
                    "-fx-font-size: 14px; -fx-text-fill: #666; -fx-padding: 20px; -fx-background-color: #f0f0f0;");
            debuggerExecutionComponent.getChildren().clear();
            debuggerExecutionComponent.getChildren().add(fallbackLabel);
        }
    }

    private void setupHistoryStatsComponent() {

        try {
            // Load the real Exercise-2 History Stats component
            javafx.fxml.FXMLLoader historyStatsLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/components/HistoryStats.fxml"));
            this.historyStatsController = new com.semulator.client.ui.components.HistoryStats.HistoryStats();
            historyStatsLoader.setController(this.historyStatsController);
            javafx.scene.layout.VBox historyStatsRoot = historyStatsLoader.load();

            // Initialize the history stats component
            this.historyStatsController.initializeWithHttp();

            historyStatsComponent.getChildren().clear();
            historyStatsComponent.getChildren().add(historyStatsRoot);

        } catch (Exception e) {
            System.err.println("Failed to load history stats component: " + e.getMessage());
            e.printStackTrace();

            // Add a fallback
            Label fallbackLabel = new Label("History Stats Component\n(Statistics will show here)");
            fallbackLabel.setStyle(
                    "-fx-font-size: 14px; -fx-text-fill: #666; -fx-padding: 20px; -fx-background-color: #f0f0f0;");
            historyStatsComponent.getChildren().clear();
            historyStatsComponent.getChildren().add(fallbackLabel);
        }
    }

    private void loadInitialData() {

        // Load initial data from server
        loadPrograms();
        loadFunctions();
        loadUserHistory();
    }

    private void loadPrograms() {
        apiClient.get("/programs", com.semulator.client.model.ApiModels.ProgramsResponse.class)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        System.err.println("Failed to load programs: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void loadFunctions() {
        apiClient.get("/functions", com.semulator.client.model.ApiModels.FunctionsResponse.class)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        System.err.println("Failed to load functions: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void loadUserHistory() {
        if (currentUser != null) {
            apiClient.get("/history?user=" + currentUser, com.semulator.client.model.ApiModels.HistoryResponse.class)
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            System.err.println("Failed to load history: " + throwable.getMessage());
                        });
                        return null;
                    });
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.showAndWait();
    }

    // Public methods for component communication
    public void refreshData() {
        loadInitialData();
    }

    public void onProgramLoaded(String programName) {
        loadInitialData();
    }

    public void onRunStarted(String runId) {
        startStatusPolling(runId);
    }

    public void onRunFinished(String runId) {
        loadUserHistory();
    }

    private void startStatusPolling(String runId) {
    }
}