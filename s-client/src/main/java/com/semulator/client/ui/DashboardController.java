package com.semulator.client.ui;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.service.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

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
    private String currentUser;
    private String selectedUser = null; // For history view
    private String selectedProgram = null;
    private String selectedFunction = null;
    private Timer refreshTimer;
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize API client and current user
        this.apiClient = AppContext.getInstance().getApiClient();
        this.currentUser = AppContext.getInstance().getCurrentUser();

        // Initialize UI components
        initializeHeaderBar();
        initializeUsersTable();
        initializeProgramsTable();
        initializeHistoryTable();
        initializeFunctionsTable();

        // Set up event handlers
        setupEventHandlers();

        // Start auto-refresh
        startAutoRefresh();

        // Set up window close handler
        setupWindowCloseHandler();

        // Load initial data
        loadInitialData();
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
            // TODO: Implement file upload to server
            loadedFilePath.setText(selectedFile.getAbsolutePath());
            showInfoAlert("File Selected",
                    "File: " + selectedFile.getName() + "\nUpload functionality will be implemented.");
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
        refreshTimer = new Timer(true); // Daemon thread
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    refreshUserData();
                    refreshProgramsData();
                    refreshFunctionsData();
                    if (selectedUser != null) {
                        loadUserHistory(selectedUser);
                    } else {
                        loadUserHistory(currentUser);
                    }
                });
            }
        }, REFRESH_INTERVAL, REFRESH_INTERVAL);
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
                            // Calculate additional fields we need for display
                            int mainPrograms = 0; // TODO: Get from server
                            int subfunctions = 0; // TODO: Get from server
                            int creditsUsed = 0; // TODO: Calculate from history

                            UserInfo localUser = new UserInfo(
                                    serverUser.username(),
                                    mainPrograms,
                                    subfunctions,
                                    serverUser.credits(),
                                    creditsUsed,
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
        // TODO: Load programs from server API
        // For now, add some sample data
        programsData.clear();
        programsData.add(new ProgramInfo("fibonacci", "user1", 15, 4, 25, 2.5));
        programsData.add(new ProgramInfo("factorial", "user2", 8, 3, 18, 1.8));
        programsData.add(new ProgramInfo("gcd", currentUser, 12, 5, 30, 3.2));
    }

    private void refreshFunctionsData() {
        // TODO: Load functions from server API
        // For now, add some sample data
        functionsData.clear();
        functionsData.add(new FunctionInfo("add", "fibonacci", "user1", 5, 2));
        functionsData.add(new FunctionInfo("multiply", "factorial", "user2", 7, 3));
        functionsData.add(new FunctionInfo("subtract", "gcd", currentUser, 4, 1));
    }

    private void loadUserHistory(String userName) {
        // TODO: Load user history from server API
        // For now, add some sample data
        historyData.clear();

        if (userName.equals(currentUser)) {
            // Current user's history
            historyData.add(new RunHistory(1, "Main Program", "fibonacci", "Basic", 2, 21L, 150));
            historyData.add(new RunHistory(2, "Utility Function", "add", "Advanced", 1, 8L, 45));
            historyData.add(new RunHistory(3, "Main Program", "gcd", "Basic", 3, 6L, 200));
        } else {
            // Other user's history
            historyData.add(new RunHistory(1, "Main Program", "factorial", "Basic", 2, 120L, 180));
            historyData.add(new RunHistory(2, "Utility Function", "multiply", "Advanced", 1, 15L, 60));
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
        // TODO: Navigate to execution screen with selected program/function
        showInfoAlert("Navigation", "Would navigate to execution screen for:\n" +
                "Target: " + targetName + "\nType: " + targetType);
    }

    private void showRunStatus(RunHistory run) {
        // TODO: Show detailed status of the selected run
        showInfoAlert("Run Status", "Run #" + run.getRunNumber() + " Status:\n" +
                "Type: " + run.getType() + "\n" +
                "Target: " + run.getName() + "\n" +
                "Result: " + run.getYValue());
    }

    private void reRunExecution(RunHistory run) {
        // TODO: Re-run the selected execution
        showInfoAlert("Re-Run", "Would re-run execution:\n" +
                "Run #" + run.getRunNumber() + " - " + run.getName());
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

        public RunHistory(int runNumber, String type, String name, String architecture, int level, long yValue,
                int cycles) {
            this.runNumber = runNumber;
            this.type = type;
            this.name = name;
            this.architecture = architecture;
            this.level = level;
            this.yValue = yValue;
            this.cycles = cycles;
        }

        // Getters
        public int getRunNumber() {
            return runNumber;
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

    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
}
