package com.semulator.server.api;

import com.semulator.engine.model.*;
import com.semulator.engine.model.RunTarget;
import com.semulator.engine.exec.ExecutionContext;
import com.semulator.server.model.ApiModels;
import com.semulator.server.state.ServerState;
import com.semulator.server.util.ServletUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

/**
 * Run endpoints
 * POST /api/run/prepare
 * POST /api/run/start
 * GET /api/run/status
 * POST /api/run/cancel
 */
@WebServlet(name = "RunServlet", urlPatterns = { "/api/run/*" })
public class RunServlet extends HttpServlet {

    private final ServerState serverState = ServerState.getInstance();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/prepare")) {
            handlePrepare(req, resp);
        } else if (pathInfo != null && pathInfo.equals("/start")) {
            handleStart(req, resp);
        } else if (pathInfo != null && pathInfo.equals("/cancel")) {
            handleCancel(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/status")) {
            handleStatus(req, resp);
        } else {
            ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    private void handlePrepare(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.RunPrepareRequest request = ServletUtils.parseJson(req, ApiModels.RunPrepareRequest.class);

            // Validate request
            if (request.target == null || request.arch == null || request.inputs == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Missing required fields");
                return;
            }

            // Check if architecture is supported
            boolean supported = isArchitectureSupported(request.arch);
            List<String> unsupported = supported ? List.of()
                    : List.of("Architecture " + request.arch + " not supported");

            // Calculate instruction counts by architecture based on actual program
            Map<String, Integer> instructionCountsByArch = calculateInstructionCountsByArchitecture(
                    request.target, request.degree);

            // Calculate architecture cost (only upfront cost)
            // Execution will be charged per instruction based on actual cycles
            int archCost = getArchitectureCost(request.arch);

            // Get average execution cost for this program (if available)
            double avgExecutionCost = serverState.getProgramAverageCost(request.target.getName());

            List<String> messages = new ArrayList<>();
            messages.add("Run prepared successfully");
            messages.add("Architecture cost: " + archCost + " credits (charged upfront)");
            messages.add("Execution cost: charged per instruction based on cycles");
            if (avgExecutionCost > 0) {
                messages.add("Average execution cost: " + String.format("%.2f", avgExecutionCost)
                        + " credits (based on history)");
                messages.add("Total estimated: " + (archCost + (int) Math.ceil(avgExecutionCost)) + " credits");
            } else {
                messages.add("No history available - first run of this program");
            }
            if (request.degree > 0) {
                messages.add("Expansion degree: " + request.degree);
            }

            // Return architecture cost as the estimated cost (for backwards compatibility)
            int estimatedCost = archCost;

            ApiModels.RunPrepareResponse response = new ApiModels.RunPrepareResponse(
                    supported,
                    unsupported,
                    instructionCountsByArch,
                    estimatedCost,
                    messages);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Prepare failed: " + e.getMessage());
        }
    }

    private void handleStart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.RunStartRequest request = ServletUtils.parseJson(req, ApiModels.RunStartRequest.class);

            // Validate request
            if (request.target == null || request.arch == null || request.inputs == null || request.username == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Missing required fields");
                return;
            }

            // Check user exists
            ServerState.UserRecord user = serverState.getUser(request.username);
            if (user == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "User not found");
                return;
            }

            // Calculate architecture cost (execution cost will be charged per instruction)
            int archCost = getArchitectureCost(request.arch);

            // Get average cost for this program (if available)
            double avgCost = serverState.getProgramAverageCost(request.target.getName());
            int estimatedTotalCost = archCost + (int) Math.ceil(avgCost);

            // Check if user has sufficient credits for architecture cost + estimated
            // execution cost
            // (Actual execution costs will be deducted per instruction based on cycles)
            if (user.credits < estimatedTotalCost) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "INSUFFICIENT_CREDITS",
                        "Insufficient credits. Required: " + estimatedTotalCost +
                                " (Architecture: " + archCost + " + Estimated Execution: " + (int) Math.ceil(avgCost)
                                + "), Available: "
                                + user.credits);
                return;
            }

            // Charge architecture cost immediately
            if (!serverState.deductCredits(request.username, archCost)) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_PAYMENT_REQUIRED, "INSUFFICIENT_CREDITS",
                        "Failed to charge architecture cost");
                return;
            }

            // Generate run ID
            String runId = "run_" + request.username + "_" + System.currentTimeMillis();

            // Convert inputs map to Inputs object
            Inputs inputs = new Inputs(request.inputs);

            // Convert architecture string to enum
            Architecture architecture;
            try {
                architecture = Architecture.valueOf(request.arch);
            } catch (IllegalArgumentException e) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "INVALID_ARCHITECTURE",
                        "Invalid architecture: " + request.arch);
                return;
            }

            // Create run session
            ServerState.RunSession session = serverState.createRunSession(
                    runId, request.username, request.target, architecture, request.degree, inputs);

            // Start execution in background (simplified)
            startExecutionAsync(session);

            ApiModels.RunStartResponse response = new ApiModels.RunStartResponse(runId);
            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Start failed: " + e.getMessage());
        }
    }

    private void handleStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String runId = req.getParameter("runId");

            if (runId == null || runId.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "runId parameter is required");
                return;
            }

            ServerState.RunSession session = serverState.getRunSession(runId);
            if (session == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Run not found");
                return;
            }

            // Get current user credits
            ServerState.UserRecord user = serverState.getUser(session.username);
            Integer remainingCredits = (user != null) ? user.credits : 0;

            ApiModels.RunStatusResponse response = new ApiModels.RunStatusResponse(
                    session.state,
                    session.cycles,
                    session.instrByArch,
                    session.pointer,
                    session.outputY,
                    session.error,
                    remainingCredits);

            ServletUtils.writeJson(resp, response);

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Status failed: " + e.getMessage());
        }
    }

    private void handleCancel(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ApiModels.RunCancelRequest request = ServletUtils.parseJson(req, ApiModels.RunCancelRequest.class);

            if (request.runId == null || request.runId.trim().isEmpty()) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "runId is required");
                return;
            }

            ServerState.RunSession session = serverState.getRunSession(request.runId);
            if (session == null) {
                ServletUtils.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Run not found");
                return;
            }

            // Cancel the run
            session.state = "STOPPED";
            session.error = "Cancelled by user";

            ServletUtils.writeJson(resp, Map.of("success", true, "message", "Run cancelled"));

        } catch (Exception e) {
            ServletUtils.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL",
                    "Cancel failed: " + e.getMessage());
        }
    }

    // Helper methods
    private boolean isArchitectureSupported(String arch) {
        return arch != null && (arch.equals("I") || arch.equals("II") || arch.equals("III") || arch.equals("IV"));
    }

    private int getArchitectureCost(String arch) {
        switch (arch) {
            case "I":
                return 5;
            case "II":
                return 100;
            case "III":
                return 500;
            case "IV":
                return 1000;
            default:
                return 5;
        }
    }

    /**
     * Calculate the count of instructions supported by each architecture level.
     * Counts how many instructions require each architecture (I, II, III, IV).
     */
    private Map<String, Integer> calculateInstructionCountsByArchitecture(RunTarget target, int degree) {
        // Reset counts
        Map<String, Integer> counts = new HashMap<>();
        counts.put("I", 0);
        counts.put("II", 0);
        counts.put("III", 0);
        counts.put("IV", 0);

        try {
            // Get the program or function
            SProgram program = null;
            ExpansionResult expansion = null;

            if (target.getType() == RunTarget.Type.PROGRAM) {
                program = serverState.getProgram(target.getName());
                if (program == null) {
                    return counts; // Return zeros if program not found
                }
                // Expand the entire program to the requested degree
                expansion = program.expandToDegree(degree);

            } else if (target.getType() == RunTarget.Type.FUNCTION) {
                program = serverState.getFunction(target.getName());
                if (program == null) {
                    return counts; // Return zeros if function not found
                }
                // Expand ONLY the specific function to the requested degree (not the entire
                // parent program!)
                expansion = program.expandFunctionToDegree(target.getName(), degree);
            }

            if (expansion == null) {
                return counts;
            }

            List<SInstruction> instructions = expansion.instructions();

            // Count commands by architecture - same logic as client
            // Architecture hierarchy: I < II < III < IV
            for (SInstruction instruction : instructions) {
                String arch = getArchitectureForInstruction(instruction.getName());

                if ("I".equals(arch)) {
                    counts.put("I", counts.get("I") + 1);
                } else if ("II".equals(arch)) {
                    counts.put("II", counts.get("II") + 1);
                } else if ("III".equals(arch)) {
                    counts.put("III", counts.get("III") + 1);
                } else if ("IV".equals(arch)) {
                    counts.put("IV", counts.get("IV") + 1);
                }
            }

        } catch (Exception e) {
            // Error calculating instruction counts
        }

        return counts;
    }

    /**
     * Determine which architecture level is required for a given instruction.
     * Based on the instruction degree mapping from SProgramImpl.
     */
    private String getArchitectureForInstruction(String instructionName) {
        switch (instructionName) {
            // Degree 0 - Architecture I (Basic instructions)
            case "NEUTRAL":
            case "INCREASE":
            case "DECREASE":
            case "JUMP_NOT_ZERO":
                return "I";

            // Degree 1 - Architecture II
            case "ZERO":
            case "ASSIGNC":
            case "CONSTANT_ASSIGNMENT":
            case "ZERO_VARIABLE":
            case "GOTO":
            case "GOTO_LABEL":
                return "II";

            // Degree 2 - Architecture III
            case "ASSIGN":
            case "ASSIGNMENT":
            case "ASSIGN_VARIABLE":
            case "IFZ":
            case "JUMP_ZERO":
            case "IFEQC":
            case "JUMP_EQUAL_CONSTANT":
            case "IFEQV":
            case "JUMP_EQUAL_VARIABLE":
                return "III";

            // Degree 3+ - Architecture IV
            case "QUOTE":
            case "JUMP_EQUAL_FUNCTION":
                return "IV";

            default:
                // Default to Architecture I for unknown instructions
                return "I";
        }
    }

    private Variable createVariableFromName(String varName) {
        if (varName.equals("y")) {
            return new VariableImpl(VariableType.RESULT, 0);
        } else if (varName.startsWith("x")) {
            try {
                int number = Integer.parseInt(varName.substring(1));
                return new VariableImpl(VariableType.INPUT, number);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid input variable name: " + varName);
            }
        } else if (varName.startsWith("z")) {
            try {
                int number = Integer.parseInt(varName.substring(1));
                return new VariableImpl(VariableType.WORK, number);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid work variable name: " + varName);
            }
        } else {
            throw new IllegalArgumentException("Unknown variable type: " + varName);
        }
    }

    private void startExecutionAsync(ServerState.RunSession session) {
        // Execute the S-program using the engine
        new Thread(() -> {
            try {
                // Get the program or function from server state
                SProgram program = null;
                List<SInstruction> instructions = null;
                boolean isFunction = false;

                if (session.target.getType() == RunTarget.Type.PROGRAM) {
                    program = serverState.getProgram(session.target.getName());
                    if (program == null) {
                        session.state = "ERROR";
                        session.error = "Program not found: " + session.target.getName();
                        return;
                    }
                    // For programs, expand and get all instructions
                    ExpansionResult expansion = program.expandToDegree(session.degree);
                    instructions = expansion.instructions();

                } else if (session.target.getType() == RunTarget.Type.FUNCTION) {
                    // For functions, get the parent program and extract ONLY the function's
                    // instructions
                    program = serverState.getFunction(session.target.getName());
                    if (program == null) {
                        session.state = "ERROR";
                        session.error = "Function not found: " + session.target.getName();
                        return;
                    }

                    // Extract the function's instructions from the parent program
                    if (program instanceof com.semulator.engine.parse.SProgramImpl) {
                        com.semulator.engine.parse.SProgramImpl programImpl = (com.semulator.engine.parse.SProgramImpl) program;
                        Map<String, List<SInstruction>> functions = programImpl.getFunctions();
                        instructions = functions.get(session.target.getName());

                        if (instructions == null) {
                            session.state = "ERROR";
                            session.error = "Function instructions not found: " + session.target.getName();
                            return;
                        }
                        isFunction = true;
                    } else {
                        session.state = "ERROR";
                        session.error = "Cannot extract function from program";
                        return;
                    }

                } else {
                    session.state = "ERROR";
                    session.error = "Invalid target type: " + session.target.getType();
                    return;
                }

                // Create execution context with input variables
                ExecutionContext context = new ExecutionContext() {
                    private final Map<String, Long> variables = new HashMap<>();

                    @Override
                    public long getVariableValue(Variable v) {
                        return variables.getOrDefault(v.getRepresentation(), 0L);
                    }

                    @Override
                    public void updateVariable(Variable v, long value) {
                        variables.put(v.getRepresentation(), value);
                    }
                };

                // Set input variables using Variable objects
                for (Map.Entry<String, Long> entry : session.inputs.getAll().entrySet()) {
                    Variable var = createVariableFromName(entry.getKey());
                    context.updateVariable(var, entry.getValue());
                }

                // Build label-to-instruction map for efficient jumping
                Map<String, Integer> labelToIndex = new HashMap<>();
                for (int i = 0; i < instructions.size(); i++) {
                    SInstruction instruction = instructions.get(i);
                    Label label = instruction.getLabel();
                    if (label != null && label != FixedLabel.EMPTY && label != FixedLabel.EXIT) {
                        labelToIndex.put(label.getLabel(), i);
                    }
                }

                // Execute the program manually, accumulating cycles in the server
                session.cycles = 0;
                session.creditsSpent = session.arch.getCost(); // Track actual credits spent
                int currentIndex = 0;

                while (currentIndex < instructions.size()) {
                    SInstruction currentInstruction = instructions.get(currentIndex);

                    // Get the cycle cost for this specific instruction
                    int cycleCost = currentInstruction.cycles();

                    // Check if user has enough credits for this instruction's cycle cost
                    ServerState.UserRecord user = serverState.getUser(session.username);
                    if (user == null || user.credits < cycleCost) {
                        session.state = "ERROR";
                        session.error = "Out of credits! Execution stopped at cycle " + session.cycles +
                                ". Required: " + cycleCost + ", Available: " + (user != null ? user.credits : 0);
                        break;
                    }

                    // Deduct credits based on instruction's actual cycle cost
                    if (!serverState.deductCredits(session.username, cycleCost)) {
                        session.state = "ERROR";
                        session.error = "Failed to deduct credits for cycle";
                        break;
                    }

                    // Track credits spent
                    session.creditsSpent += cycleCost;

                    // Accumulate cycles for this instruction (same as debug mode)
                    session.cycles += cycleCost;

                    // Execute the instruction
                    Label nextLabel = currentInstruction.execute(context);

                    // Determine next instruction
                    if (nextLabel == null || nextLabel == FixedLabel.EMPTY) {
                        // Continue to next instruction
                        currentIndex++;
                    } else if (nextLabel == FixedLabel.EXIT) {
                        // Exit execution
                        break;
                    } else {
                        // Jump to labeled instruction
                        String labelName = nextLabel.getLabel();
                        Integer targetIndex = labelToIndex.get(labelName);
                        if (targetIndex != null) {
                            currentIndex = targetIndex;
                        } else {
                            // Label not found, continue to next
                            currentIndex++;
                        }
                    }
                }

                // Get output value (y variable)
                Variable yVar = new VariableImpl(VariableType.RESULT, 0);
                session.outputY = context.getVariableValue(yVar);
                session.pointer = currentIndex;
                session.state = "FINISHED";
                session.instrByArch.put(session.arch.toString(), session.cycles);

                // Collect all final variable values for history
                Map<String, Long> finalVariables = new HashMap<>();
                // Extract all variables from the context
                if (context instanceof ExecutionContext) {
                    // Get all variables by trying common patterns
                    for (int i = 1; i <= 10; i++) {
                        try {
                            Variable xVar = new VariableImpl(VariableType.INPUT, i);
                            long value = context.getVariableValue(xVar);
                            if (value != 0 || session.inputs.getAll().containsKey("x" + i)) {
                                finalVariables.put("x" + i, value);
                            }
                        } catch (Exception e) {
                            // Variable doesn't exist, skip
                        }
                    }
                    finalVariables.put("y", session.outputY);
                    for (int i = 1; i <= 10; i++) {
                        try {
                            Variable zVar = new VariableImpl(VariableType.WORK, i);
                            long value = context.getVariableValue(zVar);
                            if (value != 0) {
                                finalVariables.put("z" + i, value);
                            }
                        } catch (Exception e) {
                            // Variable doesn't exist, skip
                        }
                    }
                }

                // Determine target type
                String targetType = session.target.getType() == RunTarget.Type.PROGRAM ? "PROGRAM" : "FUNCTION";

                // Add to history (pass both cycles for display and creditsSpent for statistics)
                serverState.addHistoryEntry(
                        session.username,
                        session.runId,
                        session.target.getName(),
                        targetType,
                        session.arch.toString(),
                        session.degree,
                        session.outputY,
                        session.cycles,
                        finalVariables,
                        session.creditsSpent);

            } catch (Exception e) {
                session.state = "ERROR";
                session.error = e.getMessage();
            }
        }).start();
    }
}
