package com.semulator.server.api;

import com.semulator.server.util.ServletUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Legacy engine endpoints (for backward compatibility)
 * Handles: /api/engine/*
 */
@WebServlet(name = "EngineServlet", urlPatterns = { "/api/engine/*" })
public class EngineServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/engine - Health check
                Map<String, String> response = Map.of(
                        "status", "ok",
                        "engine", "s-emulator",
                        "version", "1.0-SNAPSHOT",
                        "message",
                        "Use specific endpoints: /api/auth, /api/users, /api/programs, /api/functions, /api/run, /api/debug, /api/history");
                ServletUtils.writeJson(resp, response);
            } else {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                        "Use specific endpoints: /api/auth, /api/users, /api/programs, /api/functions, /api/run, /api/debug, /api/history");
            }
        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Engine health check failed: " + e.getMessage());
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletUtils.handleOptions(resp);
    }
}