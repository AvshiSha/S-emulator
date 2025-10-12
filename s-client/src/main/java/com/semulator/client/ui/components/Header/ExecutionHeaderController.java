package com.semulator.client.ui.components.Header;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.service.ApiClient;
import com.semulator.engine.model.ExpansionResult;
import com.semulator.engine.model.SInstruction;
import com.semulator.engine.model.SProgram;
import com.semulator.engine.parse.SProgramImpl;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * Execution Header Controller - handles degree expansion and label/variable
 * highlighting
 * Simplified version of Header for the execution screen
 */
public class ExecutionHeaderController implements Initializable {

    @FXML
    private ComboBox<String> levelSelector;
    @FXML
    private Label lblDegreeStatus;
    @FXML
    private ComboBox<String> labelVariableComboBox;

    private ApiClient apiClient;
    private SProgram currentProgram;
    private String programName;
    private int currentDegree = 0;
    private int maxDegree = 0;

    private ObservableList<String> degreeList = FXCollections.observableArrayList();
    private ObservableList<String> labelVariableList = FXCollections.observableArrayList();

    // Store expansion results for each degree
    private Map<Integer, ExpansionResult> expansionResultsByDegree = new HashMap<>();
    private ExpansionResult currentExpansionResult;

    // Callbacks to parent controller
    private Consumer<Integer> onDegreeChanged;
    private Consumer<String> onLabelVariableSelected;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.apiClient = AppContext.getInstance().getApiClient();

