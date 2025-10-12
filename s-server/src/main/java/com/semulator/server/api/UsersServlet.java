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
 * User management endpoints
 * GET /api/users
 * POST /api/users/{username}/credits/topup
 */
@WebServlet(name = "UsersServlet", urlPatterns = { "/api/users/*" })
public class UsersServlet extends HttpServlet {

    private final ServerState serverState = ServerState.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            handleGetUsers(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.matches("/[^/]+/credits/topup")) {
            handleTopupCredits(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    private void handleGetUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<ApiModels.UserInfo> users = serverState.getAllUsers();
            long version = serverState.getCurrentVersion();
            ApiModels.UsersResponse response = new ApiModels.UsersResponse(users, version, true);
            ServletUtils.writeJson(resp, response);
        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Failed to get users: " + e.getMessage());
        }
    }

    private void handleTopupCredits(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String pathInfo = req.getPathInfo();
            String username = pathInfo.substring(1, pathInfo.indexOf("/credits/topup"));

            if (username.isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Username is required");
                return;
            }

            ApiModels.TopupRequest request = ServletUtils.parseJson(req, ApiModels.TopupRequest.class);

            if (request.amount <= 0) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Amount must be positive");
                return;
            }

            ServerState.UserRecord user = serverState.getUser(username);
            if (user == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "User not found");
                return;
            }

            int newBalance = user.credits + request.amount;
            serverState.updateUserCredits(username, newBalance);

            ApiModels.TopupResponse response = new ApiModels.TopupResponse(newBalance);
            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Topup failed: " + e.getMessage());
        }
    }
}
