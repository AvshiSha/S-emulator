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
import java.util.UUID;

/**
 * Debug execution endpoints
 * POST /api/debug/start - Start a debug session
 * POST /api/debug/step - Execute one instruction
 * POST /api/debug/resume - Continue execution to completion
 * POST /api/debug/stop - Stop debug session
 * GET /api/debug/state?sessionId=xxx - Get current debug state
 */
@WebServlet(name = "DebugServlet", urlPatterns = { "/api/debug/*" })
public class DebugServlet extends HttpServlet {

    private final ServerState serverState = ServerState.getInstance();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
            return;
        }

        switch (pathInfo) {
            case "/start":
                handleStart(req, resp);
                break;
            case "/step":
                handleStepNew(req, resp);
                break;
            case "/resume":
                handleResume(req, resp);
                break;
            case "/stop":
                handleStopNew(req, resp);
                break;
            default:
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
                break;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/state")) {
            handleGetState(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    private void handleStart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.DebugStartRequest request = ServletUtils.parseJson(req, ApiModels.DebugStartRequest.class);

            // Validate request
            if (request.programName == null || request.programName.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "programName is required");
                return;
            }

            if (request.inputs == null) {
                request.inputs = java.util.Collections.emptyList();
            }

            // Extract username from Authorization token
            String username = getUsernameFromRequest(req);
            if (username == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                        "Authentication required. Please provide a valid token.");
                return;
            }

            // Check user exists and has sufficient credits
            ServerState.UserRecord user = serverState.getUser(username);
            if (user == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "User not found");
                return;
            }

            // Get architecture from request (client sends the selected architecture)
            String requiredArchitecture = request.architecture != null ? request.architecture : "I";
            int archCost = getArchitectureCost(requiredArchitecture);

            // Check if user has sufficient credits for architecture cost
            if (user.credits < archCost) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "INSUFFICIENT_CREDITS",
                        "Insufficient credits. Required: " + archCost + ", Available: " + user.credits);
                return;
            }

            // Deduct architecture cost
            if (!serverState.deductCredits(username, archCost)) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "INSUFFICIENT_CREDITS",
                        "Failed to charge architecture cost");
                return;
            }

            // Create debug session
            String sessionId = "debug_" + UUID.randomUUID().toString();
            ServerState.DebugSession session = serverState.createDebugSession(
                    sessionId, username, request.programName, request.degree, request.inputs);

            if (session == null) {
                // Refund architecture cost if session creation fails
                serverState.updateUserCredits(username, user.credits + archCost);
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                        "Program not found: " + request.programName);
                return;
            }

            // Set initial state to PAUSED (waiting for first step)
            session.state = "PAUSED";

            // Create response with initial state
            ApiModels.DebugStateResponse stateResponse = createStateResponse(session);
            ApiModels.DebugStartResponse response = new ApiModels.DebugStartResponse(
                    true, "Debug session started", sessionId, stateResponse);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Failed to start debug session: " + e.getMessage());
        }
    }

    private void handleStepNew(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.DebugStepRequest request = ServletUtils.parseJson(req, ApiModels.DebugStepRequest.class);

            if (request.sessionId == null || request.sessionId.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "sessionId is required");
                return;
            }

            ServerState.DebugSession session = serverState.getDebugSession(request.sessionId);
            if (session == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                        "Debug session not found");
                return;
            }

            if ("FINISHED".equals(session.state) || "ERROR".equals(session.state)) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Debug session is already finished or in error state");
                return;
            }

            // Get the current instruction to determine cycle cost
            if (session.currentInstructionIndex >= session.instructions.size()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "No more instructions to execute");
                return;
            }

            SInstruction currentInstruction = session.instructions.get(session.currentInstructionIndex);
            int cycleCost = currentInstruction.cycles();

            // Check and deduct credits based on instruction cycles
            ServerState.UserRecord user = serverState.getUser(session.username);
            if (user == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "User not found");
                return;
            }

            if (user.credits < cycleCost) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "INSUFFICIENT_CREDITS",
                        "Out of credits! Execution stopped. Required: " + cycleCost + ", Available: " + user.credits);
                session.state = "ERROR";
                session.error = "Out of credits";
                return;
            }

            // Deduct credits based on instruction cycles
            if (!serverState.deductCredits(session.username, cycleCost)) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "INSUFFICIENT_CREDITS",
                        "Failed to deduct credits for cycle");
                return;
            }

            // Track credits spent
            session.creditsSpent += cycleCost;

            // Execute one instruction
            boolean finished = executeSingleStep(session);

            if (finished) {
                session.state = "FINISHED";
                // Update program statistics when debug execution completes via step
                serverState.updateProgramStatistics(session.programName, session.creditsSpent);
            } else {
                session.state = "PAUSED";
            }

            // Create response with updated state
            ApiModels.DebugStateResponse stateResponse = createStateResponse(session);
            ApiModels.DebugStepResponse response = new ApiModels.DebugStepResponse(
                    true, "Step executed", stateResponse);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Step execution failed: " + e.getMessage());
        }
    }

    private void handleResume(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.DebugResumeRequest request = ServletUtils.parseJson(req, ApiModels.DebugResumeRequest.class);

            if (request.sessionId == null || request.sessionId.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "sessionId is required");
                return;
            }

            ServerState.DebugSession session = serverState.getDebugSession(request.sessionId);
            if (session == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                        "Debug session not found");
                return;
            }

            if ("FINISHED".equals(session.state) || "ERROR".equals(session.state)) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Debug session is already finished or in error state");
                return;
            }

            // Execute all remaining instructions
            session.state = "RUNNING";
            while (session.currentInstructionIndex < session.instructions.size()) {
                boolean finished = executeSingleStep(session);
                if (finished) {
                    break;
                }
            }

            session.state = "FINISHED";

            // Update program statistics when debug execution completes
            serverState.updateProgramStatistics(session.programName, session.creditsSpent);

            // Create response with final state
            ApiModels.DebugStateResponse stateResponse = createStateResponse(session);
            ApiModels.DebugResumeResponse response = new ApiModels.DebugResumeResponse(
                    true, "Execution completed", stateResponse);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Resume execution failed: " + e.getMessage());
        }
    }

    private void handleStopNew(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.DebugStopRequest request = ServletUtils.parseJson(req, ApiModels.DebugStopRequest.class);

            if (request.sessionId == null || request.sessionId.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "sessionId is required");
                return;
            }

            ServerState.DebugSession session = serverState.getDebugSession(request.sessionId);
            if (session == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                        "Debug session not found");
                return;
            }

            // Update program statistics and history if execution reached the end (state is
            // FINISHED)
            // Don't update if user stopped early or if no execution happened
            if ("FINISHED".equals(session.state) && session.cycles > 0) {
                serverState.updateProgramStatistics(session.programName, session.creditsSpent);

                // Increment user's total runs count
                serverState.incrementUserRuns(session.username);

                // Add to history (pass both cycles for display and creditsSpent for statistics)
                Long finalYValue = session.variables.get("y");
                serverState.addHistoryEntry(
                        session.username,
                        request.sessionId,
                        session.programName,
                        "PROGRAM", // TODO: Detect if it's a program or function
                        "I", // TODO: Determine actual architecture used
                        session.degree,
                        finalYValue != null ? finalYValue : 0L,
                        session.cycles,
                        session.variables,
                        session.creditsSpent);
            }

            // Remove the debug session
            serverState.removeDebugSession(request.sessionId);

            ApiModels.DebugStopResponse response = new ApiModels.DebugStopResponse(
                    true, "Debug session stopped");

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Stop failed: " + e.getMessage());
        }
    }

    private void handleGetState(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String sessionId = req.getParameter("sessionId");

            if (sessionId == null || sessionId.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "sessionId is required");
                return;
            }

            ServerState.DebugSession session = serverState.getDebugSession(sessionId);
            if (session == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                        "Debug session not found");
                return;
            }

            ApiModels.DebugStateResponse response = createStateResponse(session);
            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Get state failed: " + e.getMessage());
        }
    }

    /**
     * Execute a single instruction and return true if execution is finished
     */
    private boolean executeSingleStep(ServerState.DebugSession session) {
        if (session.currentInstructionIndex >= session.instructions.size()) {
            return true; // Already finished
        }

        try {
            SInstruction currentInstruction = session.instructions.get(session.currentInstructionIndex);

            // Execute the instruction
            Label nextLabel = currentInstruction.execute(session.executionContext);

            // Update cycles
            session.cycles += currentInstruction.cycles();

            // Determine next instruction
            if (nextLabel == null || nextLabel == FixedLabel.EMPTY) {
                // Continue to next instruction
                session.currentInstructionIndex++;
            } else if (nextLabel == FixedLabel.EXIT) {
                // Exit execution
                session.currentInstructionIndex = session.instructions.size();
                session.updateVariablesFromContext();
                return true;
            } else {
                // Jump to labeled instruction
                String labelName = nextLabel.getLabel();
                Integer targetIndex = session.labelToIndexMap.get(labelName);
                if (targetIndex != null) {
                    session.currentInstructionIndex = targetIndex;
                } else {
                    // Label not found, continue to next
                    session.currentInstructionIndex++;
                }
            }

            // Update variable states
            session.updateVariablesFromContext();

            // Check if execution is complete
            return session.currentInstructionIndex >= session.instructions.size();

        } catch (Exception e) {
            session.error = "Execution error: " + e.getMessage();
            session.state = "ERROR";
            return true;
        }
    }

    /**
     * Create a state response from a debug session
     */
    private ApiModels.DebugStateResponse createStateResponse(ServerState.DebugSession session) {
        // Get output value (y variable)
        Long outputY = session.variables.get("y");

        // Get current user credits
        ServerState.UserRecord user = serverState.getUser(session.username);
        Integer remainingCredits = (user != null) ? user.credits : 0;

        return new ApiModels.DebugStateResponse(
                session.state,
                session.currentInstructionIndex,
                session.cycles,
                session.variables,
                outputY,
                session.error,
                session.instructions.size(),
                remainingCredits);
    }

    /**
     * Get architecture cost based on architecture type
     */
    private int getArchitectureCost(String architecture) {
        switch (architecture) {
            case "I":
                return 5;
            case "II":
                return 100;
            case "III":
                return 500;
            case "IV":
                return 1000;
            default:
                return 5;
        }
    }

    /**
     * Extract username from Authorization header token
     * 
     * @param req The HTTP request
     * @return username if authenticated, null if not authenticated
     */
    private String getUsernameFromRequest(HttpServletRequest req) {
        // Get Authorization header
        String authHeader = req.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extract token (skip "Bearer " prefix)
            String token = authHeader.substring(7);

            // Get username from token
            String username = serverState.getUsernameFromToken(token);

            if (username != null && !username.trim().isEmpty()) {
                return username;
            }
        }

        // No valid authentication found
        return null;
    }
}
