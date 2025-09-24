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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    // Reference to Header component for controlling expansion buttons
    private ui.components.Header.Header headerController;
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

    // Debugger-specific fields
    private int currentInstructionIndex = 0;
    private List<semulator.instructions.SInstruction> currentInstructions = new java.util.ArrayList<>();
    private Map<semulator.variable.Variable, Long> previousVariableState = new HashMap<>();
    private Map<semulator.variable.Variable, Long> currentVariableState = new HashMap<>();
    private boolean isStepExecution = false;

    // Step-by-step execution context
    private ExecutionContext stepExecutionContext = null;
    private Map<String, Integer> labelToIndexMap = new HashMap<>();

    // Simple execution context implementation for step-by-step execution
    private static class StepExecutionContext implements ExecutionContext {
        private final Map<semulator.variable.Variable, Long> variables = new HashMap<>();
        private final Long[] inputs;

        public StepExecutionContext(Long[] inputs) {
            this.inputs = inputs;
            // Initialize input variables
            for (int i = 0; i < inputs.length; i++) {
                semulator.variable.Variable inputVar = new semulator.variable.VariableImpl(
                        semulator.variable.VariableType.INPUT, i + 1);
                variables.put(inputVar, inputs[i]);
            }
        }

        @Override
        public long getVariableValue(semulator.variable.Variable v) {
            return variables.getOrDefault(v, 0L);
        }

        @Override
        public void updateVariable(semulator.variable.Variable v, long value) {
            variables.put(v, value);
        }

        public Map<semulator.variable.Variable, Long> variableState() {
            return new HashMap<>(variables);
        }
    }

    @FXML
    private void initialize() {
        // Initialize Variables Table
        variableNameColumn.setCellValueFactory(cellData -> cellData.getValue().variableNameProperty());
        variableValueColumn.setCellValueFactory(cellData -> cellData.getValue().variableValueProperty());
        variablesTableView.setItems(variablesData);

        // Set up row highlighting for changed variables
        setupVariableRowHighlighting();

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
        System.out.println("DEBUG: startDebugExecution() called");

        if (currentProgram == null) {
            System.out.println("DEBUG: No program loaded, showing alert");
            showAlert("No Program", "Please load a program first.");
            return;
        }

        System.out.println("DEBUG: Starting debug mode for program: " + currentProgram.getName());
        isDebugMode.set(true);
        isPaused.set(true); // Start paused in debug mode
        isExecuting.set(true);
        currentCycles.set(0);
        currentInstructionIndex = 0;
        isStepExecution = true;

        // Initialize debugger state
        System.out.println("DEBUG: Initializing debugger state...");
        initializeDebuggerState();

        updateButtonStates();
        updateExecutionStatus("Debug Mode Started - Use Step Over to execute instructions");
        updateVariablesDisplay();

        // Highlight the first instruction immediately when debug mode starts
        if (instructionTableCallback != null && !currentInstructions.isEmpty()) {
            System.out.println("DEBUG: Highlighting first instruction (index 0)");
            instructionTableCallback.accept(0); // Highlight instruction at index 0
        }

        // Disable expansion controls during debug execution
        if (headerController != null) {
            headerController.setExpansionControlsEnabled(false);
        }

        System.out.println("DEBUG: Debug mode initialization completed");
    }

    @FXML
    private void stopExecution(ActionEvent event) {
        isExecuting.set(false);
        isPaused.set(false);
        isDebugMode.set(false);
        isStepExecution = false;

        if (executionService != null && executionService.isRunning()) {
            executionService.cancel();
        }

        updateButtonStates();
        updateExecutionStatus("Execution Stopped");

        // Re-enable expansion controls when debug execution stops
        if (headerController != null) {
            headerController.setExpansionControlsEnabled(true);
        }
    }

    @FXML
    private void resumeExecution(ActionEvent event) {
        if (isPaused.get()) {
            isPaused.set(false);
            isStepExecution = false; // Exit step-by-step mode
            updateButtonStates();
            updateExecutionStatus("Resuming Normal Execution...");

            // Re-enable expansion controls when resuming normal execution
            if (headerController != null) {
                headerController.setExpansionControlsEnabled(true);
            }

            // Continue with normal execution
            if (executionService != null && !executionService.isRunning()) {
                executionService.restart();
            }
        }
    }

    @FXML
    private void stepOver(ActionEvent event) {
        System.out.println("DEBUG: stepOver() called - isDebugMode: " + isDebugMode.get() +
                ", isPaused: " + isPaused.get() + ", isStepExecution: " + isStepExecution);

        if (isDebugMode.get() && isPaused.get() && isStepExecution) {
            System.out.println("DEBUG: Conditions met, executing single step");
            executeSingleStep();
        } else {
            System.out.println("DEBUG: Conditions not met for step execution");
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
                    } else if (instruction instanceof semulator.instructions.QuoteInstruction quote) {
                        varName = quote.getVariable().toString();
                    } else if (instruction instanceof semulator.instructions.JumpEqualFunctionInstruction jumpEqualFunc) {
                        varName = jumpEqualFunc.getVariable().toString();
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

                // Create input fields ONLY for the input variables that actually exist in the
                // program
                if (!inputNumbers.isEmpty()) {
                    // Sort the input numbers to display them in order
                    List<Integer> sortedInputNumbers = new ArrayList<>(inputNumbers);
                    Collections.sort(sortedInputNumbers);

                    for (Integer inputNumber : sortedInputNumbers) {
                        String inputVar = "x" + inputNumber;
                        // Initialize with default value 0
                        inputVariables.put(inputVar, 0);
                        createInputField(inputVar, "0");
                    }

                    // Set next input number to continue from the maximum found + 1
                    int maxInputNumber = sortedInputNumbers.get(sortedInputNumbers.size() - 1);
                    nextInputNumber = maxInputNumber + 1;

                    updateExecutionStatus("Found input variables: " +
                            sortedInputNumbers.stream()
                                    .map(n -> "x" + n)
                                    .collect(Collectors.joining(", "))
                            +
                            " (total: " + sortedInputNumbers.size() + " inputs)");
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

    // Debugger-specific methods
    private void initializeDebuggerState() {
        System.out.println("DEBUG: initializeDebuggerState() called");

        if (currentProgram != null && currentProgram.getInstructions() != null) {
            currentInstructions.clear();
            currentInstructions.addAll(currentProgram.getInstructions());
            System.out.println("DEBUG: Loaded " + currentInstructions.size() + " instructions into debugger");

            // Initialize variable states
            previousVariableState.clear();
            currentVariableState.clear();

            // Initialize step-by-step execution context
            List<Long> inputs = getOrderedInputs();
            System.out.println("DEBUG: Ordered inputs: " + inputs);
            stepExecutionContext = new StepExecutionContext(inputs.toArray(new Long[0]));

            // Build label-to-index map for step-by-step execution
            buildLabelMap();
            System.out.println("DEBUG: Label map: " + labelToIndexMap);

            // Get initial variable state
            updateVariableStates();
            System.out.println("DEBUG: Debugger state initialization completed");
        } else {
            System.out.println("DEBUG: No program or instructions available for debugger initialization");
        }
    }

    private void buildLabelMap() {
        labelToIndexMap.clear();
        if (currentProgram != null && currentProgram.getInstructions() != null) {
            List<semulator.instructions.SInstruction> instructions = currentProgram.getInstructions();

            // First pass: collect all labels that are actually defined on instructions
            for (int i = 0; i < instructions.size(); i++) {
                semulator.instructions.SInstruction instruction = instructions.get(i);
                semulator.label.Label label = instruction.getLabel();

                if (label != null && label != semulator.label.FixedLabel.EMPTY
                        && label != semulator.label.FixedLabel.EXIT) {
                    String labelName = label.getLabel();
                    if (labelName != null && !labelName.isEmpty()) {
                        labelToIndexMap.put(labelName, i);
                    }
                }
            }
        }
    }

    private void executeSingleStep() {
        System.out.println("DEBUG: executeSingleStep() called - currentInstructionIndex: " + currentInstructionIndex +
                ", totalInstructions: " + currentInstructions.size());

        if (currentInstructionIndex >= currentInstructions.size()) {
            System.out.println("DEBUG: Program execution completed - reached end of instructions");
            updateExecutionStatus("Program execution completed");
            isExecuting.set(false);
            isPaused.set(false);
            isDebugMode.set(false);
            isStepExecution = false;
            updateButtonStates();

            // Re-enable expansion controls when debug execution completes
            if (headerController != null) {
                headerController.setExpansionControlsEnabled(true);
            }

            // Record the run in history
            recordRunInHistory();
            return;
        }

        try {
            // Store previous variable state
            previousVariableState.clear();
            previousVariableState.putAll(currentVariableState);

            // Execute the current instruction
            semulator.instructions.SInstruction currentInstruction = currentInstructions.get(currentInstructionIndex);
            System.out.println("DEBUG: Executing instruction " + currentInstructionIndex + ": " +
                    currentInstruction.getName() + " (" + currentInstruction.getClass().getSimpleName() + ")");

            // Handle QUOTE and JUMP_EQUAL_FUNCTION instructions specially in debug mode
            semulator.label.Label nextLabel;
            if (currentInstruction instanceof semulator.instructions.QuoteInstruction quoteInstruction) {
                System.out.println("DEBUG: Detected QUOTE instruction - calling executeQuoteInstruction()");
                nextLabel = executeQuoteInstruction(quoteInstruction);
            } else if (currentInstruction instanceof semulator.instructions.JumpEqualFunctionInstruction jumpEqualFunctionInstruction) {
                System.out.println(
                        "DEBUG: Detected JUMP_EQUAL_FUNCTION instruction - calling executeJumpEqualFunctionInstruction()");
                nextLabel = executeJumpEqualFunctionInstruction(jumpEqualFunctionInstruction);
            } else {
                System.out.println("DEBUG: Regular instruction - calling execute() directly");
                // Execute just this one instruction using the step execution context
                nextLabel = currentInstruction.execute(stepExecutionContext);
            }

            // Update variable states from the execution context
            updateVariableStatesFromContext();

            // Calculate cycles properly for QUOTE and JUMP_EQUAL_FUNCTION instructions
            int cyclesToAdd;
            if (currentInstruction.getName().equals("QUOTE")) {
                // For QUOTE instructions, use the proper cycle calculation: 5 + function cycles
                cyclesToAdd = currentInstruction.cycles();
            } else if (currentInstruction.getName().equals("JUMP_EQUAL_FUNCTION")) {
                // For JUMP_EQUAL_FUNCTION instructions, use the proper cycle calculation
                cyclesToAdd = currentInstruction.cycles();
            } else {
                // For regular instructions, use the standard cycle count
                cyclesToAdd = currentInstruction.cycles();
            }

            currentCycles.set(currentCycles.get() + cyclesToAdd);

            System.out.println("DEBUG: Updated cycles to: " + currentCycles.get() + " (added "
                    + cyclesToAdd + ")");

            // Determine next instruction based on the returned label
            if (nextLabel == semulator.label.FixedLabel.EMPTY) {
                System.out.println("DEBUG: nextLabel is EMPTY - continuing to next instruction");
                // Add cycles for this instruction
                // Continue to next instruction in sequence
                currentInstructionIndex++;
            } else if (nextLabel == semulator.label.FixedLabel.EXIT) {
                System.out.println("DEBUG: nextLabel is EXIT - program execution completed");
                // Add cycles for this instruction
                // Exit the program
                updateExecutionStatus("Program execution completed");
                isExecuting.set(false);
                isPaused.set(false);
                isDebugMode.set(false);
                isStepExecution = false;
                updateButtonStates();

                // Re-enable expansion controls when debug execution completes
                if (headerController != null) {
                    headerController.setExpansionControlsEnabled(true);
                }

                // Record the run in history
                recordRunInHistory();
                updateCyclesDisplay();
                return;
            } else {
                System.out.println("DEBUG: nextLabel is: " + nextLabel.getLabel() + " - attempting jump");
                // Add cycles for this instruction
                // Jump to the next label

                String labelName = nextLabel.getLabel();
                Integer targetIndex = labelToIndexMap.get(labelName);
                if (targetIndex != null) {
                    System.out.println("DEBUG: Jumping to label '" + labelName + "' at index: " + targetIndex);
                    currentInstructionIndex = targetIndex;
                } else {
                    System.out.println(
                            "DEBUG: Label '" + labelName + "' not found in labelMap, continuing to next instruction");
                    // If label not found, continue to next instruction
                    currentInstructionIndex++;
                }
            }

            // Update displays
            updateCyclesDisplay();
            updateVariablesDisplay();

            // Notify instruction table to highlight current instruction
            if (instructionTableCallback != null) {
                System.out.println("DEBUG: Notifying instruction table to highlight instruction at index: "
                        + currentInstructionIndex);
                instructionTableCallback.accept(currentInstructionIndex);
            }

            updateExecutionStatus(
                    "Executed instruction " + (currentInstructionIndex - 1) + " of " + currentInstructions.size());
            System.out.println("DEBUG: Step execution completed. Next instruction index: " + currentInstructionIndex);

            // Add a small delay to ensure UI updates are visible
            try {
                Thread.sleep(100); // 100ms delay to make step execution visible
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (Exception e) {
            System.err.println("DEBUG: Exception in executeSingleStep: " + e.getMessage());
            e.printStackTrace();
            updateExecutionStatus("Error executing step: " + e.getMessage());
            System.err.println("Error in step execution: " + e.getMessage());
        }
    }

    private void updateVariableStates() {
        if (executor != null) {
            currentVariableState.clear();
            currentVariableState.putAll(executor.variableState());
        }
    }

    private void updateVariableStatesFromContext() {
        if (stepExecutionContext != null && stepExecutionContext instanceof StepExecutionContext) {
            currentVariableState.clear();
            currentVariableState.putAll(((StepExecutionContext) stepExecutionContext).variableState());
        }
    }

    private semulator.label.Label executeQuoteInstruction(semulator.instructions.QuoteInstruction quoteInstruction) {
        System.out.println("DEBUG: executeQuoteInstruction() called");
        System.out.println("DEBUG: Target variable: " + quoteInstruction.getVariable());
        System.out.println("DEBUG: Function name: " + quoteInstruction.getFunctionName());
        System.out.println("DEBUG: Function arguments: " + quoteInstruction.getFunctionArguments());

        try {
            // Get the function definition from the program
            if (!(currentProgram instanceof semulator.program.SProgramImpl)) {
                System.out.println("DEBUG: Program is not SProgramImpl, using fallback");
                // If we can't get the function, just assign 0 as fallback
                stepExecutionContext.updateVariable(quoteInstruction.getVariable(), 0L);
                return semulator.label.FixedLabel.EMPTY;
            }

            semulator.program.SProgramImpl programImpl = (semulator.program.SProgramImpl) currentProgram;
            var functions = programImpl.getFunctions();
            String functionName = quoteInstruction.getFunctionName();
            System.out.println("DEBUG: Available functions: " + functions.keySet());

            if (!functions.containsKey(functionName)) {
                System.out.println("DEBUG: Function '" + functionName + "' not found, using fallback");
                // Function not found, assign 0 as fallback
                stepExecutionContext.updateVariable(quoteInstruction.getVariable(), 0L);
                return semulator.label.FixedLabel.EMPTY;
            }

            // Get the function body
            var functionInstructions = functions.get(functionName);
            System.out.println(
                    "DEBUG: Function '" + functionName + "' has " + functionInstructions.size() + " instructions");

            // Create a new execution context for the function
            List<Long> functionInputs = new ArrayList<>();
            for (semulator.instructions.FunctionArgument arg : quoteInstruction.getFunctionArguments()) {
                if (arg.isFunctionCall()) {
                    // For function calls, we need to execute them first
                    semulator.instructions.FunctionCall call = arg.asFunctionCall();
                    System.out.println("DEBUG: Executing nested function call: " + call.getFunctionName());
                    long nestedResult = executeNestedFunctionCall(call, stepExecutionContext, functions);
                    functionInputs.add(nestedResult);
                    System.out
                            .println("DEBUG: Nested function " + call.getFunctionName() + " returned: " + nestedResult);
                } else {
                    semulator.variable.Variable var = arg.asVariable();
                    if (var.getType() == semulator.variable.VariableType.Constant) {
                        long constantValue = (long) var.getNumber();
                        functionInputs.add(constantValue);
                        System.out.println("DEBUG: Constant argument: " + var + " = " + constantValue);
                    } else {
                        // Get the value from the current execution context
                        long variableValue = stepExecutionContext.getVariableValue(var);
                        functionInputs.add(variableValue);
                        System.out.println("DEBUG: Variable argument: " + var + " = " + variableValue);
                    }
                }
            }

            System.out.println("DEBUG: Function inputs: " + functionInputs);
            StepExecutionContext functionContext = new StepExecutionContext(functionInputs.toArray(new Long[0]));

            // Execute the function body
            System.out.println("DEBUG: Executing function body...");
            long functionResult = executeFunctionBody(functionInstructions, functionContext);
            System.out.println("DEBUG: Function result: " + functionResult);

            // Assign the result to the target variable
            stepExecutionContext.updateVariable(quoteInstruction.getVariable(), functionResult);
            System.out.println(
                    "DEBUG: Assigned result " + functionResult + " to variable " + quoteInstruction.getVariable());

            return semulator.label.FixedLabel.EMPTY;

        } catch (Exception e) {
            System.err.println("DEBUG: Exception in executeQuoteInstruction: " + e.getMessage());
            e.printStackTrace();
            // Fallback: assign 0 to the target variable
            stepExecutionContext.updateVariable(quoteInstruction.getVariable(), 0L);
            return semulator.label.FixedLabel.EMPTY;
        }
    }

    private long executeFunctionBody(List<semulator.instructions.SInstruction> functionInstructions,
            StepExecutionContext functionContext) {
        System.out.println("DEBUG: executeFunctionBody() called with " + functionInstructions.size() + " instructions");

        // Execute the function body and return the result
        // The result is stored in the 'y' variable (Variable.RESULT)

        int instructionIndex = 0;
        while (instructionIndex < functionInstructions.size()) {
            semulator.instructions.SInstruction instruction = functionInstructions.get(instructionIndex);
            System.out.println(
                    "DEBUG: Executing function instruction " + instructionIndex + ": " + instruction.getName());

            semulator.label.Label nextLabel = instruction.execute(functionContext);

            // Debug: Print variable state after each instruction
            System.out.println("DEBUG: After instruction " + instructionIndex + ", y (RESULT) = "
                    + functionContext.getVariableValue(semulator.variable.Variable.RESULT));

            // Handle jumps within the function
            if (nextLabel == semulator.label.FixedLabel.EXIT) {
                System.out.println("DEBUG: Function instruction returned EXIT - ending function execution");
                break; // Exit the function
            } else if (nextLabel != semulator.label.FixedLabel.EMPTY) {
                // Find the target instruction by label
                int targetIndex = findInstructionByLabel(functionInstructions, nextLabel);
                if (targetIndex != -1) {
                    System.out.println(
                            "DEBUG: Jumping to instruction " + targetIndex + " (label: " + nextLabel.getLabel() + ")");
                    instructionIndex = targetIndex;
                } else {
                    System.out.println("DEBUG: Warning - label " + nextLabel.getLabel()
                            + " not found, continuing to next instruction");
                    instructionIndex++;
                }
            } else {
                // No jump, continue to next instruction
                instructionIndex++;
            }
        }

        // Return the value of the 'y' variable (the function's output)
        long result = functionContext.getVariableValue(semulator.variable.Variable.RESULT);
        System.out.println("DEBUG: Function body execution completed, result (y): " + result);

        // Debug: Print all variable states in the function context
        System.out.println("DEBUG: Function context variable states:");
        for (var entry : functionContext.variableState().entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }

        return result;
    }

    /**
     * Find the index of an instruction with the given label
     */
    private int findInstructionByLabel(List<semulator.instructions.SInstruction> instructions,
            semulator.label.Label targetLabel) {
        for (int i = 0; i < instructions.size(); i++) {
            semulator.instructions.SInstruction instruction = instructions.get(i);
            if (instruction.getLabel() != null && instruction.getLabel().equals(targetLabel)) {
                return i;
            }
        }
        return -1; // Label not found
    }

    private semulator.label.Label executeJumpEqualFunctionInstruction(
            semulator.instructions.JumpEqualFunctionInstruction jumpEqualFunctionInstruction) {
        try {
            // Execute the function and compare its result with the variable
            long functionResult = executeFunctionForJumpEqual(jumpEqualFunctionInstruction);
            long variableValue = stepExecutionContext.getVariableValue(jumpEqualFunctionInstruction.getVariable());

            if (variableValue == functionResult) {
                return jumpEqualFunctionInstruction.getTarget(); // Jump if equal
            } else {
                return semulator.label.FixedLabel.EMPTY; // Continue if not equal
            }

        } catch (Exception e) {
            System.err.println("Error executing jump equal function instruction: " + e.getMessage());
            // Fallback: don't jump
            return semulator.label.FixedLabel.EMPTY;
        }
    }

    private long executeFunctionForJumpEqual(
            semulator.instructions.JumpEqualFunctionInstruction jumpEqualFunctionInstruction) {
        try {
            // Get the function definition from the program
            if (!(currentProgram instanceof semulator.program.SProgramImpl)) {
                return 0L; // Fallback
            }

            semulator.program.SProgramImpl programImpl = (semulator.program.SProgramImpl) currentProgram;
            var functions = programImpl.getFunctions();
            String functionName = jumpEqualFunctionInstruction.getFunctionName();

            if (!functions.containsKey(functionName)) {
                return 0L; // Fallback
            }

            // Get the function body
            var functionInstructions = functions.get(functionName);

            // Create a new execution context for the function
            List<Long> functionInputs = new ArrayList<>();
            for (semulator.instructions.FunctionArgument arg : jumpEqualFunctionInstruction.getFunctionArguments()) {
                if (arg.isFunctionCall()) {
                    // For function calls, we need to execute them first
                    semulator.instructions.FunctionCall call = arg.asFunctionCall();
                    System.out.println(
                            "DEBUG: Executing nested function call in JumpEqualFunction: " + call.getFunctionName());
                    long nestedResult = executeNestedFunctionCall(call, stepExecutionContext, functions);
                    functionInputs.add(nestedResult);
                    System.out
                            .println("DEBUG: Nested function " + call.getFunctionName() + " returned: " + nestedResult);
                } else {
                    semulator.variable.Variable var = arg.asVariable();
                    if (var.getType() == semulator.variable.VariableType.Constant) {
                        functionInputs.add((long) var.getNumber());
                    } else {
                        // Get the value from the current execution context
                        functionInputs.add(stepExecutionContext.getVariableValue(var));
                    }
                }
            }

            StepExecutionContext functionContext = new StepExecutionContext(functionInputs.toArray(new Long[0]));

            // Execute the function body and return the result
            return executeFunctionBody(functionInstructions, functionContext);

        } catch (Exception e) {
            System.err.println("Error executing function for jump equal: " + e.getMessage());
            return 0L; // Fallback
        }
    }

    // Callback for instruction table highlighting
    private java.util.function.Consumer<Integer> instructionTableCallback;

    public void setInstructionTableCallback(java.util.function.Consumer<Integer> callback) {
        this.instructionTableCallback = callback;
    }

    public void setHeaderController(ui.components.Header.Header headerController) {
        this.headerController = headerController;
    }

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
                            setStyle("-fx-background-color: #FF6B6B; -fx-font-weight: bold;"); // Red highlight for
                                                                                               // changed variables
                        } else {
                            setStyle(""); // No highlighting
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
            Map<semulator.variable.Variable, Long> variables = null;

            // Use step execution context for debug mode, regular executor for normal mode
            if (isDebugMode.get() && isStepExecution && stepExecutionContext != null
                    && stepExecutionContext instanceof StepExecutionContext) {
                variables = ((StepExecutionContext) stepExecutionContext).variableState();
            } else if (executor != null) {
                variables = executor.variableState();
            }

            if (variables != null) {

                // Display variables in order: y, x1,x2,x3..., z1,z2,z3...
                java.util.List<VariableRow> orderedRows = new java.util.ArrayList<>();

                // First, add y variable (result variable)
                for (Map.Entry<semulator.variable.Variable, Long> entry : variables.entrySet()) {
                    semulator.variable.Variable var = entry.getKey();
                    if (var.isResult()) {
                        boolean isChanged = isVariableChanged(var, entry.getValue());
                        orderedRows.add(new VariableRow(var.toString(), String.valueOf(entry.getValue()), isChanged));
                        break;
                    }
                }

                // Then add x variables (input variables) in order
                java.util.List<semulator.variable.Variable> xVars = new java.util.ArrayList<>();
                for (Map.Entry<semulator.variable.Variable, Long> entry : variables.entrySet()) {
                    semulator.variable.Variable var = entry.getKey();
                    if (var.isInput()) {
                        xVars.add(var);
                    }
                }
                // Sort x variables by number
                xVars.sort((v1, v2) -> Integer.compare(v1.getNumber(), v2.getNumber()));

                for (semulator.variable.Variable xVar : xVars) {
                    Long value = variables.get(xVar);
                    boolean isChanged = isVariableChanged(xVar, value);
                    orderedRows.add(new VariableRow(xVar.toString(), String.valueOf(value), isChanged));
                }

                // Finally add z variables (working variables) in order
                java.util.List<semulator.variable.Variable> zVars = new java.util.ArrayList<>();
                for (Map.Entry<semulator.variable.Variable, Long> entry : variables.entrySet()) {
                    semulator.variable.Variable var = entry.getKey();
                    if (var.isWork()) {
                        zVars.add(var);
                    }
                }
                // Sort z variables by number
                zVars.sort((v1, v2) -> Integer.compare(v1.getNumber(), v2.getNumber()));

                for (semulator.variable.Variable zVar : zVars) {
                    Long value = variables.get(zVar);
                    boolean isChanged = isVariableChanged(zVar, value);
                    orderedRows.add(new VariableRow(zVar.toString(), String.valueOf(value), isChanged));
                }

                // Add all ordered rows to the table
                variablesData.addAll(orderedRows);
            }
        });
    }

    private boolean isVariableChanged(semulator.variable.Variable variable, Long currentValue) {
        if (!isDebugMode.get() || !isStepExecution) {
            return false; // Only highlight changes in debug step mode
        }

        Long previousValue = previousVariableState.get(variable);
        return previousValue == null || !previousValue.equals(currentValue);
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

                                // Calculate cycles by summing up all instruction cycles
                                int totalCycles = 0;
                                totalCycles = executor.getTotalCycles();
                                currentCycles.set(totalCycles);
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
        private final boolean isChanged;

        public VariableRow(String name, String value) {
            this.variableName = new SimpleStringProperty(name);
            this.variableValue = new SimpleStringProperty(value);
            this.isChanged = false;
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

    /**
     * Execute a nested function call and return its result
     */
    private long executeNestedFunctionCall(semulator.instructions.FunctionCall call, ExecutionContext parentContext,
            Map<String, List<semulator.instructions.SInstruction>> functions) {
        System.out.println("DEBUG: Executing nested function: " + call.getFunctionName());

        // Get the function body for the nested function
        List<semulator.instructions.SInstruction> nestedFunctionBody = functions.get(call.getFunctionName());
        if (nestedFunctionBody == null) {
            System.out.println("DEBUG: ERROR: Function '" + call.getFunctionName() + "' not found in functions map");
            System.out.println("DEBUG: Available functions: " + functions.keySet());
            throw new IllegalArgumentException("Function '" + call.getFunctionName() + "' not found");
        }

        System.out.println("DEBUG: Found function body for " + call.getFunctionName() + " with "
                + nestedFunctionBody.size() + " instructions");

        // Create execution context for the nested function
        List<Long> nestedInputs = new ArrayList<>();
        System.out.println("DEBUG: Processing arguments for nested function " + call.getFunctionName() + ": "
                + call.getArguments());
        for (semulator.instructions.FunctionArgument arg : call.getArguments()) {
            if (arg.isFunctionCall()) {
                // Recursively execute nested function calls
                semulator.instructions.FunctionCall nestedCall = arg.asFunctionCall();
                System.out.println("DEBUG: Recursively calling: " + nestedCall.getFunctionName());
                long nestedResult = executeNestedFunctionCall(nestedCall, parentContext, functions);
                System.out.println(
                        "DEBUG: Recursive call " + nestedCall.getFunctionName() + " returned: " + nestedResult);
                nestedInputs.add(nestedResult);
            } else {
                semulator.variable.Variable var = arg.asVariable();
                if (var.getType() == semulator.variable.VariableType.Constant) {
                    System.out.println("DEBUG: Constant argument: " + var.getNumber());
                    nestedInputs.add((long) var.getNumber());
                } else {
                    // Get the value from the parent execution context
                    long varValue = parentContext.getVariableValue(var);
                    System.out.println("DEBUG: Variable argument " + var + " = " + varValue);
                    nestedInputs.add(varValue);
                }
            }
        }

        System.out.println("DEBUG: Nested function " + call.getFunctionName() + " inputs: " + nestedInputs);
        StepExecutionContext nestedContext = new StepExecutionContext(nestedInputs.toArray(new Long[0]));

        // Execute the nested function body
        long result = executeFunctionBody(nestedFunctionBody, nestedContext);
        System.out.println(
                "DEBUG: Nested function " + call.getFunctionName() + " execution completed, result: " + result);
        return result;
    }

}
