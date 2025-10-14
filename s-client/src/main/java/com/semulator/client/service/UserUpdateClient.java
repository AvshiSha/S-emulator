package com.semulator.client.service;

import com.semulator.client.ui.DashboardController;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

/**
 * TCP socket client for real-time user updates
 * Connects to server socket and handles real-time user data updates
 */
public class UserUpdateClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8081;  // Real-time update server port
    private static final Gson gson = new Gson();

    private DashboardController dashboardController;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    private Thread readerThread;

    public UserUpdateClient(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    /**
     * Connect to server
     */
    public void connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            connected = true;

            // Start reader thread
            readerThread = new Thread(this::readMessages);
            readerThread.setDaemon(true);
            readerThread.start();

        } catch (IOException e) {
            // Failed to connect
        }
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        connected = false;
        try {
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
                    case "INITIAL_USERS":
                    case "USER_UPDATE":
                        handleUserUpdate(jsonMessage);
                        break;
                    case "PROGRAM_UPDATE":
                        handleProgramUpdate(jsonMessage);
                        break;
                    case "HISTORY_UPDATE":
                        handleHistoryUpdate(jsonMessage);
                        break;
                    default:
                        // Unknown message type
                }
            });
        } catch (Exception e) {
            // Error processing message
        }
    }

    /**
     * Handle user updates from server
     */
    private void handleUserUpdate(JsonObject message) {
        try {
            // Update users
            if (message.has("users")) {
                JsonArray usersArray = message.getAsJsonArray("users");
                dashboardController.updateUsersFromSocket(usersArray);
            }
        } catch (Exception e) {
            // Error handling user update
        }
    }

    /**
     * Handle program/function updates from server
     */
    private void handleProgramUpdate(JsonObject message) {
        try {
            // Update programs
            if (message.has("programs")) {
                JsonArray programsArray = message.getAsJsonArray("programs");
                dashboardController.updateProgramsFromSocket(programsArray);
            }

            // Update functions
            if (message.has("functions")) {
                JsonArray functionsArray = message.getAsJsonArray("functions");
                dashboardController.updateFunctionsFromSocket(functionsArray);
            }
        } catch (Exception e) {
            // Error handling program update
        }
    }

    /**
     * Handle history updates from server
     */
    private void handleHistoryUpdate(JsonObject message) {
        try {
            // Update history for the specific user
            if (message.has("username") && message.has("history")) {
                String username = message.get("username").getAsString();
                JsonArray historyArray = message.getAsJsonArray("history");
                dashboardController.updateHistoryFromSocket(username, historyArray);
            }
        } catch (Exception e) {
            // Error handling history update
        }
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }
}
