package ui.components.DebuggerExecution;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Service;
import javafx.beans.property.SimpleStringProperty;

import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.execution.ExecutionContext;
import semulator.variable.Variable;
import semulator.instructions.SInstruction;

import ui.RunResult;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    @FXML
    private Button stepBackwardButton;

    // FXML Components - Variables Panel
    @FXML
    private TableView<VariableRow> variablesTableView;
    @FXML
    private TableColumn<VariableRow, String> variableNameColumn;
    @FXML
    private TableColumn<VariableRow, String> variableValueColumn;
    @FXML
    private TableColumn<VariableRow, String> variableTypeColumn;

    // FXML Components - Cycles Panel
    @FXML
    private Label currentCyclesLabel;
    @FXML
    private Label maxCyclesLabel;

    // FXML Components - Execution Inputs Panel
    @FXML
    private TextField inputVariableField;
    @FXML
    private TextField inputValueField;
    @FXML
    private Button addInputButton;
    @FXML
    private TableView<InputRow> inputsTableView;
    @FXML
    private TableColumn<InputRow, String> inputVariableNameColumn;
    @FXML
    private TableColumn<InputRow, String> inputValueColumn;
    @FXML
    private TableColumn<InputRow, String> inputActionColumn;
    @FXML
    private Label executionStatusLabel;

    // Data Models
    private ObservableList<VariableRow> variablesData = FXCollections.observableArrayList();
    private ObservableList<InputRow> inputsData = FXCollections.observableArrayList();

    // Execution State
    private SProgram currentProgram;
    private ProgramExecutor executor;
    private ExecutionContext executionContext;

    // History callback
    private java.util.function.Consumer<RunResult> historyCallback;
    private AtomicBoolean isExecuting = new AtomicBoolean(false);
    private AtomicBoolean isDebugMode = new AtomicBoolean(false);
    private AtomicBoolean isPaused = new AtomicBoolean(false);
    private AtomicInteger currentCycles = new AtomicInteger(0);
    private AtomicInteger maxCycles = new AtomicInteger(1000); // Default max cycles

    // Execution Service
    private ExecutionService executionService;

    // Input Variables Map
    private Map<String, Integer> inputVariables = new HashMap<>();

    @FXML
    private void initialize() {
        // Initialize Variables Table
        variableNameColumn.setCellValueFactory(cellData -> cellData.getValue().variableNameProperty());
        variableValueColumn.setCellValueFactory(cellData -> cellData.getValue().variableValueProperty());
        variableTypeColumn.setCellValueFactory(cellData -> cellData.getValue().variableTypeProperty());
        variablesTableView.setItems(variablesData);

        // Initialize Inputs Table
        inputVariableNameColumn.setCellValueFactory(cellData -> cellData.getValue().variableNameProperty());
        inputValueColumn.setCellValueFactory(cellData -> cellData.getValue().variableValueProperty());
        inputActionColumn.setCellValueFactory(cellData -> cellData.getValue().actionProperty());
        inputsTableView.setItems(inputsData);

        // Set up table columns to be resizable
        variableNameColumn.setResizable(true);
        variableValueColumn.setResizable(true);
        variableTypeColumn.setResizable(true);
        inputVariableNameColumn.setResizable(true);
        inputValueColumn.setResizable(true);
        inputActionColumn.setResizable(true);

        // Disable sorting
        variableNameColumn.setSortable(false);
        variableValueColumn.setSortable(false);
        variableTypeColumn.setSortable(false);
        inputVariableNameColumn.setSortable(false);
        inputValueColumn.setSortable(false);
        inputActionColumn.setSortable(false);

        // Initialize execution service
        executionService = new ExecutionService();

        // Set initial button states
        updateButtonStates();

        // Initialize cycles display
        updateCyclesDisplay();
    }

    // Execution Command Handlers
    @FXML
    private void startRegularExecution(ActionEvent event) {
        if (currentProgram == null) {
            showAlert("No Program", "Please load a program first.");
            return;
        }

        isDebugMode.set(false);
        isPaused.set(false);
        isExecuting.set(true);
        currentCycles.set(0);

        updateButtonStates();
        updateExecutionStatus("Starting Regular Execution...");

        // Start execution in background
        executionService.restart();
    }

    @FXML
    private void startDebugExecution(ActionEvent event) {
        if (currentProgram == null) {
            showAlert("No Program", "Please load a program first.");
            return;
        }

        isDebugMode.set(true);
        isPaused.set(false);
        isExecuting.set(true);
        currentCycles.set(0);

        updateButtonStates();
        updateExecutionStatus("Starting Debug Execution...");

        // Start execution in background
        executionService.restart();
    }

    @FXML
    private void stopExecution(ActionEvent event) {
        isExecuting.set(false);
        isPaused.set(false);

        if (executionService != null && executionService.isRunning()) {
            executionService.cancel();
        }

        updateButtonStates();
        updateExecutionStatus("Execution Stopped");
    }

    @FXML
    private void resumeExecution(ActionEvent event) {
        if (isPaused.get()) {
            isPaused.set(false);
            updateButtonStates();
            updateExecutionStatus("Resuming Execution...");
        }
    }

    @FXML
    private void stepOver(ActionEvent event) {
        if (isDebugMode.get() && isPaused.get()) {
            // Execute one step and pause again
            isPaused.set(false);
            // The execution service will handle stepping and pause again
            updateExecutionStatus("Stepping Over...");
        }
    }

    @FXML
    private void stepBackward(ActionEvent event) {
        // Step backward functionality - this would require maintaining execution
        // history
        // For now, we'll show a message that this feature is not yet implemented
        showAlert("Feature Not Available", "Step Backward functionality is not yet implemented.");
    }

    // Input Management
    @FXML
    private void addInput(ActionEvent event) {
        String variableName = inputVariableField.getText().trim();
        String valueText = inputValueField.getText().trim();

        if (variableName.isEmpty() || valueText.isEmpty()) {
            showAlert("Invalid Input", "Please enter both variable name and value.");
            return;
        }

        try {
            int value = Integer.parseInt(valueText);
            inputVariables.put(variableName, value);

            // Add to inputs table
            InputRow inputRow = new InputRow(variableName, String.valueOf(value), "Remove");
            inputsData.add(inputRow);

            // Clear input fields
            inputVariableField.clear();
            inputValueField.clear();

            updateExecutionStatus("Input added: " + variableName + " = " + value);

        } catch (NumberFormatException e) {
            showAlert("Invalid Value", "Please enter a valid integer value.");
        }
    }

    // Public Methods for Integration
    public void setProgram(SProgram program) {
        this.currentProgram = program;
        if (program != null) {
            // Initialize executor with the program
            this.executor = new ProgramExecutorImpl(program);
            this.executionContext = null; // Will be created when execution starts

            // Input variables will be passed to executor when execution starts

            updateExecutionStatus("Program Loaded: " + program.getName());
        }
    }

    public void clearExecution() {
        stopExecution(null);
        variablesData.clear();
        inputsData.clear();
        inputVariables.clear();
        currentCycles.set(0);
        updateCyclesDisplay();
        updateExecutionStatus("Ready");
    }

    /**
     * Set inputs from history (called when user selects a run to rerun)
     */
    public void setInputs(List<Long> inputs) {
        Platform.runLater(() -> {
            inputsData.clear();
            inputVariables.clear();

            if (inputs != null && !inputs.isEmpty()) {
                // Add inputs to the input variables map and display
                for (int i = 0; i < inputs.size(); i++) {
                    String varName = "input_" + (i + 1);
                    inputVariables.put(varName, inputs.get(i).intValue());
                    InputRow row = new InputRow(varName, String.valueOf(inputs.get(i)), "Set");
                    inputsData.add(row);
                }
            }
        });
    }

    /**
     * Set the history callback to record runs
     */
    public void setHistoryCallback(java.util.function.Consumer<RunResult> callback) {
        this.historyCallback = callback;
    }

    /**
     * Record the current run in history
     */
    private void recordRunInHistory() {
        if (historyCallback != null && currentProgram != null) {
            try {
                // Get the current degree (we'll use 0 for now, but this could be dynamic)
                int currentDegree = 0;

                // Get the inputs that were used
                List<Long> inputs = new java.util.ArrayList<>();
                for (Map.Entry<String, Integer> entry : inputVariables.entrySet()) {
                    inputs.add(entry.getValue().longValue());
                }

                // Get the final y value (we'll simulate this for now)
                long yValue = 0;
                if (executor != null) {
                    Map<Variable, Long> variableState = executor.variableState();
                    // Find the y variable
                    for (Map.Entry<Variable, Long> entry : variableState.entrySet()) {
                        if (entry.getKey().toString().equals("y")) {
                            yValue = entry.getValue();
                            break;
                        }
                    }
                }

                // Get the number of cycles
                int cycles = currentCycles.get();

                // Create the run result
                RunResult runResult = new RunResult(0, currentDegree, inputs, yValue, cycles);

                // Call the history callback
                historyCallback.accept(runResult);

            } catch (Exception e) {
                System.err.println("Error recording run in history: " + e.getMessage());
            }
        }
    }

    // Helper Methods
    private void updateButtonStates() {
        boolean executing = isExecuting.get();
        boolean paused = isPaused.get();
        boolean debugMode = isDebugMode.get();

        startRegularButton.setDisable(executing);
        startDebugButton.setDisable(executing);
        stopButton.setDisable(!executing);
        resumeButton.setDisable(!executing || !paused);
        stepOverButton.setDisable(!executing || !paused || !debugMode);
        stepBackwardButton.setDisable(!executing || !paused || !debugMode);
    }

    private void updateExecutionStatus(String status) {
        Platform.runLater(() -> {
            executionStatusLabel.setText(status);
        });
    }

    private void updateCyclesDisplay() {
        Platform.runLater(() -> {
            currentCyclesLabel.setText(String.valueOf(currentCycles.get()));
            maxCyclesLabel.setText(maxCycles.get() == Integer.MAX_VALUE ? "∞" : String.valueOf(maxCycles.get()));
        });
    }

    private void updateVariablesDisplay() {
        Platform.runLater(() -> {
            variablesData.clear();
            if (executor != null) {
                // Get all variables from executor's variable state
                Map<semulator.variable.Variable, Long> variables = executor.variableState();
                for (Map.Entry<semulator.variable.Variable, Long> entry : variables.entrySet()) {
                    VariableRow row = new VariableRow(entry.getKey().toString(), String.valueOf(entry.getValue()),
                            "Integer");
                    variablesData.add(row);
                }
            }
        });
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

    // Execution Service for Background Execution
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

                            // Execute one instruction
                            if (executor != null && executionContext != null) {
                                // This would be the actual execution logic
                                // For now, we'll simulate execution
                                Thread.sleep(100); // Simulate instruction execution time

                                currentCycles.incrementAndGet();
                                updateCyclesDisplay();
                                updateVariablesDisplay();

                                if (isDebugMode.get()) {
                                    // In debug mode, pause after each step
                                    isPaused.set(true);
                                    updateButtonStates();
                                    updateExecutionStatus("Debug Mode - Paused at cycle " + currentCycles.get());
                                }

                                // Check if we've reached max cycles
                                if (currentCycles.get() >= maxCycles.get()) {
                                    isExecuting.set(false);
                                    updateExecutionStatus("Execution Complete - Max cycles reached");
                                    break;
                                }
                            }
                        }

                        if (!isCancelled()) {
                            isExecuting.set(false);
                            isPaused.set(false);
                            updateButtonStates();
                            updateExecutionStatus("Execution Complete");

                            // Record the run in history
                            recordRunInHistory();
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

    // Data Model Classes
    public static class VariableRow {
        private final SimpleStringProperty variableName;
        private final SimpleStringProperty variableValue;
        private final SimpleStringProperty variableType;

        public VariableRow(String name, String value, String type) {
            this.variableName = new SimpleStringProperty(name);
            this.variableValue = new SimpleStringProperty(value);
            this.variableType = new SimpleStringProperty(type);
        }

        public SimpleStringProperty variableNameProperty() {
            return variableName;
        }

        public SimpleStringProperty variableValueProperty() {
            return variableValue;
        }

        public SimpleStringProperty variableTypeProperty() {
            return variableType;
        }
    }

    public static class InputRow {
        private final SimpleStringProperty variableName;
        private final SimpleStringProperty variableValue;
        private final SimpleStringProperty action;

        public InputRow(String name, String value, String action) {
            this.variableName = new SimpleStringProperty(name);
            this.variableValue = new SimpleStringProperty(value);
            this.action = new SimpleStringProperty(action);
        }

        public SimpleStringProperty variableNameProperty() {
            return variableName;
        }

        public SimpleStringProperty variableValueProperty() {
            return variableValue;
        }

        public SimpleStringProperty actionProperty() {
            return action;
        }
    }
}
