package ui.components.DebuggerExecution;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
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

    // FXML Components - Cycles Panel
    @FXML
    private Label currentCyclesLabel;

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
    private int nextInputNumber = 1;

    @FXML
    private void initialize() {
        // Initialize Variables Table
        variableNameColumn.setCellValueFactory(cellData -> cellData.getValue().variableNameProperty());
        variableValueColumn.setCellValueFactory(cellData -> cellData.getValue().variableValueProperty());
        variablesTableView.setItems(variablesData);

        // Input fields container will be populated dynamically

        // Set up table columns to be resizable
        variableNameColumn.setResizable(true);
        variableValueColumn.setResizable(true);

        // Disable sorting
        variableNameColumn.setSortable(false);
        variableValueColumn.setSortable(false);

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
    public void setProgram(SProgram program) {
        this.currentProgram = program;
        if (program != null) {
            // Initialize executor with the program
            this.executor = new ProgramExecutorImpl(program);
            this.executionContext = null; // Will be created when execution starts

            // Extract required inputs and create dynamic input fields
            extractAndDisplayRequiredInputs(program);

            updateExecutionStatus("Program Loaded: " + program.getName());
        }
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
     * Set the history callback to record runs
     */
    public void setHistoryCallback(java.util.function.Consumer<RunResult> callback) {
        this.historyCallback = callback;
    }

    /**
     * Get inputs in proper order (x1, x2, x3, etc.) with zeros for missing inputs
     */
    private List<Long> getOrderedInputs() {
        List<Long> orderedInputs = new java.util.ArrayList<>();

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

    /**
     * Extract required inputs from the program and create dynamic input fields
     */
    private void extractAndDisplayRequiredInputs(SProgram program) {
        Platform.runLater(() -> {
            try {
                // Clear existing input fields
                inputFieldsContainer.getChildren().clear();
                inputVariables.clear();

                // Get the program instructions
                List<semulator.instructions.SInstruction> instructions = program.getInstructions();
                if (instructions == null || instructions.isEmpty()) {
                    updateExecutionStatus("No instructions found in program");
                    return;
                }

                // Extract all input variables (x1, x2, x3, etc.) from instructions
                java.util.Set<Integer> inputNumbers = new java.util.TreeSet<>();

                for (semulator.instructions.SInstruction instruction : instructions) {
                    // Check all instruction types that might use input variables
                    String varName = null;

                    if (instruction instanceof semulator.instructions.AssignVariableInstruction assignVar) {
                        varName = assignVar.getVariable().toString();

                        // Also check the source variable in arguments
                        String sourceVar = assignVar.getSource().toString();
                        if (sourceVar.startsWith("x") && sourceVar.length() > 1) {
                            try {
                                int inputNumber = Integer.parseInt(sourceVar.substring(1));
                                inputNumbers.add(inputNumber);
                            } catch (NumberFormatException e) {
                                // Invalid input variable format
                            }
                        }
                    } else if (instruction instanceof semulator.instructions.DecreaseInstruction decrease) {
                        varName = decrease.getVariable().toString();
                    } else if (instruction instanceof semulator.instructions.IncreaseInstruction increase) {
                        varName = increase.getVariable().toString();
                    } else if (instruction instanceof semulator.instructions.ZeroVariableInstruction zero) {
                        varName = zero.getVariable().toString();
                    } else if (instruction instanceof semulator.instructions.JumpEqualVariableInstruction jumpVar) {
                        varName = jumpVar.getVariable().toString();
                    } else if (instruction instanceof semulator.instructions.JumpNotZeroInstruction jumpNotZero) {
                        varName = jumpNotZero.getVariable().toString();
                    } else if (instruction instanceof semulator.instructions.JumpZeroInstruction jumpZero) {
                        varName = jumpZero.getVariable().toString();
                    }

                    // Check if it's a valid input variable (x1, x2, x3, etc.)
                    if (varName != null && varName.startsWith("x") && varName.length() > 1) {
                        try {
                            int inputNumber = Integer.parseInt(varName.substring(1));
                            inputNumbers.add(inputNumber);
                        } catch (NumberFormatException e) {
                            // Invalid input variable format
                        }
                    }
                }

                // Create input fields for ALL input variables from x1 to the maximum found
                if (!inputNumbers.isEmpty()) {
                    int maxInputNumber = inputNumbers.stream().mapToInt(Integer::intValue).max().orElse(0);

                    for (int i = 1; i <= maxInputNumber; i++) {
                        String inputVar = "x" + i;
                        // Initialize with default value 0
                        inputVariables.put(inputVar, 0);
                        createInputField(inputVar, "0");
                    }

                    // Set next input number to continue from where we left off
                    nextInputNumber = maxInputNumber + 1;

                    updateExecutionStatus("Found input variables x1 to x" + maxInputNumber + " (total: "
                            + maxInputNumber + " inputs)");
                } else {
                    // No input variables found, start from x1
                    nextInputNumber = 1;
                    updateExecutionStatus("No input variables found in program");
                }

            } catch (Exception e) {
                updateExecutionStatus("Error extracting inputs: " + e.getMessage());
                System.err.println("Error extracting required inputs: " + e.getMessage());
            }
        });
    }

    /**
     * Record the current run in history
     */
    private void recordRunInHistory() {
        if (historyCallback != null && currentProgram != null) {
            try {
                // Get the current degree (we'll use 0 for now, but this could be dynamic)
                int currentDegree = 0;

                // Get the inputs in proper order (x1, x2, x3, etc.)
                List<Long> inputs = getOrderedInputs();

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
        });
    }

    private void updateVariablesDisplay() {
        Platform.runLater(() -> {
            variablesData.clear();
            if (executor != null) {
                // Get all variables from executor's variable state
                Map<semulator.variable.Variable, Long> variables = executor.variableState();

                // Display variables in order: y, x1,x2,x3..., z1,z2,z3...
                java.util.List<VariableRow> orderedRows = new java.util.ArrayList<>();

                // First, add y variable
                for (Map.Entry<semulator.variable.Variable, Long> entry : variables.entrySet()) {
                    String varName = entry.getKey().toString();
                    if (varName.equals("y")) {
                        orderedRows.add(new VariableRow(varName, String.valueOf(entry.getValue())));
                        break;
                    }
                }

                // Then add x variables in order (x1, x2, x3, etc.)
                java.util.List<String> xVars = new java.util.ArrayList<>();
                for (Map.Entry<semulator.variable.Variable, Long> entry : variables.entrySet()) {
                    String varName = entry.getKey().toString();
                    if (varName.startsWith("x") && varName.length() > 1) {
                        try {
                            Integer.parseInt(varName.substring(1));
                            xVars.add(varName);
                        } catch (NumberFormatException e) {
                            // Skip invalid x variable names
                        }
                    }
                }
                xVars.sort((a, b) -> {
                    int numA = Integer.parseInt(a.substring(1));
                    int numB = Integer.parseInt(b.substring(1));
                    return Integer.compare(numA, numB);
                });

                for (String xVar : xVars) {
                    for (Map.Entry<semulator.variable.Variable, Long> entry : variables.entrySet()) {
                        if (entry.getKey().toString().equals(xVar)) {
                            orderedRows.add(new VariableRow(xVar, String.valueOf(entry.getValue())));
                            break;
                        }
                    }
                }

                // Finally add z variables in order (z1, z2, z3, etc.)
                java.util.List<String> zVars = new java.util.ArrayList<>();
                for (Map.Entry<semulator.variable.Variable, Long> entry : variables.entrySet()) {
                    String varName = entry.getKey().toString();
                    if (varName.startsWith("z") && varName.length() > 1) {
                        try {
                            Integer.parseInt(varName.substring(1));
                            zVars.add(varName);
                        } catch (NumberFormatException e) {
                            // Skip invalid z variable names
                        }
                    }
                }
                zVars.sort((a, b) -> {
                    int numA = Integer.parseInt(a.substring(1));
                    int numB = Integer.parseInt(b.substring(1));
                    return Integer.compare(numA, numB);
                });

                for (String zVar : zVars) {
                    for (Map.Entry<semulator.variable.Variable, Long> entry : variables.entrySet()) {
                        if (entry.getKey().toString().equals(zVar)) {
                            orderedRows.add(new VariableRow(zVar, String.valueOf(entry.getValue())));
                            break;
                        }
                    }
                }

                // Add all ordered rows to the table
                variablesData.addAll(orderedRows);
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

                            // Execute the program
                            if (executor != null) {
                                // Get ordered inputs
                                List<Long> inputs = getOrderedInputs();

                                // Run the program with inputs
                                long result = executor.run(inputs.toArray(new Long[0]));

                                // Update cycles (for now, we'll use a simple calculation)
                                currentCycles.set(inputs.size() + 10); // Simple cycle calculation
                                updateCyclesDisplay();
                                updateVariablesDisplay();

                                // Mark execution as complete
                                isExecuting.set(false);
                                updateExecutionStatus("Execution Complete - Result: " + result);
                                break;
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

        public VariableRow(String name, String value) {
            this.variableName = new SimpleStringProperty(name);
            this.variableValue = new SimpleStringProperty(value);
        }

        public SimpleStringProperty variableNameProperty() {
            return variableName;
        }

        public SimpleStringProperty variableValueProperty() {
            return variableValue;
        }
    }

}
