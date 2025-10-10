package com.semulator.client.ui.components.InstructionTable;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.service.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Updated imports for new package structure
import com.semulator.engine.model.SInstruction;
import com.semulator.engine.model.SProgram;
import com.semulator.engine.parse.SProgramImpl;
import com.semulator.engine.model.Label;
import com.semulator.engine.model.IncreaseInstruction;
import com.semulator.engine.model.DecreaseInstruction;
import com.semulator.engine.model.NoOpInstruction;
import com.semulator.engine.model.JumpNotZeroInstruction;
import com.semulator.engine.model.ZeroVariableInstruction;
import com.semulator.engine.model.AssignVariableInstruction;
import com.semulator.engine.model.AssignConstantInstruction;
import com.semulator.engine.model.GotoLabelInstruction;
import com.semulator.engine.model.JumpZeroInstruction;
import com.semulator.engine.model.JumpEqualConstantInstruction;
import com.semulator.engine.model.JumpEqualVariableInstruction;
import com.semulator.engine.model.QuoteInstruction;
import com.semulator.engine.model.JumpEqualFunctionInstruction;
import com.semulator.engine.model.FunctionArgument;
import com.semulator.engine.model.FunctionCall;

public class InstructionTable {
    @FXML
    private TableView<InstructionRow> instructionTableView;

    public TableView<InstructionRow> getInstructionTableView() {
        return instructionTableView;
    }

    @FXML
    private TableColumn<InstructionRow, Integer> rowNumberColumn;

    @FXML
    private TableColumn<InstructionRow, String> commandTypeColumn;

    @FXML
    private TableColumn<InstructionRow, String> labelColumn;

    @FXML
    private TableColumn<InstructionRow, String> instructionTypeColumn;

    @FXML
    private TableColumn<InstructionRow, Integer> cyclesColumn;

    @FXML
    private TableColumn<InstructionRow, String> architectureColumn;

    private ObservableList<InstructionRow> instructionData = FXCollections.observableArrayList();

    // For highlighting functionality
    private String currentHighlightTerm = null;
    private int currentExecutingInstructionIndex = -1; // -1 means no instruction is executing

    // Store references to table rows for animation
    private java.util.Map<Integer, TableRow<InstructionRow>> rowCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Store current program to access functions
    private SProgram currentProgram;

    // API Client for HTTP communication
    private ApiClient apiClient;

    public void initializeWithHttp() {
        // Initialize API client
        this.apiClient = AppContext.getInstance().getApiClient();

        // Set up the table columns
        setupTableColumns();

        // Set up row highlighting
        setupRowHighlighting();

        // InstructionTable initialized
    }

    private void setupTableColumns() {
        // Set up the table columns
        rowNumberColumn.setCellValueFactory(cellData -> cellData.getValue().rowNumberProperty().asObject());
        commandTypeColumn.setCellValueFactory(cellData -> cellData.getValue().commandTypeProperty());
        labelColumn.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        instructionTypeColumn.setCellValueFactory(cellData -> cellData.getValue().instructionTypeProperty());
        cyclesColumn.setCellValueFactory(cellData -> cellData.getValue().cyclesProperty().asObject());
        architectureColumn.setCellValueFactory(cellData -> cellData.getValue().architectureProperty());

        // Set the data source for the table
        instructionTableView.setItems(instructionData);

        // Make the instruction type column resizable to handle different screen sizes
        instructionTypeColumn.setResizable(true);
        instructionTypeColumn.setSortable(false);

        // Make other columns resizable but with constraints
        rowNumberColumn.setResizable(true);
        commandTypeColumn.setResizable(true);
        labelColumn.setResizable(true);
        cyclesColumn.setResizable(true);
        architectureColumn.setResizable(true);

        // Set sortable to false for all columns to maintain order
        rowNumberColumn.setSortable(false);
        commandTypeColumn.setSortable(false);
        labelColumn.setSortable(false);
        cyclesColumn.setSortable(false);
        architectureColumn.setSortable(false);
    }

