package ui.components.InstructionTable;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import semulator.instructions.*;
import semulator.label.Label;
import semulator.label.FixedLabel;
import semulator.program.SProgram;

import java.util.List;
import java.util.function.Consumer;

public class InstructionTable {
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

    private ObservableList<InstructionRow> instructionData = FXCollections.observableArrayList();

    // For highlighting functionality
    private String currentHighlightTerm = null;
    private int currentExecutingInstructionIndex = -1; // -1 means no instruction is executing

    @FXML
    private void initialize() {
        // Set up the table columns
        rowNumberColumn.setCellValueFactory(cellData -> cellData.getValue().rowNumberProperty().asObject());
        commandTypeColumn.setCellValueFactory(cellData -> cellData.getValue().commandTypeProperty());
        labelColumn.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        instructionTypeColumn.setCellValueFactory(cellData -> cellData.getValue().instructionTypeProperty());
        cyclesColumn.setCellValueFactory(cellData -> cellData.getValue().cyclesProperty().asObject());

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

        // Set sortable to false for all columns to maintain order
        rowNumberColumn.setSortable(false);
        commandTypeColumn.setSortable(false);
        labelColumn.setSortable(false);
        cyclesColumn.setSortable(false);

        // Set up row highlighting
        setupRowHighlighting();
    }

    public void displayProgram(SProgram program) {
        instructionData.clear();
        currentInstructions.clear();
        currentHighlightTerm = null; // Clear any existing highlighting
        currentExecutingInstructionIndex = -1; // Clear current instruction highlighting

        if (program == null || program.getInstructions() == null) {
            return;
        }

        List<SInstruction> instructions = program.getInstructions();
        currentInstructions.addAll(instructions); // Store for selection handling

        for (int i = 0; i < instructions.size(); i++) {
            SInstruction instruction = instructions.get(i);
            InstructionRow row = new InstructionRow(
                    i + 1, // Row number (1-based)
                    getCommandType(instruction), // B or S
                    getLabelText(instruction.getLabel()), // Label text
                    getInstructionText(instruction), // Instruction description
                    instruction.cycles() // Cycles
            );
            instructionData.add(row);
        }
    }

    public void clearTable() {
        instructionData.clear();
    }

    public void setHistoryChainCallback(Consumer<SInstruction> callback) {
        // Add selection listener to the table
        instructionTableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        // Get the selected instruction index
                        int selectedIndex = instructionTableView.getSelectionModel().getSelectedIndex();
                        if (selectedIndex >= 0 && selectedIndex < currentInstructions.size()) {
                            SInstruction selectedInstruction = currentInstructions.get(selectedIndex);
                            // Call the callback with the selected instruction
                            callback.accept(selectedInstruction);
                        }
                    } else {
                        // No selection - clear the history chain
                        callback.accept(null);
                    }
                });
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
        }
        return instruction.getName();
    }

    // Data model class for table rows
    public static class InstructionRow {
        private final javafx.beans.property.IntegerProperty rowNumber;
        private final javafx.beans.property.StringProperty commandType;
        private final javafx.beans.property.StringProperty label;
        private final javafx.beans.property.StringProperty instructionType;
        private final javafx.beans.property.IntegerProperty cycles;

        public InstructionRow(int rowNumber, String commandType, String label, String instructionType, int cycles) {
            this.rowNumber = new javafx.beans.property.SimpleIntegerProperty(rowNumber);
            this.commandType = new javafx.beans.property.SimpleStringProperty(commandType);
            this.label = new javafx.beans.property.SimpleStringProperty(label);
            this.instructionType = new javafx.beans.property.SimpleStringProperty(instructionType);
            this.cycles = new javafx.beans.property.SimpleIntegerProperty(cycles);
        }

        // Property getters for JavaFX binding
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

        // Regular getters
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
    }

    // Method to highlight rows containing a specific label or variable
    public void highlightRowsContaining(String term) {
        currentHighlightTerm = term;
        // Refresh the table to apply highlighting
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
    }

    // Method to clear current instruction highlighting
    public void clearCurrentInstructionHighlighting() {
        currentExecutingInstructionIndex = -1;
        instructionTableView.refresh();
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
                    } else {
                        int rowIndex = getIndex();
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

        // Check all text fields in the row
        return (row.getLabel() != null && row.getLabel().contains(term)) ||
                (row.getInstructionType() != null && row.getInstructionType().contains(term)) ||
                (row.getCommandType() != null && row.getCommandType().contains(term));
    }
}
