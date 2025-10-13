package com.semulator.server.model;

import com.semulator.engine.model.Architecture;
import com.semulator.engine.model.Inputs;
import com.semulator.engine.model.RunTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API request/response models for REST endpoints
 */
public class ApiModels {

    // Auth & Users
    public static class LoginRequest {
        public String username;
    }

    public static class LoginResponse {
        public String token;
        public String username;
        public int credits;

        public LoginResponse(String username, int credits) {
            this.token = "token_" + username + "_" + System.currentTimeMillis();
            this.username = username;
            this.credits = credits;
        }
    }

    public static class UserInfo {
        public String username;
        public int credits;
        public int totalRuns;
        public long lastActive;
        public int mainPrograms;
        public int subfunctions;
        public int creditsUsed;

        public UserInfo(String username, int credits, int totalRuns, long lastActive,
                int mainPrograms, int subfunctions, int creditsUsed) {
            this.username = username;
            this.credits = credits;
            this.totalRuns = totalRuns;
            this.lastActive = lastActive;
            this.mainPrograms = mainPrograms;
            this.subfunctions = subfunctions;
            this.creditsUsed = creditsUsed;
        }
    }

    public static class UsersResponse {
        public List<UserInfo> users;
        public long version;
        public boolean full;

        public UsersResponse(List<UserInfo> users, long version, boolean full) {
            this.users = users;
            this.version = version;
            this.full = full;
        }
    }

    public static class TopupRequest {
        public int amount;
    }

    public static class TopupResponse {
        public int newBalance;

        public TopupResponse(int newBalance) {
            this.newBalance = newBalance;
        }
    }

    // Upload
    public static class UploadRequest {
        public String filename;
        public String content;
    }

    public static class UploadResponse {
        public boolean success;
        public String message;
        public String programName;

        public UploadResponse(boolean success, String message, String programName) {
            this.success = success;
            this.message = message;
            this.programName = programName;
        }
    }

    public static class LoadResult {
        public boolean success;
        public String message;
        public String programName;
        public int instructionCount;

        public LoadResult(boolean success, String message, String programName, int instructionCount) {
            this.success = success;
            this.message = message;
            this.programName = programName;
            this.instructionCount = instructionCount;
        }
    }

    // Programs & Functions
    public static class ProgramInfo {
        public String name;
        public String uploadedBy;
        public int instructionCount;
        public int maxDegree;
        public int runs;
        public double avgCost;
        public List<String> functions;

        public ProgramInfo(String name, String uploadedBy, int instructionCount, int maxDegree, int runs,
                double avgCost, List<String> functions) {
            this.name = name;
            this.uploadedBy = uploadedBy;
            this.instructionCount = instructionCount;
            this.maxDegree = maxDegree;
            this.runs = runs;
            this.avgCost = avgCost;
            this.functions = functions;
        }
    }

    public static class FunctionInfo {
        public String name;
        public String parentProgram;
        public String uploadedBy;
        public int instructionCount;
        public int maxDegree;

        public FunctionInfo(String name, String parentProgram, String uploadedBy, int instructionCount, int maxDegree) {
            this.name = name;
            this.parentProgram = parentProgram;
            this.uploadedBy = uploadedBy;
            this.instructionCount = instructionCount;
            this.maxDegree = maxDegree;
        }
    }

    public static class ProgramWithInstructions {
        public String name;
        public List<InstructionDTO> instructions;
        public int maxDegree;
        public List<String> functions;

        public ProgramWithInstructions(String name, List<InstructionDTO> instructions, int maxDegree,
                List<String> functions) {
            this.name = name;
            this.instructions = instructions;
            this.maxDegree = maxDegree;
            this.functions = functions;
        }
    }

    public static class InstructionDTO {
        public int rowNumber;
        public String commandType; // "B" or "S"
        public String label;
        public String instruction; // Formatted instruction text for display
        public int cycles;
        public String variable;
        public String architecture; // "I", "II", "III", or "IV"
        public String name; // Original instruction name (for compatibility)

