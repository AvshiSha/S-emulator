package com.semulator.client.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.semulator.client.AppContext;
import com.semulator.client.model.ApiModels;
import com.semulator.client.ui.ChatController;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

/**
 * TCP socket client for real-time chat messaging
 * Connects to UserUpdateServer for chat functionality (same port as other
 * updates)
 */
public class ChatClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080; // Same port as UserUpdateServer
    private static final Gson gson = new Gson();

    private ChatController chatController;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    private Thread readerThread;
    private String currentUser;

    public ChatClient(ChatController chatController) {
        this.chatController = chatController;
        this.currentUser = AppContext.getInstance().getCurrentUser();
    }

    /**
     * Connect to chat server
     */
    public void connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            connected = true;

            // Send initial connection message with username
            JsonObject connectMsg = new JsonObject();
            connectMsg.addProperty("type", "CONNECT");
            connectMsg.addProperty("username", currentUser);
            out.println(gson.toJson(connectMsg));

            // Start reader thread
            readerThread = new Thread(this::readMessages);
            readerThread.setDaemon(true);
            readerThread.start();

        } catch (IOException e) {
            System.err.println("Failed to connect to chat server: " + e.getMessage());
        }
    }

    /**
     * Disconnect from chat server
     */
    public void disconnect() {
        connected = false;
        try {
            // Send disconnect message
            if (out != null) {
                JsonObject disconnectMsg = new JsonObject();
                disconnectMsg.addProperty("type", "DISCONNECT");
                disconnectMsg.addProperty("username", currentUser);
                out.println(gson.toJson(disconnectMsg));
            }

            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
            if (readerThread != null)
                readerThread.interrupt();
        } catch (IOException e) {
            // Error disconnecting
        }
    }

    /**
     * Send a chat message
     */
    public void sendMessage(String message) {
        if (!connected || out == null) {
            return;
        }

        try {
            JsonObject chatMsg = new JsonObject();
            chatMsg.addProperty("type", "CHAT_MESSAGE");
            chatMsg.addProperty("username", currentUser);
            chatMsg.addProperty("message", message);
            chatMsg.addProperty("timestamp", System.currentTimeMillis());

            out.println(gson.toJson(chatMsg));
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    /**
     * Read messages from server
     */
    private void readMessages() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                final String message = line;
                onMessage(message);
            }
        } catch (IOException e) {
            // Connection lost
            if (connected) {
                System.err.println("Chat connection lost: " + e.getMessage());
            }
        }
    }

    /**
     * Handle incoming messages
     */
    private void onMessage(String message) {
        try {
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
            String type = jsonMessage.get("type").getAsString();

            Platform.runLater(() -> {
                switch (type) {
                    case "CHAT_MESSAGE":
                        handleChatMessage(jsonMessage);
                        break;
                    case "SYSTEM_MESSAGE":
                        handleSystemMessage(jsonMessage);
                        break;
                    case "CHAT_HISTORY":
                        handleChatHistory(jsonMessage);
                        break;
                    default:
                        // Unknown message type
                }
            });
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    /**
     * Handle chat message from server
     */
    private void handleChatMessage(JsonObject message) {
        try {
            String username = message.get("username").getAsString();
            String text = message.get("message").getAsString();
            long timestamp = message.get("timestamp").getAsLong();

            ApiModels.ChatMessage chatMessage = new ApiModels.ChatMessage(username, text, timestamp);
            chatController.onMessageReceived(chatMessage);
        } catch (Exception e) {
            System.err.println("Error handling chat message: " + e.getMessage());
        }
    }

    /**
     * Handle system message from server (e.g., user joined/left)
     */
    private void handleSystemMessage(JsonObject message) {
        try {
            String text = message.get("message").getAsString();
            long timestamp = message.get("timestamp").getAsLong();

            ApiModels.ChatMessage systemMessage = new ApiModels.ChatMessage("System", text, timestamp);
            chatController.onMessageReceived(systemMessage);
        } catch (Exception e) {
            System.err.println("Error handling system message: " + e.getMessage());
        }
    }

    /**
     * Handle chat history (recent messages) when joining
     */
    private void handleChatHistory(JsonObject message) {
        try {
            if (message.has("messages")) {
                var messagesArray = message.getAsJsonArray("messages");
                for (var msgElement : messagesArray) {
                    JsonObject msgObj = msgElement.getAsJsonObject();
                    handleChatMessage(msgObj);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling chat history: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }
}
