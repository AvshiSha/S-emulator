package ui.components.HistoryChain;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import semulator.instructions.*;
import semulator.label.Label;
import semulator.program.SProgram;
import semulator.variable.Variable;
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

        // Make the instruction type column resizable to handle different screen sizes
        historyInstructionTypeColumn.setResizable(true);
        historyInstructionTypeColumn.setSortable(false);

        // Make other columns resizable but with constraints
        historyRowNumberColumn.setResizable(true);
        historyCommandTypeColumn.setResizable(true);
        historyLabelColumn.setResizable(true);
        historyCyclesColumn.setResizable(true);

        // Set sortable to false for all columns to maintain order
        historyRowNumberColumn.setSortable(false);
        historyCommandTypeColumn.setSortable(false);
        historyLabelColumn.setSortable(false);
        historyCyclesColumn.setSortable(false);
    }

    public void displayHistoryChain(List<SInstruction> chain) {
        historyData.clear();

        if (chain == null || chain.isEmpty()) {
            return;
        }

        // Display chain from most recent (top) to oldest (bottom)
        for (int i = 0; i < chain.size(); i++) {
            SInstruction instruction = chain.get(i);
            String variable = "";
            try {
                variable = instruction.getVariable().toString();
            } catch (Exception e) {
                // Some instructions don't have getVariable() method
                variable = "";
            }
            InstructionRow row = new InstructionRow(
                    i + 1, // Row number in the chain
                    getCommandType(instruction), // B or S
                    getLabelText(instruction.getLabel()), // Label text
                    getInstructionText(instruction), // Instruction description
                    instruction.cycles(), // Cycles
                    variable);
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
        } else if (instruction instanceof QuoteInstruction q) {
            String arguments = "";
            List<FunctionArgument> args = q.getFunctionArguments();
            if (args.size() > 0) {
                arguments += ",";
            }
            for (int i = 0; i < args.size(); i++) {
                arguments += args.get(i).toString();
                if (i < args.size() - 1) { // Not the last element
                    arguments += ",";
                }
            }
            return q.getVariable() + " <- (" + q.getFunctionName() + arguments + ")";
        } else if (instruction instanceof JumpEqualFunctionInstruction jef) {
            String arguments = "";
            List<FunctionArgument> args = jef.getFunctionArguments();
            if (args.size() > 0) {
                arguments += ",";
            }
            for (int i = 0; i < args.size(); i++) {
                arguments += args.get(i).toString();
                if (i < args.size() - 1) { // Not the last element
                    arguments += ",";
                }
            }
            return "IF " + jef.getVariable() + " == (" + jef.getFunctionName() + arguments + ") GOTO "
                    + jef.getTarget();
        }
        return instruction.getName();
    }
}
