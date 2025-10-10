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

            // Get username from session/token (for now, use "admin" as default)
            String username = "admin";

            // Create debug session
            String sessionId = "debug_" + UUID.randomUUID().toString();
            ServerState.DebugSession session = serverState.createDebugSession(
                    sessionId, username, request.programName, request.degree, request.inputs);

            if (session == null) {
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
            e.printStackTrace();
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

            // Execute one instruction
            boolean finished = executeSingleStep(session);

            if (finished) {
                session.state = "FINISHED";
            } else {
                session.state = "PAUSED";
            }

            // Create response with updated state
            ApiModels.DebugStateResponse stateResponse = createStateResponse(session);
            ApiModels.DebugStepResponse response = new ApiModels.DebugStepResponse(
                    true, "Step executed", stateResponse);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            e.printStackTrace();
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

            // Create response with final state
            ApiModels.DebugStateResponse stateResponse = createStateResponse(session);
            ApiModels.DebugResumeResponse response = new ApiModels.DebugResumeResponse(
                    true, "Execution completed", stateResponse);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            e.printStackTrace();
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

            // Remove the debug session
            serverState.removeDebugSession(request.sessionId);

            ApiModels.DebugStopResponse response = new ApiModels.DebugStopResponse(
                    true, "Debug session stopped");

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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

        return new ApiModels.DebugStateResponse(
                session.state,
                session.currentInstructionIndex,
                session.cycles,
                session.variables,
                outputY,
                session.error,
                session.instructions.size());
    }
}
