package ui.components.HistoryStats;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import semulator.program.SProgram;
import semulator.variable.Variable;
import ui.RunHistory;
import ui.RunResult;
import ui.Theme;
import ui.ThemeManager;
import ui.animations.Animations;

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
    private ComboBox<Theme> themeSelector;
    @FXML
    private ComboBox<String> fontSizeSelector;
    @FXML
    private CheckBox animationsCheckBox;

    private ObservableList<HistoryRow> historyData = FXCollections.observableArrayList();
    private ThemeManager themeManager;

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

        // Make columns resizable
        runNumberColumn.setResizable(true);
        runLevelColumn.setResizable(true);
        inputsColumn.setResizable(true);
        yValueColumn.setResizable(true);
        cyclesColumn.setResizable(true);

        // Disable sorting
        runNumberColumn.setSortable(false);
        runLevelColumn.setSortable(false);
        inputsColumn.setSortable(false);
        yValueColumn.setSortable(false);
        cyclesColumn.setSortable(false);

        // Set up table data
        historyTableView.setItems(historyData);

        // Set up selection listener
        historyTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                rerunButton.setDisable(false);
            } else {
                rerunButton.setDisable(true);
            }
        });

        // Initialize run history
        runHistory = new RunHistory();

        // Initialize theme manager and controls
        themeManager = ThemeManager.getInstance();

        // Initialize theme selector
        themeSelector.getItems().addAll(Theme.values());
        themeSelector.setValue(themeManager.getCurrentTheme());

        // Initialize font size selector
        fontSizeSelector.getItems().addAll("12px", "14px", "16px", "18px");
        fontSizeSelector.setValue(themeManager.getCurrentFontSize());

        // Initialize animations checkbox with current state
        animationsCheckBox.setSelected(Animations.isEnabled());
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
     * Handle theme selection
     */
    @FXML
    private void onThemeSelected() {
        Theme selectedTheme = themeSelector.getSelectionModel().getSelectedItem();
        if (selectedTheme != null) {
            themeManager.setTheme(selectedTheme);

            // Apply the theme to the current scene
            Stage stage = (Stage) themeSelector.getScene().getWindow();
            if (stage != null && stage.getScene() != null) {
                themeManager.applyCurrentTheme(stage.getScene());
            }
        }
    }

    /**
     * Handle font size selection
     */
    @FXML
    private void onFontSizeSelected() {
        String selectedFontSize = fontSizeSelector.getSelectionModel().getSelectedItem();
        if (selectedFontSize != null) {
            themeManager.setFontSize(selectedFontSize);

            // Force font size change by reapplying the entire theme
            Stage stage = (Stage) fontSizeSelector.getScene().getWindow();
            if (stage != null && stage.getScene() != null) {
                // Clear all stylesheets first
                stage.getScene().getStylesheets().clear();
                // Reapply the current theme with new font size
                themeManager.applyCurrentTheme(stage.getScene());
            }
        }
    }

    /**
     * Handle animations toggle
     */
    @FXML
    private void onAnimationsToggled() {
        boolean enabled = animationsCheckBox.isSelected();
        Animations.setEnabled(enabled);

        // Update the checkbox text to reflect current state
        animationsCheckBox.setText(enabled ? "Animations" : "Animations (Off)");
    }

}
