package ui.components.main;

import javafx.application.*;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import ui.components.Header.Header;
import ui.components.InstructionTable.InstructionTable;
import ui.components.HistoryChain.HistoryChain;

public class mainView extends Application {
    @FXML
    private VBox headerComponent;

    @FXML
    private VBox instructionTableComponent;

    @FXML
    private VBox historyChainComponent;

    private Header headerController;
    private InstructionTable instructionTableController;
    private HistoryChain historyChainController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("S-Emulator");

        // Load the main FXML
        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/ui/components/main/mainView.fxml"));
        mainLoader.setController(this);
        Pane root = mainLoader.load();

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

        // Replace the placeholder components with the actual loaded components
        headerComponent.getChildren().clear();
        headerComponent.getChildren().addAll(headerRoot.getChildren());

        instructionTableComponent.getChildren().clear();
        instructionTableComponent.getChildren().addAll(tableRoot.getChildren());

        historyChainComponent.getChildren().clear();
        historyChainComponent.getChildren().addAll(historyRoot.getChildren());

        // Wire up the components
        headerController.setInstructionTable(instructionTableController);

        // Set up the history chain callback
        instructionTableController.setHistoryChainCallback(chain -> {
            historyChainController.displayHistoryChain(chain);
        });

        Scene scene = new Scene(root, 1000, 1000);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