    @FXML
    private void initialize() {
        // This method is called by FXML automatically
        // The actual initialization is done in initializeWithHttp()
    }

    public void displayProgram(SProgram program) {

        // Add safety checks for large datasets
        if (program == null) {
            // Completely disable the table and clear all data
            try {
                instructionTableView.setDisable(true);
                instructionTableView.getSelectionModel().clearSelection();
                instructionData.clear();
                rowCache.clear();
                currentInstructions.clear();
                currentHighlightTerm = null;
                currentExecutingInstructionIndex = -1;
                currentProgram = null;
                // Don't re-enable the table for empty programs
            } catch (Exception e) {
                System.err.println("Error clearing empty program: " + e.getMessage());
            }
            return;
        }

        if (program.getInstructions() == null) {
            // Completely disable the table and clear all data
            try {
                instructionTableView.setDisable(true);
                instructionTableView.getSelectionModel().clearSelection();
                instructionData.clear();
                rowCache.clear();
                currentInstructions.clear();
                currentHighlightTerm = null;
                currentExecutingInstructionIndex = -1;
                currentProgram = null;
                // Don't re-enable the table for empty programs
            } catch (Exception e) {
                System.err.println("Error clearing empty program: " + e.getMessage());
            }
            return;
        }

        // // Check if the instruction list is too large to prevent UI issues
        List<SInstruction> instructions = program.getInstructions();
        // if (instructions.size() > 1000) {
        // System.err.println("Warning: Large instruction set (" + instructions.size()
        // + " instructions) may cause UI performance issues");
        // }

        // Clear data safely with complete table protection
        try {
            // Completely disable the table to prevent any user interaction
            instructionTableView.setDisable(true);

            // Clear selection model safely
            try {
                instructionTableView.getSelectionModel().clearSelection();
            } catch (Exception selectionException) {
                System.err.println("Warning: Could not clear selection: " + selectionException.getMessage());
            }

            // Clear data in a synchronized manner
            instructionData.clear();
            rowCache.clear(); // Clear the row cache when displaying a new program
            currentInstructions.clear();
            currentHighlightTerm = null; // Clear any existing highlighting
            currentExecutingInstructionIndex = -1; // Clear current instruction highlighting
            currentProgram = program; // Store program reference
        } catch (Exception e) {
            System.err.println("Error clearing instruction table data: " + e.getMessage());
            return;
        }

        currentInstructions.addAll(instructions); // Store for selection handling

        // Get user-strings if available
        Map<String, String> functionUserStrings = null;
        if (program instanceof SProgramImpl) {
            SProgramImpl programImpl = (SProgramImpl) program;
            functionUserStrings = programImpl.getFunctionUserStrings();
        }

        // Process instructions with safety checks and selection protection
        try {
            // Temporarily disable selection model to prevent race conditions
            instructionTableView.setDisable(true);

            for (int i = 0; i < instructions.size(); i++) {
                try {
                    SInstruction instruction = instructions.get(i);
                    if (instruction == null) {
                        System.err.println("Warning: Null instruction at index " + i);
                        continue;
                    }

                    String variable = "";
                    try {
                        variable = instruction.getVariable().toString();
                    } catch (Exception e) {
                        // Some instructions don't have getVariable() method
                        variable = "";
                    }

                    InstructionRow row = new InstructionRow(
                            i + 1, // Row number (1-based)
                            getCommandType(instruction), // B or S
                            getLabelText(instruction.getLabel()), // Label text
                            getInstructionText(instruction, functionUserStrings), // Instruction description
                            instruction.cycles(), // Cycles
                            variable,
                            getArchitectureForInstruction(instruction)); // Architecture
                    instructionData.add(row);
                } catch (Exception e) {
                    System.err.println("Error processing instruction at index " + i + ": " + e.getMessage());
                    // Continue processing other instructions
                }
            }

            // Don't re-enable the table immediately - use a delayed approach
            // This prevents the IndexOutOfBoundsException by ensuring all data is stable
            javafx.application.Platform.runLater(() -> {
                try {
                    // Add a small delay to ensure all internal JavaFX operations are complete
                    javafx.concurrent.Task<Void> enableTask = new javafx.concurrent.Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            Thread.sleep(200); // Increased delay to 200ms for better stability
                            return null;
                        }

                        @Override
                        protected void succeeded() {
                            try {
                                // Completely re-enable the table and make it visible
                                instructionTableView.setVisible(true);
                                instructionTableView.setMouseTransparent(false);
                                instructionTableView.setDisable(false);

                                // Add a custom mouse event filter to prevent problematic clicks
                                instructionTableView.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                                        event -> {
                                            try {
                                                // Check if the table has valid data before allowing clicks
                                                if (instructionData.isEmpty() || instructionTableView.isDisable()) {
                                                    event.consume();
                                                    return;
                                                }

                                                // Additional safety check for selection model
                                                if (instructionTableView.getSelectionModel() == null) {
                                                    event.consume();
                                                    return;
                                                }

                                                // Allow the event to proceed only if all checks pass
                                            } catch (Exception e) {
                                                System.err.println("Error in mouse event filter: " + e.getMessage());
                                                event.consume();
                                            }
                                        });

                            } catch (Exception e) {
                                System.err.println("Error re-enabling table: " + e.getMessage());
                            }
                        }

                        @Override
                        protected void failed() {
                            System.err.println("Error in table enable task: " + getException().getMessage());
                            // Try to re-enable anyway
                            try {
                                instructionTableView.setDisable(false);
                            } catch (Exception e) {
                                System.err.println("Failed to re-enable table: " + e.getMessage());
                            }
                        }
                    };

                    javafx.concurrent.Service<Void> enableService = new javafx.concurrent.Service<Void>() {
                        @Override
                        protected javafx.concurrent.Task<Void> createTask() {
                            return enableTask;
                        }
                    };

                    enableService.start();
                } catch (Exception e) {
                    System.err.println("Error setting up table enable task: " + e.getMessage());
                    // Fallback: try to enable immediately
                    try {
                        instructionTableView.setDisable(false);
                    } catch (Exception enableException) {
                        System.err.println("Failed to enable table in fallback: " + enableException.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error processing instruction list: " + e.getMessage());
            e.printStackTrace();
            // Make sure to re-enable the table even if there's an error
            try {
                instructionTableView.setDisable(false);
            } catch (Exception enableException) {
                System.err.println("Failed to enable table after error: " + enableException.getMessage());
            }
        }

        // Refresh the table to display the new data with safety checks
        try {
            instructionTableView.refresh();
        } catch (Exception e) {
            System.err.println("Error refreshing instruction table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void clearTable() {
        instructionData.clear();
        rowCache.clear(); // Clear the row cache when clearing the table
    }

    public void setInstructionData(javafx.collections.ObservableList<InstructionRow> data) {
        instructionData.clear();
        instructionData.addAll(data);
        instructionTableView.setItems(instructionData);
        instructionTableView.refresh();
        System.out.println("InstructionTable: Set " + data.size() + " instructions");
    }

    public void setTableEnabled(boolean enabled) {
        try {
            if (!enabled) {
                // CRITICAL FIX: Completely hide and disable table to prevent
                // IndexOutOfBoundsException
                instructionTableView.setVisible(false);
                instructionTableView.setDisable(true);
                instructionTableView.setMouseTransparent(true);

            } else {
                // Re-enable the table
                instructionTableView.setVisible(true);
                instructionTableView.setDisable(false);
                instructionTableView.setMouseTransparent(false);

            }

        } catch (Exception e) {
            System.err.println("Error setting table enabled state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isTableEnabled() {
        try {
            return !instructionTableView.isDisable();
        } catch (Exception e) {
            System.err.println("Error checking table enabled state: " + e.getMessage());
            return false;
        }
    }

    public void setHistoryChainCallback(Consumer<SInstruction> callback) {
        // Use a completely different approach - disable the built-in selection model
        // and implement our own selection mechanism to avoid IndexOutOfBoundsException
        try {
            // Completely disable the built-in selection model to prevent the
            // IndexOutOfBoundsException
            instructionTableView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);

            // Add a custom selection listener with comprehensive error handling
            instructionTableView.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldSelection, newSelection) -> {
                        try {
                            // Add a small delay to prevent race conditions
                            javafx.application.Platform.runLater(() -> {
                                try {
                                    // Comprehensive safety checks before processing selection
                                    if (newSelection != null && !instructionTableView.isDisable() &&
                                            !instructionData.isEmpty() && !currentInstructions.isEmpty()) {

                                        // Get the selected instruction index with comprehensive safety checks
                                        int selectedIndex = instructionTableView.getSelectionModel().getSelectedIndex();
                                        if (selectedIndex >= 0 && selectedIndex < currentInstructions.size() &&
                                                selectedIndex < instructionData.size()) {
                                            SInstruction selectedInstruction = currentInstructions.get(selectedIndex);
                                            if (selectedInstruction != null) {
                                                // Call the callback with the selected instruction
                                                callback.accept(selectedInstruction);
                                            }
                                        }
                                    } else {
                                        // No selection - clear the history chain
                                        callback.accept(null);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error in delayed selection listener: " + e.getMessage());
                                    // Don't rethrow - just log the error
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("Error in selection listener: " + e.getMessage());
                            // Don't rethrow - just log the error
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error setting up selection listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<SInstruction> currentInstructions = new java.util.ArrayList<>();

    private List<SInstruction> getCreationChain(SInstruction instruction) {
        // This method will be called by the callback, but the actual chain
        // will be provided by the Header controller which has access to expansion
        // results
        List<SInstruction> chain = new java.util.ArrayList<>();
        chain.add(instruction);
        return chain;
    }

    private String getCommandType(SInstruction instruction) {
        if (instruction instanceof IncreaseInstruction
                || instruction instanceof DecreaseInstruction
                || instruction instanceof NoOpInstruction
                || instruction instanceof JumpNotZeroInstruction) {
            return "B"; // Basic
        }
        return "S"; // Synthetic
    }

    private String getLabelText(Label label) {
        if (label == null) {
            return "";
        }
        if (label.isExit()) {
            return "EXIT";
        }
        return label.getLabel() != null ? label.getLabel() : "";
    }

    private String getInstructionText(SInstruction instruction) {
        return getInstructionText(instruction, null);
    }

    private String getInstructionText(SInstruction instruction, Map<String, String> functionUserStrings) {
        if (instruction instanceof IncreaseInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable() + " + 1";
        } else if (instruction instanceof DecreaseInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable() + " - 1";
        } else if (instruction instanceof NoOpInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable();
        } else if (instruction instanceof ZeroVariableInstruction) {
            return instruction.getVariable() + " <- 0";
        } else if (instruction instanceof AssignVariableInstruction a) {
            return instruction.getVariable() + " <- " + a.getSource();
        } else if (instruction instanceof AssignConstantInstruction c) {
            return instruction.getVariable() + " <- " + c.getConstant();
        } else if (instruction instanceof GotoLabelInstruction g) {
            return "GOTO " + g.getTarget();
        } else if (instruction instanceof JumpNotZeroInstruction j) {
            return "IF " + j.getVariable() + " != 0 GOTO " + j.getTarget();
        } else if (instruction instanceof JumpZeroInstruction j) {
            return "IF " + j.getVariable() + " == 0 GOTO " + j.getTarget();
        } else if (instruction instanceof JumpEqualConstantInstruction j) {
            return "IF " + j.getVariable() + " == " + j.getConstant() + " GOTO " + j.getTarget();
        } else if (instruction instanceof JumpEqualVariableInstruction j) {
            return "IF " + j.getVariable() + " == " + j.getOther() + " GOTO " + j.getTarget();
        } else if (instruction instanceof QuoteInstruction q) {
            String arguments = "";
            List<FunctionArgument> args = q.getFunctionArguments();
            if (args.size() > 0) {
                arguments += ",";
            }
            for (int i = 0; i < args.size(); i++) {
                arguments += formatFunctionArgument(args.get(i), functionUserStrings);
                if (i < args.size() - 1) { // Not the last element
                    arguments += ",";
                }
            }
            String functionName = getDisplayFunctionName(q.getFunctionName(), functionUserStrings);
            return q.getVariable() + " <- (" + functionName + arguments + ")";
        } else if (instruction instanceof JumpEqualFunctionInstruction jef) {
            String arguments = "";
            List<FunctionArgument> args = jef.getFunctionArguments();
            if (args.size() > 0) {
                arguments += ",";
            }
            for (int i = 0; i < args.size(); i++) {
                arguments += formatFunctionArgument(args.get(i), functionUserStrings);
                if (i < args.size() - 1) { // Not the last element
                    arguments += ",";
                }
            }
            String functionName = getDisplayFunctionName(jef.getFunctionName(), functionUserStrings);
            return "IF " + jef.getVariable() + " == (" + functionName + arguments + ") GOTO "
                    + jef.getTarget();
        }
        return instruction.getName(); // Use getName() method from SInstruction interface
    }

    private String getArchitectureForInstruction(SInstruction instruction) {
        // TODO: Implement architecture detection logic
        // For now, return a placeholder based on instruction type
        if (instruction instanceof IncreaseInstruction
                || instruction instanceof DecreaseInstruction
                || instruction instanceof NoOpInstruction
                || instruction instanceof JumpNotZeroInstruction
                || instruction instanceof ZeroVariableInstruction) {
            return "I"; // Basic instructions support Architecture I
        } else if (instruction instanceof AssignVariableInstruction
                || instruction instanceof AssignConstantInstruction
                || instruction instanceof GotoLabelInstruction) {
            return "II"; // Assignment and goto instructions support Architecture II
        } else if (instruction instanceof JumpZeroInstruction
                || instruction instanceof JumpEqualConstantInstruction
                || instruction instanceof JumpEqualVariableInstruction) {
            return "III"; // Jump instructions support Architecture III
        } else if (instruction instanceof QuoteInstruction
                || instruction instanceof JumpEqualFunctionInstruction) {
            return "IV"; // Function-related instructions support Architecture IV
        }
        return "?"; // Unknown architecture
    }

    private String getDisplayFunctionName(String functionName, Map<String, String> functionUserStrings) {
        if (functionUserStrings != null && functionUserStrings.containsKey(functionName)) {
            return functionUserStrings.get(functionName);
        }
        return functionName;
    }

    private String formatFunctionArgument(FunctionArgument arg, Map<String, String> functionUserStrings) {
        if (arg.isFunctionCall()) {
            FunctionCall call = arg.asFunctionCall();
            String functionName = getDisplayFunctionName(call.getFunctionName(), functionUserStrings);
            StringBuilder sb = new StringBuilder();
            sb.append("(").append(functionName);
            for (FunctionArgument nestedArg : call.getArguments()) {
                sb.append(",").append(formatFunctionArgument(nestedArg, functionUserStrings));
            }
            sb.append(")");
            return sb.toString();
        } else {
            // Simple variable argument
            return arg.toString();
        }
    }

    private void printFunctionBody(String functionName) {
        if (currentProgram == null) {

            return;
        }

        // Check if the program has functions (assuming it's SProgramImpl)
        if (currentProgram instanceof SProgramImpl) {
            SProgramImpl programImpl = (SProgramImpl) currentProgram;
            var functions = programImpl.getFunctions();

            if (functions.containsKey(functionName)) {
                var functionInstructions = functions.get(functionName);
                for (SInstruction inst : functionInstructions) {
                    String instructionText = getInstructionText(inst);

                }
            } else {

            }
        } else {

        }
    }

    public static class InstructionRow {
        private final javafx.beans.property.IntegerProperty rowNumber;
        private final javafx.beans.property.StringProperty commandType;
        private final javafx.beans.property.StringProperty label;
        private final javafx.beans.property.StringProperty instructionType;
        private final javafx.beans.property.IntegerProperty cycles;
        private final javafx.beans.property.StringProperty variable;
        private final javafx.beans.property.StringProperty architecture;
        private boolean highlighted = false;

        public InstructionRow(int rowNumber, String commandType, String label,
                String instructionType, int cycles, String variable, String architecture) {
            this.rowNumber = new javafx.beans.property.SimpleIntegerProperty(rowNumber);
            this.commandType = new javafx.beans.property.SimpleStringProperty(commandType);
            this.label = new javafx.beans.property.SimpleStringProperty(label);
            this.instructionType = new javafx.beans.property.SimpleStringProperty(instructionType);
            this.cycles = new javafx.beans.property.SimpleIntegerProperty(cycles);
            this.variable = new javafx.beans.property.SimpleStringProperty(variable);
            this.architecture = new javafx.beans.property.SimpleStringProperty(architecture);
        }

        // Property getters
        public javafx.beans.property.IntegerProperty rowNumberProperty() {
            return rowNumber;
        }

        public javafx.beans.property.StringProperty commandTypeProperty() {
            return commandType;
        }

        public javafx.beans.property.StringProperty labelProperty() {
            return label;
        }

        public javafx.beans.property.StringProperty instructionTypeProperty() {
            return instructionType;
        }

        public javafx.beans.property.IntegerProperty cyclesProperty() {
            return cycles;
        }

        public javafx.beans.property.StringProperty variableProperty() {
            return variable;
        }

        public javafx.beans.property.StringProperty architectureProperty() {
            return architecture;
        }

        // Value getters
        public int getRowNumber() {
            return rowNumber.get();
        }

        public String getCommandType() {
            return commandType.get();
        }

        public String getLabel() {
            return label.get();
        }

        public String getInstructionType() {
            return instructionType.get();
        }

        public int getCycles() {
            return cycles.get();
        }

        public String getVariable() {
            return variable.get();
        }

        public String getArchitecture() {
            return architecture.get();
        }

        // Highlighting methods
        public void setHighlighted(boolean highlighted) {
            this.highlighted = highlighted;
        }

        public boolean isHighlighted() {
            return highlighted;
        }
    }

    // Method to highlight rows containing a specific label or variable
    public void highlightRowsContaining(String term) {
        currentHighlightTerm = term;

        // First, clear all previous highlights
        for (InstructionRow row : instructionData) {
            row.setHighlighted(false);
        }

        // Then, find and highlight matching rows
        for (InstructionRow row : instructionData) {
            if (rowContainsTerm(row, term)) {
                row.setHighlighted(true);
            }
        }

        // Finally, refresh the table once to apply all changes
        instructionTableView.refresh();
    }

    // Method to clear highlighting
    public void clearHighlighting() {
        currentHighlightTerm = null;
        instructionTableView.refresh();
    }

    // Method to highlight the currently executing instruction
    public void highlightCurrentInstruction(int instructionIndex) {
        currentExecutingInstructionIndex = instructionIndex;
        instructionTableView.refresh();

        // TODO: Re-enable animations when animation classes are available
        // Trigger row pulse animation for the executed instruction
        // if (Animations.isEnabled() && instructionIndex >= 0 && instructionIndex <
        // instructionData.size()) {
        // // Use a more reliable method to get the table row
        // triggerRowPulseAnimation(instructionIndex);
        // }
    }

    // Method to clear current instruction highlighting
    public void clearCurrentInstructionHighlighting() {
        currentExecutingInstructionIndex = -1;
        rowCache.clear(); // Clear the row cache when refreshing
        instructionTableView.refresh();
    }

    /**
     * Trigger row pulse animation for a specific instruction index.
     * TODO: Re-enable when animation classes are available
     */
    private void triggerRowPulseAnimation(int instructionIndex) {
        // TODO: Re-enable animations
        return;
        /*
         * if (!Animations.isEnabled()) {
         * return;
         * }
         * 
         * // First try to get the row from our cache
         * TableRow<InstructionRow> tableRow = rowCache.get(instructionIndex);
         * 
         * if (tableRow != null) {
         * // We have a cached reference to the row, animate it directly
         * RowPulseAnimation.pulseRow(tableRow);
         * return;
         * }
         * 
         * // If not in cache, try the old lookup methods as fallback
         * // Method 1: Try CSS selector approach with specific index
         * tableRow = (TableRow<InstructionRow>) instructionTableView
         * .lookup(".table-row-cell:nth-child(" + (instructionIndex + 1) + ")");
         * 
         * // Method 2: Try generic table row lookup
         * if (tableRow == null) {
         * tableRow = (TableRow<InstructionRow>) instructionTableView
         * .lookup(".table-row-cell");
         * }
         * 
         * // Method 3: Try to find any row in the table
         * if (tableRow == null) {
         * for (javafx.scene.Node node :
         * instructionTableView.lookupAll(".table-row-cell")) {
         * if (node instanceof TableRow) {
         * tableRow = (TableRow<InstructionRow>) node;
         * break;
         * }
         * }
         * }
         * 
         * // If we found a row, animate it
         * if (tableRow != null) {
         * RowPulseAnimation.pulseRow(tableRow);
         * } else {
         * // Fallback: try to animate the table itself as a last resort
         * RowPulseAnimation.pulseRow(instructionTableView);
         * }
         */
    }

    // Set up row highlighting using row factory
    private void setupRowHighlighting() {
        instructionTableView.setRowFactory(tv -> {
            TableRow<InstructionRow> row = new TableRow<InstructionRow>() {
                @Override
                protected void updateItem(InstructionRow item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setStyle("");
                        // Remove from cache when row becomes empty
                        int rowIndex = getIndex();
                        if (rowIndex >= 1) {
                            int instructionIndex = rowIndex - 1;
                            rowCache.remove(instructionIndex);
                        }
                    } else {
                        int rowIndex = getIndex();

                        // Store this row in cache for animation access
                        // Convert from 1-based row index to 0-based instruction index
                        if (rowIndex >= 1) {
                            int instructionIndex = rowIndex - 1;
                            rowCache.put(instructionIndex, this);
                        }

                        String style = "";

                        // Check if this is the currently executing instruction
                        if (rowIndex == currentExecutingInstructionIndex) {
                            style = "-fx-background-color: #4CAF50; -fx-font-weight: bold; -fx-text-fill: white;"; // Green
                            // highlight
                            // for
                            // current
                            // instruction
                        }
                        // Check if this row should be highlighted for search
                        else if (currentHighlightTerm != null && rowContainsTerm(item, currentHighlightTerm)) {
                            style = "-fx-background-color: #FFE135; -fx-font-weight: bold;"; // Yellow highlight for
                            // search
                        }

                        setStyle(style);
                    }
                }
            };
            return row;
        });
    }

    // Helper method to check if a row contains the search term
    private boolean rowContainsTerm(InstructionRow row, String term) {
        if (term == null || term.isEmpty()) {
            return false;
        }

        // Check label field with exact matching (labels should match exactly)
        if (row.getLabel() != null && row.getLabel().equals(term)) {
            return true;
        }

        // Check variable field with exact matching (variables should match exactly)
        if (row.getVariable() != null && row.getVariable().equals(term)) {
            return true;
        }

        // Check instruction text field with word boundary matching
        if (row.getInstructionType() != null) {
            String instructionText = row.getInstructionType();
            // Use word boundaries to match complete words/tokens, not substrings
            // This prevents L1 from matching L10, L11, etc.
            if (instructionText.contains(term)) {
                // Additional check: ensure it's not a substring within a larger word
                // Look for the term surrounded by word boundaries or at start/end
                String pattern = "\\b" + java.util.regex.Pattern.quote(term) + "\\b";
                if (java.util.regex.Pattern.compile(pattern).matcher(instructionText).find()) {
                    return true;
                }
            }
        }

        // Check command type field with exact matching
        if (row.getCommandType() != null && row.getCommandType().equals(term)) {
            return true;
        }

        return false;
    }
}
