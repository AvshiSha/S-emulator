package com.semulator.server.state;

import com.semulator.engine.model.*;
import com.semulator.engine.parse.SProgramImpl;
import com.semulator.engine.exec.ExecutionContext;
import com.semulator.engine.exec.ProgramExecutor;
import com.semulator.engine.exec.ProgramExecutorImpl;
import com.semulator.server.model.ApiModels;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory server state management
 * Handles users, programs, runs, and version tracking
 */
public class ServerState {

    private static final ServerState INSTANCE = new ServerState();

    // Version clock for delta/polling
    private final AtomicLong versionClock = new AtomicLong(1);

    // User management
    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();
    private final Map<String, String> tokens = new ConcurrentHashMap<>(); // token -> username

    // Program catalog
    private final Map<String, SProgram> programs = new ConcurrentHashMap<>();
    private final Map<String, SProgram> functions = new ConcurrentHashMap<>();

    // Program metadata tracking
    private final Map<String, String> programOwners = new ConcurrentHashMap<>(); // programName -> ownerUsername
    private final Map<String, String> functionOwners = new ConcurrentHashMap<>(); // functionName -> ownerUsername
    private final Map<String, String> functionParentPrograms = new ConcurrentHashMap<>(); // functionName ->
                                                                                          // parentProgramName
    private final Map<String, Integer> programRunCounts = new ConcurrentHashMap<>(); // programName -> runCount
    private final Map<String, Double> programAvgCosts = new ConcurrentHashMap<>(); // programName -> avgCost

    // Run sessions
    private final Map<String, RunSession> runSessions = new ConcurrentHashMap<>();

    // History tracking
    private final Map<String, List<ApiModels.HistoryEntry>> userHistory = new ConcurrentHashMap<>();

    private ServerState() {
        // Initialize with admin user
        users.put("admin", new UserRecord("admin", 1000, 0, System.currentTimeMillis()));
    }

    public static ServerState getInstance() {
        return INSTANCE;
    }

    // Version management
    public long getCurrentVersion() {
        return versionClock.get();
    }

    public long incrementVersion() {
        return versionClock.incrementAndGet();
    }

    // User management
    public UserRecord getUser(String username) {
        return users.get(username);
    }

    public UserRecord createUser(String username, int initialCredits) {
        if (users.containsKey(username)) {
            return null; // User already exists
        }

        UserRecord user = new UserRecord(username, initialCredits, 0, System.currentTimeMillis());
        users.put(username, user);
        userHistory.put(username, new ArrayList<>());
        incrementVersion();

        // Broadcast user update to all connected clients
        try {
            com.semulator.server.realtime.UserUpdateServer.broadcastUserUpdate();
        } catch (Exception e) {
            System.err.println("Error broadcasting user update: " + e.getMessage());
        }

        return user;
    }

    public boolean updateUserCredits(String username, int newCredits) {
        UserRecord user = users.get(username);
        if (user != null) {
            user.credits = newCredits;
            user.lastActive = System.currentTimeMillis();
            incrementVersion();

            // Broadcast user update to all connected clients
            try {
                com.semulator.server.realtime.UserUpdateServer.broadcastUserUpdate();
            } catch (Exception e) {
                System.err.println("Error broadcasting user update: " + e.getMessage());
            }

            return true;
        }
        return false;
    }

    public boolean deductCredits(String username, int amount) {
        UserRecord user = users.get(username);
        if (user != null && user.credits >= amount) {
            user.credits -= amount;
            user.creditsUsed += amount; // Track credits used
            user.lastActive = System.currentTimeMillis();
            incrementVersion();

            // Broadcast user update to all connected clients
            try {
                com.semulator.server.realtime.UserUpdateServer.broadcastUserUpdate();
            } catch (Exception e) {
                System.err.println("Error broadcasting user update: " + e.getMessage());
            }

            return true;
        }
        return false;
    }

    public List<ApiModels.UserInfo> getAllUsers() {
        // Only return users who have active tokens (are currently connected)
        return users.values().stream()
                .filter(u -> hasActiveToken(u.username))
                .map(u -> new ApiModels.UserInfo(u.username, u.credits, u.totalRuns, u.lastActive,
                        u.mainPrograms, u.subfunctions, u.creditsUsed))
                .toList();
    }

    public boolean hasActiveToken(String username) {
        return tokens.values().stream().anyMatch(username::equals);
    }

