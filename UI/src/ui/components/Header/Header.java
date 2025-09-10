package ui.components.Header;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import semulator.program.SProgram;
import semulator.program.SProgramImpl;

import java.io.File;
import java.nio.file.Path;

public class Header {
  @FXML
  private Button loadFileButton;

  @FXML
  private TextField filePathField;

  @FXML
  private ProgressBar progressBar;

  private SProgram sProgram;
  private Path loadedXmlPath;

  public Header() {
    this.sProgram = new SProgramImpl("S");
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
          progressBar.progressProperty().unbind();
        }
      });

      // Handle task failure
      loadingTask.setOnFailed(e -> {
        showErrorAlert("Error", "Failed to load file: " + loadingTask.getException().getMessage());
        loadFileButton.setDisable(false);
        progressBar.setVisible(false);
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
          return new FileLoadResult(false, null, "Failed to load the XML file.");
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
}
