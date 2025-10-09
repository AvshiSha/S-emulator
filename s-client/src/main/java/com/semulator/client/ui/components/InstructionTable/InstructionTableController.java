package com.semulator.client.ui.components.InstructionTable;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

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

    private ObservableList<InstructionRow> instructionData = FXCollections.observableArrayList();
    private ApiClient apiClient;
    private String currentHighlightTerm = null;
    private int currentExecutingInstructionIndex = -1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.apiClient = AppContext.getInstance().getApiClient();

        setupTable();
        setupColumns();
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
        System.out.println("Updating degree view to: " + degree);
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

            // Add sample instructions
            instructionData.add(new InstructionRow(1, "B", "", "LOAD R1, #5", 2, "R1"));
            instructionData.add(new InstructionRow(2, "B", "", "STORE R1, X", 3, "X"));
            instructionData.add(new InstructionRow(3, "B", "LOOP", "LOAD R2, Y", 2, "R2"));
            instructionData.add(new InstructionRow(4, "B", "", "ADD R1, R2", 1, "R1"));
            instructionData.add(new InstructionRow(5, "B", "", "STORE R1, RESULT", 3, "RESULT"));
            instructionData.add(new InstructionRow(6, "B", "", "BRANCH LOOP", 1, ""));

            System.out.println("Loaded " + instructionData.size() + " instructions for " + type + ": " + name);
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

    // Inner class for instruction row data
    public static class InstructionRow {
        private final IntegerProperty rowNumber;
        private final StringProperty commandType;
        private final StringProperty label;
        private final StringProperty instructionType;
        private final IntegerProperty cycles;
        private final StringProperty variable;
        private boolean highlighted = false;

        public InstructionRow(int rowNumber, String commandType, String label,
                String instructionType, int cycles, String variable) {
            this.rowNumber = new SimpleIntegerProperty(rowNumber);
            this.commandType = new SimpleStringProperty(commandType);
            this.label = new SimpleStringProperty(label);
            this.instructionType = new SimpleStringProperty(instructionType);
            this.cycles = new SimpleIntegerProperty(cycles);
            this.variable = new SimpleStringProperty(variable);
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

        // Highlighting
        public boolean isHighlighted() {
            return highlighted;
        }

        public void setHighlighted(boolean highlighted) {
            this.highlighted = highlighted;
        }
    }
}
