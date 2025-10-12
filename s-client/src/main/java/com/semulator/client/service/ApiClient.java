package com.semulator.client.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for communicating with s-server REST API
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080/s-emulator/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;
    private String authToken;

    public ApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }

    public <T> CompletableFuture<T> get(String endpoint, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request.Builder builder = new Request.Builder()
                        .url(BASE_URL + endpoint);

                if (authToken != null) {
                    builder.addHeader("Authorization", "Bearer " + authToken);
                }

                Request request = builder.build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("HTTP error: " + response.code() + " " + response.message());
                    }

                    ResponseBody body = response.body();
                    if (body == null) {
                        return null;
                    }

                    String json = body.string();
                    return gson.fromJson(json, responseType);
                }
            } catch (IOException e) {
                throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
            }
        });
    }

    public <T> CompletableFuture<T> post(String endpoint, Object body, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = body != null ? gson.toJson(body) : "{}";
                RequestBody requestBody = RequestBody.create(json, JSON);

                Request.Builder builder = new Request.Builder()
                        .url(BASE_URL + endpoint)
                        .post(requestBody);

                if (authToken != null) {
                    builder.addHeader("Authorization", "Bearer " + authToken);
                }

                Request request = builder.build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        // Try to parse error message from response body
                        String errorMessage = "HTTP error: " + response.code() + " " + response.message();
                        try {
                            ResponseBody errorBody = response.body();
                            if (errorBody != null) {
                                String errorJson = errorBody.string();
                                // Try to parse as error response
                                try {
                                    com.google.gson.JsonObject errorObj = gson.fromJson(errorJson,
                                            com.google.gson.JsonObject.class);
                                    if (errorObj.has("error") && errorObj.getAsJsonObject("error").has("message")) {
                                        errorMessage = errorObj.getAsJsonObject("error").get("message").getAsString();
                                    }
                                } catch (Exception e) {
                                    // If parsing fails, use the JSON as is
                                    if (!errorJson.trim().isEmpty()) {
                                        errorMessage = errorJson;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Fall back to default error message
                        }
                        throw new RuntimeException(errorMessage);
                    }

                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        return null;
                    }

                    String responseJson = responseBody.string();
                    return gson.fromJson(responseJson, responseType);
                }
            } catch (IOException e) {
                throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Void> post(String endpoint, Object body) {
        return post(endpoint, body, Void.class);
    }

    public <T> CompletableFuture<T> get(String endpoint, Class<T> responseType, String sinceVersion) {
        final String url = sinceVersion != null
                ? BASE_URL + endpoint + "?sinceVersion=" + sinceVersion
                : BASE_URL + endpoint;

        return CompletableFuture.supplyAsync(() -> {
            try {
                Request.Builder builder = new Request.Builder().url(url);

                if (authToken != null) {
                    builder.addHeader("Authorization", "Bearer " + authToken);
                }

                Request request = builder.build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("HTTP error: " + response.code() + " " + response.message());
                    }

                    ResponseBody body = response.body();
                    if (body == null) {
                        return null;
                    }

                    String json = body.string();
                    return gson.fromJson(json, responseType);
                }
            } catch (IOException e) {
                throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
            }
        });
    }

    // Debug API methods
    public CompletableFuture<com.semulator.client.model.ApiModels.DebugStartResponse> debugStart(
            String programName, int degree, java.util.List<Long> inputs) {
        com.semulator.client.model.ApiModels.DebugStartRequest request = new com.semulator.client.model.ApiModels.DebugStartRequest(
                programName, degree, inputs);
        return post("/debug/start", request, com.semulator.client.model.ApiModels.DebugStartResponse.class);
    }

    public CompletableFuture<com.semulator.client.model.ApiModels.DebugStepResponse> debugStep(String sessionId) {
        com.semulator.client.model.ApiModels.DebugStepRequest request = new com.semulator.client.model.ApiModels.DebugStepRequest(
                sessionId);
        return post("/debug/step", request, com.semulator.client.model.ApiModels.DebugStepResponse.class);
    }

    public CompletableFuture<com.semulator.client.model.ApiModels.DebugResumeResponse> debugResume(String sessionId) {
        com.semulator.client.model.ApiModels.DebugResumeRequest request = new com.semulator.client.model.ApiModels.DebugResumeRequest(
                sessionId);
        return post("/debug/resume", request, com.semulator.client.model.ApiModels.DebugResumeResponse.class);
    }

    public CompletableFuture<com.semulator.client.model.ApiModels.DebugStopResponse> debugStop(String sessionId) {
        com.semulator.client.model.ApiModels.DebugStopRequest request = new com.semulator.client.model.ApiModels.DebugStopRequest(
                sessionId);
        return post("/debug/stop", request, com.semulator.client.model.ApiModels.DebugStopResponse.class);
    }

    public CompletableFuture<com.semulator.client.model.ApiModels.DebugStateResponse> debugGetState(String sessionId) {
        return get("/debug/state?sessionId=" + sessionId,
                com.semulator.client.model.ApiModels.DebugStateResponse.class);
    }

    // Run API Methods
    public CompletableFuture<com.semulator.client.model.ApiModels.RunPrepareResponse> runPrepare(
            String targetType, String targetName, String architecture, int degree, Map<String, Long> inputs) {
        com.semulator.client.model.ApiModels.RunTarget target = new com.semulator.client.model.ApiModels.RunTarget(
                targetType, targetName);
        com.semulator.client.model.ApiModels.RunPrepareRequest request = new com.semulator.client.model.ApiModels.RunPrepareRequest(
                target, architecture, degree, inputs);
        return post("/run/prepare", request, com.semulator.client.model.ApiModels.RunPrepareResponse.class);
    }

    public CompletableFuture<com.semulator.client.model.ApiModels.RunStartResponse> runStart(
            String targetType, String targetName, String architecture, int degree, Map<String, Long> inputs,
            String username) {
        com.semulator.client.model.ApiModels.RunTarget target = new com.semulator.client.model.ApiModels.RunTarget(
                targetType, targetName);
        com.semulator.client.model.ApiModels.RunStartRequest request = new com.semulator.client.model.ApiModels.RunStartRequest(
                target, architecture, degree, inputs, username);
        return post("/run/start", request, com.semulator.client.model.ApiModels.RunStartResponse.class);
    }

    public CompletableFuture<com.semulator.client.model.ApiModels.RunStatusResponse> runGetStatus(String runId) {
        return get("/run/status?runId=" + runId, com.semulator.client.model.ApiModels.RunStatusResponse.class);
    }

    public CompletableFuture<com.semulator.client.model.ApiModels.RunCancelResponse> runCancel(String runId) {
        com.semulator.client.model.ApiModels.RunCancelRequest request = new com.semulator.client.model.ApiModels.RunCancelRequest(
                runId);
        return post("/run/cancel", request, com.semulator.client.model.ApiModels.RunCancelResponse.class);
    }
}
