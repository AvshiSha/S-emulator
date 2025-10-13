package com.semulator.client.ui.components.DebuggerExecution;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;

import com.semulator.client.AppContext;
import com.semulator.client.service.ApiClient;
import com.semulator.client.model.ApiModels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 * Debug Execution Panel - Uses server-side debug API for step-by-step execution
 */
public class DebuggerExecution {

    // FXML Components - Execution Commands
    @FXML
    private Button startRegularButton;
    @FXML
    private Button startDebugButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button resumeButton;
    @FXML
    private Button stepOverButton;

    // FXML Components - Variables Panel
    @FXML
    private TableView<VariableRow> variablesTableView;
    @FXML
    private TableColumn<VariableRow, String> variableNameColumn;
    @FXML
    private TableColumn<VariableRow, String> variableValueColumn;

    // FXML Components - Cycles Panel
    @FXML
    private Label currentCyclesLabel;

    // FXML Components - Architecture Selection Panel
    @FXML
    private ComboBox<String> architectureComboBox;
    @FXML
    private Label architectureCostLabel;

    // FXML Components - Execution Inputs Panel
    @FXML
    private VBox inputFieldsContainer;
    @FXML
    private Button addInputButton;
    @FXML
    private Label executionStatusLabel;

    // Data Models
    private ObservableList<VariableRow> variablesData = FXCollections.observableArrayList();

    // Execution State
    private String currentProgramName;
    private String targetType = "PROGRAM"; // "PROGRAM" or "FUNCTION"
    private int currentDegree = 0;
    private String debugSessionId = null;
    private String selectedArchitecture = "I"; // Default to Architecture I

    // API Client
    private ApiClient apiClient;

    // State flags
    private AtomicBoolean isExecuting = new AtomicBoolean(false);
    private AtomicBoolean isDebugMode = new AtomicBoolean(false);
    private AtomicBoolean isPaused = new AtomicBoolean(false);
    private AtomicInteger currentCycles = new AtomicInteger(0);

    // Execution Service
    private ExecutionService executionService;

    // Input Variables Map
    private Map<String, Integer> inputVariables = new HashMap<>();
    private int nextInputNumber = 1;

    // Previous variable state for change tracking
    private Map<String, Long> previousVariableState = new HashMap<>();

    // Callback for instruction table highlighting
    private java.util.function.Consumer<Integer> instructionTableCallback;

    // Callback for architecture summary updates
    private java.util.function.Consumer<java.util.Map<String, Integer>> architectureSummaryCallback;

    // Callback for architecture selection changes
    private java.util.function.Consumer<String> architectureSelectionCallback;

    // Architecture compatibility state
    private boolean isArchitectureCompatible = true;
    private java.util.List<String> unsupportedArchitectures = new java.util.ArrayList<>();

    // Current user credits
    private int currentUserCredits = 0;

    @FXML
    private void initialize() {
        // This method is called by FXML automatically
        // The actual initialization is done in initializeWithApiClient()
    }

