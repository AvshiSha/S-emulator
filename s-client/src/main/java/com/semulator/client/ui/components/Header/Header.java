package com.semulator.client.ui.components.Header;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.service.ApiClient;
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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

// Updated imports for new package structure
import com.semulator.engine.model.SProgram;
import com.semulator.engine.parse.SProgramImpl;
import com.semulator.engine.model.SInstruction;
import com.semulator.engine.model.ExpansionResult;
import com.semulator.engine.model.AssignVariableInstruction;
import com.semulator.engine.model.JumpEqualVariableInstruction;
import com.semulator.engine.model.QuoteInstruction;
import com.semulator.engine.model.JumpEqualFunctionInstruction;
import com.semulator.engine.model.FunctionArgument;
import com.semulator.engine.model.FunctionCall;

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
  private ComboBox<String> levelSelector;

  @FXML
  private Label lblDegreeStatus;

  @FXML
  private ComboBox<String> programFunctionSelector;

  @FXML
  private ComboBox<String> labelVariableComboBox;

  private SProgram sProgram;
  private Path loadedXmlPath;
  private com.semulator.client.ui.components.InstructionTable.InstructionTable instructionTable;
  private com.semulator.client.ui.components.HistoryStats.HistoryStats historyStats;
  private com.semulator.client.ui.components.DebuggerExecution.DebuggerExecution debuggerExecution;

  // Track current selection state for program/function selector
  private boolean isShowingFunction = false;
  private String currentFunctionName = null;
  private SProgram currentFunctionProgram = null; // Store the current function program
  private boolean isProgrammaticallySettingSelection = false;
  private boolean isProgrammaticallySettingLevelSelection = false;
  private int currentDegree = 0;
  private int maxDegree = 0;

  // For tracking labels and variables
  private ObservableList<String> labelVariableList = FXCollections.observableArrayList();

  // For tracking level selector options
  private ObservableList<String> levelOptions = FXCollections.observableArrayList();

  // Store expansion results for history chain tracking
  private ExpansionResult currentExpansionResult;
  private java.util.Map<Integer, ExpansionResult> expansionResultsByDegree = new java.util.HashMap<>();

  // API Client for HTTP communication
  private ApiClient apiClient;

  public Header() {
    this.sProgram = new SProgramImpl("S");
  }

  @FXML
  private void initialize() {
    // Initialize UI controls
    setupUI();
  }

  private void setupUI() {
    // Initialize combo boxes
    if (levelSelector != null) {
      levelSelector.setItems(FXCollections.observableArrayList());
      levelSelector.setDisable(true);
    }

    if (programFunctionSelector != null) {
      programFunctionSelector.setItems(FXCollections.observableArrayList());
      programFunctionSelector.setDisable(true);
    }

    if (labelVariableComboBox != null) {
      labelVariableComboBox.setItems(FXCollections.observableArrayList());
      labelVariableComboBox.setDisable(true);
    }

    // Initialize degree status
    if (lblDegreeStatus != null) {
      lblDegreeStatus.setText("— / —");
    }

    // Initialize file path field
    if (filePathField != null) {
      filePathField.setPromptText("Currently Loaded File path");
    }

    // Initialize progress bar
    if (progressBar != null) {
      progressBar.setVisible(false);
      progressBar.setProgress(0.0);
    }

    // Initialize progress label
    if (progressLabel != null) {
      progressLabel.setVisible(false);
      progressLabel.setText("Loading...");
    }
  }

  public void initializeWithHttp() {
    // Initialize API client (for future server communication like runs, history,
    // etc.)
    this.apiClient = AppContext.getInstance().getApiClient();

    // Initialize degree controls to disabled state
    updateDegreeDisplay();
    updateLevelSelectorState();

    // Initialize the label/variable combo box
    labelVariableComboBox.setItems(labelVariableList);
    labelVariableComboBox.setDisable(true); // Disabled until a program is loaded

    // Initialize the level selector
    levelSelector.setItems(levelOptions);
    levelSelector.setDisable(true); // Disabled until a program is loaded

    // Note: We don't load programs/functions from server anymore
    // The client works exactly like Exercise-2 - loads XML files locally
    // Server is only used for runs, history, and user management
  }

  private void loadProgramsFromServer() {
    if (apiClient == null)
      return;

    apiClient.get("/programs", ApiModels.DeltaResponse.class, null)
        .thenAccept(response -> {
          Platform.runLater(() -> {
            // Populate programFunctionSelector with programs
            ObservableList<String> programItems = FXCollections.observableArrayList();
            programItems.add("Main Program");

            // Cast the response items to ProgramInfo
            if (response.items() != null) {
              for (Object item : response.items()) {
                // Convert the item to a map (Gson deserializes to Map by default)
                if (item instanceof Map) {
                  Map<?, ?> programMap = (Map<?, ?>) item;
                  String programName = (String) programMap.get("name");
                  if (programName != null) {
                    programItems.add(programName);
                  }
                }
              }
            }

            programFunctionSelector.setItems(programItems);
            programFunctionSelector.setDisable(false);
            programFunctionSelector.getSelectionModel().selectFirst();

            // If we have programs, load the first one's instructions
            if (response.items() != null && !response.items().isEmpty()) {
              Object firstProgram = response.items().get(0);
              if (firstProgram instanceof Map) {
                Map<?, ?> programMap = (Map<?, ?>) firstProgram;
                String programName = (String) programMap.get("name");
                if (programName != null) {
                  loadProgramInstructions(programName);
                }
              }
            }
          });
        })
        .exceptionally(throwable -> {
          Platform.runLater(() -> {
            System.err.println("Failed to load programs: " + throwable.getMessage());
            throwable.printStackTrace();
          });
          return null;
        });
  }

  private void loadFunctionsFromServer() {
    if (apiClient == null)
      return;

    apiClient.get("/functions", ApiModels.DeltaResponse.class, null)
        .thenAccept(response -> {
          Platform.runLater(() -> {
            // TODO: Populate programFunctionSelector with functions
          });
        })
        .exceptionally(throwable -> {
          Platform.runLater(() -> {
            System.err.println("Failed to load functions: " + throwable.getMessage());
            throwable.printStackTrace();
          });
          return null;
        });
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

      // Load file locally using Exercise-2 logic (same as original)
      loadFileLocally(selectedFile);
    }
  }

  private void loadFileLocally(File selectedFile) {
    // Use the same logic as Exercise-2 - load XML locally and parse with engine
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

  private void uploadFileToServer(File selectedFile) {
    if (apiClient == null) {
      showErrorAlert("Error", "API client not initialized");
      resetUploadUI();
      return;
    }

    try {
      // Read file content
      String fileContent = Files.readString(selectedFile.toPath());

      // Debug: Log the first 200 characters of the file content

      // Create upload request
      com.semulator.client.model.ApiModels.UploadRequest uploadRequest = new com.semulator.client.model.ApiModels.UploadRequest(
          selectedFile.getName(),
          fileContent);

      // Upload to server
      apiClient.post("/upload", uploadRequest, ApiModels.LoadResult.class)
          .thenAccept(response -> {
            Platform.runLater(() -> {
              if (response.success()) {
                // File uploaded successfully
                loadedXmlPath = selectedFile.toPath();
                filePathField.setText(loadedXmlPath.toString());

                // Load programs and functions from server
                loadProgramsFromServer();
                loadFunctionsFromServer();

                showSuccessAlert("File Uploaded", "XML file uploaded successfully!");
              } else {
                showErrorAlert("Upload Failed", response.message());
              }
              resetUploadUI();
            });
          })
          .exceptionally(throwable -> {
            Platform.runLater(() -> {
              showErrorAlert("Upload Error", "Failed to upload file: " + throwable.getMessage());
              resetUploadUI();
            });
            return null;
          });

    } catch (Exception e) {
      Platform.runLater(() -> {
        showErrorAlert("Error", "Failed to read file: " + e.getMessage());
        resetUploadUI();
      });
    }
  }

  private void resetUploadUI() {
    loadFileButton.setDisable(false);
    progressBar.setVisible(false);
    progressLabel.setVisible(false);
    progressBar.setProgress(0.0);
  }

  private void loadProgramInstructions(String programName) {
    if (apiClient == null)
      return;

    // Fetch the full program with instructions from the server using query
    // parameter
    apiClient.get("/programs?name=" + programName, ApiModels.ProgramWithInstructions.class, null)
        .thenAccept(response -> {
          Platform.runLater(() -> {

            try {
              // Create a program with the real instructions from the server
              SProgramImpl program = new SProgramImpl(programName);

              // Convert the instruction DTOs from JSON to real SInstruction objects
              if (response.instructions() != null) {
                for (Object instructionObj : response.instructions()) {
                  if (instructionObj instanceof Map) {
                    Map<?, ?> instructionMap = (Map<?, ?>) instructionObj;
                    SInstruction realInstruction = convertDTOToInstruction(instructionMap);
                    if (realInstruction != null) {
                      program.addInstruction(realInstruction);
                    }
                  }
                }
              }

              // Display in instruction table
              if (instructionTable != null) {
                instructionTable.displayProgram(program);
              }

              // Update other components
              if (historyStats != null) {
                historyStats.setProgram(program);
              }
              if (debuggerExecution != null) {
                debuggerExecution.setProgram(program);
              }

            } catch (Exception e) {
              System.err.println("Failed to create program with real instructions: " + e.getMessage());
              e.printStackTrace();
            }
          });
        })
        .exceptionally(throwable -> {
          Platform.runLater(() -> {
            System.err.println("Failed to load program instructions: " + throwable.getMessage());
            throwable.printStackTrace();
          });
          return null;
        });
  }

  private SInstruction convertDTOToInstruction(Map<?, ?> instructionMap) {
    try {
      String name = (String) instructionMap.get("name");
      if (name == null) {
        return null;
      }

      // Create a real instruction based on the type
      // For now, create a basic instruction that matches the original Exercise-2
      // pattern
      return new SInstruction() {
        @Override
        public String getName() {
          return name;
        }

        @Override
        public com.semulator.engine.model.Label execute(com.semulator.engine.exec.ExecutionContext context) {
          return null;
        }

        @Override
        public int cycles() {
          Object cyclesObj = instructionMap.get("cycles");
          if (cyclesObj instanceof Integer) {
            return (Integer) cyclesObj;
          } else if (cyclesObj instanceof Double) {
            return ((Double) cyclesObj).intValue();
          }
          return 1;
        }

        @Override
        public com.semulator.engine.model.Label getLabel() {
          Object labelObj = instructionMap.get("label");
          if (labelObj instanceof Map) {
            Map<?, ?> labelMap = (Map<?, ?>) labelObj;
            String labelName = (String) labelMap.get("label");
            Boolean isExit = (Boolean) labelMap.get("exit");
            if (isExit != null && isExit) {
              return com.semulator.engine.model.FixedLabel.EXIT;
            } else if (labelName != null && !labelName.isEmpty()) {
              // For regular labels, we need to create a LabelImpl
              // For now, return EMPTY since we don't have the address
              return com.semulator.engine.model.FixedLabel.EMPTY;
            }
          }
          return com.semulator.engine.model.FixedLabel.EMPTY;
        }

        @Override
        public com.semulator.engine.model.Variable getVariable() {
          Object variableObj = instructionMap.get("variable");
          if (variableObj instanceof String) {
            return com.semulator.engine.model.Variable.of((String) variableObj);
          } else if (variableObj instanceof Map) {
            Map<?, ?> variableMap = (Map<?, ?>) variableObj;
            String representation = (String) variableMap.get("representation");
            if (representation != null) {
              return com.semulator.engine.model.Variable.of(representation);
            }
          }
          return null;
        }
      };
    } catch (Exception e) {
      System.err.println("Error converting instruction map: " + e.getMessage());
      return null;
    }
  }

  private SInstruction createDummyInstruction(String name, String variable, String other) {
    return new SInstruction() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public com.semulator.engine.model.Label execute(com.semulator.engine.exec.ExecutionContext context) {
        return null;
      }

      @Override
      public int cycles() {
        return 1;
      }

      @Override
      public com.semulator.engine.model.Label getLabel() {
        return null;
      }

      @Override
      public com.semulator.engine.model.Variable getVariable() {
        return variable != null ? com.semulator.engine.model.Variable.of(variable) : null;
      }
    };
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
  public void setInstructionTable(
      com.semulator.client.ui.components.InstructionTable.InstructionTable instructionTable) {
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
    updateDegreeControlsForProgram(sProgram);
  }

  // Method to update degree controls for any program (main or function)
  private void updateDegreeControlsForProgram(SProgram program) {
    try {
      currentDegree = 0;
      maxDegree = program.calculateMaxDegree();

      // Clear any cached expansion results from previous programs
      expansionResultsByDegree.clear();

      // Store the base program (degree 0) expansion result
      currentExpansionResult = program.expandToDegree(0);
      expansionResultsByDegree.put(0, currentExpansionResult);

      // Ensure the debugger execution component gets the current program
      if (debuggerExecution != null) {
        debuggerExecution.setProgram(program);
      }

      // Populate level selector with options from 0 to maxDegree
      populateLevelSelector();

      updateDegreeDisplay();
      updateLevelSelectorState();

    } catch (Exception e) {
      showErrorAlert("Degree Calculation Error", "Failed to calculate maximum degree: " + e.getMessage());
      currentDegree = 0;
      maxDegree = 0;
      updateDegreeDisplay();
      updateLevelSelectorState();
    }
  }

  @FXML
  public void onLevelSelected(ActionEvent event) {
    // isProgrammaticallySettingLevelSelection: "
    // + isProgrammaticallySettingLevelSelection);

    // Skip if we're programmatically setting the selection
    if (isProgrammaticallySettingLevelSelection) {
      return;
    }

    String selectedLevel = levelSelector.getSelectionModel().getSelectedItem();

    if (selectedLevel != null) {
      try {
        // Extract the level number from the selection (e.g., "Level 0" -> 0)
        int selectedDegree = Integer.parseInt(selectedLevel.replace("Level ", ""));

        if (selectedDegree != currentDegree) {
          expandToDegree(selectedDegree);
        }
      } catch (NumberFormatException e) {
        showErrorAlert("Invalid Selection", "Invalid level selection: " + selectedLevel);
      }
    }
  }

  private void expandToDegree(int degree) {
    try {
      // DEBUG: Run expansion directly instead of using Task

      // Determine which program to expand based on current selection
      SProgram programToExpand;
      ExpansionResult result;

      if (isShowingFunction && currentFunctionName != null) {
        // Expand the currently selected function using the function expansion method
        String internalFunctionName = getInternalFunctionName(currentFunctionName);
        result = sProgram.expandFunctionToDegree(internalFunctionName, degree);
        programToExpand = sProgram; // Use main program for UI updates
      } else {
        // Expand the main program
        programToExpand = sProgram;
        result = programToExpand.expandToDegree(degree);
      }

      // Store reference to the program being expanded for later use
      SProgram activeProgram = programToExpand;

      // Update UI directly (no Platform.runLater needed)
      try {
        // Store the expansion result for history chain tracking
        currentExpansionResult = result;
        expansionResultsByDegree.put(degree, result);

        // Create a temporary SProgram to hold the expanded instructions
        SProgramImpl expandedProgram = new SProgramImpl("Expanded");
        for (SInstruction instruction : result.instructions()) {
          expandedProgram.addInstruction(instruction);
        }

        // Copy functions and user-strings from original program to expanded program
        if (sProgram instanceof SProgramImpl) {
          SProgramImpl originalProgram = (SProgramImpl) sProgram;
          var originalFunctions = originalProgram.getFunctions();
          for (Map.Entry<String, java.util.List<SInstruction>> entry : originalFunctions
              .entrySet()) {
            expandedProgram.getFunctions().put(entry.getKey(), entry.getValue());
          }

          // Copy user-string mappings
          var originalUserStrings = originalProgram.getFunctionUserStrings();
          for (Map.Entry<String, String> entry : originalUserStrings.entrySet()) {
            expandedProgram.getFunctionUserStrings().put(entry.getKey(), entry.getValue());
          }
        }

        // Display the expanded program with comprehensive error handling
        if (instructionTable != null) {
          // Use Platform.runLater to ensure UI updates happen on the JavaFX thread
          javafx.application.Platform.runLater(() -> {
            try {
              // Add comprehensive error handling and validation
              if (expandedProgram == null) {
                System.err.println("Error: Expanded program is null, skipping UI update");
                return;
              }

              List<SInstruction> instructions = expandedProgram.getInstructions();
              if (instructions == null || instructions.isEmpty()) {
                System.err.println("Error: Expanded program has no instructions, skipping UI update");
                return;
              }

              // Additional validation for large datasets
              if (instructions.size() > 2000) {
                System.err.println("Warning: Very large instruction set (" + instructions.size() +
                    " instructions) may cause UI performance issues");
              }

              // CRITICAL FIX: Completely disable the table before updating to prevent
              // IndexOutOfBoundsException
              try {
                instructionTable.setTableEnabled(false);
              } catch (Exception disableException) {
                System.err.println("Error disabling table: " + disableException.getMessage());
              }

              // Update the instruction table with comprehensive error handling
              instructionTable.displayProgram(expandedProgram);

              // CRITICAL FIX: Re-enable the table after updating
              try {
                instructionTable.setTableEnabled(true);
              } catch (Exception enableException) {
                System.err.println("Error re-enabling table: " + enableException.getMessage());
              }

            } catch (Exception e) {
              System.err.println("Critical error updating instruction table: " + e.getMessage());
              e.printStackTrace();

              // Try to recover by clearing the table
              try {
                instructionTable.clearTable();
              } catch (Exception clearException) {
                System.err.println("Error clearing table during recovery: " + clearException.getMessage());
              }
            }
          });
        } else {
          System.err.println("Warning: Instruction table is null, cannot update UI");
        }

        // Handle program/function selector - preserve current selection during
        // expansion
        // Update the debugger execution component with the appropriate program

        if (debuggerExecution != null) {
          if (degree == 0) {
            // For degree 0, use the original active program
            debuggerExecution.setProgram(activeProgram);
          } else {
            // For higher degrees, use the expanded program
            debuggerExecution.setProgram(expandedProgram);
          }
        }

        // Update the label/variable combo box with the current program
        // For degree 0, use original active program; for higher degrees, use expanded
        // program
        if (degree == 0) {
          populateLabelVariableComboBox(activeProgram);
        } else {
          populateLabelVariableComboBox(expandedProgram);
        }

        // Update current degree and max degree for the active program
        currentDegree = degree;
        if (isShowingFunction && currentFunctionName != null) {
          // For function expansions, calculate max degree for the specific function
          String internalFunctionName = getInternalFunctionName(currentFunctionName);
          maxDegree = sProgram.calculateFunctionTemplateDegree(internalFunctionName);
        } else {
          // For main program expansions, use the main program's max degree
          // CRITICAL FIX: Ensure maxDegree is at least as high as the current degree
          int calculatedMaxDegree = activeProgram.calculateMaxDegree();

          // CRITICAL FIX: Always ensure maxDegree is at least as high as the degree we're
          // expanding to
          maxDegree = Math.max(calculatedMaxDegree, degree);

          // Additional safety: if we're expanding to degree 4 or higher, maxDegree should
          // be at least that
          if (degree >= 4) {
            maxDegree = Math.max(maxDegree, degree);
          }

          // Final safety check: maxDegree should never be less than currentDegree
          maxDegree = Math.max(maxDegree, currentDegree);
        }

        // Update level selector options with new max degree
        populateLevelSelector();

        // Update degree display and level selector state
        updateDegreeDisplay();
        updateLevelSelectorState();
      } catch (Exception e) {

        e.printStackTrace();
        showErrorAlert("Expansion Error", "Failed to display expanded program: " + e.getMessage());
        // Revert to previous degree
        if (degree > currentDegree) {
          currentDegree--;
        } else if (degree < currentDegree) {
          currentDegree++;
        }
        updateDegreeDisplay();
        updateLevelSelectorState();
      }

    } catch (Exception e) {
      e.printStackTrace();
      showErrorAlert("Expansion Error", "Failed to expand to degree " + degree + ": " + e.getMessage());
      // Revert to previous degree
      if (degree > currentDegree) {
        currentDegree--;
      } else if (degree < currentDegree) {
        currentDegree++;
      }
      updateDegreeDisplay();
      updateLevelSelectorState();
    }
  }

  private void updateDegreeDisplay() {
    if (maxDegree == 0) {
      lblDegreeStatus.setText("— / —");
    } else {
      // CRITICAL FIX: Ensure maxDegree is at least as high as currentDegree
      if (maxDegree < currentDegree) {
        maxDegree = currentDegree;
      }

      // Ensure currentDegree is not higher than maxDegree
      int displayDegree = Math.min(currentDegree, maxDegree);
      lblDegreeStatus.setText(displayDegree + " / " + maxDegree);

    }
  }

  private void updateLevelSelectorState() {
    if (maxDegree == 0) {
      levelSelector.setDisable(true);
    } else {
      levelSelector.setDisable(false);
      // Update the selection to reflect current degree
      updateLevelSelectorSelection();
    }
  }

  private void populateLevelSelector() {
    levelOptions.clear();

    if (maxDegree >= 0) {
      for (int i = 0; i <= maxDegree; i++) {
        levelOptions.add("Level " + i);
      }
    }

    levelSelector.setItems(levelOptions);
    updateLevelSelectorSelection();
  }

  private void updateLevelSelectorSelection() {
    if (levelSelector != null && !levelOptions.isEmpty()) {
      isProgrammaticallySettingLevelSelection = true;
      try {
        String currentLevelText = "Level " + currentDegree;
        levelSelector.getSelectionModel().select(currentLevelText);
      } finally {
        isProgrammaticallySettingLevelSelection = false;
      }
    }
  }

  // Method to disable/enable expansion controls during debug execution
  public void setExpansionControlsEnabled(boolean enabled) {
    levelSelector.setDisable(!enabled);
  }

  // Method to get the history chain for a selected instruction
  public java.util.List<SInstruction> getHistoryChain(
      SInstruction selectedInstruction) {
    java.util.List<SInstruction> chain = new java.util.ArrayList<>();

    if (selectedInstruction == null || currentExpansionResult == null) {
      return chain;
    }

    // Start with the selected instruction
    chain.add(selectedInstruction);

    // Trace back through the parent chain
    SInstruction current = selectedInstruction;
    int currentDegreeForTracing = currentDegree;

    // Use a local variable for the expansion result to avoid modifying class state
    ExpansionResult tracingExpansionResult = currentExpansionResult;

    while (currentDegreeForTracing > 0) {

      // Get the parent of the current instruction from the current expansion result
      // First try exact object identity match
      SInstruction parent = tracingExpansionResult.parent().get(current);

      // If no exact match, try matching by name and variable
      if (parent == null) {

        // Look for a parent that has the same name and variable but different object
        // identity
        // The parent map structure is: child -> parent, so we need to look for the
        // current instruction as a KEY
        for (java.util.Map.Entry<SInstruction, SInstruction> entry : tracingExpansionResult
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
      ExpansionResult prevDegreeResult = expansionResultsByDegree.get(currentDegreeForTracing);
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
  public void setDebuggerExecution(
      com.semulator.client.ui.components.DebuggerExecution.DebuggerExecution debuggerExecution) {
    this.debuggerExecution = debuggerExecution;
  }

  // Method to set the history stats component
  public void setHistoryStats(com.semulator.client.ui.components.HistoryStats.HistoryStats historyStats) {
    this.historyStats = historyStats;
    if (historyStats != null && sProgram != null) {
      historyStats.setProgram(sProgram);
    }
  }

  // Method to populate the label/variable combo box
  private void populateLabelVariableComboBox() {
    // Determine which program to use based on current degree
    SProgram programToUse;
    if (currentDegree == 0) {
      programToUse = sProgram; // Use original program for degree 0
    } else {
      // For higher degrees, use the expanded program from current expansion result
      if (currentExpansionResult != null) {
        // Create a temporary program from the current expansion result
        SProgramImpl expandedProgram = new SProgramImpl("Expanded");
        for (SInstruction instruction : currentExpansionResult.instructions()) {
          expandedProgram.addInstruction(instruction);
        }

        // Copy functions and user-strings from original program to expanded program
        if (sProgram instanceof SProgramImpl) {
          SProgramImpl originalProgram = (SProgramImpl) sProgram;
          var originalFunctions = originalProgram.getFunctions();
          for (Map.Entry<String, java.util.List<SInstruction>> entry : originalFunctions
              .entrySet()) {
            expandedProgram.getFunctions().put(entry.getKey(), entry.getValue());
          }

          // Copy user-string mappings
          var originalUserStrings = originalProgram.getFunctionUserStrings();
          for (Map.Entry<String, String> entry : originalUserStrings.entrySet()) {
            expandedProgram.getFunctionUserStrings().put(entry.getKey(), entry.getValue());
          }
        }
        programToUse = expandedProgram;
      } else {
        programToUse = sProgram; // Fallback to original program
      }
    }

    populateLabelVariableComboBox(programToUse);
  }

  // Overloaded method to populate the combo box with a specific program
  private void populateLabelVariableComboBox(SProgram program) {
    labelVariableList.clear();

    if (program == null || program.getInstructions() == null) {
      labelVariableComboBox.setDisable(true);
      return;
    }

    java.util.Set<String> uniqueItems = new java.util.TreeSet<>();

    // Extract labels and variables from the specified program
    for (SInstruction instruction : program.getInstructions()) {
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
      if (instruction instanceof AssignVariableInstruction assignVar) {
        if (assignVar.getSource() != null) {
          uniqueItems.add(assignVar.getSource().toString());
        }
      }

      // Add other variables for comparisons
      if (instruction instanceof JumpEqualVariableInstruction jumpVar) {
        if (jumpVar.getOther() != null) {
          uniqueItems.add(jumpVar.getOther().toString());
        }
      }

      // Add function arguments for QUOTE and JUMP_EQUAL_FUNCTION
      if (instruction instanceof QuoteInstruction quote) {
        for (FunctionArgument arg : quote.getFunctionArguments()) {
          uniqueItems.add(arg.toString());
        }
      }
      if (instruction instanceof JumpEqualFunctionInstruction jumpEqualFunc) {
        for (FunctionArgument arg : jumpEqualFunc.getFunctionArguments()) {
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
  public void onProgramFunctionSelected(ActionEvent event) {
    // Skip if we're programmatically setting the selection
    if (isProgrammaticallySettingSelection) {
      return;
    }

    String selectedItem = programFunctionSelector.getSelectionModel().getSelectedItem();
    if (instructionTable != null && sProgram != null) {
      if (selectedItem != null) {
        if (selectedItem.equals("Main Program")) {
          // Show the main program instructions (either original or expanded based on
          // current degree)
          isShowingFunction = false;
          currentFunctionName = null;

          // Clear function program when switching back to main program
          currentFunctionProgram = null;

          // Always update degree controls for main program when switching back
          updateDegreeControlsForProgram(sProgram);

          if (currentDegree == 0) {
            // Show original program
            instructionTable.displayProgram(sProgram);
          } else {
            // Show expanded program for current degree
            displayExpandedProgram();
          }
        } else {
          // Show function instructions
          isShowingFunction = true;
          currentFunctionName = selectedItem;

          // Convert user-friendly name back to internal function name
          String internalFunctionName = getInternalFunctionName(selectedItem);
          displayFunctionInstructions(internalFunctionName);
        }
      }
    }
  }

  // Method to display the expanded program for the current degree
  private void displayExpandedProgram() {
    if (currentExpansionResult != null) {
      // Create a temporary program from the current expansion result
      SProgramImpl expandedProgram = new SProgramImpl("Expanded");
      for (SInstruction instruction : currentExpansionResult.instructions()) {
        expandedProgram.addInstruction(instruction);
      }

      // Copy functions and user-strings from original program to expanded program
      if (sProgram instanceof SProgramImpl) {
        SProgramImpl originalProgram = (SProgramImpl) sProgram;
        var originalFunctions = originalProgram.getFunctions();
        for (Map.Entry<String, java.util.List<SInstruction>> entry : originalFunctions
            .entrySet()) {
          expandedProgram.getFunctions().put(entry.getKey(), entry.getValue());
        }

        // Copy user-string mappings
        var originalUserStrings = originalProgram.getFunctionUserStrings();
        for (Map.Entry<String, String> entry : originalUserStrings.entrySet()) {
          expandedProgram.getFunctionUserStrings().put(entry.getKey(), entry.getValue());
        }
      }

      // Display the expanded program
      instructionTable.displayProgram(expandedProgram);
    } else {
      // Fallback to original program
      instructionTable.displayProgram(sProgram);
    }
  }

  // Method to get the current function program (if a function is selected)
  private SProgram getCurrentFunctionProgram() {
    if (!isShowingFunction || currentFunctionName == null) {
      return null;
    }

    // Return the stored function program if it exists and matches the current
    // function
    if (currentFunctionProgram != null && currentFunctionProgram.getName().equals(currentFunctionName)) {
      return currentFunctionProgram;
    }

    if (sProgram instanceof SProgramImpl) {
      SProgramImpl programImpl = (SProgramImpl) sProgram;
      var functions = programImpl.getFunctions();

      if (functions.containsKey(currentFunctionName)) {
        var functionInstructions = functions.get(currentFunctionName);

        // Create a full SProgramImpl instance for the function
        SProgramImpl functionProgram = new SProgramImpl(currentFunctionName);

        // Add all function instructions to the program
        for (SInstruction instruction : functionInstructions) {
          functionProgram.addInstruction(instruction);
        }

        // Copy all functions from the original program so the function can call other
        // functions
        var originalFunctions = programImpl.getFunctions();
        for (Map.Entry<String, java.util.List<SInstruction>> entry : originalFunctions
            .entrySet()) {
          functionProgram.getFunctions().put(entry.getKey(), entry.getValue());
        }

        // Store the function program for future use
        currentFunctionProgram = functionProgram;
        return functionProgram;
      }
    }

    return null;
  }

  // Method to display function instructions in the instruction table
  private void displayFunctionInstructions(String functionName) {
    if (sProgram instanceof SProgramImpl) {
      SProgramImpl programImpl = (SProgramImpl) sProgram;
      var functions = programImpl.getFunctions();

      if (functions.containsKey(functionName)) {
        var functionInstructions = functions.get(functionName);

        // Create a custom SProgramImpl that considers its own instructions for
        // degree calculation while keeping access to all functions
        SProgramImpl functionProgram = new SProgramImpl(functionName) {
          @Override
          public int calculateMaxDegree() {
            // For function programs, use the template degree calculation without +4
            // correction
            // This ensures consistency with calculateFunctionTemplateDegree()
            return calculateFunctionTemplateDegree(functionName);
          }
        };

        // Add all function instructions to the program
        for (SInstruction instruction : functionInstructions) {
          functionProgram.addInstruction(instruction);
        }

        // Copy all functions from the original program so the function can call other
        // functions
        var originalFunctions = programImpl.getFunctions();
        for (Map.Entry<String, java.util.List<SInstruction>> entry : originalFunctions
            .entrySet()) {
          functionProgram.getFunctions().put(entry.getKey(), entry.getValue());
        }

        // Store the function program for future use
        currentFunctionProgram = functionProgram;

        // Display the function program
        instructionTable.displayProgram(functionProgram);

        // Always update degree controls for the function program (resets max degree)
        updateDegreeControlsForProgram(functionProgram);

        // Update the label/variable combo box for the function
        populateLabelVariableComboBox(functionProgram);

      } else {
      }
    } else {
    }
  }

  // Method to populate the program/function selector
  private void populateProgramFunctionSelector() {
    if (programFunctionSelector == null || sProgram == null) {
      return;
    }

    ObservableList<String> items = FXCollections.observableArrayList();

    // Always add "Main Program" as the first option
    items.add("Main Program");

    // Add function names if available
    if (sProgram instanceof SProgramImpl) {
      SProgramImpl programImpl = (SProgramImpl) sProgram;
      var functions = programImpl.getFunctions();
      // Discover all functions that are referenced anywhere in the program
      Set<String> allReferencedFunctions = discoverAllReferencedFunctions(programImpl);

      if (!allReferencedFunctions.isEmpty()) {
        // Get user-strings for display
        Map<String, String> functionUserStrings = programImpl.getFunctionUserStrings();

        // Sort function names alphabetically for better user experience
        List<String> sortedFunctionNames = new ArrayList<>(allReferencedFunctions);
        Collections.sort(sortedFunctionNames);

        // Add display names (user-strings if available, otherwise function names)
        for (String functionName : sortedFunctionNames) {
          String displayName = functionUserStrings.containsKey(functionName)
              ? functionUserStrings.get(functionName)
              : functionName;
          items.add(displayName);
        }
      }
    } else {
    }

    programFunctionSelector.setItems(items);

    // Set default selection to "Main Program" (only if not programmatically
    // setting)
    isProgrammaticallySettingSelection = true;
    programFunctionSelector.getSelectionModel().selectFirst();
    isProgrammaticallySettingSelection = false;

    // Note: Function selection state is preserved during expansion

    // Enable the combo box and make it visible
    programFunctionSelector.setDisable(items.isEmpty());
    programFunctionSelector.setVisible(true);

    // Update the prompt text based on whether we have functions
    if (items.size() > 1) {
      programFunctionSelector.setPromptText("Select Program/Function (" + (items.size() - 1) + " functions)");
    } else {
      programFunctionSelector.setPromptText("Main Program Only");
    }
  }

  // Method to discover all functions referenced anywhere in the program
  private Set<String> discoverAllReferencedFunctions(SProgramImpl programImpl) {
    Set<String> referencedFunctions = new HashSet<>();

    // Add all directly defined functions
    referencedFunctions.addAll(programImpl.getFunctions().keySet());

    // Discover functions called in the main program
    discoverFunctionsInInstructions(programImpl.getInstructions(), referencedFunctions);

    // Discover functions called within other functions (recursive)
    for (List<SInstruction> functionBody : programImpl.getFunctions().values()) {
      discoverFunctionsInInstructions(functionBody, referencedFunctions);
    }

    return referencedFunctions;
  }

  // Helper method to discover functions called in a list of instructions
  private void discoverFunctionsInInstructions(List<SInstruction> instructions,
      Set<String> referencedFunctions) {
    for (SInstruction instruction : instructions) {
      if (instruction instanceof QuoteInstruction quote) {
        // Add the function being called
        referencedFunctions.add(quote.getFunctionName());

        // Also check for nested function calls in arguments
        for (FunctionArgument arg : quote.getFunctionArguments()) {
          if (arg.isFunctionCall()) {
            discoverFunctionsInFunctionCall(arg.asFunctionCall(), referencedFunctions);
          }
        }
      } else if (instruction instanceof JumpEqualFunctionInstruction jef) {
        // Add the function being called
        referencedFunctions.add(jef.getFunctionName());

        // Also check for nested function calls in arguments
        for (FunctionArgument arg : jef.getFunctionArguments()) {
          if (arg.isFunctionCall()) {
            discoverFunctionsInFunctionCall(arg.asFunctionCall(), referencedFunctions);
          }
        }
      }
    }
  }

  // Helper method to discover functions in nested function calls
  private void discoverFunctionsInFunctionCall(FunctionCall call,
      Set<String> referencedFunctions) {
    // Add the function being called
    referencedFunctions.add(call.getFunctionName());

    // Check for nested function calls in arguments
    for (FunctionArgument arg : call.getArguments()) {
      if (arg.isFunctionCall()) {
        discoverFunctionsInFunctionCall(arg.asFunctionCall(), referencedFunctions);
      }
    }
  }

  // Method to handle ComboBox selection
  @FXML
  public void onLabelVariableSelected(ActionEvent event) {
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

  // Helper method to convert user-friendly function name back to internal
  // function name
  private String getInternalFunctionName(String displayName) {
    if (sProgram instanceof SProgramImpl) {
      SProgramImpl programImpl = (SProgramImpl) sProgram;
      Map<String, String> functionUserStrings = programImpl.getFunctionUserStrings();

      // Look for the internal function name that has this display name
      for (Map.Entry<String, String> entry : functionUserStrings.entrySet()) {
        if (entry.getValue().equals(displayName)) {
          return entry.getKey(); // Return the internal function name
        }
      }
    }

    // If no user-string mapping found, assume it's already an internal name
    return displayName;
  }

}
