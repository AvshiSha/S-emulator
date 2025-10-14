package com.semulator.server.realtime;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.semulator.server.state.ServerState;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple TCP socket server for real-time updates and chat
 * Notifies all connected clients when users, programs, functions change
 * and handles real-time chat messaging
 */
public class UserUpdateServer {

    private static final int PORT = 8081;
    private static final Set<ClientConnection> clients = ConcurrentHashMap.newKeySet();
    private static final Gson gson = new Gson();
    private static final ServerState serverState = ServerState.getInstance();
    private static ServerSocket serverSocket;
    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private static boolean running = false;

    // Chat history - store last 50 messages
    private static final int MAX_CHAT_HISTORY = 50;
    private static final List<JsonObject> chatHistory = Collections.synchronizedList(new ArrayList<>());

    /**
     * Start the user update server
     */
    public static void start() {
        if (running) {
            return;
        }

        running = true;
        executorService.submit(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                // User update server started

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientConnection client = new ClientConnection(clientSocket);
                        clients.add(client);
                        executorService.submit(client);
                        // Client connected

                        // Broadcast current user list to all clients when a new client connects
                        broadcastUserUpdate();
                    } catch (IOException e) {
                        if (running) {
                            // Error accepting client
                        }
                    }
                }
            } catch (IOException e) {
                // Error starting user update server
            }
        });
    }

    /**
     * Stop the user update server
     */
    public static void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientConnection client : clients) {
                client.close();
            }
            clients.clear();
            executorService.shutdown();
        } catch (IOException e) {
            // Error stopping user update server
        }
    }

    /**
     * Broadcast user update to all connected clients
     */
    public static void broadcastUserUpdate() {
        if (clients.isEmpty()) {
            return;
        }

        try {
            var users = serverState.getAllUsers();

            JsonObject update = new JsonObject();
            update.addProperty("type", "USER_UPDATE");
            update.add("users", gson.toJsonTree(users));
            update.addProperty("timestamp", System.currentTimeMillis());

            String message = gson.toJson(update) + "\n";

            for (ClientConnection client : clients) {
                client.send(message);
            }

        } catch (Exception e) {
            // Error broadcasting user update
        }
    }

    /**
     * Broadcast program/function update to all connected clients
     */
    public static void broadcastProgramUpdate() {
        if (clients.isEmpty()) {
            return;
        }

        try {
            var programs = serverState.getPrograms();
            var functions = serverState.getFunctions();

            JsonObject update = new JsonObject();
            update.addProperty("type", "PROGRAM_UPDATE");
            update.add("programs", gson.toJsonTree(programs));
            update.add("functions", gson.toJsonTree(functions));
            update.addProperty("timestamp", System.currentTimeMillis());

            String message = gson.toJson(update) + "\n";

            for (ClientConnection client : clients) {
                client.send(message);
            }

        } catch (Exception e) {
            // Error broadcasting program update
        }
    }

    /**
     * Broadcast history update for a specific user to all connected clients
     */
    public static void broadcastHistoryUpdate(String username) {
        if (clients.isEmpty()) {
            return;
        }

        try {
            var history = serverState.getUserHistory(username);

            JsonObject update = new JsonObject();
            update.addProperty("type", "HISTORY_UPDATE");
            update.addProperty("username", username);
            update.add("history", gson.toJsonTree(history));
            update.addProperty("timestamp", System.currentTimeMillis());

            String message = gson.toJson(update) + "\n";

            for (ClientConnection client : clients) {
                client.send(message);
            }

        } catch (Exception e) {
            // Error broadcasting history update
        }
    }

    /**
     * Broadcast chat message to all connected clients
     */
    public static void broadcastChatMessage(String username, String message, long timestamp) {
        if (clients.isEmpty()) {
            return;
        }

        try {
            JsonObject chatMsg = new JsonObject();
            chatMsg.addProperty("type", "CHAT_MESSAGE");
            chatMsg.addProperty("username", username);
            chatMsg.addProperty("message", message);
            chatMsg.addProperty("timestamp", timestamp);

            String msgJson = gson.toJson(chatMsg) + "\n";

            // Add to history
            synchronized (chatHistory) {
                chatHistory.add(chatMsg);
                if (chatHistory.size() > MAX_CHAT_HISTORY) {
                    chatHistory.remove(0);
                }
            }

            // Broadcast to all clients
            for (ClientConnection client : clients) {
                client.send(msgJson);
            }

        } catch (Exception e) {
            System.err.println("Error broadcasting chat message: " + e.getMessage());
        }
    }

    /**
     * Broadcast system message to all connected clients
     */
    public static void broadcastSystemMessage(String message) {
        if (clients.isEmpty()) {
            return;
        }

        try {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("type", "SYSTEM_MESSAGE");
            sysMsg.addProperty("message", message);
            sysMsg.addProperty("timestamp", System.currentTimeMillis());

            String msgJson = gson.toJson(sysMsg) + "\n";

            for (ClientConnection client : clients) {
                client.send(msgJson);
            }

        } catch (Exception e) {
            System.err.println("Error broadcasting system message: " + e.getMessage());
        }
    }

    /**
     * Send chat history to a specific client
     */
    private static void sendChatHistory(ClientConnection client) {
        try {
            synchronized (chatHistory) {
                if (!chatHistory.isEmpty()) {
                    JsonObject historyMsg = new JsonObject();
                    historyMsg.addProperty("type", "CHAT_HISTORY");
                    historyMsg.add("messages", gson.toJsonTree(chatHistory));

                    client.send(gson.toJson(historyMsg) + "\n");
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending chat history: " + e.getMessage());
        }
    }

    /**
     * Client connection handler
     */
    private static class ClientConnection implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean connected = true;
        private String username;

        public ClientConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Send initial user list
                sendInitialData();

                // Keep connection alive and listen for messages
                String line;
                while (connected && (line = in.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException e) {
                // Client connection error
            } finally {
                close();
            }
        }

        /**
         * Handle incoming message from client
         */
        private void handleMessage(String message) {
            try {
                JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
                String type = jsonMessage.get("type").getAsString();

                switch (type) {
                    case "CONNECT":
                        handleConnect(jsonMessage);
                        break;
                    case "CHAT_MESSAGE":
                        handleChatMessage(jsonMessage);
                        break;
                    case "DISCONNECT":
                        handleDisconnect(jsonMessage);
                        break;
                    default:
                        // Unknown message type
                }
            } catch (Exception e) {
                System.err.println("Error handling message: " + e.getMessage());
            }
        }

        /**
         * Handle client connect for chat
         */
        private void handleConnect(JsonObject message) {
            username = message.get("username").getAsString();

            // Send chat history to the new client
            sendChatHistory(this);

            // Broadcast system message
            broadcastSystemMessage(username + " joined the chat");
        }

        /**
         * Handle chat message from client
         */
        private void handleChatMessage(JsonObject message) {
            String username = message.get("username").getAsString();
            String text = message.get("message").getAsString();
            long timestamp = message.get("timestamp").getAsLong();

            // Broadcast to all clients
            broadcastChatMessage(username, text, timestamp);
        }

        /**
         * Handle client disconnect from chat
         */
        private void handleDisconnect(JsonObject message) {
            String username = message.get("username").getAsString();
            // System.out.println("User disconnected from chat: " + username);

            // Broadcast system message
            broadcastSystemMessage(username + " left the chat");
        }

        private void sendInitialData() {
            try {
                var users = serverState.getAllUsers();

                JsonObject update = new JsonObject();
                update.addProperty("type", "INITIAL_USERS");
                update.add("users", gson.toJsonTree(users));
                update.addProperty("timestamp", System.currentTimeMillis());

                send(gson.toJson(update) + "\n");
            } catch (Exception e) {
                // Error sending initial data
            }
        }

        public void send(String message) {
            if (connected && out != null) {
                out.print(message);
                out.flush();
            }
        }

        public void close() {
            connected = false;
            clients.remove(this);

            if (username != null) {
                // System.out.println("Client disconnected: " + username + ". Total clients: " +
                // clients.size());
            }

            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                // Error closing client connection
            }
        }
    }
}
