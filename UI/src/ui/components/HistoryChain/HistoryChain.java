package ui.components.HistoryChain;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import semulator.instructions.*;
import semulator.label.Label;
import semulator.program.SProgram;
import ui.components.InstructionTable.InstructionTable.InstructionRow;

import java.util.List;

public class HistoryChain {
    @FXML
    private TableView<InstructionRow> historyTableView;

    @FXML
    private TableColumn<InstructionRow, Integer> historyRowNumberColumn;

    @FXML
    private TableColumn<InstructionRow, String> historyCommandTypeColumn;

    @FXML
    private TableColumn<InstructionRow, String> historyLabelColumn;

    @FXML
    private TableColumn<InstructionRow, String> historyInstructionTypeColumn;

    @FXML
    private TableColumn<InstructionRow, Integer> historyCyclesColumn;

    private ObservableList<InstructionRow> historyData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Set up the table columns
        historyRowNumberColumn.setCellValueFactory(cellData -> cellData.getValue().rowNumberProperty().asObject());
        historyCommandTypeColumn.setCellValueFactory(cellData -> cellData.getValue().commandTypeProperty());
        historyLabelColumn.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        historyInstructionTypeColumn.setCellValueFactory(cellData -> cellData.getValue().instructionTypeProperty());
        historyCyclesColumn.setCellValueFactory(cellData -> cellData.getValue().cyclesProperty().asObject());

        // Set the data source for the table
        historyTableView.setItems(historyData);
    }

    public void displayHistoryChain(List<SInstruction> chain) {
        historyData.clear();

        if (chain == null || chain.isEmpty()) {
            return;
        }

        // Display chain from most recent (top) to oldest (bottom)
        for (int i = 0; i < chain.size(); i++) {
            SInstruction instruction = chain.get(i);
            InstructionRow row = new InstructionRow(
                    i + 1, // Row number in the chain
                    getCommandType(instruction), // B or S
                    getLabelText(instruction.getLabel()), // Label text
                    getInstructionText(instruction), // Instruction description
                    instruction.cycles() // Cycles
            );
            historyData.add(row);
        }
    }

    public void clearHistory() {
        historyData.clear();
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
}
