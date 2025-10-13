package com.semulator.server.api;

import com.semulator.engine.model.SProgram;
import com.semulator.server.model.ApiModels;
import com.semulator.server.state.ServerState;
import com.semulator.server.util.ServletUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Upload endpoints
 * POST /api/upload - Upload XML program
 */
@WebServlet(name = "UploadServlet", urlPatterns = { "/api/upload" })
public class UploadServlet extends HttpServlet {

    private final ServerState serverState = ServerState.getInstance();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleUpload(req, resp);
    }

    private void handleUpload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Parse JSON request body
            String jsonBody = ServletUtils.readRequestBody(req);

            if (jsonBody == null || jsonBody.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Request body is required");
                return;
            }

            // Parse the UploadRequest JSON
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.semulator.server.model.ApiModels.UploadRequest uploadRequest = gson.fromJson(jsonBody,
                    com.semulator.server.model.ApiModels.UploadRequest.class);

            if (uploadRequest.filename == null || uploadRequest.content == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "filename and content are required");
                return;
            }

            String xmlContent = uploadRequest.content;

            // Debug: Log the first 200 characters of the XML content

            // Debug: Check for BOM or invisible characters at the beginning
            byte[] bytes = xmlContent.getBytes("UTF-8");

            // Debug: Check if XML starts with BOM
            if (xmlContent.startsWith("\uFEFF")) {
                xmlContent = xmlContent.substring(1); // Remove BOM
            }

            // Extract username from token (if available)
            String username = "anonymous";
            String authHeader = req.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                username = serverState.getUsernameFromToken(token);
                if (username == null) {
                    username = "anonymous";
                }
            }

            // Load program
            SProgram program = serverState.loadProgram(xmlContent, username);

            ApiModels.LoadResult response = new ApiModels.LoadResult(
                    true,
                    "Program loaded successfully",
                    program.getName(),
                    program.getInstructions().size());

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ApiModels.LoadResult response = new ApiModels.LoadResult(
                    false,
                    "Failed to load program: " + e.getMessage(),
                    null,
                    0);
            ServletUtils.writeJson(resp, response);
        }
    }
}
