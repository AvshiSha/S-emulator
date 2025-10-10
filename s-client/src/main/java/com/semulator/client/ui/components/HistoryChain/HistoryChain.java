package com.semulator.client.ui.components.HistoryChain;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.semulator.engine.model.SInstruction;
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
import com.semulator.client.ui.components.InstructionTable.InstructionTableController.InstructionRow;
import com.semulator.client.model.ApiModels;

import java.util.List;
import java.util.Map;

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
        displayHistoryChain(chain, null);
    }

    /**
     * Display history chain from server response (preferred method for
     * client-server architecture)
     */
    public void displayHistoryChainFromServer(List<ApiModels.HistoryChainItem> chainItems) {
        historyData.clear();

        if (chainItems == null || chainItems.isEmpty()) {
            return;
        }

        // Display chain from most recent (top) to oldest (bottom)
        for (int i = 0; i < chainItems.size(); i++) {
            ApiModels.HistoryChainItem item = chainItems.get(i);

            InstructionRow row = new InstructionRow(
                    i + 1, // Row number in the chain (sequential numbering)
                    item.commandType(), // B or S from server
                    item.label(), // Label from server
                    item.instructionText(), // Formatted instruction text from server
                    item.cycles(), // Cycles from server
                    item.variable()); // Variable from server
            historyData.add(row);
        }
    }

    public void displayHistoryChain(List<SInstruction> chain, Map<String, String> functionUserStrings) {
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
                    i + 1, // Row number in the chain (sequential numbering)
                    getCommandType(instruction), // B or S
                    getLabelText(instruction.getLabel()), // Label text
                    getInstructionText(instruction, functionUserStrings), // Instruction description
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
        return instruction.getName();
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
}
