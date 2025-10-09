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
 * History endpoints
 * GET /api/history?user={username}
 */
@WebServlet(name = "HistoryServlet", urlPatterns = { "/api/history" })
public class HistoryServlet extends HttpServlet {

    private final ServerState serverState = ServerState.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleGetHistory(req, resp);
    }

    private void handleGetHistory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String username = req.getParameter("user");

            if (username == null || username.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "user parameter is required");
                return;
            }

            // Check for delta/polling support
            String sinceVersionParam = req.getParameter("sinceVersion");
            long sinceVersion = sinceVersionParam != null ? Long.parseLong(sinceVersionParam) : 0;
            long currentVersion = serverState.getCurrentVersion();

            if (sinceVersion > 0 && sinceVersion >= currentVersion) {
                // No changes since requested version
                ApiModels.DeltaResponse<ApiModels.HistoryEntry> response = new ApiModels.DeltaResponse<>(currentVersion,
                        false, List.of());
                ServletUtils.writeJson(resp, response);
                return;
            }

            List<ApiModels.HistoryEntry> history = serverState.getUserHistory(username);

            if (sinceVersion > 0) {
                // Delta response - for simplicity, return all entries
                // In a real implementation, you'd track changes since the given version
                ApiModels.DeltaResponse<ApiModels.HistoryEntry> response = new ApiModels.DeltaResponse<>(
                        currentVersion,
                        false,
                        new ApiModels.DeltaData<>(history, List.of(), List.of()));
                ServletUtils.writeJson(resp, response);
            } else {
                // Full response
                ApiModels.DeltaResponse<ApiModels.HistoryEntry> response = new ApiModels.DeltaResponse<>(
                        currentVersion,
                        true,
                        history);
                ServletUtils.writeJson(resp, response);
            }

        } catch (NumberFormatException e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    "Invalid sinceVersion parameter");
        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Failed to get history: " + e.getMessage());
        }
    }
}
