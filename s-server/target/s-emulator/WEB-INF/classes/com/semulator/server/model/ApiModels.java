package com.semulator.server.model;

import com.semulator.engine.model.Architecture;
import com.semulator.engine.model.Inputs;
import com.semulator.engine.model.RunTarget;

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

        public UserInfo(String username, int credits, int totalRuns, long lastActive) {
            this.username = username;
            this.credits = credits;
            this.totalRuns = totalRuns;
            this.lastActive = lastActive;
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
        public Architecture arch;
        public int degree;
        public Inputs inputs;
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
        public Architecture arch;
        public int degree;
        public Inputs inputs;
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

        public RunStatusResponse(String state, int cycles, Map<String, Integer> instrByArch,
                int pointer, Long outputY, String error) {
            this.state = state;
            this.cycles = cycles;
            this.instrByArch = instrByArch;
            this.pointer = pointer;
            this.outputY = outputY;
            this.error = error;
        }
    }

    public static class RunCancelRequest {
        public String runId;
    }

    // Debug
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

    // History
    public static class HistoryEntry {
        public String runId;
        public String programName;
        public long result;
        public int cycles;
        public long timestamp;

        public HistoryEntry(String runId, String programName, long result, int cycles, long timestamp) {
            this.runId = runId;
            this.programName = programName;
            this.result = result;
            this.cycles = cycles;
            this.timestamp = timestamp;
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
}