    public void initializeWithApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
        setupDebuggerComponents();
    }

    private void setupDebuggerComponents() {
        // Initialize Variables Table
        variableNameColumn.setCellValueFactory(cellData -> cellData.getValue().variableNameProperty());
        variableValueColumn.setCellValueFactory(cellData -> cellData.getValue().variableValueProperty());
        variablesTableView.setItems(variablesData);

        // Set up row highlighting for changed variables
        setupVariableRowHighlighting();

        // Initialize Architecture Selection
        initializeArchitectureSelection();

        // Set up table columns to be resizable
        variableNameColumn.setResizable(true);
        variableValueColumn.setResizable(true);

        // Disable sorting
        variableNameColumn.setSortable(false);
        variableValueColumn.setSortable(false);

        // Initialize Architecture Selection ComboBox
        initializeArchitectureSelection();

        // Initialize execution service
        executionService = new ExecutionService();

        // Set initial button states
        updateButtonStates();

        // Initialize cycles display
        updateCyclesDisplay();
    }

    private void initializeArchitectureSelection() {
        // Initialize architecture options with credit costs
        ObservableList<String> architectureOptions = FXCollections.observableArrayList(
                "Architecture I",
                "Architecture II",
                "Architecture III",
                "Architecture IV");

        architectureComboBox.setItems(architectureOptions);
        architectureComboBox.setValue("Architecture I (5 credits)");
        architectureCostLabel.setText("Cost: 5 credits");

        // Add change listener to update selected architecture and cost
        architectureComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.equals("Architecture I")) {
                    selectedArchitecture = "I";
                    architectureCostLabel.setText("Cost: 5 credits");
                } else if (newValue.equals("Architecture II")) {
                    selectedArchitecture = "II";
                    architectureCostLabel.setText("Cost: 100 credits");
                } else if (newValue.equals("Architecture III")) {
                    selectedArchitecture = "III";
                    architectureCostLabel.setText("Cost: 500 credits");
                } else if (newValue.equals("Architecture IV")) {
                    selectedArchitecture = "IV";
                    architectureCostLabel.setText("Cost: 1000 credits");
                }

                // Notify instruction table of architecture change
                if (architectureSelectionCallback != null) {
                    architectureSelectionCallback.accept(selectedArchitecture);
                }
            }
        });
    }

    // Execution Command Handlers
    @FXML
    private void startRegularExecution(ActionEvent event) {
        if (currentProgramName == null) {
            showAlert("No Program", "Please load a program first.");
            return;
        }

        // The server will handle credit deduction automatically
        isDebugMode.set(false);
        isPaused.set(false);
        isExecuting.set(true);
        currentCycles.set(0);

        updateButtonStates();
        updateExecutionStatus("Starting Regular Execution...");

        // Start execution in background using server API
        // Server will deduct architecture cost and per-cycle credits automatically
        executionService.restart();
    }

    @FXML
    private void startDebugExecution(ActionEvent event) {
        if (currentProgramName == null) {
            showAlert("No Program", "Please load a program first.");
            return;
        }

        // Server will handle credit deduction automatically
        isDebugMode.set(true);
        isPaused.set(true);
        isExecuting.set(true);
        currentCycles.set(0);

        updateButtonStates();
        updateExecutionStatus("Starting Debug Session...");

        // Get inputs in proper order
        List<Long> inputs = getOrderedInputs();

        // Start debug session via API
        // Server will deduct architecture cost automatically
        apiClient.debugStart(currentProgramName, currentDegree, inputs, selectedArchitecture)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.success()) {
                            debugSessionId = response.sessionId();
                            updateExecutionStatus("Debug Mode Started - Use Step Over to execute instructions");
                            updateFromDebugState(response.state());

                            // Credits already updated in updateFromDebugState() - no extra fetch needed!

                            // Highlight the first instruction
                            if (instructionTableCallback != null) {
                                instructionTableCallback.accept(response.state().currentInstructionIndex());
                            }
                        } else {
                            // Check if it's an insufficient credits error
                            if (response.message().contains("Insufficient credits")) {
                                showAlert("Insufficient Credits",
                                        response.message() + "\n\nPlease load more credits and try again.");
                            } else {
                                showAlert("Debug Start Failed", response.message());
                            }
                            stopExecution(null);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        String errorMsg = ex.getMessage();
                        if (errorMsg != null && errorMsg.toLowerCase().contains("credit")) {
                            showAlert("Insufficient Credits", errorMsg + "\n\nPlease load more credits and try again.");
                        } else {
                            showAlert("Debug Start Error", "Failed to start debug session: " + errorMsg);
                        }
                        stopExecution(null);
                    });
                    return null;
                });
    }

    @FXML
    private void stopExecution(ActionEvent event) {
        isExecuting.set(false);
        isPaused.set(false);
        isDebugMode.set(false);

        // Stop debug session if active
        if (debugSessionId != null && isDebugMode.get()) {
            apiClient.debugStop(debugSessionId)
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            debugSessionId = null;
                            updateExecutionStatus("Debug session stopped");
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            updateExecutionStatus("Error stopping debug session: " + ex.getMessage());
                        });
                        return null;
                    });
        }

        // Stop execution service if running
        if (executionService != null && executionService.isRunning()) {
            executionService.cancel();
        }

        updateButtonStates();
        updateExecutionStatus("Execution Stopped");
    }

    @FXML
    private void resumeExecution(ActionEvent event) {
        if (debugSessionId == null) {
            return;
        }

        isPaused.set(false);
        updateButtonStates();
        updateExecutionStatus("Resuming Execution...");

        // Resume execution on server
        apiClient.debugResume(debugSessionId)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.success()) {
                            updateFromDebugState(response.state());
                            updateExecutionStatus("Execution Completed");
                            isExecuting.set(false);
                            isPaused.set(false);
                            isDebugMode.set(false);
                            updateButtonStates();
                        } else {
                            showAlert("Resume Failed", response.message());
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showAlert("Resume Error", "Failed to resume execution: " + ex.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    private void stepOver(ActionEvent event) {
        if (debugSessionId == null || !isDebugMode.get() || !isPaused.get()) {
            return;
        }

        updateExecutionStatus("Executing step...");

        // Execute one step on server
        // Server will handle credit deduction automatically
        apiClient.debugStep(debugSessionId)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.success()) {
                            updateFromDebugState(response.state());

                            // Credits already updated in updateFromDebugState() - instant update!

                            // Check if finished
                            if ("FINISHED".equals(response.state().state())) {
                                updateExecutionStatus("Program execution completed");
                                isExecuting.set(false);
                                isPaused.set(false);
                                isDebugMode.set(false);
                                updateButtonStates();
                            } else if ("ERROR".equals(response.state().state())) {
                                // Check if error is due to out of credits
                                String error = response.state().error();
                                if (error != null && error.toLowerCase().contains("credit")) {
                                    showAlert("Out of Credits",
                                            "You have run out of credits!\n\n" +
                                                    "Execution has been stopped. Please load more credits and try again.");
                                    navigateBackToDashboard();
                                } else {
                                    showAlert("Execution Error", error);
                                }
                                stopExecution(null);
                            } else {
                                updateExecutionStatus("Step executed - Instruction " +
                                        response.state().currentInstructionIndex() + " of " +
                                        response.state().totalInstructions() +
                                        " | Credits: " + currentUserCredits);
                            }

                            // Highlight current instruction
                            if (instructionTableCallback != null) {
                                instructionTableCallback.accept(response.state().currentInstructionIndex());
                            }
                        } else {
                            // Check if it's an out of credits error
                            String message = response.message();
                            if (message != null && message.toLowerCase().contains("credit")) {
                                showAlert("Out of Credits",
                                        "You have run out of credits!\n\n" +
                                                "Execution has been stopped. Please load more credits and try again.");
                                navigateBackToDashboard();
                            } else {
                                showAlert("Step Failed", message);
                            }
                            stopExecution(null);
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        String errorMsg = ex.getMessage();
                        if (errorMsg != null && errorMsg.toLowerCase().contains("credit")) {
                            showAlert("Out of Credits",
                                    "You have run out of credits!\n\n" +
                                            "Execution has been stopped. Please load more credits and try again.");
                            navigateBackToDashboard();
                        } else {
                            showAlert("Step Error", "Failed to execute step: " + errorMsg);
                        }
                        stopExecution(null);
                    });
                    return null;
                });
    }

    /**
     * Navigate back to dashboard (programs screen)
     * This should be called when user runs out of credits
     */
    private void navigateBackToDashboard() {
        // This method should trigger navigation back to dashboard
        // Implementation depends on navigation framework being used
        // For now, just log it
        System.out.println("Navigating back to dashboard due to insufficient credits");

        // In a real implementation, this would be something like:
        // AppContext.getInstance().navigateToDashboard();
    }

    /**
     * Update UI from debug state response
     */
    private void updateFromDebugState(ApiModels.DebugStateResponse state) {
        // Update cycles
        currentCycles.set(state.cycles());
        updateCyclesDisplay();

        // Update variables display
        updateVariablesDisplay(state.variables());

        // Update credits from response (fast, no extra network call!)
        if (state.remainingCredits() != null) {
            currentUserCredits = state.remainingCredits();
        }
    }

    /**
     * Create an input field for a variable
     */
    private void createInputField(String variableName, String initialValue) {
        HBox inputRow = new HBox(10);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setStyle("-fx-padding: 5;");

        // Variable name label
        Label varLabel = new Label(variableName + ":");
        varLabel.setMinWidth(50);
        varLabel.setStyle("-fx-font-weight: bold;");

        // Value text field
        TextField valueField = new TextField(initialValue);
        valueField.setPrefWidth(100);
        valueField.setPromptText("Enter value");

        // Add change listener to update the input variables map
        valueField.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                if (!newValue.isEmpty()) {
                    int value = Integer.parseInt(newValue);
                    inputVariables.put(variableName, value);
                } else {
                    inputVariables.put(variableName, 0);
                }
            } catch (NumberFormatException e) {
                // Invalid input, keep the old value
                valueField.setText(oldValue);
            }
        });

        inputRow.getChildren().addAll(varLabel, valueField);
        inputFieldsContainer.getChildren().add(inputRow);
    }

    /**
     * Add a new input variable manually
     */
    @FXML
    private void addInputVariable(ActionEvent event) {
        // Find the next available input number
        while (inputVariables.containsKey("x" + nextInputNumber)) {
            nextInputNumber++;
        }

        String newInputVar = "x" + nextInputNumber;

        // Add to input variables map with default value 0
        inputVariables.put(newInputVar, 0);

        // Create the input field
        createInputField(newInputVar, "0");

        // Update status
        updateExecutionStatus("Added input variable: " + newInputVar);

        // Move to next number for future additions
        nextInputNumber++;
    }

    // Public Methods for Integration
    public void setProgramName(String programName, int degree) {
        this.currentProgramName = programName;
        this.targetType = "PROGRAM";
        this.currentDegree = degree;

        if (programName != null) {
            updateExecutionStatus("Program Loaded: " + programName + " (degree " + degree + ")");
            // Fetch current credits when program is loaded
            fetchCurrentCredits();
        }
    }

    public void setFunctionName(String functionName, int degree) {
        this.currentProgramName = functionName;
        this.targetType = "FUNCTION";
        this.currentDegree = degree;

        if (functionName != null) {
            updateExecutionStatus("Function Loaded: " + functionName + " (degree " + degree + ")");
            // Fetch current credits when function is loaded
            fetchCurrentCredits();
        }
    }

    /**
     * Update the current degree for debug execution
     * Called when user changes degree in the UI
     */
    public void updateDegree(int degree) {
        this.currentDegree = degree;

        if (currentProgramName != null) {
            updateExecutionStatus("Program Loaded: " + currentProgramName + " (degree " + degree + ")");
        }
    }

    public void setInputVariables(List<String> inputVarNames) {
        Platform.runLater(() -> {
            // Clear existing input fields
            inputFieldsContainer.getChildren().clear();
            inputVariables.clear();

            if (inputVarNames != null && !inputVarNames.isEmpty()) {
                for (String varName : inputVarNames) {
                    inputVariables.put(varName, 0);
                    createInputField(varName, "0");
                }

                // Set next input number
                int maxNum = 0;
                for (String varName : inputVarNames) {
                    if (varName.startsWith("x")) {
                        try {
                            int num = Integer.parseInt(varName.substring(1));
                            maxNum = Math.max(maxNum, num);
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
                nextInputNumber = maxNum + 1;
            } else {
                nextInputNumber = 1;
            }
        });
    }

    public void clearExecution() {
        stopExecution(null);
        variablesData.clear();
        inputFieldsContainer.getChildren().clear();
        inputVariables.clear();
        nextInputNumber = 1;
        currentCycles.set(0);
        updateCyclesDisplay();
        updateExecutionStatus("Ready");
    }

    /**
     * Set inputs from history (called when user selects a run to rerun)
     */
    public void setInputs(List<Long> inputs) {
        Platform.runLater(() -> {
            // Clear existing input fields
            inputFieldsContainer.getChildren().clear();
            inputVariables.clear();

            if (inputs != null && !inputs.isEmpty()) {
                // Create input fields for each input
                for (int i = 0; i < inputs.size(); i++) {
                    String varName = "x" + (i + 1);
                    inputVariables.put(varName, inputs.get(i).intValue());
                    createInputField(varName, String.valueOf(inputs.get(i)));
                }
            }
        });
    }

    /**
     * Convert List<Long> inputs to Map<String, Long> format expected by server
     */
    private Map<String, Long> convertInputsToMap(List<Long> inputs) {
        Map<String, Long> inputMap = new HashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            inputMap.put("x" + (i + 1), inputs.get(i));
        }
        return inputMap;
    }

    /**
     * Get inputs in proper order (x1, x2, x3, etc.) with zeros for missing inputs
     */
    private List<Long> getOrderedInputs() {
        List<Long> orderedInputs = new ArrayList<>();

        // Find the maximum input number
        int maxInputNumber = 0;
        for (String varName : inputVariables.keySet()) {
            if (varName.startsWith("x")) {
                try {
                    int inputNumber = Integer.parseInt(varName.substring(1));
                    maxInputNumber = Math.max(maxInputNumber, inputNumber);
                } catch (NumberFormatException e) {
                    // Skip invalid input variable names
                }
            }
        }

        // Create ordered list with zeros for missing inputs
        for (int i = 1; i <= maxInputNumber; i++) {
            String varName = "x" + i;
            int value = inputVariables.getOrDefault(varName, 0);
            orderedInputs.add((long) value);
        }

        return orderedInputs;
    }

    public void setInstructionTableCallback(java.util.function.Consumer<Integer> callback) {
        this.instructionTableCallback = callback;
    }

    public void setArchitectureSummaryCallback(java.util.function.Consumer<java.util.Map<String, Integer>> callback) {
        this.architectureSummaryCallback = callback;
    }

    public void setArchitectureSelectionCallback(java.util.function.Consumer<String> callback) {
        this.architectureSelectionCallback = callback;
    }

    /**
     * Called when architecture compatibility status changes
     * 
     * @param compatible  true if selected architecture supports all instructions
     * @param unsupported list of architecture names that have unsupported
     *                    instructions
     */
    public void onArchitectureCompatibilityChanged(boolean compatible, java.util.List<String> unsupported) {
        Platform.runLater(() -> {
            this.isArchitectureCompatible = compatible;
            this.unsupportedArchitectures = unsupported != null ? unsupported : new java.util.ArrayList<>();

            updateButtonStates();

            if (!compatible && !unsupported.isEmpty()) {
                updateExecutionStatus("⚠ Program contains instructions requiring: " + String.join(", ", unsupported));
            } else if (compatible && currentProgramName != null) {
                updateExecutionStatus("Ready to execute");
            }
        });
    }

    /**
     * Fetch current user credits from server
     */
    private void fetchCurrentCredits() {
        String currentUser = AppContext.getInstance().getCurrentUser();
        apiClient.get("/users", com.semulator.client.model.ApiModels.UsersResponse.class, null)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        // Find current user's credits
                        for (com.semulator.client.model.ApiModels.UserInfo user : response.users()) {
                            if (user.username().equals(currentUser)) {
                                currentUserCredits = user.credits();
                                break;
                            }
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        System.err.println("Failed to fetch credits: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    // Credit deduction is now handled entirely by the server
    // The server (DebugServlet) deducts:
    // - Architecture cost when starting a debug session
    // - 1 credit per cycle when executing steps
    // The client just fetches and displays the current credit balance

    private void setupVariableRowHighlighting() {
        variablesTableView.setRowFactory(tv -> {
            TableRow<VariableRow> row = new TableRow<VariableRow>() {
                @Override
                protected void updateItem(VariableRow item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        // Highlight changed variables in debug mode
                        if (item.isChanged()) {
                            setStyle("-fx-background-color: #FF6B6B; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            };
            return row;
        });
    }

    // Helper Methods
    private void updateButtonStates() {
        boolean executing = isExecuting.get();
        boolean paused = isPaused.get();
        boolean debugMode = isDebugMode.get();

        // Disable execution buttons if architecture is not compatible or if no program
        // is loaded
        boolean canStart = !executing && isArchitectureCompatible && (currentProgramName != null);

        startRegularButton.setDisable(!canStart);
        startDebugButton.setDisable(!canStart);
        stopButton.setDisable(!executing);
        resumeButton.setDisable(!executing || !paused);
        stepOverButton.setDisable(!executing || !paused || !debugMode);
    }

    private void updateExecutionStatus(String status) {
        Platform.runLater(() -> {
            executionStatusLabel.setText(status);
        });
    }

    private void updateCyclesDisplay() {
        Platform.runLater(() -> {
            currentCyclesLabel.setText(String.valueOf(currentCycles.get()));
        });
    }

    private void updateVariablesDisplay(Map<String, Long> serverVariables) {
        // Make effectively final for use in lambda
        final Map<String, Long> variables = (serverVariables != null) ? serverVariables : new HashMap<>();

        Platform.runLater(() -> {
            variablesData.clear();

            List<VariableRow> orderedRows = new ArrayList<>();

            // Always show y variable first
            Long yValue = variables.getOrDefault("y", 0L);
            boolean yChanged = isVariableChanged("y", yValue);
            orderedRows.add(new VariableRow("y", String.valueOf(yValue), yChanged));

            // Then add x variables (input variables) in order
            List<String> xVars = new ArrayList<>();
            for (String varName : variables.keySet()) {
                if (varName.startsWith("x") && !varName.equals("x")) {
                    xVars.add(varName);
                }
            }
            xVars.sort((v1, v2) -> {
                try {
                    int n1 = Integer.parseInt(v1.substring(1));
                    int n2 = Integer.parseInt(v2.substring(1));
                    return Integer.compare(n1, n2);
                } catch (NumberFormatException e) {
                    return v1.compareTo(v2);
                }
            });

            for (String xVar : xVars) {
                Long value = variables.get(xVar);
                boolean changed = isVariableChanged(xVar, value);
                orderedRows.add(new VariableRow(xVar, String.valueOf(value), changed));
            }

            // Finally add z variables (working variables) in order
            List<String> zVars = new ArrayList<>();
            for (String varName : variables.keySet()) {
                if (varName.startsWith("z")) {
                    zVars.add(varName);
                }
            }
            zVars.sort((v1, v2) -> {
                try {
                    int n1 = Integer.parseInt(v1.substring(1));
                    int n2 = Integer.parseInt(v2.substring(1));
                    return Integer.compare(n1, n2);
                } catch (NumberFormatException e) {
                    return v1.compareTo(v2);
                }
            });

            for (String zVar : zVars) {
                Long value = variables.get(zVar);
                boolean changed = isVariableChanged(zVar, value);
                orderedRows.add(new VariableRow(zVar, String.valueOf(value), changed));
            }

            // Add all ordered rows to the table
            variablesData.addAll(orderedRows);

            // Update previous state for next comparison
            previousVariableState.clear();
            previousVariableState.putAll(variables);
        });
    }

    private boolean isVariableChanged(String varName, Long currentValue) {
        if (!isDebugMode.get()) {
            return false;
        }

        Long previousValue = previousVariableState.get(varName);
        return previousValue != null && !previousValue.equals(currentValue);
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Data Model Classes
    public static class VariableRow {
        private final SimpleStringProperty variableName;
        private final SimpleStringProperty variableValue;
        private final boolean isChanged;

        public VariableRow(String name, String value) {
            this(name, value, false);
        }

        public VariableRow(String name, String value, boolean isChanged) {
            this.variableName = new SimpleStringProperty(name);
            this.variableValue = new SimpleStringProperty(value);
            this.isChanged = isChanged;
        }

        public SimpleStringProperty variableNameProperty() {
            return variableName;
        }

        public SimpleStringProperty variableValueProperty() {
            return variableValue;
        }

        public boolean isChanged() {
            return isChanged;
        }
    }

    // Execution Service for Background Execution (Exe 2 style)
    private class ExecutionService extends Service<Void> {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        while (isExecuting.get() && !isCancelled()) {
                            if (isPaused.get() && isDebugMode.get()) {
                                // In debug mode and paused, wait for step command
                                Thread.sleep(100);
                                continue;
                            }

                            // Execute the program using server API (instead of local executor)
                            if (currentProgramName != null) {
                                // Get ordered inputs
                                List<Long> inputs = getOrderedInputs();
                                Map<String, Long> inputMap = convertInputsToMap(inputs);

                                // Call server API to run the program
                                try {
                                    String currentUser = AppContext.getInstance().getCurrentUser();
                                    apiClient
                                            .runPrepare(targetType, currentProgramName, selectedArchitecture,
                                                    currentDegree, inputMap)
                                            .thenCompose(prepareResponse -> {
                                                // Update architecture summary in instruction table
                                                if (architectureSummaryCallback != null) {
                                                    architectureSummaryCallback
                                                            .accept(prepareResponse.instructionCountsByArch());
                                                }

                                                if (prepareResponse.supported()) {
                                                    return apiClient.runStart(targetType, currentProgramName,
                                                            selectedArchitecture,
                                                            currentDegree, inputMap, currentUser);
                                                } else {
                                                    throw new RuntimeException(
                                                            "Architecture not supported: "
                                                                    + prepareResponse.unsupported());
                                                }
                                            })
                                            .thenCompose(startResponse -> {
                                                // Poll for completion
                                                return pollForCompletion(startResponse.runId());
                                            })
                                            .thenAccept(result -> {
                                                Platform.runLater(() -> {
                                                    // Mark execution as complete
                                                    isExecuting.set(false);
                                                    updateButtonStates();
                                                    updateExecutionStatus("Execution Complete - Result: " + result);
                                                });
                                            })
                                            .exceptionally(ex -> {
                                                Platform.runLater(() -> {
                                                    isExecuting.set(false);
                                                    updateButtonStates();
                                                    showAlert("Execution Error",
                                                            "Failed to execute program" + currentProgramName + ": "
                                                                    + ex.getMessage());
                                                    updateExecutionStatus("Execution Failed");
                                                });
                                                return null;
                                            })
                                            .join(); // Wait for the entire chain to complete
                                } catch (Exception e) {
                                    Platform.runLater(() -> {
                                        isExecuting.set(false);
                                        updateButtonStates();
                                        showAlert("Execution Error", "Failed to execute program 2: " + e.getMessage());
                                        updateExecutionStatus("Execution Failed");
                                    });
                                }

                                // Exit the loop after starting execution
                                break;
                            }
                        }

                        if (!isCancelled()) {
                            isExecuting.set(false);
                            isPaused.set(false);
                            updateButtonStates();
                        }

                    } catch (InterruptedException e) {
                        // Execution was cancelled
                        isExecuting.set(false);
                        isPaused.set(false);
                        updateButtonStates();
                        updateExecutionStatus("Execution Cancelled");
                    }

                    return null;
                }
            };
        }
    }

    // Helper method to poll for completion
    private CompletableFuture<Long> pollForCompletion(String runId) {
        return CompletableFuture.supplyAsync(() -> {
            while (isExecuting.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    com.semulator.client.model.ApiModels.RunStatusResponse statusResponse = apiClient
                            .runGetStatus(runId).join();

                    Platform.runLater(() -> {
                        currentCycles.set(statusResponse.cycles());
                        updateCyclesDisplay();

                        // Display both input and output variables
                        Map<String, Long> variables = new HashMap<>();

                        // Add input variables
                        List<Long> inputs = getOrderedInputs();
                        for (int i = 0; i < inputs.size(); i++) {
                            variables.put("x" + (i + 1), inputs.get(i));
                        }

                        // Add output variable if available
                        if (statusResponse.outputY() != null) {
                            variables.put("y", statusResponse.outputY());
                        }

                        updateVariablesDisplay(variables);

                        // Update credits from response - instant, no extra network call!
                        if (statusResponse.remainingCredits() != null) {
                            currentUserCredits = statusResponse.remainingCredits();
                        }

                        updateExecutionStatus("Running... (Cycles: " + statusResponse.cycles() + " | Credits: "
                                + currentUserCredits + ")");
                    });

                    // Check if finished
                    if ("FINISHED".equals(statusResponse.state())) {
                        Platform.runLater(() -> {
                            isExecuting.set(false);
                            updateButtonStates();
                            updateExecutionStatus("Execution Completed - Result: " + statusResponse.outputY());
                        });
                        return statusResponse.outputY();
                    } else if ("ERROR".equals(statusResponse.state())) {
                        // Check if error is due to insufficient credits
                        String errorMsg = statusResponse.error();
                        if (errorMsg != null && errorMsg.toLowerCase().contains("credit")) {
                            Platform.runLater(() -> {
                                showAlert("Out of Credits",
                                        "You have run out of credits during execution!\n\n" +
                                                "Please load more credits and try again.");
                                navigateBackToDashboard();
                            });
                        }
                        throw new RuntimeException(errorMsg);
                    }

                    Thread.sleep(500); // Poll every 500ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Execution interrupted");
                } catch (Exception e) {
                    throw new RuntimeException("Polling failed: " + e.getMessage());
                }
            }
            return 0L; // Default return
        });
    }

}
