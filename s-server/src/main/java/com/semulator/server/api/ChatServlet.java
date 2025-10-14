package com.semulator.server.api;

import com.semulator.server.model.ApiModels;
import com.semulator.server.state.ServerState;
import com.semulator.server.util.ServletUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Chat endpoints for HTTP polling-based chat
 * GET /api/chat/messages?sinceTimestamp={timestamp} - Get new messages
 * POST /api/chat/messages - Send a message
 * GET /api/chat/history - Get full chat history
 */
@WebServlet(name = "ChatServlet", urlPatterns = { "/api/chat/*" })
public class ChatServlet extends HttpServlet {

    private final ServerState serverState = ServerState.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        } else if (pathInfo.equals("/messages")) {
            handleGetMessages(req, resp);
        } else if (pathInfo.equals("/history")) {
            handleGetHistory(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/messages")) {
            handleSendMessage(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    /**
     * Get new messages since a given timestamp (for polling)
     */
    private void handleGetMessages(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String sinceTimestampParam = req.getParameter("sinceTimestamp");
            long sinceTimestamp = sinceTimestampParam != null ? Long.parseLong(sinceTimestampParam) : 0;

            List<ApiModels.ChatMessage> messages = serverState.getChatMessagesSince(sinceTimestamp);

            ApiModels.ChatHistoryResponse response = new ApiModels.ChatHistoryResponse(messages);
            ServletUtils.writeJson(resp, response);

        } catch (NumberFormatException e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    "Invalid sinceTimestamp parameter");
        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Failed to get messages: " + e.getMessage());
        }
    }

    /**
     * Get full chat history
     */
    private void handleGetHistory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<ApiModels.ChatMessage> messages = serverState.getChatHistory();
            ApiModels.ChatHistoryResponse response = new ApiModels.ChatHistoryResponse(messages);
            ServletUtils.writeJson(resp, response);
        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Failed to get chat history: " + e.getMessage());
        }
    }

    /**
     * Send a new chat message
     */
    private void handleSendMessage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.ChatMessage message = ServletUtils.parseJson(req, ApiModels.ChatMessage.class);

            if (message.username == null || message.username.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "username is required");
                return;
            }

            if (message.message == null || message.message.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "message is required");
                return;
            }

            // Add message to server state
            long timestamp = System.currentTimeMillis();
            ApiModels.ChatMessage savedMessage = new ApiModels.ChatMessage(
                    message.username,
                    message.message,
                    timestamp);

            serverState.addChatMessage(savedMessage);

            // Return the saved message with timestamp
            ServletUtils.writeJson(resp, savedMessage);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Failed to send message: " + e.getMessage());
        }
    }
}

