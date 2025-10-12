package com.semulator.client.ui;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.service.ApiClient;
import com.semulator.client.service.UserUpdateClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.ResourceBundle;
import java.util.Timer;

/**
 * New Client-Server Dashboard Controller
 * Provides real-time overview of all users, programs, functions, and system
 * status
 */
public class DashboardController implements Initializable {

    // Header Bar Components
    @FXML
    private HBox headerBar;
    @FXML
    private Label currentUserName;
    @FXML
    private Button loadFileButton;
    @FXML
    private TextField loadedFilePath;
    @FXML
    private Label availableCredits;
    @FXML
    private Button chargeCreditsButton;
    @FXML
    private TextField creditsInput;
    @FXML
    private Button logoutButton;

    // Available Users Table (Top-Left)
    @FXML
    private TableView<UserInfo> usersTable;
    @FXML
    private TableColumn<UserInfo, String> userNameColumn;
    @FXML
    private TableColumn<UserInfo, Integer> mainProgramsColumn;
    @FXML
    private TableColumn<UserInfo, Integer> subfunctionsColumn;
    @FXML
    private TableColumn<UserInfo, Integer> userCreditsColumn;
    @FXML
    private TableColumn<UserInfo, Integer> creditsUsedColumn;
    @FXML
    private TableColumn<UserInfo, Integer> runsColumn;
    @FXML
    private Button unselectUserButton;

    // Available Programs Table (Top-Right)
    @FXML
    private TableView<ProgramInfo> programsTable;
    @FXML
    private TableColumn<ProgramInfo, String> programNameColumn;
    @FXML
    private TableColumn<ProgramInfo, String> uploadedByColumn;
    @FXML
    private TableColumn<ProgramInfo, Integer> instructionCountColumn;
    @FXML
    private TableColumn<ProgramInfo, Integer> maxLevelColumn;
    @FXML
    private TableColumn<ProgramInfo, Integer> programRunsColumn;
    @FXML
    private TableColumn<ProgramInfo, Double> avgCostColumn;
    @FXML
    private Button executeProgramButton;

    // Users History / Statistics Table (Bottom-Left)
    @FXML
    private TableView<RunHistory> historyTable;
    @FXML
    private TableColumn<RunHistory, Integer> runNumberColumn;
    @FXML
    private TableColumn<RunHistory, String> typeColumn;
    @FXML
    private TableColumn<RunHistory, String> nameColumn;
    @FXML
    private TableColumn<RunHistory, String> architectureColumn;
    @FXML
    private TableColumn<RunHistory, Integer> levelColumn;
    @FXML
    private TableColumn<RunHistory, Long> yValueColumn;
    @FXML
    private TableColumn<RunHistory, Integer> cyclesColumn;
    @FXML
    private Label historyTableTitle;
    @FXML
    private Button showStatusButton;
    @FXML
    private Button reRunButton;

    // Available Functions Table (Bottom-Right)
    @FXML
    private TableView<FunctionInfo> functionsTable;
    @FXML
    private TableColumn<FunctionInfo, String> functionNameColumn;
    @FXML
    private TableColumn<FunctionInfo, String> parentProgramColumn;
    @FXML
    private TableColumn<FunctionInfo, String> functionUploadedByColumn;
    @FXML
    private TableColumn<FunctionInfo, Integer> functionInstructionCountColumn;
    @FXML
    private TableColumn<FunctionInfo, Integer> functionMaxLevelColumn;
    @FXML
    private Button executeFunctionButton;

    // Data Models
    private ObservableList<UserInfo> usersData = FXCollections.observableArrayList();
    private ObservableList<ProgramInfo> programsData = FXCollections.observableArrayList();
    private ObservableList<RunHistory> historyData = FXCollections.observableArrayList();
    private ObservableList<FunctionInfo> functionsData = FXCollections.observableArrayList();

    // State Management
    private ApiClient apiClient;
    private UserUpdateClient userUpdateClient;
    private String currentUser;
    private String selectedUser = null; // For history view
    private String selectedProgram = null;
    private String selectedFunction = null;
    private Timer refreshTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize API client and current user
        this.apiClient = AppContext.getInstance().getApiClient();
        this.currentUser = AppContext.getInstance().getCurrentUser();

        // Initialize user update socket client
        this.userUpdateClient = new UserUpdateClient(this);

        // Initialize UI components
        initializeHeaderBar();
        initializeUsersTable();
        initializeProgramsTable();
        initializeHistoryTable();
        initializeFunctionsTable();

