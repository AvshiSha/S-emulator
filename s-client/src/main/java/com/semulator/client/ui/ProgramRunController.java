package com.semulator.client.ui;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.service.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Controller for the Program Run Screen
 * Handles program execution, debugging, and architecture selection
 */
public class ProgramRunController implements Initializable {

    // Header Components
    @FXML
    private HBox headerBar;
    @FXML
    private Label currentUserName;
    @FXML
    private Label screenTitle;
    @FXML
    private Label availableCredits;

    // Left Panel - Instructions and History
    @FXML
    private VBox leftPanel;

    // Component references (controllers are injected with fx:id + "Controller"
    // suffix)
    @FXML
    private com.semulator.client.ui.components.Header.ExecutionHeaderController executionHeaderController;
    @FXML
    private com.semulator.client.ui.components.InstructionTable.InstructionTable instructionTableComponentController;
    @FXML
    private com.semulator.client.ui.components.HistoryChain.HistoryChain historyChainComponentController;
    @FXML
    private com.semulator.client.ui.components.DebuggerExecution.DebuggerExecution debuggerExecutionComponentController;

    // Summary Line
    @FXML
    private Label summaryLine;

    // Navigation
    @FXML
    private Button backToDashboardButton;

    // State Management
    private ApiClient apiClient;
    private String currentUser;
    private String selectedProgram;
    private String selectedFunction;
    private String targetType; // "PROGRAM" or "FUNCTION"
    private String currentRunId;
    private String selectedArchitecture = "I";
    private boolean isExecuting = false;
    private boolean isDebugging = false;
    private Timer statusUpdateTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Initialize API client and current user
            this.apiClient = AppContext.getInstance().getApiClient();
            this.currentUser = AppContext.getInstance().getCurrentUser();

            // Initialize UI components
            initializeHeader();
            initializeExecutionHeader();
            initializeInstructionsTable();
            initializeHistoryTable();
            initializeDebuggerExecution();

            // Set up event handlers
            setupEventHandlers();

