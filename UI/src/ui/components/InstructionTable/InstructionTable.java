package ui.components.InstructionTable;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import semulator.instructions.*;
import semulator.label.Label;
import semulator.label.FixedLabel;
import semulator.program.SProgram;

import java.util.List;

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
    }

    public void displayProgram(SProgram program) {
        instructionData.clear();
        currentInstructions.clear();

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

    public void setHistoryChainCallback(java.util.function.Consumer<List<SInstruction>> callback) {
        // Add selection listener to the table
        instructionTableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        // Get the selected instruction index
                        int selectedIndex = instructionTableView.getSelectionModel().getSelectedIndex();
                        if (selectedIndex >= 0 && selectedIndex < currentInstructions.size()) {
                            SInstruction selectedInstruction = currentInstructions.get(selectedIndex);
                            // Get the creation chain for this instruction
                            List<SInstruction> chain = getCreationChain(selectedInstruction);
                            // Call the callback with the chain
                            callback.accept(chain);
                        }
                    }
                });
    }

    private List<SInstruction> currentInstructions = new java.util.ArrayList<>();

    private List<SInstruction> getCreationChain(SInstruction instruction) {
        // For demonstration purposes, create a mock creation chain
        // In a real implementation, this would use the ExpansionResult parent map
        // to trace back through the creation chain from the engine

        List<SInstruction> chain = new java.util.ArrayList<>();

        // Add the selected instruction (most recent)
        chain.add(instruction);

        // For synthetic instructions, add a mock parent instruction
        if (getCommandType(instruction).equals("S")) {
            // Create a mock parent instruction (this would come from the ExpansionResult
            // parent map)
            // For demonstration, we'll create a simple parent instruction
            try {
                // This is just for demonstration - in reality, the parent would come from the
                // expansion result
                if (instruction instanceof ZeroVariableInstruction) {
                    // Mock parent: a synthetic instruction that created this zero instruction
                    chain.add(new semulator.instructions.AssignConstantInstruction(
                            instruction.getVariable(), 0L, instruction.getLabel()));
                } else if (instruction instanceof AssignVariableInstruction) {
                    // Mock parent: a basic instruction that created this assignment
                    chain.add(new semulator.instructions.IncreaseInstruction(
                            instruction.getVariable(), instruction.getLabel()));
                } else {
                    // For other synthetic instructions, add a mock basic instruction
                    chain.add(new semulator.instructions.DecreaseInstruction(
                            instruction.getVariable(), instruction.getLabel()));
                }
            } catch (Exception e) {
                // If we can't create mock instructions, just return the original
            }
        }

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
}
