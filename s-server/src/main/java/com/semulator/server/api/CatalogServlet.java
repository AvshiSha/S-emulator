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
 * Catalog endpoints
 * GET /api/programs
 * GET /api/functions
 */
@WebServlet(name = "CatalogServlet", urlPatterns = { "/api/programs", "/api/functions" })
public class CatalogServlet extends HttpServlet {

    private final ServerState serverState = ServerState.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();

        try {
            // Check for delta/polling support
            String sinceVersionParam = req.getParameter("sinceVersion");
            long sinceVersion = sinceVersionParam != null ? Long.parseLong(sinceVersionParam) : 0;
            long currentVersion = serverState.getCurrentVersion();

            if ("/api/programs".equals(servletPath)) {
                handleGetPrograms(req, resp, sinceVersion, currentVersion);
            } else if ("/api/functions".equals(servletPath)) {
                handleGetFunctions(req, resp, sinceVersion, currentVersion);
            } else {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
            }

        } catch (NumberFormatException e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    "Invalid sinceVersion parameter");
        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Failed to get catalog: " + e.getMessage());
        }
    }

    private void handleGetPrograms(HttpServletRequest req, HttpServletResponse resp, long sinceVersion,
            long currentVersion) throws IOException {

        // Check for specific program name query parameter
        String programName = req.getParameter("name");
        if (programName != null && !programName.trim().isEmpty()) {
            // Return specific program with instructions
            handleGetSpecificProgram(req, resp, programName);
            return;
        }

        List<ApiModels.ProgramInfo> programs = serverState.getPrograms();

        if (sinceVersion > 0) {
            // Delta response
            ApiModels.DeltaResponse<ApiModels.ProgramInfo> response = new ApiModels.DeltaResponse<>(
                    currentVersion,
                    false,
                    new ApiModels.DeltaData<>(programs, List.of(), List.of()));
            ServletUtils.writeJson(resp, response);
        } else {
            // Full response
            ApiModels.DeltaResponse<ApiModels.ProgramInfo> response = new ApiModels.DeltaResponse<>(
                    currentVersion,
                    true,
                    programs);
            ServletUtils.writeJson(resp, response);
        }
    }

    private void handleGetSpecificProgram(HttpServletRequest req, HttpServletResponse resp, String programName)
            throws IOException {
        try {
            // Check for degree parameter
            String degreeParam = req.getParameter("degree");
            int degree = degreeParam != null ? Integer.parseInt(degreeParam) : 0;

            ApiModels.ProgramWithInstructions programWithInstructions = serverState
                    .getProgramWithInstructions(programName, degree);
            if (programWithInstructions != null) {
                ServletUtils.writeJson(resp, programWithInstructions);
            } else {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "PROGRAM_NOT_FOUND",
                        "Program not found: " + programName);
            }
        } catch (NumberFormatException e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    "Invalid degree parameter");
        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Failed to get program: " + e.getMessage());
        }
    }

    private void handleGetFunctions(HttpServletRequest req, HttpServletResponse resp, long sinceVersion,
            long currentVersion) throws IOException {

        // Check for specific function name query parameter
        String functionName = req.getParameter("name");
        if (functionName != null && !functionName.trim().isEmpty()) {
            // Return specific function with instructions
            handleGetSpecificFunction(req, resp, functionName);
            return;
        }

        List<ApiModels.FunctionInfo> functions = serverState.getFunctions();

        if (sinceVersion > 0) {
            // Delta response
            ApiModels.DeltaResponse<ApiModels.FunctionInfo> response = new ApiModels.DeltaResponse<>(
                    currentVersion,
                    false,
                    new ApiModels.DeltaData<>(functions, List.of(), List.of()));
            ServletUtils.writeJson(resp, response);
        } else {
            // Full response
            ApiModels.DeltaResponse<ApiModels.FunctionInfo> response = new ApiModels.DeltaResponse<>(
                    currentVersion,
                    true,
                    functions);
            ServletUtils.writeJson(resp, response);
        }
    }

    private void handleGetSpecificFunction(HttpServletRequest req, HttpServletResponse resp, String functionName)
            throws IOException {
        // System.out.println(">>> API: handleGetSpecificFunction called for: " +
        // functionName);
        try {
            // Check for degree parameter
            String degreeParam = req.getParameter("degree");
            int degree = degreeParam != null ? Integer.parseInt(degreeParam) : 0;

            // System.out.println(">>> API: Requesting function with degree: " + degree);

            // Get function with instructions from server state
            ApiModels.ProgramWithInstructions functionWithInstructions = serverState
                    .getFunctionWithInstructions(functionName, degree);
            if (functionWithInstructions != null) {
                ServletUtils.writeJson(resp, functionWithInstructions);
            } else {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "FUNCTION_NOT_FOUND",
                        "Function not found: " + functionName);
            }
        } catch (NumberFormatException e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    "Invalid degree parameter");
        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Failed to get function: " + e.getMessage());
        }
    }
}