        public InstructionDTO(int rowNumber, String commandType, String label, String instruction,
                int cycles, String variable, String architecture, String instructionTypeName) {
            this.rowNumber = rowNumber;
            this.commandType = commandType;
            this.label = label;
            this.instruction = instruction;
            this.cycles = cycles;
            this.variable = variable;
            this.architecture = architecture;
            this.name = instructionTypeName; // Original instruction type name
        }
    }

    // Runs
    public static class RunPrepareRequest {
        public RunTarget target;
        public String arch;
        public int degree;
        public Map<String, Long> inputs;
    }

    public static class RunPrepareResponse {
        public boolean supported;
        public List<String> unsupported;
        public Map<String, Integer> instructionCountsByArch;
        public int estimatedCost;
        public List<String> messages;

        public RunPrepareResponse(boolean supported, List<String> unsupported,
                Map<String, Integer> instructionCountsByArch,
                int estimatedCost, List<String> messages) {
            this.supported = supported;
            this.unsupported = unsupported;
            this.instructionCountsByArch = instructionCountsByArch;
            this.estimatedCost = estimatedCost;
            this.messages = messages;
        }
    }

    public static class RunStartRequest {
        public RunTarget target;
        public String arch;
        public int degree;
        public Map<String, Long> inputs;
        public String username;
    }

    public static class RunStartResponse {
        public String runId;

        public RunStartResponse(String runId) {
            this.runId = runId;
        }
    }

    public static class RunStatusResponse {
        public String state;
        public int cycles;
        public Map<String, Integer> instrByArch;
        public int pointer;
        public Long outputY;
        public String error;
        public Integer remainingCredits;

        public RunStatusResponse(String state, int cycles, Map<String, Integer> instrByArch,
                int pointer, Long outputY, String error, Integer remainingCredits) {
            this.state = state;
            this.cycles = cycles;
            this.instrByArch = instrByArch;
            this.pointer = pointer;
            this.outputY = outputY;
            this.error = error;
            this.remainingCredits = remainingCredits;
        }
    }

    public static class RunCancelRequest {
        public String runId;
    }

    // Debug - Legacy (keep for compatibility)
    public static class DebugRequest {
        public String runId;
    }

    public static class DebugResponse {
        public String state;
        public int cycles;
        public int pointer;
        public Long outputY;
        public String error;

        public DebugResponse(String state, int cycles, int pointer, Long outputY, String error) {
            this.state = state;
            this.cycles = cycles;
            this.pointer = pointer;
            this.outputY = outputY;
            this.error = error;
        }
    }

    // Debug - New Debug Execution API
    public static class DebugStartRequest {
        public String programName;
        public int degree;
        public List<Long> inputs;
        public String architecture;
    }

    public static class DebugStartResponse {
        public boolean success;
        public String message;
        public String sessionId;
        public DebugStateResponse state;

        public DebugStartResponse(boolean success, String message, String sessionId, DebugStateResponse state) {
            this.success = success;
            this.message = message;
            this.sessionId = sessionId;
            this.state = state;
        }
    }

    public static class DebugStepRequest {
        public String sessionId;
    }

    public static class DebugStepResponse {
        public boolean success;
        public String message;
        public DebugStateResponse state;

        public DebugStepResponse(boolean success, String message, DebugStateResponse state) {
            this.success = success;
            this.message = message;
            this.state = state;
        }
    }

    public static class DebugResumeRequest {
        public String sessionId;
    }

    public static class DebugResumeResponse {
        public boolean success;
        public String message;
        public DebugStateResponse state;

        public DebugResumeResponse(boolean success, String message, DebugStateResponse state) {
            this.success = success;
            this.message = message;
            this.state = state;
        }
    }

    public static class DebugStopRequest {
        public String sessionId;
    }

