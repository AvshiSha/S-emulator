package com.semulator.client.ui.components.InstructionTable;

import com.semulator.client.AppContext;
import com.semulator.client.service.ApiClient;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Instruction Table component controller - adapted for HTTP communication
 * Displays program/function instructions in a table format
 */
public class InstructionTableController implements Initializable {

    @FXML
    private TableView<InstructionRow> instructionTableView;
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

    // Architecture Summary Components
    @FXML
    private HBox architectureSummaryContainer;
    @FXML
    private Label archISummary;
    @FXML
    private Label archIISummary;
    @FXML
    private Label archIIISummary;
    @FXML
    private Label archIVSummary;

    private ObservableList<InstructionRow> instructionData = FXCollections.observableArrayList();
    private ApiClient apiClient;
    private String currentHighlightTerm = null;
    private int currentExecutingInstructionIndex = -1;

    // Architecture command counts
    private int totalCommands = 0;
    private int archICommands = 0;
    private int archIICommands = 0;
    private int archIIICommands = 0;
    private int archIVCommands = 0;

    // Selected architecture for highlighting
    private String selectedArchitecture = "I";

    // Callback for architecture compatibility changes
    private java.util.function.Consumer<Boolean> architectureCompatibilityCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.apiClient = AppContext.getInstance().getApiClient();

