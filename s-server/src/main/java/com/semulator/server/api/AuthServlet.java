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
}
