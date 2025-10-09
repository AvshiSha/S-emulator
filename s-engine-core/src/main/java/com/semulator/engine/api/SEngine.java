package com.semulator.engine.api;

import com.semulator.engine.model.*;

import java.util.List;
import java.util.Map;

/**
 * Public API for the S-Emulator Engine.
 * This is the stable interface that server and client components will use.
 */
public interface SEngine {

    /**
     * Get a summary of all loaded programs and functions with statistics
     */
    CatalogSummary summarize();

    /**
     * Load a program from XML content
     * 
     * @param xmlContent    The XML program definition
     * @param ownerUsername Username of the program owner
     * @return Result of the load operation
     */
    LoadResult loadProgram(String xmlContent, String ownerUsername);

    /**
     * Prepare a run without executing (validation + expansion)
     * 
     * @param target The target to run (program or function)
     * @param arch   The architecture (determines which instructions are available)
     * @param degree Expansion degree
     * @param inputs Input values for the run
     * @return Preparation result with expanded program
     */
    PrepareResult prepareRun(RunTarget target, Architecture arch, int degree, Inputs inputs);

    /**
     * Start an execution run
     * 
     * @param target   The target to run (program or function)
     * @param arch     The architecture
     * @param degree   Expansion degree
     * @param inputs   Input values
     * @param username User running the program
     * @return RunId for tracking this execution
     */
    RunId startRun(RunTarget target, Architecture arch, int degree, Inputs inputs, String username);

    /**
     * Get the current status of a running execution
     */
    StatusResult getStatus(RunId runId);

    /**
     * Cancel a running execution
     */
    void cancelRun(RunId runId);

    /**
     * Execute one step in debug mode
     */
    DebugResult step(RunId runId);

    /**
     * Execute one "step over" in debug mode (execute full synthetic instruction)
     */
    DebugResult stepOver(RunId runId);

    /**
     * Stop a debug session
     */
    void stop(RunId runId);

    // === Supporting Types ===

    record CatalogSummary(
            List<ProgramInfo> programs,
            Map<String, FunctionInfo> functions,
            int totalInstructions) {
    }

    record ProgramInfo(
            String name,
            int instructionCount,
            int maxDegree,
            List<String> functionsUsed) {
    }

    record FunctionInfo(
            String name,
            String userString,
            int templateDegree,
            int instructionCount) {
    }

    record LoadResult(
            boolean success,
            String programName,
            String errorMessage) {
    }

    record RunTarget(
            String programOrFunctionName,
            boolean isFunction) {
    }

    record Architecture(
            String name,
            List<String> allowedInstructions) {
        public static final Architecture BASIC = new Architecture("BASIC",
                List.of("INCREASE", "DECREASE", "NEUTRAL", "JUMP_NOT_ZERO"));
        public static final Architecture EXTENDED = new Architecture("EXTENDED",
                List.of("INCREASE", "DECREASE", "NEUTRAL", "JUMP_NOT_ZERO",
                        "ZERO_VARIABLE", "ASSIGNMENT", "GOTO_LABEL"));
    }

    record Inputs(
            List<Long> values) {
    }

    record RunId(
            String id) {
    }

    record PrepareResult(
            boolean success,
            ExpansionResult expandedProgram,
            String errorMessage) {
    }

    record StatusResult(
            RunId runId,
            String state, // RUNNING, COMPLETED, ERROR
            Long result,
            int cyclesExecuted,
            String errorMessage) {
    }

    record DebugResult(
            RunId runId,
            int currentLine,
            SInstruction currentInstruction,
            Map<Variable, Long> variableState,
            boolean isFinished) {
    }
}
