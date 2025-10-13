package com.semulator.client.ui.components.Header;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.service.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Header component controller - adapted for HTTP communication
 * Handles file loading, program/function selection, and degree controls
 */
public class HeaderController implements Initializable {

    @FXML
    private Button loadFileButton;
    @FXML
    private TextField filePathField;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private ComboBox<String> programFunctionSelector;
    @FXML
    private ComboBox<String> levelSelector;
    @FXML
    private Label lblDegreeStatus;
    @FXML
    private ComboBox<String> labelVariableComboBox;

    private ApiClient apiClient;
    private ObservableList<String> programsList = FXCollections.observableArrayList();
    private ObservableList<String> functionsList = FXCollections.observableArrayList();
    private ObservableList<String> combinedList = FXCollections.observableArrayList();
    private ObservableList<String> degreeList = FXCollections.observableArrayList();
    private ObservableList<String> labelVariableList = FXCollections.observableArrayList();

    private String currentProgramName;
    private int currentMaxDegree = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.apiClient = AppContext.getInstance().getApiClient();

        setupComboBoxes();
        setupEventHandlers();
        loadInitialData();
    }

    private void setupComboBoxes() {
        // Setup program/function selector
        programFunctionSelector.setItems(combinedList);

        // Setup level selector
        levelSelector.setItems(degreeList);

        // Setup label/variable selector
        labelVariableComboBox.setItems(labelVariableList);
    }

    private void setupEventHandlers() {
        loadFileButton.setOnAction(event -> loadFileButtonPressed());
        programFunctionSelector.setOnAction(event -> onProgramFunctionSelected());
        levelSelector.setOnAction(event -> onLevelSelected());
        labelVariableComboBox.setOnAction(event -> onLabelVariableSelected());
    }

    private void loadInitialData() {
        // Load programs and functions from server
        loadPrograms();
        loadFunctions();
    }

    private void loadPrograms() {
        apiClient.get("/programs", ApiModels.ProgramsResponse.class)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        programsList.clear();
                        for (ApiModels.ProgramSummary program : response.programs()) {
                            programsList.add("PROGRAM: " + program.name());
                        }
                        updateCombinedList();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showError("Failed to load programs: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void loadFunctions() {
        apiClient.get("/functions", ApiModels.FunctionsResponse.class)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        functionsList.clear();
                        for (ApiModels.FunctionSummary function : response.functions()) {
                            functionsList.add("FUNCTION: " + function.name());
                        }
                        updateCombinedList();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showError("Failed to load functions: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void updateCombinedList() {
        combinedList.clear();
        combinedList.addAll(programsList);
        combinedList.addAll(functionsList);
    }

    @FXML
    private void loadFileButtonPressed() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select S-Emulator Program File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        File selectedFile = fileChooser.showOpenDialog(loadFileButton.getScene().getWindow());
        if (selectedFile != null) {
            uploadFile(selectedFile);
        }
    }

    private void uploadFile(File file) {
        try {
            // Show progress
            progressBar.setVisible(true);
            progressLabel.setVisible(true);
            progressLabel.setText("Uploading...");
            progressBar.setProgress(0.5);

            // Read file content
            String xmlContent = Files.readString(file.toPath());
            String username = AppContext.getInstance().getCurrentUser();

            // Upload to server
            apiClient.post("/upload", xmlContent, ApiModels.LoadResult.class)
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response.success()) {
                                // Success
                                progressBar.setProgress(1.0);
                                progressLabel.setText("Uploaded successfully!");
                                filePathField.setText(file.getAbsolutePath());
                                currentProgramName = response.programName();

                                // Refresh data
                                loadInitialData();

                                // Calculate max degree for the new program
                                calculateMaxDegree(response.programName());

                                // Hide progress after delay
                                CompletableFuture.delayedExecutor(2, java.util.concurrent.TimeUnit.SECONDS)
                                        .execute(() -> Platform.runLater(() -> {
                                            progressBar.setVisible(false);
                                            progressLabel.setVisible(false);
                                        }));
                            } else {
                                showError("Upload failed: " + response.message());
                                progressBar.setVisible(false);
                                progressLabel.setVisible(false);
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            showError("Upload failed: " + throwable.getMessage());
                            progressBar.setVisible(false);
                            progressLabel.setVisible(false);
                        });
                        return null;
                    });

        } catch (IOException e) {
            showError("Failed to read file: " + e.getMessage());
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
        }
    }

    private void calculateMaxDegree(String programName) {
        // TODO: Implement degree calculation via API
        // This would call a server endpoint to calculate max degree
        // For now, simulate with a default value
        currentMaxDegree = 5; // Default simulation

        Platform.runLater(() -> {
            updateDegreeList();
            lblDegreeStatus.setText("0 / " + currentMaxDegree);
        });
    }

    private void updateDegreeList() {
        degreeList.clear();
        for (int i = 0; i <= currentMaxDegree; i++) {
            degreeList.add(String.valueOf(i));
        }
    }

    @FXML
    private void onProgramFunctionSelected() {
        String selected = programFunctionSelector.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Notify instruction table to update
            notifyInstructionTableUpdate(selected);

            // Update degree calculation
            if (selected.startsWith("PROGRAM:")) {
                String programName = selected.substring(9); // Remove "PROGRAM: " prefix
                calculateMaxDegree(programName);
            }
        }
    }

    @FXML
    private void onLevelSelected() {
        String selectedDegree = levelSelector.getSelectionModel().getSelectedItem();
        if (selectedDegree != null) {
            int degree = Integer.parseInt(selectedDegree);
            lblDegreeStatus.setText(degree + " / " + currentMaxDegree);

            // Notify instruction table to update degree view
            notifyDegreeUpdate(degree);
        }
    }

    @FXML
    private void onLabelVariableSelected() {
        String selected = labelVariableComboBox.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Notify instruction table to highlight
            notifyLabelVariableHighlight(selected);
        }
    }

    private void notifyInstructionTableUpdate(String selectedProgramFunction) {
        // TODO: Implement communication with InstructionTable component
    }

    private void notifyDegreeUpdate(int degree) {
        // TODO: Implement communication with InstructionTable component
    }

    private void notifyLabelVariableHighlight(String labelVariable) {
        // TODO: Implement communication with InstructionTable component
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.showAndWait();
    }

    // Public methods for external communication
    public void refreshData() {
        loadInitialData();
    }

    public String getSelectedProgramFunction() {
        return programFunctionSelector.getSelectionModel().getSelectedItem();
    }

    public int getSelectedDegree() {
        String selected = levelSelector.getSelectionModel().getSelectedItem();
        return selected != null ? Integer.parseInt(selected) : 0;
    }

    public String getCurrentProgramName() {
        return currentProgramName;
    }
}
