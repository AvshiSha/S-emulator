package com.semulator.client.ui;

import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.service.ChatClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Chat Window Controller
 * Handles real-time chat messaging between users
 */
public class ChatController implements Initializable {

    @FXML
    private ScrollPane messagesScrollPane;

    @FXML
    private VBox messagesContainer;

    @FXML
    private TextField messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private Button closeButton;

    private ChatClient chatClient;
    private String currentUser;
    private Stage stage;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = AppContext.getInstance().getCurrentUser();

        // Initialize chat client
        chatClient = new ChatClient(this);
        chatClient.connect();

        // Set up auto-scrolling
        messagesContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            messagesScrollPane.setVvalue(1.0);
        });

        // Enable send button only when there's text
        sendButton.disableProperty().bind(messageInput.textProperty().isEmpty());

        // Allow Enter key to send messages
        messageInput.setOnAction(e -> handleSendMessage());
    }

    @FXML
    private void handleSendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        // Send message through chat client
        chatClient.sendMessage(message);

        // Clear input field
        messageInput.clear();
    }

    @FXML
    private void handleClose() {
        // Disconnect chat client
        if (chatClient != null) {
            chatClient.disconnect();
        }

        // Close the stage
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Called when a new chat message is received
     */
    public void onMessageReceived(ApiModels.ChatMessage chatMessage) {
        Platform.runLater(() -> {
            addMessageToUI(chatMessage);
        });
    }

    /**
     * Add a message to the UI
     */
    private void addMessageToUI(ApiModels.ChatMessage chatMessage) {
        // Create message container
        VBox messageBox = new VBox(3);
        messageBox.setPadding(new Insets(8, 10, 8, 10));

        boolean isCurrentUser = chatMessage.username().equals(currentUser);

        if (isCurrentUser) {
            // Current user's messages (right-aligned, blue)
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 8; -fx-border-radius: 8;");
        } else {
            // Other users' messages (left-aligned, gray)
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8; -fx-border-radius: 8;");
        }

        // Create header (username and timestamp)
        HBox header = new HBox(8);
        header.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label usernameLabel = new Label(chatMessage.username());
        usernameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #0066cc;");

        Label timestampLabel = new Label(formatTimestamp(chatMessage.timestamp()));
        timestampLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        if (isCurrentUser) {
            header.getChildren().addAll(timestampLabel, usernameLabel);
        } else {
            header.getChildren().addAll(usernameLabel, timestampLabel);
        }

        // Create message text
        TextFlow messageText = new TextFlow();
        Text text = new Text(chatMessage.message());
        text.setStyle("-fx-font-size: 14px; -fx-fill: #333;");
        messageText.getChildren().add(text);
        messageText.setStyle("-fx-padding: 2 0 0 0;");

        // Add header and message to container
        messageBox.getChildren().addAll(header, messageText);

        // Add to messages container
        messagesContainer.getChildren().add(messageBox);

        // Auto-scroll to bottom
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault());
        return dateTime.format(TIME_FORMATTER);
    }

    /**
     * Set the stage reference for closing
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Clean up when window is closed
     */
    public void cleanup() {
        if (chatClient != null) {
            chatClient.disconnect();
        }
    }
}
