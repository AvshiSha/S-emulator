package com.semulator.client.ui;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.service.ApiClient;
import com.semulator.engine.model.ExpansionResult;
import com.semulator.engine.model.SInstruction;
import com.semulator.engine.parse.SProgramImpl;
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

    // Summary Line
    @FXML
    private Label summaryLine;

    // Selected Instruction History Chain
    // History Chain Table (now handled by component)

    // Right Panel - Debugger/Execution
    @FXML
    private VBox rightPanel;
    @FXML
    private Label debuggerTitle;

    // Debugger/Execution Commands (Yellow Box)
    @FXML
    private VBox executionCommandsBox;
    @FXML
    private Button startExecutionButton;
    @FXML
    private Button startRegularButton;
    @FXML
    private Button startDebugButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button resumeButton;
    @FXML
    private Button stepForwardButton;
    @FXML
    private Button stepBackwardButton;

    // Architecture Selection (Blue Box)
    @FXML
    private VBox architectureSelectionBox;
    @FXML
    private ComboBox<String> architectureComboBox;

    // Variables (Magenta Box)
    @FXML
    private VBox variablesBox;
    @FXML
    private TableView<VariableRow> variablesTable;
    @FXML
    private TableColumn<VariableRow, String> variableNameColumn;
    @FXML
    private TableColumn<VariableRow, Integer> variableValueColumn;

    // Execution Inputs (Blue Box)
    @FXML
    private VBox executionInputsBox;
    @FXML
    private TextField executionInputField;
    @FXML
    private Button setInputButton;

    // Cycles (Magenta Box)
    @FXML
    private VBox cyclesBox;
    @FXML
    private Label cyclesLabel;

    // Navigation
    @FXML
    private Button backToDashboardButton;

    // Data Models
    // Data collections (now handled by components)
    private ObservableList<VariableRow> variablesData = FXCollections.observableArrayList();

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
            initializeVariablesTable();
            initializeArchitectureSelection();
            initializeExecutionCommands();

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
        if (instructionTableComponentController == null)
            return;

        // Initialize the instruction table component
        instructionTableComponentController.initializeWithHttp();

        // Set up selection callback for history chain
        instructionTableComponentController.setHistoryChainCallback(instruction -> {
            if (instruction != null && historyChainComponentController != null) {
                // Create a simple history chain with just the selected instruction
                java.util.List<com.semulator.engine.model.SInstruction> chain = new java.util.ArrayList<>();
                chain.add(instruction);
                historyChainComponentController.displayHistoryChain(chain);
            } else if (historyChainComponentController != null) {
                historyChainComponentController.clearHistory();
            }
        });
    }

    private void initializeHistoryTable() {
        if (historyChainComponentController == null)
            return;

        // Initialize the history chain component
        historyChainComponentController.initializeWithHttp();
    }

    private void initializeVariablesTable() {
        if (variablesTable != null) {
            if (variableNameColumn != null)
                variableNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            if (variableValueColumn != null)
                variableValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

            variablesTable.setItems(variablesData);
        }
    }

    private void initializeArchitectureSelection() {
        if (architectureComboBox != null) {
            // Initialize architecture combo box with options I-IV
            architectureComboBox.getItems().addAll("I", "II", "III", "IV");
            architectureComboBox.setValue("I");

            // Set up selection handler
            architectureComboBox.setOnAction(e -> {
                selectedArchitecture = architectureComboBox.getValue();
                updateSummaryLine();
                validateArchitectureCompatibility();
            });
        }
    }

    private void initializeExecutionCommands() {
        // Initially disable execution buttons
        if (stopButton != null)
            stopButton.setDisable(true);
        if (resumeButton != null)
            resumeButton.setDisable(true);
        if (stepForwardButton != null)
            stepForwardButton.setDisable(true);
        if (stepBackwardButton != null)
            stepBackwardButton.setDisable(true);
    }

    private void setupEventHandlers() {
        // Execution commands
        if (startExecutionButton != null)
            startExecutionButton.setOnAction(e -> handleStartExecution());
        if (startRegularButton != null)
            startRegularButton.setOnAction(e -> handleStartRegular());
        if (startDebugButton != null)
            startDebugButton.setOnAction(e -> handleStartDebug());
        if (stopButton != null)
            stopButton.setOnAction(e -> handleStop());
        if (resumeButton != null)
            resumeButton.setOnAction(e -> handleResume());
        if (stepForwardButton != null)
            stepForwardButton.setOnAction(e -> handleStepForward());
        if (stepBackwardButton != null)
            stepBackwardButton.setOnAction(e -> handleStepBackward());

        // Navigation
        if (backToDashboardButton != null)
            backToDashboardButton.setOnAction(e -> navigateBackToDashboard());

        // Input handling
        if (setInputButton != null)
            setInputButton.setOnAction(e -> handleSetInput());
    }

    public void setTarget(String targetName, String targetType) {
        this.selectedProgram = targetType.equals("PROGRAM") ? targetName : null;
        this.selectedFunction = targetType.equals("FUNCTION") ? targetName : null;
        this.targetType = targetType;

        // Load program/function instructions
        loadInstructions();
        updateSummaryLine();
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

    private void displayExpandedInstructions(ExpansionResult expansionResult) {
        // Display expanded instructions from the expansion result
        javafx.collections.ObservableList<com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow> tableData = javafx.collections.FXCollections
                .observableArrayList();

        List<SInstruction> instructions = expansionResult.instructions();
        System.out.println("DEBUG: Displaying " + instructions.size() + " expanded instructions");

        for (int i = 0; i < instructions.size(); i++) {
            SInstruction instruction = instructions.get(i);

            String labelName = "";
            if (instruction.getLabel() != null) {
                if (instruction.getLabel().isExit()) {
                    labelName = "EXIT";
                } else {
                    labelName = instruction.getLabel().getLabel();
                }
            }

            String variableName = "";
            if (instruction.getVariable() != null) {
                variableName = instruction.getVariable().toString();
            }

            String commandType = getCommandTypeForInstruction(instruction);
            String instructionText = getInstructionTextForDisplay(instruction);
            String architecture = getArchitectureForInstructionClient(instruction);

            com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow row = new com.semulator.client.ui.components.InstructionTable.InstructionTable.InstructionRow(
                    i + 1, commandType, labelName, instructionText, instruction.cycles(), variableName, architecture);

            tableData.add(row);
        }

        instructionTableComponentController.setInstructionData(tableData);
        updateSummaryLine();
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

    // Execution Command Handlers
    private void handleStartExecution() {
        validateAndExecute(false); // Regular execution
    }

    private void handleStartRegular() {
        validateAndExecute(false); // Regular execution
    }

    private void handleStartDebug() {
        validateAndExecute(true); // Debug execution
    }

    private void validateAndExecute(boolean debugMode) {
        // Get current user credits and validate
        apiClient.get("/users", ApiModels.UsersResponse.class, null)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        // Find current user's credits
                        int userCredits = 0;
                        for (ApiModels.UserInfo user : response.users()) {
                            if (user.username().equals(currentUser)) {
                                userCredits = user.credits();
                                break;
                            }
                        }

                        // Calculate estimated cost
                        int architectureCost = getArchitectureCost(selectedArchitecture);
                        int estimatedExecutionCost = calculateEstimatedCost();
                        int totalCost = architectureCost + estimatedExecutionCost;

                        if (userCredits < totalCost) {
                            showErrorAlert("Insufficient Credits",
                                    "You need " + totalCost + " credits to run this program.\n" +
                                            "Architecture cost: " + architectureCost + " credits\n" +
                                            "Estimated execution cost: " + estimatedExecutionCost + " credits\n" +
                                            "Current credits: " + userCredits + "\n\n" +
                                            "Please add more credits before running the program.");
                        } else {
                            // Credits are sufficient, proceed with execution
                            startExecution(debugMode);
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showErrorAlert("Credit Validation Failed",
                                "Failed to validate credits: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void handleStop() {
        if (currentRunId != null) {
            stopExecution();
        }
    }

    private void handleResume() {
        if (currentRunId != null) {
            resumeExecution();
        }
    }

    private void handleStepForward() {
        if (currentRunId != null && isDebugging) {
            stepExecution(true);
        }
    }

    private void handleStepBackward() {
        if (currentRunId != null && isDebugging) {
            stepExecution(false);
        }
    }

    private void handleSetInput() {
        // TODO: Handle input setting
        String input = executionInputField.getText().trim();
        if (!input.isEmpty()) {
            // Process input
            executionInputField.clear();
        }
    }

    private int getArchitectureCost(String architecture) {
        // Architecture costs: I=10, II=20, III=30, IV=40 credits
        switch (architecture) {
            case "I":
                return 10;
            case "II":
                return 20;
            case "III":
                return 30;
            case "IV":
                return 40;
            default:
                return 10;
        }
    }

    private int calculateEstimatedCost() {
        // Estimate cost based on instruction count and cycles
        // TODO: Get actual cycle count from component
        int totalCycles = 10; // Placeholder estimate

        // Each cycle costs 1 credit
        return totalCycles;
    }

    private void startExecution(boolean debugMode) {
        // Create run target
        ApiModels.RunTarget target = new ApiModels.RunTarget(
                targetType,
                targetType.equals("PROGRAM") ? selectedProgram : selectedFunction);

        // Create start request
        ApiModels.StartRequest startRequest = new ApiModels.StartRequest(
                target,
                selectedArchitecture,
                1, // degree - TODO: Make this configurable
                new HashMap<>() // inputs - TODO: Add input handling
        );

        // Send start request to server
        apiClient.post("/run/start", startRequest, ApiModels.StartResponse.class)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        currentRunId = response.runId();
                        isExecuting = true;
                        isDebugging = debugMode;

                        // Update UI state
                        updateExecutionButtons();

                        // Start status polling
                        startStatusPolling();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showErrorAlert("Execution Failed", "Failed to start execution: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void stopExecution() {
        if (currentRunId == null)
            return;

        ApiModels.CancelRequest cancelRequest = new ApiModels.CancelRequest(currentRunId);

        apiClient.post("/run/cancel", cancelRequest, String.class)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        currentRunId = null;
                        isExecuting = false;
                        isDebugging = false;
                        updateExecutionButtons();
                        stopStatusPolling();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showErrorAlert("Stop Failed", "Failed to stop execution: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void resumeExecution() {
        // TODO: Implement resume functionality
        // This would depend on server API support
    }

    private void stepExecution(boolean forward) {
        if (currentRunId == null)
            return;

        String endpoint = forward ? "/debug/step" : "/debug/stepBack"; // stepBack might not exist
        ApiModels.DebugRequest debugRequest = new ApiModels.DebugRequest(currentRunId);

        apiClient.post(endpoint, debugRequest, ApiModels.DebugResponse.class)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.success()) {
                            // Update UI with new state
                            updateExecutionState();
                        } else {
                            showErrorAlert("Step Failed", response.message());
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showErrorAlert("Step Failed", "Failed to step execution: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void startStatusPolling() {
        if (statusUpdateTimer != null) {
            statusUpdateTimer.cancel();
        }

        statusUpdateTimer = new Timer(true);
        statusUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currentRunId != null) {
                    pollExecutionStatus();
                }
            }
        }, 0, 1000); // Poll every second
    }

    private void stopStatusPolling() {
        if (statusUpdateTimer != null) {
            statusUpdateTimer.cancel();
            statusUpdateTimer = null;
        }
    }

    private void pollExecutionStatus() {
        if (currentRunId == null)
            return;

        apiClient.get("/run/status?runId=" + currentRunId, ApiModels.StatusResponse.class, null)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        updateExecutionState(response);
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        System.err.println("Failed to poll status: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void updateExecutionState() {
        // Update based on current state
        // This is called after step operations
    }

    private void updateExecutionState(ApiModels.StatusResponse status) {
        // Update cycles display
        cyclesLabel.setText("Cycles: " + status.cycles());

        // Update variables if available
        if (status.instrByArch() != null) {
            variablesData.clear();
            for (Map.Entry<String, Integer> entry : status.instrByArch().entrySet()) {
                variablesData.add(new VariableRow(entry.getKey(), entry.getValue()));
            }
        }

        // Update credits in real-time (deduct 1 credit per cycle)
        updateCreditsDisplay();

        // Check if execution is complete
        if ("COMPLETED".equals(status.state()) || "ERROR".equals(status.state())) {
            isExecuting = false;
            isDebugging = false;
            currentRunId = null;
            updateExecutionButtons();
            stopStatusPolling();

            if ("ERROR".equals(status.state()) && status.error() != null) {
                if (status.error().contains("insufficient credits") || status.error().contains("out of credits")) {
                    showErrorAlert("Out of Credits",
                            "Execution stopped: You have run out of credits.\n" +
                                    "Returning to Programs screen.");
                    // Navigate back to dashboard after a delay
                    Platform.runLater(() -> {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        navigateBackToDashboard();
                    });
                } else {
                    showErrorAlert("Execution Error", status.error());
                }
            }
        }
    }

    private void updateExecutionButtons() {
        boolean canStart = !isExecuting && instructionTableComponentController != null;
        boolean canStop = isExecuting;
        boolean canDebug = isDebugging;

        startExecutionButton.setDisable(!canStart);
        startRegularButton.setDisable(!canStart);
        startDebugButton.setDisable(!canStart);

        stopButton.setDisable(!canStop);
        resumeButton.setDisable(!canStop);
        stepForwardButton.setDisable(!canDebug);
        stepBackwardButton.setDisable(!canDebug);
    }

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
        // Clean up resources
        stopStatusPolling();

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

    // Data Model Classes (now handled by components)

    private void initializeHeaderWithProgram(ApiModels.ProgramWithInstructions programData) {
        if (executionHeaderController == null)
            return;

        try {
            // Parse the program XML to get an SProgram object for the header
            String programXml = convertToXml(programData);
            SProgramImpl program = new SProgramImpl(programData.name());
            program.loadFromXmlContent(programXml);

            // Set the program in the header for degree expansion
            executionHeaderController.setProgram(program);

            System.out.println("DEBUG: Header initialized with program");
            System.out.println("DEBUG: Program maxDegree from server: " + programData.maxDegree());
            System.out.println("DEBUG: Program maxDegree calculated: " + program.calculateMaxDegree());
        } catch (Exception e) {
            System.err.println("Failed to initialize header with program: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getCommandTypeForInstruction(SInstruction instruction) {
        if (instruction instanceof com.semulator.engine.model.IncreaseInstruction ||
                instruction instanceof com.semulator.engine.model.DecreaseInstruction ||
                instruction instanceof com.semulator.engine.model.NoOpInstruction ||
                instruction instanceof com.semulator.engine.model.JumpNotZeroInstruction) {
            return "B";
        }
        return "S";
    }

    private String getInstructionTextForDisplay(SInstruction instruction) {
        if (instruction instanceof com.semulator.engine.model.IncreaseInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable() + " + 1";
        } else if (instruction instanceof com.semulator.engine.model.DecreaseInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable() + " - 1";
        } else if (instruction instanceof com.semulator.engine.model.NoOpInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable();
        } else if (instruction instanceof com.semulator.engine.model.JumpNotZeroInstruction) {
            com.semulator.engine.model.JumpNotZeroInstruction jnz = (com.semulator.engine.model.JumpNotZeroInstruction) instruction;
            return "IF " + jnz.getVariable() + " != 0 GOTO " + jnz.getTarget();
        } else if (instruction instanceof com.semulator.engine.model.ZeroVariableInstruction) {
            return instruction.getVariable() + " <- 0";
        } else if (instruction instanceof com.semulator.engine.model.AssignVariableInstruction) {
            com.semulator.engine.model.AssignVariableInstruction assign = (com.semulator.engine.model.AssignVariableInstruction) instruction;
            return instruction.getVariable() + " <- " + assign.getSource();
        } else if (instruction instanceof com.semulator.engine.model.AssignConstantInstruction) {
            com.semulator.engine.model.AssignConstantInstruction assign = (com.semulator.engine.model.AssignConstantInstruction) instruction;
            return instruction.getVariable() + " <- " + assign.getConstant();
        } else if (instruction instanceof com.semulator.engine.model.GotoLabelInstruction) {
            com.semulator.engine.model.GotoLabelInstruction goto_ = (com.semulator.engine.model.GotoLabelInstruction) instruction;
            return "GOTO " + goto_.getTarget();
        } else if (instruction instanceof com.semulator.engine.model.JumpZeroInstruction) {
            com.semulator.engine.model.JumpZeroInstruction jz = (com.semulator.engine.model.JumpZeroInstruction) instruction;
            return "IF " + jz.getVariable() + " == 0 GOTO " + jz.getTarget();
        } else if (instruction instanceof com.semulator.engine.model.QuoteInstruction) {
            com.semulator.engine.model.QuoteInstruction quote = (com.semulator.engine.model.QuoteInstruction) instruction;
            return instruction.getVariable() + " <- (" + quote.getFunctionName() + ", ...)";
        }
        return instruction.getName();
    }

    private String getArchitectureForInstructionClient(SInstruction instruction) {
        if (instruction instanceof com.semulator.engine.model.IncreaseInstruction ||
                instruction instanceof com.semulator.engine.model.DecreaseInstruction ||
                instruction instanceof com.semulator.engine.model.NoOpInstruction ||
                instruction instanceof com.semulator.engine.model.JumpNotZeroInstruction) {
            return "I";
        } else if (instruction instanceof com.semulator.engine.model.ZeroVariableInstruction ||
                instruction instanceof com.semulator.engine.model.AssignVariableInstruction ||
                instruction instanceof com.semulator.engine.model.AssignConstantInstruction ||
                instruction instanceof com.semulator.engine.model.GotoLabelInstruction) {
            return "II";
        } else if (instruction instanceof com.semulator.engine.model.JumpZeroInstruction ||
                instruction instanceof com.semulator.engine.model.JumpEqualConstantInstruction ||
                instruction instanceof com.semulator.engine.model.JumpEqualVariableInstruction) {
            return "III";
        } else if (instruction instanceof com.semulator.engine.model.QuoteInstruction ||
                instruction instanceof com.semulator.engine.model.JumpEqualFunctionInstruction) {
            return "IV";
        }
        return "I";
    }

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
