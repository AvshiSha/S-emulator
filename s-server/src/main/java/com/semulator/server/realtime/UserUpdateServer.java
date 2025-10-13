package com.semulator.server.realtime;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.semulator.server.state.ServerState;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple TCP socket server for real-time updates
 * Notifies all connected clients when users, programs, or functions change
 */
public class UserUpdateServer {

    private static final int PORT = 8081;
    private static final Set<ClientConnection> clients = ConcurrentHashMap.newKeySet();
    private static final Gson gson = new Gson();
    private static final ServerState serverState = ServerState.getInstance();
    private static ServerSocket serverSocket;
    private static ExecutorService executorService = Executors.newCachedThreadPool();
    private static boolean running = false;

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
            System.out
                    .println("Message to broadcast: " + message.substring(0, Math.min(100, message.length())) + "...");

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
            System.out
                    .println("Message to broadcast: " + message.substring(0, Math.min(100, message.length())) + "...");

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
            System.out
                    .println("Message to broadcast: " + message.substring(0, Math.min(100, message.length())) + "...");

            for (ClientConnection client : clients) {
                client.send(message);
            }

        } catch (Exception e) {
            // Error broadcasting history update
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
                    // Handle client messages if needed
                    // Received from client
                }
            } catch (IOException e) {
                // Client connection error
            } finally {
                close();
            }
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
