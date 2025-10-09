package com.semulator.server.api;

import com.semulator.engine.model.*;
import com.semulator.server.model.ApiModels;
import com.semulator.server.state.ServerState;
import com.semulator.server.util.ServletUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

/**
 * Run endpoints
 * POST /api/run/prepare
 * POST /api/run/start
 * GET /api/run/status
 * POST /api/run/cancel
 */
@WebServlet(name = "RunServlet", urlPatterns = { "/api/run/*" })
public class RunServlet extends HttpServlet {

    private final ServerState serverState = ServerState.getInstance();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/prepare")) {
            handlePrepare(req, resp);
        } else if (pathInfo != null && pathInfo.equals("/start")) {
            handleStart(req, resp);
        } else if (pathInfo != null && pathInfo.equals("/cancel")) {
            handleCancel(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/status")) {
            handleStatus(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    private void handlePrepare(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.RunPrepareRequest request = ServletUtils.parseJson(req, ApiModels.RunPrepareRequest.class);

            // Validate request
            if (request.target == null || request.arch == null || request.inputs == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Missing required fields");
                return;
            }

            // Check if architecture is supported
            boolean supported = isArchitectureSupported(request.arch);
            List<String> unsupported = supported ? List.of()
                    : List.of("Architecture " + request.arch + " not supported");

            // Calculate instruction counts by architecture (simplified)
            Map<String, Integer> instructionCountsByArch = new HashMap<>();
            instructionCountsByArch.put("I", 10);
            instructionCountsByArch.put("II", 20);
            instructionCountsByArch.put("III", 30);
            instructionCountsByArch.put("IV", 40);

            // Estimate cost based on degree and architecture
            int estimatedCost = calculateEstimatedCost(request.arch, request.degree);

            List<String> messages = new ArrayList<>();
            messages.add("Run prepared successfully");
            if (request.degree > 0) {
                messages.add("Expansion degree: " + request.degree);
            }

            ApiModels.RunPrepareResponse response = new ApiModels.RunPrepareResponse(
                    supported,
                    unsupported,
                    instructionCountsByArch,
                    estimatedCost,
                    messages);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Prepare failed: " + e.getMessage());
        }
    }

    private void handleStart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.RunStartRequest request = ServletUtils.parseJson(req, ApiModels.RunStartRequest.class);

            // Validate request
            if (request.target == null || request.arch == null || request.inputs == null || request.username == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Missing required fields");
                return;
            }

            // Check user exists
            ServerState.UserRecord user = serverState.getUser(request.username);
            if (user == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "User not found");
                return;
            }

            // Calculate costs
            int archCost = getArchitectureCost(request.arch);
            int estimatedCost = calculateEstimatedCost(request.arch, request.degree);
            int totalCost = archCost + estimatedCost;

            // Check if user has sufficient credits
            if (user.credits < totalCost) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "INSUFFICIENT_CREDITS",
                        "Insufficient credits. Required: " + totalCost + ", Available: " + user.credits);
                return;
            }

            // Charge architecture cost immediately
            if (!serverState.deductCredits(request.username, archCost)) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "INSUFFICIENT_CREDITS",
                        "Failed to charge architecture cost");
                return;
            }

            // Generate run ID
            String runId = "run_" + request.username + "_" + System.currentTimeMillis();

            // Create run session
            ServerState.RunSession session = serverState.createRunSession(
                    runId, request.username, request.target, request.arch, request.degree, request.inputs);

            // Start execution in background (simplified)
            startExecutionAsync(session);

            ApiModels.RunStartResponse response = new ApiModels.RunStartResponse(runId);
            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Start failed: " + e.getMessage());
        }
    }

    private void handleStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String runId = req.getParameter("runId");

            if (runId == null || runId.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "runId parameter is required");
                return;
            }

            ServerState.RunSession session = serverState.getRunSession(runId);
            if (session == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Run not found");
                return;
            }

            ApiModels.RunStatusResponse response = new ApiModels.RunStatusResponse(
                    session.state,
                    session.cycles,
                    session.instrByArch,
                    session.pointer,
                    session.outputY,
                    session.error);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Status failed: " + e.getMessage());
        }
    }

    private void handleCancel(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.RunCancelRequest request = ServletUtils.parseJson(req, ApiModels.RunCancelRequest.class);

            if (request.runId == null || request.runId.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "runId is required");
                return;
            }

            ServerState.RunSession session = serverState.getRunSession(request.runId);
            if (session == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Run not found");
                return;
            }

            // Cancel the run
            session.state = "STOPPED";
            session.error = "Cancelled by user";

            ServletUtils.writeJson(resp, Map.of("success", true, "message", "Run cancelled"));

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Cancel failed: " + e.getMessage());
        }
    }

    // Helper methods
    private boolean isArchitectureSupported(Architecture arch) {
        return arch != null; // Simplified - all architectures supported
    }

    private int getArchitectureCost(Architecture arch) {
        // Simplified cost calculation
        switch (arch.toString()) {
            case "I":
                return 10;
            case "II":
                return 20;
            case "III":
                return 30;
            case "IV":
                return 40;
            default:
                return 10;
        }
    }

    private int calculateEstimatedCost(Architecture arch, int degree) {
        int baseCost = getArchitectureCost(arch);
        return baseCost * (degree + 1); // Cost increases with degree
    }

    private void startExecutionAsync(ServerState.RunSession session) {
        // Simplified async execution simulation
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate execution time

                // Simulate successful execution
                session.state = "FINISHED";
                session.cycles = 100 + (int) (Math.random() * 900);
                session.pointer = session.cycles;
                session.outputY = (long) (Math.random() * 1000);
                session.instrByArch.put(session.arch.toString(), session.cycles);

                // Add to history
                serverState.addHistoryEntry(
                        session.username,
                        session.runId,
                        session.target.getName(),
                        session.outputY,
                        session.cycles);

            } catch (InterruptedException e) {
                session.state = "STOPPED";
                session.error = "Execution interrupted";
            } catch (Exception e) {
                session.state = "ERROR";
                session.error = e.getMessage();
            }
        }).start();
    }
}
