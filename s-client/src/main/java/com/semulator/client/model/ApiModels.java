package com.semulator.client.model;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Objects for REST API communication
 */
public class ApiModels {

        // Auth & Users
        public record LoginRequest(String username) {
        }

        public record LoginResponse(String token, String username) {
        }

        public record UserInfo(String username, int credits, int totalRuns, long lastActive) {
        }

        public record UsersResponse(List<UserInfo> users, long version, boolean full) {
        }

        public record TopupRequest(int amount) {
        }

        public record TopupResponse(int newBalance) {
        }

        // Catalog & Upload
        public record UploadRequest(String filename, String content) {
                // Upload request for XML files
        }

        public record LoadResult(boolean success, String message, String programName, int instructionCount) {
        }

        public record ProgramSummary(String name, String owner, int instructionCount, String lastModified) {
        }

        public record FunctionSummary(String name, String owner, int instructionCount, List<String> parameters) {
        }

        public record ProgramsResponse(List<ProgramSummary> programs, String version, boolean full) {
        }

        public record FunctionsResponse(List<FunctionSummary> functions, String version, boolean full) {
        }

        // Server response models (matching server-side ApiModels)
        public record ProgramInfo(String name, String uploadedBy, int instructionCount, int maxDegree, int runs,
                        double avgCost, List<String> functions) {
        }

        public record ProgramWithInstructions(String name, List<Object> instructions, int maxDegree,
                        List<String> functions) {
                // Note: instructions are returned as Object because Gson deserializes them as
                // Maps
        }

        public record FunctionInfo(String name, String parentProgram, String uploadedBy, int instructionCount,
                        int maxDegree) {
        }

        // Runs
        public record RunTarget(String type, String name) {
                public enum Type {
                        PROGRAM, FUNCTION
                }
        }

        public record PrepareRequest(RunTarget target, String arch, int degree, Map<String, Integer> inputs) {
        }

        public record PrepareResponse(
                        boolean supported,
                        List<String> unsupported,
                        Map<String, Integer> instructionCountsByArch,
                        int estimatedCost,
                        List<String> messages) {
        }

        public record StartRequest(RunTarget target, String arch, int degree, Map<String, Integer> inputs) {
        }

        public record StartResponse(String runId) {
        }

        public record StatusResponse(
                        String state,
                        int cycles,
                        Map<String, Integer> instrByArch,
                        int pointer,
                        Integer outputY,
                        String error) {
        }

        public record CancelRequest(String runId) {
        }

        // Debug
        public record DebugRequest(String runId) {
        }

        public record DebugResponse(String message, boolean success) {
        }

        // History
        public record RunHistory(
                        String runId,
                        String username,
                        String targetName,
                        String targetType,
                        String architecture,
                        int degree,
                        String state,
                        int cycles,
                        int cost,
                        String timestamp) {
        }

        public record HistoryResponse(List<RunHistory> runs, String version, boolean full) {
        }

        // Error
        public record ErrorResponse(ErrorDetail error) {
        }

        public record ErrorDetail(String code, String message, Map<String, Object> details) {
        }

        // Delta responses
        public record DeltaResponse<T>(
                        long version,
                        boolean full,
                        List<T> items,
                        DeltaChanges<T> delta) {
        }

        public record DeltaChanges<T>(
                        List<T> added,
                        List<T> updated,
                        List<T> removed) {
        }

        // History Chain
        public record HistoryChainResponse(
                        boolean success,
                        String message,
                        List<HistoryChainItem> chain,
                        int totalInstructions,
                        int currentDegree) {
        }

        public record HistoryChainItem(
                        int rowNumber,
                        String commandType,
                        String label,
                        String instructionText,
                        int cycles,
                        String variable,
                        String architecture,
                        String instructionName,
                        int degree) {
        }
}