        // Set up event handlers
        setupEventHandlers();

        // Start auto-refresh (disabled for better UX)
        // startAutoRefresh();

        // Set up window close handler
        setupWindowCloseHandler();

        // Load initial data
        loadInitialData();

        // Start auto-refresh
        startAutoRefresh();

        // Connect to user update socket
        userUpdateClient.connect();
    }

    private void initializeHeaderBar() {
        // Set current user name
        currentUserName.setText(currentUser != null ? currentUser : "Unknown User");

        // Initialize credits display
        availableCredits.setText("0");

        // Set up file chooser for load file button
        loadFileButton.setOnAction(e -> handleLoadFile());

        // Set up charge credits button
        chargeCreditsButton.setOnAction(e -> handleChargeCredits());

        // Set up logout button
        logoutButton.setOnAction(e -> handleLogout());
    }

    private void initializeUsersTable() {
        // Set up table columns
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        mainProgramsColumn.setCellValueFactory(new PropertyValueFactory<>("mainPrograms"));
        subfunctionsColumn.setCellValueFactory(new PropertyValueFactory<>("subfunctions"));
        userCreditsColumn.setCellValueFactory(new PropertyValueFactory<>("credits"));
        creditsUsedColumn.setCellValueFactory(new PropertyValueFactory<>("creditsUsed"));
        runsColumn.setCellValueFactory(new PropertyValueFactory<>("runs"));

        // Set table data
        usersTable.setItems(usersData);

        // Set up selection handler
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedUser = newSelection.getUserName();
                updateHistoryTableTitle();
                // Load history for the selected user
                loadUserHistory(selectedUser);
            }
        });
    }

    private void initializeProgramsTable() {
        // Set up table columns
        programNameColumn.setCellValueFactory(new PropertyValueFactory<>("programName"));
        uploadedByColumn.setCellValueFactory(new PropertyValueFactory<>("uploadedBy"));
        instructionCountColumn.setCellValueFactory(new PropertyValueFactory<>("instructionCount"));
        maxLevelColumn.setCellValueFactory(new PropertyValueFactory<>("maxLevel"));
        programRunsColumn.setCellValueFactory(new PropertyValueFactory<>("runs"));
        avgCostColumn.setCellValueFactory(new PropertyValueFactory<>("avgCost"));

        // Set table data
        programsTable.setItems(programsData);

        // Set up selection handler
        programsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedProgram = newSelection.getProgramName();
                executeProgramButton.setDisable(false);
            } else {
                selectedProgram = null;
                executeProgramButton.setDisable(true);
            }
        });

        // Initially disable execute button
        executeProgramButton.setDisable(true);
    }

    private void initializeHistoryTable() {
        // Set up table columns
        runNumberColumn.setCellValueFactory(new PropertyValueFactory<>("runNumber"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        architectureColumn.setCellValueFactory(new PropertyValueFactory<>("architecture"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        yValueColumn.setCellValueFactory(new PropertyValueFactory<>("yValue"));
        cyclesColumn.setCellValueFactory(new PropertyValueFactory<>("cycles"));

        // Set table data
        historyTable.setItems(historyData);

        // Initially disable action buttons
        showStatusButton.setDisable(true);
        reRunButton.setDisable(true);

        // Set up selection handler
        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            showStatusButton.setDisable(!hasSelection);
            reRunButton.setDisable(!hasSelection);
        });
    }

    private void initializeFunctionsTable() {
        // Set up table columns
        functionNameColumn.setCellValueFactory(new PropertyValueFactory<>("functionName"));
        parentProgramColumn.setCellValueFactory(new PropertyValueFactory<>("parentProgram"));
        functionUploadedByColumn.setCellValueFactory(new PropertyValueFactory<>("uploadedBy"));
        functionInstructionCountColumn.setCellValueFactory(new PropertyValueFactory<>("instructionCount"));
        functionMaxLevelColumn.setCellValueFactory(new PropertyValueFactory<>("maxLevel"));

        // Set table data
        functionsTable.setItems(functionsData);

        // Set up selection handler
        functionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedFunction = newSelection.getFunctionName();
                executeFunctionButton.setDisable(false);
            } else {
                selectedFunction = null;
                executeFunctionButton.setDisable(true);
            }
        });

        // Initially disable execute button
        executeFunctionButton.setDisable(true);
    }

    private void setupEventHandlers() {
        // Unselect User button
        unselectUserButton.setOnAction(e -> {
            selectedUser = null;
            usersTable.getSelectionModel().clearSelection();
            updateHistoryTableTitle();
            loadUserHistory(currentUser); // Load current user's history
        });

        // Execute Program button
        executeProgramButton.setOnAction(e -> {
            if (selectedProgram != null) {
                navigateToExecutionScreen(selectedProgram, "PROGRAM");
            }
        });

        // Execute Function button
        executeFunctionButton.setOnAction(e -> {
            if (selectedFunction != null) {
                navigateToExecutionScreen(selectedFunction, "FUNCTION");
            }
        });

        // Show Status button
        showStatusButton.setOnAction(e -> {
            RunHistory selectedRun = historyTable.getSelectionModel().getSelectedItem();
            if (selectedRun != null) {
                showRunStatus(selectedRun);
            }
        });

        // Re-Run button
        reRunButton.setOnAction(e -> {
            RunHistory selectedRun = historyTable.getSelectionModel().getSelectedItem();
            if (selectedRun != null) {
                reRunExecution(selectedRun);
            }
        });
    }

    private void handleLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select S-Program XML File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        Stage stage = (Stage) loadFileButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Read file content
                String xmlContent = java.nio.file.Files.readString(selectedFile.toPath());

                // Create upload request
                ApiModels.UploadRequest uploadRequest = new ApiModels.UploadRequest(
                        selectedFile.getName(),
                        xmlContent);

                // Upload to server
                apiClient.post("/upload", uploadRequest, ApiModels.LoadResult.class)
                        .thenAccept(response -> {
                            Platform.runLater(() -> {
                                if (response.success()) {
                                    loadedFilePath.setText(selectedFile.getAbsolutePath());
                                    showInfoAlert("Upload Successful",
                                            "Program '" + response.programName() + "' uploaded successfully!\n" +
                                                    "Instructions: " + response.instructionCount());

                                    // Refresh data to show new program
                                    refreshProgramsData();
                                    refreshFunctionsData();
                                } else {
                                    showErrorAlert("Upload Failed", response.message());
                                }
                            });
                        })
                        .exceptionally(throwable -> {
                            Platform.runLater(() -> {
                                showErrorAlert("Upload Failed",
                                        "Failed to upload file: " + throwable.getMessage());
                            });
                            return null;
                        });

            } catch (Exception e) {
                showErrorAlert("File Error", "Failed to read file: " + e.getMessage());
            }
        }
    }

    private void handleChargeCredits() {
        try {
            String amountText = creditsInput.getText().trim();
            if (amountText.isEmpty()) {
                showErrorAlert("Invalid Amount", "Please enter an amount to charge.");
                return;
            }

            int amount = Integer.parseInt(amountText);
            if (amount <= 0) {
                showErrorAlert("Invalid Amount", "Amount must be positive.");
                return;
            }

            // TODO: Implement credit charging via API
            showInfoAlert("Credits Charged",
                    "Added " + amount + " credits to your account.\nAPI integration will be implemented.");
            creditsInput.clear();

            // Refresh user data to show updated credits
            refreshUserData();

        } catch (NumberFormatException e) {
            showErrorAlert("Invalid Amount", "Please enter a valid number.");
        }
    }

    private void handleLogout() {
        if (apiClient != null) {
            // Call logout API
            apiClient.post("/auth/logout", null, String.class)
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            showInfoAlert("Logged Out", "You have been successfully logged out.");
                            // Close the application or return to login screen
                            System.exit(0);
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            showErrorAlert("Logout Error", "Failed to logout: " + throwable.getMessage());
                        });
                        return null;
                    });
        } else {
            showInfoAlert("Logged Out", "You have been logged out locally.");
            System.exit(0);
        }
    }

    private void setupWindowCloseHandler() {
        // Get the stage from any FXML element
        if (currentUserName != null && currentUserName.getScene() != null) {
            Stage stage = (Stage) currentUserName.getScene().getWindow();
            stage.setOnCloseRequest(this::handleWindowClose);
        }
    }

    private void handleWindowClose(WindowEvent event) {
        // Call logout API when window is closed
        if (apiClient != null) {
            try {
                // Make a synchronous logout call to ensure it completes before closing
                apiClient.post("/auth/logout", null, String.class)
                        .thenAccept(response -> {
                            System.out.println("User logged out successfully on window close");
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Failed to logout on window close: " + throwable.getMessage());
                            return null;
                        });

                // Small delay to allow the logout request to complete
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                System.err.println("Error during logout on window close: " + e.getMessage());
            }
        }

        // Clean up resources
        cleanup();

        // Allow the window to close
        event.consume(); // Don't prevent the close, just handle cleanup
    }

    private void startAutoRefresh() {
        // History updates are now handled via WebSocket broadcasts
        // No polling needed - real-time updates for users, programs, functions, and
        // history
        System.out.println("Auto-refresh disabled - using WebSocket for real-time updates");
    }

    private void loadInitialData() {
        refreshUserData();
        refreshProgramsData();
        refreshFunctionsData();
        loadUserHistory(currentUser);
        updateHistoryTableTitle();
    }

    private void refreshUserData() {
        if (apiClient == null) {
            return;
        }

        apiClient.get("/auth/users", ApiModels.UsersResponse.class, null)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        usersData.clear();

                        // Convert server UserInfo to local UserInfo model
                        for (ApiModels.UserInfo serverUser : response.users()) {
                            UserInfo localUser = new UserInfo(
                                    serverUser.username(),
                                    serverUser.mainPrograms(),
                                    serverUser.subfunctions(),
                                    serverUser.credits(),
                                    serverUser.creditsUsed(),
                                    serverUser.totalRuns());
                            usersData.add(localUser);
                        }

                        // Update current user's credits in header
                        updateCurrentUserCredits();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        System.err.println("Failed to load users: " + throwable.getMessage());
                        // Fallback to sample data if API fails
                        usersData.clear();
                        usersData.add(new UserInfo(currentUser, 1, 2, 100, 75, 15));
                    });
                    return null;
                });
    }

    private void refreshProgramsData() {
        if (apiClient == null) {
            return;
        }

        apiClient.get("/programs", ApiModels.DeltaResponse.class, null)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        System.out.println("DEBUG: Received programs response: " + response);
                        programsData.clear();

                        // Convert server ProgramInfo to local ProgramInfo model
                        if (response.items() != null) {
                            System.out.println("DEBUG: Found " + response.items().size() + " programs");
                            for (Object item : response.items()) {
                                System.out.println("DEBUG: Processing item: " + item + " (type: "
                                        + item.getClass().getName() + ")");

                                // Handle LinkedTreeMap objects from Gson deserialization
                                if (item instanceof com.google.gson.internal.LinkedTreeMap) {
                                    @SuppressWarnings("unchecked")
                                    com.google.gson.internal.LinkedTreeMap<String, Object> map = (com.google.gson.internal.LinkedTreeMap<String, Object>) item;

                                    String name = (String) map.get("name");
                                    String uploadedBy = (String) map.get("uploadedBy");
                                    int instructionCount = ((Double) map.get("instructionCount")).intValue();
                                    int maxDegree = ((Double) map.get("maxDegree")).intValue();
                                    int runs = ((Double) map.get("runs")).intValue();
                                    double avgCost = ((Double) map.get("avgCost")).doubleValue();

                                    System.out.println("DEBUG: Creating local program: " + name);
                                    ProgramInfo localProgram = new ProgramInfo(
                                            name, uploadedBy, instructionCount, maxDegree, runs, avgCost);
                                    programsData.add(localProgram);
                                }
                            }
                        } else {
                            System.out.println("DEBUG: No items in response");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        System.err.println("Failed to load programs: " + throwable.getMessage());
                        // Fallback to sample data if API fails
                        programsData.clear();
                        programsData.add(new ProgramInfo("fibonacci", "user1", 15, 4, 25, 2.5));
                        programsData.add(new ProgramInfo("factorial", "user2", 8, 3, 18, 1.8));
                        programsData.add(new ProgramInfo("gcd", currentUser, 12, 5, 30, 3.2));
                    });
                    return null;
                });
    }

    private void refreshFunctionsData() {
        if (apiClient == null) {
            return;
        }

        apiClient.get("/functions", ApiModels.DeltaResponse.class, null)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        System.out.println("DEBUG: Received functions response: " + response);
                        functionsData.clear();

                        // Convert server FunctionInfo to local FunctionInfo model
                        if (response.items() != null) {
                            System.out.println("DEBUG: Found " + response.items().size() + " functions");
                            for (Object item : response.items()) {
                                System.out.println("DEBUG: Processing function item: " + item + " (type: "
                                        + item.getClass().getName() + ")");

                                // Handle LinkedTreeMap objects from Gson deserialization
                                if (item instanceof com.google.gson.internal.LinkedTreeMap) {
                                    @SuppressWarnings("unchecked")
                                    com.google.gson.internal.LinkedTreeMap<String, Object> map = (com.google.gson.internal.LinkedTreeMap<String, Object>) item;

                                    String name = (String) map.get("name");
                                    String parentProgram = (String) map.get("parentProgram");
                                    String uploadedBy = (String) map.get("uploadedBy");
                                    int instructionCount = ((Double) map.get("instructionCount")).intValue();
                                    int maxDegree = ((Double) map.get("maxDegree")).intValue();

                                    System.out.println("DEBUG: Creating local function: " + name);
                                    FunctionInfo localFunction = new FunctionInfo(
                                            name, parentProgram, uploadedBy, instructionCount, maxDegree);
                                    functionsData.add(localFunction);
                                }
                            }
                        } else {
                            System.out.println("DEBUG: No function items in response");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        System.err.println("Failed to load functions: " + throwable.getMessage());
                        // Fallback to sample data if API fails
                        functionsData.clear();
                        functionsData.add(new FunctionInfo("add", "fibonacci", "user1", 5, 2));
                        functionsData.add(new FunctionInfo("multiply", "factorial", "user2", 7, 3));
                        functionsData.add(new FunctionInfo("subtract", "gcd", currentUser, 4, 1));
                    });
                    return null;
                });
    }

    private void loadUserHistory(String userName) {
        if (apiClient == null || userName == null) {
            return;
        }

        // Fetch user history from server
        String endpoint = "/history?user=" + userName;

        apiClient.get(endpoint, ApiModels.HistoryListResponse.class)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        historyData.clear();

                        if (response.items() != null) {
                            int runNumber = 1;
                            for (ApiModels.HistoryEntry entry : response.items()) {
                                // Map architecture number to readable name
                                String archName = getArchitectureName(entry.architecture());

                                // Create RunHistory object
                                RunHistory history = new RunHistory(
                                        runNumber++,
                                        entry.runId(),
                                        entry.targetType(),
                                        entry.targetName(),
                                        archName,
                                        entry.degree(),
                                        entry.finalYValue(),
                                        entry.cycles(),
                                        entry.finalVariables());
                                historyData.add(history);
                            }
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        System.err.println("Failed to load user history: " + throwable.getMessage());
                        historyData.clear();
                    });
                    return null;
                });
    }

    private String getArchitectureName(String archCode) {
        switch (archCode) {
            case "I":
                return "Basic (I)";
            case "II":
                return "Enhanced (II)";
            case "III":
                return "Advanced (III)";
            case "IV":
                return "Expert (IV)";
            default:
                return archCode;
        }
    }

    private void updateHistoryTableTitle() {
        if (selectedUser != null && !selectedUser.equals(currentUser)) {
            historyTableTitle.setText("User History / Statistics Table (" + selectedUser + ")");
        } else {
            historyTableTitle.setText("User History / Statistics Table");
        }
    }

    private void updateCurrentUserCredits() {
        // Find current user in the users list and update credits display
        for (UserInfo user : usersData) {
            if (user.getUserName().equals(currentUser)) {
                availableCredits.setText(String.valueOf(user.getCredits()));
                break;
            }
        }
    }

    private void navigateToExecutionScreen(String targetName, String targetType) {
        try {
            // Check if the resource exists
            java.net.URL resourceUrl = getClass().getResource("/fxml/program-run.fxml");

            if (resourceUrl == null) {
                throw new IOException("FXML resource not found: /fxml/program-run.fxml");
            }

            // Load the program run screen
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            // Get the controller and set the target
            ProgramRunController controller = loader.getController();
            controller.setTarget(targetName, targetType);

            Stage stage = (Stage) executeProgramButton.getScene().getWindow();
            stage.setTitle("S-Emulator - Execution - " + targetName);
            stage.setScene(scene);
            stage.setResizable(true);

        } catch (IOException e) {
            System.err.println("Navigation error: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Failed to navigate to execution screen: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected navigation error: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Unexpected error: " + e.getMessage());
        }
    }

    private void showRunStatus(RunHistory run) {
        // Create a detailed status dialog showing all final variable values
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Run Status - Run #" + run.getRunNumber());
        alert.setHeaderText("Final Variable Values");

        // Build the content text with all variables
        StringBuilder content = new StringBuilder();
        content.append("Program/Function: ").append(run.getName()).append("\n");
        content.append("Type: ").append(run.getType()).append("\n");
        content.append("Architecture: ").append(run.getArchitecture()).append("\n");
        content.append("Level (Degree): ").append(run.getLevel()).append("\n");
        content.append("Total Cycles: ").append(run.getCycles()).append("\n\n");

        content.append("=== Variable Values ===\n");

        // Get final variables and display them in a sorted order
        Map<String, Long> variables = run.getFinalVariables();
        if (variables.isEmpty()) {
            content.append("No variable data available.\n");
        } else {
            // Sort variables: inputs (x), result (y), work variables (z)
            List<String> sortedKeys = new ArrayList<>(variables.keySet());
            sortedKeys.sort((a, b) -> {
                // Custom sort: x1, x2, ..., y, z1, z2, ...
                if (a.equals("y"))
                    return b.equals("y") ? 0 : (b.startsWith("x") ? 1 : -1);
                if (b.equals("y"))
                    return a.equals("y") ? 0 : (a.startsWith("x") ? 1 : -1);
                if (a.startsWith("x") && !b.startsWith("x"))
                    return -1;
                if (!a.startsWith("x") && b.startsWith("x"))
                    return 1;
                return a.compareTo(b);
            });

            for (String varName : sortedKeys) {
                Long value = variables.get(varName);
                content.append(String.format("%-6s = %d\n", varName, value));
            }
        }

        alert.setContentText(content.toString());

        // Make the dialog resizable and larger
        alert.setResizable(true);
        alert.getDialogPane().setPrefWidth(500);
        alert.getDialogPane().setPrefHeight(400);

        alert.showAndWait();
    }

    private void reRunExecution(RunHistory run) {
        try {
            // Determine the target type
            String targetType = "PROGRAM".equals(run.getType()) ? "PROGRAM" : "FUNCTION";

            // Navigate to execution screen with the target
            navigateToExecutionScreenWithContext(run.getName(), targetType, run.getLevel(), run.getFinalVariables());

        } catch (Exception e) {
            System.err.println("Re-run error: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Re-run Error", "Failed to re-run execution: " + e.getMessage());
        }
    }

    private void navigateToExecutionScreenWithContext(String targetName, String targetType, int degree,
            Map<String, Long> previousInputs) {
        try {
            // Check if the resource exists
            java.net.URL resourceUrl = getClass().getResource("/fxml/program-run.fxml");

            if (resourceUrl == null) {
                throw new IOException("FXML resource not found: /fxml/program-run.fxml");
            }

            // Load the program run screen
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            // Get the controller and set the target with context
            ProgramRunController controller = loader.getController();
            controller.setTarget(targetName, targetType);

            // Set the degree/level
            controller.setDegree(degree);

            // Extract and set input values (only x variables)
            Map<String, Long> inputs = new HashMap<>();
            for (Map.Entry<String, Long> entry : previousInputs.entrySet()) {
                if (entry.getKey().startsWith("x")) {
                    inputs.put(entry.getKey(), entry.getValue());
                }
            }
            controller.setInputs(inputs);

            Stage stage = (Stage) reRunButton.getScene().getWindow();
            stage.setTitle("S-Emulator - Re-Run - " + targetName);
            stage.setScene(scene);
            stage.setResizable(true);

        } catch (IOException e) {
            System.err.println("Navigation error: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Failed to navigate to execution screen: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected navigation error: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Navigation Error", "Unexpected error: " + e.getMessage());
        }
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Data Model Classes
    public static class UserInfo {
        private final String userName;
        private final int mainPrograms;
        private final int subfunctions;
        private final int credits;
        private final int creditsUsed;
        private final int runs;

        public UserInfo(String userName, int mainPrograms, int subfunctions, int credits, int creditsUsed, int runs) {
            this.userName = userName;
            this.mainPrograms = mainPrograms;
            this.subfunctions = subfunctions;
            this.credits = credits;
            this.creditsUsed = creditsUsed;
            this.runs = runs;
        }

        // Getters
        public String getUserName() {
            return userName;
        }

        public int getMainPrograms() {
            return mainPrograms;
        }

        public int getSubfunctions() {
            return subfunctions;
        }

        public int getCredits() {
            return credits;
        }

        public int getCreditsUsed() {
            return creditsUsed;
        }

        public int getRuns() {
            return runs;
        }
    }

    public static class ProgramInfo {
        private final String programName;
        private final String uploadedBy;
        private final int instructionCount;
        private final int maxLevel;
        private final int runs;
        private final double avgCost;

        public ProgramInfo(String programName, String uploadedBy, int instructionCount, int maxLevel, int runs,
                double avgCost) {
            this.programName = programName;
            this.uploadedBy = uploadedBy;
            this.instructionCount = instructionCount;
            this.maxLevel = maxLevel;
            this.runs = runs;
            this.avgCost = avgCost;
        }

        // Getters
        public String getProgramName() {
            return programName;
        }

        public String getUploadedBy() {
            return uploadedBy;
        }

        public int getInstructionCount() {
            return instructionCount;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public int getRuns() {
            return runs;
        }

        public double getAvgCost() {
            return avgCost;
        }
    }

    public static class RunHistory {
        private final int runNumber;
        private final String type;
        private final String name;
        private final String architecture;
        private final int level;
        private final long yValue;
        private final int cycles;
        private final String runId;
        private final Map<String, Long> finalVariables;

        public RunHistory(int runNumber, String runId, String type, String name, String architecture, int level,
                long yValue, int cycles, Map<String, Long> finalVariables) {
            this.runNumber = runNumber;
            this.runId = runId;
            this.type = type;
            this.name = name;
            this.architecture = architecture;
            this.level = level;
            this.yValue = yValue;
            this.cycles = cycles;
            this.finalVariables = finalVariables != null ? new HashMap<>(finalVariables) : new HashMap<>();
        }

        // Getters
        public int getRunNumber() {
            return runNumber;
        }

        public String getRunId() {
            return runId;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getArchitecture() {
            return architecture;
        }

        public int getLevel() {
            return level;
        }

        public long getYValue() {
            return yValue;
        }

        public int getCycles() {
            return cycles;
        }

        public Map<String, Long> getFinalVariables() {
            return new HashMap<>(finalVariables);
        }
    }

    public static class FunctionInfo {
        private final String functionName;
        private final String parentProgram;
        private final String uploadedBy;
        private final int instructionCount;
        private final int maxLevel;

        public FunctionInfo(String functionName, String parentProgram, String uploadedBy, int instructionCount,
                int maxLevel) {
            this.functionName = functionName;
            this.parentProgram = parentProgram;
            this.uploadedBy = uploadedBy;
            this.instructionCount = instructionCount;
            this.maxLevel = maxLevel;
        }

        // Getters
        public String getFunctionName() {
            return functionName;
        }

        public String getParentProgram() {
            return parentProgram;
        }

        public String getUploadedBy() {
            return uploadedBy;
        }

        public int getInstructionCount() {
            return instructionCount;
        }

        public int getMaxLevel() {
            return maxLevel;
        }
    }

    /**
     * Handle real-time program updates from socket
     */
    public void updateProgramsFromSocket(com.google.gson.JsonArray programsArray) {
        programsData.clear();

        for (int i = 0; i < programsArray.size(); i++) {
            com.google.gson.JsonObject programObj = programsArray.get(i).getAsJsonObject();

            String name = programObj.get("name").getAsString();
            String uploadedBy = programObj.get("uploadedBy").getAsString();
            int instructionCount = programObj.get("instructionCount").getAsInt();
            int maxDegree = programObj.get("maxDegree").getAsInt();
            int runs = programObj.get("runs").getAsInt();
            double avgCost = programObj.get("avgCost").getAsDouble();

            ProgramInfo localProgram = new ProgramInfo(
                    name, uploadedBy, instructionCount, maxDegree, runs, avgCost);
            programsData.add(localProgram);
        }

        System.out.println("Updated programs from WebSocket: " + programsData.size() + " programs");
    }

    /**
     * Handle real-time function updates from socket
     */
    public void updateFunctionsFromSocket(com.google.gson.JsonArray functionsArray) {
        functionsData.clear();

        for (int i = 0; i < functionsArray.size(); i++) {
            com.google.gson.JsonObject functionObj = functionsArray.get(i).getAsJsonObject();

            String name = functionObj.get("name").getAsString();
            String parentProgram = functionObj.get("parentProgram").getAsString();
            String uploadedBy = functionObj.get("uploadedBy").getAsString();
            int instructionCount = functionObj.get("instructionCount").getAsInt();
            int maxDegree = functionObj.get("maxDegree").getAsInt();

            FunctionInfo localFunction = new FunctionInfo(
                    name, parentProgram, uploadedBy, instructionCount, maxDegree);
            functionsData.add(localFunction);
        }

        System.out.println("Updated functions from WebSocket: " + functionsData.size() + " functions");
    }

    /**
     * Handle real-time user updates from WebSocket
     */
    public void updateUsersFromWebSocket(com.google.gson.JsonArray usersArray) {
        usersData.clear();

        for (int i = 0; i < usersArray.size(); i++) {
            com.google.gson.JsonObject userObj = usersArray.get(i).getAsJsonObject();

            String username = userObj.get("username").getAsString();
            int credits = userObj.get("credits").getAsInt();
            int totalRuns = userObj.get("totalRuns").getAsInt();
            int mainPrograms = userObj.has("mainPrograms") ? userObj.get("mainPrograms").getAsInt() : 0;
            int subfunctions = userObj.has("subfunctions") ? userObj.get("subfunctions").getAsInt() : 0;
            int creditsUsed = userObj.has("creditsUsed") ? userObj.get("creditsUsed").getAsInt() : 0;

            UserInfo localUser = new UserInfo(
                    username, mainPrograms, subfunctions, credits, creditsUsed, totalRuns);
            usersData.add(localUser);
        }

        // Update current user's credits in header
        updateCurrentUserCredits();

        System.out.println("Updated users from WebSocket: " + usersData.size() + " users");
    }

    /**
     * Update users from socket message
     */
    public void updateUsersFromSocket(com.google.gson.JsonArray usersArray) {
        usersData.clear();

        for (int i = 0; i < usersArray.size(); i++) {
            com.google.gson.JsonObject userObj = usersArray.get(i).getAsJsonObject();

            String username = userObj.get("username").getAsString();
            int credits = userObj.get("credits").getAsInt();
            int totalRuns = userObj.get("totalRuns").getAsInt();
            long lastActive = userObj.get("lastActive").getAsLong();
            int mainPrograms = userObj.has("mainPrograms") ? userObj.get("mainPrograms").getAsInt() : 0;
            int subfunctions = userObj.has("subfunctions") ? userObj.get("subfunctions").getAsInt() : 0;
            int creditsUsed = userObj.has("creditsUsed") ? userObj.get("creditsUsed").getAsInt() : 0;

            UserInfo localUser = new UserInfo(
                    username, mainPrograms, subfunctions, credits, creditsUsed, totalRuns);
            usersData.add(localUser);
        }

        // Update current user's credits in header
        updateCurrentUserCredits();

        System.out.println("Updated users from socket: " + usersData.size() + " users");
    }

    /**
     * Update history from WebSocket message
     */
    public void updateHistoryFromSocket(String username, com.google.gson.JsonArray historyArray) {
        // Only update if this is the currently viewed user's history
        String viewedUser = selectedUser != null ? selectedUser : currentUser;
        if (!username.equals(viewedUser)) {
            System.out.println("Ignoring history update for " + username + " (currently viewing " + viewedUser + ")");
            return;
        }

        historyData.clear();

        int runNumber = 1;
        for (int i = 0; i < historyArray.size(); i++) {
            com.google.gson.JsonObject entryObj = historyArray.get(i).getAsJsonObject();

            String runId = entryObj.get("runId").getAsString();
            String targetName = entryObj.get("targetName").getAsString();
            String targetType = entryObj.get("targetType").getAsString();
            String architecture = entryObj.get("architecture").getAsString();
            int degree = entryObj.get("degree").getAsInt();
            long finalYValue = entryObj.get("finalYValue").getAsLong();
            int cycles = entryObj.get("cycles").getAsInt();

            // Parse finalVariables map
            Map<String, Long> finalVariables = new HashMap<>();
            if (entryObj.has("finalVariables")) {
                com.google.gson.JsonObject varsObj = entryObj.getAsJsonObject("finalVariables");
                for (String varName : varsObj.keySet()) {
                    finalVariables.put(varName, varsObj.get(varName).getAsLong());
                }
            }

            // Map architecture code to readable name
            String archName = getArchitectureName(architecture);

            RunHistory history = new RunHistory(
                    runNumber++,
                    runId,
                    targetType,
                    targetName,
                    archName,
                    degree,
                    finalYValue,
                    cycles,
                    finalVariables);
            historyData.add(history);
        }

        System.out.println("Updated history from socket for user " + username + ": " + historyData.size() + " entries");
    }

    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }

        if (userUpdateClient != null) {
            userUpdateClient.disconnect();
        }
    }
}
