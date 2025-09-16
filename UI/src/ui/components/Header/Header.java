package ui.components.Header;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import ui.components.InstructionTable.InstructionTable;
import ui.components.HistoryStats.HistoryStats;

import java.io.File;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

public class Header {
  @FXML
  private Button loadFileButton;

  @FXML
  private TextField filePathField;

  @FXML
  private ProgressBar progressBar;

  @FXML
  private Label progressLabel;

  @FXML
  private Button btnDecreaseDegree;

  @FXML
  private Label lblDegreeStatus;

  @FXML
  private Button btnIncreaseDegree;

  @FXML
  private ComboBox<String> programFunctionSelector;

  @FXML
  private ComboBox<String> labelVariableComboBox;

  private SProgram sProgram;
  private Path loadedXmlPath;
  private InstructionTable instructionTable;
  private HistoryStats historyStats;
  private ui.components.DebuggerExecution.DebuggerExecution debuggerExecution;

  // Track current selection state for program/function selector
  private boolean isShowingFunction = false;
  private String currentFunctionName = null;
  private boolean isProgrammaticallySettingSelection = false;
  private int currentDegree = 0;
  private int maxDegree = 0;

  // For tracking labels and variables
  private ObservableList<String> labelVariableList = FXCollections.observableArrayList();

  // Store expansion results for history chain tracking
  private semulator.program.ExpansionResult currentExpansionResult;
  private java.util.Map<Integer, semulator.program.ExpansionResult> expansionResultsByDegree = new java.util.HashMap<>();

  public Header() {
    this.sProgram = new SProgramImpl("S");
  }

  @FXML
  private void initialize() {
    // Initialize degree controls to disabled state
    updateDegreeDisplay();
    updateButtonStates();

    // Initialize the label/variable combo box
    labelVariableComboBox.setItems(labelVariableList);
    labelVariableComboBox.setDisable(true); // Disabled until a program is loaded
  }

  @FXML
  public void loadFileButtonPressed(ActionEvent event) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select XML File");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("XML Files", "*.xml"));

    // Get the stage from the event
    Stage stage = (Stage) loadFileButton.getScene().getWindow();
    File selectedFile = fileChooser.showOpenDialog(stage);

