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
                System.out.println("User update server started on port " + PORT);

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientConnection client = new ClientConnection(clientSocket);
                        clients.add(client);
                        executorService.submit(client);
                        System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                        // Broadcast current user list to all clients when a new client connects
                        broadcastUserUpdate();
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("Error accepting client: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error starting user update server: " + e.getMessage());
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
            System.err.println("Error stopping user update server: " + e.getMessage());
        }
    }

    /**
     * Broadcast user update to all connected clients
     */
    public static void broadcastUserUpdate() {
        System.out.println("broadcastUserUpdate called. Connected clients: " + clients.size());

        if (clients.isEmpty()) {
            System.out.println("No clients connected, skipping broadcast");
            return;
        }

        try {
            var users = serverState.getAllUsers();
            System.out.println("Broadcasting update for " + users.size() + " users");

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

            System.out.println("✓ Broadcasted user update to " + clients.size() + " clients");
        } catch (Exception e) {
            System.err.println("✗ Error broadcasting user update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcast program/function update to all connected clients
     */
    public static void broadcastProgramUpdate() {
        System.out.println("broadcastProgramUpdate called. Connected clients: " + clients.size());

        if (clients.isEmpty()) {
            System.out.println("No clients connected, skipping broadcast");
            return;
        }

        try {
            var programs = serverState.getPrograms();
            var functions = serverState.getFunctions();
            System.out.println(
                    "Broadcasting update for " + programs.size() + " programs and " + functions.size() + " functions");

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

            System.out.println("✓ Broadcasted program update to " + clients.size() + " clients");
        } catch (Exception e) {
            System.err.println("✗ Error broadcasting program update: " + e.getMessage());
            e.printStackTrace();
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
                    System.out.println("Received from client: " + line);
                }
            } catch (IOException e) {
                System.err.println("Client connection error: " + e.getMessage());
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
                System.out.println("Sent initial user data to client");
            } catch (Exception e) {
                System.err.println("Error sending initial data: " + e.getMessage());
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
                System.err.println("Error closing client connection: " + e.getMessage());
            }
        }
    }
}
