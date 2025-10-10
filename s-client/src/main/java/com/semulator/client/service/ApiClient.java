package com.semulator.client.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;

import java.io.IOException;
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
}