    if (selectedFile != null) {
      // Disable button and show progress bar
      loadFileButton.setDisable(true);
      progressBar.setVisible(true);
      progressLabel.setVisible(true);
      progressBar.setProgress(0.0);
      // Create and start the file loading task
      FileLoadingTask loadingTask = new FileLoadingTask(selectedFile);

      // Bind progress bar to task progress
      progressBar.progressProperty().bind(loadingTask.progressProperty());

      // Handle task completion
      loadingTask.setOnSucceeded(e -> {
        try {
          FileLoadResult result = loadingTask.get();
          if (result.isSuccess()) {
            // Replace the current file (only one active at a time)
            loadedXmlPath = result.getLoadedPath();
            filePathField.setText(loadedXmlPath.toString());

            // Initialize degree controls
            initializeDegreeControls();

            // Display the loaded program in the instruction table
            if (instructionTable != null) {
              instructionTable.displayProgram(sProgram);
            }

            // Populate the program/function selector
            populateProgramFunctionSelector();

            // Set the program in the history stats component
            if (historyStats != null) {
              historyStats.setProgram(sProgram);
            }

            // Set the program in the debugger execution component
            if (debuggerExecution != null) {
              debuggerExecution.setProgram(sProgram);
            }

            // Populate the label/variable combo box
            populateLabelVariableComboBox();

            showSuccessAlert("File Loaded", "XML file loaded successfully!");
          } else {
            showErrorAlert("Load Failed", result.getErrorMessage());
          }
        } catch (Exception ex) {
          showErrorAlert("Error", "Unexpected error: " + ex.getMessage());
        } finally {
          // Re-enable button and hide progress bar
          loadFileButton.setDisable(false);
          progressBar.setVisible(false);
          progressLabel.setVisible(false);
          progressBar.progressProperty().unbind();
        }
      });

      // Handle task failure
      loadingTask.setOnFailed(e -> {
        showErrorAlert("Error", "Failed to load file: " + loadingTask.getException().getMessage());
        loadFileButton.setDisable(false);
        progressBar.setVisible(false);
        progressLabel.setVisible(false);
        progressBar.progressProperty().unbind();
      });

      // Start the task in a background thread
      Thread loadingThread = new Thread(loadingTask);
      loadingThread.setDaemon(true);
      loadingThread.start();
    }
  }

  private void showErrorAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private void showSuccessAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  // Getter methods for other components to access the loaded program
  public SProgram getSProgram() {
    return sProgram;
  }

  public Path getLoadedXmlPath() {
    return loadedXmlPath;
  }

  public boolean isProgramLoaded() {
    return loadedXmlPath != null && sProgram.getInstructions() != null && !sProgram.getInstructions().isEmpty();
  }

  // Method to set the instruction table reference
  public void setInstructionTable(InstructionTable instructionTable) {
    this.instructionTable = instructionTable;
  }

  // Method to capture detailed XML validation errors
  private String captureDetailedValidationErrors(Path xmlPath) {
    try {
      // Create a custom SProgram instance for validation
      SProgramImpl validationProgram = new SProgramImpl("Validation");

      // First validate the path (this sets the xmlPath internally)
      String basicValidation = validationProgram.validate(xmlPath);
      if (!basicValidation.equals("Valid")) {
        return basicValidation; // Return basic validation error
      }

      // Capture System.out.println output
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream originalOut = System.out;
      PrintStream capturedOut = new PrintStream(baos);

      try {
        // Redirect System.out to capture validation messages
        System.setOut(capturedOut);

        // Perform the detailed validation by calling load() which internally calls
        // validateXmlFile
        Object result = validationProgram.load();

        // Get the captured output
        String capturedMessages = baos.toString();

        // If load failed (returned null), return the captured error messages
        if (result == null && !capturedMessages.trim().isEmpty()) {
          return capturedMessages.trim();
        }

        return null; // No errors captured

      } finally {
        // Restore original System.out
        System.setOut(originalOut);
        capturedOut.close();
      }

    } catch (Exception e) {
      return "Error during validation: " + e.getMessage();
    }
  }

  // Inner class for file loading task
  private class FileLoadingTask extends Task<FileLoadResult> {
    private final File selectedFile;

    public FileLoadingTask(File selectedFile) {
      this.selectedFile = selectedFile;
    }

    @Override
    protected FileLoadResult call() throws Exception {
      try {
        // Update progress: 10% - Starting validation
        updateProgress(0.1, 1.0);
        Thread.sleep(200); // Artificial delay

        // Validate file path (handles spaces in paths)
        Path xmlPath = selectedFile.toPath();
        String validationResult = sProgram.validate(xmlPath);

        // Update progress: 30% - Validation complete
        updateProgress(0.3, 1.0);
        Thread.sleep(300); // Artificial delay

        if (!validationResult.equals("Valid")) {
          return new FileLoadResult(false, null, validationResult);
        }

        // Update progress: 50% - Starting file load
        updateProgress(0.5, 1.0);
        Thread.sleep(400); // Artificial delay

        // Load the program
        Object loadResult = sProgram.load();

        // Update progress: 80% - File loaded
        updateProgress(0.8, 1.0);
        Thread.sleep(300); // Artificial delay

        // Update progress: 100% - Complete
        updateProgress(1.0, 1.0);
        Thread.sleep(200); // Final artificial delay

        if (loadResult instanceof Path path) {
          return new FileLoadResult(true, path, null);
        } else if (loadResult != null) {
          return new FileLoadResult(true, Path.of(loadResult.toString()), null);
        } else {
          // Capture detailed validation errors for display in GUI
          String detailedErrors = captureDetailedValidationErrors(xmlPath);
          if (detailedErrors != null && !detailedErrors.trim().isEmpty()) {
            return new FileLoadResult(false, null, detailedErrors);
          } else {
            return new FileLoadResult(false, null, "Failed to load the XML file. The XML structure may be invalid.");
          }
        }

      } catch (Exception e) {
        return new FileLoadResult(false, null, "Error loading file: " + e.getMessage());
      }
    }
  }

  // Result class for file loading operations
  private static class FileLoadResult {
    private final boolean success;
    private final Path loadedPath;
    private final String errorMessage;

    public FileLoadResult(boolean success, Path loadedPath, String errorMessage) {
      this.success = success;
      this.loadedPath = loadedPath;
      this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
      return success;
    }

    public Path getLoadedPath() {
      return loadedPath;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }

  // Degree control methods
  private void initializeDegreeControls() {
    try {
      currentDegree = 0;
      maxDegree = sProgram.calculateMaxDegree();

      // Store the base program (degree 0) expansion result
      currentExpansionResult = sProgram.expandToDegree(0);
      expansionResultsByDegree.put(0, currentExpansionResult);

      // Ensure the debugger execution component gets the original program (degree 0)
      if (debuggerExecution != null) {
        debuggerExecution.setProgram(sProgram);
      }

      updateDegreeDisplay();
      updateButtonStates();
    } catch (Exception e) {
      showErrorAlert("Degree Calculation Error", "Failed to calculate maximum degree: " + e.getMessage());
      currentDegree = 0;
      maxDegree = 0;
      updateDegreeDisplay();
      updateButtonStates();
    }
  }

  @FXML
  public void decreaseDegreePressed(ActionEvent event) {
    System.out
        .println("DEBUG: decreaseDegreePressed called, currentDegree: " + currentDegree + ", maxDegree: " + maxDegree);
    if (currentDegree > 0) {
      currentDegree--;
      expandToDegree(currentDegree);
    } else {
      System.out.println("DEBUG: Cannot decrease degree, already at minimum");
    }
  }

  @FXML
  public void increaseDegreePressed(ActionEvent event) {
    System.out
        .println("DEBUG: increaseDegreePressed called, currentDegree: " + currentDegree + ", maxDegree: " + maxDegree);
    if (currentDegree < maxDegree) {
      currentDegree++;
      expandToDegree(currentDegree);
    } else {
      System.out.println("DEBUG: Cannot increase degree, already at maximum");
    }
  }

  private void expandToDegree(int degree) {
    System.out.println("DEBUG: expandToDegree called with degree: " + degree);
    try {
      // DEBUG: Run expansion directly instead of using Task
      System.out.println("DEBUG: Starting expansion for degree: " + degree);
      // Always expand the main program, regardless of what's currently displayed
      // Perform expansion directly
      semulator.program.ExpansionResult result = sProgram.expandToDegree(degree);
      System.out.println("DEBUG: Expansion completed, got " + result.instructions().size() + " instructions");

      // Update UI directly (no Platform.runLater needed)
      try {
        // Store the expansion result for history chain tracking
        currentExpansionResult = result;
        expansionResultsByDegree.put(degree, result);

        // Create a temporary SProgram to hold the expanded instructions
        SProgramImpl expandedProgram = new SProgramImpl("Expanded");
        for (semulator.instructions.SInstruction instruction : result.instructions()) {
          expandedProgram.addInstruction(instruction);
        }

        // Copy functions from original program to expanded program
        if (sProgram instanceof SProgramImpl) {
          SProgramImpl originalProgram = (SProgramImpl) sProgram;
          var originalFunctions = originalProgram.getFunctions();
          for (Map.Entry<String, java.util.List<semulator.instructions.SInstruction>> entry : originalFunctions
              .entrySet()) {
            expandedProgram.getFunctions().put(entry.getKey(), entry.getValue());
          }
          System.out.println("DEBUG: Copied " + originalFunctions.size() + " functions to expanded program");
        }

        // Display the expanded program
        System.out.println("DEBUG: About to display expanded program with " + expandedProgram.getInstructions().size()
            + " instructions");
        if (instructionTable != null) {
          System.out.println("DEBUG: instructionTable is not null, calling displayProgram");
          instructionTable.displayProgram(expandedProgram);
          System.out.println("DEBUG: displayProgram called successfully");
        } else {
          System.out.println("DEBUG: instructionTable is null!");
        }

        // For expanded programs, we should only show "Program" in the selector
        // since functions are not available in expanded form
        System.out.println("DEBUG: About to reset program/function selector");
        if (programFunctionSelector != null) {
          // Set flag BEFORE any operations that might trigger events
          System.out.println("DEBUG: Setting programmatically setting selection flag");
          isProgrammaticallySettingSelection = true;

          ObservableList<String> items = FXCollections.observableArrayList();
          items.add("Program");
          programFunctionSelector.setItems(items);

          // Reset function selection state since we're now showing expanded program
          isShowingFunction = false;
          currentFunctionName = null;

          // Set selection without triggering event (to avoid overwriting expanded program
          // display)
          programFunctionSelector.getSelectionModel().select(0);
          programFunctionSelector.setDisable(false);

          // Clear flag AFTER all operations are complete
          isProgrammaticallySettingSelection = false;
          System.out.println("DEBUG: Finished setting programmatically setting selection flag");
        }
        System.out.println("DEBUG: Finished resetting program/function selector");

        // Update the debugger execution component with the appropriate program
        System.out.println("DEBUG: About to update debugger execution component");
        if (debuggerExecution != null) {
          if (degree == 0) {
            // For degree 0, use the original program
            System.out.println("DEBUG: Setting debugger to original program");
            debuggerExecution.setProgram(sProgram);
          } else {
            // For higher degrees, use the expanded program
            System.out.println("DEBUG: Setting debugger to expanded program");
            debuggerExecution.setProgram(expandedProgram);
          }
        }
        System.out.println("DEBUG: Finished updating debugger execution component");

        // Update the label/variable combo box with the current program
        // For degree 0, use original program; for higher degrees, use expanded program
        System.out.println("DEBUG: About to populate label/variable combo box for degree: " + degree);
        if (degree == 0) {
          populateLabelVariableComboBox(sProgram);
        } else {
          populateLabelVariableComboBox(expandedProgram);
        }
        System.out.println("DEBUG: Finished populating label/variable combo box");

        // Update current degree
        currentDegree = degree;

        // Update degree display and button states
        System.out.println("DEBUG: About to update degree display and button states");
        updateDegreeDisplay();
        updateButtonStates();
        System.out.println("DEBUG: Finished updating degree display and button states");

      } catch (Exception e) {
        System.out.println("DEBUG: Exception in expansion: " + e.getMessage());
        e.printStackTrace();
        showErrorAlert("Expansion Error", "Failed to display expanded program: " + e.getMessage());
        // Revert to previous degree
        if (degree > currentDegree) {
          currentDegree--;
        } else if (degree < currentDegree) {
          currentDegree++;
        }
        updateDegreeDisplay();
        updateButtonStates();
      }

    } catch (Exception e) {
      System.out.println("DEBUG: Exception in expandToDegree: " + e.getMessage());
      e.printStackTrace();
      showErrorAlert("Expansion Error", "Failed to expand to degree " + degree + ": " + e.getMessage());
      // Revert to previous degree
      if (degree > currentDegree) {
        currentDegree--;
      } else if (degree < currentDegree) {
        currentDegree++;
      }
      updateDegreeDisplay();
      updateButtonStates();
    }
  }

  private void updateDegreeDisplay() {
    if (sProgram == null || maxDegree == 0) {
      lblDegreeStatus.setText("— / —");
    } else {
      lblDegreeStatus.setText(currentDegree + " / " + maxDegree);
    }
  }

  private void updateButtonStates() {
    if (sProgram == null) {
      btnDecreaseDegree.setDisable(true);
      btnIncreaseDegree.setDisable(true);
    } else {
      btnDecreaseDegree.setDisable(currentDegree == 0);
      btnIncreaseDegree.setDisable(currentDegree == maxDegree);
    }
  }

  // Method to disable/enable expansion controls during debug execution
  public void setExpansionControlsEnabled(boolean enabled) {
    btnDecreaseDegree.setDisable(!enabled || currentDegree == 0);
    btnIncreaseDegree.setDisable(!enabled || currentDegree == maxDegree);
  }

  // Method to get the history chain for a selected instruction
  public java.util.List<semulator.instructions.SInstruction> getHistoryChain(
      semulator.instructions.SInstruction selectedInstruction) {
    java.util.List<semulator.instructions.SInstruction> chain = new java.util.ArrayList<>();

    if (selectedInstruction == null || currentExpansionResult == null) {
      return chain;
    }

    // Start with the selected instruction
    chain.add(selectedInstruction);

    // Trace back through the parent chain
    semulator.instructions.SInstruction current = selectedInstruction;
    int currentDegreeForTracing = currentDegree;

    // Use a local variable for the expansion result to avoid modifying class state
    semulator.program.ExpansionResult tracingExpansionResult = currentExpansionResult;

    while (currentDegreeForTracing > 0) {

      // Get the parent of the current instruction from the current expansion result
      // First try exact object identity match
      semulator.instructions.SInstruction parent = tracingExpansionResult.parent().get(current);

      // If no exact match, try matching by name and variable
      if (parent == null) {

        // Look for a parent that has the same name and variable but different object
        // identity
        // The parent map structure is: child -> parent, so we need to look for the
        // current instruction as a KEY
        for (java.util.Map.Entry<semulator.instructions.SInstruction, semulator.instructions.SInstruction> entry : tracingExpansionResult
            .parent().entrySet()) {
          if (entry.getKey().getName().equals(current.getName()) &&
              entry.getKey().getVariable().equals(current.getVariable())) {
            parent = entry.getValue();
            break;
          }
        }
      }

      if (parent == null) {
        // No parent found, we've reached the end of the chain
        break;
      }

      // Add parent to chain
      chain.add(parent);

      // Move to the previous degree
      currentDegreeForTracing--;

      // Get the expansion result for the previous degree
      semulator.program.ExpansionResult prevDegreeResult = expansionResultsByDegree.get(currentDegreeForTracing);
      if (prevDegreeResult == null) {
        // If we don't have the previous degree result, we need to get it
        try {
          prevDegreeResult = sProgram.expandToDegree(currentDegreeForTracing);
          expansionResultsByDegree.put(currentDegreeForTracing, prevDegreeResult);
        } catch (Exception e) {
          // If we can't get the previous degree, stop tracing
          break;
        }
      }

      // Update local expansion result for next iteration (don't modify class field)
      tracingExpansionResult = prevDegreeResult;
      current = parent;
    }

    return chain;
  }

  // Method to set the debugger execution component
  public void setDebuggerExecution(ui.components.DebuggerExecution.DebuggerExecution debuggerExecution) {
    this.debuggerExecution = debuggerExecution;
  }

  // Method to set the history stats component
  public void setHistoryStats(HistoryStats historyStats) {
    this.historyStats = historyStats;
    if (historyStats != null && sProgram != null) {
      historyStats.setProgram(sProgram);
    }
  }

  // Method to populate the label/variable combo box
  private void populateLabelVariableComboBox() {
    System.out.println("DEBUG: populateLabelVariableComboBox() called with currentDegree: " + currentDegree);
    // Determine which program to use based on current degree
    SProgram programToUse;
    if (currentDegree == 0) {
      programToUse = sProgram; // Use original program for degree 0
      System.out.println("DEBUG: Using original program for degree 0");
    } else {
      // For higher degrees, use the expanded program from current expansion result
      if (currentExpansionResult != null) {
        // Create a temporary program from the current expansion result
        SProgramImpl expandedProgram = new SProgramImpl("Expanded");
        for (semulator.instructions.SInstruction instruction : currentExpansionResult.instructions()) {
          expandedProgram.addInstruction(instruction);
        }

        // Copy functions from original program to expanded program
        if (sProgram instanceof SProgramImpl) {
          SProgramImpl originalProgram = (SProgramImpl) sProgram;
          var originalFunctions = originalProgram.getFunctions();
          for (Map.Entry<String, java.util.List<semulator.instructions.SInstruction>> entry : originalFunctions
              .entrySet()) {
            expandedProgram.getFunctions().put(entry.getKey(), entry.getValue());
          }
          System.out.println("DEBUG: Copied " + originalFunctions.size() + " functions to expanded program");
        }
        programToUse = expandedProgram;
        System.out.println("DEBUG: Using expanded program for degree " + currentDegree);
      } else {
        programToUse = sProgram; // Fallback to original program
        System.out.println("DEBUG: Using original program as fallback");
      }
    }

    populateLabelVariableComboBox(programToUse);
  }

  // Overloaded method to populate the combo box with a specific program
  private void populateLabelVariableComboBox(SProgram program) {
    System.out.println(
        "DEBUG: populateLabelVariableComboBox called with program: " + (program != null ? program.getName() : "null"));
    labelVariableList.clear();

    if (program == null || program.getInstructions() == null) {
      labelVariableComboBox.setDisable(true);
      return;
    }

    java.util.Set<String> uniqueItems = new java.util.TreeSet<>();

    // Extract labels and variables from the specified program
    for (semulator.instructions.SInstruction instruction : program.getInstructions()) {
      // Add the main variable
      if (instruction.getVariable() != null) {
        uniqueItems.add(instruction.getVariable().toString());
      }

      // Add the label
      if (instruction.getLabel() != null) {
        if (instruction.getLabel().isExit()) {
          uniqueItems.add("EXIT");
        } else if (instruction.getLabel().getLabel() != null) {
          uniqueItems.add(instruction.getLabel().getLabel());
        }
      }

      // Add source variables for assignments
      if (instruction instanceof semulator.instructions.AssignVariableInstruction assignVar) {
        if (assignVar.getSource() != null) {
          uniqueItems.add(assignVar.getSource().toString());
        }
      }

      // Add other variables for comparisons
      if (instruction instanceof semulator.instructions.JumpEqualVariableInstruction jumpVar) {
        if (jumpVar.getOther() != null) {
          uniqueItems.add(jumpVar.getOther().toString());
        }
      }

      // Add function arguments for QUOTE and JUMP_EQUAL_FUNCTION
      if (instruction instanceof semulator.instructions.QuoteInstruction quote) {
        for (semulator.variable.Variable arg : quote.getFunctionArguments()) {
          uniqueItems.add(arg.toString());
        }
      }
      if (instruction instanceof semulator.instructions.JumpEqualFunctionInstruction jumpEqualFunc) {
        for (semulator.variable.Variable arg : jumpEqualFunc.getFunctionArguments()) {
          uniqueItems.add(arg.toString());
        }
      }
    }

    // Sort items in the specified order: labels, y, x variables, z variables
    java.util.List<String> sortedItems = new java.util.ArrayList<>();

    // 1. Add labels (L1, L2, L3, etc.) - sorted by number
    java.util.List<String> labels = new java.util.ArrayList<>();
    for (String item : uniqueItems) {
      if (item.startsWith("L") && item.length() > 1) {
        try {
          Integer.parseInt(item.substring(1)); // Validate it's a number
          labels.add(item);
        } catch (NumberFormatException e) {
          // Skip invalid label format
        }
      }
    }
    labels.sort((a, b) -> {
      int numA = Integer.parseInt(a.substring(1));
      int numB = Integer.parseInt(b.substring(1));
      return Integer.compare(numA, numB);
    });
    sortedItems.addAll(labels);

    // 2. Add output variable (y)
    if (uniqueItems.contains("y")) {
      sortedItems.add("y");
    }

    // 3. Add input variables (x1, x2, x3, etc.) - sorted by number
    java.util.List<String> xVars = new java.util.ArrayList<>();
    for (String item : uniqueItems) {
      if (item.startsWith("x") && item.length() > 1) {
        try {
          Integer.parseInt(item.substring(1)); // Validate it's a number
          xVars.add(item);
        } catch (NumberFormatException e) {
          // Skip invalid x variable format
        }
      }
    }
    xVars.sort((a, b) -> {
      int numA = Integer.parseInt(a.substring(1));
      int numB = Integer.parseInt(b.substring(1));
      return Integer.compare(numA, numB);
    });
    sortedItems.addAll(xVars);

    // 4. Add working variables (z1, z2, z3, etc.) - sorted by number
    java.util.List<String> zVars = new java.util.ArrayList<>();
    for (String item : uniqueItems) {
      if (item.startsWith("z") && item.length() > 1) {
        try {
          Integer.parseInt(item.substring(1)); // Validate it's a number
          zVars.add(item);
        } catch (NumberFormatException e) {
          // Skip invalid z variable format
        }
      }
    }
    zVars.sort((a, b) -> {
      int numA = Integer.parseInt(a.substring(1));
      int numB = Integer.parseInt(b.substring(1));
      return Integer.compare(numA, numB);
    });
    sortedItems.addAll(zVars);

    // 5. Add any other items (like EXIT) that don't fit the above categories
    for (String item : uniqueItems) {
      if (!sortedItems.contains(item)) {
        sortedItems.add(item);
      }
    }

    // Add all sorted items to the list
    labelVariableList.addAll(sortedItems);

    // Enable the combo box if we have items
    labelVariableComboBox.setDisable(labelVariableList.isEmpty());
  }

  // Method to handle Program/Function selector
  @FXML
  private void onProgramFunctionSelected(ActionEvent event) {
    System.out.println("DEBUG: onProgramFunctionSelected called, isProgrammaticallySettingSelection: "
        + isProgrammaticallySettingSelection);
    // Skip if we're programmatically setting the selection
    if (isProgrammaticallySettingSelection) {
      System.out.println("DEBUG: Skipping onProgramFunctionSelected due to programmatic setting");
      return;
    }

    String selectedItem = programFunctionSelector.getSelectionModel().getSelectedItem();
    System.out.println("DEBUG: onProgramFunctionSelected processing selection: " + selectedItem);
    if (instructionTable != null && sProgram != null) {
      if (selectedItem != null) {
        if (selectedItem.equals("Program")) {
          // Show the main program instructions
          System.out.println("DEBUG: onProgramFunctionSelected displaying original program");
          isShowingFunction = false;
          currentFunctionName = null;
          instructionTable.displayProgram(sProgram);
        } else {
          // Show function instructions
          System.out.println("DEBUG: onProgramFunctionSelected displaying function: " + selectedItem);
          isShowingFunction = true;
          currentFunctionName = selectedItem;
          displayFunctionInstructions(selectedItem);
        }
      }
    }
  }

  // Method to display function instructions in the instruction table
  private void displayFunctionInstructions(String functionName) {
    if (sProgram instanceof SProgramImpl) {
      SProgramImpl programImpl = (SProgramImpl) sProgram;
      var functions = programImpl.getFunctions();

      if (functions.containsKey(functionName)) {
        var functionInstructions = functions.get(functionName);

        // Create a temporary program with just the function instructions
        SProgram functionProgram = new SProgram() {
          @Override
          public String getName() {
            return functionName;
          }

          @Override
          public void addInstruction(semulator.instructions.SInstruction instruction) {
            // Not needed for display
          }

          @Override
          public java.util.List<semulator.instructions.SInstruction> getInstructions() {
            return functionInstructions;
          }

          @Override
          public String validate(java.nio.file.Path xmlPath) {
            return null; // Not needed for display
          }

          @Override
          public int calculateMaxDegree() {
            return 0; // Functions are typically basic
          }

          @Override
          public int calculateCycles() {
            return 0; // Not needed for display
          }

          @Override
          public semulator.program.ExpansionResult expandToDegree(int degree) {
            return null; // Not needed for display
          }

          @Override
          public Object load()
              throws javax.xml.parsers.ParserConfigurationException, java.io.IOException, org.xml.sax.SAXException {
            return null; // Not needed for display
          }
        };

        instructionTable.displayProgram(functionProgram);
      }
    }
  }

  // Method to populate the program/function selector
  private void populateProgramFunctionSelector() {
    if (programFunctionSelector == null || sProgram == null) {
      System.out.println("DEBUG: populateProgramFunctionSelector - programFunctionSelector or sProgram is null");
      return;
    }

    ObservableList<String> items = FXCollections.observableArrayList();

    // Always add "Program" as the first option
    items.add("Program");

    // Add function names if available
    if (sProgram instanceof SProgramImpl) {
      SProgramImpl programImpl = (SProgramImpl) sProgram;
      var functions = programImpl.getFunctions();
      System.out.println("DEBUG: Found " + functions.size() + " functions: " + functions.keySet());
      if (functions != null && !functions.isEmpty()) {
        items.addAll(functions.keySet());
      }
    } else {
      System.out.println("DEBUG: sProgram is not an instance of SProgramImpl");
    }

    System.out.println("DEBUG: Populating dropdown with items: " + items);
    programFunctionSelector.setItems(items);

    // Set default selection to "Program"
    isProgrammaticallySettingSelection = true;
    programFunctionSelector.getSelectionModel().selectFirst();
    isProgrammaticallySettingSelection = false;

    // Reset function selection state
    isShowingFunction = false;
    currentFunctionName = null;

    // Enable the combo box if we have items
    programFunctionSelector.setDisable(items.isEmpty());
  }

  // Method to handle ComboBox selection
  @FXML
  private void onLabelVariableSelected(ActionEvent event) {
    String selectedItem = labelVariableComboBox.getSelectionModel().getSelectedItem();

    if (instructionTable != null) {
      if (selectedItem != null) {
        // Tell the instruction table to highlight rows containing this label/variable
        instructionTable.highlightRowsContaining(selectedItem);
      } else {
        // Clear highlighting if no item is selected
        instructionTable.clearHighlighting();
      }
    }
  }
}