    public static class DebugStopResponse {
        public boolean success;
        public String message;

        public DebugStopResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static class DebugStateResponse {
        public String state; // READY, RUNNING, PAUSED, FINISHED, ERROR
        public int currentInstructionIndex;
        public int cycles;
        public Map<String, Long> variables;
        public Long outputY;
        public String error;
        public int totalInstructions;
        public Integer remainingCredits;

        public DebugStateResponse(String state, int currentInstructionIndex, int cycles,
                Map<String, Long> variables, Long outputY, String error, int totalInstructions,
                Integer remainingCredits) {
            this.state = state;
            this.currentInstructionIndex = currentInstructionIndex;
            this.cycles = cycles;
            this.variables = variables;
            this.outputY = outputY;
            this.error = error;
            this.totalInstructions = totalInstructions;
            this.remainingCredits = remainingCredits;
        }
    }

    // History
    public static class HistoryEntry {
        public String runId;
        public String targetName;
        public String targetType; // "PROGRAM" or "FUNCTION"
        public String architecture; // "I", "II", "III", or "IV"
        public int degree;
        public long finalYValue;
        public int cycles;
        public long timestamp;
        public Map<String, Long> finalVariables; // All variable values at the end of execution

        public HistoryEntry(String runId, String targetName, String targetType, String architecture,
                int degree, long finalYValue, int cycles, long timestamp, Map<String, Long> finalVariables) {
            this.runId = runId;
            this.targetName = targetName;
            this.targetType = targetType;
            this.architecture = architecture;
            this.degree = degree;
            this.finalYValue = finalYValue;
            this.cycles = cycles;
            this.timestamp = timestamp;
            this.finalVariables = finalVariables != null ? finalVariables : new HashMap<>();
        }
    }

    // Delta/Polling
    public static class DeltaResponse<T> {
        public long version;
        public boolean full;
        public List<T> items;
        public DeltaData<T> delta;

        public DeltaResponse(long version, boolean full, List<T> items) {
            this.version = version;
            this.full = full;
            this.items = items;
            this.delta = null;
        }

        public DeltaResponse(long version, boolean full, DeltaData<T> delta) {
            this.version = version;
            this.full = full;
            this.items = null;
            this.delta = delta;
        }
    }

    public static class DeltaData<T> {
        public List<T> added;
        public List<T> updated;
        public List<T> removed;

        public DeltaData(List<T> added, List<T> updated, List<T> removed) {
            this.added = added;
            this.updated = updated;
            this.removed = removed;
        }
    }

    // History Chain
    public static class HistoryChainRequest {
        public String programName;
        public int instructionIndex; // Index of the selected instruction
        public int degree; // Current degree for chain tracing
    }

    public static class HistoryChainResponse {
        public boolean success;
        public String message;
        public List<HistoryChainItem> chain;
        public int totalInstructions;
        public int currentDegree;

        public HistoryChainResponse(boolean success, String message, List<HistoryChainItem> chain,
                int totalInstructions, int currentDegree) {
            this.success = success;
            this.message = message;
            this.chain = chain;
            this.totalInstructions = totalInstructions;
            this.currentDegree = currentDegree;
        }
    }

    public static class HistoryChainItem {
        public int rowNumber;
        public String commandType; // "B" or "S"
        public String label;
        public String instructionText;
        public int cycles;
        public String variable;
        public String architecture;
        public String instructionName;
        public int degree; // Which degree this instruction came from

        public HistoryChainItem(int rowNumber, String commandType, String label, String instructionText,
                int cycles, String variable, String architecture, String instructionName, int degree) {
            this.rowNumber = rowNumber;
            this.commandType = commandType;
            this.label = label;
            this.instructionText = instructionText;
            this.cycles = cycles;
            this.variable = variable;
            this.architecture = architecture;
            this.instructionName = instructionName;
            this.degree = degree;
        }
    }
}
