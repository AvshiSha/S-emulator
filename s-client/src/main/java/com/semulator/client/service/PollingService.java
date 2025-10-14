package com.semulator.client.service;

import com.google.gson.JsonArray;
import com.semulator.client.model.ApiModels;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * HTTP Polling Service for real-time updates
 * Periodically polls the server for changes using delta fetching
 * All communication through port 8080 (HTTP REST API)
 */
public class PollingService {

    private static final int POLL_INTERVAL_MS = 2000; // Poll every 2 seconds

    private final ApiClient apiClient;
    private final ScheduledExecutorService scheduler;

    // Version tracking for delta fetching
    private long usersVersion = 0;
    private long programsVersion = 0;
    private long functionsVersion = 0;
    private long historyVersion = 0;
    private long chatTimestamp = 0;

    // Callbacks for handling updates
    private Consumer<JsonArray> onUsersUpdate;
    private Consumer<JsonArray> onProgramsUpdate;
    private Consumer<JsonArray> onFunctionsUpdate;
    private Consumer<ApiModels.ChatMessage> onChatMessage;
    private Consumer<String> onHistoryUpdate;

    private boolean running = false;
    private String currentUsername;

    public PollingService(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Start polling for updates
     */
    public void start(String username) {
        if (running) {
            return;
        }

        this.currentUsername = username;
        this.running = true;

        // Schedule periodic polling
        scheduler.scheduleAtFixedRate(() -> {
            try {
                pollAllEndpoints();
            } catch (Exception e) {
                // Ignore polling errors - will retry on next interval
            }
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop polling
     */
    public void stop() {
        running = false;
        scheduler.shutdownNow();
    }

    /**
     * Poll all endpoints for updates
     */
    private void pollAllEndpoints() {
        if (!running) {
            return;
        }

        // Poll users
        pollUsers();

        // Poll programs and functions
        pollPrograms();
        pollFunctions();

        // Poll history for current user
        if (currentUsername != null) {
            pollHistory(currentUsername);
        }

        // Poll chat messages
        pollChat();
    }

    /**
     * Poll users endpoint with delta fetching
     */
    private void pollUsers() {
        apiClient.get("/users?sinceVersion=" + usersVersion, ApiModels.DeltaResponse.class)
                .thenAccept(response -> {
                    if (response != null) {
                        usersVersion = response.version();

                        // Handle both full response (items) and delta response (delta)
                        java.util.List<?> usersList = null;
                        if (response.items() != null) {
                            // Full response
                            usersList = response.items();
                        } else if (response.delta() != null && response.delta().added() != null) {
                            // Delta response - use added items
                            usersList = response.delta().added();
                        }

                        if (usersList != null && !usersList.isEmpty()) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            JsonArray usersArray = gson.toJsonTree(usersList).getAsJsonArray();

                            if (onUsersUpdate != null) {
                                Platform.runLater(() -> onUsersUpdate.accept(usersArray));
                            }
                        }
                    }
                })
                .exceptionally(error -> {
                    // Ignore errors
                    return null;
                });
    }

    /**
     * Poll programs endpoint with delta fetching
     */
    private void pollPrograms() {
        apiClient.get("/programs?sinceVersion=" + programsVersion, ApiModels.DeltaResponse.class)
                .thenAccept(response -> {
                    if (response != null) {
                        programsVersion = response.version();

                        // Handle both full response (items) and delta response (delta)
                        java.util.List<?> programsList = null;
                        if (response.items() != null) {
                            programsList = response.items();
                        } else if (response.delta() != null && response.delta().added() != null) {
                            programsList = response.delta().added();
                        }

                        if (programsList != null && !programsList.isEmpty()) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            JsonArray programsArray = gson.toJsonTree(programsList).getAsJsonArray();

                            if (onProgramsUpdate != null) {
                                Platform.runLater(() -> onProgramsUpdate.accept(programsArray));
                            }
                        }
                    }
                })
                .exceptionally(error -> {
                    // Ignore errors
                    return null;
                });
    }

    /**
     * Poll functions endpoint with delta fetching
     */
    private void pollFunctions() {
        apiClient.get("/functions?sinceVersion=" + functionsVersion, ApiModels.DeltaResponse.class)
                .thenAccept(response -> {
                    if (response != null) {
                        functionsVersion = response.version();

                        // Handle both full response (items) and delta response (delta)
                        java.util.List<?> functionsList = null;
                        if (response.items() != null) {
                            functionsList = response.items();
                        } else if (response.delta() != null && response.delta().added() != null) {
                            functionsList = response.delta().added();
                        }

                        if (functionsList != null && !functionsList.isEmpty()) {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            JsonArray functionsArray = gson.toJsonTree(functionsList).getAsJsonArray();

                            if (onFunctionsUpdate != null) {
                                Platform.runLater(() -> onFunctionsUpdate.accept(functionsArray));
                            }
                        }
                    }
                })
                .exceptionally(error -> {
                    // Ignore errors
                    return null;
                });
    }

    /**
     * Poll history for a specific user
     */
    private void pollHistory(String username) {
        apiClient.get("/history?user=" + username + "&sinceVersion=" + historyVersion,
                ApiModels.DeltaResponse.class)
                .thenAccept(response -> {
                    if (response != null && response.items() != null) {
                        historyVersion = response.version();

                        if (onHistoryUpdate != null) {
                            Platform.runLater(() -> onHistoryUpdate.accept(username));
                        }
                    }
                })
                .exceptionally(error -> {
                    // Ignore errors
                    return null;
                });
    }

    /**
     * Poll chat messages
     */
    private void pollChat() {
        apiClient.get("/chat/messages?sinceTimestamp=" + chatTimestamp, ApiModels.ChatHistoryResponse.class)
                .thenAccept(response -> {
                    if (response != null && response.messages() != null && !response.messages().isEmpty()) {
                        // Update chat timestamp to latest message
                        for (ApiModels.ChatMessage msg : response.messages()) {
                            if (msg.timestamp() > chatTimestamp) {
                                chatTimestamp = msg.timestamp();
                            }

                            // Notify callback for each new message
                            if (onChatMessage != null) {
                                Platform.runLater(() -> onChatMessage.accept(msg));
                            }
                        }
                    }
                })
                .exceptionally(error -> {
                    // Ignore errors
                    return null;
                });
    }

    // Callback setters
    public void setOnUsersUpdate(Consumer<JsonArray> callback) {
        this.onUsersUpdate = callback;
    }

    public void setOnProgramsUpdate(Consumer<JsonArray> callback) {
        this.onProgramsUpdate = callback;
    }

    public void setOnFunctionsUpdate(Consumer<JsonArray> callback) {
        this.onFunctionsUpdate = callback;
    }

    public void setOnChatMessage(Consumer<ApiModels.ChatMessage> callback) {
        this.onChatMessage = callback;
    }

    public void setOnHistoryUpdate(Consumer<String> callback) {
        this.onHistoryUpdate = callback;
    }

    public boolean isRunning() {
        return running;
    }
}
