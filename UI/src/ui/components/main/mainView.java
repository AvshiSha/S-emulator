package ui.components.main;

import javafx.application.*;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import ui.components.Header.Header;
import ui.components.InstructionTable.InstructionTable;
import ui.components.HistoryChain.HistoryChain;
import ui.components.DebuggerExecution.DebuggerExecution;
import ui.components.HistoryStats.HistoryStats;

public class mainView extends Application {
    @FXML
    private VBox headerComponent;

    @FXML
    private VBox instructionTableComponent;

    @FXML
    private VBox historyChainComponent;

    @FXML
    private VBox debuggerExecutionComponent;

    @FXML
    private VBox historyStatsComponent;

    private Header headerController;
    private InstructionTable instructionTableController;
    private HistoryChain historyChainController;
    private DebuggerExecution debuggerExecutionController;
    private HistoryStats historyStatsController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("S-Emulator");

        // Load the main FXML
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/ui/components/main/mainView.fxml"));
        mainLoader.setController(this);
        ScrollPane root = mainLoader.load();

        // Load the Header component and get its controller
        FXMLLoader headerLoader = new FXMLLoader(getClass().getResource("/ui/components/Header/Header.fxml"));
        VBox headerRoot = headerLoader.load();
        headerController = headerLoader.getController();

        // Load the InstructionTable component and get its controller
        FXMLLoader tableLoader = new FXMLLoader(
                getClass().getResource("/ui/components/InstructionTable/InstructionTable.fxml"));
        VBox tableRoot = tableLoader.load();
        instructionTableController = tableLoader.getController();

        // Load the HistoryChain component and get its controller
        FXMLLoader historyLoader = new FXMLLoader(
                getClass().getResource("/ui/components/HistoryChain/HistoryChain.fxml"));
        VBox historyRoot = historyLoader.load();
        historyChainController = historyLoader.getController();

        // Load the DebuggerExecution component and get its controller
        FXMLLoader debuggerLoader = new FXMLLoader(
                getClass().getResource("/ui/components/DebuggerExecution/DebuggerExecution.fxml"));
        VBox debuggerRoot = debuggerLoader.load();
        debuggerExecutionController = debuggerLoader.getController();

        // Load the HistoryStats component and get its controller
        FXMLLoader historyStatsLoader = new FXMLLoader(
                getClass().getResource("/ui/components/HistoryStats/HistoryStats.fxml"));
        VBox historyStatsRoot = historyStatsLoader.load();
        historyStatsController = historyStatsLoader.getController();

        // Replace the placeholder components with the actual loaded components
        headerComponent.getChildren().clear();
        headerComponent.getChildren().addAll(headerRoot.getChildren());

        instructionTableComponent.getChildren().clear();
        instructionTableComponent.getChildren().addAll(tableRoot.getChildren());

        historyChainComponent.getChildren().clear();
        historyChainComponent.getChildren().addAll(historyRoot.getChildren());

        debuggerExecutionComponent.getChildren().clear();
        debuggerExecutionComponent.getChildren().addAll(debuggerRoot.getChildren());

        historyStatsComponent.getChildren().clear();
        historyStatsComponent.getChildren().addAll(historyStatsRoot.getChildren());

        // Wire up the components
        headerController.setInstructionTable(instructionTableController);

        // Set up the history chain callback
        instructionTableController.setHistoryChainCallback(selectedInstruction -> {
            if (selectedInstruction != null) {
                // Get the real history chain from the Header controller
                java.util.List<semulator.instructions.SInstruction> chain = headerController
                        .getHistoryChain(selectedInstruction);
                historyChainController.displayHistoryChain(chain);
            } else {
                historyChainController.clearHistory();
            }
        });

        // Wire up the debugger execution component
        // When a program is loaded in the header, also set it in the debugger
        headerController.setDebuggerExecution(debuggerExecutionController);

        // Wire up the history stats component
        headerController.setHistoryStats(historyStatsController);

        // Set up the input callback for history stats to debugger
        historyStatsController.setInputCallback(inputs -> {
            // Set the inputs in the debugger execution component
            debuggerExecutionController.setInputs(inputs);
        });

        // Set up the history callback for debugger to history stats
        debuggerExecutionController.setHistoryCallback(runResult -> {
            // Add the run to the history stats
            historyStatsController.addRun(runResult.level(), runResult.inputs(), runResult.yValue(),
                    runResult.cycles());
        });

        // Wire up debugger execution with instruction table for highlighting
        debuggerExecutionController.setInstructionTableCallback(instructionIndex -> {
            // Highlight the current executing instruction in the instruction table
            instructionTableController.highlightCurrentInstruction(instructionIndex);
        });

        Scene scene = new Scene(root, 800, 800);
        primaryStage.setScene(scene);

        // Set minimum window size to ensure usability on small screens
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);

        // Ensure window is resizable
        primaryStage.setResizable(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