        setupComboBoxes();
        setupEventHandlers();
    }

    private void setupComboBoxes() {
        levelSelector.setItems(degreeList);
        labelVariableComboBox.setItems(labelVariableList);
    }

    private void setupEventHandlers() {
        levelSelector.setOnAction(event -> onLevelSelected());
        labelVariableComboBox.setOnAction(event -> onLabelVariableSelected());
    }

    public void setProgram(SProgram program) {
        this.currentProgram = program;
        this.programName = program.getName();
        this.maxDegree = program.calculateMaxDegree();

        // Initialize degree selector
        updateDegreeSelector();

        // Initialize label/variable selector with degree 0
        expandToCurrentDegree();
    }

    public void setProgramInfo(String programName, int maxDegree) {
        this.programName = programName;
        this.maxDegree = maxDegree;
        this.currentDegree = 0;

        // Initialize degree selector
        updateDegreeSelector();

        System.out.println("ExecutionHeader: Set program " + programName + " with maxDegree " + maxDegree);
    }

    private void updateDegreeSelector() {
        degreeList.clear();
        for (int i = 0; i <= maxDegree; i++) {
            degreeList.add("Degree " + i);
        }

        if (!degreeList.isEmpty()) {
            levelSelector.setValue("Degree 0");
        }

        updateDegreeStatus();
    }

    private void updateDegreeStatus() {
        lblDegreeStatus.setText(currentDegree + " / " + maxDegree);
    }

    private void onLevelSelected() {
        String selected = levelSelector.getValue();
        System.out.println("DEBUG ExecutionHeader: onLevelSelected called with: " + selected);

        if (selected == null || selected.isEmpty()) {
            System.out.println("DEBUG ExecutionHeader: Selection is null or empty");
            return;
        }

        // Parse degree from "Degree X"
        try {
            int degree = Integer.parseInt(selected.replace("Degree ", ""));
            System.out.println("DEBUG ExecutionHeader: Parsed degree: " + degree + ", current: " + currentDegree);

            if (degree != currentDegree) {
                currentDegree = degree;
                expandToCurrentDegree();
                updateDegreeStatus();
            } else {
                System.out.println("DEBUG ExecutionHeader: Degree unchanged, skipping");
            }
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse degree: " + selected);
        }
    }

    private void expandToCurrentDegree() {
        // Simply notify the parent controller about the degree change
        // The parent will request expanded instructions from the server
        System.out.println("ExecutionHeader: Degree changed to " + currentDegree);
        notifyDegreeChanged();
    }

    private void updateLabelVariableList() {
        labelVariableList.clear();

        if (currentExpansionResult == null) {
            return;
        }

        Set<String> labelsAndVariables = new HashSet<>();

        // Collect all labels and variables from expanded instructions
        for (SInstruction instruction : currentExpansionResult.instructions()) {
            // Add label if present
            if (instruction.getLabel() != null && instruction.getLabel().getLabel() != null) {
                String label = instruction.getLabel().getLabel();
                if (!label.isEmpty()) {
                    labelsAndVariables.add(label);
                }
            }

            // Add variable if present
            if (instruction.getVariable() != null) {
                String variable = instruction.getVariable().toString();
                if (!variable.isEmpty()) {
                    labelsAndVariables.add(variable);
                }
            }
        }

        // Sort and add to list
        List<String> sorted = new ArrayList<>(labelsAndVariables);
        Collections.sort(sorted);
        labelVariableList.addAll(sorted);
    }

    private void onLabelVariableSelected() {
        String selected = labelVariableComboBox.getValue();
        if (selected != null && !selected.isEmpty() && onLabelVariableSelected != null) {
            onLabelVariableSelected.accept(selected);
        }
    }

    private void notifyDegreeChanged() {
        if (onDegreeChanged != null) {
            onDegreeChanged.accept(currentDegree);
        }
    }

    // Callback setters
    public void setOnDegreeChanged(Consumer<Integer> callback) {
        this.onDegreeChanged = callback;
    }

    public void setOnLabelVariableSelected(Consumer<String> callback) {
        this.onLabelVariableSelected = callback;
    }

    public ExpansionResult getCurrentExpansionResult() {
        return currentExpansionResult;
    }

    public int getCurrentDegree() {
        return currentDegree;
    }

    public int getMaxDegree() {
        return maxDegree;
    }

    public void updateLabelVariableList(List<String> labelsAndVariables) {
        labelVariableList.clear();

        // Sort variables by priority: output labels (y) -> labels (L1, L2, ...) ->
        // input variables (x1, x2, ...) -> working variables
        List<String> sortedList = new ArrayList<>(labelsAndVariables);
        sortedList.sort((a, b) -> {
            // Priority 1: Output labels (y, y1, y2, etc.)
            boolean aIsOutput = a.matches("^y\\d*$");
            boolean bIsOutput = b.matches("^y\\d*$");
            if (aIsOutput && !bIsOutput)
                return -1;
            if (!aIsOutput && bIsOutput)
                return 1;
            if (aIsOutput && bIsOutput) {
                // Sort output variables: y, y1, y2, etc.
                return extractNumber(a).compareTo(extractNumber(b));
            }

            // Priority 2: Labels (L1, L2, L3, etc.)
            boolean aIsLabel = a.matches("^L\\d+$");
            boolean bIsLabel = b.matches("^L\\d+$");
            if (aIsLabel && !bIsLabel)
                return -1;
            if (!aIsLabel && bIsLabel)
                return 1;
            if (aIsLabel && bIsLabel) {
                // Sort labels numerically: L1, L2, L3, etc.
                return extractNumber(a).compareTo(extractNumber(b));
            }

            // Priority 3: Input variables (x1, x2, x3, etc.)
            boolean aIsInput = a.matches("^x\\d+$");
            boolean bIsInput = b.matches("^x\\d+$");
            if (aIsInput && !bIsInput)
                return -1;
            if (!aIsInput && bIsInput)
                return 1;
            if (aIsInput && bIsInput) {
                // Sort input variables numerically: x1, x2, x3, etc.
                return extractNumber(a).compareTo(extractNumber(b));
            }

            // Priority 4: Working variables (everything else) - alphabetical
            return a.compareTo(b);
        });

        labelVariableList.addAll(sortedList);
        System.out.println("ExecutionHeader: Updated label/variable list with " + labelsAndVariables.size() + " items");
    }

    private Integer extractNumber(String variable) {
        // Extract number from variables like "y1", "L2", "x3", etc.
        String numberStr = variable.replaceAll("\\D+", "");
        if (numberStr.isEmpty()) {
            return 0; // For variables like "y" without number
        }
        try {
            return Integer.parseInt(numberStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Set the degree programmatically (for re-run functionality)
     */
    public void setDegree(int degree) {
        if (degree >= 0 && degree <= maxDegree) {
            this.currentDegree = degree;
            Platform.runLater(() -> {
                levelSelector.setValue("Degree " + degree);
                updateDegreeStatus();
                expandToCurrentDegree();
            });
        }
    }
}
