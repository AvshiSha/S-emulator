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
import java.util.Map;

/**
 * Debug endpoints
 * POST /api/debug/step
 * POST /api/debug/stepOver
 * POST /api/debug/stop
 */
@WebServlet(name = "DebugServlet", urlPatterns = { "/api/debug/*" })
public class DebugServlet extends HttpServlet {

    private final ServerState serverState = ServerState.getInstance();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/step")) {
            handleStep(req, resp);
        } else if (pathInfo != null && pathInfo.equals("/stepOver")) {
            handleStepOver(req, resp);
        } else if (pathInfo != null && pathInfo.equals("/stop")) {
            handleStop(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    private void handleStep(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.DebugRequest request = ServletUtils.parseJson(req, ApiModels.DebugRequest.class);

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

            if (!"RUNNING".equals(session.state)) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Run is not in RUNNING state");
                return;
            }

            // Simulate step execution
            session.cycles++;
            session.pointer++;

            // Check if execution is complete
            if (session.pointer >= 100) { // Simplified completion check
                session.state = "FINISHED";
                session.outputY = (long) (Math.random() * 1000);

                // Add to history
                serverState.addHistoryEntry(
                        session.username,
                        session.runId,
                        session.target.getName(),
                        session.outputY,
                        session.cycles);
            }

            ApiModels.DebugResponse response = new ApiModels.DebugResponse(
                    session.state,
                    session.cycles,
                    session.pointer,
                    session.outputY,
                    session.error);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Step failed: " + e.getMessage());
        }
    }

    private void handleStepOver(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.DebugRequest request = ServletUtils.parseJson(req, ApiModels.DebugRequest.class);

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

            if (!"RUNNING".equals(session.state)) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Run is not in RUNNING state");
                return;
            }

            // Simulate step over execution (execute multiple steps)
            int stepsToExecute = 5; // Simplified: step over executes 5 instructions
            session.cycles += stepsToExecute;
            session.pointer += stepsToExecute;

            // Check if execution is complete
            if (session.pointer >= 100) {
                session.state = "FINISHED";
                session.outputY = (long) (Math.random() * 1000);

                // Add to history
                serverState.addHistoryEntry(
                        session.username,
                        session.runId,
                        session.target.getName(),
                        session.outputY,
                        session.cycles);
            }

            ApiModels.DebugResponse response = new ApiModels.DebugResponse(
                    session.state,
                    session.cycles,
                    session.pointer,
                    session.outputY,
                    session.error);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Step over failed: " + e.getMessage());
        }
    }

    private void handleStop(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.DebugRequest request = ServletUtils.parseJson(req, ApiModels.DebugRequest.class);

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

            // Stop the execution
            session.state = "STOPPED";
            session.error = "Stopped by user";

            ServletUtils.writeJson(resp, Map.of("success", true, "message", "Run stopped"));

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Stop failed: " + e.getMessage());
        }
    }
}