    // Token management
    public String createToken(String username) {
        String token = "token_" + username + "_" + System.currentTimeMillis();
        tokens.put(token, username);
        return token;
    }

    public String getUsernameFromToken(String token) {
        return tokens.get(token);
    }

    public boolean removeToken(String token) {
        return tokens.remove(token) != null;
    }

    // Program management
    public SProgram loadProgram(String xmlContent, String ownerUsername) throws Exception {
        try {
            // Create program instance
            SProgramImpl program = new SProgramImpl("LoadedProgram_" + System.currentTimeMillis());

            // Validate XML content directly
            String validation = program.validateXmlContent(xmlContent);
            if (!"Valid".equals(validation)) {
                throw new Exception("Invalid XML: " + validation);
            }

            // Load the program from XML content
            String programName = program.loadFromXmlContent(xmlContent);

            // Set the program name from XML
            if (programName != null && !programName.trim().isEmpty()) {
                // Create a new instance with the correct name
                SProgramImpl namedProgram = new SProgramImpl(programName);
                namedProgram.loadFromXmlContent(xmlContent);
                program = namedProgram;
            }

            // Store the program
            programs.put(program.getName(), program);

            // Track program metadata
            programOwners.put(program.getName(), ownerUsername);
            programRunCounts.put(program.getName(), 0);
            programAvgCosts.put(program.getName(), 0.0);

            // Extract and store functions if any
            int functionCount = 0;
            if (program instanceof SProgramImpl) {
                SProgramImpl impl = (SProgramImpl) program;
                Map<String, List<SInstruction>> programFunctions = impl.getFunctions();
                functionCount = programFunctions.size();

                for (String funcName : programFunctions.keySet()) {
                    // Create a function program with only the function's instructions
                    SProgramImpl functionProgram = new SProgramImpl(funcName);

                    // Get the function's specific instructions
                    List<SInstruction> functionInstructions = programFunctions.get(funcName);

                    // Add only the function's instructions to the function program
                    for (SInstruction instruction : functionInstructions) {
                        functionProgram.addInstruction(instruction);
                    }

                    // Copy function user strings if any
                    Map<String, String> functionUserStrings = impl.getFunctionUserStrings();
                    if (functionUserStrings.containsKey(funcName)) {
                        // Note: We can't directly set user strings, but this preserves the function
                        // name
                        // The function name itself is preserved in the SProgramImpl constructor
                    }

                    functions.put(funcName, functionProgram);

                    // Track function metadata
                    functionOwners.put(funcName, ownerUsername);
                    functionParentPrograms.put(funcName, program.getName());
                }
            }

            // Update user statistics: increment mainPrograms by 1 and subfunctions by
            // function count
            UserRecord user = users.get(ownerUsername);
            if (user != null) {
                user.mainPrograms++;
                user.subfunctions += functionCount;
                user.lastActive = System.currentTimeMillis();
                System.out.println("Updated user statistics for " + ownerUsername +
                        ": mainPrograms=" + user.mainPrograms + ", subfunctions=" + user.subfunctions);
            }

            incrementVersion();

            // Broadcast program and user updates to all connected clients
            try {
                com.semulator.server.realtime.UserUpdateServer.broadcastProgramUpdate();
                com.semulator.server.realtime.UserUpdateServer.broadcastUserUpdate();
            } catch (Exception e) {
                System.err.println("Error broadcasting updates: " + e.getMessage());
            }

            return program;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List<ApiModels.ProgramInfo> getPrograms() {
        return programs.values().stream()
                .map(p -> {
                    String owner = programOwners.getOrDefault(p.getName(), "unknown");
                    int runs = programRunCounts.getOrDefault(p.getName(), 0);
                    double avgCost = programAvgCosts.getOrDefault(p.getName(), 0.0);

                    // Extract function names from the program
                    List<String> functionNames = new ArrayList<>();
                    if (p instanceof SProgramImpl) {
                        SProgramImpl impl = (SProgramImpl) p;
                        functionNames.addAll(impl.getFunctions().keySet());
                    }

                    return new ApiModels.ProgramInfo(
                            p.getName(),
                            owner,
                            p.getInstructions().size(),
                            p.calculateMaxDegree(),
                            runs,
                            avgCost,
                            functionNames);
                })
                .toList();
    }

    public List<ApiModels.FunctionInfo> getFunctions() {
        List<ApiModels.FunctionInfo> result = new ArrayList<>();

        for (SProgram function : functions.values()) {
            String funcName = function.getName();
            String owner = functionOwners.getOrDefault(funcName, "unknown");
            String parentProgram = functionParentPrograms.getOrDefault(funcName, "unknown");

            result.add(new ApiModels.FunctionInfo(
                    funcName,
                    parentProgram,
                    owner,
                    function.getInstructions().size(),
                    function.calculateMaxDegree()));
        }

        return result;
    }

    // Run session management
    public RunSession createRunSession(String runId, String username, RunTarget target,
            Architecture arch, int degree, Inputs inputs) {
        RunSession session = new RunSession(runId, username, target, arch, degree, inputs);
        runSessions.put(runId, session);
        return session;
    }

    public RunSession getRunSession(String runId) {
        return runSessions.get(runId);
    }

    public void removeRunSession(String runId) {
        runSessions.remove(runId);
    }

    // History management
    public void addHistoryEntry(String username, String runId, String targetName, String targetType,
            String architecture, int degree, long finalYValue, int cycles, Map<String, Long> finalVariables) {
        addHistoryEntry(username, runId, targetName, targetType, architecture, degree, finalYValue, cycles,
                finalVariables, cycles);
    }

    /**
     * Add history entry with separate cycles and creditsSpent tracking
     */
    public void addHistoryEntry(String username, String runId, String targetName, String targetType,
            String architecture, int degree, long finalYValue, int cycles, Map<String, Long> finalVariables,
            int creditsSpent) {
        List<ApiModels.HistoryEntry> history = userHistory.computeIfAbsent(username, k -> new ArrayList<>());
        history.add(new ApiModels.HistoryEntry(runId, targetName, targetType, architecture,
                degree, finalYValue, cycles, System.currentTimeMillis(), finalVariables));

        // Update user run count
        UserRecord user = users.get(username);
        if (user != null) {
            user.totalRuns++;
            user.lastActive = System.currentTimeMillis();
            System.out.println("Updated user statistics for " + username +
                    ": totalRuns=" + user.totalRuns);
        }

        // Update program run statistics with actual credits spent (this will also
        // broadcast the program update)
        updateProgramStatistics(targetName, creditsSpent);

        // Broadcast user update to all connected clients
        try {
            com.semulator.server.realtime.UserUpdateServer.broadcastUserUpdate();
        } catch (Exception e) {
            System.err.println("Error broadcasting user update: " + e.getMessage());
        }

        // Broadcast history update to all connected clients
        try {
            com.semulator.server.realtime.UserUpdateServer.broadcastHistoryUpdate(username);
        } catch (Exception e) {
            System.err.println("Error broadcasting history update: " + e.getMessage());
        }

        incrementVersion();
    }

    /**
     * Increment user's total runs count
     * Public so it can be called from servlets (Debug, Run, etc.)
     */
    public void incrementUserRuns(String username) {
        UserRecord user = users.get(username);
        if (user != null) {
            user.totalRuns++;
            user.lastActive = System.currentTimeMillis();
            System.out.println("Incremented run count for " + username + ": totalRuns=" + user.totalRuns);

            // Broadcast user update to all connected clients
            try {
                com.semulator.server.realtime.UserUpdateServer.broadcastUserUpdate();
            } catch (Exception e) {
                System.err.println("Error broadcasting user update: " + e.getMessage());
            }

            incrementVersion();
        } else {
            System.out.println("User not found: " + username);
        }
    }

    /**
     * Update program run count and average cost
     * Public so it can be called from servlets (Debug, Run, etc.)
     */
    public void updateProgramStatistics(String programName, int creditsSpent) {
        // Update run count
        int currentRuns = programRunCounts.getOrDefault(programName, 0);
        programRunCounts.put(programName, currentRuns + 1);

        // Update average cost (based on actual credits spent, not cycles)
        double currentAvgCost = programAvgCosts.getOrDefault(programName, 0.0);
        double newAvgCost = ((currentAvgCost * currentRuns) + creditsSpent) / (currentRuns + 1);
        programAvgCosts.put(programName, newAvgCost);

        System.out.println("Updated statistics for " + programName +
                ": runs=" + (currentRuns + 1) +
                ", avgCost=" + String.format("%.2f", newAvgCost) + " credits");

        // Broadcast program update to all connected clients
        try {
            com.semulator.server.realtime.UserUpdateServer.broadcastProgramUpdate();
        } catch (Exception e) {
            System.err.println("Error broadcasting program update: " + e.getMessage());
        }
    }

    public List<ApiModels.HistoryEntry> getUserHistory(String username) {
        return userHistory.getOrDefault(username, new ArrayList<>());
    }

    // Data classes
    public static class UserRecord {
        public String username;
        public int credits;
        public int totalRuns;
        public long lastActive;
        public int mainPrograms;
        public int subfunctions;
        public int creditsUsed;

        public UserRecord(String username, int credits, int totalRuns, long lastActive) {
            this.username = username;
            this.credits = credits;
            this.totalRuns = totalRuns;
            this.lastActive = lastActive;
            this.mainPrograms = 0;
            this.subfunctions = 0;
            this.creditsUsed = 0;
        }
    }

    public static class RunSession {
        public final String runId;
        public final String username;
        public final RunTarget target;
        public final Architecture arch;
        public final int degree;
        public final Inputs inputs;
        public final long created;

        public String state = "RUNNING";
        public int cycles = 0;
        public int creditsSpent = 0; // Track actual credits spent during execution
        public int pointer = 0;
        public Long outputY = null;
        public String error = null;
        public Map<String, Integer> instrByArch = new HashMap<>();

        public RunSession(String runId, String username, RunTarget target,
                Architecture arch, int degree, Inputs inputs) {
            this.runId = runId;
            this.username = username;
            this.target = target;
            this.arch = arch;
            this.degree = degree;
            this.inputs = inputs;
            this.created = System.currentTimeMillis();
        }
    }

    public ApiModels.ProgramWithInstructions getProgramWithInstructions(String programName) {
        return getProgramWithInstructions(programName, 0);
    }

    public ApiModels.ProgramWithInstructions getProgramWithInstructions(String programName, int degree) {
        SProgram program = programs.get(programName);
        if (program == null) {
            return null;
        }

        // Expand program to the requested degree
        ExpansionResult expansion = program.expandToDegree(degree);
        List<SInstruction> expandedInstructions = expansion.instructions();

        List<ApiModels.InstructionDTO> instructionDTOs = new ArrayList<>();
        int rowNumber = 1;

        for (SInstruction instruction : expandedInstructions) {
            String labelName = "";
            if (instruction.getLabel() != null) {
                if (instruction.getLabel().isExit()) {
                    labelName = "EXIT";
                } else {
                    labelName = instruction.getLabel().getLabel();
                }
            }

            String variableName = "";
            if (instruction.getVariable() != null) {
                variableName = instruction.getVariable().getRepresentation();
            }

            // Determine command type (B/S)
            String commandType = getCommandType(instruction);

            // Get instruction text for display
            String instructionText = getInstructionText(instruction);

            // Determine architecture (placeholder for now - will be implemented later)
            String architecture = getArchitectureForInstruction(instruction);

            instructionDTOs.add(new ApiModels.InstructionDTO(
                    rowNumber++,
                    commandType,
                    labelName,
                    instructionText,
                    instruction.cycles(),
                    variableName,
                    architecture,
                    instruction.getName())); // Original instruction type name
        }

        return new ApiModels.ProgramWithInstructions(
                program.getName(),
                instructionDTOs,
                program.calculateMaxDegree(),
                new ArrayList<>() // TODO: Get function names from program
        );
    }

    public ApiModels.ProgramWithInstructions getFunctionWithInstructions(String functionName) {
        return getFunctionWithInstructions(functionName, 0);
    }

    public ApiModels.ProgramWithInstructions getFunctionWithInstructions(String functionName, int degree) {
        SProgram function = functions.get(functionName);
        if (function == null) {
            return null;
        }

        // Expand function to the requested degree
        ExpansionResult expansion = function.expandToDegree(degree);
        List<SInstruction> expandedInstructions = expansion.instructions();

        List<ApiModels.InstructionDTO> instructionDTOs = new ArrayList<>();
        int rowNumber = 1;

        for (SInstruction instruction : expandedInstructions) {
            String labelName = "";
            if (instruction.getLabel() != null) {
                if (instruction.getLabel().isExit()) {
                    labelName = "EXIT";
                } else {
                    labelName = instruction.getLabel().getLabel();
                }
            }

            String variableName = "";
            if (instruction.getVariable() != null) {
                variableName = instruction.getVariable().getRepresentation();
            }

            // Determine command type (B/S)
            String commandType = getCommandType(instruction);

            // Get instruction text for display
            String instructionText = getInstructionText(instruction);

            // Determine architecture (placeholder for now - will be implemented later)
            String architecture = getArchitectureForInstruction(instruction);

            instructionDTOs.add(new ApiModels.InstructionDTO(
                    rowNumber++,
                    commandType,
                    labelName,
                    instructionText,
                    instruction.cycles(),
                    variableName,
                    architecture,
                    instruction.getName())); // Original instruction type name
        }

        return new ApiModels.ProgramWithInstructions(
                function.getName(),
                instructionDTOs,
                function.calculateMaxDegree(),
                new ArrayList<>() // Functions don't have sub-functions
        );
    }

    public SProgram getProgram(String programName) {
        return programs.get(programName);
    }

    public SProgram getFunction(String functionName) {
        return functions.get(functionName);
    }

    public double getProgramAverageCost(String programName) {
        return programAvgCosts.getOrDefault(programName, 0.0);
    }

    private String getCommandType(SInstruction instruction) {
        // Basic instructions: INCREASE, DECREASE, NEUTRAL, JUMP_NOT_ZERO
        if (instruction instanceof IncreaseInstruction ||
                instruction instanceof DecreaseInstruction ||
                instruction instanceof NoOpInstruction ||
                instruction instanceof JumpNotZeroInstruction) {
            return "B";
        }
        return "S"; // Synthetic
    }

    private String getInstructionText(SInstruction instruction) {
        // Format instruction for display
        if (instruction instanceof IncreaseInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable() + " + 1";
        } else if (instruction instanceof DecreaseInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable() + " - 1";
        } else if (instruction instanceof NoOpInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable();
        } else if (instruction instanceof JumpNotZeroInstruction) {
            JumpNotZeroInstruction jnz = (JumpNotZeroInstruction) instruction;
            return "IF " + jnz.getVariable() + " != 0 GOTO " + jnz.getTarget();
        } else if (instruction instanceof ZeroVariableInstruction) {
            return instruction.getVariable() + " <- 0";
        } else if (instruction instanceof AssignVariableInstruction) {
            AssignVariableInstruction assign = (AssignVariableInstruction) instruction;
            return instruction.getVariable() + " <- " + assign.getSource();
        } else if (instruction instanceof AssignConstantInstruction) {
            AssignConstantInstruction assign = (AssignConstantInstruction) instruction;
            return instruction.getVariable() + " <- " + assign.getConstant();
        } else if (instruction instanceof GotoLabelInstruction) {
            GotoLabelInstruction goto_ = (GotoLabelInstruction) instruction;
            return "GOTO " + goto_.getTarget();
        } else if (instruction instanceof JumpZeroInstruction) {
            JumpZeroInstruction jz = (JumpZeroInstruction) instruction;
            return "IF " + jz.getVariable() + " == 0 GOTO " + jz.getTarget();
        } else if (instruction instanceof JumpEqualConstantInstruction) {
            JumpEqualConstantInstruction jec = (JumpEqualConstantInstruction) instruction;
            return "IF " + jec.getVariable() + " == " + jec.getConstant() + " GOTO " + jec.getTarget();
        } else if (instruction instanceof JumpEqualVariableInstruction) {
            JumpEqualVariableInstruction jev = (JumpEqualVariableInstruction) instruction;
            return "IF " + jev.getVariable() + " == " + jev.getOther() + " GOTO " + jev.getTarget();
        } else if (instruction instanceof QuoteInstruction) {
            QuoteInstruction quote = (QuoteInstruction) instruction;
            String args = formatFunctionArguments(quote.getFunctionArguments());
            return instruction.getVariable() + " <- (" + quote.getFunctionName() + args + ")";
        } else if (instruction instanceof JumpEqualFunctionInstruction) {
            JumpEqualFunctionInstruction jef = (JumpEqualFunctionInstruction) instruction;
            String args = formatFunctionArguments(jef.getFunctionArguments());
            return "IF " + jef.getVariable() + " == (" + jef.getFunctionName() + args + ") GOTO " + jef.getTarget();
        }
        return instruction.getName();
    }

    private String formatFunctionArguments(List<FunctionArgument> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arguments.size(); i++) {
            sb.append(",");
            FunctionArgument arg = arguments.get(i);

            if (arg.isFunctionCall()) {
                // Nested function call
                FunctionCall call = arg.asFunctionCall();
                sb.append("(").append(call.getFunctionName());
                sb.append(formatFunctionArguments(call.getArguments()));
                sb.append(")");
            } else {
                // Simple variable argument
                sb.append(arg.toString());
            }
        }

        return sb.toString();
    }

    private String getArchitectureForInstruction(SInstruction instruction) {
        // Architecture classification matching the instruction degree mapping
        // Architecture I - Degree 0: Basic instructions
        if (instruction instanceof IncreaseInstruction ||
                instruction instanceof DecreaseInstruction ||
                instruction instanceof NoOpInstruction ||
                instruction instanceof JumpNotZeroInstruction) {
            return "I";
        }
        // Architecture II - Degree 1: Zero, Constant Assignment, Goto
        else if (instruction instanceof ZeroVariableInstruction ||
                instruction instanceof AssignConstantInstruction ||
                instruction instanceof GotoLabelInstruction) {
            return "II";
        }
        // Architecture III - Degree 2: Variable Assignment, Jump Zero, Jump Equal
        else if (instruction instanceof AssignVariableInstruction ||
                instruction instanceof JumpZeroInstruction ||
                instruction instanceof JumpEqualConstantInstruction ||
                instruction instanceof JumpEqualVariableInstruction) {
            return "III";
        }
        // Architecture IV - Degree 3+: Quote and Function comparisons
        else if (instruction instanceof QuoteInstruction ||
                instruction instanceof JumpEqualFunctionInstruction) {
            return "IV";
        }
        return "I"; // Default
    }

    /**
     * Get history chain for a selected instruction using Exercise 2 logic
     */
    public List<ApiModels.HistoryChainItem> getHistoryChain(String programName, int instructionIndex,
            int currentDegree) {
        List<ApiModels.HistoryChainItem> chain = new ArrayList<>();

        try {
            // Get the program
            SProgram program = programs.get(programName);
            if (program == null) {
                return chain;
            }

            // Get expansion result for current degree
            ExpansionResult currentExpansionResult;
            try {
                currentExpansionResult = program.expandToDegree(currentDegree);
            } catch (Exception e) {
                return chain;
            }

            // Validate instruction index
            if (instructionIndex < 0 || instructionIndex >= currentExpansionResult.instructions().size()) {
                return chain;
            }

            // Get the selected instruction
            SInstruction selectedInstruction = currentExpansionResult.instructions().get(instructionIndex);

            // Start building the chain (most recent first)
            chain.add(createHistoryChainItem(selectedInstruction, instructionIndex, currentDegree));

            // Trace back through the parent chain (Exercise 2 logic)
            SInstruction current = selectedInstruction;
            int currentDegreeForTracing = currentDegree;
            ExpansionResult tracingExpansionResult = currentExpansionResult;

            while (currentDegreeForTracing > 0) {

                // Get the parent of the current instruction from the current expansion result
                SInstruction parent = tracingExpansionResult.parent().get(current);

                // If no exact match, try matching by name and variable
                if (parent == null) {
                    for (Map.Entry<SInstruction, SInstruction> entry : tracingExpansionResult.parent().entrySet()) {
                        if (entry.getKey().getName().equals(current.getName()) &&
                                entry.getKey().getVariable().equals(current.getVariable())) {
                            parent = entry.getValue();
                            break;
                        }
                    }
                }

                if (parent == null) {
                    break;
                }

                // Move to the previous degree
                currentDegreeForTracing--;

                // Get the expansion result for the previous degree
                ExpansionResult prevDegreeResult;
                try {
                    prevDegreeResult = program.expandToDegree(currentDegreeForTracing);
                } catch (Exception e) {
                    break;
                }

                // Update local expansion result for next iteration
                tracingExpansionResult = prevDegreeResult;
                current = parent;

                // Add parent to chain
                chain.add(createHistoryChainItem(parent, -1, currentDegreeForTracing));
            }

        } catch (Exception e) {
            // Silently handle errors
        }

        return chain;
    }

    private ApiModels.HistoryChainItem createHistoryChainItem(SInstruction instruction, int rowNumber, int degree) {
        String commandType = isBasicInstruction(instruction) ? "B" : "S";
        String label = instruction.getLabel() != null ? instruction.getLabel().getLabel() : "";
        String instructionText = getInstructionText(instruction);
        String variable = instruction.getVariable() != null ? instruction.getVariable().toString() : "";
        String architecture = getArchitectureForInstruction(instruction);

        return new ApiModels.HistoryChainItem(
                rowNumber,
                commandType,
                label,
                instructionText,
                instruction.cycles(),
                variable,
                architecture,
                instruction.getName(),
                degree);
    }

    private boolean isBasicInstruction(SInstruction instruction) {
        return instruction instanceof IncreaseInstruction ||
                instruction instanceof DecreaseInstruction ||
                instruction instanceof NoOpInstruction ||
                instruction instanceof JumpNotZeroInstruction;
    }

    // Debug session management
    private final Map<String, DebugSession> debugSessions = new ConcurrentHashMap<>();

    public DebugSession createDebugSession(String sessionId, String username, String programName,
            int degree, List<Long> inputs) {
        SProgram program = programs.get(programName);
        if (program == null) {
            return null;
        }

        DebugSession session = new DebugSession(sessionId, username, programName, program, degree, inputs);
        debugSessions.put(sessionId, session);
        return session;
    }

    public DebugSession getDebugSession(String sessionId) {
        return debugSessions.get(sessionId);
    }

    public void removeDebugSession(String sessionId) {
        debugSessions.remove(sessionId);
    }

    /**
     * Debug session state container
     */
    public static class DebugSession {
        public final String sessionId;
        public final String username;
        public final String programName;
        public final SProgram program;
        public final int degree;
        public final List<Long> inputs;
        public final long created;

        public ProgramExecutor executor;
        public ExecutionContext executionContext;
        public List<SInstruction> instructions;
        public Map<String, Integer> labelToIndexMap;

        public String state = "READY"; // READY, RUNNING, PAUSED, FINISHED, ERROR
        public int currentInstructionIndex = 0;
        public int cycles = 0;
        public int creditsSpent = 0; // Track actual credits spent during execution
        public Map<String, Long> variables = new HashMap<>();
        public String error = null;

        public DebugSession(String sessionId, String username, String programName, SProgram program,
                int degree, List<Long> inputs) {
            this.sessionId = sessionId;
            this.username = username;
            this.programName = programName;
            this.program = program;
            this.degree = degree;
            this.inputs = inputs;
            this.created = System.currentTimeMillis();

            this.executor = new ProgramExecutorImpl(program);
            ExpansionResult expansion = program.expandToDegree(degree);
            this.instructions = expansion.instructions();
            this.labelToIndexMap = buildLabelMap(instructions);
            initializeContext();
        }

        private void initializeContext() {
            StepExecutionContext stepContext = new StepExecutionContext();
            this.executionContext = stepContext;

            // Set input variables
            for (int i = 0; i < inputs.size(); i++) {
                String varName = "x" + (i + 1);
                stepContext.setVariable(varName, inputs.get(i));
            }

            // Initialize other variables to 0
            for (SInstruction instruction : instructions) {
                if (instruction.getVariable() != null) {
                    String varName = instruction.getVariable().toString();
                    if (!stepContext.hasVariable(varName)) {
                        stepContext.setVariable(varName, 0L);
                    }
                }
            }

            updateVariablesFromContext();
        }

        private Map<String, Integer> buildLabelMap(List<SInstruction> instructions) {
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < instructions.size(); i++) {
                SInstruction instruction = instructions.get(i);
                if (instruction.getLabel() != null && !instruction.getLabel().isExit()
                        && instruction.getLabel().getLabel() != null) {
                    map.put(instruction.getLabel().getLabel(), i);
                }
            }
            return map;
        }

        public void updateVariablesFromContext() {
            variables.clear();
            if (executionContext instanceof StepExecutionContext) {
                StepExecutionContext stepContext = (StepExecutionContext) executionContext;
                variables.putAll(stepContext.getVariableMap());
            }
        }
    }

    /**
     * Custom execution context for step-by-step execution
     */
    private static class StepExecutionContext implements ExecutionContext {
        private final Map<String, Long> variables = new HashMap<>();

        @Override
        public long getVariableValue(Variable v) {
            return variables.getOrDefault(v.getRepresentation(), 0L);
        }

        @Override
        public void updateVariable(Variable v, long value) {
            variables.put(v.getRepresentation(), value);
        }

        public void setVariable(String name, Long value) {
            variables.put(name, value);
        }

        public boolean hasVariable(String name) {
            return variables.containsKey(name);
        }

        public void clearVariables() {
            variables.clear();
        }

        public Map<String, Long> getVariableMap() {
            return new HashMap<>(variables);
        }
    }
}
