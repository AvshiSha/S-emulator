package com.semulator.server.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.semulator.engine.model.RunTarget;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;

/**
 * Utility class for common servlet operations
 */
public class ServletUtils {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(RunTarget.class, new RunTargetDeserializer())
            .create();

    /**
     * Enable CORS headers for cross-origin requests
     */
    public static void enableCors(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }

    /**
     * Write JSON response with proper headers
     */
    public static void writeJson(HttpServletResponse resp, Object data) throws IOException {
        enableCors(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (PrintWriter writer = resp.getWriter()) {
            writer.write(gson.toJson(data));
        }
    }

    /**
     * Write error response
     */
    public static void writeError(HttpServletResponse resp, int status, String code, String message)
            throws IOException {
        enableCors(resp);
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ErrorResponse error = new ErrorResponse(code, message);

        try (PrintWriter writer = resp.getWriter()) {
            writer.write(gson.toJson(error));
        }
    }

    /**
     * Write error response with details
     */
    public static void writeError(HttpServletResponse resp, int status, String code, String message, Object details)
            throws IOException {
        enableCors(resp);
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ErrorResponse error = new ErrorResponse(code, message, details);

        try (PrintWriter writer = resp.getWriter()) {
            writer.write(gson.toJson(error));
        }
    }

    /**
     * Read request body as string
     */
    public static String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }

    /**
     * Parse JSON request body
     */
    public static <T> T parseJson(HttpServletRequest req, Class<T> clazz) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            sb.append(line);
        }
        return gson.fromJson(sb.toString(), clazz);
    }

    /**
     * Handle OPTIONS request for CORS preflight
     */
    public static void handleOptions(HttpServletResponse resp) throws IOException {
        enableCors(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Standard error response structure
     */
    public static class ErrorResponse {
        public final Error error;

        public ErrorResponse(String code, String message) {
            this.error = new Error(code, message, null);
        }

        public ErrorResponse(String code, String message, Object details) {
            this.error = new Error(code, message, details);
        }

        public static class Error {
            public final String code;
            public final String message;
            public final Object details;

            public Error(String code, String message, Object details) {
                this.code = code;
                this.message = message;
                this.details = details;
            }
        }
    }

    /**
     * Custom deserializer for RunTarget to handle string-to-enum conversion
     */
    private static class RunTargetDeserializer implements JsonDeserializer<RunTarget> {
        @Override
        public RunTarget deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            String typeString = jsonObject.get("type").getAsString();
            String name = jsonObject.get("name").getAsString();

            RunTarget.Type type;
            try {
                type = RunTarget.Type.valueOf(typeString);
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Invalid RunTarget type: " + typeString, e);
            }

            return new RunTarget(type, name);
        }
    }
}