        setupTable();
        setupColumns();
        initializeArchitectureSummary();
    }

    private void setupTable() {
        instructionTableView.setItems(instructionData);

        // Enable row highlighting
        instructionTableView.setRowFactory(tv -> {
            TableRow<InstructionRow> row = new TableRow<InstructionRow>() {
                @Override
                protected void updateItem(InstructionRow item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        // Apply highlighting based on row state
                        if (item.isHighlighted()) {
                            setStyle("-fx-background-color: #FFE4B5; -fx-font-weight: bold;");
                        } else if (getIndex() == currentExecutingInstructionIndex) {
                            setStyle("-fx-background-color: #90EE90; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            };
            return row;
        });
    }

    private void setupColumns() {
        // Row Number
        rowNumberColumn.setCellValueFactory(cellData -> cellData.getValue().rowNumberProperty().asObject());

        // Command Type (B/S)
        commandTypeColumn.setCellValueFactory(cellData -> cellData.getValue().commandTypeProperty());

        // Label
        labelColumn.setCellValueFactory(cellData -> cellData.getValue().labelProperty());

        // Instruction Type
        instructionTypeColumn.setCellValueFactory(cellData -> cellData.getValue().instructionTypeProperty());

        // Cycles
        cyclesColumn.setCellValueFactory(cellData -> cellData.getValue().cyclesProperty().asObject());

        // Architecture
        architectureColumn.setCellValueFactory(cellData -> cellData.getValue().architectureProperty());
    }

    private void initializeArchitectureSummary() {
        // Initialize summary labels with default values
        updateArchitectureSummary();
    }

    private void updateArchitectureSummary() {
        Platform.runLater(() -> {
            // Update text content
            archISummary.setText("I: " + archICommands + " commands");
            archIISummary.setText("II: " + archIICommands + " commands");
            archIIISummary.setText("III: " + archIIICommands + " commands");
            archIVSummary.setText("IV: " + archIVCommands + " commands");

            // Update styling based on support
            updateArchitectureStyling(archISummary, archICommands, totalCommands, "I");
            updateArchitectureStyling(archIISummary, archIICommands, totalCommands, "II");
            updateArchitectureStyling(archIIISummary, archIIICommands, totalCommands, "III");
            updateArchitectureStyling(archIVSummary, archIVCommands, totalCommands, "IV");
        });
    }

    private void updateArchitectureStyling(Label label, int supportedCommands, int totalCommands,
            String architectureCode) {

        // Convert architecture codes to numbers for comparison
        int selectedArchNum = getArchitectureNumber(selectedArchitecture);
        int currentArchNum = getArchitectureNumber(architectureCode);

        // Your pseudo code logic:
        // IF the architecture selector is not equal to the architectureCode, and the
        // architectureCode is bigger than architecture selector, and the number of
        // commands of the architectureCode is bigger than zero:
        // paint the architectureCode in red in the summary line.
        // else if architecture selector is equal to the architectureCode:
        // paint the architectureCode in white in the summary line.
        // else
        // paint the architectureCode in white in the summary line.

        if (!selectedArchitecture.equals(architectureCode) &&
                currentArchNum > selectedArchNum &&
                supportedCommands > 0) {
            // Red highlighting - architecture is higher than selected and has commands
            label.setStyle(
                    "-fx-font-size: 11px; -fx-text-fill: #FFFFFF; -fx-padding: 2px 8px; -fx-background-color: #FF6B6B; -fx-border-color: #DC143C; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;");
        } else if (selectedArchitecture.equals(architectureCode)) {
            // White highlighting - selected architecture
            label.setStyle(
                    "-fx-font-size: 11px; -fx-text-fill: #000000; -fx-padding: 2px 8px; -fx-background-color: #FFFFFF; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;");
        } else {
            // White highlighting - default case
            label.setStyle(
                    "-fx-font-size: 11px; -fx-text-fill: #000000; -fx-padding: 2px 8px; -fx-background-color: #FFFFFF; -fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;");
        }
    }

    private int getArchitectureNumber(String architecture) {
        switch (architecture) {
            case "I":
                return 1;
            case "II":
                return 2;
            case "III":
                return 3;
            case "IV":
                return 4;
            default:
                return 0;
        }
    }

    // Public methods for external communication
    public void updateProgramFunction(String programFunctionName) {
        if (programFunctionName == null || programFunctionName.isEmpty()) {
            clearTable();
            return;
        }

        // Parse program/function name
        String type = programFunctionName.startsWith("PROGRAM:") ? "PROGRAM" : "FUNCTION";
        String name = programFunctionName.substring(type.length() + 2); // Remove "TYPE: " prefix

        // TODO: Load instructions from server
        // For now, simulate with sample data
        loadInstructionsFromServer(type, name);
    }

    public void updateDegree(int degree) {
        // TODO: Implement degree-based instruction expansion
        // This would call the server to get expanded instructions for the given degree
    }

    public void highlightLabelVariable(String labelVariable) {
        clearHighlighting();
        currentHighlightTerm = labelVariable;

        // Highlight matching rows
        for (InstructionRow row : instructionData) {
            if (matchesLabelVariable(row, labelVariable)) {
                row.setHighlighted(true);
            }
        }

        // Refresh table display
        instructionTableView.refresh();
    }

    public void setExecutingInstruction(int index) {
        currentExecutingInstructionIndex = index;
        instructionTableView.refresh();
    }

    public void clearHighlighting() {
        currentHighlightTerm = null;
        for (InstructionRow row : instructionData) {
            row.setHighlighted(false);
        }
        instructionTableView.refresh();
    }

    public void clearTable() {
        instructionData.clear();
        currentExecutingInstructionIndex = -1;
        clearHighlighting();

        // Reset architecture counts
        totalCommands = 0;
        archICommands = 0;
        archIICommands = 0;
        archIIICommands = 0;
        archIVCommands = 0;
        updateArchitectureSummary();
    }

    public void initializeWithHttp() {
        // Initialize API client
        this.apiClient = AppContext.getInstance().getApiClient();
    }

    public TableView<InstructionRow> getInstructionTableView() {
        return instructionTableView; // Return the table view for external access
    }

    public void setInstructionData(ObservableList<InstructionRow> data) {
        instructionData.clear();
        instructionData.addAll(data);
        instructionTableView.setItems(instructionData);
        instructionTableView.refresh();

        // Calculate architecture support after setting new data
        calculateArchitectureSupport();

    }

    public void highlightCurrentInstruction(int instructionIndex) {
        currentExecutingInstructionIndex = instructionIndex;
        instructionTableView.refresh();
    }

    public void highlightRowsContaining(String term) {
        clearHighlighting();
        currentHighlightTerm = term;

        // Highlight matching rows
        for (InstructionRow row : instructionData) {
            if (matchesLabelVariable(row, term)) {
                row.setHighlighted(true);
            }
        }

        // Refresh table display
        instructionTableView.refresh();
    }

    /**
     * Update architecture summary with data from server (e.g., from
     * RunPrepareResponse)
     */
    public void updateArchitectureSummaryFromServer(java.util.Map<String, Integer> instructionCountsByArch) {
        if (instructionCountsByArch == null) {
            return;
        }

        // Update counts from server data
        archICommands = instructionCountsByArch.getOrDefault("I", 0);
        archIICommands = instructionCountsByArch.getOrDefault("II", 0);
        archIIICommands = instructionCountsByArch.getOrDefault("III", 0);
        archIVCommands = instructionCountsByArch.getOrDefault("IV", 0);

        // Use the highest count as total (since higher architectures support all lower
        // architecture commands)
        totalCommands = Math.max(Math.max(archICommands, archIICommands), Math.max(archIIICommands, archIVCommands));

        updateArchitectureSummary();
    }

    /**
     * Update the selected architecture and refresh styling
     */
    public void updateSelectedArchitecture(String architecture) {
        this.selectedArchitecture = architecture;
        updateArchitectureSummary();
        // Notify compatibility change
        notifyCompatibilityChange();
    }

    /**
     * Check if the selected architecture supports all instructions in the program
     * 
     * @return true if all instructions are compatible with the selected
     *         architecture
     */
    public boolean isArchitectureCompatible() {
        int selectedArchNum = getArchitectureNumber(selectedArchitecture);

        // Check if there are any instructions requiring a higher architecture
        switch (selectedArchNum) {
            case 1: // Architecture I
                return (archIICommands == 0 && archIIICommands == 0 && archIVCommands == 0);
            case 2: // Architecture II
                return (archIIICommands == 0 && archIVCommands == 0);
            case 3: // Architecture III
                return (archIVCommands == 0);
            case 4: // Architecture IV
                return true; // Architecture IV supports everything
            default:
                return false;
        }
    }

    /**
     * Get list of unsupported instruction architectures for the current selection
     * 
     * @return List of architecture names that have instructions not supported by
     *         selected architecture
     */
    public java.util.List<String> getUnsupportedArchitectures() {
        java.util.List<String> unsupported = new java.util.ArrayList<>();
        int selectedArchNum = getArchitectureNumber(selectedArchitecture);

        if (selectedArchNum < 2 && archIICommands > 0) {
            unsupported.add("Architecture II");
        }
        if (selectedArchNum < 3 && archIIICommands > 0) {
            unsupported.add("Architecture III");
        }
        if (selectedArchNum < 4 && archIVCommands > 0) {
            unsupported.add("Architecture IV");
        }

        return unsupported;
    }

    /**
     * Set callback for architecture compatibility status changes
     */
    public void setArchitectureCompatibilityCallback(java.util.function.Consumer<Boolean> callback) {
        this.architectureCompatibilityCallback = callback;
    }

    /**
     * Notify listeners about compatibility change
     */
    private void notifyCompatibilityChange() {
        if (architectureCompatibilityCallback != null) {
            boolean compatible = isArchitectureCompatible();
            architectureCompatibilityCallback.accept(compatible);
        }
    }

    private void loadInstructionsFromServer(String type, String name) {
        // TODO: Implement actual server call
        // This would call something like:
        // apiClient.get("/programs/" + name + "/instructions",
        // InstructionsResponse.class)
        // or
        // apiClient.get("/functions/" + name + "/instructions",
        // InstructionsResponse.class)

        // For now, simulate with sample data
        simulateInstructions(type, name);
    }

    private void simulateInstructions(String type, String name) {
        Platform.runLater(() -> {
            instructionData.clear();

            // // Add sample instructions that demonstrate different architecture support
            // instructionData.add(new InstructionRow(1, "B", "", "INCREASE", 1, "x1"));
            // instructionData.add(new InstructionRow(2, "B", "", "DECREASE", 1, "x2"));
            // instructionData.add(new InstructionRow(3, "B", "LOOP", "NEUTRAL", 0, ""));
            // instructionData.add(new InstructionRow(4, "B", "", "JUMP_NOT_ZERO", 2,
            // "LOOP"));
            // instructionData.add(new InstructionRow(5, "S", "", "ZERO_VARIABLE", 1,
            // "x3"));
            // instructionData.add(new InstructionRow(6, "S", "", "CONSTANT_ASSIGNMENT", 2,
            // "x4"));
            // instructionData.add(new InstructionRow(7, "S", "", "GOTO_LABEL", 1, "END"));
            // instructionData.add(new InstructionRow(8, "S", "", "ASSIGNMENT", 4, "x5"));
            // instructionData.add(new InstructionRow(9, "S", "", "JUMP_ZERO", 2, "x6"));
            // instructionData.add(new InstructionRow(10, "S", "", "QUOTE", 5, "x7"));

            // Calculate architecture support after loading instructions
            calculateArchitectureSupport();

        });
    }

    private boolean matchesLabelVariable(InstructionRow row, String term) {
        // Check if the term matches any part of the instruction
        String instruction = row.getInstructionType().toLowerCase();
        String label = row.getLabel().toLowerCase();
        String variable = row.getVariable().toLowerCase();

        return instruction.contains(term.toLowerCase()) ||
                label.contains(term.toLowerCase()) ||
                variable.contains(term.toLowerCase());
    }

    private void calculateArchitectureSupport() {
        // Reset counts
        totalCommands = instructionData.size();
        archICommands = 0;
        archIICommands = 0;
        archIIICommands = 0;
        archIVCommands = 0;

        // Count commands supported by each architecture based on the architecture field
        for (InstructionRow row : instructionData) {
            String arch = row.getArchitecture();

            // Count how many commands each architecture can support
            // Architecture hierarchy: I < II < III < IV
            // Higher architectures support all lower architecture commands

            if ("I".equals(arch)) {
                archICommands++;
            } else if ("II".equals(arch)) {
                archIICommands++;
            } else if ("III".equals(arch)) {
                archIIICommands++;
            } else if ("IV".equals(arch)) {
                archIVCommands++;
            }
        }

        updateArchitectureSummary();
        // Notify compatibility change when instruction data changes
        notifyCompatibilityChange();
    }

    private boolean isSupportedByArchI(String instructionType) {
        return instructionType.contains("NEUTRAL") ||
                instructionType.contains("INCREASE") ||
                instructionType.contains("DECREASE") ||
                instructionType.contains("JUMP_NOT_ZERO");
    }

    private boolean isSupportedByArchII(String instructionType) {
        return isSupportedByArchI(instructionType) ||
                instructionType.contains("ZERO") ||
                instructionType.contains("CONSTANT_ASSIGNMENT") ||
                instructionType.contains("GOTO_LABEL");
    }

    private boolean isSupportedByArchIII(String instructionType) {
        return isSupportedByArchII(instructionType) ||
                instructionType.contains("ASSIGNMENT") ||
                instructionType.contains("JUMP_ZERO") ||
                instructionType.contains("JUMP_EQUAL_CONSTANT") ||
                instructionType.contains("JUMP_EQUAL_VARIABLE");
    }

    private boolean isSupportedByArchIV(String instructionType) {
        return isSupportedByArchIII(instructionType) ||
                instructionType.contains("QUOTE") ||
                instructionType.contains("JUMP_EQUAL_FUNCTION");
    }

    // Inner class for instruction row data
    public static class InstructionRow {
        private final IntegerProperty rowNumber;
        private final StringProperty commandType;
        private final StringProperty label;
        private final StringProperty instructionType;
        private final IntegerProperty cycles;
        private final StringProperty variable;
        private final StringProperty architecture;
        private boolean highlighted = false;

        public InstructionRow(int rowNumber, String commandType, String label,
                String instructionType, int cycles, String variable, String architecture) {
            this.rowNumber = new SimpleIntegerProperty(rowNumber);
            this.commandType = new SimpleStringProperty(commandType);
            this.label = new SimpleStringProperty(label);
            this.instructionType = new SimpleStringProperty(instructionType);
            this.cycles = new SimpleIntegerProperty(cycles);
            this.variable = new SimpleStringProperty(variable);
            this.architecture = new SimpleStringProperty(architecture != null ? architecture : "I");
        }

        // Property getters
        public IntegerProperty rowNumberProperty() {
            return rowNumber;
        }

        public StringProperty commandTypeProperty() {
            return commandType;
        }

        public StringProperty labelProperty() {
            return label;
        }

        public StringProperty instructionTypeProperty() {
            return instructionType;
        }

        public IntegerProperty cyclesProperty() {
            return cycles;
        }

        public StringProperty variableProperty() {
            return variable;
        }

        public StringProperty architectureProperty() {
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

        // Highlighting
        public boolean isHighlighted() {
            return highlighted;
        }

        public void setHighlighted(boolean highlighted) {
            this.highlighted = highlighted;
        }
    }
}
