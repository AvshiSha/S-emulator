package com.semulator.server.state;

import com.semulator.engine.model.*;
import com.semulator.engine.parse.SProgramImpl;
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
            return true;
        }
        return false;
    }

    public boolean deductCredits(String username, int amount) {
        UserRecord user = users.get(username);
        if (user != null && user.credits >= amount) {
            user.credits -= amount;
            user.lastActive = System.currentTimeMillis();
            incrementVersion();
            return true;
        }
        return false;
    }

    public List<ApiModels.UserInfo> getAllUsers() {
        // Only return users who have active tokens (are currently connected)
        return users.values().stream()
                .filter(u -> hasActiveToken(u.username))
                .map(u -> new ApiModels.UserInfo(u.username, u.credits, u.totalRuns, u.lastActive))
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
            if (program instanceof SProgramImpl) {
                SProgramImpl impl = (SProgramImpl) program;
                Map<String, List<SInstruction>> programFunctions = impl.getFunctions();
                for (String funcName : programFunctions.keySet()) {
                    // Create a function program for each function
                    SProgramImpl functionProgram = new SProgramImpl(funcName);
                    functionProgram.loadFromXmlContent(xmlContent);
                    functions.put(funcName, functionProgram);

                    // Track function metadata
                    functionOwners.put(funcName, ownerUsername);
                    functionParentPrograms.put(funcName, program.getName());
                }
            }

            incrementVersion();

            // Broadcast program update to all connected clients
            try {
                com.semulator.server.realtime.UserUpdateServer.broadcastProgramUpdate();
            } catch (Exception e) {
                System.err.println("Error broadcasting program update: " + e.getMessage());
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
    public void addHistoryEntry(String username, String runId, String programName,
            long result, int cycles) {
        List<ApiModels.HistoryEntry> history = userHistory.computeIfAbsent(username, k -> new ArrayList<>());
        history.add(new ApiModels.HistoryEntry(runId, programName, result, cycles, System.currentTimeMillis()));

        // Update user run count
        UserRecord user = users.get(username);
        if (user != null) {
            user.totalRuns++;
            user.lastActive = System.currentTimeMillis();
        }

        incrementVersion();
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

        public UserRecord(String username, int credits, int totalRuns, long lastActive) {
            this.username = username;
            this.credits = credits;
            this.totalRuns = totalRuns;
            this.lastActive = lastActive;
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
        SProgram program = programs.get(programName);
        if (program == null) {
            return null;
        }

        List<ApiModels.InstructionDTO> instructionDTOs = new ArrayList<>();
        for (SInstruction instruction : program.getInstructions()) {
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

            instructionDTOs.add(new ApiModels.InstructionDTO(
                    instruction.getName(),
                    instruction.cycles(),
                    labelName,
                    variableName));
        }

        return new ApiModels.ProgramWithInstructions(
                program.getName(),
                instructionDTOs,
                program.calculateMaxDegree(),
                new ArrayList<>() // TODO: Get function names from program
        );
    }
}
