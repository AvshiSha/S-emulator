package ui.components.HistoryStats;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import semulator.program.SProgram;
import semulator.variable.Variable;
import ui.RunHistory;
import ui.RunResult;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class HistoryStats implements Initializable {

    @FXML
    private TableView<HistoryRow> historyTableView;
    @FXML
    private TableColumn<HistoryRow, String> runNumberColumn;
    @FXML
    private TableColumn<HistoryRow, String> runLevelColumn;
    @FXML
    private TableColumn<HistoryRow, String> inputsColumn;
    @FXML
    private TableColumn<HistoryRow, String> yValueColumn;
    @FXML
    private TableColumn<HistoryRow, String> cyclesColumn;

    @FXML
    private Button rerunButton;
    @FXML
    private Button clearHistoryButton;

    @FXML
    private Label variableStateLabel;
    @FXML
    private TableView<VariableRow> variableStateTableView;
    @FXML
    private TableColumn<VariableRow, String> variableNameColumn;
    @FXML
    private TableColumn<VariableRow, String> variableValueColumn;

    private ObservableList<HistoryRow> historyData = FXCollections.observableArrayList();
    private ObservableList<VariableRow> variableData = FXCollections.observableArrayList();

    private RunHistory runHistory;
    private SProgram currentProgram;
    private Consumer<List<Long>> inputCallback; // Callback to set inputs in debugger

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up history table
        runNumberColumn.setCellValueFactory(new PropertyValueFactory<>("runNumber"));
        runLevelColumn.setCellValueFactory(new PropertyValueFactory<>("runLevel"));
        inputsColumn.setCellValueFactory(new PropertyValueFactory<>("inputs"));
        yValueColumn.setCellValueFactory(new PropertyValueFactory<>("yValue"));
        cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));

        // Set up variable state table
        variableNameColumn.setCellValueFactory(new PropertyValueFactory<>("variableName"));
        variableValueColumn.setCellValueFactory(new PropertyValueFactory<>("variableValue"));

        // Make columns resizable
        runNumberColumn.setResizable(true);
        runLevelColumn.setResizable(true);
        inputsColumn.setResizable(true);
        yValueColumn.setResizable(true);
        cyclesColumn.setResizable(true);
        variableNameColumn.setResizable(true);
        variableValueColumn.setResizable(true);

        // Disable sorting
        runNumberColumn.setSortable(false);
        runLevelColumn.setSortable(false);
        inputsColumn.setSortable(false);
        yValueColumn.setSortable(false);
        cyclesColumn.setSortable(false);
        variableNameColumn.setSortable(false);
        variableValueColumn.setSortable(false);

        // Set up table data
        historyTableView.setItems(historyData);
        variableStateTableView.setItems(variableData);

        // Set up selection listener
        historyTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                rerunButton.setDisable(false);
                displayVariableState(Integer.parseInt(newSelection.getRunNumber()));
            } else {
                rerunButton.setDisable(true);
                clearVariableState();
            }
        });

        // Initialize run history
        runHistory = new RunHistory();
    }

    /**
     * Set the current program
     */
    public void setProgram(SProgram program) {
        this.currentProgram = program;
    }

    /**
     * Set the callback for setting inputs in the debugger
     */
    public void setInputCallback(Consumer<List<Long>> callback) {
        this.inputCallback = callback;
    }

    /**
     * Add a run to the history
     */
    public void addRun(int level, List<Long> inputs, long yValue, int cycles) {
        if (runHistory != null) {
            runHistory.addRun(level, inputs, yValue, cycles);
            refreshHistoryTable();
        }
    }

    /**
     * Clear all history
     */
    public void clearHistory() {
        if (runHistory != null) {
            runHistory.clear();
            refreshHistoryTable();
            clearVariableState();
            rerunButton.setDisable(true);
        }
    }

    /**
     * Refresh the history table
     */
    private void refreshHistoryTable() {
        Platform.runLater(() -> {
            historyData.clear();
            if (runHistory != null && !runHistory.isEmpty()) {
                for (RunResult run : runHistory.getAllRuns()) {
                    HistoryRow row = new HistoryRow(
                            String.valueOf(run.runNumber()),
                            String.valueOf(run.level()),
                            run.inputsCsv(),
                            String.valueOf(run.yValue()),
                            String.valueOf(run.cycles()));
                    historyData.add(row);
                }
            }
        });
    }

    /**
     * Display variable state for a selected run
     */
    private void displayVariableState(int runNumber) {
        Platform.runLater(() -> {
            variableData.clear();
            if (runHistory != null && currentProgram != null) {
                // Find the run
                RunResult selectedRun = null;
                for (RunResult run : runHistory.getAllRuns()) {
                    if (run.runNumber() == runNumber) {
                        selectedRun = run;
                        break;
                    }
                }

                if (selectedRun != null) {
                    // Simulate the run to get variable state
                    try {
                        semulator.execution.ProgramExecutor executor = new semulator.execution.ProgramExecutorImpl(
                                currentProgram);
                        semulator.program.ExpansionResult expandedProgram = currentProgram
                                .expandToDegree(selectedRun.level());
                        executor.run(selectedRun.inputs().toArray(new Long[0]));
                        Map<Variable, Long> variableState = executor.variableState();

                        // Display all variables
                        for (Map.Entry<Variable, Long> entry : variableState.entrySet()) {
                            VariableRow row = new VariableRow(entry.getKey().toString(),
                                    String.valueOf(entry.getValue()));
                            variableData.add(row);
                        }
                    } catch (Exception e) {
                        // If we can't simulate the run, just show the y value
                        VariableRow row = new VariableRow("y", String.valueOf(selectedRun.yValue()));
                        variableData.add(row);
                    }
                }
            }
        });
    }

    /**
     * Clear variable state display
     */
    private void clearVariableState() {
        Platform.runLater(() -> {
            variableData.clear();
        });
    }

    /**
     * Handle rerun button click
     */
    @FXML
    private void rerunSelectedRun() {
        HistoryRow selectedRow = historyTableView.getSelectionModel().getSelectedItem();
        if (selectedRow != null && inputCallback != null) {
            // Find the corresponding RunResult
            int runNumber = Integer.parseInt(selectedRow.getRunNumber());
            for (RunResult run : runHistory.getAllRuns()) {
                if (run.runNumber() == runNumber) {
                    // Set the inputs in the debugger
                    inputCallback.accept(run.inputs());
                    break;
                }
            }
        }
    }

    /**
     * Get the run history for saving/loading
     */
    public RunHistory getRunHistory() {
        return runHistory;
    }

    /**
     * Set the run history (for loading saved state)
     */
    public void setRunHistory(RunHistory history) {
        this.runHistory = history;
        refreshHistoryTable();
    }

    /**
     * Data model for history table rows
     */
    public static class HistoryRow {
        private final SimpleStringProperty runNumber;
        private final SimpleStringProperty runLevel;
        private final SimpleStringProperty inputs;
        private final SimpleStringProperty yValue;
        private final SimpleStringProperty cycles;

        public HistoryRow(String runNumber, String runLevel, String inputs, String yValue, String cycles) {
            this.runNumber = new SimpleStringProperty(runNumber);
            this.runLevel = new SimpleStringProperty(runLevel);
            this.inputs = new SimpleStringProperty(inputs);
            this.yValue = new SimpleStringProperty(yValue);
            this.cycles = new SimpleStringProperty(cycles);
        }

        // Getters for PropertyValueFactory
        public String getRunNumber() {
            return runNumber.get();
        }

        public String getRunLevel() {
            return runLevel.get();
        }

        public String getInputs() {
            return inputs.get();
        }

        public String getYValue() {
            return yValue.get();
        }

        public String getCycles() {
            return cycles.get();
        }
    }

    /**
     * Data model for variable state table rows
     */
    public static class VariableRow {
        private final SimpleStringProperty variableName;
        private final SimpleStringProperty variableValue;

        public VariableRow(String variableName, String variableValue) {
            this.variableName = new SimpleStringProperty(variableName);
            this.variableValue = new SimpleStringProperty(variableValue);
        }

        // Getters for PropertyValueFactory
        public String getVariableName() {
            return variableName.get();
        }

        public String getVariableValue() {
            return variableValue.get();
        }
    }
}