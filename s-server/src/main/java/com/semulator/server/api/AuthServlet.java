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
 * Authentication endpoints
 * POST /api/auth/login
 */
@WebServlet(name = "AuthServlet", urlPatterns = { "/api/auth/*" })
public class AuthServlet extends HttpServlet {

    private final ServerState serverState = ServerState.getInstance();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/login")) {
            handleLogin(req, resp);
        } else if (pathInfo != null && pathInfo.equals("/logout")) {
            handleLogout(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/users")) {
            handleGetUsers(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletUtils.handleOptions(resp);
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.LoginRequest request = ServletUtils.parseJson(req, ApiModels.LoginRequest.class);

            if (request.username == null || request.username.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Username is required");
                return;
            }

            String username = request.username.trim();
            
            // Check if user is already logged in (has active token)
            if (serverState.hasActiveToken(username)) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_CONFLICT, "USER_ALREADY_LOGGED_IN",
                        "User '" + username + "' is already logged in. Please logout first or use a different username.");
                return;
            }

            ServerState.UserRecord user = serverState.getUser(username);

            if (user == null) {
                // Create new user with default credits
                user = serverState.createUser(username, 100);
                if (user == null) {
                    ServletUtils.writeError(resp, HttpServletResponse.SC_CONFLICT, "CONFLICT",
                            "Username already exists");
                    return;
                }
            }

            String token = serverState.createToken(username);
            ApiModels.LoginResponse response = new ApiModels.LoginResponse(username, user.credits);
            response.token = token;

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Login failed: " + e.getMessage());
        }
    }

    private void handleGetUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<ApiModels.UserInfo> users = serverState.getAllUsers();

            // Create a response wrapper for consistency with other endpoints
            UsersResponse response = new UsersResponse(users, serverState.getCurrentVersion(), true);
            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Failed to get users: " + e.getMessage());
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Get token from Authorization header or request parameter
            String token = req.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            } else {
                token = req.getParameter("token");
            }

            if (token != null && !token.trim().isEmpty()) {
                String username = serverState.getUsernameFromToken(token);
                if (username != null) {
                    // Remove token (logout user)
                    serverState.removeToken(token);

                    // Optionally, you could also remove the user entirely, but for now we'll keep
                    // them
                    // serverState.removeUser(username);

                    ServletUtils.writeJson(resp, new LogoutResponse("success"));
                    return;
                }
            }

            ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "INVALID_TOKEN",
                    "Invalid or missing token");

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Logout failed: " + e.getMessage());
        }
    }

    // Response wrapper for users endpoint
    public static class UsersResponse {
        public List<ApiModels.UserInfo> users;
        public long version;
        public boolean full;

        public UsersResponse(List<ApiModels.UserInfo> users, long version, boolean full) {
            this.users = users;
            this.version = version;
            this.full = full;
        }
    }

    // Response for logout endpoint
    public static class LogoutResponse {
        public String status;

        public LogoutResponse(String status) {
            this.status = status;
        }
    }
}