            // Start status update timer
            startStatusUpdateTimer();

        } catch (Exception e) {
            System.err.println("Error during ProgramRunController initialization: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize ProgramRunController", e);
        }
    }

    private void initializeHeader() {
        if (currentUserName != null) {
            currentUserName.setText(currentUser != null ? currentUser : "Unknown User");
        }
        if (screenTitle != null) {
            screenTitle.setText("S-Emulator - Execution");
        }
        if (availableCredits != null) {
            availableCredits.setText("0");
        }
    }

    private void initializeExecutionHeader() {
        if (executionHeaderController == null)
            return;

        // Set up callbacks for degree changes and label/variable selection
        executionHeaderController.setOnDegreeChanged(degree -> {
            // When degree changes, request expanded instructions from server
            if (degree >= 0 && instructionTableComponentController != null) {
                loadInstructionsForDegree(degree);

                // Also refresh history chain if an instruction is currently selected
                var selectedItem = instructionTableComponentController.getInstructionTableView().getSelectionModel()
                        .getSelectedItem();
                if (selectedItem != null && historyChainComponentController != null) {
                    loadHistoryChainFromServer(selectedItem);
                }
            }
        });

        executionHeaderController.setOnLabelVariableSelected(labelOrVariable -> {
            // When label/variable selected, highlight in instruction table
            if (labelOrVariable != null && instructionTableComponentController != null) {
                instructionTableComponentController.highlightRowsContaining(labelOrVariable);
            }
        });
    }

    private void initializeInstructionsTable() {
        if (instructionTableComponentController == null) {
            return;
        }

        // Initialize the instruction table component
        instructionTableComponentController.initializeWithHttp();

        // Simple direct table selection - show creation chain like Exercise 2
        instructionTableComponentController.getInstructionTableView().getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null && historyChainComponentController != null) {
                        // Get history chain from server
                        loadHistoryChainFromServer(newSelection);
                    } else if (historyChainComponentController != null) {
                        historyChainComponentController.clearHistory();
                    }
                });
    }

    private void initializeHistoryTable() {
        // History chain component initializes automatically via FXML
    }

    private void initializeDebuggerExecution() {
        if (debuggerExecutionComponentController == null) {
            return;
        }

        // Initialize the debugger execution component with the API client
        debuggerExecutionComponentController.initializeWithApiClient(apiClient);

        // Wire up instruction table callback for highlighting
        if (instructionTableComponentController != null) {
            debuggerExecutionComponentController.setInstructionTableCallback(instructionIndex -> {
                // Highlight the instruction in the instruction table
                instructionTableComponentController.highlightCurrentInstruction(instructionIndex);
            });
        }
    }

    private void loadHistoryChainFromServer(
            com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow selectedRow) {
        try {
            // Get current degree from the execution header
            int currentDegree = executionHeaderController != null ? executionHeaderController.getCurrentDegree() : 1;
            String programName = selectedProgram != null ? selectedProgram : selectedFunction;

            if (programName == null) {
                return;
            }

            // Convert row number (1-based) to instruction index (0-based)
            int instructionIndex = selectedRow.getRowNumber() - 1;

            // Call server endpoint
            String url = "/history/chain?program=" + java.net.URLEncoder.encode(programName, "UTF-8") +
                    "&instruction=" + instructionIndex +
                    "&degree=" + currentDegree;

            apiClient.get(url, ApiModels.HistoryChainResponse.class, null)
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response.success() && historyChainComponentController != null) {
                                // Display server response directly (no conversion needed!)
                                historyChainComponentController.displayHistoryChainFromServer(response.chain());
                            } else {
                                if (historyChainComponentController != null) {
                                    historyChainComponentController.clearHistory();
                                }
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            if (historyChainComponentController != null) {
                                historyChainComponentController.clearHistory();
                            }
                        });
                        return null;
                    });

        } catch (Exception e) {
            if (historyChainComponentController != null) {
                historyChainComponentController.clearHistory();
            }
        }
    }

    private List<com.semulator.engine.model.SInstruction> convertHistoryChainToInstructions(
            List<ApiModels.HistoryChainItem> chainItems) {
        List<com.semulator.engine.model.SInstruction> instructions = new ArrayList<>();

        for (ApiModels.HistoryChainItem item : chainItems) {
            try {
                com.semulator.engine.model.SInstruction instruction = createInstructionFromChainItem(item);
                if (instruction != null) {
                    instructions.add(instruction);
                }
            } catch (Exception e) {
                // Silently skip items that can't be converted
            }
        }

        return instructions;
    }

    private com.semulator.engine.model.SInstruction createInstructionFromChainItem(ApiModels.HistoryChainItem item) {
        try {
            com.semulator.engine.model.Variable variable = com.semulator.engine.model.Variable.of(item.variable());
            com.semulator.engine.model.Label label = new com.semulator.engine.model.LabelImpl(item.label());

            // Create appropriate instruction type based on command type and instruction
            // name
            switch (item.instructionName()) {
                case "INCREASE":
                    return new com.semulator.engine.model.IncreaseInstruction(variable, label);
                case "DECREASE":
                    return new com.semulator.engine.model.DecreaseInstruction(variable, label);
                case "NO_OP":
                case "NEUTRAL":
                    return new com.semulator.engine.model.NoOpInstruction(variable, label);
                case "ZERO_VARIABLE":
                    return new com.semulator.engine.model.ZeroVariableInstruction(variable, label);
                case "ASSIGN_VARIABLE":
                    return new com.semulator.engine.model.AssignVariableInstruction(variable, variable, label);
                case "ASSIGN_CONSTANT":
                    return new com.semulator.engine.model.AssignConstantInstruction(variable, 0, label);
                case "JUMP_NOT_ZERO":
                    return new com.semulator.engine.model.JumpNotZeroInstruction(variable, label);
                case "JUMP_ZERO":
                    return new com.semulator.engine.model.JumpZeroInstruction(variable, label);
                case "GOTO_LABEL":
                    return new com.semulator.engine.model.GotoLabelInstruction(label);
                default:
                    if (item.commandType().equals("B")) {
                        return new com.semulator.engine.model.IncreaseInstruction(variable, label);
                    } else {
                        return new com.semulator.engine.model.NoOpInstruction(variable, label);
                    }
            }
        } catch (Exception e) {
            return null;
        }
    }

    private com.semulator.engine.model.SInstruction createMockInstruction(
            com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow row) {
        try {
            com.semulator.engine.model.Variable variable = com.semulator.engine.model.Variable.of(row.getVariable());
            com.semulator.engine.model.Label label = new com.semulator.engine.model.LabelImpl(row.getLabel());

            // Create appropriate instruction type based on command type
            if (row.getCommandType().equals("B")) {
                return new com.semulator.engine.model.IncreaseInstruction(variable, label);
            } else {
                return new com.semulator.engine.model.NoOpInstruction(variable, label);
            }
        } catch (Exception e) {
            System.err.println("Error creating mock instruction: " + e.getMessage());
            return null;
        }
    }

    private void addBasicInstructionsToChain(List<com.semulator.engine.model.SInstruction> chain,
            com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow selectedRow) {
        try {
            // Add basic instructions that would have created this synthetic instruction
            com.semulator.engine.model.Variable var1 = com.semulator.engine.model.Variable.of("z1");
            com.semulator.engine.model.Variable var2 = com.semulator.engine.model.Variable.of("z2");

            chain.add(new com.semulator.engine.model.IncreaseInstruction(var1,
                    new com.semulator.engine.model.LabelImpl("L1")));
            chain.add(new com.semulator.engine.model.DecreaseInstruction(var2,
                    new com.semulator.engine.model.LabelImpl("L2")));
        } catch (Exception e) {
            System.err.println("Error adding basic instructions: " + e.getMessage());
        }
    }

    private void addAncestorInstructionsToChain(List<com.semulator.engine.model.SInstruction> chain,
            com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow selectedRow) {
        try {
            // Add even older instructions to show the full creation history
            com.semulator.engine.model.Variable inputVar = com.semulator.engine.model.Variable.of("x1");
            com.semulator.engine.model.Variable workVar = com.semulator.engine.model.Variable.of("z3");

            chain.add(new com.semulator.engine.model.AssignVariableInstruction(inputVar, workVar,
                    new com.semulator.engine.model.LabelImpl("L3")));
            chain.add(new com.semulator.engine.model.AssignConstantInstruction(workVar, 5,
                    new com.semulator.engine.model.LabelImpl("L4")));
        } catch (Exception e) {
            System.err.println("Error adding ancestor instructions: " + e.getMessage());
        }
    }

    private void addParentInstructions(List<com.semulator.engine.model.SInstruction> chain,
            com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow selectedRow) {
        try {
            // Add parent instructions (degree 2)
            com.semulator.engine.model.Variable var1 = com.semulator.engine.model.Variable.of("z1");
            com.semulator.engine.model.Variable var2 = com.semulator.engine.model.Variable.of("z2");

            chain.add(new com.semulator.engine.model.IncreaseInstruction(var1,
                    new com.semulator.engine.model.LabelImpl("L1")));
            chain.add(new com.semulator.engine.model.DecreaseInstruction(var2,
                    new com.semulator.engine.model.LabelImpl("L2")));
        } catch (Exception e) {
            System.err.println("Error adding parent instructions: " + e.getMessage());
        }
    }

    private void addGrandparentInstructions(List<com.semulator.engine.model.SInstruction> chain,
            com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow selectedRow) {
        try {
            // Add grandparent instructions (degree 3)
            com.semulator.engine.model.Variable inputVar = com.semulator.engine.model.Variable.of("x1");
            com.semulator.engine.model.Variable workVar = com.semulator.engine.model.Variable.of("z3");

            chain.add(new com.semulator.engine.model.AssignVariableInstruction(inputVar, workVar,
                    new com.semulator.engine.model.LabelImpl("L3")));
            chain.add(new com.semulator.engine.model.AssignConstantInstruction(workVar, 5,
                    new com.semulator.engine.model.LabelImpl("L4")));
        } catch (Exception e) {
            System.err.println("Error adding grandparent instructions: " + e.getMessage());
        }
    }

    private void addGreatGrandparentInstructions(List<com.semulator.engine.model.SInstruction> chain,
            com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow selectedRow) {
        try {
            // Add great-grandparent instructions (degree 4)
            com.semulator.engine.model.Variable resultVar = com.semulator.engine.model.Variable.RESULT;
            com.semulator.engine.model.Variable inputVar2 = com.semulator.engine.model.Variable.of("x2");

            chain.add(new com.semulator.engine.model.AssignVariableInstruction(resultVar, inputVar2,
                    new com.semulator.engine.model.LabelImpl("L5")));
            chain.add(new com.semulator.engine.model.ZeroVariableInstruction(inputVar2,
                    new com.semulator.engine.model.LabelImpl("L6")));
        } catch (Exception e) {
            System.err.println("Error adding great-grandparent instructions: " + e.getMessage());
        }
    }

    private void buildCreationChainFromServer(List<com.semulator.engine.model.SInstruction> chain,
            com.semulator.engine.model.SInstruction selectedInstruction, int currentDegree) {
        try {
            // Start with the selected instruction (already added)
            com.semulator.engine.model.SInstruction current = selectedInstruction;
            int currentDegreeForTracing = currentDegree;

            // For client-server architecture, we'll simulate the parent chain
            // In a real implementation, this would come from the server's expansion results

            while (currentDegreeForTracing > 0) {
                // Get the parent of the current instruction
                com.semulator.engine.model.SInstruction parent = findParentInstruction(current,
                        currentDegreeForTracing);

                if (parent == null) {
                    // No parent found, we've reached the end of the chain
                    break;
                }

                // Add parent to chain
                chain.add(parent);

                // Move to the previous degree
                currentDegreeForTracing--;
                current = parent;
            }

        } catch (Exception e) {
            System.err.println("Error building creation chain from server: " + e.getMessage());
        }
    }

    private com.semulator.engine.model.SInstruction findParentInstruction(
            com.semulator.engine.model.SInstruction current, int degree) {
        try {
            // Simulate finding parent instruction based on the current instruction
            // This mimics the parent mapping from Exercise 2's ExpansionResult

            if (degree <= 1) {
                return null; // No parent at degree 0
            }

            // Create a parent instruction based on the current instruction type
            String currentName = current.getName();
            com.semulator.engine.model.Variable currentVar = current.getVariable();
            com.semulator.engine.model.Label currentLabel = current.getLabel();

            // Generate parent instruction based on current instruction
            return createParentInstruction(currentName, currentVar, currentLabel, degree);

        } catch (Exception e) {
            System.err.println("Error finding parent instruction: " + e.getMessage());
            return null;
        }
    }

    private com.semulator.engine.model.SInstruction createParentInstruction(
            String currentName, com.semulator.engine.model.Variable currentVar,
            com.semulator.engine.model.Label currentLabel, int degree) {
        try {
            // Create parent instruction based on current instruction and degree
            // This simulates the expansion logic from Exercise 2

            switch (currentName) {
                case "NEUTRAL":
                case "INCREASE":
                case "DECREASE":
                    // Basic instructions - their parents are usually assignments
                    return new com.semulator.engine.model.AssignVariableInstruction(
                            currentVar,
                            com.semulator.engine.model.Variable.of("x" + degree),
                            new com.semulator.engine.model.LabelImpl("L" + degree));
                case "ASSIGNMENT":
                case "CONSTANT_ASSIGNMENT":
                    // Assignment instructions - their parents are usually constants or other
                    // variables
                    return new com.semulator.engine.model.AssignConstantInstruction(
                            currentVar,
                            degree * 2,
                            new com.semulator.engine.model.LabelImpl("L" + degree));
                default:
                    // Default parent - a basic instruction
                    return new com.semulator.engine.model.IncreaseInstruction(
                            currentVar,
                            new com.semulator.engine.model.LabelImpl("L" + degree));
            }
        } catch (Exception e) {
            System.err.println("Error creating parent instruction: " + e.getMessage());
            return null;
        }
    }

    private void setupEventHandlers() {
        // Navigation
        if (backToDashboardButton != null)
            backToDashboardButton.setOnAction(e -> navigateBackToDashboard());
    }

    private void loadInstructions() {
        if (selectedProgram == null && selectedFunction == null) {
            return;
        }

        String targetName = selectedProgram != null ? selectedProgram : selectedFunction;
        System.out.println("DEBUG: Attempting to load instructions for program: '" + targetName + "'");

        // URL encode the program name to handle special characters
        String encodedName;
        try {
            encodedName = java.net.URLEncoder.encode(targetName, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            encodedName = targetName; // Fallback to original name
        }

        System.out.println("DEBUG: API request URL: /programs?name=" + encodedName);

        // Load program/function instructions from server
        apiClient.get("/programs?name=" + encodedName, ApiModels.ProgramWithInstructions.class, null)
                .thenAccept(programWithInstructions -> {
                    Platform.runLater(() -> {
                        if (instructionTableComponentController != null && programWithInstructions != null) {
                            System.out.println("DEBUG: Displaying instructions directly from server");
                            System.out.println("DEBUG: Program name: " + programWithInstructions.name());
                            System.out.println("DEBUG: Instruction count: " +
                                    (programWithInstructions.instructions() != null
                                            ? programWithInstructions.instructions().size()
                                            : 0));

                            // Display instructions directly in the table without re-parsing
                            displayInstructionsInTable(programWithInstructions);

                            // Initialize the header with program info (name and maxDegree)
                            if (executionHeaderController != null) {
                                executionHeaderController.setProgramInfo(programWithInstructions.name(),
                                        programWithInstructions.maxDegree());
                            }

                            // Initialize the debugger execution component with program info
                            if (debuggerExecutionComponentController != null) {
                                debuggerExecutionComponentController.setProgramName(
                                        programWithInstructions.name(),
                                        programWithInstructions.maxDegree());

                                // Extract input variables from instructions
                                Set<String> inputVars = extractInputVariables(programWithInstructions);
                                if (!inputVars.isEmpty()) {
                                    debuggerExecutionComponentController.setInputVariables(new ArrayList<>(inputVars));
                                }
                            }
                        } else {
                            System.err.println("DEBUG: Component or program data is null");
                        }

                        updateSummaryLine();
                        validateArchitectureCompatibility();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        System.err.println("DEBUG: Server request failed for program: '" + targetName + "'");
                        System.err.println("DEBUG: Error details: " + throwable.getMessage());
                        if (throwable.getCause() != null) {
                            System.err.println("DEBUG: Root cause: " + throwable.getCause().getMessage());
                        }

                        // Fallback to sample data if server request fails
                        loadSampleInstructions();
                        showErrorAlert("Load Warning", "Failed to load instructions from server, using sample data: "
                                + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void loadInstructionsForDegree(int degree) {
        if (selectedProgram == null && selectedFunction == null) {
            return;
        }

        String targetName = selectedProgram != null ? selectedProgram : selectedFunction;
        String encodedName;
        try {
            encodedName = java.net.URLEncoder.encode(targetName, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            encodedName = targetName;
        }

        System.out.println("DEBUG: Loading instructions for degree " + degree);

        // Request expanded instructions from server for specific degree
        apiClient.get("/programs?name=" + encodedName + "&degree=" + degree,
                ApiModels.ProgramWithInstructions.class, null)
                .thenAccept(programWithInstructions -> {
                    Platform.runLater(() -> {
                        if (instructionTableComponentController != null && programWithInstructions != null) {
                            displayInstructionsInTable(programWithInstructions);
                            updateLabelVariableListFromInstructions(programWithInstructions);

                            // Update debugger component with new degree
                            if (debuggerExecutionComponentController != null) {
                                debuggerExecutionComponentController.updateDegree(degree);
                            }
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        System.err.println(
                                "Failed to load instructions for degree " + degree + ": " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void updateLabelVariableListFromInstructions(ApiModels.ProgramWithInstructions programData) {
        // Extract labels and variables from instructions and update the header
        Set<String> labelsAndVars = new HashSet<>();

        if (programData.instructions() != null) {
            for (Object instrObj : programData.instructions()) {
                if (instrObj instanceof com.google.gson.internal.LinkedTreeMap) {
                    @SuppressWarnings("unchecked")
                    com.google.gson.internal.LinkedTreeMap<String, Object> instr = (com.google.gson.internal.LinkedTreeMap<String, Object>) instrObj;

                    String label = (String) instr.get("label");
                    if (label != null && !label.isEmpty()) {
                        labelsAndVars.add(label);
                    }

                    String variable = (String) instr.get("variable");
                    if (variable != null && !variable.isEmpty()) {
                        labelsAndVars.add(variable);
                    }
                }
            }
        }

        if (executionHeaderController != null) {
            executionHeaderController.updateLabelVariableList(new ArrayList<>(labelsAndVars));
        }
    }

    private Set<String> extractInputVariables(ApiModels.ProgramWithInstructions programData) {
        // Extract input variables (x1, x2, x3, etc.) from instructions
        Set<String> inputVars = new java.util.TreeSet<>();

        if (programData.instructions() != null) {
            for (Object instrObj : programData.instructions()) {
                if (instrObj instanceof com.google.gson.internal.LinkedTreeMap) {
                    @SuppressWarnings("unchecked")
                    com.google.gson.internal.LinkedTreeMap<String, Object> instr = (com.google.gson.internal.LinkedTreeMap<String, Object>) instrObj;

                    // Check main variable field
                    String variable = (String) instr.get("variable");
                    if (variable != null && variable.startsWith("x") && variable.length() > 1) {
                        try {
                            Integer.parseInt(variable.substring(1));
                            inputVars.add(variable);
                        } catch (NumberFormatException e) {
                            // Not a valid input variable
                        }
                    }

                    // Check instruction text for x variables (for cases like function arguments)
                    String instructionText = (String) instr.get("instructionText");

                    // Also check other possible field names for instruction text
                    String instruction = (String) instr.get("instruction");

                    String text = (String) instr.get("text");

                    if (instructionText != null) {
                        // Look for x1, x2, x3, etc. in the instruction text
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\bx\\d+\\b");
                        java.util.regex.Matcher matcher = pattern.matcher(instructionText);
                        while (matcher.find()) {
                            String xVar = matcher.group();
                            inputVars.add(xVar);

                        }
                    } else if (instruction != null) {
                        // Try the instruction field instead
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\bx\\d+\\b");
                        java.util.regex.Matcher matcher = pattern.matcher(instruction);
                        while (matcher.find()) {
                            String xVar = matcher.group();
                            inputVars.add(xVar);

                        }
                    }
                }
            }
        }

        // If no x variables found, add at least x1 as a default for basic programs
        if (inputVars.isEmpty()) {
            inputVars.add("x1");
        }

        return inputVars;
    }

    private void displayInstructionsInTable(ApiModels.ProgramWithInstructions programData) {
        // Clear existing data
        instructionTableComponentController.clearTable();

        System.out.println("DEBUG: Displaying " +
                (programData.instructions() != null ? programData.instructions().size() : 0) + " instructions");

        // Get the table's data collection
        javafx.collections.ObservableList<com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow> tableData = javafx.collections.FXCollections
                .observableArrayList();

        // Convert server instruction DTOs to table rows
        if (programData.instructions() != null) {
            for (Object instrObj : programData.instructions()) {
                if (instrObj instanceof com.google.gson.internal.LinkedTreeMap) {
                    @SuppressWarnings("unchecked")
                    com.google.gson.internal.LinkedTreeMap<String, Object> instr = (com.google.gson.internal.LinkedTreeMap<String, Object>) instrObj;

                    int rowNumber = ((Double) instr.getOrDefault("rowNumber", 1.0)).intValue();
                    String commandType = (String) instr.getOrDefault("commandType", "B");
                    String label = (String) instr.getOrDefault("label", "");
                    String instruction = (String) instr.getOrDefault("instruction", "");
                    int cycles = ((Double) instr.getOrDefault("cycles", 1.0)).intValue();
                    String variable = (String) instr.getOrDefault("variable", "");
                    String architecture = (String) instr.getOrDefault("architecture", "I");

                    // Create instruction row for the table
                    com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow row = new com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow(
                            rowNumber, commandType, label, instruction, cycles, variable, architecture);

                    tableData.add(row);

                    System.out.println("DEBUG: Added instruction #" + rowNumber + ": " + instruction);
                }
            }
        }

        // Set the data in the table
        instructionTableComponentController.setInstructionData(tableData);

        System.out.println("DEBUG: Successfully displayed " + tableData.size() + " instructions in table");
    }

    private String convertToXml(ApiModels.ProgramWithInstructions programData) {
        // Convert the server response back to XML format for parsing
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<S-Program name=\"").append(escapeXml(programData.name())).append("\">\n");
        xml.append("  <S-instructions>\n");

        if (programData.instructions() != null) {
            for (Object instrObj : programData.instructions()) {
                if (instrObj instanceof com.google.gson.internal.LinkedTreeMap) {
                    @SuppressWarnings("unchecked")
                    com.google.gson.internal.LinkedTreeMap<String, Object> instr = (com.google.gson.internal.LinkedTreeMap<String, Object>) instrObj;

                    String name = (String) instr.get("name");
                    if (name == null)
                        name = "NEUTRAL";

                    xml.append("  <S-Instruction name=\"").append(name).append("\">\n");

                    // Add arguments
                    xml.append("    <S-Instruction-Arguments>\n");

                    if (instr.containsKey("variable")) {
                        xml.append("      <S-Instruction-Argument name=\"variable\" value=\"")
                                .append(escapeXml(String.valueOf(instr.get("variable")))).append("\" />\n");
                    }
                    if (instr.containsKey("label")) {
                        String label = String.valueOf(instr.get("label"));
                        if (label != null && !label.isEmpty() && !label.equals("null")) {
                            xml.append("      <S-Instruction-Argument name=\"label\" value=\"")
                                    .append(escapeXml(label)).append("\" />\n");
                        }
                    }

                    xml.append("    </S-Instruction-Arguments>\n");
                    xml.append("  </S-Instruction>\n");
                }
            }
        }

        xml.append("  </S-instructions>\n");
        xml.append("</S-Program>");
        return xml.toString();
    }

    private String escapeXml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private Object parseInstructionFromServer(Object instructionObj, int instructionNumber) {
        // Handle different instruction formats from server
        if (instructionObj instanceof com.google.gson.internal.LinkedTreeMap) {
            @SuppressWarnings("unchecked")
            com.google.gson.internal.LinkedTreeMap<String, Object> instructionMap = (com.google.gson.internal.LinkedTreeMap<String, Object>) instructionObj;

            String instructionType = (String) instructionMap.get("type");
            String instructionText = (String) instructionMap.get("instruction");
            Integer cycles = ((Double) instructionMap.get("cycles")).intValue();
            String architecture = (String) instructionMap.getOrDefault("architecture", "I");
            String bs = (String) instructionMap.getOrDefault("bs", "B");

            // Return the parsed instruction data as a map for now
            // TODO: Convert to proper SInstruction object for component
            return instructionMap;
        }

        return null;
    }

    private void loadSampleInstructions() {
        if (instructionTableComponentController != null) {
            // For now, we'll create a simple sample program with basic instructions
            // TODO: Implement proper SProgram creation from server data
            System.out.println("Loading sample instructions for component integration");

            // Clear any existing data
            instructionTableComponentController.clearTable();

            // The component will handle its own data management
            // We just need to trigger the summary update
            updateSummaryLine();
            validateArchitectureCompatibility();
        }
    }

    private void updateSummaryLine() {
        if (summaryLine == null)
            return;

        // For now, display a placeholder summary
        // TODO: Get actual instruction counts from the component
        String summary = "Architecture Support: I:0 II:0 III:0 IV:0";
        summaryLine.setText(summary);
    }

    private void validateArchitectureCompatibility() {
        // TODO: Implement architecture compatibility validation with components
        // For now, this is a placeholder
        System.out.println("Architecture compatibility validation - placeholder");
    }

    private boolean isArchitectureSupported(String selectedArch, String instructionArch) {
        // Architecture hierarchy: I < II < III < IV
        // Higher architectures support lower ones
        return instructionArch.equals("I") ||
                (instructionArch.equals("II")
                        && (selectedArch.equals("II") || selectedArch.equals("III") || selectedArch.equals("IV")))
                ||
                (instructionArch.equals("III") && (selectedArch.equals("III") || selectedArch.equals("IV"))) ||
                instructionArch.equals("IV") && selectedArch.equals("IV");
    }

    private void updateHistoryChain(Object selectedInstruction) {
        // History chain is now handled by the component
        // This method is kept for compatibility but functionality moved to component
        System.out.println("History chain update - now handled by component");
    }

    // All execution command handling is now done by the DebuggerExecution component

    private void startStatusUpdateTimer() {
        // Timer for updating credits and other status
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    updateCreditsDisplay();
                });
            }
        }, 0, 5000); // Update every 5 seconds
    }

    private void updateCreditsDisplay() {
        // Fetch current user credits from server
        apiClient.get("/users", ApiModels.UsersResponse.class, null)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        // Find current user's credits
                        for (ApiModels.UserInfo user : response.users()) {
                            if (user.username().equals(currentUser)) {
                                availableCredits.setText(String.valueOf(user.credits()));
                                break;
                            }
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        // Keep current display on error
                        System.err.println("Failed to update credits: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void navigateBackToDashboard() {
        // Clean up resources (now handled by DebuggerExecution component)

        try {
            // Load dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            Stage stage = (Stage) backToDashboardButton.getScene().getWindow();
            stage.setTitle("S-Emulator - " + currentUser);
            stage.setScene(scene);
            stage.setResizable(true);

        } catch (IOException e) {
            showErrorAlert("Navigation Error", "Failed to navigate back to dashboard: " + e.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Set the target program or function for execution
     * Called from DashboardController when navigating to execution screen
     */
    public void setTarget(String targetName, String targetType) {
        System.out.println("ProgramRunController: Setting target - " + targetType + ": " + targetName);

        if ("PROGRAM".equals(targetType)) {
            this.selectedProgram = targetName;
            this.selectedFunction = null;
        } else if ("FUNCTION".equals(targetType)) {
            this.selectedProgram = null;
            this.selectedFunction = targetName;
        }

        this.targetType = targetType;

        // Load the program/function instructions
        loadInstructions();
        updateSummaryLine();
    }

    // Data Model Classes (now handled by components)

    public static class VariableRow {
        private final String name;
        private final int value;

        public VariableRow(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
